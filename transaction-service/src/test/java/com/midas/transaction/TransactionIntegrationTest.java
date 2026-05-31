package com.midas.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midas.common.event.EventType;
import com.midas.transaction.dto.CreateTransactionRequest;
import com.midas.transaction.dto.UpdateTransactionStatusRequest;
import com.midas.transaction.kafka.TransactionEventProducer;
import com.midas.transaction.model.Transaction;
import com.midas.transaction.model.TransactionStatus;
import com.midas.transaction.model.TransactionType;
import com.midas.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionIntegrationTest {

    private static final String USER_ID = "user-123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TransactionRepository transactionRepository;
    @MockBean  private TransactionEventProducer eventProducer;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        reset(eventProducer);
    }

    @Test
    void createTransaction_returns201AndPersistsWithPendingStatus() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.type").value("TRANSFER"))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.initiatedBy").value(USER_ID));

        verify(eventProducer).publish(any(Transaction.class), eq(EventType.TRANSACTION_CREATED));
    }

    @Test
    void getTransactionById_returns200WithCorrectData() throws Exception {
        String body = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(body).at("/data/id").asText();

        mockMvc.perform(get("/api/transactions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.type").value("TRANSFER"))
                .andExpect(jsonPath("$.data.fromAccount").value("acc-001"))
                .andExpect(jsonPath("$.data.toAccount").value("acc-002"));
    }

    @Test
    void updateStatusToCompleted_returns200AndPublishesEvent() throws Exception {
        String body = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(body).at("/data/id").asText();

        UpdateTransactionStatusRequest statusRequest = new UpdateTransactionStatusRequest();
        statusRequest.setStatus(TransactionStatus.COMPLETED);

        mockMvc.perform(patch("/api/transactions/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(eventProducer).publish(any(Transaction.class), eq(EventType.TRANSACTION_COMPLETED));
    }

    private CreateTransactionRequest createRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(TransactionType.TRANSFER);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setFromAccount("acc-001");
        request.setToAccount("acc-002");
        return request;
    }
}
