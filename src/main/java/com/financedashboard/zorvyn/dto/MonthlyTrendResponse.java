package com.financedashboard.zorvyn.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyTrendResponse {
    private int year;
    private int month;
    private BigDecimal total;
}
