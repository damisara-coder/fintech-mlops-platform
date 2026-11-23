package com.fintech.risk;

import com.fintech.risk.dto.RiskScoreRequest;
import com.fintech.risk.dto.RiskScoreResponse;
import com.fintech.risk.dto.ml.PredictResponse;
import com.fintech.risk.model.Decision;
import com.fintech.risk.repository.DecisionRepository;
import com.fintech.risk.service.RiskDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RiskDecisionServiceTest {

    private DecisionRepository decisionRepository;
    private RiskDecisionService riskService;

    @BeforeEach
    void setUp() {
        decisionRepository = Mockito.mock(DecisionRepository.class);
        WebClient.Builder webClientBuilder = WebClient.builder();
        riskService = new RiskDecisionService(webClientBuilder, decisionRepository, "http://localhost:8000");

        when(decisionRepository.save(any(Decision.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Règle §4.1 : Incertitude élevée -> HUMAN_REVIEW obligatoire")
    void testRuleUncertaintyForcesHumanReview() {
        RiskScoreRequest request = RiskScoreRequest.builder()
                .txId("TX-101")
                .accountId("ACC-1")
                .customerId("CUST-1")
                .amount(new BigDecimal("500.00"))
                .currency("EUR")
                .channel("card")
                .build();

        PredictResponse mlResponse = PredictResponse.builder()
                .probability(0.20) // Probabilité faible mais incertitude déclarée
                .uncertaintyScore(0.35)
                .isUncertain(true)
                .interval(List.of(0.10, 0.45))
                .modelVersion("v4.2")
                .build();

        RiskScoreResponse response = riskService.applyDecisionPolicy(request, mlResponse, 42);

        assertNotNull(response);
        assertEquals("HUMAN_REVIEW", response.getDecision());
        assertEquals(0.35, response.getUncertainty());
        assertNotNull(response.getAuditId());
    }

    @Test
    @DisplayName("Règle §4.1 : Probabilité >= 0.90 et certain -> BLOCKED (Fraude quasi-certaine)")
    void testRuleHighProbabilityBlocked() {
        RiskScoreRequest request = RiskScoreRequest.builder()
                .txId("TX-102")
                .amount(new BigDecimal("4500.00"))
                .build();

        PredictResponse mlResponse = PredictResponse.builder()
                .probability(0.96)
                .uncertaintyScore(0.01)
                .isUncertain(false)
                .interval(List.of(0.94, 0.98))
                .modelVersion("v4.2")
                .build();

        RiskScoreResponse response = riskService.applyDecisionPolicy(request, mlResponse, 28);

        assertEquals("BLOCKED", response.getDecision());
        assertEquals(96, response.getRiskScore());
        assertEquals("CRITICAL", response.getRiskBand());
    }

    @Test
    @DisplayName("Règle §4.1 : Probabilité <= 0.50 et certain -> APPROVED (Transaction légitime)")
    void testRuleLowProbabilityApproved() {
        RiskScoreRequest request = RiskScoreRequest.builder()
                .txId("TX-103")
                .amount(new BigDecimal("25.50"))
                .build();

        PredictResponse mlResponse = PredictResponse.builder()
                .probability(0.08)
                .uncertaintyScore(0.02)
                .isUncertain(false)
                .interval(List.of(0.05, 0.11))
                .modelVersion("v4.2")
                .build();

        RiskScoreResponse response = riskService.applyDecisionPolicy(request, mlResponse, 15);

        assertEquals("APPROVED", response.getDecision());
        assertEquals(8, response.getRiskScore());
        assertEquals("LOW", response.getRiskBand());
    }

    @Test
    @DisplayName("Règle §4.1 : Zone grise (0.50 < proba < 0.90) -> HUMAN_REVIEW")
    void testRuleGreyZoneHumanReview() {
        RiskScoreRequest request = RiskScoreRequest.builder()
                .txId("TX-104")
                .amount(new BigDecimal("350.00"))
                .build();

        PredictResponse mlResponse = PredictResponse.builder()
                .probability(0.68)
                .uncertaintyScore(0.08)
                .isUncertain(false)
                .interval(List.of(0.60, 0.76))
                .modelVersion("v4.2")
                .build();

        RiskScoreResponse response = riskService.applyDecisionPolicy(request, mlResponse, 35);

        assertEquals("HUMAN_REVIEW", response.getDecision());
        assertEquals(68, response.getRiskScore());
        assertEquals("HIGH", response.getRiskBand());
    }

    @Test
    @DisplayName("Résilience : En cas de timeout ou panne FastAPI, fallbackDecision() bascule en HUMAN_REVIEW")
    void testResilienceFallbackWhenFastApiTimesOut() {
        RiskScoreRequest request = RiskScoreRequest.builder()
                .txId("TX-TIMEOUT-999")
                .amount(new BigDecimal("800.00"))
                .features(Map.of("V1", 0.1, "Amount", 800.0))
                .build();

        Throwable timeoutException = new TimeoutException("Timeout FastAPI dépassé (> 800ms)");

        RiskScoreResponse fallbackResponse = riskService.fallbackDecision(request, timeoutException);

        assertNotNull(fallbackResponse);
        assertEquals("HUMAN_REVIEW", fallbackResponse.getDecision());
        assertEquals(50, fallbackResponse.getRiskScore());
        assertEquals("MEDIUM", fallbackResponse.getRiskBand());
        assertEquals("FALLBACK_OFFLINE", fallbackResponse.getModelVersion());
    }
}
