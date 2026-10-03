package com.rolecompass.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Client-facing representation of a single assessment question.
 *
 * <p><strong>Dumb-Client Rule:</strong> Internal dimension tags and trigger
 * predicates are intentionally excluded. Only text, options, and section-context
 * metadata (section_number, section_label, subsection_label) are returned.
 * All routing intelligence stays on the backend.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionDTO {

    /** Database primary key of the question. */
    private Long id;

    /** The question statement shown to the candidate. */
    private String text;

    /** Likert scale options: always [1, 2, 3, 4, 5]. Present for legacy compatibility. */
    @Builder.Default
    private List<Integer> options = List.of(1, 2, 3, 4, 5);

    /**
     * Response instrument type. Tells the frontend which widget component to render:
     * <ul>
     *   <li>{@code LIKERT_5}     — classic 5-point agreement scale (Section 1)</li>
     *   <li>{@code INTEREST_4}   — 4-point interest+exposure scale, no neutral (Sections 2 & 4)</li>
     *   <li>{@code PREFERENCE_4} — 4-point bipolar forced-choice scale (Section 3)</li>
     * </ul>
     */
    @JsonProperty("response_type")
    private String responseType;

    /** Section number (1–4) this question belongs to. */
    @JsonProperty("section_number")
    private Integer sectionNumber;

    /**
     * Human-readable section label, e.g. "Personality &amp; Work Style".
     * Null for sections where a label is not applicable.
     */
    @JsonProperty("section_label")
    private String sectionLabel;

    /**
     * Human-readable subsection label used within Section 2 only,
     * e.g. "Server Logic", "Data Storage". Null for Sections 1, 3, 4.
     */
    @JsonProperty("subsection_label")
    private String subsectionLabel;
}
