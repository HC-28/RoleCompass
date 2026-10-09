package com.rolecompass.service;

import com.rolecompass.dto.response.PredictionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MlClientService — thin HTTP client for the FastAPI ML scoring service.
 *
 * <p>Responsibilities (and ONLY these — Single Responsibility Principle):
 * <ol>
 *   <li>Build the JSON request body expected by {@code POST /score}.</li>
 *   <li>POST the 20-dim tech vector and candidate_roles to the ML service.</li>
 *   <li>Parse the raw response into a {@link PredictionResponse}.</li>
 *   <li>Provide a local fallback prediction when the ML service is unreachable.</li>
 * </ol>
 *
 * <p><strong>Never</strong> inject session, routing, or aggregation concerns here.
 * Those belong in {@link SessionService}.</p>
 */
@Slf4j
@Service
public class MlClientService {

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Posts the 20-feature tech vector and the surviving candidate role list to
     * {@code POST <ml.service.url>/score} and returns the parsed prediction.
     *
     * @param techVector     20-dimensional normalised feature vector [0, 1]
     * @param candidateRoles surviving role names after routing gate passes
     * @return scored prediction with predicted role, confidence, and alternates
     * @throws RuntimeException when the ML service is unreachable or returns null
     */
    public PredictionResponse score(double[] techVector, List<String> candidateRoles) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        Double[] features = new Double[techVector.length];
        for (int i = 0; i < techVector.length; i++) {
            features[i] = techVector[i];
        }
        requestBody.put("features", features);
        requestBody.put("candidate_roles", candidateRoles);

        @SuppressWarnings("unchecked")
        Map<String, Object> raw = restTemplate.postForObject(
                mlServiceUrl + "/score", requestBody, Map.class);

        if (raw == null) {
            throw new RuntimeException("Null response from ML service at " + mlServiceUrl + "/score");
        }

        String predictedRole = (String) raw.get("predicted_role");
        double confidence = ((Number) raw.get("confidence")).doubleValue();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawAlternates =
                (List<Map<String, Object>>) raw.get("alternates");

        List<PredictionResponse.AlternateRole> alternates = new ArrayList<>();
        if (rawAlternates != null) {
            for (Map<String, Object> item : rawAlternates) {
                alternates.add(PredictionResponse.AlternateRole.builder()
                        .role((String) item.get("role"))
                        .confidence(((Number) item.get("confidence")).doubleValue())
                        .build());
            }
        }

        // Parse full probability distribution (all 11 roles) for Bayesian adjustment
        @SuppressWarnings("unchecked")
        Map<String, Object> rawAllProbs = (Map<String, Object>) raw.get("all_probabilities");
        Map<String, Double> allProbabilities = new LinkedHashMap<>();
        if (rawAllProbs != null) {
            rawAllProbs.forEach((role, prob) ->
                    allProbabilities.put(role, ((Number) prob).doubleValue()));
        }

        return PredictionResponse.builder()
                .predictedRole(predictedRole)
                .confidence(confidence)
                .alternates(alternates)
                .allProbabilities(allProbabilities)
                .build();
    }

    /**
     * Returns a fallback prediction using the first surviving candidate role.
     * Used exclusively when the ML service is unreachable.
     * Sets {@code is_fallback=true} so the frontend can display a warning banner.
     *
     * @param candidateRoles surviving role names (must not be null)
     * @return a best-guess prediction with 0.5 confidence and {@code is_fallback=true}
     */
    public PredictionResponse fallback(List<String> candidateRoles) {
        log.warn("Using local fallback prediction. Candidate pool: {}", candidateRoles);
        String role = (candidateRoles == null || candidateRoles.isEmpty())
                ? "Backend Developer"
                : candidateRoles.get(0);

        List<PredictionResponse.AlternateRole> alternates = new ArrayList<>();
        if (candidateRoles != null && candidateRoles.size() > 1) {
            alternates.add(PredictionResponse.AlternateRole.builder()
                    .role(candidateRoles.get(1))
                    .confidence(0.30)
                    .build());
        }

        return PredictionResponse.builder()
                .predictedRole(role)
                .confidence(0.50)
                .alternates(alternates)
                .eliminatedRoles(List.of())
                .fallback(true)
                .build();
    }
}
