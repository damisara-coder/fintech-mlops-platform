package com.fintech.audit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogRequest {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("payload_json")
    private String payloadJson;

    @JsonProperty("actor")
    @Builder.Default
    private String actor = "SYSTEM";
}
