package com.financedashboard.zorvyn.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.financedashboard.zorvyn.enums.RecordTypeEnum;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating and updating financial records.
 * BigDecimal is used for amount to ensure precise monetary arithmetic.
 * The authenticated user's identity is extracted from the JWT — never supplied by the client.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Record type is required (INCOME or EXPENSE)")
    private RecordTypeEnum type;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    /** Optional description or memo for the transaction. */
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
