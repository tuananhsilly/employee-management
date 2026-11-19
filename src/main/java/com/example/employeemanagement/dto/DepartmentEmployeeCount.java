package com.example.employeemanagement.dto;

/** One element of {@code GET /api/statistics/by-department}. */
public record DepartmentEmployeeCount(Long departmentId, String departmentName, long employeeCount) {
}
