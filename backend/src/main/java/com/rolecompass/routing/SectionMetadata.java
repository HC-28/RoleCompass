package com.rolecompass.routing;

import java.util.Map;

/**
 * SectionMetadata — static lookup tables that map FSM states and technical
 * dimension tags to human-readable section and subsection labels for the UI.
 *
 * <p>This is the single source of truth for all display labels. The frontend
 * SectionBanner component reads the {@code section_label} and
 * {@code subsection_label} fields on each {@link com.rolecompass.dto.QuestionDTO}
 * which are populated from these maps by {@link com.rolecompass.service.QuestionService}.
 * </p>
 */
public final class SectionMetadata {

    private SectionMetadata() {}

    // ─── Section Labels (by section_id) ──────────────────────────────────────

    /** Maps sectionId → user-facing section title. */
    public static final Map<Integer, String> SECTION_LABELS = Map.of(
            1, "Personality & Work Style",
            2, "Technical Core Assessment",
            3, "Role-Pair Discriminators",
            4, "Specialist Probes"
    );

    // ─── Subsection Labels (Section 2 only — by dimension tag) ───────────────

    /**
     * Maps a Section 2 dimension tag (first tag on each question) to the
     * human-readable feature subsection label shown in the UI.
     * Matches the 20 features defined in the DOCX question bank.
     */
    public static final Map<String, String> SUBSECTION_LABELS = Map.ofEntries(
            Map.entry("TECH_SERVER_LOGIC",     "Server Logic"),
            Map.entry("TECH_DATA_STORAGE",     "Data Storage"),
            Map.entry("TECH_API_DESIGN",       "API Design"),
            Map.entry("TECH_UI_RENDERING",     "UI Rendering"),
            Map.entry("TECH_STATE_MGMT",       "State Management"),
            Map.entry("TECH_BUILD_PIPELINE",   "Build Pipelines"),
            Map.entry("TECH_INFRA_PROVISION",  "Infrastructure Provisioning"),
            Map.entry("TECH_CONTAINER_ORCH",   "Container Orchestration"),
            Map.entry("TECH_CLOUD_SERVICES",   "Cloud Services"),
            Map.entry("TECH_STAT_ANALYSIS",    "Statistical Analysis"),
            Map.entry("TECH_MODEL_BUILDING",   "Model Building"),
            Map.entry("TECH_DATA_PIPELINE",    "Data Pipelines"),
            Map.entry("TECH_MOBILE_CLIENT",    "Mobile Client"),
            Map.entry("TECH_THREAT_ANALYSIS",  "Threat Analysis"),
            Map.entry("TECH_SYSTEM_HARDENING", "System Hardening"),
            Map.entry("TECH_TEST_DESIGN",      "Test Design"),
            Map.entry("TECH_TEST_AUTOMATION",  "Test Automation"),
            Map.entry("TECH_OBSERVABILITY",    "Observability"),
            Map.entry("TECH_PERF_OPTIM",       "Performance Optimisation"),
            Map.entry("TECH_FULL_SPECTRUM",    "Full Spectrum Delivery")
    );

    /**
     * Resolves the subsection label for a Section 2 question given its
     * primary dimension tag. Returns null for all other sections.
     *
     * @param sectionId       the question's section (1–4)
     * @param primaryDimTag   the question's first dimension tag
     * @return human-readable subsection label, or null if not applicable
     */
    public static String resolveSubsectionLabel(int sectionId, String primaryDimTag) {
        if (sectionId != 2 || primaryDimTag == null) return null;
        return SUBSECTION_LABELS.get(primaryDimTag);
    }
}
