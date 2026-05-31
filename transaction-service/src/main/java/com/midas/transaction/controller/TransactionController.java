package com.midas.transaction.controller;

import com.midas.common.dto.ApiResponse;
import com.midas.transaction.dto.CreateTransactionRequest;
import com.midas.transaction.dto.UpdateTransactionStatusRequest;
import com.midas.transaction.model.Transaction;
import com.midas.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> create(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader("X-User-Id") String userId) {
        Transaction transaction = transactionService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(transaction));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> getById(@PathVariable UUID id) {
        Transaction transaction = transactionService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(transaction));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Transaction>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionStatusRequest request) {
        Transaction updated = transactionService.updateStatus(id, request.getStatus(), request.getFailureReason());
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<Transaction>>> getMyTransactions(
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<Transaction> page = transactionService.findByInitiatedBy(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }
}
