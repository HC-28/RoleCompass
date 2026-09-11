package com.rolecompass.routing;

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
        Set<Long> answeredQuestionIds
) {
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
                new HashSet<>()
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
     * How many Section 3 questions have been answered by this session.
     */
    public int section3AnsweredCount() {
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
                psychProfile, techVector, answeredCount, answeredQuestionIds
        );
    }
}
