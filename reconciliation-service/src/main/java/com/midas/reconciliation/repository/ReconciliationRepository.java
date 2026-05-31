package com.midas.reconciliation.repository;

import com.midas.reconciliation.model.ReconciliationRecord;
import com.midas.reconciliation.model.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRepository extends JpaRepository<ReconciliationRecord, UUID> {

    Optional<ReconciliationRecord> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    List<ReconciliationRecord> findByStatus(ReconciliationStatus status);

    Page<ReconciliationRecord> findByStatus(ReconciliationStatus status, Pageable pageable);

    List<ReconciliationRecord> findByStatusAndProcessedAtBefore(ReconciliationStatus status, Instant cutoff);
}
