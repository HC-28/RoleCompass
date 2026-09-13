package com.rolecompass.routing;

import com.rolecompass.aggregation.FeatureIndex;
import com.rolecompass.dto.response.PredictionResponse.EliminatedRole;
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

    /**
     * Maximum Section 2 technical questions (2 per domain × 20 domains).
     * Actual questions answered is typically 22–30 due to the adaptive Q2 skip rule.
     */
    public static final int SECTION_2_MAX_QUESTION_COUNT = 40;

    /** Number of questions delivered per batch to the client (Sections 1, 3, 4). */
    public static final int BATCH_SIZE = 4;

    /**
     * Section 2 delivers exactly ONE question per API call so the adaptive Q2 skip
     * fires correctly between questions rather than within a pre-packed batch.
     * After Q1 is answered: if extreme (\u2264 LOW or \u2265 HIGH) the next call returns Q1
     * of the following domain; if not extreme, Q2 of the same domain is returned next.
     */
    public static final int SECTION_2_BATCH_SIZE = 1;

    /** Safety floor: never eliminate below this many candidates. */
    public static final int MIN_CANDIDATES = 2;

    /**
     * Adaptive skip threshold for Section 2 domain Q2.
     * If a domain's Q1 Likert answer is ≤ LOW or ≥ HIGH, the domain is considered
     * resolved and Q2 is skipped.
     */
    /** Lower extreme threshold — Likert ≤ SCORE_LOW resolves a domain (Q2 skipped). */
    public static final int SCORE_LOW  = 2;   // Disagree or Strongly Disagree
    /** Upper extreme threshold — Likert ≥ SCORE_HIGH resolves a domain (Q2 skipped). */
    public static final int SCORE_HIGH = 4;   // Agree or Strongly Agree

    // Package-private aliases kept for internal use
    static final int ADAPTIVE_EXTREME_LOW  = SCORE_LOW;
    static final int ADAPTIVE_EXTREME_HIGH = SCORE_HIGH;

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

    /**
     * Section 4 gate roles — if any of these survive after Section 2/3,
     * the corresponding specialist probes are served.
     * (Data Scientist, Data Engineer, Cybersecurity Engineer, DevOps Engineer)
     */
    private static final Set<String> SECTION_4_GATE_ROLES = Set.of(
            "Data Scientist", "Data Engineer", "Cybersecurity Engineer", "DevOps Engineer"
    );

    private final QuestionRepository questionRepository;

    // ─── Gate Result (survivors + elimination log from one gate pass) ─────────

    /**
     * Pairs the list of surviving roles with the elimination records generated
     * during one gate evaluation. Allows callers to accumulate the log across
     * multiple gate passes without losing any elimination entries.
     */
    public record GateResult(List<String> survivors, List<EliminatedRole> log) {}

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
                GateResult gr = applyPsychometricGates(state);
                log.info("PRUNE_PSYCHOMETRICS: {} → {} candidates: {}",
                        state.candidateRoles().size(), gr.survivors().size(), gr.survivors());
                // Immediately advance to Section 2 (technical core)
                yield continueWithNextState(
                        state.withEliminationLog(gr.log()),
                        FsmState.SECTION_2_TECH_CORE, gr.survivors());
            }

            case SECTION_2_TECH_CORE -> handleSection2(state);

            case PRUNE_TECH_SKILLS -> {
                GateResult gr = applyTechFloorGates(state);
                log.info("PRUNE_TECH_SKILLS: {} → {} candidates: {}",
                        state.candidateRoles().size(), gr.survivors().size(), gr.survivors());
                yield continueWithNextState(
                        state.withEliminationLog(gr.log()),
                        FsmState.RESOLVER_EVALUATION, gr.survivors());
            }

            case RESOLVER_EVALUATION -> handleResolverEvaluation(state);

            case SECTION_3_RESOLVER -> handleSection3(state);

            case SECTION_4_SPECIALIST -> handleSection4(state);

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
            log.info("Section 1 complete for session {}. Advancing to PRUNE_PSYCHOMETRICS.", state.sessionId());
            return continueWithNextState(state, FsmState.PRUNE_PSYCHOMETRICS, state.candidateRoles());
        }
        List<Long> batch = unanswered.stream().limit(BATCH_SIZE).collect(Collectors.toList());
        return RoutingDecision.continueWith(
                state.candidateRoles(), batch, FsmState.SECTION_1_RIASEC,
                String.format("Section 1 in progress: %d/%d answered", state.section1AnsweredCount(), SECTION_1_QUESTION_COUNT));
    }

    /**
     * Section 2 handler with adaptive Q2 skip rule.
     *
     * <p>For each of the 20 technical domains, questions are ordered by ID ascending.
     * The first question per domain (Q1) is always served. The second question (Q2)
     * is skipped if Q1's Likert answer was extreme: ≤ {@value #ADAPTIVE_EXTREME_LOW}
     * (strong disagreement) or ≥ {@value #ADAPTIVE_EXTREME_HIGH} (strong agreement).
     * Both extremes mean the domain signal is already clear; Q2 would only add noise.</p>
     */
    private RoutingDecision handleSection2(RoutingState state) {
        // Load all Section 2 questions ordered by ID (Q1 always < Q2 for each domain)
        List<Question> allSection2 = questionRepository.findBySectionIdOrderByIdAsc(2);

        // Group by primary dimension tag: each domain has exactly one unique tech tag
        Map<String, List<Question>> byDomain = new java.util.LinkedHashMap<>();
        for (Question q : allSection2) {
            String primaryTag = (q.getDimensionTags() != null && q.getDimensionTags().length > 0)
                    ? q.getDimensionTags()[0]
                    : ("UNTAGGED_" + q.getId());
            byDomain.computeIfAbsent(primaryTag, k -> new ArrayList<>()).add(q);
        }

        List<Long> toServe = new ArrayList<>();
        Map<Long, Integer> rawAnswers = state.rawAnswers();

        for (Map.Entry<String, List<Question>> entry : byDomain.entrySet()) {
            List<Question> domainQs = entry.getValue(); // ordered: Q1 first, Q2 second
            for (int i = 0; i < domainQs.size(); i++) {
                Question q = domainQs.get(i);
                if (state.answeredQuestionIds().contains(q.getId())) continue; // already answered

                if (i == 1) {
                    // This is Q2 — check if Q1 was extreme
                    Question q1 = domainQs.get(0);
                    Integer q1Answer = rawAnswers.get(q1.getId());
                    if (q1Answer != null
                            && (q1Answer <= ADAPTIVE_EXTREME_LOW || q1Answer >= ADAPTIVE_EXTREME_HIGH)) {
                        // Domain resolved by Q1 extremity — skip Q2
                        log.debug("Adaptive skip: domain={} Q1={} is extreme, skipping Q2 id={}",
                                entry.getKey(), q1Answer, q.getId());
                        continue;
                    }
                }
                toServe.add(q.getId());
            }
        }

        if (toServe.isEmpty()) {
            log.info("Section 2 complete for session {}. Advancing to PRUNE_TECH_SKILLS.", state.sessionId());
            return continueWithNextState(state, FsmState.PRUNE_TECH_SKILLS, state.candidateRoles());
        }

        List<Long> batch = toServe.stream().limit(SECTION_2_BATCH_SIZE).collect(Collectors.toList());
        return RoutingDecision.continueWith(
                state.candidateRoles(), batch, FsmState.SECTION_2_TECH_CORE,
                String.format("Section 2 in progress: %d answered, %d remaining",
                        state.section2AnsweredCount(), toServe.size()));
    }

    private RoutingDecision handleResolverEvaluation(RoutingState state) {
        List<Long> applicableResolvers = getApplicableResolverQuestions(state);

        if (!applicableResolvers.isEmpty()) {
            log.info("Tie detected for session {}. Advancing to SECTION_3_RESOLVER with {} questions.",
                    state.sessionId(), applicableResolvers.size());
            List<Long> batch = applicableResolvers.stream().limit(BATCH_SIZE).collect(Collectors.toList());
            return RoutingDecision.continueWith(
                    state.candidateRoles(), batch, FsmState.SECTION_3_RESOLVER,
                    "Tie exists between candidates. Serving resolver questions.");
        }

        // No tie — check if Section 4 specialist gate opens
        return advanceFromResolver(state);
    }

    private RoutingDecision handleSection3(RoutingState state) {
        List<Long> unanswered = getApplicableResolverQuestions(state);
        if (unanswered.isEmpty()) {
            log.info("Section 3 resolver complete for session {}.", state.sessionId());
            return advanceFromResolver(state);
        }
        List<Long> batch = unanswered.stream().limit(BATCH_SIZE).collect(Collectors.toList());
        return RoutingDecision.continueWith(
                state.candidateRoles(), batch, FsmState.SECTION_3_RESOLVER,
                "Section 3 resolver in progress");
    }

    private RoutingDecision handleSection4(RoutingState state) {
        // Only serve specialist probe questions applicable to surviving candidates
        Set<String> candidates = new HashSet<>(state.candidateRoles());
        List<Long> unanswered = questionRepository.findBySectionIdOrderByIdAsc(4).stream()
                .filter(q -> !state.answeredQuestionIds().contains(q.getId()))
                .filter(q -> isTriggerSatisfied(q.getTriggerPredicate(), candidates))
                .map(Question::getId)
                .collect(Collectors.toList());
        if (unanswered.isEmpty()) {
            log.info("Section 4 specialist probes complete for session {}. Advancing to TERMINAL_SCORING.", state.sessionId());
            return RoutingDecision.readyToPredict(state.candidateRoles(), FsmState.TERMINAL_SCORING);
        }
        List<Long> batch = unanswered.stream().limit(BATCH_SIZE).collect(Collectors.toList());
        return RoutingDecision.continueWith(
                state.candidateRoles(), batch, FsmState.SECTION_4_SPECIALIST,
                "Section 4 specialist probes in progress");
    }

    private RoutingDecision advanceFromResolver(RoutingState state) {
        boolean section4Opens = state.candidateRoles().stream()
                .anyMatch(SECTION_4_GATE_ROLES::contains);

        if (section4Opens) {
            log.info("Section 4 specialist gate open for session {} — specialist roles survived: {}",
                    state.sessionId(),
                    state.candidateRoles().stream().filter(SECTION_4_GATE_ROLES::contains).collect(Collectors.toList()));
            return continueWithNextState(state, FsmState.SECTION_4_SPECIALIST, state.candidateRoles());
        }

        log.info("No specialist roles in candidate set for session {}. Advancing to TERMINAL_SCORING.", state.sessionId());
        return RoutingDecision.readyToPredict(state.candidateRoles(), FsmState.TERMINAL_SCORING);
    }

    // ─── Psychometric Elimination Gates ──────────────────────────────────────

    /**
     * Applies O*NET-grounded RIASEC threshold elimination using 11 psychometric dimensions.
     * Returns a {@link GateResult} containing survivors and plain-English elimination records.
     */
    GateResult applyPsychometricGates(RoutingState state) {
        Map<String, Double> psych = state.psychProfile();
        double A = psych.getOrDefault(DIM_A, 0.5);
        double I = psych.getOrDefault(DIM_I, 0.5);
        double R = psych.getOrDefault(DIM_R, 0.5);
        double C = psych.getOrDefault(DIM_C, 0.5);

        Map<String, String> eliminationReasons = new LinkedHashMap<>();

        // Artistic Gate: low artistic preference → no creative/visual design drive → eliminate Frontend Developer
        if (A < THRESHOLD_A_LOW) {
            eliminationReasons.put("Frontend Developer",
                "Your answers indicate you prefer structured, logical tasks over creative design work. "
              + "Frontend development centres on crafting visual interfaces and user experiences, "
              + "which calls for a strong appreciation of aesthetics and creative expression.");
            log.debug("Artistic gate: A={} < {} → eliminated Frontend Developer", A, THRESHOLD_A_LOW);
        }

        // Investigative Gate: low analytical research drive → eliminate Data Scientist
        if (I < THRESHOLD_I_LOW) {
            eliminationReasons.put("Data Scientist",
                "Your answers suggest you prefer building practical systems over deep analytical research. "
              + "Data Science demands a strong drive to investigate complex questions, "
              + "run statistical experiments, and derive insights from data — even without a clear outcome in advance.");
            log.debug("Investigative gate: I={} < {} → eliminated Data Scientist", I, THRESHOLD_I_LOW);
        }

        // Realistic Gate: low hands-on / systems-building drive → eliminate DevOps, Cloud
        if (R < THRESHOLD_R_LOW) {
            String reason = "Your answers suggest you lean toward abstract or interpersonal work "
                          + "rather than hands-on infrastructure configuration. "
                          + "This role involves setting up, configuring, and managing servers, "
                          + "networks, and deployment pipelines — work that rewards a practical, "
                          + "build-and-fix mindset.";
            eliminationReasons.put("DevOps Engineer", reason);
            eliminationReasons.put("Cloud Engineer", reason);
            log.debug("Realistic gate: R={} < {} → eliminated DevOps/Cloud", R, THRESHOLD_R_LOW);
        }

        // Anti-Artistic Gate: high creative preference → incompatible with structured data / security work
        if (A > THRESHOLD_A_HIGH) {
            eliminationReasons.put("Data Engineer",
                "Your answers indicate a strong creative and expressive preference. "
              + "Data Engineering is a highly structured discipline focused on building "
              + "reliable data pipelines, enforcing schemas, and ensuring data quality — "
              + "tasks that demand precision and consistency over creativity.");
            eliminationReasons.put("Cybersecurity Engineer",
                "Your answers indicate a strong creative preference. "
              + "Cybersecurity Engineering requires a systematic, rule-driven mindset "
              + "for auditing systems, applying security controls, and following compliance frameworks — "
              + "which typically suits someone who values structure and thoroughness.");
            log.debug("Anti-Artistic gate: A={} > {} → eliminated DataEng/Cyber", A, THRESHOLD_A_HIGH);
        }

        // Conventional Gate: low preference for orderly / procedural work → eliminate Data Engineer, QA
        if (C < THRESHOLD_C_LOW) {
            eliminationReasons.merge("Data Engineer",
                "Your answers suggest you prefer open-ended or creative tasks over strict, "
              + "procedural work. Data Engineering involves rigorous schema design, "
              + "pipeline standardisation, and data governance — disciplines that reward "
              + "people who enjoy well-defined rules and repeatable processes.",
                (existing, extra) -> existing); // keep first reason if already eliminated
            eliminationReasons.put("QA / Test Automation Engineer",
                "Your answers suggest you prefer flexibility and open-ended exploration over "
              + "methodical, process-driven work. QA Engineering is built on systematic test design, "
              + "strict verification procedures, and rigorous documentation — "
              + "tasks that suit someone who values consistency and attention to detail.");
            log.debug("Conventional gate: C={} < {} → eliminated DataEng/QA", C, THRESHOLD_C_LOW);
        }

        List<String> survivors = state.candidateRoles().stream()
                .filter(r -> !eliminationReasons.containsKey(r))
                .collect(Collectors.toList());
        List<String> finalSurvivors = enforceSafetyFloor(state.candidateRoles(), survivors);

        // Build EliminatedRole records only for roles that truly survived elimination
        // (safety floor may have restored some — don't log restored roles as eliminated)
        Set<String> actuallyEliminated = new HashSet<>(eliminationReasons.keySet());
        finalSurvivors.forEach(actuallyEliminated::remove);

        List<EliminatedRole> log = eliminationReasons.entrySet().stream()
                .filter(e -> actuallyEliminated.contains(e.getKey()))
                .map(e -> EliminatedRole.builder()
                        .role(e.getKey())
                        .stage("PSYCHOMETRIC")
                        .reason(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return new GateResult(finalSurvivors, log);
    }

    // ─── Tech Floor Elimination Gates ─────────────────────────────────────────

    /**
     * Applies technical skill floor rules after Section 3 answers are collected.
     * Returns a {@link GateResult} containing survivors and plain-English elimination records.
     */
    GateResult applyTechFloorGates(RoutingState state) {
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

        Map<String, String> eliminationReasons = new LinkedHashMap<>();

        if (mobile < THRESHOLD_MOBILE) {
            eliminationReasons.put("Android Developer",
                "Your answers showed little interest in mobile application development. "
              + "Android development requires a genuine enthusiasm for building apps that "
              + "run on smartphones — including working with touch interfaces, device sensors, "
              + "and mobile-specific performance constraints.");
            log.debug("Mobile floor: MOBILE={} < {} → eliminated Android Developer", mobile, THRESHOLD_MOBILE);
        }

        if (stats < THRESHOLD_STATS && model < THRESHOLD_MODEL) {
            eliminationReasons.put("Data Scientist",
                "Your answers showed limited enthusiasm for both statistical analysis "
              + "and machine learning. Data Science requires genuine interest in "
              + "working with numbers, designing experiments, and building predictive models "
              + "from data — not just using data as a byproduct of other work.");
            log.debug("Stats+Model floor: STATS={} MODEL={} → eliminated Data Scientist", stats, model);
        }

        if (threat < THRESHOLD_THREAT && hardening < THRESHOLD_HARDENING) {
            eliminationReasons.put("Cybersecurity Engineer",
                "Your answers showed limited interest in both threat analysis and system hardening. "
              + "Cybersecurity Engineering requires a genuine drive to think like an attacker — "
              + "finding vulnerabilities — and equal discipline in applying controls to close those gaps. "
              + "Both mindsets are essential for effective security work.");
            log.debug("Security floor: THREAT={} HARDENING={} → eliminated Cybersecurity Engineer", threat, hardening);
        }

        List<String> survivors = state.candidateRoles().stream()
                .filter(r -> !eliminationReasons.containsKey(r))
                .collect(Collectors.toList());
        List<String> finalSurvivors = enforceSafetyFloor(state.candidateRoles(), survivors);

        Set<String> actuallyEliminated = new HashSet<>(eliminationReasons.keySet());
        finalSurvivors.forEach(actuallyEliminated::remove);

        List<EliminatedRole> log = eliminationReasons.entrySet().stream()
                .filter(e -> actuallyEliminated.contains(e.getKey()))
                .map(e -> EliminatedRole.builder()
                        .role(e.getKey())
                        .stage("TECHNICAL")
                        .reason(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return new GateResult(finalSurvivors, log);
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
    /**
     * Returns Section 3 resolver question IDs whose requires_both predicate is satisfied
     * by the current candidate set, and which have not yet been answered.
     */
    List<Long> getApplicableResolverQuestions(RoutingState state) {
        Set<String> candidates = new HashSet<>(state.candidateRoles());
        return questionRepository.findBySectionIdOrderByIdAsc(3).stream()
                .filter(q -> !state.answeredQuestionIds().contains(q.getId()))
                .filter(q -> isTriggerSatisfied(q.getTriggerPredicate(), candidates))
                .map(Question::getId)
                .collect(Collectors.toList());
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
