package com.fintech.audit;

import com.fintech.audit.dto.AuditLogRequest;
import com.fintech.audit.dto.AuditVerificationResponse;
import com.fintech.audit.model.AuditLog;
import com.fintech.audit.repository.AuditLogRepository;
import com.fintech.audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private AuditLogRepository repository;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AuditLogRepository.class);
        auditService = new AuditService(repository);
    }

    @Test
    @DisplayName("Devrait calculer un hash SHA-256 valide et non nul")
    void testCalculateSha256() {
        String hash = AuditService.calculateSha256("test-data-payload");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("Devrait enregistrer la première entrée avec le hash GENESIS")
    void testRecordFirstEventGenesis() {
        when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(repository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });

        AuditLogRequest request = AuditLogRequest.builder()
                .transactionId("TX-100")
                .eventType("TRANSACTION_CREATED")
                .payloadJson("{\"amount\": 150.0}")
                .actor("TEST_USER")
                .build();

        AuditLog result = auditService.recordEvent(request);

        assertNotNull(result);
        assertEquals(AuditService.GENESIS_HASH, result.getPreviousHash());
        assertNotNull(result.getCurrentHash());
        assertEquals(64, result.getCurrentHash().length());
    }

    @Test
    @DisplayName("Devrait valider l'intégrité lorsque la chaîne n'a subi aucune altération")
    void testVerifyIntegritySuccess() {
        Instant t1 = Instant.ofEpochMilli(1700000000000L);
        Instant t2 = Instant.ofEpochMilli(1700000005000L);

        String prev0 = AuditService.GENESIS_HASH;
        String raw1 = prev0 + ":TRANSACTION_CREATED:TX-100:payload1:" + t1.toEpochMilli();
        String hash1 = AuditService.calculateSha256(raw1);

        String raw2 = hash1 + ":RISK_EVALUATED:TX-100:payload2:" + t2.toEpochMilli();
        String hash2 = AuditService.calculateSha256(raw2);

        AuditLog log1 = AuditLog.builder()
                .id(1L).auditId("a-1").transactionId("TX-100")
                .eventType("TRANSACTION_CREATED").payloadJson("payload1")
                .previousHash(prev0).currentHash(hash1).actor("SYSTEM").timestamp(t1)
                .build();

        AuditLog log2 = AuditLog.builder()
                .id(2L).auditId("a-2").transactionId("TX-100")
                .eventType("RISK_EVALUATED").payloadJson("payload2")
                .previousHash(hash1).currentHash(hash2).actor("SYSTEM").timestamp(t2)
                .build();

        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(log1, log2));

        AuditVerificationResponse response = auditService.verifyIntegrity();

        assertTrue(response.isValid());
        assertEquals(2, response.getTotalRecordsChecked());
        assertEquals("VERIFIED_TAMPER_FREE", response.getStatus());
        assertEquals(hash2, response.getLastHash());
    }

    @Test
    @DisplayName("Devrait détecter une falsification si un payload a été altéré")
    void testDetectTampering() {
        Instant t1 = Instant.ofEpochMilli(1700000000000L);
        String prev0 = AuditService.GENESIS_HASH;
        String raw1 = prev0 + ":TRANSACTION_CREATED:TX-100:legitimatePayload:" + t1.toEpochMilli();
        String hash1 = AuditService.calculateSha256(raw1);

        // Falsification simulée : quelqu'un a modifié le payload en BDD sans pouvoir recalculer le hash
        AuditLog tamperedLog = AuditLog.builder()
                .id(1L).auditId("a-1").transactionId("TX-100")
                .eventType("TRANSACTION_CREATED").payloadJson("FORGED_PAYLOAD_AMOUNT_MILLION")
                .previousHash(prev0).currentHash(hash1).actor("HACKER").timestamp(t1)
                .build();

        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(tamperedLog));

        AuditVerificationResponse response = auditService.verifyIntegrity();

        assertFalse(response.isValid());
        assertEquals("CORRUPTION_DETECTED", response.getStatus());
        assertEquals(1L, response.getCorruptedRecordId());
    }
}
