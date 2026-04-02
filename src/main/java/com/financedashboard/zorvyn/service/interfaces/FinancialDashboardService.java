package com.financedashboard.zorvyn.service.interfaces;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.financedashboard.zorvyn.dto.DashboardSummaryResponse;
import com.financedashboard.zorvyn.dto.MonthlyTrendResponse;
import com.financedashboard.zorvyn.dto.RecordResponse;

public interface FinancialDashboardService {

    /**
     * Fetch complete financial dashboard summary for authenticated user.
     * Includes income, expenses, net balance, categories, trends, and recent activity.
     *
     * @param userEmail the authenticated user's email
     * @return DashboardSummaryResponse with all dashboard components
     */
    DashboardSummaryResponse summary(String userEmail);

    /**
     * Calculate category-wise expense/income totals for a user.
     *
     * @param userId the user ID (null for ADMIN = all records)
     * @return Map of category name to total amount
     */
    Map<String, BigDecimal> calculateCategoryTotals(Long userId);

    /**
     * Calculate monthly trend totals for the last 12 months.
     *
     * @param userId the user ID (null for ADMIN = all records)
     * @return List of monthly trend responses sorted by year/month descending
     */
    List<MonthlyTrendResponse> calculateMonthlyTrends(Long userId);

    /**
     * Fetch the 5 most recent transactions for a user.
     *
     * @param userId the user ID (null for ADMIN = all records)
     * @return List of recent financial records
     */
    List<RecordResponse> calculateRecentActivity(Long userId);

}