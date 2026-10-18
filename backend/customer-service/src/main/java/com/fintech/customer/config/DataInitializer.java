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

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(CustomerRepository customerRepo, AccountRepository accountRepo) {
        return args -> {
            if (customerRepo.count() == 0) {
                // Client de référence du document de cadrage (Maquette 3)
                Customer customer = Customer.builder()
                        .id("CUST-10428")
                        .firstName("Laila")
                        .lastName("Benali")
                        .email("l.benali@example.com")
                        .phone("+33612345678")
                        .country("FR")
                        .riskProfile("STANDARD")
                        .createdAt(Instant.now().minus(2190, ChronoUnit.DAYS))
                        .build();
                customerRepo.save(customer);

                // Compte bancaire actif depuis 6 ans (2190 jours)
                Account account = Account.builder()
                        .id("ACC-5521")
                        .customerId(customer.getId())
                        .accountNumber("FR7630001007941234567890185")
                        .currency("EUR")
                        .balance(new BigDecimal("3420.50"))
                        .status("ACTIVE")
                        .openedAt(Instant.now().minus(2190, ChronoUnit.DAYS))
                        .accountAgeDays(2190)
                        .build();
                accountRepo.save(account);
            }
        };
    }
}
