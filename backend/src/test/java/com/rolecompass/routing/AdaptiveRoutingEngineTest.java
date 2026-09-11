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
        Question q3 = Question.builder().id(301L).sectionId(3).text("S3 Q1").triggerPredicate("{\"always\": true}").build();
        Question q4 = Question.builder().id(201L).sectionId(2).text("S2 Resolver")
                .triggerPredicate("{\"requires_both\": [\"DevOps Engineer\", \"Cloud Engineer\"]}").build();
        Question q5 = Question.builder().id(401L).sectionId(4).text("S4 Gated")
                .triggerPredicate("{\"requires_any\": [\"Data Engineer\", \"Backend Developer\", \"Data Scientist\"]}").build();

        lenient().when(questionRepository.findBySectionIdOrderByIdAsc(1)).thenReturn(List.of(q1, q2));
        lenient().when(questionRepository.findBySectionIdOrderByIdAsc(2)).thenReturn(List.of(q4));
        lenient().when(questionRepository.findBySectionIdOrderByIdAsc(3)).thenReturn(List.of(q3));
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
                answered != null ? answered : Set.of()
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

        // Advances through PRUNE_PSYCHOMETRICS into SECTION_3_TECH_CORE
        assertThat(decision.nextFsmState()).isEqualTo(FsmState.SECTION_3_TECH_CORE);
        assertThat(decision.nextQuestionIds()).contains(301L);
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
        List<String> survivors = engine.applyPsychometricGates(state);

        assertThat(survivors).doesNotContain("Frontend Developer");
        assertThat(survivors).contains("Backend Developer");
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
        List<String> survivors = engine.applyPsychometricGates(state);

        assertThat(survivors).doesNotContain("Data Scientist");
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
        List<String> survivors = engine.applyPsychometricGates(state);

        assertThat(survivors).doesNotContain("DevOps Engineer", "Cloud Engineer");
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
        List<String> survivors = engine.applyPsychometricGates(state);

        assertThat(survivors.size()).isGreaterThanOrEqualTo(AdaptiveRoutingEngine.MIN_CANDIDATES);
    }

    // ─── Tech Floor Gates ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Mobile floor: low MOBILE skill eliminates Android Developer")
    void techFloor_mobileEliminatesAndroid() {
        double[] tech = new double[20];
        Arrays.fill(tech, 0.8);
        tech[12] = 0.20; // MOBILE feature at index 12 < 0.40

        RoutingState state = stateWith(FsmState.PRUNE_TECH_SKILLS, new ArrayList<>(RoutingState.ALL_ROLES), Map.of(), tech, Set.of());
        List<String> survivors = engine.applyTechFloorGates(state);

        assertThat(survivors).doesNotContain("Android Developer");
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
}
