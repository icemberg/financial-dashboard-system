package com.financedashboard.zorvyn.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.financedashboard.zorvyn.enums.RecordTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Entity
@Table(name = "financial_records")
@RequiredArgsConstructor
public class FinancialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordTypeEnum type;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDate updatedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private boolean deleted = false;
}
