package com.rolecompass.controller;

import com.rolecompass.dto.request.AnswerRequest;
import com.rolecompass.dto.response.PredictionResponse;
import com.rolecompass.dto.response.SessionStartResponse;
import com.rolecompass.entity.User;
import com.rolecompass.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * SessionController — REST entry points for the adaptive assessment session.
 *
 * <ul>
 *   <li>{@code POST /api/session/start}        — create a new session, receive first batch</li>
 *   <li>{@code POST /api/session/{id}/answers} — submit answers, receive next batch or completion</li>
 *   <li>{@code POST /api/session/{id}/predict} — trigger ML prediction for a completed session</li>
 *   <li>{@code GET  /api/session/profile}      — retrieve the authenticated user's latest result</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<SessionStartResponse> startSession(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(sessionService.startSession(user));
    }

    @PostMapping("/{id}/answers")
    public ResponseEntity<Map<String, Object>> submitAnswers(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AnswerRequest request
    ) {
        return ResponseEntity.ok(sessionService.submitAnswers(id, user, request));
    }

    @PostMapping("/{id}/predict")
    public ResponseEntity<PredictionResponse> predict(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(sessionService.predict(id, user));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(sessionService.getProfile(user));
    }
}
