package com.financedashboard.zorvyn.service.util;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.financedashboard.zorvyn.dto.MonthlyTrendResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for transforming repository query results (Object[] rows) to DTOs.
 * Responsible for mapping raw database results to type-safe data transfer objects.
 * Follows Single Responsibility Principle by centralizing data transformation logic.
 */
@Slf4j
@Component
public class DataMapperUtil {

    /**
     * Maps category totals from repository Object[] rows to a typed Map.
     * Object[0] = category name (String)
     * Object[1] = total amount (BigDecimal)
     *
     * @param rows the raw repository query results
     * @return Map of category name to total amount, empty map if rows is null
     */
    public Map<String, BigDecimal> mapCategoryTotals(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            log.debug("Category totals rows are null or empty, returning empty map");
            return Collections.emptyMap();
        }

        log.debug("Mapping {} category total rows", rows.size());
        return rows.stream()
                .collect(
                        LinkedHashMap::new,
                        (map, row) -> map.put((String) row[0], (BigDecimal) row[1]),
                        Map::putAll
                );
    }

    /**
     * Maps monthly trends from repository Object[] rows to MonthlyTrendResponse list.
     * Object[0] = year (Number)
     * Object[1] = month (Number)
     * Object[2] = total amount (BigDecimal)
     *
     * @param rows the raw repository query results
     * @return List of MonthlyTrendResponse, empty list if rows is null
     */
    public List<MonthlyTrendResponse> mapMonthlyTrends(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            log.debug("Monthly trends rows are null or empty, returning empty list");
            return Collections.emptyList();
        }

        log.debug("Mapping {} monthly trend rows", rows.size());
        return rows.stream()
                .map(row -> new MonthlyTrendResponse(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        (BigDecimal) row[2]
                ))
                .toList();
    }
}