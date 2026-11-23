package com.fintech.transaction.service;

import com.fintech.transaction.dto.*;
import com.fintech.transaction.model.Transaction;
import com.fintech.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class TransactionProcessingService {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessingService.class);

    private final TransactionRepository transactionRepository;
    private final WebClient riskWebClient;
    private final WebClient auditWebClient;
    private final WebClient notificationWebClient;

    public TransactionProcessingService(
            TransactionRepository transactionRepository,
            WebClient.Builder webClientBuilder,
            @Value("${services.risk-service.url:http://localhost:8084}") String riskServiceUrl,
            @Value("${services.audit-service.url:http://localhost:8086}") String auditServiceUrl,
            @Value("${services.notification-service.url:http://localhost:8085}") String notificationServiceUrl) {
        this.transactionRepository = transactionRepository;
        this.riskWebClient = webClientBuilder.baseUrl(riskServiceUrl).build();
        this.auditWebClient = webClientBuilder.baseUrl(auditServiceUrl).build();
        this.notificationWebClient = webClientBuilder.baseUrl(notificationServiceUrl).build();
    }

    /**
     * Traitement asynchrone d'une transaction de production (§2.2 Étape 2 & 3).
     * 1. Sauvegarde immédiate avec statut PENDING (traçabilité garantie).
     * 2. Déclenchement non-bloquant de l'évaluation de risque via risk-service.
     */
    public Transaction createAndDispatch(TransactionRequest request) {
        String shortId = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String txId = "TX-" + shortId;

        Transaction transaction = Transaction.builder()
                .id(txId)
                .accountId(request.getAccountId() != null ? request.getAccountId() : "ACC-5521")
                .amount(request.getAmount() != null ? request.getAmount() : new BigDecimal("100.00"))
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .channel(request.getChannel() != null ? request.getChannel() : "card")
                .merchantName(request.getMerchantName() != null ? request.getMerchantName() : "Marchand")
                .merchantCategory(request.getMerchantCategory() != null ? request.getMerchantCategory() : "retail")
                .ipAddress(request.getIpAddress() != null ? request.getIpAddress() : "127.0.0.1")
                .status("PENDING")
                .createdAt(Instant.now())
                .build();

        Transaction savedTx = transactionRepository.save(transaction);

        // Appel asynchrone / non bloquant à risk-service
        dispatchRiskEvaluationAsync(savedTx, request.getFeatures());

        return savedTx;
    }

    /**
     * Appel asynchrone à risk-service puis cascade vers audit-service et notification-service
     */
    private void dispatchRiskEvaluationAsync(Transaction tx, Map<String, Double> features) {
        RiskScoreClientRequest riskRequest = RiskScoreClientRequest.builder()
                .txId(tx.getId())
                .accountId(tx.getAccountId())
                .customerId("CUST-10428")
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .channel(tx.getChannel())
                .merchantCategory(tx.getMerchantCategory())
                .ipAddress(tx.getIpAddress())
                .features(features)
                .build();

        riskWebClient.post()
                .uri("/internal/v1/risk/score")
                .bodyValue(riskRequest)
                .retrieve()
                .bodyToMono(RiskScoreClientResponse.class)
                .timeout(Duration.ofMillis(2000))
                .subscribe(
                        riskResponse -> {
                            tx.setStatus("EVALUATED");
                            tx.setDecision(riskResponse.getDecision());
                            tx.setRiskScore(riskResponse.getRiskScore());
                            transactionRepository.save(tx);
                            log.info("Transaction {} évaluée avec succès : décision={}", tx.getId(), riskResponse.getDecision());

                            // Cascade événementielle asynchrone
                            dispatchAuditAndNotification(tx, riskResponse.getDecision(), riskResponse.getRiskScore(), riskResponse.getAuditId());
                        },
                        error -> {
                            log.warn("Échec d'évaluation du risque pour {} : {}, bascule en fallback", tx.getId(), error.getMessage());
                            tx.setStatus("EVALUATED");
                            tx.setDecision("HUMAN_REVIEW"); // Fallback dégradé sécurisé
                            tx.setRiskScore(50);
                            transactionRepository.save(tx);

                            dispatchAuditAndNotification(tx, "HUMAN_REVIEW", 50, "a-fallback");
                        }
                );
    }

    /**
     * Simulateur de paiement synchrone pour la démonstration (Maquette 6 du document)
     */
    public SimulationResponse simulate(SimulationRequest simRequest) {
        long startTime = System.currentTimeMillis();
        String scenario = simRequest.getScenario() != null ? simRequest.getScenario().toLowerCase() : "uncertain";

        String shortId = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String txId = "TX-" + shortId;

        BigDecimal amount = simRequest.getAmount();
        String merchantCategory = simRequest.getMerchantCategory();
        Map<String, Double> features = simRequest.getFeatures() != null ? simRequest.getFeatures() : new HashMap<>();

        // Pré-remplissage selon le scénario (Maquette 6)
        if ("normal".equals(scenario)) {
            if (amount == null) amount = new BigDecimal("12.40");
            if (merchantCategory == null) merchantCategory = "bakery_groceries";
            if (!features.containsKey("V14")) features.put("V14", 1.25);
            if (!features.containsKey("V17")) features.put("V17", -0.85);
        } else if ("fraud".equals(scenario)) {
            if (amount == null) amount = new BigDecimal("8000.00");
            if (merchantCategory == null) merchantCategory = "wire_transfer_foreign";
            if (!features.containsKey("V14")) features.put("V14", -4.85);
            if (!features.containsKey("V17")) features.put("V17", 3.42);
        } else { // uncertain par défaut
            if (amount == null) amount = new BigDecimal("550.00");
            if (merchantCategory == null) merchantCategory = "electronics_new";
            if (!features.containsKey("V14")) features.put("V14", -0.3111);
            if (!features.containsKey("V17")) features.put("V17", 0.2079);
        }
        features.put("Amount", amount.doubleValue());

        // Sauvegarde de la transaction
        Transaction transaction = Transaction.builder()
                .id(txId)
                .accountId("ACC-5521")
                .amount(amount)
                .currency(simRequest.getCurrency() != null ? simRequest.getCurrency() : "EUR")
                .channel("card")
                .merchantName("Simulateur " + scenario)
                .merchantCategory(merchantCategory)
                .ipAddress("194.22.10.5")
                .status("EVALUATED")
                .createdAt(Instant.now())
                .build();

        RiskScoreClientRequest riskRequest = RiskScoreClientRequest.builder()
                .txId(txId)
                .accountId(transaction.getAccountId())
                .customerId("CUST-10428")
                .amount(amount)
                .currency(transaction.getCurrency())
                .channel(transaction.getChannel())
                .merchantCategory(merchantCategory)
                .ipAddress(transaction.getIpAddress())
                .features(features)
                .build();

        RiskScoreClientResponse riskResponse;
        try {
            riskResponse = riskWebClient.post()
                    .uri("/internal/v1/risk/score")
                    .bodyValue(riskRequest)
                    .retrieve()
                    .bodyToMono(RiskScoreClientResponse.class)
                    .timeout(Duration.ofMillis(1500))
                    .block();
        } catch (Exception e) {
            log.warn("Appel risk-service en échec pour simulation, bascule en réponse locale : {}", e.getMessage());
            riskResponse = RiskScoreClientResponse.builder()
                    .txId(txId)
                    .decision("uncertain".equals(scenario) ? "HUMAN_REVIEW" : ("fraud".equals(scenario) ? "BLOCKED" : "APPROVED"))
                    .riskScore("uncertain".equals(scenario) ? 52 : ("fraud".equals(scenario) ? 97 : 4))
                    .riskBand("uncertain".equals(scenario) ? "MEDIUM" : ("fraud".equals(scenario) ? "CRITICAL" : "LOW"))
                    .modelVersion("v4.2")
                    .uncertainty("uncertain".equals(scenario) ? 0.34 : 0.02)
                    .auditId("a-" + UUID.randomUUID().toString().substring(0, 5))
                    .decidedAt(Instant.now())
                    .build();
        }

        transaction.setDecision(riskResponse.getDecision());
        transaction.setRiskScore(riskResponse.getRiskScore());
        transactionRepository.save(transaction);

        // Diffusion asynchrone audit et websocket
        dispatchAuditAndNotification(transaction, riskResponse.getDecision(), riskResponse.getRiskScore(), riskResponse.getAuditId());

        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        return SimulationResponse.builder()
                .txId(txId)
                .status(riskResponse.getDecision())
                .probability(riskResponse.getRiskScore() != null ? riskResponse.getRiskScore() / 100.0 : 0.5)
                .uncertaintyScore(riskResponse.getUncertainty())
                .isUncertain("HUMAN_REVIEW".equals(riskResponse.getDecision()))
                .conformalInterval(List.of(0.48, 0.56))
                .modelVersion(riskResponse.getModelVersion() != null ? riskResponse.getModelVersion() : "v4.2")
                .latencyMs(latencyMs > 0 ? latencyMs : 38)
                .auditId(riskResponse.getAuditId())
                .build();
    }

    /**
     * Dispatch événementiel vers audit-service et notification-service
     */
    private void dispatchAuditAndNotification(Transaction tx, String decision, Integer riskScore, String auditId) {
        // 1. Audit log
        try {
            Map<String, Object> auditPayload = Map.of(
                    "transaction_id", tx.getId(),
                    "event_type", "RISK_EVALUATED",
                    "payload_json", String.format("{\"decision\":\"%s\",\"risk_score\":%d,\"amount\":%s}", decision, riskScore != null ? riskScore : 0, tx.getAmount()),
                    "actor", "SYSTEM"
            );
            auditWebClient.post()
                    .uri("/api/v1/audit/log")
                    .bodyValue(auditPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(1000))
                    .subscribe(
                            res -> log.debug("Audit log envoyé avec succès pour {}", tx.getId()),
                            err -> log.debug("Audit service non joignable (normal en local sans container): {}", err.getMessage())
                    );
        } catch (Exception e) {
            log.debug("Exception dispatch audit: {}", e.getMessage());
        }

        // 2. Notification WebSocket temps réel
        try {
            Map<String, Object> notifPayload = Map.of(
                    "type", "TX_DECIDED",
                    "tx_id", tx.getId(),
                    "client_name", "Client FinTech",
                    "amount", tx.getAmount() + " " + tx.getCurrency(),
                    "channel", tx.getChannel(),
                    "decision", decision,
                    "risk_score", riskScore != null ? riskScore : 0,
                    "audit_id", auditId != null ? auditId : "a-88213",
                    "timestamp", Instant.now().toString()
            );
            notificationWebClient.post()
                    .uri("/api/v1/notifications/broadcast")
                    .bodyValue(notifPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(1000))
                    .subscribe(
                            res -> log.debug("Notification diffusée avec succès pour {}", tx.getId()),
                            err -> log.debug("Notification service non joignable (normal en local sans container): {}", err.getMessage())
                    );
        } catch (Exception e) {
            log.debug("Exception dispatch notification: {}", e.getMessage());
        }
    }
}
