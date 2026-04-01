package com.financedashboard.zorvyn.service.interfaces;

import com.financedashboard.zorvyn.dto.DashboardSummaryResponse;

public interface FinancialDashboardService {
	
	DashboardSummaryResponse summary(String userEmail);
	
	public void calculateCategoryTotals();
	
	public void calculateMonthlyTrends();
	
	public void viewRecentActivity();
	
}