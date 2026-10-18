package com.fintech.risk.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictResponse {

    private Integer prediction; // 0 ou 1
    private Double probability; // 0.0 à 1.0

    @JsonProperty("uncertainty_score")
    private Double uncertaintyScore;

    @JsonProperty("is_uncertain")
    private Boolean isUncertain;

    private List<Double> interval; // [inf, sup]

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("latency_ms")
    private Integer latencyMs;
}
