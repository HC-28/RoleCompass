package com.rolecompass.aggregation;

/**
 * Authoritative constants for the 31-feature vector used by FeatureAggregationService.
 *
 * <p>INDEX ORDER IS FROZEN. Changing any constant here MUST be accompanied by
 * corresponding changes in:</p>
 * <ul>
 *   <li>docs/model/feature-schema-v1.md</li>
 *   <li>Python synthetic data generator (future)</li>
 *   <li>FastAPI inference endpoint (future)</li>
 * </ul>
 *
 * <p>Dimension tag strings match Question.dimensionTags values exactly.</p>
 */
public final class FeatureIndex {

    private FeatureIndex() {
        // static constants only
    }

    // ─── Psychological Dimensions (0–10) ─────────────────────────────────────

    public static final int DIM_REALISTIC     = 0;
    public static final int DIM_INVESTIGATIVE = 1;
    public static final int DIM_ARTISTIC      = 2;
    public static final int DIM_SOCIAL        = 3;
    public static final int DIM_ENTERPRISING  = 4;
    public static final int DIM_CONVENTIONAL  = 5;
    public static final int DIM_DATA_IDEAS    = 6;
    public static final int DIM_THINGS_PEOPLE = 7;
    public static final int DIM_BREADTH_DEPTH = 8;
    public static final int DIM_STRUCT_AMBIG  = 9;
    public static final int DIM_OFFENSE_DEF   = 10;

    // ─── Technical Aggregate Features (11–30) ────────────────────────────────

    public static final int TECH_SERVER_LOGIC     = 11;
    public static final int TECH_DATA_STORAGE     = 12;
    public static final int TECH_API_DESIGN       = 13;
    public static final int TECH_UI_RENDERING     = 14;
    public static final int TECH_STATE_MGMT       = 15;
    public static final int TECH_BUILD_PIPELINE   = 16;
    public static final int TECH_INFRA_PROVISION  = 17;
    public static final int TECH_CONTAINER_ORCH   = 18;
    public static final int TECH_CLOUD_SERVICES   = 19;
    public static final int TECH_STAT_ANALYSIS    = 20;
    public static final int TECH_MODEL_BUILDING   = 21;
    public static final int TECH_DATA_PIPELINE    = 22;
    public static final int TECH_MOBILE_CLIENT    = 23;
    public static final int TECH_THREAT_ANALYSIS  = 24;
    public static final int TECH_SYSTEM_HARDENING = 25;
    public static final int TECH_TEST_DESIGN      = 26;
    public static final int TECH_TEST_AUTOMATION  = 27;
    public static final int TECH_OBSERVABILITY    = 28;
    public static final int TECH_PERF_OPTIM       = 29;
    public static final int TECH_FULL_SPECTRUM    = 30;

    // ─── Totals ───────────────────────────────────────────────────────────────

    /** Total number of features in the vector. Immutable. */
    public static final int TOTAL_FEATURES = 31;

    /** Index of the first psychological dimension feature. */
    public static final int PSYCH_START = 0;

    /** Exclusive end index of psychological dimension features. */
    public static final int PSYCH_END = 11;

    /** Index of the first technical aggregate feature. */
    public static final int TECH_START = 11;

    /** Exclusive end index of technical aggregate features. */
    public static final int TECH_END = 31;

    // ─── Tag string constants (must match Question.dimensionTags values) ──────

    public static final String TAG_DIM_REALISTIC     = "DIM_REALISTIC";
    public static final String TAG_DIM_INVESTIGATIVE = "DIM_INVESTIGATIVE";
    public static final String TAG_DIM_ARTISTIC      = "DIM_ARTISTIC";
    public static final String TAG_DIM_SOCIAL        = "DIM_SOCIAL";
    public static final String TAG_DIM_ENTERPRISING  = "DIM_ENTERPRISING";
    public static final String TAG_DIM_CONVENTIONAL  = "DIM_CONVENTIONAL";
    public static final String TAG_DIM_DATA_IDEAS    = "DIM_DATA_IDEAS";
    public static final String TAG_DIM_THINGS_PEOPLE = "DIM_THINGS_PEOPLE";
    public static final String TAG_DIM_BREADTH_DEPTH = "DIM_BREADTH_DEPTH";
    public static final String TAG_DIM_STRUCT_AMBIG  = "DIM_STRUCT_AMBIG";
    public static final String TAG_DIM_OFFENSE_DEF   = "DIM_OFFENSE_DEF";

    public static final String TAG_TECH_SERVER_LOGIC     = "TECH_SERVER_LOGIC";
    public static final String TAG_TECH_DATA_STORAGE     = "TECH_DATA_STORAGE";
    public static final String TAG_TECH_API_DESIGN       = "TECH_API_DESIGN";
    public static final String TAG_TECH_UI_RENDERING     = "TECH_UI_RENDERING";
    public static final String TAG_TECH_STATE_MGMT       = "TECH_STATE_MGMT";
    public static final String TAG_TECH_BUILD_PIPELINE   = "TECH_BUILD_PIPELINE";
    public static final String TAG_TECH_INFRA_PROVISION  = "TECH_INFRA_PROVISION";
    public static final String TAG_TECH_CONTAINER_ORCH   = "TECH_CONTAINER_ORCH";
    public static final String TAG_TECH_CLOUD_SERVICES   = "TECH_CLOUD_SERVICES";
    public static final String TAG_TECH_STAT_ANALYSIS    = "TECH_STAT_ANALYSIS";
    public static final String TAG_TECH_MODEL_BUILDING   = "TECH_MODEL_BUILDING";
    public static final String TAG_TECH_DATA_PIPELINE    = "TECH_DATA_PIPELINE";
    public static final String TAG_TECH_MOBILE_CLIENT    = "TECH_MOBILE_CLIENT";
    public static final String TAG_TECH_THREAT_ANALYSIS  = "TECH_THREAT_ANALYSIS";
    public static final String TAG_TECH_SYSTEM_HARDENING = "TECH_SYSTEM_HARDENING";
    public static final String TAG_TECH_TEST_DESIGN      = "TECH_TEST_DESIGN";
    public static final String TAG_TECH_TEST_AUTOMATION  = "TECH_TEST_AUTOMATION";
    public static final String TAG_TECH_OBSERVABILITY    = "TECH_OBSERVABILITY";
    public static final String TAG_TECH_PERF_OPTIM       = "TECH_PERF_OPTIM";
    public static final String TAG_TECH_FULL_SPECTRUM    = "TECH_FULL_SPECTRUM";

    /**
     * Returns the feature index for a given dimension tag string.
     * Returns -1 if the tag is unrecognised.
     */
    public static int indexForTag(String tag) {
        return switch (tag) {
            case TAG_DIM_REALISTIC     -> DIM_REALISTIC;
            case TAG_DIM_INVESTIGATIVE -> DIM_INVESTIGATIVE;
            case TAG_DIM_ARTISTIC      -> DIM_ARTISTIC;
            case TAG_DIM_SOCIAL        -> DIM_SOCIAL;
            case TAG_DIM_ENTERPRISING  -> DIM_ENTERPRISING;
            case TAG_DIM_CONVENTIONAL  -> DIM_CONVENTIONAL;
            case TAG_DIM_DATA_IDEAS    -> DIM_DATA_IDEAS;
            case TAG_DIM_THINGS_PEOPLE -> DIM_THINGS_PEOPLE;
            case TAG_DIM_BREADTH_DEPTH -> DIM_BREADTH_DEPTH;
            case TAG_DIM_STRUCT_AMBIG  -> DIM_STRUCT_AMBIG;
            case TAG_DIM_OFFENSE_DEF   -> DIM_OFFENSE_DEF;
            case TAG_TECH_SERVER_LOGIC     -> TECH_SERVER_LOGIC;
            case TAG_TECH_DATA_STORAGE     -> TECH_DATA_STORAGE;
            case TAG_TECH_API_DESIGN       -> TECH_API_DESIGN;
            case TAG_TECH_UI_RENDERING     -> TECH_UI_RENDERING;
            case TAG_TECH_STATE_MGMT       -> TECH_STATE_MGMT;
            case TAG_TECH_BUILD_PIPELINE   -> TECH_BUILD_PIPELINE;
            case TAG_TECH_INFRA_PROVISION  -> TECH_INFRA_PROVISION;
            case TAG_TECH_CONTAINER_ORCH   -> TECH_CONTAINER_ORCH;
            case TAG_TECH_CLOUD_SERVICES   -> TECH_CLOUD_SERVICES;
            case TAG_TECH_STAT_ANALYSIS    -> TECH_STAT_ANALYSIS;
            case TAG_TECH_MODEL_BUILDING   -> TECH_MODEL_BUILDING;
            case TAG_TECH_DATA_PIPELINE    -> TECH_DATA_PIPELINE;
            case TAG_TECH_MOBILE_CLIENT    -> TECH_MOBILE_CLIENT;
            case TAG_TECH_THREAT_ANALYSIS  -> TECH_THREAT_ANALYSIS;
            case TAG_TECH_SYSTEM_HARDENING -> TECH_SYSTEM_HARDENING;
            case TAG_TECH_TEST_DESIGN      -> TECH_TEST_DESIGN;
            case TAG_TECH_TEST_AUTOMATION  -> TECH_TEST_AUTOMATION;
            case TAG_TECH_OBSERVABILITY    -> TECH_OBSERVABILITY;
            case TAG_TECH_PERF_OPTIM       -> TECH_PERF_OPTIM;
            case TAG_TECH_FULL_SPECTRUM    -> TECH_FULL_SPECTRUM;
            default -> -1;
        };
    }
}
