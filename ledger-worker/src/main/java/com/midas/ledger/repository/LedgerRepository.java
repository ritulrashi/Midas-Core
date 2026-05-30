package com.midas.ledger.repository;

import com.midas.ledger.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(String transactionId);

    List<LedgerEntry> findByAccountId(String accountId);

    boolean existsByTransactionId(String transactionId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE -e.amount END), 0) " +
           "FROM LedgerEntry e WHERE e.accountId = :accountId AND e.currency = :currency")
    BigDecimal calculateBalance(String accountId, String currency);
}
