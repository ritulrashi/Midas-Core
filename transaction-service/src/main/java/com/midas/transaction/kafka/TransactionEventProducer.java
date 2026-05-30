package com.midas.transaction.kafka;

import com.midas.common.event.EventType;
import com.midas.common.event.TransactionEvent;
import com.midas.transaction.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    @Value("${kafka.topics.transaction-events:midas.transaction.events}")
    private String topic;

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void publish(Transaction transaction, EventType eventType) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionId(transaction.getId().toString())
                .eventType(eventType)
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .initiatedBy(transaction.getInitiatedBy())
                .failureReason(transaction.getFailureReason())
                .occurredAt(Instant.now())
                .build();

        CompletableFuture<SendResult<String, TransactionEvent>> future =
                kafkaTemplate.send(topic, transaction.getId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event {} for transaction {}: {}",
                        eventType, transaction.getId(), ex.getMessage());
            } else {
                log.debug("Published event {} for transaction {} to partition {} offset {}",
                        eventType, transaction.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
