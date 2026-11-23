package com.fintech.audit.service;

import com.fintech.audit.dto.AuditLogRequest;
import com.fintech.audit.dto.AuditVerificationResponse;
import com.fintech.audit.model.AuditLog;
import com.fintech.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service de journalisation immuable à hash chaîné SHA-256 (Tâche P1.6).
 * Garantit la non-répudiation et la détection infaillible de toute tentative de corruption en BDD.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Enregistre un événement dans le journal d'audit en calculant son hash chaîné SHA-256.
     * hash_n = SHA256(hash_{n-1} + ":" + eventType + ":" + txId + ":" + payload + ":" + timestamp)
     */
    @Transactional
    public synchronized AuditLog recordEvent(AuditLogRequest request) {
        Instant now = Instant.now();
        String auditId = "a-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Récupération du hash précédent (ou GENESIS si première entrée)
        Optional<AuditLog> lastLog = auditLogRepository.findTopByOrderByIdDesc();
        String previousHash = lastLog.map(AuditLog::getCurrentHash).orElse(GENESIS_HASH);

        // 2. Calcul du hash chaîné SHA-256
        String rawData = buildRawData(
                previousHash,
                request.getEventType(),
                request.getTransactionId(),
                request.getPayloadJson(),
                now.toEpochMilli()
        );
        String currentHash = calculateSha256(rawData);

        // 3. Persistance immuable
        AuditLog auditLog = AuditLog.builder()
                .auditId(auditId)
                .transactionId(request.getTransactionId())
                .eventType(request.getEventType() != null ? request.getEventType() : "GENERIC_EVENT")
                .payloadJson(request.getPayloadJson() != null ? request.getPayloadJson() : "{}")
                .previousHash(previousHash)
                .currentHash(currentHash)
                .actor(request.getActor() != null ? request.getActor() : "SYSTEM")
                .timestamp(now)
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log enregistré [id={}, auditId={}, tx={}, event={}, hash={}]",
                saved.getId(), saved.getAuditId(), saved.getTransactionId(), saved.getEventType(), saved.getCurrentHash());
        return saved;
    }

    /**
     * Récupère l'historique d'audit pour une transaction spécifique.
     */
    public List<AuditLog> getAuditTrail(String transactionId) {
        return auditLogRepository.findByTransactionIdOrderByTimestampAsc(transactionId);
    }

    /**
     * Récupère les 50 derniers événements d'audit pour la console de supervision.
     */
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    /**
     * Vérifie l'intégrité cryptographique complète du journal d'audit (Preuve de non-falsification).
     * Parcourt l'ensemble de la chaîne et recalcule chaque hash.
     */
    public AuditVerificationResponse verifyIntegrity() {
        List<AuditLog> allLogs = auditLogRepository.findAllByOrderByIdAsc();

        if (allLogs.isEmpty()) {
            return AuditVerificationResponse.builder()
                    .valid(true)
                    .totalRecordsChecked(0)
                    .lastHash(GENESIS_HASH)
                    .status("VERIFIED_EMPTY")
                    .details("Aucun enregistrement d'audit dans la base de données")
                    .build();
        }

        String expectedPreviousHash = GENESIS_HASH;

        for (int i = 0; i < allLogs.size(); i++) {
            AuditLog current = allLogs.get(i);

            // Vérification 1 : Le hash précédent correspond-il exactement au hash calculé du record précédent ?
            if (!current.getPreviousHash().equals(expectedPreviousHash)) {
                String errorMsg = String.format("Rupture de chaîne détectée à l'ID %d (tx: %s). previousHash attendu: %s, trouvé: %s",
                        current.getId(), current.getTransactionId(), expectedPreviousHash, current.getPreviousHash());
                log.error("ALERTE SÉCURITÉ AUDIT : {}", errorMsg);
                return AuditVerificationResponse.builder()
                        .valid(false)
                        .totalRecordsChecked(i + 1)
                        .corruptedRecordId(current.getId())
                        .status("CORRUPTION_DETECTED")
                        .details(errorMsg)
                        .build();
            }

            // Vérification 2 : Le hash actuel correspond-il au recalcul strict des données du record ?
            String recomputedHash = calculateSha256(buildRawData(
                    current.getPreviousHash(),
                    current.getEventType(),
                    current.getTransactionId(),
                    current.getPayloadJson(),
                    current.getTimestamp().toEpochMilli()
            ));

            if (!recomputedHash.equals(current.getCurrentHash())) {
                String errorMsg = String.format("Altération des données détectée à l'ID %d (tx: %s). Hash stocké: %s, Hash recalculé: %s",
                        current.getId(), current.getTransactionId(), current.getCurrentHash(), recomputedHash);
                log.error("ALERTE SÉCURITÉ AUDIT : {}", errorMsg);
                return AuditVerificationResponse.builder()
                        .valid(false)
                        .totalRecordsChecked(i + 1)
                        .corruptedRecordId(current.getId())
                        .status("CORRUPTION_DETECTED")
                        .details(errorMsg)
                        .build();
            }

            expectedPreviousHash = current.getCurrentHash();
        }

        String lastHash = allLogs.get(allLogs.size() - 1).getCurrentHash();
        log.info("Vérification d'intégrité réussie : {} enregistrements cryptographiquement valides. Dernier hash: {}",
                allLogs.size(), lastHash);

        return AuditVerificationResponse.builder()
                .valid(true)
                .totalRecordsChecked(allLogs.size())
                .lastHash(lastHash)
                .status("VERIFIED_TAMPER_FREE")
                .details("Intégrité cryptographique confirmée. Aucun enregistrement n'a été modifié ou falsifié.")
                .build();
    }

    private String buildRawData(String prevHash, String eventType, String txId, String payload, long timestampEpoch) {
        return prevHash + ":" + eventType + ":" + txId + ":" + payload + ":" + timestampEpoch;
    }

    public static String calculateSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithme SHA-256 non disponible dans la JVM", e);
        }
    }
}
