package com.fintech.customer.controller;

import com.fintech.customer.model.Account;
import com.fintech.customer.model.Customer;
import com.fintech.customer.repository.AccountRepository;
import com.fintech.customer.repository.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public CustomerController(CustomerRepository customerRepository, AccountRepository accountRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable String id) {
        return customerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customers/{id}/accounts")
    public ResponseEntity<List<Account>> getCustomerAccounts(@PathVariable String id) {
        List<Account> accounts = accountRepository.findByCustomerId(id);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable String id) {
        return accountRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/customers")
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        Customer saved = customerRepository.save(customer);
        return ResponseEntity.ok(saved);
    }
}
