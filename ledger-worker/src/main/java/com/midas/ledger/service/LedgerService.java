package com.midas.ledger.service;

import com.midas.common.event.EventType;
import com.midas.common.event.TransactionEvent;
import com.midas.ledger.model.EntryType;
import com.midas.ledger.model.LedgerEntry;
import com.midas.ledger.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    /**
     * Records a double-entry when a transaction completes:
     * - DEBIT  the sender's account (money leaves)
     * - CREDIT the receiver's account (money arrives)
     *
     * Idempotent: skips if entries for this transactionId already exist.
     */
    @Transactional
    public void record(TransactionEvent event) {
        if (event.getEventType() != EventType.TRANSACTION_COMPLETED) {
            log.debug("Skipping non-COMPLETED event {} for transaction {}",
                    event.getEventType(), event.getTransactionId());
            return;
        }

        if (ledgerRepository.existsByTransactionId(event.getTransactionId())) {
            log.debug("Ledger entries already exist for transaction {} — skipping", event.getTransactionId());
            return;
        }

        LedgerEntry debit = LedgerEntry.builder()
                .transactionId(event.getTransactionId())
                .accountId(event.getFromAccount())
                .entryType(EntryType.DEBIT)
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .eventType(event.getEventType().name())
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .transactionId(event.getTransactionId())
                .accountId(event.getToAccount())
                .entryType(EntryType.CREDIT)
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .eventType(event.getEventType().name())
                .build();

        ledgerRepository.saveAll(List.of(debit, credit));
        log.info("Ledger entries recorded for transaction {} ({} {} → {})",
                event.getTransactionId(), event.getAmount(), event.getCurrency(),
                event.getToAccount());
    }
}
