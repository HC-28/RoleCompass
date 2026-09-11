package com.rolecompass.routing;

import com.rolecompass.aggregation.FeatureAggregationService;
import com.rolecompass.aggregation.FeatureIndex;
import com.rolecompass.dto.AnswerItem;
import com.rolecompass.dto.AnswerRequest;
import com.rolecompass.dto.QuestionDTO;
import com.rolecompass.dto.SessionStartResponse;
import com.rolecompass.entity.Answer;
import com.rolecompass.entity.AnswerId;
import com.rolecompass.entity.Question;
import com.rolecompass.entity.Session;
import com.rolecompass.entity.User;
import com.rolecompass.repository.AnswerRepository;
import com.rolecompass.repository.QuestionRepository;
import com.rolecompass.repository.SessionRepository;
import com.rolecompass.seed.DataSeeder;
import com.rolecompass.service.QuestionService;
import com.rolecompass.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Section 1 Assessment Foundation Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SectionOneAssessmentFoundationTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private AnswerRepository answerRepository;

    private List<Question> seededQuestions;
    private QuestionService questionService;
    private AdaptiveRoutingEngine routingEngine;
    private FeatureAggregationService aggregationService;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        seededQuestions = DataSeeder.buildSection1();
        // Set numeric IDs for seeded questions 1..10
        for (int i = 0; i < seededQuestions.size(); i++) {
            seededQuestions.get(i).setId((long) (i + 1));
        }

        when(questionRepository.findBySectionIdOrderByIdAsc(1)).thenReturn(seededQuestions);
        when(questionRepository.findAll()).thenReturn(seededQuestions);
        when(questionRepository.findAllById(any())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return seededQuestions.stream().filter(q -> ids.contains(q.getId())).collect(Collectors.toList());
        });

        questionService = new QuestionService(questionRepository);
        routingEngine = new AdaptiveRoutingEngine(questionRepository);
        aggregationService = new FeatureAggregationService(answerRepository, questionRepository);
        sessionService = new SessionService(
                sessionRepository,
                answerRepository,
                questionRepository,
                questionService,
                routingEngine,
                aggregationService
        );
    }

    // ── 1. Section 1 Count & Universal Rule ──────────────────────────────────

    @Test
    @DisplayName("Requirement 1: Section 1 contains exactly 16 psychological questions")
    void sectionOne_hasExpected16Questions() {
        assertThat(seededQuestions).hasSize(16);
    }

    @Test
    @DisplayName("Requirement 2: Every Section 1 question has section_id = 1")
    void sectionOne_allQuestionsHaveSectionIdOne() {
        for (Question q : seededQuestions) {
            assertThat(q.getSectionId())
                    .as("Question ID %d must have section_id = 1", q.getId())
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Requirement 3: Every Section 1 question is universal (no role-dependent gate)")
    void sectionOne_allQuestionsAreUniversal() {
        for (Question q : seededQuestions) {
            assertThat(q.getTriggerPredicate())
                    .as("Question ID %d must be universal", q.getId())
                    .contains("\"always\": true");
        }
    }

    // ── 2. Exposure-Independence Rule ────────────────────────────────────────

    @Test
    @DisplayName("Requirement 4: Question wording does NOT name specific technologies, frameworks, or roles")
    void sectionOne_exposureIndependentWording() {
        String[] prohibitedTerms = {
                "java", "python", "javascript", "react", "spring", "docker", "kubernetes", "k8s",
                "sql", "database", "backend", "frontend", "devops", "cloud", "aws", "azure", "c#",
                "git", "ci/cd", "rest api", "cybersecurity", "qa", "test automation"
        };

        for (Question q : seededQuestions) {
            String lower = q.getText().toLowerCase(Locale.ROOT);
            for (String prohibited : prohibitedTerms) {
                assertThat(lower)
                        .as("Question %d '%s' must not contain prohibited term '%s'", q.getId(), q.getText(), prohibited)
                        .doesNotContain(prohibited);
            }
        }
    }

    // ── 3. Internal Dimension Tags ────────────────────────────────────────────

    @Test
    @DisplayName("Requirement 5: Section 1 covers all key psychological dimensions")
    void sectionOne_dimensionTagDistribution() {
        Set<String> presentTags = new HashSet<>();
        for (Question q : seededQuestions) {
            assertThat(q.getDimensionTags()).isNotEmpty();
            Collections.addAll(presentTags, q.getDimensionTags());
        }

        assertThat(presentTags).contains(
                FeatureIndex.TAG_DIM_REALISTIC,
                FeatureIndex.TAG_DIM_INVESTIGATIVE,
                FeatureIndex.TAG_DIM_ARTISTIC,
                FeatureIndex.TAG_DIM_SOCIAL,
                FeatureIndex.TAG_DIM_ENTERPRISING,
                FeatureIndex.TAG_DIM_CONVENTIONAL,
                FeatureIndex.TAG_DIM_DATA_IDEAS,
                FeatureIndex.TAG_DIM_THINGS_PEOPLE,
                FeatureIndex.TAG_DIM_BREADTH_DEPTH,
                FeatureIndex.TAG_DIM_STRUCT_AMBIG
        );
    }

    @Test
    @DisplayName("Requirement 6: Client QuestionDTO does NOT expose dimension tags or internal metadata")
    void questionDTO_doesNotExposeTags() {
        List<QuestionDTO> dtos = questionService.getSectionOneQuestions();
        for (QuestionDTO dto : dtos) {
            assertThat(dto.getId()).isNotNull();
            assertThat(dto.getText()).isNotBlank();
            assertThat(dto.getOptions()).containsExactly(1, 2, 3, 4, 5);
        }
    }

    // ── 4. Batch Delivery & Progression ──────────────────────────────────────

    @Test
    @DisplayName("Requirement 7: Start session returns the assessment questions")
    void startSession_returnsQuestions() {
        User mockUser = User.builder().id(UUID.randomUUID()).email("student@test.com").build();
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SessionStartResponse response = sessionService.startSession(mockUser);

        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getQuestions()).hasSize(AdaptiveRoutingEngine.BATCH_SIZE);
    }

    @Test
    @DisplayName("Requirement 8: Submitting Section 1 answers advances session in_progress into next section")
    void submitSectionOneAnswers_progressesSession() {
        UUID sessionId = UUID.randomUUID();
        User mockUser = User.builder().id(UUID.randomUUID()).email("student@test.com").build();

        Session session = Session.builder()
                .id(sessionId)
                .userId(mockUser.getId())
                .status("in_progress")
                .candidateRoleIds(new Long[]{1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L})
                .answeredVector(new Double[20])
                .answeredDimsMask(new Boolean[20])
                .build();

        when(sessionRepository.findByIdAndUserId(sessionId, mockUser.getId())).thenReturn(Optional.of(session));

        // 10 answers
        List<Answer> all10Answers = new ArrayList<>();
        List<AnswerItem> submittedItems = new ArrayList<>();
        for (long id = 1; id <= 10; id++) {
            all10Answers.add(Answer.builder()
                    .id(new AnswerId(sessionId, id))
                    .likertValue(4)
                    .build());
            submittedItems.add(new AnswerItem(id, 4));
        }
        when(answerRepository.findByIdSessionId(sessionId)).thenReturn(all10Answers);

        Map<String, Object> response = sessionService.submitAnswers(sessionId, mockUser, new AnswerRequest(submittedItems));

        assertThat(response.get("status")).isEqualTo("in_progress");
        assertThat(response.get("answers_count")).isEqualTo(10);
    }

    // ── 5. Profile Service Endpoint ──────────────────────────────────────────

    @Test
    @DisplayName("Requirement 9: User Profile service retrieves assessment statistics")
    void getProfile_retrievesUserStats() {
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder().id(userId).email("student@test.com").build();
        Session mockSession = Session.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status("ready_to_predict")
                .build();

        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(mockSession));

        Map<String, Object> profile = sessionService.getProfile(mockUser);

        assertThat(profile.get("email")).isEqualTo("student@test.com");
        assertThat(profile.get("total_assessments")).isEqualTo(1);
        assertThat(profile.get("sessions")).isNotNull();
    }

    // ── 6. Deterministic Profile Aggregation ──────────────────────────────────

    @Test
    @DisplayName("Requirement 10: Assessment profile updates deterministically based on Likert scores")
    void assessmentProfile_updatesDeterministically() {
        Map<Long, Integer> answers = Map.of(
                1L, 5 // Realistic (q1) = 5 -> normalized = (5 - 1)/4 = 1.0
        );
        Map<Long, Question> qMap = seededQuestions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Map<String, Double> psych = FeatureAggregationService.buildPsychProfileFromMaps(answers, qMap);

        assertThat(psych.get(FeatureIndex.TAG_DIM_REALISTIC)).isEqualTo(1.0);
        // Unanswered dimensions should be neutral 0.50
        assertThat(psych.get(FeatureIndex.TAG_DIM_INVESTIGATIVE)).isEqualTo(0.50);
        assertThat(psych.get(FeatureIndex.TAG_DIM_ARTISTIC)).isEqualTo(0.50);
    }
}
