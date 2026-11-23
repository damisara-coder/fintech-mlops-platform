package com.fintech.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;

    @JsonProperty("refresh_token")
    private String refreshToken;

    private String role;
    private String email;
    private String organization;

    @JsonProperty("expires_in")
    private Long expiresIn;
}
