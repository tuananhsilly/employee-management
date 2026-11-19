package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lab 4: update, delete, search and pagination. Each test starts and ends with no Employees. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@WithMockUser(roles = "ADMIN")
class EmployeeCrudAndSearchTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcClient jdbcClient;

	@BeforeEach
	@AfterEach
	void deleteAllEmployees() {
		jdbcClient.sql("DELETE FROM employee").update();
	}

	@Test
	void updateChangesFieldsButKeepsEmployeeCode() throws Exception {
		MockHttpServletResponse created = create("Nguyễn Văn An", "an@example.com", 1).andReturn().getResponse();
		String code = JsonPath.read(created.getContentAsString(), "$.employeeCode");
		String location = created.getHeader("Location");

		mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
						.content(body("Nguyễn Văn An", "an.nguyen@example.com", 2, "Team Lead")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.employeeCode").value(code))
				.andExpect(jsonPath("$.email").value("an.nguyen@example.com"))
				.andExpect(jsonPath("$.position").value("Team Lead"))
				.andExpect(jsonPath("$.departmentId").value(2));

		mockMvc.perform(get(location))
				.andExpect(jsonPath("$.email").value("an.nguyen@example.com"))
				.andExpect(jsonPath("$.departmentId").value(2));
	}

	@Test
	void updateUnknownIdReturns404() throws Exception {
		mockMvc.perform(put("/api/employees/{id}", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
						.content(body("Ai Đó", "nobody@example.com", 1, "Developer")))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteRemovesEmployeeAndReturns204() throws Exception {
		String location = create("Trần Thị Bình", "binh@example.com", 1).andReturn().getResponse().getHeader("Location");

		mockMvc.perform(delete(location)).andExpect(status().isNoContent());
		mockMvc.perform(get(location)).andExpect(status().isNotFound());
	}

	@Test
	void deleteUnknownIdReturns404() throws Exception {
		mockMvc.perform(delete("/api/employees/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
	}

	@Test
	void employeeCodeOfDeletedEmployeeIsNeverReused() throws Exception {
		MockHttpServletResponse deleted = create("Lê Văn Cường", "cuong@example.com", 1).andReturn().getResponse();
		String deletedCode = JsonPath.read(deleted.getContentAsString(), "$.employeeCode");
		mockMvc.perform(delete(deleted.getHeader("Location"))).andExpect(status().isNoContent());

		String nextCode = JsonPath.read(create("Phạm Thị Dung", "dung@example.com", 1)
				.andReturn().getResponse().getContentAsString(), "$.employeeCode");

		assertThat(nextCode).isGreaterThan(deletedCode);
	}

	@Test
	void searchByPartOfNameIsCaseInsensitive() throws Exception {
		create("Nguyễn Văn An", "an@example.com", 1);
		create("Trần Thị Bình", "binh@example.com", 1);
		create("Nguyễn Thị Hoa", "hoa@example.com", 2);

		mockMvc.perform(get("/api/employees").param("name", "nGUYỄN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].fullName").value(containsInAnyOrder("Nguyễn Văn An", "Nguyễn Thị Hoa")));
	}

	@Test
	void filterByDepartment() throws Exception {
		create("Nguyễn Văn An", "an@example.com", 1);
		create("Trần Thị Bình", "binh@example.com", 2);
		create("Nguyễn Thị Hoa", "hoa@example.com", 2);

		mockMvc.perform(get("/api/employees").param("departmentId", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].fullName").value(containsInAnyOrder("Trần Thị Bình", "Nguyễn Thị Hoa")));
	}

	@Test
	void nameAndDepartmentFiltersCombine() throws Exception {
		create("Nguyễn Văn An", "an@example.com", 1);
		create("Trần Thị Bình", "binh@example.com", 2);
		create("Nguyễn Thị Hoa", "hoa@example.com", 2);

		mockMvc.perform(get("/api/employees").param("name", "nguyễn").param("departmentId", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].fullName").value(contains("Nguyễn Thị Hoa")));
	}

	@Test
	void listIsPaginatedTenPerPageSortedByEmployeeCode() throws Exception {
		for (int i = 1; i <= 12; i++) {
			create("Nhân Viên " + i, "nv%02d@example.com".formatted(i), 1);
		}

		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Total-Count", "12"))
				.andExpect(header().string("X-Total-Pages", "2"))
				.andExpect(jsonPath("$.length()").value(10))
				.andExpect(jsonPath("$[0].email").value("nv01@example.com"))
				.andExpect(jsonPath("$[9].email").value("nv10@example.com"));

		mockMvc.perform(get("/api/employees").param("page", "1"))
				.andExpect(jsonPath("$[*].email").value(contains("nv11@example.com", "nv12@example.com")));

		mockMvc.perform(get("/api/employees").param("page", "1").param("size", "5"))
				.andExpect(header().string("X-Total-Pages", "3"))
				.andExpect(jsonPath("$[*].email").value(contains(
						"nv06@example.com", "nv07@example.com", "nv08@example.com", "nv09@example.com", "nv10@example.com")));
	}

	private ResultActions create(String fullName, String email, long departmentId) throws Exception {
		return mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
				.content(body(fullName, email, departmentId, "Developer")));
	}

	private static String body(String fullName, String email, long departmentId, String position) {
		return """
				{
				  "fullName": "%s",
				  "email": "%s",
				  "phone": "0901234567",
				  "position": "%s",
				  "hireDate": "2024-03-15",
				  "departmentId": %d
				}
				""".formatted(fullName, email, position, departmentId);
	}

}
