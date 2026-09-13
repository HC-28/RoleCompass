package com.rolecompass.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response body for {@code POST /api/auth/register} and {@code POST /api/auth/login}.
 *
 * <p>The token is a signed JWT. Clients must include it as
 * {@code Authorization: Bearer <token>} on all authenticated endpoints.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";

    @JsonProperty("user_id")
    private UUID userId;

    private String email;
}
