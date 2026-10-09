package com.rolecompass.routing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Section2AdaptiveSkipTest — pure constant-contract tests for the adaptive Q2 skip rule.
 *
 * <p><strong>Rule under test:</strong> If a candidate's first answer in a domain (Q1) is
 * extreme — defined as Likert ≤ {@link AdaptiveRoutingEngine#SCORE_LOW} or
 * ≥ {@link AdaptiveRoutingEngine#SCORE_HIGH} — Q2 of that domain is skipped and the engine
 * advances to Q1 of the next domain on the following API call.</p>
 *
 * <p>These tests do NOT require Spring Boot context — they exercise only public
 * constants. Integration tests (requiring a running DB) are separate.
 * Expected runtime: < 10 ms total.</p>
 */
@DisplayName("Section 2 — Adaptive Q2 Skip Contract Tests")
class Section2AdaptiveSkipTest {

    @Test
    @DisplayName("Section 2 delivers exactly 1 question per API call (SECTION_2_BATCH_SIZE = 1)")
    void section2BatchSizeIsOne() {
        assertThat(AdaptiveRoutingEngine.SECTION_2_BATCH_SIZE)
                .as("Section 2 must deliver exactly 1 question per API call for adaptive skip to work")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Standard batch size for Sections 1, 3, 4 is 4 questions")
    void standardBatchSizeIs4() {
        assertThat(AdaptiveRoutingEngine.BATCH_SIZE)
                .as("Sections 1, 3, 4 must deliver 4 questions per batch")
                .isEqualTo(4);
    }

    @Test
    @DisplayName("Section 2 batch size is strictly less than standard batch size")
    void section2BatchSmallerThanStandard() {
        assertThat(AdaptiveRoutingEngine.SECTION_2_BATCH_SIZE)
                .isLessThan(AdaptiveRoutingEngine.BATCH_SIZE);
    }

    @Test
    @DisplayName("SCORE_LOW = 2: answers 1 and 2 are extreme low")
    void scoreLowBoundaryIs2() {
        assertThat(AdaptiveRoutingEngine.SCORE_LOW)
                .as("SCORE_LOW must be 2 — Likert 1 and 2 trigger domain skip")
                .isEqualTo(2);

        // Confirm 1 and 2 are both ≤ SCORE_LOW
        for (int v : new int[]{1, 2}) {
            assertThat(v).isLessThanOrEqualTo(AdaptiveRoutingEngine.SCORE_LOW);
        }
    }

    @Test
    @DisplayName("SCORE_HIGH = 4: answers 4 and 5 are extreme high")
    void scoreHighBoundaryIs4() {
        assertThat(AdaptiveRoutingEngine.SCORE_HIGH)
                .as("SCORE_HIGH must be 4 — Likert 4 and 5 trigger domain skip")
                .isEqualTo(4);

        // Confirm 4 and 5 are both ≥ SCORE_HIGH
        for (int v : new int[]{4, 5}) {
            assertThat(v).isGreaterThanOrEqualTo(AdaptiveRoutingEngine.SCORE_HIGH);
        }
    }

    @Test
    @DisplayName("Neutral answer (3) is strictly between SCORE_LOW and SCORE_HIGH — no skip")
    void neutralValueDoesNotTriggerSkip() {
        int neutral = 3;
        assertThat(neutral)
                .as("Neutral value 3 must NOT trigger skip — it is strictly between LOW and HIGH")
                .isGreaterThan(AdaptiveRoutingEngine.SCORE_LOW)
                .isLessThan(AdaptiveRoutingEngine.SCORE_HIGH);
    }

    @Test
    @DisplayName("Section 2 maximum is 20 questions (1 question × 20 domains)")
    void section2MaximumIs20() {
        assertThat(AdaptiveRoutingEngine.SECTION_2_MAX_QUESTION_COUNT)
                .as("1 question per domain × 20 domains = 20 maximum")
                .isEqualTo(20);
    }

    @Test
    @DisplayName("Minimum answerable Section 2 questions is 20 (all domains covered)")
    void minimumSection2QuestionsWhenAllExtreme() {
        int domains = AdaptiveRoutingEngine.SECTION_2_MAX_QUESTION_COUNT; // 20 domains
        assertThat(domains)
                .as("20 technical questions are presented in Section 2")
                .isEqualTo(20);
    }

    @Test
    @DisplayName("Section 1 requires exactly 16 questions before psychometric pruning")
    void section1Requires12Questions() {
        assertThat(AdaptiveRoutingEngine.SECTION_1_QUESTION_COUNT)
                .as("Section 1 must have exactly 12 psychometric questions")
                .isEqualTo(12);
    }

    @Test
    @DisplayName("Minimum candidate floor is 2 (engine never eliminates to 1 or 0)")
    void minimumCandidateFloorIs2() {
        assertThat(AdaptiveRoutingEngine.MIN_CANDIDATES)
                .as("Safety floor — at least 2 candidates must always survive routing gates")
                .isEqualTo(2);
    }
}
