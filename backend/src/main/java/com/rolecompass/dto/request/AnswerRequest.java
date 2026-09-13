package com.rolecompass.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for {@code POST /api/session/{id}/answers}.
 *
 * <p>The {@code @Valid} annotation on the list propagates validation into
 * each {@link AnswerItem}, so individual constraint violations are surfaced
 * per item rather than swallowed at the list level.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {

    @NotEmpty(message = "Answers list must not be empty")
    @Valid
    private List<AnswerItem> answers;
}
