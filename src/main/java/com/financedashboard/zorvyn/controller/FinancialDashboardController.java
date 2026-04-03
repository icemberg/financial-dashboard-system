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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for financial dashboard analytics — /v1/dashboard
 * Provides aggregated views of financial data.
 *
 * All endpoints are role-scoped:
 * ADMIN sees aggregates over ALL records.
 * VIEWER/ANALYST see aggregates over their OWN records only.
 */
@Slf4j
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Analytics", description = "Aggregated financial summaries, category breakdowns, trends, and recent activity")
public class FinancialDashboardController {

        private final FinancialDashboardService financialDashboardService;
        private final AuthenticationHelper authenticationHelper;
        private final UserResolutionUtil userResolutionService;

        @Operation(summary = "Get full dashboard summary", description = "Returns a composite summary including total income, total expenses, net balance, "
                        + "category-wise breakdown, monthly trends (last 12 months), and 5 most recent transactions. "
                        + "ADMIN sees data for all users; others see only their own data.")
        @ApiResponse(responseCode = "200", description = "Dashboard summary retrieved")
        @ApiResponse(responseCode = "401", description = "Not authenticated")
        @GetMapping("/summary")
        public ResponseEntity<DashboardSummaryResponse> summary(Authentication authentication) {
                log.info("Dashboard summary endpoint called");

                String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
                DashboardSummaryResponse response = financialDashboardService.summary(userEmail);
                log.info("Dashboard summary built successfully for userEmail={} with response={}", userEmail, response);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get category-wise totals", description = "Returns a map of category names to their total amounts (sum of all non-deleted records). "
                        + "Example: {\"Food\": 8200.50, \"Salary\": 45000.00}. Role-scoped.")
        @ApiResponse(responseCode = "200", description = "Category totals retrieved")
        @ApiResponse(responseCode = "401", description = "Not authenticated")
        @GetMapping("/category-totals")
        public ResponseEntity<Map<String, BigDecimal>> categoryTotals(Authentication authentication) {
                log.info("Category totals endpoint called");

                String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
                Long userId = userResolutionService.resolveUserIdFilterByEmail(userEmail);

                log.info("Resolved userId={} for email={}", userId, userEmail);
                Map<String, BigDecimal> response = financialDashboardService.calculateCategoryTotals(userId);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get monthly trends", description = "Returns monthly totals for the last 12 months, sorted newest first. "
                        + "Each entry contains year, month, and total amount. Role-scoped.")
        @ApiResponse(responseCode = "200", description = "Monthly trends retrieved")
        @ApiResponse(responseCode = "401", description = "Not authenticated")
        @GetMapping("/monthly-trends")
        public ResponseEntity<List<MonthlyTrendResponse>> monthlyTrends(Authentication authentication) {
                log.info("Monthly trends endpoint called");

                String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
                Long userId = userResolutionService.resolveUserIdFilterByEmail(userEmail);
                log.info("Resolved userId={} for email={}", userId, userEmail);
                List<MonthlyTrendResponse> response = financialDashboardService.calculateMonthlyTrends(userId);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get recent activity", description = "Returns the 5 most recent transactions ordered by transaction date descending. Role-scoped.")
        @ApiResponse(responseCode = "200", description = "Recent activity retrieved")
        @ApiResponse(responseCode = "401", description = "Not authenticated")
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