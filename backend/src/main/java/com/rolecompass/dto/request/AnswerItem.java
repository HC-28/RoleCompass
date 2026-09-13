package com.rolecompass.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single question-answer pair within an {@link AnswerRequest}.
 *
 * <p>Constraints are enforced by Jakarta Bean Validation — if the client
 * omits a field or sends an out-of-range Likert value, Spring returns HTTP 400
 * automatically without reaching service code.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerItem {

    @NotNull(message = "Question ID is required")
    @JsonProperty("question_id")
    private Long questionId;

    @NotNull(message = "Likert value is required")
    @Min(value = 1, message = "Likert value must be at least 1")
    @Max(value = 5, message = "Likert value must be at most 5")
    @JsonProperty("likert_value")
    private Integer likertValue;
}
