package com.financedashboard.zorvyn.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
public class DashboardSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netBalance;

    private Map<String, BigDecimal> categoryTotals;

    private List<RecordResponse> recentActivity;

    private List<MonthlyTrendResponse> monthlyTrends;

}