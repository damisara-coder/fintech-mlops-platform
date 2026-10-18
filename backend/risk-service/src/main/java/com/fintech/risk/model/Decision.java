package com.fintech.risk.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Decision {

    @Id
    @Column(length = 64)
    private String id; // ex: DEC-9F219

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(nullable = false, length = 30)
    private String decision; // APPROVED, BLOCKED, HUMAN_REVIEW

    @Column(nullable = false)
    private Double probability;

    @Column(name = "uncertainty_score", nullable = false)
    private Double uncertaintyScore;

    @Column(name = "is_uncertain", nullable = false)
    private Boolean isUncertain;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "risk_band", nullable = false, length = 20)
    private String riskBand;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "review_status", length = 30)
    @Builder.Default
    private String reviewStatus = "NONE";

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "decided_at")
    @Builder.Default
    private Instant decidedAt = Instant.now();
}
