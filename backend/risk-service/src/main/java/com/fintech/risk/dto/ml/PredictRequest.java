package com.fintech.risk.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictRequest {

    @JsonProperty("tx_id")
    private String txId;

    private BigDecimal amount;
    private String currency;
    private String channel;

    @JsonProperty("ip_country")
    private String ipCountry;

    @JsonProperty("ip_risk")
    private BigDecimal ipRisk;

    @JsonProperty("merchant_cat")
    private String merchantCat;

    private Integer hour;

    @JsonProperty("account_age_days")
    private Integer accountAgeDays;
}
