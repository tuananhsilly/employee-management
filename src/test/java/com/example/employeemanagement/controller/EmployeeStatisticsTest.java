package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import com.example.employeemanagement.dto.DepartmentEmployeeCount;
import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeTotal;
import com.example.employeemanagement.service.EmployeeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Lab 10: Employee statistics over the API and on a page. Each test starts and ends with no Employees. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@WithMockUser(roles = "USER")
class EmployeeStatisticsTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcClient jdbcClient;

	@Autowired
	EmployeeService employeeService;

	@BeforeEach
	@AfterEach
	void deleteAllEmployees() {
		jdbcClient.sql("DELETE FROM employee").update();
	}

	@Test
	void totalCountsEveryEmployee() throws Exception {
		mockMvc.perform(get("/api/statistics/total"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalEmployees").value(0));

		create("an@example.com", 1L, LocalDate.of(2020, 1, 15));
		create("binh@example.com", 2L, LocalDate.of(2021, 6, 1));
		create("cuong@example.com", 2L, LocalDate.of(2022, 9, 30));

		mockMvc.perform(get("/api/statistics/total"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalEmployees").value(3));
	}

	@Test
	void byDepartmentCountsEveryDepartmentIncludingEmptyOnes() throws Exception {
		create("an@example.com", 1L, LocalDate.of(2020, 1, 15));
		create("binh@example.com", 2L, LocalDate.of(2021, 6, 1));
		create("cuong@example.com", 2L, LocalDate.of(2022, 9, 30));
		create("dung@example.com", 2L, LocalDate.of(2023, 2, 10));
		create("em@example.com", 5L, LocalDate.of(2023, 4, 4));

		// Seeded Departments: 1 IT, 2 HR, 3 Sales, 4 Finance, 5 Marketing.
		mockMvc.perform(get("/api/statistics/by-department"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[*].departmentName").value(contains("IT", "HR", "Sales", "Finance", "Marketing")))
				.andExpect(jsonPath("$[*].departmentId").value(contains(1, 2, 3, 4, 5)))
				.andExpect(jsonPath("$[*].employeeCount").value(contains(1, 3, 0, 0, 1)));
	}

	@Test
	void hiresByMonthCoversTheLastTwelveMonthsOldestFirstWithZeros() throws Exception {
		YearMonth thisMonth = YearMonth.now();
		create("an@example.com", 1L, thisMonth.atDay(1));
		create("binh@example.com", 2L, thisMonth.minusMonths(3).atDay(5));
		create("cuong@example.com", 3L, thisMonth.minusMonths(3).atEndOfMonth());
		create("dung@example.com", 1L, thisMonth.minusMonths(11).atDay(1));
		// Outside the window: the month before it, and long ago.
		create("em@example.com", 1L, thisMonth.minusMonths(12).atEndOfMonth());
		create("giang@example.com", 1L, LocalDate.of(2015, 7, 1));

		mockMvc.perform(get("/api/statistics/hires-by-month"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(12))
				.andExpect(jsonPath("$[0].month").value(thisMonth.minusMonths(11).toString()))
				.andExpect(jsonPath("$[8].month").value(thisMonth.minusMonths(3).toString()))
				.andExpect(jsonPath("$[11].month").value(thisMonth.toString()))
				.andExpect(jsonPath("$[*].hires").value(contains(1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 1)));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void statisticsPageShowsAllThreeStatistics() throws Exception {
		YearMonth thisMonth = YearMonth.now();
		create("an@example.com", 1L, thisMonth.atDay(1));
		create("binh@example.com", 2L, thisMonth.minusMonths(2).atDay(1));
		create("cuong@example.com", 2L, LocalDate.of(2015, 7, 1));

		mockMvc.perform(get("/employees/statistics"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/statistics"))
				.andExpect(model().attribute("total", new EmployeeTotal(3)))
				.andExpect(model().attribute("byDepartment", List.of(
						new DepartmentEmployeeCount(1L, "IT", 1), new DepartmentEmployeeCount(2L, "HR", 2),
						new DepartmentEmployeeCount(3L, "Sales", 0), new DepartmentEmployeeCount(4L, "Finance", 0),
						new DepartmentEmployeeCount(5L, "Marketing", 0))))
				.andExpect(model().attribute("hiresByMonth", hasSize(12)))
				.andExpect(content().string(containsString("Tổng số nhân viên")))
				.andExpect(content().string(containsString("Marketing")))
				.andExpect(content().string(containsString(monthLabel(thisMonth.minusMonths(11)))))
				.andExpect(content().string(containsString(monthLabel(thisMonth))));
	}

	@ParameterizedTest
	@ValueSource(strings = { "USER", "ADMIN" })
	void bothRolesSeeTheStatistics(String role) throws Exception {
		for (String url : List.of("/api/statistics/total", "/api/statistics/by-department",
				"/api/statistics/hires-by-month", "/employees/statistics")) {
			mockMvc.perform(get(url).with(user("hr.lan").roles(role))).andExpect(status().isOk());
		}
	}

	@Test
	@WithAnonymousUser
	void statisticsNeedALogin() throws Exception {
		mockMvc.perform(get("/api/statistics/total")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/employees/statistics")).andExpect(status().is3xxRedirection());
	}

	private static String monthLabel(YearMonth month) {
		return "%02d/%d".formatted(month.getMonthValue(), month.getYear());
	}

	private void create(String email, Long departmentId, LocalDate hireDate) {
		EmployeeRequest request = new EmployeeRequest();
		request.setFullName("Nhân Viên " + email);
		request.setEmail(email);
		request.setPosition("Developer");
		request.setHireDate(hireDate);
		request.setDepartmentId(departmentId);
		employeeService.create(request);
	}

}
