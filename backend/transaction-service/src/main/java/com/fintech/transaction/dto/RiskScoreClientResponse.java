package com.fintech.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoreClientResponse {

    @JsonProperty("tx_id")
    private String txId;

    private String decision; // APPROVED, BLOCKED, HUMAN_REVIEW

    @JsonProperty("risk_score")
    private Integer riskScore;

    @JsonProperty("risk_band")
    private String riskBand;

    @JsonProperty("model_version")
    private String modelVersion;

    private Double uncertainty;

    @JsonProperty("audit_id")
    private String auditId;

    @JsonProperty("decided_at")
    private Instant decidedAt;
}
