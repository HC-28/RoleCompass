package com.rolecompass.routing;

import com.rolecompass.aggregation.FeatureIndex;
import com.rolecompass.entity.Question;
import com.rolecompass.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AdaptiveRoutingEngine — Finite State Machine controlling assessment flow.
 *
 * <p><b>Architecture contract (AGENTS.md, non-negotiable):</b></p>
 * <ul>
 *   <li>Uses 11 psychometric dimensions for candidate elimination.</li>
 *   <li>Does NOT call the ML model. Does NOT make final predictions.</li>
 *   <li>Does NOT contain any React/HTTP/presentation logic.</li>
 *   <li>Returns {@link RoutingDecision}: next question IDs or readyToPredict=true.</li>
 * </ul>
 *
 * <p><b>FSM transition order:</b></p>
 * <pre>
 * SECTION_1_RIASEC
 *   → PRUNE_PSYCHOMETRICS   (11-dim threshold gates applied)
 *   → SECTION_3_TECH_CORE
 *   → PRUNE_TECH_SKILLS     (tech floor gates applied)
 *   → RESOLVER_EVALUATION   (tie check)
 *   → SECTION_2_RESOLVER    (conditional — if tie exists)
 *   → SECTION_4_GATED       (conditional — if data/backend role survived)
 *   → TERMINAL_SCORING
 *   → COMPLETED
 * </pre>
 *
 * <p><b>Elimination rules at PRUNE_PSYCHOMETRICS</b> (O*NET DB 31.0 thresholds):</p>
 * <ul>
 *   <li>Artistic Gate: A &lt; 0.35 → eliminate Frontend Developer</li>
 *   <li>Investigative Gate: I &lt; 0.60 → eliminate Data Scientist</li>
 *   <li>Realistic Gate: R &lt; 0.40 → eliminate DevOps Engineer, Cloud Engineer</li>
 *   <li>Anti-Artistic Gate: A &gt; 0.55 → eliminate Data Engineer, Cybersecurity Engineer</li>
 *   <li>Conventional Gate: C &lt; 0.65 → eliminate Data Engineer, QA / Test Automation Engineer</li>
 * </ul>
 *
 * <p><b>Elimination rules at PRUNE_TECH_SKILLS:</b></p>
 * <ul>
 *   <li>Mobile floor: MOBILE &lt; 0.40 → eliminate Android Developer</li>
 *   <li>Stats+Model floor: STATS &lt; 0.50 AND MODEL &lt; 0.50 → eliminate Data Scientist</li>
 *   <li>Security floor: THREAT &lt; 0.40 AND HARDENING &lt; 0.40 → eliminate Cybersecurity Engineer</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveRoutingEngine {

    // ─── Constants ────────────────────────────────────────────────────────────

    /** Total Section 1 questions every user must complete before psychometric pruning. */
    public static final int SECTION_1_QUESTION_COUNT = 16;

    /** Total Section 3 questions every user must complete before tech pruning. */
    public static final int SECTION_3_QUESTION_COUNT = 20;

    /** Number of questions delivered per batch to the client. */
    public static final int BATCH_SIZE = 4;

    /** Safety floor: never eliminate below this many candidates. */
    public static final int MIN_CANDIDATES = 2;

    // Psychometric dimension tag names (must match FeatureIndex constants)
    private static final String DIM_R  = FeatureIndex.TAG_DIM_REALISTIC;
    private static final String DIM_I  = FeatureIndex.TAG_DIM_INVESTIGATIVE;
    private static final String DIM_A  = FeatureIndex.TAG_DIM_ARTISTIC;
    private static final String DIM_C  = FeatureIndex.TAG_DIM_CONVENTIONAL;
    private static final String DIM_BD = FeatureIndex.TAG_DIM_BREADTH_DEPTH;

    // ONET O*NET elimination thresholds
    private static final double THRESHOLD_A_LOW    = 0.35;  // Artistic gate (eliminate Frontend if below)
    private static final double THRESHOLD_I_LOW    = 0.60;  // Investigative gate (eliminate Data Scientist if below)
    private static final double THRESHOLD_R_LOW    = 0.40;  // Realistic gate (eliminate DevOps/Cloud if below)
    private static final double THRESHOLD_A_HIGH   = 0.55;  // Anti-Artistic gate (eliminate DataEngineer/Cyber if above)
    private static final double THRESHOLD_C_LOW    = 0.65;  // Conventional gate (eliminate DataEngineer/QA if below)

    // Tech floor thresholds
    private static final double THRESHOLD_MOBILE   = 0.40;
    private static final double THRESHOLD_STATS    = 0.50;
    private static final double THRESHOLD_MODEL    = 0.50;
    private static final double THRESHOLD_THREAT   = 0.40;
    private static final double THRESHOLD_HARDENING = 0.40;

    // Section 4 gate roles (trigger Section 4 if any of these survive)
    private static final Set<String> SECTION_4_GATE_ROLES = Set.of(
            "Data Engineer", "Backend Developer", "Data Scientist"
    );

    private final QuestionRepository questionRepository;

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Evaluates the current FSM state and returns the next routing decision.
     *
     * @param state  current session snapshot (fsm state, answers, candidate set, psych profile, tech vector)
     * @return immutable {@link RoutingDecision}
     */
    public RoutingDecision evaluate(RoutingState state) {
        FsmState fsm = state.fsmState();
        log.debug("evaluate(): session={} fsm={} candidates={} answered={}",
                state.sessionId(), fsm, state.candidateRoles(), state.answeredCount());

        return switch (fsm) {

            case SECTION_1_RIASEC -> handleSection1(state);

            case PRUNE_PSYCHOMETRICS -> {
                List<String> survived = applyPsychometricGates(state);
                log.info("PRUNE_PSYCHOMETRICS: {} → {} candidates: {}",
                        state.candidateRoles().size(), survived.size(), survived);
                // Immediately advance to Section 3
                yield continueWithNextState(state, FsmState.SECTION_3_TECH_CORE, survived);
            }

            case SECTION_3_TECH_CORE -> handleSection3(state);

            case PRUNE_TECH_SKILLS -> {
                List<String> survived = applyTechFloorGates(state);
                log.info("PRUNE_TECH_SKILLS: {} → {} candidates: {}",
                        state.candidateRoles().size(), survived.size(), survived);
                yield continueWithNextState(state, FsmState.RESOLVER_EVALUATION, survived);
            }

            case RESOLVER_EVALUATION -> handleResolverEvaluation(state);

            case SECTION_2_RESOLVER -> handleSection2(state);

            case SECTION_4_GATED -> handleSection4(state);

            case TERMINAL_SCORING -> RoutingDecision.readyToPredict(
                    state.candidateRoles(), FsmState.COMPLETED);

            case COMPLETED -> {
                log.warn("evaluate() called on COMPLETED session {}", state.sessionId());
                yield RoutingDecision.readyToPredict(state.candidateRoles(), FsmState.COMPLETED);
            }
        };
    }

    // ─── FSM State Handlers ───────────────────────────────────────────────────

    private RoutingDecision handleSection1(RoutingState state) {
        List<Long> unanswered = getUnansweredInSection(1, state);
        if (unanswered.isEmpty()) {
            // All Section 1 questions answered — advance to psychometric pruning
            log.info("Section 1 complete for session {}. Advancing to PRUNE_PSYCHOMETRICS.", state.sessionId());
            return continueWithNextState(state, FsmState.PRUNE_PSYCHOMETRICS, state.candidateRoles());
        }
        List<Long> batch = unanswered.stream().limit(BATCH_SIZE).collect(Collectors.toList());
        return RoutingDecision.continueWith(
                state.candidateRoles(), batch, FsmState.SECTION_1_RIASEC,
                String.format("Section 1 in progress: %d/%d answered", state.section1AnsweredCount(), SECTION_1_QUESTION_COUNT));
    }

    private RoutingDecision handleSection3(RoutingState state) {
        List<Long> unanswered = getUnansweredInSection(3, state);
        if (unanswered.isEmpty()) {
            log.info("Section 3 complete for session {}. Advancing to PRUNE_TECH_SKILLS.", state.sessionId());
            return continueWithNextState(state, FsmState.PRUNE_TECH_SKILLS, state.candidateRoles());
        }
        List<Long> batch = unanswered.stream().limit(BATCH_SIZE).collect(Collectors.toList());
        return RoutingDecision.continueWith(
                state.candidateRoles(), batch, FsmState.SECTION_3_TECH_CORE,
                String.format("Section 3 in progress: %d/%d answered", state.section3AnsweredCount(), SECTION_3_QUESTION_COUNT));
    }

    private RoutingDecision handleResolverEvaluation(RoutingState state) {
        // Check if any Section 2 resolver questions are applicable given current candidates
        List<Long> applicableResolvers = getApplicableResolverQuestions(state);

        if (!applicableResolvers.isEmpty()) {
            log.info("Tie detected for session {}. Advancing to SECTION_2_RESOLVER with {} questions.",
                    state.sessionId(), applicableResolvers.size());
            List<Long> batch = applicableResolvers.stream().limit(BATCH_SIZE).collect(Collectors.toList());
            return RoutingDecision.continueWith(
                    state.candidateRoles(), batch, FsmState.SECTION_2_RESOLVER,
                    "Tie exists between candidates. Serving resolver questions.");
        }

        // No tie — check if Section 4 gate opens
        return advanceFromResolver(state);
    }

    private RoutingDecision handleSection2(RoutingState state) {
        List<Long> unanswered = getUnansweredApplicableResolverQuestions(state);
        if (unanswered.isEmpty()) {
            log.info("Section 2 resolver complete for session {}.", state.sessionId());
            return advanceFromResolver(state);
        }
        List<Long> batch = unanswered.stream().limit(BATCH_SIZE).collect(Collectors.toList());
        return RoutingDecision.continueWith(
                state.candidateRoles(), batch, FsmState.SECTION_2_RESOLVER,
                "Section 2 resolver in progress");
    }

    private RoutingDecision handleSection4(RoutingState state) {
        List<Long> unanswered = getUnansweredInSection(4, state);
        if (unanswered.isEmpty()) {
            log.info("Section 4 complete for session {}. Advancing to TERMINAL_SCORING.", state.sessionId());
            return RoutingDecision.readyToPredict(state.candidateRoles(), FsmState.TERMINAL_SCORING);
        }
        List<Long> batch = unanswered.stream().limit(BATCH_SIZE).collect(Collectors.toList());
        return RoutingDecision.continueWith(
                state.candidateRoles(), batch, FsmState.SECTION_4_GATED,
                "Section 4 (gated) in progress");
    }

    private RoutingDecision advanceFromResolver(RoutingState state) {
        boolean section4Opens = state.candidateRoles().stream()
                .anyMatch(SECTION_4_GATE_ROLES::contains);

        if (section4Opens) {
            log.info("Section 4 gate open for session {} — candidates include data/backend roles.", state.sessionId());
            return continueWithNextState(state, FsmState.SECTION_4_GATED, state.candidateRoles());
        }

        log.info("No gate roles in candidate set for session {}. Advancing to TERMINAL_SCORING.", state.sessionId());
        return RoutingDecision.readyToPredict(state.candidateRoles(), FsmState.TERMINAL_SCORING);
    }

    // ─── Psychometric Elimination Gates ──────────────────────────────────────

    /**
     * Applies O*NET-grounded RIASEC threshold elimination using 11 psychometric dimensions.
     * Returns the surviving candidate roles. Enforces MIN_CANDIDATES safety floor.
     */
    List<String> applyPsychometricGates(RoutingState state) {
        Map<String, Double> psych = state.psychProfile();
        double A = psych.getOrDefault(DIM_A, 0.5);
        double I = psych.getOrDefault(DIM_I, 0.5);
        double R = psych.getOrDefault(DIM_R, 0.5);
        double C = psych.getOrDefault(DIM_C, 0.5);

        Set<String> eliminated = new HashSet<>();

        // Artistic Gate: low artistic → eliminate Frontend Developer
        if (A < THRESHOLD_A_LOW) {
            eliminated.add("Frontend Developer");
            log.debug("Artistic gate: A={:.3f} < {:.3f} → eliminated Frontend Developer", A, THRESHOLD_A_LOW);
        }

        // Investigative Gate: low investigative → eliminate Data Scientist
        if (I < THRESHOLD_I_LOW) {
            eliminated.add("Data Scientist");
            log.debug("Investigative gate: I={:.3f} < {:.3f} → eliminated Data Scientist", I, THRESHOLD_I_LOW);
        }

        // Realistic Gate: low realistic → eliminate DevOps Engineer, Cloud Engineer
        if (R < THRESHOLD_R_LOW) {
            eliminated.add("DevOps Engineer");
            eliminated.add("Cloud Engineer");
            log.debug("Realistic gate: R={:.3f} < {:.3f} → eliminated DevOps/Cloud", R, THRESHOLD_R_LOW);
        }

        // Anti-Artistic Gate: high artistic → eliminate Data Engineer, Cybersecurity Engineer
        if (A > THRESHOLD_A_HIGH) {
            eliminated.add("Data Engineer");
            eliminated.add("Cybersecurity Engineer");
            log.debug("Anti-Artistic gate: A={:.3f} > {:.3f} → eliminated DataEng/Cyber", A, THRESHOLD_A_HIGH);
        }

        // Conventional Gate: low conventional → eliminate Data Engineer, QA / Test Automation Engineer
        if (C < THRESHOLD_C_LOW) {
            eliminated.add("Data Engineer");
            eliminated.add("QA / Test Automation Engineer");
            log.debug("Conventional gate: C={:.3f} < {:.3f} → eliminated DataEng/QA", C, THRESHOLD_C_LOW);
        }

        List<String> survivors = state.candidateRoles().stream()
                .filter(r -> !eliminated.contains(r))
                .collect(Collectors.toList());

        return enforceSafetyFloor(state.candidateRoles(), survivors);
    }

    // ─── Tech Floor Elimination Gates ─────────────────────────────────────────

    /**
     * Applies technical skill floor rules after Section 3 answers are collected.
     * Returns the surviving candidate roles. Enforces MIN_CANDIDATES safety floor.
     */
    List<String> applyTechFloorGates(RoutingState state) {
        double[] tech = state.techVector();
        // Tech vector indices match FeatureAggregationService.TECH_FEATURE_NAMES order:
        // 0=SERVER, 1=STORAGE, 2=API, 3=UI, 4=STATE, 5=BUILD, 6=INFRA, 7=CONTAINER,
        // 8=CLOUD, 9=STATS, 10=MODEL, 11=PIPELINE, 12=MOBILE, 13=THREAT, 14=HARDENING,
        // 15=TESTDES, 16=TESTAUTO, 17=OBSERV, 18=PERF, 19=FULLSPEC
        double mobile    = tech[12];
        double stats     = tech[9];
        double model     = tech[10];
        double threat    = tech[13];
        double hardening = tech[14];

        Set<String> eliminated = new HashSet<>();

        if (mobile < THRESHOLD_MOBILE) {
            eliminated.add("Android Developer");
            log.debug("Mobile floor: MOBILE={:.3f} < {:.3f} → eliminated Android Developer", mobile, THRESHOLD_MOBILE);
        }

        if (stats < THRESHOLD_STATS && model < THRESHOLD_MODEL) {
            eliminated.add("Data Scientist");
            log.debug("Stats+Model floor: STATS={:.3f} MODEL={:.3f} → eliminated Data Scientist", stats, model);
        }

        if (threat < THRESHOLD_THREAT && hardening < THRESHOLD_HARDENING) {
            eliminated.add("Cybersecurity Engineer");
            log.debug("Security floor: THREAT={:.3f} HARDENING={:.3f} → eliminated Cybersecurity Engineer", threat, hardening);
        }

        List<String> survivors = state.candidateRoles().stream()
                .filter(r -> !eliminated.contains(r))
                .collect(Collectors.toList());

        return enforceSafetyFloor(state.candidateRoles(), survivors);
    }

    // ─── Question Selection Helpers ───────────────────────────────────────────

    private List<Long> getUnansweredInSection(int sectionId, RoutingState state) {
        return questionRepository.findBySectionIdOrderByIdAsc(sectionId).stream()
                .filter(q -> !state.answeredQuestionIds().contains(q.getId()))
                .map(Question::getId)
                .collect(Collectors.toList());
    }

    /**
     * Returns Section 2 resolver question IDs whose trigger_predicate is satisfied
     * by the current candidate set, and which have not yet been answered.
     */
    List<Long> getApplicableResolverQuestions(RoutingState state) {
        Set<String> candidates = new HashSet<>(state.candidateRoles());
        return questionRepository.findBySectionIdOrderByIdAsc(2).stream()
                .filter(q -> !state.answeredQuestionIds().contains(q.getId()))
                .filter(q -> isTriggerSatisfied(q.getTriggerPredicate(), candidates))
                .map(Question::getId)
                .collect(Collectors.toList());
    }

    private List<Long> getUnansweredApplicableResolverQuestions(RoutingState state) {
        return getApplicableResolverQuestions(state);
    }

    /**
     * Evaluates a trigger_predicate JSON string against the current candidate set.
     * Supported predicate formats:
     * <ul>
     *   <li>{@code {"always": true}} — always trigger</li>
     *   <li>{@code {"requires_both": ["Role A", "Role B"]}} — trigger iff both roles are in candidate set</li>
     *   <li>{@code {"requires_any": ["Role A", "Role B", ...]}} — trigger iff at least one role is in candidate set</li>
     * </ul>
     */
    boolean isTriggerSatisfied(String triggerPredicate, Set<String> candidates) {
        if (triggerPredicate == null || triggerPredicate.isBlank()) return false;
        String pred = triggerPredicate.trim();

        if (pred.contains("\"always\"")) return true;

        if (pred.contains("\"requires_both\"")) {
            // Extract the two role names from the JSON array
            List<String> roles = extractRoleNamesFromPredicate(pred);
            return roles.size() >= 2 && candidates.containsAll(roles);
        }

        if (pred.contains("\"requires_any\"")) {
            List<String> roles = extractRoleNamesFromPredicate(pred);
            return roles.stream().anyMatch(candidates::contains);
        }

        log.warn("Unrecognized trigger_predicate format: {}", pred);
        return false;
    }

    /**
     * Extracts quoted string values from a JSONB predicate like:
     * {@code {"requires_both": ["Role A", "Role B"]}}
     * Returns only the role name strings (skips the predicate key).
     */
    private List<String> extractRoleNamesFromPredicate(String predicate) {
        // Simple extraction without JSON library — find all quoted strings after the first colon+bracket
        List<String> values = new ArrayList<>();
        int arrayStart = predicate.indexOf('[');
        if (arrayStart < 0) return values;
        String arrayPart = predicate.substring(arrayStart);
        // Find all "..." inside the array
        int pos = 0;
        while (pos < arrayPart.length()) {
            int open = arrayPart.indexOf('"', pos);
            if (open < 0) break;
            int close = arrayPart.indexOf('"', open + 1);
            if (close < 0) break;
            values.add(arrayPart.substring(open + 1, close));
            pos = close + 1;
        }
        return values;
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    /**
     * Ensures that candidates never drop below MIN_CANDIDATES.
     * If elimination caused too many removals, restores from the original set
     * until the floor is met.
     */
    private List<String> enforceSafetyFloor(List<String> original, List<String> survivors) {
        if (survivors.size() >= MIN_CANDIDATES) return survivors;
        log.warn("Safety floor triggered: survivors={} < MIN={}. Restoring from original set.", survivors.size(), MIN_CANDIDATES);
        List<String> padded = new ArrayList<>(survivors);
        for (String role : original) {
            if (padded.size() >= MIN_CANDIDATES) break;
            if (!padded.contains(role)) padded.add(role);
        }
        return padded;
    }

    /**
     * Helper to produce a continueWith RoutingDecision that immediately handles
     * transient states (PRUNE_*, RESOLVER_EVALUATION) by calling evaluate() recursively
     * with the new state — so transient states never stall awaiting user input.
     */
    private RoutingDecision continueWithNextState(
            RoutingState state, FsmState nextFsm, List<String> candidates) {

        // Re-evaluate immediately with the advanced FSM state
        RoutingState advanced = state.withFsmStateAndCandidates(nextFsm, candidates);
        return evaluate(advanced);
    }
}
