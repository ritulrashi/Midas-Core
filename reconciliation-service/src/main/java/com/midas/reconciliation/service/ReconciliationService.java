package com.midas.reconciliation.service;

import com.midas.common.event.TransactionEvent;
import com.midas.reconciliation.model.ReconciliationRecord;
import com.midas.reconciliation.model.ReconciliationStatus;
import com.midas.reconciliation.repository.ReconciliationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;

    @Transactional
    public void process(TransactionEvent event) {
        if (reconciliationRepository.existsByTransactionId(event.getTransactionId())) {
            log.debug("Skipping duplicate event for transaction {}", event.getTransactionId());
            return;
        }

        ReconciliationRecord record = ReconciliationRecord.builder()
                .transactionId(event.getTransactionId())
                .eventType(event.getEventType().name())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .fromAccount(event.getFromAccount())
                .toAccount(event.getToAccount())
                .status(ReconciliationStatus.RECEIVED)
                .eventOccurredAt(event.getOccurredAt())
                .build();

        reconciliationRepository.save(record);
        log.info("Reconciliation record created for transaction {}", event.getTransactionId());
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationRecord> findAll(Pageable pageable) {
        return reconciliationRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationRecord> findByStatus(ReconciliationStatus status, Pageable pageable) {
        return reconciliationRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public ReconciliationRecord findById(UUID id) {
        return reconciliationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reconciliation record not found: " + id));
    }

    @Transactional(readOnly = true)
    public ReconciliationRecord findByTransactionId(String transactionId) {
        return reconciliationRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("No reconciliation record for transaction: " + transactionId));
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRecord> findUnmatched(Instant olderThan) {
        return reconciliationRepository.findByStatusAndProcessedAtBefore(
                ReconciliationStatus.RECEIVED, olderThan);
    }

    @Transactional
    public void markAsUnmatched(ReconciliationRecord record) {
        record.setStatus(ReconciliationStatus.UNMATCHED);
        record.setNotes("Flagged by scheduled reconciliation job");
        reconciliationRepository.save(record);
    }
}
