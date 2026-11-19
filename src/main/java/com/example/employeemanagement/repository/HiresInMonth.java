package com.example.employeemanagement.repository;

/** A row of {@link EmployeeRepository#countHiresByMonth}: only months with at least one hire appear. */
public record HiresInMonth(Integer year, Integer month, Long hires) {
}
