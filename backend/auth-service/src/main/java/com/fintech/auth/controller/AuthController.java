package com.fintech.auth.controller;

import com.fintech.auth.dto.AuthResponse;
import com.fintech.auth.dto.LoginRequest;
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
     * Authentification utilisateur et émission du token JWT
     * Supporte les 4 profils de démonstration (Maquette 1 & §3)
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
            // Rôle par défaut si utilisateur inconnu pour tester facilement
            role = "ANALYST";
        }

        String token = tokenProvider.generateToken(email, role, defaultOrg);

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .role(role)
                .email(email)
                .organization(defaultOrg)
                .expiresIn(3600L)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        boolean isValid = tokenProvider.validateToken(token);
        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "email", tokenProvider.getUsernameFromToken(token),
                    "role", tokenProvider.getRoleFromToken(token)
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }
    }
}
