package com.midas.transaction.repository;

import com.midas.transaction.model.Transaction;
import com.midas.transaction.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByFromAccount(String fromAccount, Pageable pageable);

    Page<Transaction> findByToAccount(String toAccount, Pageable pageable);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findByInitiatedBy(String initiatedBy, Pageable pageable);
}
