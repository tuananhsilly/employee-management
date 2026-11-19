package com.example.employeemanagement.controller;

import com.example.employeemanagement.service.StatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The statistics page: the same three statistics as {@code /api/statistics/**}, for both Roles. */
@Controller
public class StatisticsPageController {

	private final StatisticsService statisticsService;

	public StatisticsPageController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping("/employees/statistics")
	public String statistics(Model model) {
		model.addAttribute("total", statisticsService.employeeTotal());
		model.addAttribute("byDepartment", statisticsService.employeeCountByDepartment());
		model.addAttribute("hiresByMonth", statisticsService.hiresByMonth());
		return "employees/statistics";
	}

}
