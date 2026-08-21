package com.rolecompass.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {

    @JsonProperty("predicted_role")
    private String predictedRole;

    private Double confidence;

    private List<AlternateRole> alternates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternateRole {
        private String role;
        private Double confidence;
    }
}
