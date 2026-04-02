package com.financedashboard.zorvyn.repository.interfaces;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.financedashboard.zorvyn.entity.FinancialRecord;
import com.financedashboard.zorvyn.enums.RecordTypeEnum;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    // ── Dashboard Queries ─────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(fr.amount), 0) FROM FinancialRecord fr " +
           "WHERE fr.deleted = false AND fr.type = com.financedashboard.zorvyn.enums.RecordTypeEnum.INCOME " +
           "AND (:userId IS NULL OR fr.createdBy.id = :userId)")
    BigDecimal sumIncome(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(fr.amount), 0) FROM FinancialRecord fr " +
           "WHERE fr.deleted = false AND fr.type = com.financedashboard.zorvyn.enums.RecordTypeEnum.EXPENSE " +
           "AND (:userId IS NULL OR fr.createdBy.id = :userId)")
    BigDecimal sumExpense(@Param("userId") Long userId);

    @Query("SELECT fr.category, COALESCE(SUM(fr.amount), 0) FROM FinancialRecord fr " +
           "WHERE fr.deleted = false " +
           "AND (:userId IS NULL OR fr.createdBy.id = :userId) " +
           "GROUP BY fr.category")
    List<Object[]> categoryTotals(@Param("userId") Long userId);

    @Query("SELECT YEAR(fr.transactionDate), MONTH(fr.transactionDate), COALESCE(SUM(fr.amount), 0) " +
           "FROM FinancialRecord fr WHERE fr.deleted = false " +
           "AND (:startDate IS NULL OR fr.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR fr.transactionDate <= :endDate) " +
           "AND (:userId IS NULL OR fr.createdBy.id = :userId) " +
           "GROUP BY YEAR(fr.transactionDate), MONTH(fr.transactionDate) " +
           "ORDER BY YEAR(fr.transactionDate) DESC, MONTH(fr.transactionDate) DESC")
    List<Object[]> monthlyTrends(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("userId") Long userId);

    @Query("SELECT fr FROM FinancialRecord fr WHERE fr.deleted = false " +
           "AND (:userId IS NULL OR fr.createdBy.id = :userId) " +
           "ORDER BY fr.transactionDate DESC, fr.createdAt DESC")
    List<FinancialRecord> findRecentActivity(@Param("userId") Long userId, Pageable pageable);

    // ── CRUD Queries ──────────────────────────────────────────────────────────

    /** Fetch by ID excluding soft-deleted records. */
    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);

    /**
     * Filtered list query supporting all optional params.
     * ADMIN passes userId=null to see all records; others pass their own ID.
     */
    @Query("SELECT fr FROM FinancialRecord fr WHERE fr.deleted = false " +
           "AND (:userId IS NULL OR fr.createdBy.id = :userId) " +
           "AND (:category IS NULL OR fr.category = :category) " +
           "AND (:type IS NULL OR fr.type = :type) " +
           "AND (:startDate IS NULL OR fr.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR fr.transactionDate <= :endDate) " +
           "ORDER BY fr.transactionDate DESC, fr.createdAt DESC")
    List<FinancialRecord> findAllByFilters(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("type") RecordTypeEnum type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}