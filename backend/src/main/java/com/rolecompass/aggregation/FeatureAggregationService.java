package com.rolecompass.aggregation;

import com.rolecompass.entity.Answer;
import com.rolecompass.entity.Question;
import com.rolecompass.repository.AnswerRepository;
import com.rolecompass.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FeatureAggregationService
 *
 * <p>Converts a session's Answer records into two distinct outputs, keeping the
 * two architectural layers strictly separated:</p>
 *
 * <ul>
 *   <li><b>Output A — Psychometric profile:</b> {@link #buildPsychProfile(UUID)}
 *       returns a {@code Map<String, Double>} with 11 normalized psychometric
 *       dimension scores. Used ONLY by the routing engine for RIASEC-based
 *       candidate elimination. Never sent to the ML model.</li>
 *
 *   <li><b>Output B — Tech vector:</b> {@link #buildTechVector(UUID)}
 *       returns a {@code double[20]} in exact feature-index order (SERVER…FULLSPEC).
 *       Used ONLY as input to the FastAPI ML service. Never receives psychometric data.</li>
 * </ul>
 *
 * <p>Aggregation rules (authoritative):</p>
 * <ul>
 *   <li>Mean of all Likert values for questions tagged to a dimension.</li>
 *   <li>Normalization: {@code (mean - 1.0) / 4.0} → [0.0, 1.0].</li>
 *   <li>No answers for a feature → neutral value = {@link #NEUTRAL_VALUE} (0.5).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureAggregationService {

    /** Neutral value used when no answers cover a feature. */
    public static final double NEUTRAL_VALUE = 0.5;

    /** Minimum Likert value in the scale. */
    public static final double LIKERT_MIN = 1.0;

    /** Maximum Likert value in the scale. */
    public static final double LIKERT_MAX = 5.0;

    /** Ordered psychometric dimension names (must match FeatureIndex TAG constants). */
    public static final List<String> PSYCH_DIMENSION_NAMES = List.of(
            FeatureIndex.TAG_DIM_REALISTIC,
            FeatureIndex.TAG_DIM_INVESTIGATIVE,
            FeatureIndex.TAG_DIM_ARTISTIC,
            FeatureIndex.TAG_DIM_SOCIAL,
            FeatureIndex.TAG_DIM_ENTERPRISING,
            FeatureIndex.TAG_DIM_CONVENTIONAL,
            FeatureIndex.TAG_DIM_DATA_IDEAS,
            FeatureIndex.TAG_DIM_THINGS_PEOPLE,
            FeatureIndex.TAG_DIM_BREADTH_DEPTH,
            FeatureIndex.TAG_DIM_STRUCT_AMBIG,
            FeatureIndex.TAG_DIM_OFFENSE_DEF
    );

    /**
     * Ordered tech feature names in exact ML input order (must match train_model.py
     * TECH_FEATURES list and FeatureIndex TECH_* constants, indices 11–30).
     */
    public static final List<String> TECH_FEATURE_NAMES = List.of(
            FeatureIndex.TAG_TECH_SERVER_LOGIC,
            FeatureIndex.TAG_TECH_DATA_STORAGE,
            FeatureIndex.TAG_TECH_API_DESIGN,
            FeatureIndex.TAG_TECH_UI_RENDERING,
            FeatureIndex.TAG_TECH_STATE_MGMT,
            FeatureIndex.TAG_TECH_BUILD_PIPELINE,
            FeatureIndex.TAG_TECH_INFRA_PROVISION,
            FeatureIndex.TAG_TECH_CONTAINER_ORCH,
            FeatureIndex.TAG_TECH_CLOUD_SERVICES,
            FeatureIndex.TAG_TECH_STAT_ANALYSIS,
            FeatureIndex.TAG_TECH_MODEL_BUILDING,
            FeatureIndex.TAG_TECH_DATA_PIPELINE,
            FeatureIndex.TAG_TECH_MOBILE_CLIENT,
            FeatureIndex.TAG_TECH_THREAT_ANALYSIS,
            FeatureIndex.TAG_TECH_SYSTEM_HARDENING,
            FeatureIndex.TAG_TECH_TEST_DESIGN,
            FeatureIndex.TAG_TECH_TEST_AUTOMATION,
            FeatureIndex.TAG_TECH_OBSERVABILITY,
            FeatureIndex.TAG_TECH_PERF_OPTIM,
            FeatureIndex.TAG_TECH_FULL_SPECTRUM
    );

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    // ─── Output A: Psychometric Profile (for routing engine) ─────────────────

    /**
     * Builds the 11-dimension psychometric profile for the given session.
     * Used ONLY by the AdaptiveRoutingEngine for candidate elimination.
     * Never sent to the ML service.
     *
     * @param sessionId the UUID of the session
     * @return map of dimension tag name → normalized score in [0.0, 1.0]
     */
    public Map<String, Double> buildPsychProfile(UUID sessionId) {
        List<Answer> answers = answerRepository.findByIdSessionId(sessionId);
        Map<Long, Question> questionMap = loadQuestionMap();
        return buildPsychProfileFromMaps(toAnswerMap(answers), questionMap);
    }

    /**
     * Builds the psychometric profile from pre-loaded maps (for unit testing
     * and routing engine use without requiring DB access).
     */
    public static Map<String, Double> buildPsychProfileFromMaps(
            Map<Long, Integer> answerMap,
            Map<Long, Question> questionMap
    ) {
        Map<String, List<Integer>> buckets = new LinkedHashMap<>();
        for (String dim : PSYCH_DIMENSION_NAMES) {
            buckets.put(dim, new ArrayList<>());
        }

        for (Map.Entry<Long, Integer> entry : answerMap.entrySet()) {
            Question question = questionMap.get(entry.getKey());
            if (question == null || question.getDimensionTags() == null) continue;
            for (String tag : question.getDimensionTags()) {
                if (buckets.containsKey(tag)) {
                    buckets.get(tag).add(entry.getValue());
                }
            }
        }

        Map<String, Double> profile = new LinkedHashMap<>();
        for (String dim : PSYCH_DIMENSION_NAMES) {
            List<Integer> bucket = buckets.get(dim);
            profile.put(dim, bucket.isEmpty() ? NEUTRAL_VALUE : normalize(average(bucket)));
        }
        return profile;
    }

    // ─── Output B: Tech Feature Vector (for ML service) ──────────────────────

    /**
     * Builds the 20-dimensional technical feature vector for the ML service.
     * Contains ONLY tech features (FeatureIndex indices 11–30).
     * Psychometric features are never included.
     *
     * @param sessionId the UUID of the session
     * @return double[20] normalized in [0.0, 1.0], in exact ML feature order
     */
    public double[] buildTechVector(UUID sessionId) {
        List<Answer> answers = answerRepository.findByIdSessionId(sessionId);
        Map<Long, Question> questionMap = loadQuestionMap();
        return buildTechVectorFromMaps(toAnswerMap(answers), questionMap);
    }

    /**
     * Builds the tech vector from pre-loaded maps (for unit testing).
     */
    public static double[] buildTechVectorFromMaps(
            Map<Long, Integer> answerMap,
            Map<Long, Question> questionMap
    ) {
        Map<String, List<Integer>> buckets = new LinkedHashMap<>();
        for (String feat : TECH_FEATURE_NAMES) {
            buckets.put(feat, new ArrayList<>());
        }

        for (Map.Entry<Long, Integer> entry : answerMap.entrySet()) {
            Question question = questionMap.get(entry.getKey());
            if (question == null || question.getDimensionTags() == null) continue;
            for (String tag : question.getDimensionTags()) {
                if (buckets.containsKey(tag)) {
                    buckets.get(tag).add(entry.getValue());
                }
            }
        }

        double[] vector = new double[TECH_FEATURE_NAMES.size()];
        for (int i = 0; i < TECH_FEATURE_NAMES.size(); i++) {
            List<Integer> bucket = buckets.get(TECH_FEATURE_NAMES.get(i));
            vector[i] = bucket.isEmpty() ? NEUTRAL_VALUE : normalize(average(bucket));
        }
        return vector;
    }

    // ─── Legacy / Full Vector Method (31 features) ──────────────────────────

    /**
     * Builds the full 31-feature vector for backwards compatibility and test verification.
     */
    public double[] buildFeatureVector(UUID sessionId) {
        List<Answer> answers = answerRepository.findByIdSessionId(sessionId);
        Map<Long, Question> questionMap = loadQuestionMap();
        return buildFeatureVectorFromMaps(toAnswerMap(answers), questionMap);
    }

    /**
     * Builds the full 31-feature vector from pre-loaded maps.
     */
    public static double[] buildFeatureVectorFromMaps(
            Map<Long, Integer> answerMap,
            Map<Long, Question> questionMap
    ) {
        List<List<Integer>> featureBuckets = new ArrayList<>(FeatureIndex.TOTAL_FEATURES);
        for (int i = 0; i < FeatureIndex.TOTAL_FEATURES; i++) {
            featureBuckets.add(new ArrayList<>());
        }

        for (Map.Entry<Long, Integer> entry : answerMap.entrySet()) {
            Question question = questionMap.get(entry.getKey());
            if (question == null || question.getDimensionTags() == null) continue;
            for (String tag : question.getDimensionTags()) {
                int index = FeatureIndex.indexForTag(tag);
                if (index >= 0 && index < FeatureIndex.TOTAL_FEATURES) {
                    featureBuckets.get(index).add(entry.getValue());
                }
            }
        }

        double[] vector = new double[FeatureIndex.TOTAL_FEATURES];
        for (int i = 0; i < FeatureIndex.TOTAL_FEATURES; i++) {
            List<Integer> bucket = featureBuckets.get(i);
            vector[i] = bucket.isEmpty() ? NEUTRAL_VALUE : normalize(average(bucket));
        }
        return vector;
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    /**
     * Normalizes a raw mean Likert value to [0.0, 1.0].
     * Formula: (mean - 1) / 4. Clamped to [0.0, 1.0].
     */
    public static double normalize(double meanLikert) {
        double normalized = (meanLikert - LIKERT_MIN) / (LIKERT_MAX - LIKERT_MIN);
        return Math.max(0.0, Math.min(1.0, normalized));
    }

    private static double average(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).average().orElse(3.0);
    }

    private Map<Long, Question> loadQuestionMap() {
        return questionRepository.findAll().stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
    }

    private static Map<Long, Integer> toAnswerMap(List<Answer> answers) {
        Map<Long, Integer> map = new HashMap<>();
        for (Answer a : answers) {
            map.put(a.getId().getQuestionId(), a.getLikertValue());
        }
        return map;
    }
}
