package com.fintech.transaction.controller;

import com.fintech.transaction.dto.SimulationRequest;
import com.fintech.transaction.dto.SimulationResponse;
import com.fintech.transaction.dto.TransactionRequest;
import com.fintech.transaction.dto.TransactionResponse;
import com.fintech.transaction.model.Transaction;
import com.fintech.transaction.repository.TransactionRepository;
import com.fintech.transaction.service.TransactionProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final TransactionProcessingService transactionProcessingService;

    public TransactionController(
            TransactionRepository transactionRepository,
            TransactionProcessingService transactionProcessingService) {
        this.transactionRepository = transactionRepository;
        this.transactionProcessingService = transactionProcessingService;
    }

    /**
     * Ingestion d'une transaction bancaire de production (Asynchrone 202 Accepted).
     * Enregistrée immédiatement en statut PENDING pour audit, puis évaluée en arrière-plan.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> submitTransaction(@RequestBody TransactionRequest request) {
        Transaction tx = transactionProcessingService.createAndDispatch(request);

        TransactionResponse response = TransactionResponse.builder()
                .txId(tx.getId())
                .status(tx.getStatus())
                .message("Transaction enregistrée et en cours d'évaluation de risque")
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Simulateur de paiement synchrone (Maquette 6 du document général).
     * Idéal pour tester en direct les 3 scénarios (normal, fraude, incertain) pendant la démo.
     */
    @PostMapping("/simulate")
    public ResponseEntity<SimulationResponse> simulateTransaction(@RequestBody SimulationRequest request) {
        SimulationResponse response = transactionProcessingService.simulate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Consultation du détail d'une transaction par son identifiant
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable String id) {
        return transactionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Flux des transactions avec filtrage par statut et limitation du nombre de résultats.
     * Alimente le Live Feed du Dashboard React (Maquette 2).
     */
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "50") int limit) {

        List<Transaction> transactions;
        if (status != null && !status.isBlank()) {
            transactions = transactionRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
        } else {
            transactions = transactionRepository.findAllByOrderByCreatedAtDesc();
        }

        List<Transaction> limited = transactions.stream()
                .limit(limit)
                .collect(Collectors.toList());

        return ResponseEntity.ok(limited);
    }
}
