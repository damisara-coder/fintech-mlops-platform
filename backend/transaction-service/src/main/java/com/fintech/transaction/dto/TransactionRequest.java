package com.fintech.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {

    @JsonProperty("account_id")
    private String accountId;

    private BigDecimal amount;
    private String currency;
    private String channel;

    @JsonProperty("merchant_name")
    private String merchantName;

    @JsonProperty("merchant_category")
    private String merchantCategory;

    @JsonProperty("ip_address")
    private String ipAddress;

    private Map<String, Double> features;
}
