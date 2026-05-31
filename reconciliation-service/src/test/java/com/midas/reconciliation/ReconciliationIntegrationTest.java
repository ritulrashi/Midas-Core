package com.midas.reconciliation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midas.common.event.EventType;
import com.midas.common.event.TransactionEvent;
import com.midas.reconciliation.consumer.ReconciliationConsumer;
import com.midas.reconciliation.model.ReconciliationRecord;
import com.midas.reconciliation.model.ReconciliationStatus;
import com.midas.reconciliation.repository.ReconciliationRepository;
import com.midas.reconciliation.scheduler.ReconciliationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReconciliationIntegrationTest {

    /**
     * Overrides the production kafkaListenerContainerFactory with one that never
     * auto-starts, so no listener tries to connect to a real Kafka broker.
     */
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

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReconciliationConsumer reconciliationConsumer;
    @Autowired private ReconciliationRepository reconciliationRepository;
    @MockBean  private ReconciliationScheduler reconciliationScheduler;

    @BeforeEach
    void setUp() {
        reconciliationRepository.deleteAll();
    }

    @Test
    void consumer_createsRecordForTransactionCreatedEvent() {
        TransactionEvent event = buildEvent(UUID.randomUUID().toString());
        Acknowledgment ack = mock(Acknowledgment.class);

        reconciliationConsumer.consume(event, 0, 0L, ack);

        Optional<ReconciliationRecord> saved =
                reconciliationRepository.findByTransactionId(event.getTransactionId());

        assertThat(saved).isPresent();
        assertThat(saved.get().getTransactionId()).isEqualTo(event.getTransactionId());
        assertThat(saved.get().getStatus()).isEqualTo(ReconciliationStatus.RECEIVED);
        assertThat(saved.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(saved.get().getCurrency()).isEqualTo("USD");
        assertThat(saved.get().getFromAccount()).isEqualTo("acc-001");
        assertThat(saved.get().getToAccount()).isEqualTo("acc-002");
        verify(ack).acknowledge();
    }

    @Test
    void consumer_ignoresDuplicateTransactionId() {
        String transactionId = UUID.randomUUID().toString();
        TransactionEvent event = buildEvent(transactionId);
        Acknowledgment ack = mock(Acknowledgment.class);

        reconciliationConsumer.consume(event, 0, 0L, ack);
        reconciliationConsumer.consume(event, 0, 1L, ack);

        assertThat(reconciliationRepository.findAll()).hasSize(1);
    }

    @Test
    void getRecords_returns200WithPagedResults() throws Exception {
        reconciliationConsumer.consume(buildEvent(UUID.randomUUID().toString()), 0, 0L, mock(Acknowledgment.class));
        reconciliationConsumer.consume(buildEvent(UUID.randomUUID().toString()), 0, 1L, mock(Acknowledgment.class));

        mockMvc.perform(get("/api/reconciliation/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].status").value("RECEIVED"));
    }

    @Test
    void getByTransactionId_returnsCorrectRecord() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        reconciliationConsumer.consume(buildEvent(transactionId), 0, 0L, mock(Acknowledgment.class));

        mockMvc.perform(get("/api/reconciliation/records/transaction/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value(transactionId))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.fromAccount").value("acc-001"))
                .andExpect(jsonPath("$.data.toAccount").value("acc-002"));
    }

    private TransactionEvent buildEvent(String transactionId) {
        return TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionId(transactionId)
                .eventType(EventType.TRANSACTION_CREATED)
                .fromAccount("acc-001")
                .toAccount("acc-002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status("PENDING")
                .initiatedBy("user-123")
                .occurredAt(Instant.now())
                .build();
    }
}
