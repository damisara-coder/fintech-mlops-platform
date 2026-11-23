package com.fintech.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Filtre Global d'Authentification JWT pour Spring Cloud Gateway (Tâche P1.5).
 * - Intercepte toutes les requêtes entrantes.
 * - Laisse passer les routes publiques (/api/v1/auth/**, /actuator/**, /ws/**, OPTIONS).
 * - Valide la signature cryptographique et l'expiration du JWT pour les routes sécurisées.
 * - Propage l'identité et les rôles dans les en-têtes HTTP 'X-User-Id' et 'X-User-Role'.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final SecretKey key;

    // Routes publiques exemptées de validation JWT
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/",
            "/actuator/",
            "/ws/"
    );

    public JwtAuthenticationFilter(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Autoriser les requêtes CORS pre-flight (OPTIONS)
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 2. Vérifier si le chemin est public
        for (String publicPrefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(publicPrefix)) {
                return chain.filter(exchange);
            }
        }

        // 3. Extraire l'en-tête Authorization
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Tentative d'accès non autorisée à '{}' : Token JWT manquant", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Token JWT manquant ou format invalide (Bearer requis)");
        }

        String token = authHeader.substring(7).trim();

        // 4. Valider le token JWT
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            log.debug("Authentification réussie pour '{}' (rôle: {}) sur '{}'", username, role, path);

            // 5. Propager l'identité de l'utilisateur aux microservices internes
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", username != null ? username : "anonymous")
                    .header("X-User-Role", role != null ? role : "ANALYST")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expiré pour la requête sur '{}'", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Token JWT expiré. Veuillez rafraîchir votre session via /api/v1/auth/refresh");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Signature JWT invalide pour la requête sur '{}' : {}", path, e.getMessage());
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Token JWT invalide ou corrompu");
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonError = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getURI().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(jsonError.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Priorité haute pour intercepter les requêtes avant le routage
        return -100;
    }
}
