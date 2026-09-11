package com.rolecompass.routing;

import com.rolecompass.aggregation.FeatureIndex;

/**
 * Immutable role target-vector for one of the 10 IT roles.
 *
 * <p>The values in this class come from docs/model/role-vector-placeholder-v1.md.
 * They are PROPOSED values and require owner approval before use in ML training.
 * The routing engine uses them for relative candidate elimination only.</p>
 *
 * <p>Only the 11 psychological dimensions (indices 0–10) are used for routing.
 * The 20 technical features (indices 11–30) are set to 0.5 (neutral) here
 * because routing elimination is based on psychological fit; technical
 * differentiation happens at ML inference time.</p>
 */
public final class RoleProfile {

    /** Numeric role ID (1-indexed, matches AGENTS.md role list). */
    public final int roleId;

    /** Human-readable name. Used only for logging — never sent to React. */
    public final String roleName;

    /**
     * Ideal normalized target vector [0.0, 1.0] for all 31 features.
     * Only indices 0–10 (psychological) carry meaningful values for routing.
     * Indices 11–30 (technical) default to 0.5 in this routing-phase profile.
     */
    public final double[] targetVector;

    private RoleProfile(int roleId, String roleName, double[] psychTargets) {
        if (psychTargets.length != FeatureIndex.PSYCH_END) {
            throw new IllegalArgumentException(
                    "psychTargets must have " + FeatureIndex.PSYCH_END + " values, got " + psychTargets.length);
        }
        this.roleId = roleId;
        this.roleName = roleName;
        this.targetVector = new double[FeatureIndex.TOTAL_FEATURES];
        // Copy the 11 psychological values
        System.arraycopy(psychTargets, 0, this.targetVector, 0, FeatureIndex.PSYCH_END);
        // Technical features default to neutral (0.5) at routing stage
        for (int i = FeatureIndex.TECH_START; i < FeatureIndex.TOTAL_FEATURES; i++) {
            this.targetVector[i] = 0.5;
        }
    }

    // ─── PROPOSED Role Profiles ───────────────────────────────────────────────
    // Source: docs/model/role-vector-placeholder-v1.md
    // Columns: R, I, A, S, E, C, D-I, T-P, B-D, S-A, O-D

    public static final RoleProfile BACKEND_DEVELOPER = new RoleProfile(1, "Backend Developer",
            new double[]{0.70, 0.80, 0.30, 0.30, 0.40, 0.70, 0.75, 0.75, 0.65, 0.75, 0.75});

    public static final RoleProfile FRONTEND_DEVELOPER = new RoleProfile(2, "Frontend Developer",
            new double[]{0.50, 0.60, 0.70, 0.40, 0.40, 0.60, 0.45, 0.45, 0.50, 0.60, 0.80});

    public static final RoleProfile FULL_STACK_DEVELOPER = new RoleProfile(3, "Full Stack Developer",
            new double[]{0.65, 0.70, 0.55, 0.35, 0.50, 0.60, 0.60, 0.55, 0.35, 0.65, 0.80});

    public static final RoleProfile DATA_SCIENTIST = new RoleProfile(4, "Data Scientist",
            new double[]{0.50, 0.90, 0.45, 0.30, 0.40, 0.65, 0.90, 0.65, 0.70, 0.50, 0.65});

    public static final RoleProfile DATA_ENGINEER = new RoleProfile(5, "Data Engineer",
            new double[]{0.65, 0.75, 0.30, 0.25, 0.40, 0.75, 0.85, 0.75, 0.65, 0.75, 0.70});

    public static final RoleProfile CYBERSECURITY_ENGINEER = new RoleProfile(6, "Cybersecurity Engineer",
            new double[]{0.70, 0.85, 0.25, 0.25, 0.35, 0.80, 0.70, 0.80, 0.60, 0.85, 0.25});

    public static final RoleProfile DEVOPS_ENGINEER = new RoleProfile(7, "DevOps Engineer",
            new double[]{0.75, 0.70, 0.25, 0.25, 0.45, 0.80, 0.70, 0.80, 0.45, 0.80, 0.70});

    public static final RoleProfile CLOUD_ENGINEER = new RoleProfile(8, "Cloud Engineer",
            new double[]{0.70, 0.75, 0.25, 0.25, 0.50, 0.75, 0.75, 0.80, 0.45, 0.75, 0.70});

    public static final RoleProfile ANDROID_DEVELOPER = new RoleProfile(9, "Android Developer",
            new double[]{0.65, 0.65, 0.60, 0.40, 0.40, 0.65, 0.55, 0.55, 0.55, 0.65, 0.80});

    public static final RoleProfile QA_TEST_AUTOMATION = new RoleProfile(10, "QA / Test Automation Engineer",
            new double[]{0.65, 0.70, 0.30, 0.35, 0.35, 0.85, 0.65, 0.60, 0.55, 0.85, 0.40});

    /** All 10 roles in canonical order (roleId 1..10). */
    public static final RoleProfile[] ALL_ROLES = {
            BACKEND_DEVELOPER,
            FRONTEND_DEVELOPER,
            FULL_STACK_DEVELOPER,
            DATA_SCIENTIST,
            DATA_ENGINEER,
            CYBERSECURITY_ENGINEER,
            DEVOPS_ENGINEER,
            CLOUD_ENGINEER,
            ANDROID_DEVELOPER,
            QA_TEST_AUTOMATION
    };

    /**
     * Looks up a RoleProfile by roleId (1-indexed).
     *
     * @param roleId 1 through 10
     * @return the matching RoleProfile
     * @throws IllegalArgumentException if roleId is out of range
     */
    public static RoleProfile forId(int roleId) {
        for (RoleProfile p : ALL_ROLES) {
            if (p.roleId == roleId) return p;
        }
        throw new IllegalArgumentException("Unknown roleId: " + roleId);
    }

    public static RoleProfile forRoleName(String roleName) {
        for (RoleProfile p : ALL_ROLES) {
            if (p.roleName.equalsIgnoreCase(roleName)) return p;
        }
        throw new IllegalArgumentException("Unknown roleName: " + roleName);
    }

    public static int idForRoleName(String roleName) {
        return forRoleName(roleName).roleId;
    }

    public static String nameForId(int roleId) {
        return forId(roleId).roleName;
    }
}
