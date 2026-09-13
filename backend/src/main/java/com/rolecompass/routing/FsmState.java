package com.rolecompass.routing;

/**
 * FSM states for the adaptive assessment session.
 *
 * <p>Canonical transition order:</p>
 * <pre>
 * SECTION_1_RIASEC
 *   → PRUNE_PSYCHOMETRICS    (routing engine applies 11-dim threshold elimination)
 *   → SECTION_2_TECH_CORE   (40 technical questions; adaptive per-domain Q2 skip rule)
 *   → PRUNE_TECH_SKILLS      (routing engine applies tech floor rules)
 *   → RESOLVER_EVALUATION    (conditional — fires if ≥2 roles tied in candidate set)
 *   → SECTION_3_RESOLVER     (conditional — asks resolver questions when a tie exists)
 *   → SECTION_4_SPECIALIST   (conditional — fires if Data Scientist / Data Engineer /
 *                              Cybersecurity Engineer / DevOps Engineer survived)
 *   → TERMINAL_SCORING       (sends 20-dim tech vector + candidate_roles to ML service)
 *   → COMPLETED
 * </pre>
 */
public enum FsmState {

    /** User is answering the 16 Section 1 psychometric/RIASEC questions (mandatory). */
    SECTION_1_RIASEC,

    /**
     * All Section 1 answers collected. Engine applies 11-dim psychometric
     * threshold gates and eliminates incompatible roles.
     * This is a transient computation state — not delivered to the user.
     */
    PRUNE_PSYCHOMETRICS,

    /**
     * User is answering the up-to-40 Section 2 technical interest questions.
     * Each of the 20 ML feature domains has 2 questions (Q1 + Q2).
     * Adaptive skip rule: if a domain's Q1 answer is extreme (≤ 2 or ≥ 4 on
     * the 1–5 Likert scale), Q2 for that domain is skipped — domain is considered
     * resolved. Typical path: 22–30 questions answered out of 40 maximum.
     */
    SECTION_2_TECH_CORE,

    /**
     * All applicable Section 2 answers collected. Engine applies tech floor rules
     * (MOBILE floor, STATS+MODEL floor, THREAT+HARDENING floor).
     * Transient computation state.
     */
    PRUNE_TECH_SKILLS,

    /**
     * Engine evaluates whether a tie exists in the candidate set.
     * Transient — resolves to SECTION_3_RESOLVER or SECTION_4_SPECIALIST.
     */
    RESOLVER_EVALUATION,

    /**
     * A tie was detected between two similar roles. User is answering the relevant
     * Section 3 pair-discriminator question(s) selected by requires_both predicate matching.
     * Maximum 12 questions (6 pairs × 2), typically 2–4.
     */
    SECTION_3_RESOLVER,

    /**
     * A specialist role (Data Scientist, Data Engineer, Cybersecurity Engineer,
     * or DevOps Engineer) survived pruning. User is answering the 4 dedicated
     * Section 4 probe questions for that role. Maximum 16 questions (4 roles × 4).
     */
    SECTION_4_SPECIALIST,

    /**
     * All question collection is complete. The 20-dim tech vector and
     * surviving candidate_roles list are sent to the ML service.
     * Transient — transitions immediately to COMPLETED.
     */
    TERMINAL_SCORING,

    /** Session fully complete. Prediction has been persisted. */
    COMPLETED
}
