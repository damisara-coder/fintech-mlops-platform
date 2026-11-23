package com.fintech.audit.repository;

import com.fintech.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Récupère le dernier log inséré pour chaîner le nouveau hash SHA-256.
     */
    Optional<AuditLog> findTopByOrderByIdDesc();

    /**
     * Récupère l'audit trail complet d'une transaction.
     */
    List<AuditLog> findByTransactionIdOrderByTimestampAsc(String transactionId);

    /**
     * Récupère tous les logs dans l'ordre chronologique pour vérifier l'intégrité de la chaîne.
     */
    List<AuditLog> findAllByOrderByIdAsc();

    /**
     * Récupère les N derniers logs pour l'affichage tableau de bord.
     */
    List<AuditLog> findTop50ByOrderByTimestampDesc();
}
