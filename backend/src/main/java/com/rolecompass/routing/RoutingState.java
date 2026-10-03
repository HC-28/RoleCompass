package com.rolecompass.routing;

import com.rolecompass.dto.response.PredictionResponse.EliminatedRole;
import java.util.*;

/**
 * Immutable snapshot of the current routing state for a session.
 * Passed into {@link AdaptiveRoutingEngine} to allow pure, testable FSM logic
 * without requiring a live database connection.
 */
public record RoutingState(

        /** Session identifier. */
        UUID sessionId,

        /** Current FSM state. */
        FsmState fsmState,

        /**
         * Surviving candidate role names (human-readable, matching AGENTS.md list).
         * All 10 roles on session start; shrinks as elimination gates fire.
         */
        List<String> candidateRoles,

        /**
         * 11-dimensional psychometric profile produced by FeatureAggregationService.buildPsychProfile().
         * Map key = dimension tag name (e.g. "DIM_REALISTIC"), value = normalized score [0.0, 1.0].
         * Used ONLY by the routing engine for RIASEC elimination. Never sent to ML.
         */
        Map<String, Double> psychProfile,

        /**
         * 20-dimensional technical feature vector produced by FeatureAggregationService.buildTechVector().
         * Indices match FeatureAggregationService.TECH_FEATURE_NAMES order (SERVER … FULLSPEC).
         * Used ONLY as ML service input after TERMINAL_SCORING state.
         */
        double[] techVector,

        /**
         * Total number of answers submitted so far in this session.
         */
        int answeredCount,

        /**
         * Set of question IDs already answered in this session.
         */
        Set<Long> answeredQuestionIds,

        /**
         * Raw Likert values keyed by question ID.
         * Used exclusively by the Section 2 adaptive skip rule:
         * if a domain's Q1 answer is extreme (≤ 2 or ≥ 4), Q2 is skipped.
         * Values are in [1, 5] (1=Strongly Disagree … 5=Strongly Agree).
         */
        Map<Long, Integer> rawAnswers,

        /**
         * Accumulated elimination log built up as the FSM passes through
         * PRUNE_PSYCHOMETRICS and PRUNE_TECH_SKILLS gates.
         * Entries are plain-English EliminatedRole records that will be
         * included in the final PredictionResponse.
         */
        List<EliminatedRole> eliminationLog,

        /**
         * Optional: Confidence margin (P(Top 1) - P(Top 2)) evaluated by the ML service
         * right after Section 2. If margin >= 0.18, the top role is decisive.
         * If margin < 0.18, multiple roles have similar confidence, triggering Section 3 & 4.
         */
        Double mlConfidenceMargin,

        /**
         * Top candidate roles from intermediate ML preview scoring [Top1, Top2].
         */
        List<String> mlTopRoles

) {
    /**
     * Backwards-compatible canonical constructor without intermediate ML preview fields.
     */
    public RoutingState(
            UUID sessionId,
            FsmState fsmState,
            List<String> candidateRoles,
            Map<String, Double> psychProfile,
            double[] techVector,
            int answeredCount,
            Set<Long> answeredQuestionIds,
            Map<Long, Integer> rawAnswers,
            List<EliminatedRole> eliminationLog
    ) {
        this(sessionId, fsmState, candidateRoles, psychProfile, techVector, answeredCount,
             answeredQuestionIds, rawAnswers, eliminationLog, null, null);
    }
    /** All 10 role names in canonical order (matches AGENTS.md). */
    public static final List<String> ALL_ROLES = List.of(
            "Backend Developer",
            "Frontend Developer",
            "Full Stack Developer",
            "Data Scientist",
            "Data Engineer",
            "Cybersecurity Engineer",
            "DevOps Engineer",
            "Cloud Engineer",
            "Android Developer",
            "QA / Test Automation Engineer"
    );

    /**
     * Convenience factory — initialises a fresh session state.
     * All 10 roles are candidates. Vectors are neutral. No answers yet.
     */
    public static RoutingState initialState(UUID sessionId) {
        Map<String, Double> neutralPsych = new LinkedHashMap<>();
        // All 11 psychometric dimensions default to neutral 0.5
        neutralPsych.put("DIM_REALISTIC",     0.5);
        neutralPsych.put("DIM_INVESTIGATIVE", 0.5);
        neutralPsych.put("DIM_ARTISTIC",      0.5);
        neutralPsych.put("DIM_SOCIAL",        0.5);
        neutralPsych.put("DIM_ENTERPRISING",  0.5);
        neutralPsych.put("DIM_CONVENTIONAL",  0.5);
        neutralPsych.put("DIM_DATA_IDEAS",    0.5);
        neutralPsych.put("DIM_THINGS_PEOPLE", 0.5);
        neutralPsych.put("DIM_BREADTH_DEPTH", 0.5);
        neutralPsych.put("DIM_STRUCT_AMBIG",  0.5);
        neutralPsych.put("DIM_OFFENSE_DEF",   0.5);
        double[] neutralTech = new double[20];
        Arrays.fill(neutralTech, 0.5);
        return new RoutingState(
                sessionId,
                FsmState.SECTION_1_RIASEC,
                new ArrayList<>(ALL_ROLES),
                neutralPsych,
                neutralTech,
                0,
                new HashSet<>(),
                new HashMap<>(),
                new ArrayList<>()
        );
    }

    /**
     * How many Section 1 questions have been answered by this session.
     * Used for progress reporting.
     */
    public int section1AnsweredCount() {
        return answeredCount; // placeholder — SessionService provides precise count per section
    }

    /**
     * How many Section 2 tech questions have been answered by this session.
     * The adaptive skip rule means this may be less than 40 even when Section 2 is complete.
     */
    public int section2AnsweredCount() {
        return answeredCount; // placeholder — SessionService provides precise count per section
    }

    /**
     * Returns a new RoutingState with the FSM state and candidates replaced.
     * All other fields are carried over unchanged. Used by the engine to advance
     * through transient states (PRUNE_*, RESOLVER_EVALUATION) without creating
     * a full new object graph.
     */
    public RoutingState withFsmStateAndCandidates(FsmState newFsm, List<String> newCandidates) {
        return new RoutingState(
                sessionId, newFsm, new ArrayList<>(newCandidates),
                psychProfile, techVector, answeredCount, answeredQuestionIds,
                rawAnswers, new ArrayList<>(eliminationLog), mlConfidenceMargin, mlTopRoles
        );
    }

    /**
     * Returns a new RoutingState with the provided elimination entries appended
     * to the existing log. Used by the FSM when a gate fires to accumulate
     * all elimination records across multiple gate passes.
     */
    public RoutingState withEliminationLog(List<EliminatedRole> newEntries) {
        List<EliminatedRole> merged = new ArrayList<>(eliminationLog);
        merged.addAll(newEntries);
        return new RoutingState(
                sessionId, fsmState, new ArrayList<>(candidateRoles),
                psychProfile, techVector, answeredCount, answeredQuestionIds,
                rawAnswers, merged, mlConfidenceMargin, mlTopRoles
        );
    }

    /**
     * Returns a new RoutingState with both candidate roles and elimination log updated.
     */
    public RoutingState withCandidatesAndEliminationLog(List<String> newCandidates, List<EliminatedRole> newEntries) {
        List<EliminatedRole> merged = new ArrayList<>(eliminationLog);
        merged.addAll(newEntries);
        return new RoutingState(
                sessionId, fsmState, new ArrayList<>(newCandidates),
                psychProfile, techVector, answeredCount, answeredQuestionIds,
                rawAnswers, merged, mlConfidenceMargin, mlTopRoles
        );
    }

    /**
     * Returns a new RoutingState with updated ML confidence preview margin and top roles.
     */
    public RoutingState withMlPreview(Double margin, List<String> topRoles) {
        return new RoutingState(
                sessionId, fsmState, new ArrayList<>(candidateRoles),
                psychProfile, techVector, answeredCount, answeredQuestionIds,
                rawAnswers, new ArrayList<>(eliminationLog), margin, topRoles
        );
    }
}
