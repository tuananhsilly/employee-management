package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.DepartmentEmployeeCount;
import com.example.employeemanagement.dto.EmployeeTotal;
import com.example.employeemanagement.dto.MonthlyHires;
import com.example.employeemanagement.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping("/by-department")
	public List<DepartmentEmployeeCount> byDepartment() {
		return statisticsService.employeeCountByDepartment();
	}

	@GetMapping("/total")
	public EmployeeTotal total() {
		return statisticsService.employeeTotal();
	}

	@GetMapping("/hires-by-month")
	public List<MonthlyHires> hiresByMonth() {
		return statisticsService.hiresByMonth();
	}

}
