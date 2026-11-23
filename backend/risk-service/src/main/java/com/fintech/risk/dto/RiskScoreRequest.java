package com.fintech.risk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoreRequest {

    @JsonProperty("tx_id")
    private String txId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("account_id")
    private String accountId;

    private BigDecimal amount;
    private String currency;
    private String channel;

    @JsonProperty("merchant_category")
    private String merchantCategory;

    @JsonProperty("ip_address")
    private String ipAddress;

    private Map<String, Double> features;
}
