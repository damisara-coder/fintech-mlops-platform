package com.fintech.audit.controller;

import com.fintech.audit.dto.AuditLogRequest;
import com.fintech.audit.dto.AuditVerificationResponse;
import com.fintech.audit.model.AuditLog;
import com.fintech.audit.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Enregistre un événement dans le journal d'audit cryptographique.
     */
    @PostMapping("/log")
    public ResponseEntity<AuditLog> logEvent(@RequestBody AuditLogRequest request) {
        AuditLog savedLog = auditService.recordEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLog);
    }

    /**
     * Récupère la chaîne d'audit complète pour une transaction donnée.
     */
    @GetMapping("/{txId}")
    public ResponseEntity<List<AuditLog>> getAuditTrail(@PathVariable String txId) {
        List<AuditLog> trail = auditService.getAuditTrail(txId);
        return ResponseEntity.ok(trail);
    }

    /**
     * Point d'audit réglementaire : Vérification formelle de non-falsification de la chaîne de hash.
     */
    @GetMapping("/verify")
    public ResponseEntity<AuditVerificationResponse> verifyIntegrity() {
        AuditVerificationResponse response = auditService.verifyIntegrity();
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère les 50 derniers événements d'audit pour le tableau de bord superviseur.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AuditLog>> getRecentLogs() {
        return ResponseEntity.ok(auditService.getRecentLogs());
    }
}
