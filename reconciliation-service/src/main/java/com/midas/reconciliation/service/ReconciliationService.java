package com.midas.reconciliation.service;

import com.midas.common.event.TransactionEvent;
import com.midas.reconciliation.model.ReconciliationRecord;
import com.midas.reconciliation.model.ReconciliationStatus;
import com.midas.reconciliation.repository.ReconciliationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
