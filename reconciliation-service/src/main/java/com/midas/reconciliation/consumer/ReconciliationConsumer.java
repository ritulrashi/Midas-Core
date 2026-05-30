package com.midas.reconciliation.consumer;

import com.midas.common.event.TransactionEvent;
import com.midas.reconciliation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationConsumer {

    private final ReconciliationService reconciliationService;

    @KafkaListener(
            topics = "${kafka.topics.transaction-events:midas.transaction.events}",
            groupId = "reconciliation-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received event {} for transaction {} [partition={}, offset={}]",
                event.getEventType(), event.getTransactionId(), partition, offset);

        try {
            reconciliationService.process(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process reconciliation for transaction {}: {}",
                    event.getTransactionId(), e.getMessage(), e);
            // Let retry handle it; don't ack so the message is redelivered
        }
    }
}
