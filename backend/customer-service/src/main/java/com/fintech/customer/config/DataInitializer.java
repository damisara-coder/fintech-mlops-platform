package com.fintech.customer.config;

import com.fintech.customer.model.Account;
import com.fintech.customer.model.Customer;
import com.fintech.customer.repository.AccountRepository;
import com.fintech.customer.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(CustomerRepository customerRepo, AccountRepository accountRepo) {
        return args -> {
            if (customerRepo.count() == 0) {
                // 1. Client de référence Maquette 3 (Laila Benali - Transaction Incertaine)
                Customer c1 = Customer.builder()
                        .id("CUST-10428")
                        .firstName("Laila")
                        .lastName("Benali")
                        .email("l.benali@example.com")
                        .phone("+33612345678")
                        .country("FR")
                        .riskProfile("STANDARD")
                        .createdAt(Instant.now().minus(2190, ChronoUnit.DAYS))
                        .build();

                Account a1 = Account.builder()
                        .id("ACC-5521")
                        .customerId("CUST-10428")
                        .accountNumber("FR7630001007941234567890185")
                        .currency("EUR")
                        .balance(new BigDecimal("3420.50"))
                        .status("ACTIVE")
                        .openedAt(Instant.now().minus(2190, ChronoUnit.DAYS))
                        .accountAgeDays(2190)
                        .build();

                // 2. Client référence Maquette 2 (Claire Moreau - Transactions normales)
                Customer c2 = Customer.builder()
                        .id("CUST-10429")
                        .firstName("Claire")
                        .lastName("Moreau")
                        .email("c.moreau@example.fr")
                        .phone("+33698765432")
                        .country("FR")
                        .riskProfile("LOW")
                        .createdAt(Instant.now().minus(720, ChronoUnit.DAYS))
                        .build();

                Account a2 = Account.builder()
                        .id("ACC-5522")
                        .customerId("CUST-10429")
                        .accountNumber("FR7630001007949876543210942")
                        .currency("EUR")
                        .balance(new BigDecimal("1250.00"))
                        .status("ACTIVE")
                        .openedAt(Instant.now().minus(720, ChronoUnit.DAYS))
                        .accountAgeDays(720)
                        .build();

                // 3. Client référence Maquette 2 (Aleksander Kowalski - Transaction bloquée)
                Customer c3 = Customer.builder()
                        .id("CUST-10430")
                        .firstName("Aleksander")
                        .lastName("Kowalski")
                        .email("a.kowalski@example.pl")
                        .phone("+48221234567")
                        .country("PL")
                        .riskProfile("HIGH")
                        .createdAt(Instant.now().minus(180, ChronoUnit.DAYS))
                        .build();

                Account a3 = Account.builder()
                        .id("ACC-5523")
                        .customerId("CUST-10430")
                        .accountNumber("PL61109010140000071219812874")
                        .currency("EUR")
                        .balance(new BigDecimal("14500.00"))
                        .status("ACTIVE")
                        .openedAt(Instant.now().minus(180, ChronoUnit.DAYS))
                        .accountAgeDays(180)
                        .build();

                // 4. Client référence Maquette 2 (Sami Haddad - Transaction incertaine 2 450€)
                Customer c4 = Customer.builder()
                        .id("CUST-10431")
                        .firstName("Sami")
                        .lastName("Haddad")
                        .email("s.haddad@example.ae")
                        .phone("+971501234567")
                        .country("AE")
                        .riskProfile("STANDARD")
                        .createdAt(Instant.now().minus(450, ChronoUnit.DAYS))
                        .build();

                Account a4 = Account.builder()
                        .id("ACC-5524")
                        .customerId("CUST-10431")
                        .accountNumber("AE4803300000123456789012345")
                        .currency("EUR")
                        .balance(new BigDecimal("5800.00"))
                        .status("ACTIVE")
                        .openedAt(Instant.now().minus(450, ChronoUnit.DAYS))
                        .accountAgeDays(450)
                        .build();

                customerRepo.saveAll(List.of(c1, c2, c3, c4));
                accountRepo.saveAll(List.of(a1, a2, a3, a4));
            }
        };
    }
}
