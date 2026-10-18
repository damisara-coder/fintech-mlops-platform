package com.fintech.customer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Column(length = 64)
    private String id; // ex: CUST-10428

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 2)
    private String country; // ex: FR, ES

    @Column(name = "risk_profile", length = 20)
    private String riskProfile; // STANDARD, LOW, HIGH

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
