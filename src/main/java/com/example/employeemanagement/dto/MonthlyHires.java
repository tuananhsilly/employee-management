package com.example.employeemanagement.dto;

import java.time.YearMonth;

/** One element of {@code GET /api/statistics/hires-by-month}; {@code month} is written as {@code yyyy-MM}. */
public record MonthlyHires(YearMonth month, long hires) {
}
