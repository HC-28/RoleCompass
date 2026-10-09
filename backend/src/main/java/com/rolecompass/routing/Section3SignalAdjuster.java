package com.rolecompass.routing;

import com.rolecompass.dto.response.PredictionResponse;
import com.rolecompass.dto.response.PredictionResponse.AlternateRole;
import com.rolecompass.entity.Question;
import com.rolecompass.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Section3SignalAdjuster - Bayesian posterior probability adjustment.
 *
 * The ML model gives P(role | tech_features). Section 3 provides
 * P(preference | roles). Using Bayes:
 *   P(role | tech + preference) is proportional to P(role | tech) * likelihood(preference | role)
 *
 * We apply role-specific multipliers to the raw RF probability distribution
 * for each answered S3 pair, then renormalize. This is semantically correct,
 * requires no retraining, and does not corrupt the feature vector.
 *
 * Answer semantics (PREFERENCE_4 scale used in Section 3):
 *   4 = Strongly prefer Option A
 *   3 = Prefer Option A
 *   2 = Prefer Option B
 *   1 = Strongly prefer Option B
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Section3SignalAdjuster {

    private static final double STRONG_BOOST    = 1.70;
    private static final double MODERATE_BOOST  = 1.30;
    private static final double MODERATE_DAMPEN = 0.75;
    private static final double STRONG_DAMPEN   = 0.45;

    private final QuestionRepository questionRepository;

    /**
     * Applies Bayesian probability adjustment using Section 3 answers,
     * then rebuilds PredictionResponse with the adjusted winner from the candidate set.
     */
    public PredictionResponse adjust(
            PredictionResponse mlResponse,
            Map<Long, Integer> rawAnswers,
            List<String> candidateRoles
    ) {
        Map<String, Double> probs = mlResponse.getAllProbabilities();
        if (probs == null || probs.isEmpty()) {
            log.debug("Section3SignalAdjuster: no all_probabilities available, skipping.");
            return mlResponse;
        }

        Map<String, Double> adjusted = new LinkedHashMap<>(probs);
        List<Question> s3Questions = questionRepository.findBySectionIdOrderByIdAsc(3);

        for (Question q : s3Questions) {
            Integer ans = rawAnswers.get(q.getId());
            if (ans == null) continue;
            applyPairAdjustment(adjusted, ans, q.getText());
        }

        // Renormalize full distribution
        double total = adjusted.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) {
            adjusted.replaceAll((role, p) -> p / total);
        }

        // Pick winner + alternates from candidate set only
        List<Map.Entry<String, Double>> ranked = adjusted.entrySet().stream()
                .filter(e -> candidateRoles.contains(e.getKey()))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        if (ranked.isEmpty()) {
            log.warn("Section3SignalAdjuster: no candidate matches after adjustment. Returning original.");
            return mlResponse;
        }

        String winner     = ranked.get(0).getKey();
        double winnerConf = ranked.get(0).getValue();

        List<AlternateRole> alternates = ranked.stream()
                .skip(1).limit(2)
                .map(e -> AlternateRole.builder().role(e.getKey()).confidence(e.getValue()).build())
                .collect(Collectors.toList());

        log.info("S3Adjust: winner='{}' conf={:.3f}  was='{}' conf={:.3f}",
                winner, winnerConf,
                mlResponse.getPredictedRole(), mlResponse.getConfidence());

        return PredictionResponse.builder()
                .predictedRole(winner)
                .confidence(winnerConf)
                .alternates(alternates)
                .allProbabilities(adjusted)
                .eliminatedRoles(mlResponse.getEliminatedRoles())
                .fallback(mlResponse.isFallback())
                .build();
    }

    // -------------------------------------------------------------------------

    private void applyPairAdjustment(Map<String, Double> p, int ans, String text) {
        // PAIR A: Full Stack Developer (A) vs Backend Developer (B)
        if (text.contains("Creating complete applications from scratch")) {
            boost(p, ans, "Full Stack Developer", "Backend Developer");
        }
        // PAIR B: Frontend Developer (A) vs Mobile Developer (B)
        if (text.contains("Building websites and web applications")) {
            boost(p, ans, "Frontend Developer", "Mobile Developer");
        }
        // PAIR C: DevOps Engineer (A) vs Cloud Engineer (B)
        if (text.contains("Automating how code gets tested and delivered")) {
            boost(p, ans, "DevOps Engineer", "Cloud Engineer");
        }
        // PAIR D: Data Scientist (A) vs Data Engineer (B)
        if (text.contains("Analyzing complex data and building mathematical AI models")) {
            boost(p, ans, "Data Scientist", "Data Engineer");
        }
        // PAIR E: Cybersecurity Engineer (A) vs QA / Test Automation Engineer (B)
        if (text.contains("Thinking like an attacker to find vulnerabilities")) {
            boost(p, ans, "Cybersecurity Engineer", "QA / Test Automation Engineer");
        }
        // PAIR F: Frontend Developer (A) vs Full Stack Developer (B)
        if (text.contains("Spending extra time perfecting the visual details")) {
            boost(p, ans, "Frontend Developer", "Full Stack Developer");
        }
        // PAIR G: Data Scientist (A) vs AI / ML Engineer (B)
        if (text.contains("Running statistical experiments, exploring datasets")) {
            boost(p, ans, "Data Scientist", "AI / ML Engineer");
        }
    }

    /**
     * Applies directional multipliers. roleA = Option A, roleB = Option B.
     * ans=4 strongly prefers A (boost A, dampen B).
     * ans=1 strongly prefers B (boost B, dampen A).
     */
    private void boost(Map<String, Double> p, int ans, String roleA, String roleB) {
        switch (ans) {
            case 4 -> { p.computeIfPresent(roleA, (r, v) -> v * STRONG_BOOST);
                        p.computeIfPresent(roleB, (r, v) -> v * STRONG_DAMPEN); }
            case 3 -> { p.computeIfPresent(roleA, (r, v) -> v * MODERATE_BOOST);
                        p.computeIfPresent(roleB, (r, v) -> v * MODERATE_DAMPEN); }
            case 2 -> { p.computeIfPresent(roleB, (r, v) -> v * MODERATE_BOOST);
                        p.computeIfPresent(roleA, (r, v) -> v * MODERATE_DAMPEN); }
            case 1 -> { p.computeIfPresent(roleB, (r, v) -> v * STRONG_BOOST);
                        p.computeIfPresent(roleA, (r, v) -> v * STRONG_DAMPEN); }
            default -> {}
        }
        log.debug("S3 adjust ans={}: optionA='{}' optionB='{}'", ans, roleA, roleB);
    }
}