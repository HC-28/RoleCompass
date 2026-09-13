package com.rolecompass.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response body for {@code POST /api/session/start}.
 *
 * <p>On session start the backend delivers the first batch of questions
 * (Section 1 — Personality &amp; Work Style, batch size = 4).
 * Subsequent batches are delivered via {@code POST /api/session/{id}/answers}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStartResponse {

    @JsonProperty("session_id")
    private UUID sessionId;

    private List<QuestionDTO> questions;
}
