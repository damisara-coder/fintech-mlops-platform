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
public class SimulationRequest {

    private String scenario; // normal, fraud, uncertain, custom

    private BigDecimal amount;
    private String currency;
    private String country;

    @JsonProperty("merchant_category")
    private String merchantCategory;

    private Map<String, Double> features;
}
