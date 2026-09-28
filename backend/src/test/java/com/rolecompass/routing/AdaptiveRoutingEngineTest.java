package com.rolecompass.routing;

import com.rolecompass.aggregation.FeatureIndex;
import com.rolecompass.entity.Question;
import com.rolecompass.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for AdaptiveRoutingEngine with FSM and discrete threshold gates.
 */
@DisplayName("AdaptiveRoutingEngine FSM Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdaptiveRoutingEngineTest {

    @Mock
    private QuestionRepository questionRepository;

    private AdaptiveRoutingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AdaptiveRoutingEngine(questionRepository);

        Question q1 = Question.builder().id(101L).sectionId(1).text("S1 Q1").triggerPredicate("{\"always\": true}").build();
        Question q2 = Question.builder().id(102L).sectionId(1).text("S1 Q2").triggerPredicate("{\"always\": true}").build();
        Question q3 = Question.builder().id(201L).sectionId(2).text("S2 Tech Core")
                .dimensionTags(new String[]{"TECH_SERVER"}).triggerPredicate("{\"always\": true}").build();
        Question q4 = Question.builder().id(301L).sectionId(3).text("S3 Resolver")
                .triggerPredicate("{\"requires_both\": [\"DevOps Engineer\", \"Cloud Engineer\"]}").build();
        Question q5 = Question.builder().id(401L).sectionId(4).text("S4 Specialist")
                .triggerPredicate("{\"requires_any\": [\"Data Engineer\", \"Backend Developer\", \"Data Scientist\"]}").build();

        lenient().when(questionRepository.findBySectionIdOrderByIdAsc(1)).thenReturn(List.of(q1, q2));
        lenient().when(questionRepository.findBySectionIdOrderByIdAsc(2)).thenReturn(List.of(q3));
        lenient().when(questionRepository.findBySectionIdOrderByIdAsc(3)).thenReturn(List.of(q4));
        lenient().when(questionRepository.findBySectionIdOrderByIdAsc(4)).thenReturn(List.of(q5));
    }

    private RoutingState stateWith(FsmState fsm, List<String> candidates, Map<String, Double> psych, double[] tech, Set<Long> answered) {
        return new RoutingState(
                UUID.randomUUID(),
                fsm,
                candidates,
                psych,
                tech != null ? tech : new double[20],
                answered != null ? answered.size() : 0,
                answered != null ? answered : Set.of(),
                Map.of(),
                new java.util.ArrayList<>()   // eliminationLog — empty for unit tests
        );
    }

    // ─── Initial State ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Initial state has all 10 candidate roles and SECTION_1_RIASEC state")
    void initialState_hasAllTenCandidates() {
        RoutingState state = RoutingState.initialState(UUID.randomUUID());
        assertThat(state.fsmState()).isEqualTo(FsmState.SECTION_1_RIASEC);
        assertThat(state.candidateRoles()).containsExactlyInAnyOrderElementsOf(RoutingState.ALL_ROLES);
        assertThat(state.psychProfile()).isNotEmpty();
    }

    // ─── Section 1 Progression & Batching ─────────────────────────────────────

    @Test
    @DisplayName("Section 1 delivers unanswered questions in batch")
    void section1_deliversBatch() {
        RoutingState state = stateWith(FsmState.SECTION_1_RIASEC, RoutingState.ALL_ROLES, Map.of(), null, Set.of());
        RoutingDecision decision = engine.evaluate(state);

        assertThat(decision.readyToPredict()).isFalse();
        assertThat(decision.nextFsmState()).isEqualTo(FsmState.SECTION_1_RIASEC);
        assertThat(decision.nextQuestionIds()).containsExactly(101L, 102L);
    }

    @Test
    @DisplayName("Section 1 transitions when all questions in section are answered")
    void section1_transitionsWhenComplete() {
        RoutingState state = stateWith(FsmState.SECTION_1_RIASEC, RoutingState.ALL_ROLES, Map.of(), null, Set.of(101L, 102L));
        RoutingDecision decision = engine.evaluate(state);

        // Advances through PRUNE_PSYCHOMETRICS into SECTION_2_TECH_CORE
        assertThat(decision.nextFsmState()).isEqualTo(FsmState.SECTION_2_TECH_CORE);
        assertThat(decision.nextQuestionIds()).contains(201L);
    }

    // ─── Discrete RIASEC Elimination Gates ────────────────────────────────────

    @Test
    @DisplayName("Artistic Gate: Low Artistic score eliminates Frontend Developer")
    void artisticGate_eliminatesFrontend() {
        Map<String, Double> psych = new HashMap<>();
        psych.put(FeatureIndex.TAG_DIM_ARTISTIC, 0.20); // below 0.35 threshold
        psych.put(FeatureIndex.TAG_DIM_INVESTIGATIVE, 0.80);
        psych.put(FeatureIndex.TAG_DIM_REALISTIC, 0.80);
        psych.put(FeatureIndex.TAG_DIM_CONVENTIONAL, 0.80);

        RoutingState state = stateWith(FsmState.PRUNE_PSYCHOMETRICS, new ArrayList<>(RoutingState.ALL_ROLES), psych, null, Set.of());
        AdaptiveRoutingEngine.GateResult result = engine.applyPsychometricGates(state);

        assertThat(result.survivors()).doesNotContain("Frontend Developer");
        assertThat(result.survivors()).contains("Backend Developer");
        // Elimination log should record why Frontend Developer was ruled out
        assertThat(result.log()).anyMatch(e -> e.getRole().equals("Frontend Developer"));
        assertThat(result.log()).anyMatch(e -> "PSYCHOMETRIC".equals(e.getStage()));
    }

    @Test
    @DisplayName("Investigative Gate: Low Investigative score eliminates Data Scientist")
    void investigativeGate_eliminatesDataScientist() {
        Map<String, Double> psych = new HashMap<>();
        psych.put(FeatureIndex.TAG_DIM_INVESTIGATIVE, 0.40); // below 0.60 threshold
        psych.put(FeatureIndex.TAG_DIM_ARTISTIC, 0.60);
        psych.put(FeatureIndex.TAG_DIM_REALISTIC, 0.60);
        psych.put(FeatureIndex.TAG_DIM_CONVENTIONAL, 0.80);

        RoutingState state = stateWith(FsmState.PRUNE_PSYCHOMETRICS, new ArrayList<>(RoutingState.ALL_ROLES), psych, null, Set.of());
        AdaptiveRoutingEngine.GateResult result = engine.applyPsychometricGates(state);

        assertThat(result.survivors()).doesNotContain("Data Scientist");
        assertThat(result.log()).anyMatch(e -> e.getRole().equals("Data Scientist"));
    }

    @Test
    @DisplayName("Realistic Gate: Low Realistic score eliminates DevOps and Cloud Engineers")
    void realisticGate_eliminatesDevOpsAndCloud() {
        Map<String, Double> psych = new HashMap<>();
        psych.put(FeatureIndex.TAG_DIM_REALISTIC, 0.30); // below 0.40 threshold
        psych.put(FeatureIndex.TAG_DIM_INVESTIGATIVE, 0.70);
        psych.put(FeatureIndex.TAG_DIM_ARTISTIC, 0.40);
        psych.put(FeatureIndex.TAG_DIM_CONVENTIONAL, 0.80);

        RoutingState state = stateWith(FsmState.PRUNE_PSYCHOMETRICS, new ArrayList<>(RoutingState.ALL_ROLES), psych, null, Set.of());
        AdaptiveRoutingEngine.GateResult result = engine.applyPsychometricGates(state);

        assertThat(result.survivors()).doesNotContain("DevOps Engineer", "Cloud Engineer");
        // Both DevOps and Cloud should appear in the elimination log
        assertThat(result.log().stream().map(e -> e.getRole()).toList())
            .containsExactlyInAnyOrder("DevOps Engineer", "Cloud Engineer");
    }

    @Test
    @DisplayName("Safety floor: candidate set is never reduced below MIN_CANDIDATES")
    void safetyFloor_enforced() {
        Map<String, Double> extremePsych = new HashMap<>();
        // All low scores triggers all elimination gates
        extremePsych.put(FeatureIndex.TAG_DIM_ARTISTIC, 0.10);
        extremePsych.put(FeatureIndex.TAG_DIM_INVESTIGATIVE, 0.10);
        extremePsych.put(FeatureIndex.TAG_DIM_REALISTIC, 0.10);
        extremePsych.put(FeatureIndex.TAG_DIM_CONVENTIONAL, 0.10);

        RoutingState state = stateWith(FsmState.PRUNE_PSYCHOMETRICS, new ArrayList<>(RoutingState.ALL_ROLES), extremePsych, null, Set.of());
        AdaptiveRoutingEngine.GateResult result = engine.applyPsychometricGates(state);

        assertThat(result.survivors().size()).isGreaterThanOrEqualTo(AdaptiveRoutingEngine.MIN_CANDIDATES);
    }

    // ─── Tech Floor Gates ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Mobile floor: low MOBILE skill eliminates Android Developer")
    void techFloor_mobileEliminatesAndroid() {
        double[] tech = new double[20];
        Arrays.fill(tech, 0.8);
        tech[12] = 0.20; // MOBILE feature at index 12 < 0.40

        RoutingState state = stateWith(FsmState.PRUNE_TECH_SKILLS, new ArrayList<>(RoutingState.ALL_ROLES), Map.of(), tech, Set.of());
        AdaptiveRoutingEngine.GateResult result = engine.applyTechFloorGates(state);

        assertThat(result.survivors()).doesNotContain("Android Developer");
        assertThat(result.log()).anyMatch(e -> e.getRole().equals("Android Developer"));
        assertThat(result.log()).anyMatch(e -> "TECHNICAL".equals(e.getStage()));
    }

    // ─── Trigger Predicate Evaluation ─────────────────────────────────────────

    @Test
    @DisplayName("Trigger predicate: requires_both is true only when both roles are candidates")
    void triggerPredicate_requiresBoth() {
        String pred = "{\"requires_both\": [\"DevOps Engineer\", \"Cloud Engineer\"]}";

        assertThat(engine.isTriggerSatisfied(pred, Set.of("DevOps Engineer", "Cloud Engineer", "Backend Developer"))).isTrue();
        assertThat(engine.isTriggerSatisfied(pred, Set.of("DevOps Engineer", "Backend Developer"))).isFalse();
    }

    @Test
    @DisplayName("Trigger predicate: requires_any is true when at least one role is candidate")
    void triggerPredicate_requiresAny() {
        String pred = "{\"requires_any\": [\"Data Engineer\", \"Backend Developer\"]}";

        assertThat(engine.isTriggerSatisfied(pred, Set.of("Backend Developer", "Cloud Engineer"))).isTrue();
        assertThat(engine.isTriggerSatisfied(pred, Set.of("DevOps Engineer", "Cloud Engineer"))).isFalse();
    }

    // ─── Terminal State & Decisions ───────────────────────────────────────────

    @Test
    @DisplayName("TERMINAL_SCORING returns readyToPredict=true with COMPLETED state")
    void terminalScoring_readyToPredict() {
        RoutingState state = stateWith(FsmState.TERMINAL_SCORING, List.of("Backend Developer", "Full Stack Developer"), Map.of(), null, Set.of());
        RoutingDecision decision = engine.evaluate(state);

        assertThat(decision.readyToPredict()).isTrue();
        assertThat(decision.survivingRoles()).containsExactly("Backend Developer", "Full Stack Developer");
        assertThat(decision.nextFsmState()).isEqualTo(FsmState.COMPLETED);
    }

    // ─── Option 3 Confidence-Gated Active Learning Tests ───────────────────────

    private RoutingState stateWithOptions(FsmState fsm, List<String> candidates, Double margin, List<String> topRoles) {
        return new RoutingState(
                UUID.randomUUID(),
                fsm,
                candidates,
                Map.of(),
                new double[20],
                0,
                Set.of(),
                Map.of(),
                new ArrayList<>(),
                margin,
                topRoles
        );
    }

    @Test
    @DisplayName("Option 3: Decisive ML confidence (margin >= 0.18) bypasses Section 3 and 4")
    void option3_decisiveConfidence_bypassesSections3And4() {
        // High confidence lead: Backend Developer 55%, Runner-up 30% -> margin = 0.25 >= 0.18
        RoutingState state = stateWithOptions(
                FsmState.RESOLVER_EVALUATION,
                List.of("DevOps Engineer", "Cloud Engineer"),
                0.25,
                List.of("DevOps Engineer", "Cloud Engineer")
        );

        RoutingDecision decision = engine.evaluate(state);

        assertThat(decision.readyToPredict()).isTrue();
        assertThat(decision.nextFsmState()).isEqualTo(FsmState.TERMINAL_SCORING);
        assertThat(decision.nextQuestionIds()).isEmpty();
    }

    @Test
    @DisplayName("Option 3: Close tie / ambiguous confidence (margin < 0.18) fires Section 3 Resolvers")
    void option3_ambiguousConfidence_firesSection3Resolvers() {
        // Close tie: DevOps 40%, Cloud 37% -> margin = 0.03 < 0.18
        RoutingState state = stateWithOptions(
                FsmState.RESOLVER_EVALUATION,
                List.of("DevOps Engineer", "Cloud Engineer"),
                0.03,
                List.of("DevOps Engineer", "Cloud Engineer")
        );

        RoutingDecision decision = engine.evaluate(state);

        assertThat(decision.readyToPredict()).isFalse();
        assertThat(decision.nextFsmState()).isEqualTo(FsmState.SECTION_3_RESOLVER);
        // q4 (301L) has requires_both: [DevOps Engineer, Cloud Engineer]
        assertThat(decision.nextQuestionIds()).contains(301L);
    }

    @Test
    @DisplayName("Option 3: Close tie / ambiguous confidence with no Section 3 resolver fires Section 4 Specialist Probes")
    void option3_ambiguousConfidence_noResolver_firesSection4Probes() {
        // Ambiguous tie: Data Engineer 36%, Backend Developer 34% -> margin = 0.02 < 0.18
        // No Section 3 pair resolver exists for Data Engineer & Backend Developer in mock setup,
        // but Data Engineer is a Section 4 specialist role.
        RoutingState state = stateWithOptions(
                FsmState.RESOLVER_EVALUATION,
                List.of("Data Engineer", "Backend Developer"),
                0.02,
                List.of("Data Engineer", "Backend Developer")
        );

        RoutingDecision decision = engine.evaluate(state);

        assertThat(decision.readyToPredict()).isFalse();
        assertThat(decision.nextFsmState()).isEqualTo(FsmState.SECTION_4_SPECIALIST);
        // q5 (401L) has requires_any: [Data Engineer, Backend Developer, Data Scientist]
        assertThat(decision.nextQuestionIds()).contains(401L);
    }

    @Test
    @DisplayName("Option 3: ML service is dynamically called at RESOLVER_EVALUATION to query confidence margin")
    void option3_dynamicMlCallAtResolverEvaluation() {
        com.rolecompass.service.MlClientService mockMl = org.mockito.Mockito.mock(com.rolecompass.service.MlClientService.class);
        AdaptiveRoutingEngine engineWithMl = new AdaptiveRoutingEngine(questionRepository, mockMl);

        // Mock ML response: Decisive lead for DevOps Engineer (55% vs 30% -> margin 0.25)
        com.rolecompass.dto.response.PredictionResponse mlResponse = com.rolecompass.dto.response.PredictionResponse.builder()
                .predictedRole("DevOps Engineer")
                .confidence(0.55)
                .alternates(List.of(
                        com.rolecompass.dto.response.PredictionResponse.AlternateRole.builder()
                                .role("Cloud Engineer")
                                .confidence(0.30)
                                .build()
                ))
                .build();

        org.mockito.Mockito.when(mockMl.score(any(double[].class), any())).thenReturn(mlResponse);

        // State has null margin (initial state arriving at RESOLVER_EVALUATION)
        RoutingState state = stateWithOptions(
                FsmState.RESOLVER_EVALUATION,
                List.of("DevOps Engineer", "Cloud Engineer"),
                null,
                null
        );

        RoutingDecision decision = engineWithMl.evaluate(state);

        // Verify ML was called with the exact candidate roles and tech vector
        org.mockito.Mockito.verify(mockMl).score(state.techVector(), state.candidateRoles());

        // Because margin is 0.25 >= 0.18, decisive bypass should fire
        assertThat(decision.readyToPredict()).isTrue();
        assertThat(decision.nextFsmState()).isEqualTo(FsmState.TERMINAL_SCORING);
    }

    @Test
    @DisplayName("Option 3: ML service is dynamically called at advanceFromResolver to check if tie was resolved")
    void option3_dynamicMlCallAtAdvanceFromResolver() {
        com.rolecompass.service.MlClientService mockMl = org.mockito.Mockito.mock(com.rolecompass.service.MlClientService.class);
        AdaptiveRoutingEngine engineWithMl = new AdaptiveRoutingEngine(questionRepository, mockMl);

        // Mock ML response after resolvers: now decisive (Data Engineer 60% vs Backend 35% -> margin 0.25)
        com.rolecompass.dto.response.PredictionResponse resolvedMl = com.rolecompass.dto.response.PredictionResponse.builder()
                .predictedRole("Data Engineer")
                .confidence(0.60)
                .alternates(List.of(
                        com.rolecompass.dto.response.PredictionResponse.AlternateRole.builder()
                                .role("Backend Developer")
                                .confidence(0.35)
                                .build()
                ))
                .build();

        org.mockito.Mockito.when(mockMl.score(any(double[].class), any())).thenReturn(resolvedMl);

        // Section 3 has completed (all resolver questions answered)
        RoutingState state = stateWithOptions(
                FsmState.SECTION_3_RESOLVER,
                List.of("Data Engineer", "Backend Developer"),
                null,
                null
        );

        RoutingDecision decision = engineWithMl.evaluate(state);

        // Verify ML re-scoring was called
        org.mockito.Mockito.verify(mockMl).score(state.techVector(), state.candidateRoles());

        // Because margin is now 0.25 >= 0.18, Section 4 specialist probes are skipped
        assertThat(decision.readyToPredict()).isTrue();
        assertThat(decision.nextFsmState()).isEqualTo(FsmState.TERMINAL_SCORING);
    }
}
