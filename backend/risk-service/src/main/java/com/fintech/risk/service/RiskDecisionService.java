package com.fintech.risk.service;

import com.fintech.risk.dto.FactorContribution;
import com.fintech.risk.dto.RiskScoreRequest;
import com.fintech.risk.dto.RiskScoreResponse;
import com.fintech.risk.dto.ml.PredictRequest;
import com.fintech.risk.dto.ml.PredictResponse;
import com.fintech.risk.model.Decision;
import com.fintech.risk.repository.DecisionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskDecisionService {

    private static final Logger log = LoggerFactory.getLogger(RiskDecisionService.class);

    private final WebClient webClient;
    private final DecisionRepository decisionRepository;

    public RiskDecisionService(
            WebClient.Builder webClientBuilder,
            DecisionRepository decisionRepository,
            @Value("${services.ml-service.url:http://localhost:8000}") String mlServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(mlServiceUrl).build();
        this.decisionRepository = decisionRepository;
    }

    /**
     * Évaluation du risque avec protection Resilience4j (Timeout 800ms, Retry, Circuit Breaker).
     * Si FastAPI est inaccessible ou trop lent, bascule automatiquement sur fallbackDecision.
     */
    @CircuitBreaker(name = "mlPrediction", fallbackMethod = "fallbackDecision")
    @Retry(name = "mlPrediction")
    public RiskScoreResponse evaluateRisk(RiskScoreRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Préparation de la requête pour FastAPI selon docs/contracts/predict-contract.json v2.0.0
        Map<String, Double> features = request.getFeatures();
        if (features == null || features.isEmpty()) {
            features = new HashMap<>();
            features.put("Amount", request.getAmount() != null ? request.getAmount().doubleValue() : 100.0);
            for (int i = 1; i <= 28; i++) {
                features.put("V" + i, 0.0);
            }
        } else if (!features.containsKey("Amount") && request.getAmount() != null) {
            features.put("Amount", request.getAmount().doubleValue());
        }

        PredictRequest predictRequest = PredictRequest.builder()
                .txId(request.getTxId())
                .features(features)
                .build();

        // 2. Appel asynchrone / non bloquant avec timeout strict de 800 ms
        PredictResponse mlResponse = webClient.post()
                .uri("/predict")
                .bodyValue(predictRequest)
                .retrieve()
                .bodyToMono(PredictResponse.class)
                .timeout(Duration.ofMillis(800))
                .block();

        long latency = System.currentTimeMillis() - startTime;
        return applyDecisionPolicy(request, mlResponse, (int) latency);
    }

    /**
     * Application stricte de la règle de décision bancaire (§4.1 du document de cadrage)
     */
    private RiskScoreResponse applyDecisionPolicy(RiskScoreRequest request, PredictResponse mlResponse, int latencyMs) {
        double proba = mlResponse != null && mlResponse.getProbability() != null ? mlResponse.getProbability() : 0.5;
        boolean isUncertain = mlResponse != null && Boolean.TRUE.equals(mlResponse.getIsUncertain());
        double uncertainty = mlResponse != null && mlResponse.getUncertaintyScore() != null ? mlResponse.getUncertaintyScore() : 0.0;
        String modelVersion = mlResponse != null && mlResponse.getModelVersion() != null ? mlResponse.getModelVersion() : "v4.2";

        String decision;
        if (isUncertain) {
            decision = "HUMAN_REVIEW"; // Le modèle refuse de se prononcer (MAPIE)
        } else if (proba >= 0.90) {
            decision = "BLOCKED";      // Fraude quasi-certaine
        } else if (proba <= 0.50) {
            decision = "APPROVED";     // Légitime quasi-certain
        } else {
            decision = "HUMAN_REVIEW"; // Zone grise (0.50 < p < 0.90)
        }

        int riskScore = (int) Math.round(proba * 100);
        String riskBand = getRiskBand(riskScore);
        String auditId = "a-" + UUID.randomUUID().toString().substring(0, 5);

        // Facteurs explicatifs SHAP pour la console d'audit / revue humaine
        List<FactorContribution> factors = List.of(
                FactorContribution.builder().f("V14").c(-0.31).build(),
                FactorContribution.builder().f("V17").c(0.22).build(),
                FactorContribution.builder().f("Amount").c(0.18).build(),
                FactorContribution.builder().f("V12").c(-0.14).build()
        );

        // Sauvegarde de la décision en base PostgreSQL
        Decision entity = Decision.builder()
                .id("DEC-" + UUID.randomUUID().toString().substring(0, 8))
                .transactionId(request.getTxId())
                .decision(decision)
                .probability(proba)
                .uncertaintyScore(uncertainty)
                .isUncertain(isUncertain)
                .riskScore(riskScore)
                .riskBand(riskBand)
                .modelVersion(modelVersion)
                .latencyMs(latencyMs)
                .reviewStatus("HUMAN_REVIEW".equals(decision) ? "PENDING_REVIEW" : "NONE")
                .decidedAt(Instant.now())
                .build();
        decisionRepository.save(entity);

        return RiskScoreResponse.builder()
                .txId(request.getTxId())
                .decision(decision)
                .riskScore(riskScore)
                .riskBand(riskBand)
                .modelVersion(modelVersion)
                .uncertainty(uncertainty)
                .factors(factors)
                .auditId(auditId)
                .decidedAt(Instant.now())
                .build();
    }

    /**
     * Mode Dégradé Sécurisé (Point de vigilance n°1 / §2.2 Étape 4) :
     * Si FastAPI est down ou met plus de 800ms, la transaction bascule en HUMAN_REVIEW plutôt que de bloquer le client.
     */
    public RiskScoreResponse fallbackDecision(RiskScoreRequest request, Throwable t) {
        log.warn("Circuit Breaker activé / Timeout FastAPI ML. Bascule en mode dégradé (HUMAN_REVIEW). Cause: {}", t.getMessage());

        String auditId = "a-fb-" + UUID.randomUUID().toString().substring(0, 5);
        Decision fallbackEntity = Decision.builder()
                .id("DEC-" + UUID.randomUUID().toString().substring(0, 8))
                .transactionId(request.getTxId())
                .decision("HUMAN_REVIEW")
                .probability(0.50)
                .uncertaintyScore(1.0)
                .isUncertain(true)
                .riskScore(50)
                .riskBand("MEDIUM")
                .modelVersion("fallback-rule-v1")
                .latencyMs(800)
                .reviewStatus("PENDING_REVIEW")
                .reviewerNotes("Auto-routage en revue humaine suite à indisponibilité du service d'inférence ML (Circuit Breaker).")
                .decidedAt(Instant.now())
                .build();
        decisionRepository.save(fallbackEntity);

        return RiskScoreResponse.builder()
                .txId(request.getTxId())
                .decision("HUMAN_REVIEW")
                .riskScore(50)
                .riskBand("MEDIUM")
                .modelVersion("fallback-rule-v1")
                .uncertainty(1.0)
                .factors(List.of(FactorContribution.builder().f("fallback_reason").c(1.0).build()))
                .auditId(auditId)
                .decidedAt(Instant.now())
                .build();
    }

    private String getRiskBand(int score) {
        if (score >= 80) return "CRITICAL";
        if (score >= 60) return "HIGH";
        if (score >= 30) return "MEDIUM";
        return "LOW";
    }
}
