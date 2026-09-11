package com.rolecompass.routing;

/**
 * FSM states for the adaptive assessment session.
 *
 * <p>Canonical transition order:</p>
 * <pre>
 * SECTION_1_RIASEC
 *   → PRUNE_PSYCHOMETRICS    (routing engine applies 11-dim threshold elimination)
 *   → SECTION_3_TECH_CORE
 *   → PRUNE_TECH_SKILLS      (routing engine applies tech floor rules)
 *   → RESOLVER_EVALUATION    (conditional — fires if ≥2 roles tied in candidate set)
 *   → SECTION_2_RESOLVER     (conditional — asks resolver questions when a tie exists)
 *   → SECTION_4_GATED        (conditional — fires if Data Engineer / Backend / Data Scientist survived)
 *   → TERMINAL_SCORING       (sends 20-dim tech vector + candidate_roles to ML service)
 *   → COMPLETED
 * </pre>
 */
public enum FsmState {

    /** User is answering the 16 Section 1 psychometric/RIASEC questions. */
    SECTION_1_RIASEC,

    /**
     * All Section 1 answers collected. Engine applies 11-dim psychometric
     * threshold gates and eliminates incompatible roles.
     * This is a transient computation state — not delivered to the user.
     */
    PRUNE_PSYCHOMETRICS,

    /** User is answering the 20 Section 3 core technical skill questions. */
    SECTION_3_TECH_CORE,

    /**
     * All Section 3 answers collected. Engine applies tech floor rules
     * (MOBILE floor, STATS+MODEL floor, THREAT+HARDENING floor).
     * Transient computation state.
     */
    PRUNE_TECH_SKILLS,

    /**
     * Engine evaluates whether a tie exists in the candidate set.
     * Transient — resolves to SECTION_2_RESOLVER or SECTION_4_GATED.
     */
    RESOLVER_EVALUATION,

    /**
     * A tie was detected. User is answering the relevant Section 2
     * resolver question(s) selected by trigger predicate matching.
     */
    SECTION_2_RESOLVER,

    /**
     * Data Engineer, Backend Developer, or Data Scientist survived.
     * User is answering Section 4 database deep-dive questions.
     */
    SECTION_4_GATED,

    /**
     * All question collection is complete. The 20-dim tech vector and
     * surviving candidate_roles list are sent to the ML service.
     * Transient — transitions immediately to COMPLETED.
     */
    TERMINAL_SCORING,

    /** Session fully complete. Prediction has been persisted. */
    COMPLETED
}
