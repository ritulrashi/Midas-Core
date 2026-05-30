package com.midas.transaction.service;

import com.midas.common.event.EventType;
import com.midas.transaction.dto.CreateTransactionRequest;
import com.midas.transaction.kafka.TransactionEventProducer;
import com.midas.transaction.model.Transaction;
import com.midas.transaction.model.TransactionStatus;
import com.midas.transaction.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventProducer eventProducer;

    @Transactional
    public Transaction create(CreateTransactionRequest request, String initiatedBy) {
        Transaction transaction = Transaction.builder()
                .type(request.getType())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .description(request.getDescription())
                .status(TransactionStatus.PENDING)
                .initiatedBy(initiatedBy)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        eventProducer.publish(saved, EventType.TRANSACTION_CREATED);
        log.info("Transaction {} created by {}", saved.getId(), initiatedBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public Transaction findById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> findByInitiatedBy(String userId, Pageable pageable) {
        return transactionRepository.findByInitiatedBy(userId, pageable);
    }

    @Transactional
    public Transaction updateStatus(UUID id, TransactionStatus newStatus, String failureReason) {
        Transaction transaction = findById(id);
        transaction.setStatus(newStatus);
        transaction.setFailureReason(failureReason);

        Transaction updated = transactionRepository.save(transaction);

        EventType eventType = switch (newStatus) {
            case PROCESSING -> EventType.TRANSACTION_PROCESSING;
            case COMPLETED  -> EventType.TRANSACTION_COMPLETED;
            case FAILED     -> EventType.TRANSACTION_FAILED;
            case REVERSED   -> EventType.TRANSACTION_REVERSED;
            default         -> null;
        };

        if (eventType != null) {
            eventProducer.publish(updated, eventType);
        }

        return updated;
    }
}
