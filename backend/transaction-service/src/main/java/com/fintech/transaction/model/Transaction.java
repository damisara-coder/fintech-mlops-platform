package com.fintech.transaction.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Column(length = 64)
    private String id; // ex: TX-9F219

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "EUR";

    @Column(nullable = false, length = 20)
    private String channel; // card, wire, sepa

    @Column(name = "merchant_name", length = 150)
    private String merchantName;

    @Column(name = "merchant_category", length = 50)
    private String merchantCategory;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "ip_country", length = 2)
    private String ipCountry;

    @Column(name = "ip_risk", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal ipRisk = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING, EVALUATED, FAILED

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
