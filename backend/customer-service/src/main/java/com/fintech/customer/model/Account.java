package com.fintech.customer.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(length = 64)
    private String id; // ex: ACC-5521

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @Column(length = 3)
    @Builder.Default
    private String currency = "EUR";

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, FROZEN

    @Column(name = "opened_at")
    @Builder.Default
    private Instant openedAt = Instant.now();

    @Column(name = "account_age_days")
    @Builder.Default
    private Integer accountAgeDays = 0;
}
