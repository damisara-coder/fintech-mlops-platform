package com.fintech.risk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoreResponse {

    @JsonProperty("tx_id")
    private String txId;

    private String decision; // APPROVED, BLOCKED, HUMAN_REVIEW

    @JsonProperty("risk_score")
    private Integer riskScore; // 0 à 100

    @JsonProperty("risk_band")
    private String riskBand; // LOW, MEDIUM, HIGH, CRITICAL

    @JsonProperty("model_version")
    private String modelVersion;

    private Double uncertainty;

    private List<FactorContribution> factors;

    @JsonProperty("audit_id")
    private String auditId;

    @JsonProperty("decided_at")
    private Instant decidedAt;
}
