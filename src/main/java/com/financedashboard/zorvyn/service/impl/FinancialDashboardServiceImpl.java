package com.financedashboard.zorvyn.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.financedashboard.zorvyn.dto.DashboardSummaryResponse;
import com.financedashboard.zorvyn.dto.MonthlyTrendResponse;
import com.financedashboard.zorvyn.dto.RecordResponse;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.repository.interfaces.FinancialRecordRepository;
import com.financedashboard.zorvyn.repository.interfaces.UserRepository;
import com.financedashboard.zorvyn.service.interfaces.FinancialDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialDashboardServiceImpl implements FinancialDashboardService {

    private final FinancialRecordRepository financialRecordRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardSummaryResponse summary(String userEmail) {
        log.info("FinancialDashboardServiceImpl: building dashboard summary for user={}", userEmail);

        Optional<User> maybeUser = userRepository.findByEmail(userEmail);
        if (maybeUser.isEmpty()) {
            throw new FinancialDashboardException("User not found for email: " + userEmail, ErrorCodeEnum.USER_NOT_FOUND, org.springframework.http.HttpStatus.NOT_FOUND);
        }

        User user = maybeUser.get();

        Long userIdFilter = null;
        if (user.getRole() != RolesEnum.ADMIN) {
            userIdFilter = user.getId();
        }

        // Aggregations from repository (database-side)
        BigDecimal totalIncome = financialRecordRepository.sumIncome(userIdFilter);
        BigDecimal totalExpense = financialRecordRepository.sumExpense(userIdFilter);

        // Category totals
        List<Object[]> categoryRows = financialRecordRepository.categoryTotals(userIdFilter);
        Map<String, BigDecimal> categoryTotals = categoryRows == null ? Collections.emptyMap()
                : categoryRows.stream().collect(LinkedHashMap::new,
                        (m, row) -> m.put((String) row[0], (BigDecimal) row[1]), Map::putAll);

        // Monthly trends - by default last 12 months (optional)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(11).withDayOfMonth(1);
        List<Object[]> trendRows = financialRecordRepository.monthlyTrends(startDate, endDate, userIdFilter);
        List<MonthlyTrendResponse> monthlyTrends = trendRows == null ? Collections.emptyList()
                : trendRows.stream().map(row -> new MonthlyTrendResponse(((Number) row[0]).intValue(), ((Number) row[1]).intValue(), (BigDecimal) row[2]))
                        .collect(Collectors.toList());

        // Recent activity - limit 5
        Pageable recentPage = PageRequest.of(0, 5);
        List<RecordResponse> recentActivity = financialRecordRepository.findRecentActivity(userIdFilter, recentPage).stream()
                .map(RecordResponse::fromEntity)
                .collect(Collectors.toList());

        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .categoryTotals(categoryTotals)
                .recentActivity(recentActivity)
                .monthlyTrends(monthlyTrends)
                .build();

        return response;
    }

    @Override
    public void calculateCategoryTotals() {
        // unused
    }

    @Override
    public void calculateMonthlyTrends() {
        // unused
    }

    @Override
    public void viewRecentActivity() {
        // unused
    }

}