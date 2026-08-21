package com.rolecompass.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
