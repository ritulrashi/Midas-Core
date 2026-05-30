package com.midas.ledger.consumer;

import com.midas.common.event.TransactionEvent;
import com.midas.ledger.service.LedgerService;
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
public class LedgerConsumer {

    private final LedgerService ledgerService;

    @KafkaListener(
            topics = "${kafka.topics.transaction-events:midas.transaction.events}",
            groupId = "ledger-worker",
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
            ledgerService.record(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to record ledger entries for transaction {}: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }
}
