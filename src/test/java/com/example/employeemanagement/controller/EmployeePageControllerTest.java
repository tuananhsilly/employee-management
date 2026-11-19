package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.service.EmployeeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Lab 6: Thymeleaf Employee pages. Each test starts and ends with no Employees. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EmployeePageControllerTest {

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
	void listShowsOnePageOfEmployeesWithDepartmentNames() throws Exception {
		for (int i = 1; i <= 12; i++) {
			create("Nhân Viên " + i, "nv" + i + "@example.com", 1L);
		}
		create("Trần Thị Bình", "binh@example.com", 2L);

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/list"))
				.andExpect(model().attribute("employees", hasProperty("totalElements", is(13L))))
				.andExpect(model().attribute("employees", hasProperty("content", hasSize(10))))
				.andExpect(content().string(containsString("Nhân Viên 1")))
				.andExpect(content().string(containsString("IT")));

		mockMvc.perform(get("/employees/list").param("page", "1"))
				.andExpect(model().attribute("employees", hasProperty("content", hasSize(3))))
				.andExpect(content().string(containsString("Trần Thị Bình")))
				.andExpect(content().string(containsString("HR")));
	}

	@Test
	void addFormOffersEveryDepartment() throws Exception {
		mockMvc.perform(get("/employees/add"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/form"))
				.andExpect(model().attributeExists("employee"))
				.andExpect(model().attribute("departments", hasSize(5)))
				.andExpect(content().string(containsString("<option value=\"3\">Sales</option>")));
	}

	@Test
	void addingValidEmployeeRedirectsToListWhereItAppears() throws Exception {
		mockMvc.perform(post("/employees/add")
						.param("fullName", "Nguyễn Văn An")
						.param("email", "an@example.com")
						.param("phone", "0901234567")
						.param("position", "Developer")
						.param("hireDate", "2024-03-01")
						.param("departmentId", "1"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/employees/list"))
				.andExpect(flash().attribute("message", "Đã thêm nhân viên Nguyễn Văn An"));

		mockMvc.perform(get("/employees/list"))
				.andExpect(content().string(containsString("Nguyễn Văn An")))
				.andExpect(content().string(containsString("01/03/2024")));
	}

	@Test
	void invalidInputRedisplaysFormWithVietnameseErrorsAndKeepsInput() throws Exception {
		mockMvc.perform(post("/employees/add")
						.param("fullName", "")
						.param("email", "khong-phai-email")
						.param("phone", "0901234567")
						.param("position", "Developer")
						.param("hireDate", "")
						.param("departmentId", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/form"))
				.andExpect(model().attributeHasFieldErrors("employee", "fullName", "email", "hireDate", "departmentId"))
				.andExpect(content().string(containsString("Họ tên không được để trống")))
				.andExpect(content().string(containsString("Email không đúng định dạng")))
				.andExpect(content().string(containsString("Ngày vào làm không được để trống")))
				.andExpect(content().string(containsString("Phòng ban không được để trống")))
				.andExpect(content().string(containsString("value=\"khong-phai-email\"")))
				.andExpect(content().string(containsString("value=\"0901234567\"")));

		mockMvc.perform(get("/employees/list"))
				.andExpect(model().attribute("employees", hasProperty("totalElements", is(0L))));
	}

	@Test
	void unreadableDateIsAFieldErrorInVietnamese() throws Exception {
		mockMvc.perform(post("/employees/add")
						.param("fullName", "Nguyễn Văn An")
						.param("email", "an@example.com")
						.param("hireDate", "31/02/2024")
						.param("departmentId", "1"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("employee", "hireDate"))
				.andExpect(content().string(containsString("Ngày vào làm không hợp lệ")))
				.andExpect(content().string(containsString("value=\"31/02/2024\"")));
	}

	@Test
	void emailUsedByAnotherEmployeeIsAFieldError() throws Exception {
		create("Nguyễn Văn An", "an@example.com", 1L);

		mockMvc.perform(post("/employees/add")
						.param("fullName", "Trần Thị Bình")
						.param("email", "an@example.com")
						.param("hireDate", "2024-03-01")
						.param("departmentId", "2"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/form"))
				.andExpect(model().attributeHasFieldErrors("employee", "email"))
				.andExpect(content().string(containsString("Email an@example.com đã được nhân viên khác sử dụng")))
				.andExpect(model().attribute("employee", hasProperty("fullName", is("Trần Thị Bình"))))
				.andExpect(content().string(containsString("value=\"an@example.com\"")));
	}

	@Test
	void unknownDepartmentIsAFieldError() throws Exception {
		mockMvc.perform(post("/employees/add")
						.param("fullName", "Nguyễn Văn An")
						.param("email", "an@example.com")
						.param("hireDate", "2024-03-01")
						.param("departmentId", "999"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("employee", "departmentId"))
				.andExpect(content().string(containsString("Phòng ban với id 999 không tồn tại")));
	}

	@Test
	void searchFiltersByNameAndDepartment() throws Exception {
		create("Nguyễn Văn An", "an@example.com", 1L);
		create("Nguyễn Thị Hoa", "hoa@example.com", 2L);
		create("Trần Thị Bình", "binh@example.com", 2L);

		mockMvc.perform(get("/employees/search"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/search"))
				.andExpect(model().attribute("employees", hasProperty("totalElements", is(3L))));

		mockMvc.perform(get("/employees/search").param("name", "nguyễn"))
				.andExpect(model().attribute("employees", hasProperty("totalElements", is(2L))))
				.andExpect(model().attribute("name", "nguyễn"));

		mockMvc.perform(get("/employees/search").param("departmentId", "2"))
				.andExpect(model().attribute("employees", hasProperty("totalElements", is(2L))))
				.andExpect(content().string(containsString("<option value=\"2\" selected=\"selected\">HR</option>")));

		mockMvc.perform(get("/employees/search").param("name", "nguyễn").param("departmentId", "2"))
				.andExpect(model().attribute("employees", hasProperty("content", hasSize(1))))
				.andExpect(model().attribute("employees",
						hasProperty("content", contains(hasProperty("email", is("hoa@example.com"))))));
	}

	@Test
	void editFormIsPrefilledAndSavingUpdatesTheEmployee() throws Exception {
		EmployeeResponse an = create("Nguyễn Văn An", "an@example.com", 1L);

		mockMvc.perform(get("/employees/{id}/edit", an.getId()))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/form"))
				.andExpect(model().attribute("employee", hasProperty("email", is("an@example.com"))))
				.andExpect(model().attribute("employeeId", an.getId()))
				.andExpect(content().string(containsString("action=\"/employees/" + an.getId() + "/edit\"")))
				.andExpect(content().string(containsString(an.getEmployeeCode())))
				.andExpect(content().string(containsString("value=\"2024-03-01\"")));

		mockMvc.perform(post("/employees/{id}/edit", an.getId())
						.param("fullName", "Nguyễn Văn An")
						.param("email", "an.nguyen@example.com")
						.param("position", "Team Lead")
						.param("hireDate", "2024-03-01")
						.param("departmentId", "3"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/employees/list"))
				.andExpect(flash().attribute("message", "Đã cập nhật nhân viên Nguyễn Văn An"));

		EmployeeResponse updated = employeeService.findById(an.getId());
		assertThat(updated.getEmail()).isEqualTo("an.nguyen@example.com");
		assertThat(updated.getDepartmentId()).isEqualTo(3L);
		assertThat(updated.getEmployeeCode()).isEqualTo(an.getEmployeeCode());
	}

	@Test
	void invalidEditRedisplaysFormForTheSameEmployee() throws Exception {
		EmployeeResponse an = create("Nguyễn Văn An", "an@example.com", 1L);

		mockMvc.perform(post("/employees/{id}/edit", an.getId())
						.param("fullName", "")
						.param("email", "an@example.com")
						.param("hireDate", "2024-03-01")
						.param("departmentId", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("employees/form"))
				.andExpect(model().attributeHasFieldErrors("employee", "fullName"))
				.andExpect(content().string(containsString("action=\"/employees/" + an.getId() + "/edit\"")));
	}

	@Test
	void editingUnknownEmployeeShowsNotFoundPage() throws Exception {
		mockMvc.perform(get("/employees/{id}/edit", Long.MAX_VALUE))
				.andExpect(status().isNotFound())
				.andExpect(view().name("employees/error"))
				.andExpect(content().string(containsString("Không tìm thấy nhân viên với id " + Long.MAX_VALUE)));

		mockMvc.perform(get("/employees/abc/edit"))
				.andExpect(status().isNotFound())
				.andExpect(view().name("employees/error"));
	}

	@Test
	void listOffersDeleteAsConfirmedPostForm() throws Exception {
		EmployeeResponse an = create("Nguyễn Văn An", "an@example.com", 1L);

		mockMvc.perform(get("/employees/list"))
				.andExpect(content().string(containsString(
						"action=\"/employees/" + an.getId() + "/delete\" method=\"post\"")))
				.andExpect(content().string(containsString("onsubmit=\"return confirm(")));
	}

	@Test
	void deleteRemovesEmployeeAndRedirectsToList() throws Exception {
		EmployeeResponse an = create("Nguyễn Văn An", "an@example.com", 1L);
		create("Trần Thị Bình", "binh@example.com", 1L);

		mockMvc.perform(post("/employees/{id}/delete", an.getId()))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/employees/list"))
				.andExpect(flash().attribute("message", "Đã xoá nhân viên Nguyễn Văn An"));

		mockMvc.perform(get("/employees/list"))
				.andExpect(model().attribute("employees",
						hasProperty("content", contains(hasProperty("email", is("binh@example.com"))))));
	}

	@Test
	void deletingUnknownEmployeeShowsNotFoundPage() throws Exception {
		mockMvc.perform(post("/employees/{id}/delete", Long.MAX_VALUE))
				.andExpect(status().isNotFound())
				.andExpect(view().name("employees/error"));
	}

	@Test
	void badQueryParameterShowsBadRequestPage() throws Exception {
		mockMvc.perform(get("/employees/search").param("departmentId", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("employees/error"))
				.andExpect(content().string(containsString("Tham số &#39;departmentId&#39; có giá trị sai kiểu dữ liệu")));
	}

	@Test
	void searchPagerKeepsTheFilters() throws Exception {
		for (int i = 1; i <= 11; i++) {
			create("Nguyễn Nhân Viên " + i, "nv" + i + "@example.com", 2L);
		}

		mockMvc.perform(get("/employees/search").param("name", "nguyễn").param("departmentId", "2"))
				.andExpect(model().attribute("employees", hasProperty("totalPages", is(2))))
				.andExpect(content().string(containsString(
						"href=\"/employees/search?name=nguy%E1%BB%85n&amp;departmentId=2&amp;page=1\"")));
	}

	private EmployeeResponse create(String fullName, String email, Long departmentId) {
		EmployeeRequest request = new EmployeeRequest();
		request.setFullName(fullName);
		request.setEmail(email);
		request.setPosition("Developer");
		request.setHireDate(LocalDate.of(2024, 3, 1));
		request.setDepartmentId(departmentId);
		return employeeService.create(request);
	}

}
