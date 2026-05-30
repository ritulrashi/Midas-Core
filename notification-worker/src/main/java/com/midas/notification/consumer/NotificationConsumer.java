package com.midas.notification.consumer;

import com.midas.common.event.TransactionEvent;
import com.midas.notification.service.NotificationService;
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
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.transaction-events:midas.transaction.events}",
            groupId = "notification-worker",
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
            notificationService.notify(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to send notification for transaction {}: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }
}
