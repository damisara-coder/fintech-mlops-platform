package com.fintech.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationResponse {

    @JsonProperty("tx_id")
    private String txId;

    private String status; // APPROVED, BLOCKED, HUMAN_REVIEW

    private Double probability;

    @JsonProperty("uncertainty_score")
    private Double uncertaintyScore;

    @JsonProperty("is_uncertain")
    private Boolean isUncertain;

    @JsonProperty("conformal_interval")
    private List<Double> conformalInterval;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("latency_ms")
    private Integer latencyMs;

    @JsonProperty("audit_id")
    private String auditId;
}
