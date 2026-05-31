package com.midas.reconciliation.controller;

import com.midas.common.dto.ApiResponse;
import com.midas.reconciliation.model.ReconciliationRecord;
import com.midas.reconciliation.model.ReconciliationStatus;
import com.midas.reconciliation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<Page<ReconciliationRecord>>> getRecords(
            @RequestParam(required = false) ReconciliationStatus status,
            @PageableDefault(size = 20, sort = "processedAt") Pageable pageable) {
        Page<ReconciliationRecord> page = status != null
                ? reconciliationService.findByStatus(status, pageable)
                : reconciliationService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/records/{id}")
    public ResponseEntity<ApiResponse<ReconciliationRecord>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(reconciliationService.findById(id)));
    }

    @GetMapping("/records/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<ReconciliationRecord>> getByTransactionId(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(ApiResponse.ok(reconciliationService.findByTransactionId(transactionId)));
    }
}
