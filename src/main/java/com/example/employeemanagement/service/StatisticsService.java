package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.DepartmentEmployeeCount;
import com.example.employeemanagement.dto.EmployeeTotal;
import com.example.employeemanagement.dto.MonthlyHires;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.repository.HiresInMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Workforce statistics, each computed by an aggregate query. */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

	private static final int HIRES_MONTHS = 12;

	private final EmployeeRepository employeeRepository;

	private final DepartmentRepository departmentRepository;

	public StatisticsService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
		this.employeeRepository = employeeRepository;
		this.departmentRepository = departmentRepository;
	}

	public List<DepartmentEmployeeCount> employeeCountByDepartment() {
		return departmentRepository.countEmployeesByDepartment();
	}

	public EmployeeTotal employeeTotal() {
		return new EmployeeTotal(employeeRepository.countEmployees());
	}

	/**
	 * Hires in each of the last 12 months, this month included, oldest first; a month with
	 * no hires is reported as 0.
	 */
	public List<MonthlyHires> hiresByMonth() {
		YearMonth last = YearMonth.now();
		YearMonth first = last.minusMonths(HIRES_MONTHS - 1);
		Map<YearMonth, Long> hires = employeeRepository
				.countHiresByMonth(first.atDay(1), last.plusMonths(1).atDay(1))
				.stream()
				.collect(Collectors.toMap(row -> YearMonth.of(row.year(), row.month()), HiresInMonth::hires));
		return Stream.iterate(first, month -> month.plusMonths(1))
				.limit(HIRES_MONTHS)
				.map(month -> new MonthlyHires(month, hires.getOrDefault(month, 0L)))
				.toList();
	}

}
