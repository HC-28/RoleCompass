package com.rolecompass.controller;

import com.rolecompass.dto.AnswerRequest;
import com.rolecompass.dto.PredictionResponse;
import com.rolecompass.dto.SessionStartResponse;
import com.rolecompass.entity.User;
import com.rolecompass.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<SessionStartResponse> startSession(
            @AuthenticationPrincipal User user
    ) {
        SessionStartResponse response = sessionService.startSession(user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/answers")
    public ResponseEntity<Map<String, Object>> submitAnswers(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AnswerRequest request
    ) {
        Map<String, Object> response = sessionService.submitAnswers(id, user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/predict")
    public ResponseEntity<PredictionResponse> predict(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User user
    ) {
        PredictionResponse response = sessionService.predict(id, user);
        return ResponseEntity.ok(response);
    }
}
