package com.fintech.risk.controller;

import com.fintech.risk.dto.RiskScoreRequest;
import com.fintech.risk.dto.RiskScoreResponse;
import com.fintech.risk.model.Decision;
import com.fintech.risk.repository.DecisionRepository;
import com.fintech.risk.service.RiskDecisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1/risk")
public class RiskController {

    private final RiskDecisionService riskDecisionService;
    private final DecisionRepository decisionRepository;

    public RiskController(RiskDecisionService riskDecisionService, DecisionRepository decisionRepository) {
        this.riskDecisionService = riskDecisionService;
        this.decisionRepository = decisionRepository;
    }

    @PostMapping("/score")
    public ResponseEntity<RiskScoreResponse> evaluateRisk(@RequestBody RiskScoreRequest request) {
        RiskScoreResponse response = riskDecisionService.evaluateRisk(request);
        return ResponseEntity.ok(response);
    }

    /**
     * File d'attente Human Review Queue (Maquette 3) : triée par incertitude décroissante !
     */
    @GetMapping("/human-review")
    public ResponseEntity<List<Decision>> getHumanReviewQueue() {
        List<Decision> pendingQueue = decisionRepository.findByDecisionOrderByUncertaintyScoreDesc("HUMAN_REVIEW");
        return ResponseEntity.ok(pendingQueue);
    }

    /**
     * Action de l'analyste sur une transaction incertaine (Approuver / Rejeter)
     */
    @PostMapping("/{txId}/review")
    public ResponseEntity<?> submitHumanReviewDecision(
            @PathVariable String txId,
            @RequestBody Map<String, String> body) {
        String reviewerAction = body.get("decision"); // APPROVED ou REJECTED
        String notes = body.get("notes");

        return decisionRepository.findByTransactionId(txId)
                .map(decision -> {
                    decision.setReviewStatus("REVIEWED_" + reviewerAction);
                    decision.setReviewerNotes(notes);
                    decisionRepository.save(decision);
                    return ResponseEntity.ok(Map.of(
                            "status", "SUCCESS",
                            "message", "Décision humaine enregistrée pour ré-entraînement ML",
                            "tx_id", txId,
                            "final_decision", reviewerAction
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
