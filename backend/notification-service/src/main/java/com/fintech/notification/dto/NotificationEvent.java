package com.fintech.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Modèle d'événement temps réel conforme à docs/contracts/websocket-events.json v1.1.0.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationEvent {

    @JsonProperty("type")
    private String type; // TX_DECIDED, QUEUE_UPDATED, POD_EVENT, ALERT

    @JsonProperty("tx_id")
    private String txId;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("decision")
    private String decision; // APPROVED, BLOCKED, HUMAN_REVIEW

    @JsonProperty("risk_score")
    private Integer riskScore;

    @JsonProperty("probability")
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

    @JsonProperty("pending")
    private Integer pending;

    @JsonProperty("delta")
    private Integer delta;

    @JsonProperty("pod")
    private String pod;

    @JsonProperty("state")
    private String state;

    @JsonProperty("timestamp")
    private String timestamp;
}
