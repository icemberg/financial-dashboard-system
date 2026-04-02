package com.financedashboard.zorvyn.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class MonthlyTrendResponse {
    private int year;
    private int month;
    private BigDecimal total;
}
