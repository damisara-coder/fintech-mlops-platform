package com.fintech.risk.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictRequest {

    @JsonProperty("tx_id")
    private String txId;

    private Map<String, Double> features;
}
