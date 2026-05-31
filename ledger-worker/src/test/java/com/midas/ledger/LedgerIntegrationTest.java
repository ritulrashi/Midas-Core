package com.midas.ledger;

import com.midas.common.event.EventType;
import com.midas.common.event.TransactionEvent;
import com.midas.ledger.consumer.LedgerConsumer;
import com.midas.ledger.model.EntryType;
import com.midas.ledger.model.LedgerEntry;
import com.midas.ledger.repository.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class LedgerIntegrationTest {

    @TestConfiguration
    static class KafkaTestConfig {
        @Bean
        ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> kafkaListenerContainerFactory(
                ConsumerFactory<String, TransactionEvent> consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            factory.setAutoStartup(false);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
            return factory;
        }
    }

    @Autowired private LedgerConsumer ledgerConsumer;
    @Autowired private LedgerRepository ledgerRepository;

    @BeforeEach
    void setUp() {
        ledgerRepository.deleteAll();
    }

    @Test
    void consumer_writesDebitAndCreditEntriesForCompletedTransaction() {
        String transactionId = UUID.randomUUID().toString();
        TransactionEvent event = buildEvent(transactionId, EventType.TRANSACTION_COMPLETED);
        Acknowledgment ack = mock(Acknowledgment.class);

        ledgerConsumer.consume(event, 0, 0L, ack);

        List<LedgerEntry> entries = ledgerRepository.findByTransactionId(transactionId);
        assertThat(entries).hasSize(2);

        LedgerEntry debit = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .findFirst().orElseThrow();
        LedgerEntry credit = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .findFirst().orElseThrow();

        assertThat(debit.getAccountId()).isEqualTo("acc-001");
        assertThat(debit.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(debit.getCurrency()).isEqualTo("USD");
        assertThat(debit.getEventType()).isEqualTo("TRANSACTION_COMPLETED");

        assertThat(credit.getAccountId()).isEqualTo("acc-002");
        assertThat(credit.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(credit.getCurrency()).isEqualTo("USD");
        assertThat(credit.getEventType()).isEqualTo("TRANSACTION_COMPLETED");

        verify(ack).acknowledge();
    }

    @Test
    void consumer_skipsNonCompletedEvents() {
        for (EventType type : List.of(
                EventType.TRANSACTION_CREATED,
                EventType.TRANSACTION_PROCESSING,
                EventType.TRANSACTION_FAILED)) {

            String transactionId = UUID.randomUUID().toString();
            Acknowledgment ack = mock(Acknowledgment.class);

            ledgerConsumer.consume(buildEvent(transactionId, type), 0, 0L, ack);

            assertThat(ledgerRepository.findByTransactionId(transactionId))
                    .as("Expected no entries for event type %s", type)
                    .isEmpty();
            // consumer still acks — no exception is thrown for skipped events
            verify(ack).acknowledge();
        }
    }

    @Test
    void consumer_isIdempotentOnDuplicateTransactionId() {
        String transactionId = UUID.randomUUID().toString();
        TransactionEvent event = buildEvent(transactionId, EventType.TRANSACTION_COMPLETED);
        Acknowledgment ack = mock(Acknowledgment.class);

        ledgerConsumer.consume(event, 0, 0L, ack);
        ledgerConsumer.consume(event, 0, 1L, ack);

        assertThat(ledgerRepository.findByTransactionId(transactionId))
                .hasSize(2); // exactly one DEBIT + one CREDIT, not doubled
        verify(ack, times(2)).acknowledge();
    }

    private TransactionEvent buildEvent(String transactionId, EventType eventType) {
        return TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionId(transactionId)
                .eventType(eventType)
                .fromAccount("acc-001")
                .toAccount("acc-002")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .status(eventType.name())
                .initiatedBy("user-123")
                .occurredAt(Instant.now())
                .build();
    }
}
