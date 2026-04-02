package com.financedashboard.zorvyn.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.financedashboard.zorvyn.dto.DashboardSummaryResponse;
import com.financedashboard.zorvyn.dto.MonthlyTrendResponse;
import com.financedashboard.zorvyn.dto.RecordResponse;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.repository.interfaces.FinancialRecordRepository;
import com.financedashboard.zorvyn.service.interfaces.FinancialDashboardService;
import com.financedashboard.zorvyn.service.util.DataMapperUtil;
import com.financedashboard.zorvyn.service.util.UserResolutionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Service for fetching and aggregating financial dashboard data.
 * Orchestrates repository calls and utility services to build dashboard summary.
 * Follows Single Responsibility Principle by delegating cross-cutting concerns to utility services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialDashboardServiceImpl implements FinancialDashboardService {

    private final FinancialRecordRepository financialRecordRepository;
    private final UserResolutionUtil userResolutionService;
    private final DataMapperUtil dataMapperUtil;

    /**
     * Builds complete financial dashboard summary for authenticated user.
     * Includes income, expenses, net balance, categories, trends, and recent activity.
     * 
     * @param userEmail the authenticated user's email
     * @return DashboardSummaryResponse with all dashboard components
     * @throws FinancialDashboardException if dashboard summary generation fails
     */
    @Override
    public DashboardSummaryResponse summary(String userEmail) {
        log.info("Building dashboard summary for user={}", userEmail);

        try {
            // Resolve user and apply role-based filtering
            User user = userResolutionService.getUserOrThrow(userEmail);
            Long userIdFilter = userResolutionService.resolveUserIdFilter(user);

            // Fetch aggregated data from repository
            BigDecimal totalIncome = financialRecordRepository.sumIncome(userIdFilter);
            BigDecimal totalExpense = financialRecordRepository.sumExpense(userIdFilter);
            
            // Validate calculations
            if (totalIncome == null || totalExpense == null) {
                log.warn("Null values detected in income/expense calculation for user={}", userEmail);
                throw new FinancialDashboardException(
                        ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getErrorCode(),
                        ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getErrorMessage(),
                        ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getHttpStatus()
                );
            }
            
            BigDecimal netBalance = totalIncome.subtract(totalExpense);

            // Get all components for dashboard
            Map<String, BigDecimal> categoryTotals = calculateCategoryTotals(userIdFilter);
            List<MonthlyTrendResponse> monthlyTrends = calculateMonthlyTrends(userIdFilter);
            List<RecordResponse> recentActivity = calculateRecentActivity(userIdFilter);

            log.info("Dashboard summary built successfully for user={}", userEmail);
            return buildResponse(totalIncome, totalExpense, netBalance,
                    categoryTotals, recentActivity, monthlyTrends);
                    
        } catch (FinancialDashboardException ex) {
            // Re-throw dashboard exceptions as-is
            throw ex;
        } catch (Exception ex) {
            log.error("Error building dashboard summary for user={}", userEmail, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.DASHBOARD_SUMMARY_ERROR.getErrorCode(),
                    ErrorCodeEnum.DASHBOARD_SUMMARY_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DASHBOARD_SUMMARY_ERROR.getHttpStatus()
            );
        }
    }

    /**
     * Calculate category-wise expense/income totals for a user.
     * 
     * @param userId the user ID (null for ADMIN = all records)
     * @return Map of category name to total amount
     * @throws FinancialDashboardException if category calculation fails
     */
    @Override
    public Map<String, BigDecimal> calculateCategoryTotals(Long userId) {
        log.debug("Calculating category totals for userId={}", userId);
        
        try {
            List<Object[]> rows = financialRecordRepository.categoryTotals(userId);
            
            if (rows == null || rows.isEmpty()) {
                log.warn("No category data found for userId={}", userId);
                return dataMapperUtil.mapCategoryTotals(rows);
            }
            
            Map<String, BigDecimal> result = dataMapperUtil.mapCategoryTotals(rows);
            log.debug("Category totals calculated successfully for userId={}", userId);
            return result;
            
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error calculating category totals for userId={}", userId, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getErrorCode(),
                    ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getHttpStatus()
            );
        }
    }

    /**
     * Calculate monthly trend totals for the last 12 months.
     * 
     * @param userId the user ID (null for ADMIN = all records)
     * @return List of monthly trend responses sorted by year/month descending
     * @throws FinancialDashboardException if monthly trend calculation fails
     */
    @Override
    public List<MonthlyTrendResponse> calculateMonthlyTrends(Long userId) {
        log.debug("Calculating monthly trends for userId={}", userId);
        
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusMonths(11).withDayOfMonth(1);

            // Validate date range
            if (startDate.isAfter(endDate)) {
                log.warn("Invalid date range for monthly trends: start={}, end={}", startDate, endDate);
                throw new FinancialDashboardException(
                        ErrorCodeEnum.DASHBOARD_INVALID_DATE_RANGE.getErrorCode(),
                        ErrorCodeEnum.DASHBOARD_INVALID_DATE_RANGE.getErrorMessage(),
                        ErrorCodeEnum.DASHBOARD_INVALID_DATE_RANGE.getHttpStatus()
                );
            }

            List<Object[]> rows = financialRecordRepository.monthlyTrends(startDate, endDate, userId);
            List<MonthlyTrendResponse> result = dataMapperUtil.mapMonthlyTrends(rows);
            
            log.debug("Monthly trends calculated successfully for userId={}", userId);
            return result;
            
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error calculating monthly trends for userId={}", userId, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getErrorCode(),
                    ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getHttpStatus()
            );
        }
    }

    /**
     * Fetch the 5 most recent transactions for a user.
     * 
     * @param userId the user ID (null for ADMIN = all records)
     * @return List of recent financial records
     * @throws FinancialDashboardException if recent activity fetch fails
     */
    @Override
    public List<RecordResponse> calculateRecentActivity(Long userId) {
        log.debug("Fetching recent activity for userId={}", userId);
        
        try {
            Pageable pageable = PageRequest.of(0, 5);

            List<RecordResponse> result = financialRecordRepository.findRecentActivity(userId, pageable)
                    .stream()
                    .map(RecordResponse::fromEntity)
                    .toList();
            
            log.debug("Recent activity fetched successfully for userId={}", userId);
            return result;
            
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching recent activity for userId={}", userId, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.DASHBOARD_FETCH_FAILED.getErrorCode(),
                    ErrorCodeEnum.DASHBOARD_FETCH_FAILED.getErrorMessage(),
                    ErrorCodeEnum.DASHBOARD_FETCH_FAILED.getHttpStatus()
            );
        }
    }

    /**
     * Builds the final DashboardSummaryResponse DTO from all calculated components.
     * This method encapsulates DTO construction logic for better maintainability.
     * 
     * @param income total income amount
     * @param expense total expense amount
     * @param net net balance (income - expense)
     * @param categoryTotals category-wise totals map
     * @param recent recent activity records
     * @param trends monthly trend data
     * @return DashboardSummaryResponse with all components
     * @throws FinancialDashboardException if response building fails
     */
    private DashboardSummaryResponse buildResponse(
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net,
            Map<String, BigDecimal> categoryTotals,
            List<RecordResponse> recent,
            List<MonthlyTrendResponse> trends
    ) {
        try {
            log.debug("Building dashboard summary response with calculated components");
            
            // Validate input parameters
            if (income == null || expense == null || net == null) {
                log.warn("Null values detected in response building parameters");
                throw new FinancialDashboardException(
                        ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getErrorCode(),
                        ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getErrorMessage(),
                        ErrorCodeEnum.DASHBOARD_CALCULATION_ERROR.getHttpStatus()
                );
            }
            
            return DashboardSummaryResponse.builder()
                    .totalIncome(income)
                    .totalExpense(expense)
                    .netBalance(net)
                    .categoryTotals(categoryTotals)
                    .recentActivity(recent)
                    .monthlyTrends(trends)
                    .build();
                    
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error building dashboard response", ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.DASHBOARD_SUMMARY_ERROR.getErrorCode(),
                    ErrorCodeEnum.DASHBOARD_SUMMARY_ERROR.getErrorMessage(),
                    ErrorCodeEnum.DASHBOARD_SUMMARY_ERROR.getHttpStatus()
            );
        }
    }

}
