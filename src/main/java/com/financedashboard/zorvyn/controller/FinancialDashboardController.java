package com.financedashboard.zorvyn.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financedashboard.zorvyn.dto.DashboardSummaryResponse;
import com.financedashboard.zorvyn.service.interfaces.FinancialDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class FinancialDashboardController {
	
	private final FinancialDashboardService financialDashboardService;
	
	@GetMapping("/summary")
	public ResponseEntity<DashboardSummaryResponse> summary(Authentication authentication) {
		log.info("FinancialDashboardController: summary endpoint called by principal={}", authentication == null ? "anonymous" : authentication.getName());

		String userEmail = authentication == null ? null : authentication.getName();

		DashboardSummaryResponse response = financialDashboardService.summary(userEmail);

		return ResponseEntity.ok(response);
	}
	
}