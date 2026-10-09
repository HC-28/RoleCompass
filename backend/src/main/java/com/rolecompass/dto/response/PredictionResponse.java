package com.rolecompass.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response body for {@code POST /api/session/{id}/predict}.
 *
 * <p>Contains the top predicted role, its confidence score, alternates for
 * the results page, and roles eliminated during psychometric/technical gates
 * with plain-English explanations the student can understand.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {

    @JsonProperty("predicted_role")
    private String predictedRole;

    private Double confidence;

    private List<AlternateRole> alternates;

    /**
     * Roles eliminated during psychometric and technical routing gates,
     * each with a plain-English explanation the student can understand.
     */
    @JsonProperty("eliminated_roles")
    @Builder.Default
    private List<EliminatedRole> eliminatedRoles = List.of();

    /**
     * Full probability distribution over all 11 roles from the Random Forest.
     * Keys = role names, values = raw model probabilities (NOT renormalized to candidate set).
     * Used by Section3SignalAdjuster for Bayesian posterior adjustment before final winner selection.
     */
    @JsonProperty("all_probabilities")
    @Builder.Default
    private Map<String, Double> allProbabilities = Map.of();

    /**
     * True when the ML service was unreachable and the result was produced by
     * the local fallback heuristic, not by the trained model.
     * The frontend should display a warning banner when this is true.
     */
    @JsonProperty("is_fallback")
    @Builder.Default
    private boolean fallback = false;

    // ─── Nested Types ─────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternateRole {
        private String role;
        private Double confidence;
    }

    /**
     * One eliminated role with a student-facing plain-English reason.
     * Example: role="Data Scientist",
     *          stage="PSYCHOMETRIC",
     *          reason="Your answers suggest you prefer working with concrete systems
     *                  over analytical research and statistical modelling."
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EliminatedRole {

        /** The role name that was ruled out. */
        private String role;

        /**
         * The gate that eliminated this role.
         * One of: "PSYCHOMETRIC" or "TECHNICAL".
         */
        private String stage;

        /**
         * Plain-English explanation of why this role was eliminated.
         * Written for a student — no RIASEC acronyms, no dimension indices.
         */
        private String reason;
    }
}
