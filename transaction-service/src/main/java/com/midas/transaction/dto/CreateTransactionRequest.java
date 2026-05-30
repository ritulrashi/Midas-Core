package com.midas.transaction.dto;

import com.midas.transaction.model.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {

    @NotNull
    private TransactionType type;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotBlank
    private String fromAccount;

    @NotBlank
    private String toAccount;

    @Size(max = 500)
    private String description;
}
