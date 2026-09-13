package com.rolecompass.api;

import com.rolecompass.dto.request.AnswerItem;
import com.rolecompass.dto.request.AnswerRequest;
import com.rolecompass.dto.request.LoginRequest;
import com.rolecompass.dto.request.RegisterRequest;
import com.rolecompass.entity.User;
import com.rolecompass.repository.AnswerRepository;
import com.rolecompass.repository.QuestionRepository;
import com.rolecompass.repository.SessionRepository;
import com.rolecompass.repository.UserRepository;
import com.rolecompass.security.JwtService;
import com.rolecompass.service.AuthService;
import com.rolecompass.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("API & Service Unit Regression Tests")
@ExtendWith(MockitoExtension.class)
class ApiServiceRegressionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Register contract produces expected token response structure")
    void register_contract_matches() {
        RegisterRequest request = new RegisterRequest("test@rolecompass.com", "password123", null);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        var response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("test@rolecompass.com");
        assertThat(response.getUserId()).isNotNull();
    }

    @Test
    @DisplayName("Login contract produces expected token response structure")
    void login_contract_matches() {
        LoginRequest request = new LoginRequest("test@rolecompass.com", "password123");
        User mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@rolecompass.com")
                .password("encodedHash")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser)).thenReturn("mock-jwt-token-login");

        var response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock-jwt-token-login");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("test@rolecompass.com");
        assertThat(response.getUserId()).isEqualTo(mockUser.getId());
    }
}
