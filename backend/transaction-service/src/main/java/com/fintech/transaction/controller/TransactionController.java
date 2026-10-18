package com.fintech.transaction.controller;

import com.fintech.transaction.dto.TransactionRequest;
import com.fintech.transaction.dto.TransactionResponse;
import com.fintech.transaction.model.Transaction;
import com.fintech.transaction.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> submitTransaction(@RequestBody TransactionRequest request) {
        // 1. Génération de l'identifiant transaction (ex: TX-9F219)
        String shortId = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String txId = "TX-" + shortId;

        // 2. Persistance immédiate en base de données avec statut PENDING (Spécification §2.2 Étape 2)
        Transaction transaction = Transaction.builder()
                .id(txId)
                .accountId(request.getAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .channel(request.getChannel())
                .merchantName(request.getMerchantName())
                .merchantCategory(request.getMerchantCategory())
                .ipAddress(request.getIpAddress())
                .status("PENDING")
                .createdAt(Instant.now())
                .build();

        transactionRepository.save(transaction);

        // 3. Réponse 202 Accepted (traçabilité assurée même si l'évaluation ultérieure prend du temps)
        TransactionResponse response = TransactionResponse.builder()
                .txId(txId)
                .status("PENDING")
                .message("Transaction enregistrée et en cours d'évaluation de risque")
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable String id) {
        return transactionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }
}
