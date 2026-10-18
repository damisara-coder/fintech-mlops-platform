package com.fintech.risk.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactorContribution {
    private String f; // nom de la variable (feature)
    private Double c; // contribution au score
}
