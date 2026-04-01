package com.financedashboard.zorvyn.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.financedashboard.zorvyn.entity.FinancialRecord;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecordResponse {

    private Long id;
    private BigDecimal amount;
    private String type;          // INCOME / EXPENSE
    private String category;
    private LocalDate transactionDate;
    private String notes;

    private Long createdBy;       // userId
    private LocalDateTime createdAt;

    public static RecordResponse fromEntity(FinancialRecord record) {
        return RecordResponse.builder()
                .id(record.getId())
                .amount(record.getAmount())
                .type(record.getType().name())
                .category(record.getCategory())
                .transactionDate(record.getTransactionDate())
                .notes(record.getNotes())
                .createdBy(record.getCreatedBy().getId())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
