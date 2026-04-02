package com.financedashboard.zorvyn.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financedashboard.zorvyn.dto.DashboardSummaryResponse;
import com.financedashboard.zorvyn.dto.MonthlyTrendResponse;
import com.financedashboard.zorvyn.dto.RecordResponse;
import com.financedashboard.zorvyn.service.interfaces.FinancialDashboardService;
import com.financedashboard.zorvyn.service.util.AuthenticationHelper;
import com.financedashboard.zorvyn.service.util.UserResolutionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



/**
 * REST Controller for financial dashboard endpoints.
 * Delegates authentication extraction to AuthenticationHelper and user resolution to UserResolutionService.
 * Follows Single Responsibility Principle by keeping controller focused on HTTP request/response handling.
 */
@Slf4j
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class FinancialDashboardController {

    private final FinancialDashboardService financialDashboardService;
    private final AuthenticationHelper authenticationHelper;
    private final UserResolutionUtil userResolutionService;

    /**
     * GET /v1/dashboard/summary
     * Fetches complete financial dashboard summary including income, expenses, trends, and recent activity.
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary(Authentication authentication) {
        log.info("Dashboard summary endpoint called");

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        DashboardSummaryResponse response = financialDashboardService.summary(userEmail);
        log.info("Dashboard summary built successfully for userEmail={} with response={}", userEmail, response);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /v1/dashboard/category-totals
     * Fetches breakdown of totals by category for the authenticated user.
     */
    @GetMapping("/category-totals")
    public ResponseEntity<Map<String, BigDecimal>> categoryTotals(Authentication authentication) {
        log.info("Category totals endpoint called");

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        Long userId = userResolutionService.resolveUserIdFilterByEmail(userEmail);
        
        log.info("Resolved userId={} for email={}", userId, userEmail);
        Map<String, BigDecimal> response = financialDashboardService.calculateCategoryTotals(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /v1/dashboard/monthly-trends
     * Fetches monthly trend data for the authenticated user (last 12 months).
     */
    @GetMapping("/monthly-trends")
    public ResponseEntity<List<MonthlyTrendResponse>> monthlyTrends(Authentication authentication) {
        log.info("Monthly trends endpoint called");

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        Long userId = userResolutionService.resolveUserIdFilterByEmail(userEmail);
        log.info("Resolved userId={} for email={}", userId, userEmail);
        List<MonthlyTrendResponse> response = financialDashboardService.calculateMonthlyTrends(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /v1/dashboard/recent-activity
     * Fetches the 5 most recent transactions for the authenticated user.
     */
    @GetMapping("/recent-activity")
    public ResponseEntity<List<RecordResponse>> recentActivity(Authentication authentication) {
        log.info("Recent activity endpoint called");

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        Long userId = userResolutionService.resolveUserIdFilterByEmail(userEmail);
        
        log.info("Resolved userId={} for email={}", userId, userEmail);
        List<RecordResponse> response = financialDashboardService.calculateRecentActivity(userId);

        return ResponseEntity.ok(response);
    }

}