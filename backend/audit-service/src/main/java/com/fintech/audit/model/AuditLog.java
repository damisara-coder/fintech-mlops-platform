package com.fintech.audit.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entité JPA du journal d'audit immuable à hash cryptographique chaîné (Tâche P1.6).
 * Conforme au schéma PostgreSQL (docs/architecture/data-model.sql §5).
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_tx", columnList = "transaction_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_id", nullable = false, unique = true, length = 64)
    private String auditId; // ex: a-88213 ou a-UUID

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // TRANSACTION_CREATED, RISK_EVALUATED, HUMAN_REVIEW_SUBMITTED

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash; // Hash SHA-256 de l'entrée précédente (ou GENESIS)

    @Column(name = "current_hash", nullable = false, length = 64)
    private String currentHash; // SHA-256(previousHash + ":" + eventType + ":" + transactionId + ":" + payloadJson + ":" + timestamp)

    @Column(name = "actor", nullable = false, length = 100)
    @Builder.Default
    private String actor = "SYSTEM";

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
