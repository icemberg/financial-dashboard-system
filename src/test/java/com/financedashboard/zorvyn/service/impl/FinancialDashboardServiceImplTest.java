package com.financedashboard.zorvyn.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.financedashboard.zorvyn.dto.*;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.repository.interfaces.FinancialRecordRepository;
import com.financedashboard.zorvyn.service.util.DataMapperUtil;
import com.financedashboard.zorvyn.service.util.UserResolutionUtil;

@ExtendWith(MockitoExtension.class)
class FinancialDashboardServiceImplTest {

    @Mock
    private FinancialRecordRepository financialRecordRepository;

    @Mock
    private UserResolutionUtil userResolutionUtil;

    @Mock
    private DataMapperUtil dataMapperUtil;

    @InjectMocks
    private FinancialDashboardServiceImpl dashboardService;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .role(RolesEnum.VIEWER)
                .build();
    }

    // ─────────────────────────────────────────────
    // ✅ SUMMARY TEST
    // ─────────────────────────────────────────────

    @Test
    void summary_success() {

        // Arrange
        when(userResolutionUtil.getUserOrThrow("test@example.com")).thenReturn(user);
        when(userResolutionUtil.resolveUserIdFilter(user)).thenReturn(1L);

        when(financialRecordRepository.sumIncome(1L))
                .thenReturn(BigDecimal.valueOf(1000));

        when(financialRecordRepository.sumExpense(1L))
                .thenReturn(BigDecimal.valueOf(400));

        // ✅ FIX: Explicit List<Object[]>
        List<Object[]> categoryRows = new ArrayList<>();
        categoryRows.add(new Object[] { "Food", BigDecimal.valueOf(200) });

        when(financialRecordRepository.categoryTotals(1L))
                .thenReturn(categoryRows);

        when(dataMapperUtil.mapCategoryTotals(categoryRows))
                .thenReturn(Map.of("Food", BigDecimal.valueOf(200)));

        when(financialRecordRepository.monthlyTrends(any(LocalDate.class), any(LocalDate.class), eq(1L)))
                .thenReturn(Collections.emptyList());

        when(dataMapperUtil.mapMonthlyTrends(any()))
                .thenReturn(Collections.emptyList());

        when(financialRecordRepository.findRecentActivity(eq(1L), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        // Act
        DashboardSummaryResponse response = dashboardService.summary("test@example.com");

        // Assert
        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(1000), response.getTotalIncome());
        assertEquals(BigDecimal.valueOf(400), response.getTotalExpense());
        assertEquals(BigDecimal.valueOf(600), response.getNetBalance());
    }

    // ─────────────────────────────────────────────
    // ❌ NULL INCOME TEST
    // ─────────────────────────────────────────────

    @Test
    void summary_nullIncome_shouldThrow() {

        when(userResolutionUtil.getUserOrThrow(any()))
                .thenReturn(user);

        when(userResolutionUtil.resolveUserIdFilter(user))
                .thenReturn(1L);

        when(financialRecordRepository.sumIncome(1L))
                .thenReturn(null);

        when(financialRecordRepository.sumExpense(1L))
                .thenReturn(BigDecimal.TEN);

        assertThrows(FinancialDashboardException.class,
                () -> dashboardService.summary("test@example.com"));
    }

    // ─────────────────────────────────────────────
    // ✅ CATEGORY TOTALS
    // ─────────────────────────────────────────────

    @Test
    void calculateCategoryTotals_success() {

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { "Food", BigDecimal.valueOf(200) });

        when(financialRecordRepository.categoryTotals(1L))
                .thenReturn(rows);

        when(dataMapperUtil.mapCategoryTotals(rows))
                .thenReturn(Map.of("Food", BigDecimal.valueOf(200)));

        Map<String, BigDecimal> result = dashboardService.calculateCategoryTotals(1L);

        assertEquals(1, result.size());
        assertEquals(BigDecimal.valueOf(200), result.get("Food"));
    }

    @Test
    void calculateCategoryTotals_empty() {

        when(financialRecordRepository.categoryTotals(1L))
                .thenReturn(Collections.emptyList());

        when(dataMapperUtil.mapCategoryTotals(any()))
                .thenReturn(Collections.emptyMap());

        Map<String, BigDecimal> result = dashboardService.calculateCategoryTotals(1L);

        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────────
    // ✅ MONTHLY TRENDS
    // ─────────────────────────────────────────────

    @Test
    void calculateMonthlyTrends_success() {

        when(financialRecordRepository.monthlyTrends(any(), any(), eq(1L)))
                .thenReturn(Collections.emptyList());

        when(dataMapperUtil.mapMonthlyTrends(any()))
                .thenReturn(Collections.emptyList());

        List<MonthlyTrendResponse> result = dashboardService.calculateMonthlyTrends(1L);

        assertNotNull(result);
    }

    // ─────────────────────────────────────────────
    // ✅ RECENT ACTIVITY
    // ─────────────────────────────────────────────

    @Test
    void calculateRecentActivity_success() {

        when(financialRecordRepository.findRecentActivity(eq(1L), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        List<RecordResponse> result = dashboardService.calculateRecentActivity(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void calculateRecentActivity_exception() {

        when(financialRecordRepository.findRecentActivity(eq(1L), any(Pageable.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(FinancialDashboardException.class,
                () -> dashboardService.calculateRecentActivity(1L));
    }
}