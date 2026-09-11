package com.rolecompass.aggregation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests that verify the FeatureIndex constants are frozen and correct.
 * These tests MUST FAIL if anyone reorders or changes feature indices.
 */
@DisplayName("FeatureIndex — frozen constants")
class FeatureIndexTest {

    @Test
    @DisplayName("TOTAL_FEATURES is exactly 31")
    void totalFeatures_is31() {
        assertThat(FeatureIndex.TOTAL_FEATURES).isEqualTo(31);
    }

    @Test
    @DisplayName("PSYCH dimensions span indices 0..10 (inclusive)")
    void psychRange() {
        assertThat(FeatureIndex.PSYCH_START).isEqualTo(0);
        assertThat(FeatureIndex.PSYCH_END).isEqualTo(11);
        assertThat(FeatureIndex.PSYCH_END - FeatureIndex.PSYCH_START).isEqualTo(11);
    }

    @Test
    @DisplayName("TECH features span indices 11..30 (inclusive)")
    void techRange() {
        assertThat(FeatureIndex.TECH_START).isEqualTo(11);
        assertThat(FeatureIndex.TECH_END).isEqualTo(31);
        assertThat(FeatureIndex.TECH_END - FeatureIndex.TECH_START).isEqualTo(20);
    }

    @Test
    @DisplayName("Psychological dimension indices are exactly 0-10")
    void psychIndices_exact() {
        assertThat(FeatureIndex.DIM_REALISTIC).isEqualTo(0);
        assertThat(FeatureIndex.DIM_INVESTIGATIVE).isEqualTo(1);
        assertThat(FeatureIndex.DIM_ARTISTIC).isEqualTo(2);
        assertThat(FeatureIndex.DIM_SOCIAL).isEqualTo(3);
        assertThat(FeatureIndex.DIM_ENTERPRISING).isEqualTo(4);
        assertThat(FeatureIndex.DIM_CONVENTIONAL).isEqualTo(5);
        assertThat(FeatureIndex.DIM_DATA_IDEAS).isEqualTo(6);
        assertThat(FeatureIndex.DIM_THINGS_PEOPLE).isEqualTo(7);
        assertThat(FeatureIndex.DIM_BREADTH_DEPTH).isEqualTo(8);
        assertThat(FeatureIndex.DIM_STRUCT_AMBIG).isEqualTo(9);
        assertThat(FeatureIndex.DIM_OFFENSE_DEF).isEqualTo(10);
    }

    @Test
    @DisplayName("Technical feature indices are exactly 11-30")
    void techIndices_exact() {
        assertThat(FeatureIndex.TECH_SERVER_LOGIC).isEqualTo(11);
        assertThat(FeatureIndex.TECH_DATA_STORAGE).isEqualTo(12);
        assertThat(FeatureIndex.TECH_API_DESIGN).isEqualTo(13);
        assertThat(FeatureIndex.TECH_UI_RENDERING).isEqualTo(14);
        assertThat(FeatureIndex.TECH_STATE_MGMT).isEqualTo(15);
        assertThat(FeatureIndex.TECH_BUILD_PIPELINE).isEqualTo(16);
        assertThat(FeatureIndex.TECH_INFRA_PROVISION).isEqualTo(17);
        assertThat(FeatureIndex.TECH_CONTAINER_ORCH).isEqualTo(18);
        assertThat(FeatureIndex.TECH_CLOUD_SERVICES).isEqualTo(19);
        assertThat(FeatureIndex.TECH_STAT_ANALYSIS).isEqualTo(20);
        assertThat(FeatureIndex.TECH_MODEL_BUILDING).isEqualTo(21);
        assertThat(FeatureIndex.TECH_DATA_PIPELINE).isEqualTo(22);
        assertThat(FeatureIndex.TECH_MOBILE_CLIENT).isEqualTo(23);
        assertThat(FeatureIndex.TECH_THREAT_ANALYSIS).isEqualTo(24);
        assertThat(FeatureIndex.TECH_SYSTEM_HARDENING).isEqualTo(25);
        assertThat(FeatureIndex.TECH_TEST_DESIGN).isEqualTo(26);
        assertThat(FeatureIndex.TECH_TEST_AUTOMATION).isEqualTo(27);
        assertThat(FeatureIndex.TECH_OBSERVABILITY).isEqualTo(28);
        assertThat(FeatureIndex.TECH_PERF_OPTIM).isEqualTo(29);
        assertThat(FeatureIndex.TECH_FULL_SPECTRUM).isEqualTo(30);
    }

    @Test
    @DisplayName("indexForTag returns correct index for all 31 tags")
    void indexForTag_allTags() {
        assertThat(FeatureIndex.indexForTag("DIM_REALISTIC")).isEqualTo(0);
        assertThat(FeatureIndex.indexForTag("DIM_INVESTIGATIVE")).isEqualTo(1);
        assertThat(FeatureIndex.indexForTag("DIM_ARTISTIC")).isEqualTo(2);
        assertThat(FeatureIndex.indexForTag("DIM_SOCIAL")).isEqualTo(3);
        assertThat(FeatureIndex.indexForTag("DIM_ENTERPRISING")).isEqualTo(4);
        assertThat(FeatureIndex.indexForTag("DIM_CONVENTIONAL")).isEqualTo(5);
        assertThat(FeatureIndex.indexForTag("DIM_DATA_IDEAS")).isEqualTo(6);
        assertThat(FeatureIndex.indexForTag("DIM_THINGS_PEOPLE")).isEqualTo(7);
        assertThat(FeatureIndex.indexForTag("DIM_BREADTH_DEPTH")).isEqualTo(8);
        assertThat(FeatureIndex.indexForTag("DIM_STRUCT_AMBIG")).isEqualTo(9);
        assertThat(FeatureIndex.indexForTag("DIM_OFFENSE_DEF")).isEqualTo(10);
        assertThat(FeatureIndex.indexForTag("TECH_SERVER_LOGIC")).isEqualTo(11);
        assertThat(FeatureIndex.indexForTag("TECH_DATA_STORAGE")).isEqualTo(12);
        assertThat(FeatureIndex.indexForTag("TECH_API_DESIGN")).isEqualTo(13);
        assertThat(FeatureIndex.indexForTag("TECH_UI_RENDERING")).isEqualTo(14);
        assertThat(FeatureIndex.indexForTag("TECH_STATE_MGMT")).isEqualTo(15);
        assertThat(FeatureIndex.indexForTag("TECH_BUILD_PIPELINE")).isEqualTo(16);
        assertThat(FeatureIndex.indexForTag("TECH_INFRA_PROVISION")).isEqualTo(17);
        assertThat(FeatureIndex.indexForTag("TECH_CONTAINER_ORCH")).isEqualTo(18);
        assertThat(FeatureIndex.indexForTag("TECH_CLOUD_SERVICES")).isEqualTo(19);
        assertThat(FeatureIndex.indexForTag("TECH_STAT_ANALYSIS")).isEqualTo(20);
        assertThat(FeatureIndex.indexForTag("TECH_MODEL_BUILDING")).isEqualTo(21);
        assertThat(FeatureIndex.indexForTag("TECH_DATA_PIPELINE")).isEqualTo(22);
        assertThat(FeatureIndex.indexForTag("TECH_MOBILE_CLIENT")).isEqualTo(23);
        assertThat(FeatureIndex.indexForTag("TECH_THREAT_ANALYSIS")).isEqualTo(24);
        assertThat(FeatureIndex.indexForTag("TECH_SYSTEM_HARDENING")).isEqualTo(25);
        assertThat(FeatureIndex.indexForTag("TECH_TEST_DESIGN")).isEqualTo(26);
        assertThat(FeatureIndex.indexForTag("TECH_TEST_AUTOMATION")).isEqualTo(27);
        assertThat(FeatureIndex.indexForTag("TECH_OBSERVABILITY")).isEqualTo(28);
        assertThat(FeatureIndex.indexForTag("TECH_PERF_OPTIM")).isEqualTo(29);
        assertThat(FeatureIndex.indexForTag("TECH_FULL_SPECTRUM")).isEqualTo(30);
    }

    @Test
    @DisplayName("indexForTag returns -1 for unknown tags")
    void indexForTag_unknownTag() {
        assertThat(FeatureIndex.indexForTag("UNKNOWN")).isEqualTo(-1);
        assertThat(FeatureIndex.indexForTag("")).isEqualTo(-1);
        assertThat(FeatureIndex.indexForTag("dim_realistic")).isEqualTo(-1); // case-sensitive
    }
}
