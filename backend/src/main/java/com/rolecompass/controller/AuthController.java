package com.rolecompass.controller;

import com.rolecompass.dto.request.LoginRequest;
import com.rolecompass.dto.request.RegisterRequest;
import com.rolecompass.dto.response.AuthResponse;
import com.rolecompass.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController — stateless authentication endpoints.
 *
 * <ul>
 *   <li>{@code POST /api/auth/register} — create account, returns JWT</li>
 *   <li>{@code POST /api/auth/login}    — authenticate, returns JWT</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
