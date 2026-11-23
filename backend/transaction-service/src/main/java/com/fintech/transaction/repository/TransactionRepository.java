package com.fintech.transaction.repository;

import com.fintech.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId);
    List<Transaction> findByStatusOrderByCreatedAtDesc(String status);
    List<Transaction> findAllByOrderByCreatedAtDesc();
}
