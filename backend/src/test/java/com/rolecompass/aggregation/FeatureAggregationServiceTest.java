package com.rolecompass.aggregation;

import com.rolecompass.entity.Answer;
import com.rolecompass.entity.AnswerId;
import com.rolecompass.entity.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.rolecompass.aggregation.FeatureIndex.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FeatureAggregationService.
 *
 * <p>These tests use the static {@code buildFeatureVectorFromMaps} method
 * so no Spring context or database is required.</p>
 *
 * <p>Every test that checks vector length must verify it equals exactly 31.</p>
 */
@DisplayName("FeatureAggregationService")
class FeatureAggregationServiceTest {

    // ─── Helper builders ──────────────────────────────────────────────────────

    private static Question questionWith(Long id, String... tags) {
        Question q = new Question();
        q.setId(id);
        q.setSectionId(1);
        q.setText("Test question " + id);
        q.setDimensionTags(tags);
        return q;
    }

    private static Map<Long, Question> questionMap(Question... questions) {
        Map<Long, Question> map = new HashMap<>();
        for (Question q : questions) {
            map.put(q.getId(), q);
        }
        return map;
    }

    // ─── Output length tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Vector length is exactly 31 for a single answer")
    void vectorLength_singleAnswer() {
        Question q = questionWith(1L, TAG_DIM_REALISTIC);
        Map<Long, Integer> answers = Map.of(1L, 3);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));
        assertThat(vector).hasSize(TOTAL_FEATURES);
        assertThat(vector).hasSize(31);
    }

    @Test
    @DisplayName("Vector length is exactly 31 when no answers are provided")
    void vectorLength_noAnswers() {
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(
                Map.of(), Map.of());
        assertThat(vector).hasSize(31);
    }

    // ─── Normalization tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Likert value 1 normalizes to 0.0")
    void normalize_minLikert() {
        double result = FeatureAggregationService.normalize(1.0);
        assertThat(result).isEqualTo(0.0, within(1e-9));
    }

    @Test
    @DisplayName("Likert value 5 normalizes to 1.0")
    void normalize_maxLikert() {
        double result = FeatureAggregationService.normalize(5.0);
        assertThat(result).isEqualTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("Likert value 3 normalizes to 0.5")
    void normalize_midLikert() {
        double result = FeatureAggregationService.normalize(3.0);
        assertThat(result).isEqualTo(0.5, within(1e-9));
    }

    @Test
    @DisplayName("Likert value 2 normalizes to 0.25")
    void normalize_likert2() {
        double result = FeatureAggregationService.normalize(2.0);
        assertThat(result).isEqualTo(0.25, within(1e-9));
    }

    @Test
    @DisplayName("Likert value 4 normalizes to 0.75")
    void normalize_likert4() {
        double result = FeatureAggregationService.normalize(4.0);
        assertThat(result).isEqualTo(0.75, within(1e-9));
    }

    // ─── Missing answer handling ──────────────────────────────────────────────

    @Test
    @DisplayName("Features with no answers default to NEUTRAL_VALUE (0.5)")
    void missingFeature_defaultsToNeutral() {
        // Only answer DIM_REALISTIC — all other features should be neutral
        Question q = questionWith(1L, TAG_DIM_REALISTIC);
        Map<Long, Integer> answers = Map.of(1L, 5);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));

        // All tech features and other psych dims should be neutral
        for (int i = PSYCH_START + 1; i < TOTAL_FEATURES; i++) {
            assertThat(vector[i])
                    .as("Feature index %d should be neutral", i)
                    .isEqualTo(FeatureAggregationService.NEUTRAL_VALUE, within(1e-9));
        }
    }

    // ─── Known answer → known vector tests ───────────────────────────────────

    @Test
    @DisplayName("Single answer with Likert 5 on DIM_REALISTIC produces 1.0 at index 0")
    void knownAnswer_maxLikert_realistic() {
        Question q = questionWith(1L, TAG_DIM_REALISTIC);
        Map<Long, Integer> answers = Map.of(1L, 5);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));

        assertThat(vector[DIM_REALISTIC]).isEqualTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("Single answer with Likert 1 on DIM_REALISTIC produces 0.0 at index 0")
    void knownAnswer_minLikert_realistic() {
        Question q = questionWith(1L, TAG_DIM_REALISTIC);
        Map<Long, Integer> answers = Map.of(1L, 1);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));

        assertThat(vector[DIM_REALISTIC]).isEqualTo(0.0, within(1e-9));
    }

    @Test
    @DisplayName("Multi-tag question contributes to multiple feature buckets")
    void multiTag_contributesToMultipleBuckets() {
        Question q = questionWith(1L, TAG_DIM_REALISTIC, TAG_TECH_SERVER_LOGIC);
        Map<Long, Integer> answers = Map.of(1L, 5);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));

        assertThat(vector[DIM_REALISTIC]).isEqualTo(1.0, within(1e-9));
        assertThat(vector[TECH_SERVER_LOGIC]).isEqualTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("Multiple questions for same feature are averaged")
    void multiQuestion_sameDimension_averaged() {
        Question q1 = questionWith(1L, TAG_DIM_INVESTIGATIVE);
        Question q2 = questionWith(2L, TAG_DIM_INVESTIGATIVE);
        // q1=5 (normalized=1.0), q2=1 (normalized=0.0) → mean Likert=3 → normalized=0.5
        Map<Long, Integer> answers = Map.of(1L, 5, 2L, 1);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(
                answers, questionMap(q1, q2));

        assertThat(vector[DIM_INVESTIGATIVE]).isEqualTo(0.5, within(1e-9));
    }

    @Test
    @DisplayName("Answer to question with unknown tag is silently ignored")
    void unknownTag_isIgnored() {
        Question q = questionWith(1L, "UNKNOWN_TAG_XYZ");
        Map<Long, Integer> answers = Map.of(1L, 5);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));

        // All features should remain neutral since the tag was unrecognised
        for (int i = 0; i < TOTAL_FEATURES; i++) {
            assertThat(vector[i]).isEqualTo(FeatureAggregationService.NEUTRAL_VALUE, within(1e-9));
        }
        assertThat(vector).hasSize(31);
    }

    @Test
    @DisplayName("Answer to non-existent question is ignored")
    void answerToNonExistentQuestion_isIgnored() {
        Map<Long, Integer> answers = Map.of(999L, 5);  // question 999 not in map
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, Map.of());

        assertThat(vector).hasSize(31);
        for (double v : vector) {
            assertThat(v).isEqualTo(FeatureAggregationService.NEUTRAL_VALUE, within(1e-9));
        }
    }

    // ─── Feature ordering tests ───────────────────────────────────────────────

    @Test
    @DisplayName("Feature at index 0 is DIM_REALISTIC")
    void featureOrdering_index0_isRealistic() {
        assertThat(DIM_REALISTIC).isEqualTo(0);
        Question q = questionWith(1L, TAG_DIM_REALISTIC);
        Map<Long, Integer> answers = Map.of(1L, 5);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));
        assertThat(vector[0]).isEqualTo(1.0, within(1e-9)); // index 0 = REALISTIC
        assertThat(vector[1]).isEqualTo(0.5, within(1e-9)); // index 1 = INVESTIGATIVE (untouched)
    }

    @Test
    @DisplayName("Feature at index 10 is DIM_OFFENSE_DEF")
    void featureOrdering_index10_isOffenseDef() {
        assertThat(DIM_OFFENSE_DEF).isEqualTo(10);
        Question q = questionWith(1L, TAG_DIM_OFFENSE_DEF);
        Map<Long, Integer> answers = Map.of(1L, 1);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));
        assertThat(vector[10]).isEqualTo(0.0, within(1e-9));
    }

    @Test
    @DisplayName("Feature at index 11 is TECH_SERVER_LOGIC (first technical feature)")
    void featureOrdering_index11_isServerLogic() {
        assertThat(TECH_SERVER_LOGIC).isEqualTo(11);
        Question q = questionWith(1L, TAG_TECH_SERVER_LOGIC);
        Map<Long, Integer> answers = Map.of(1L, 5);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));
        assertThat(vector[11]).isEqualTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("Feature at index 30 is TECH_FULL_SPECTRUM (last feature)")
    void featureOrdering_index30_isFullSpectrum() {
        assertThat(TECH_FULL_SPECTRUM).isEqualTo(30);
        Question q = questionWith(1L, TAG_TECH_FULL_SPECTRUM);
        Map<Long, Integer> answers = Map.of(1L, 5);
        double[] vector = FeatureAggregationService.buildFeatureVectorFromMaps(answers, questionMap(q));
        assertThat(vector[30]).isEqualTo(1.0, within(1e-9));
    }

    // ─── Clamping tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Normalize clamps below 0.0 to 0.0")
    void normalize_clampsBelow() {
        double result = FeatureAggregationService.normalize(0.0); // below min=1
        assertThat(result).isEqualTo(0.0, within(1e-9));
    }

    @Test
    @DisplayName("Normalize clamps above 1.0 to 1.0")
    void normalize_clampsAbove() {
        double result = FeatureAggregationService.normalize(6.0); // above max=5
        assertThat(result).isEqualTo(1.0, within(1e-9));
    }
}
