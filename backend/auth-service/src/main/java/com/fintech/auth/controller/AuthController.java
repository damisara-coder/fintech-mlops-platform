package com.fintech.auth.controller;

import com.fintech.auth.dto.AuthResponse;
import com.fintech.auth.dto.LoginRequest;
import com.fintech.auth.dto.RefreshTokenRequest;
import com.fintech.auth.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    public AuthController(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * Authentification utilisateur et émission du token JWT + Refresh Token
     * Supporte les 4 profils de démonstration (Maquette 1 & §3) :
     * - ANALYST   (amina@bank-fintech.com)
     * - RISK_OPS  (karim@bank-fintech.com)
     * - DEV       (sofia@bank-fintech.com / yanis@bank-fintech.com)
     * - ADMIN     (mehdi@bank-fintech.com)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String role;
        String defaultOrg = loginRequest.getOrganization() != null ? loginRequest.getOrganization() : "Risk Operations";

        if ("amina@bank-fintech.com".equalsIgnoreCase(email)) {
            role = "ANALYST";
        } else if ("karim@bank-fintech.com".equalsIgnoreCase(email)) {
            role = "RISK_OPS";
        } else if ("sofia@bank-fintech.com".equalsIgnoreCase(email) || "yanis@bank-fintech.com".equalsIgnoreCase(email)) {
            role = "DEV";
        } else if ("mehdi@bank-fintech.com".equalsIgnoreCase(email)) {
            role = "ADMIN";
        } else {
            role = "ANALYST";
        }

        String accessToken = tokenProvider.generateToken(email, role, defaultOrg);
        String refreshToken = tokenProvider.generateRefreshToken(email, role, defaultOrg);

        AuthResponse response = AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .role(role)
                .email(email)
                .organization(defaultOrg)
                .expiresIn(3600L)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Renouvellement du token d'accès via le refresh token (Tâche P1.3)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid or expired refresh token"
            ));
        }

        String email = tokenProvider.getUsernameFromToken(refreshToken);
        String role = tokenProvider.getRoleFromToken(refreshToken);
        String organization = tokenProvider.getOrganizationFromToken(refreshToken);

        String newAccessToken = tokenProvider.generateToken(email, role, organization);
        String newRefreshToken = tokenProvider.generateRefreshToken(email, role, organization);

        AuthResponse response = AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .role(role)
                .email(email)
                .organization(organization)
                .expiresIn(3600L)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Validation du token pour l'API Gateway et les microservices
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        boolean isValid = tokenProvider.validateToken(token);
        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "email", tokenProvider.getUsernameFromToken(token),
                    "role", tokenProvider.getRoleFromToken(token),
                    "organization", tokenProvider.getOrganizationFromToken(token) != null ? tokenProvider.getOrganizationFromToken(token) : ""
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }
    }
}
