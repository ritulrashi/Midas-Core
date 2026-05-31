package com.midas.transaction.dto;

import com.midas.transaction.model.TransactionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTransactionStatusRequest {

    @NotNull
    private TransactionStatus status;

    private String failureReason;
}
