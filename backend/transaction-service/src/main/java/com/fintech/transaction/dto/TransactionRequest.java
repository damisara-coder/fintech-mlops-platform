package com.fintech.transaction.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private String merchantName;
    private String merchantCategory;
    private String ipAddress;
    private Map<String, Double> features;
}
