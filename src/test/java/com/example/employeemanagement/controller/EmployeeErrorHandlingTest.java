package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lab 5: every failure comes back in one JSON error shape with Vietnamese messages. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EmployeeErrorHandlingTest {

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
	void invalidBodyReturns400WithFieldErrors() throws Exception {
		postEmployee("""
				{ "fullName": "  ", "email": "not-an-email", "phone": "0901234567", "position": "Developer" }
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
				.andExpect(jsonPath("$.path").value("/api/employees"))
				.andExpect(jsonPath("$.fieldErrors.fullName").value("Họ tên không được để trống"))
				.andExpect(jsonPath("$.fieldErrors.email").value("Email không đúng định dạng"))
				.andExpect(jsonPath("$.fieldErrors.hireDate").value("Ngày vào làm không được để trống"))
				.andExpect(jsonPath("$.fieldErrors.departmentId").value("Phòng ban không được để trống"));
	}

	@Test
	void invalidBodyOnUpdateReturns400() throws Exception {
		String location = postEmployee(validBody("an@example.com", "\"2024-03-15\"", 1))
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
						.content(validBody("", "null", 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.email").value("Email không được để trống"))
				.andExpect(jsonPath("$.fieldErrors.hireDate").value("Ngày vào làm không được để trống"));
	}

	@Test
	void malformedJsonReturns400() throws Exception {
		postEmployee("{ \"fullName\": \"Nguyễn Văn An\", ")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("Thiếu dữ liệu hoặc dữ liệu JSON không đúng cú pháp"))
				.andExpect(jsonPath("$.path").value("/api/employees"))
				.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void wrongValueTypeReturns400NamingTheField() throws Exception {
		postEmployee(validBody("an@example.com", "\"ngày mai\"", 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Trường 'hireDate' có giá trị sai kiểu dữ liệu"))
				.andExpect(jsonPath("$.fieldErrors.hireDate").value("Giá trị sai kiểu dữ liệu"));
	}

	@Test
	void nonNumericIdInPathReturns400() throws Exception {
		mockMvc.perform(get("/api/employees/{id}", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Tham số 'id' có giá trị sai kiểu dữ liệu"))
				.andExpect(jsonPath("$.path").value("/api/employees/abc"));
	}

	@Test
	void unknownEmployeeIdReturns404OnGetUpdateAndDelete() throws Exception {
		String path = "/api/employees/" + Long.MAX_VALUE;
		String message = "Không tìm thấy nhân viên với id " + Long.MAX_VALUE;

		mockMvc.perform(get(path))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value(message))
				.andExpect(jsonPath("$.path").value(path));
		mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
						.content(validBody("an@example.com", "\"2024-03-15\"", 1)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(message));
		mockMvc.perform(delete(path))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(message));
	}

	@Test
	void unknownDepartmentIdReturns400() throws Exception {
		postEmployee(validBody("an@example.com", "\"2024-03-15\"", 999))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Phòng ban với id 999 không tồn tại"))
				.andExpect(jsonPath("$.fieldErrors.departmentId").value("Phòng ban với id 999 không tồn tại"));
	}

	@Test
	void emailUsedByAnotherEmployeeReturns409OnCreate() throws Exception {
		postEmployee(validBody("an@example.com", "\"2024-03-15\"", 1)).andExpect(status().isCreated());

		postEmployee(validBody("an@example.com", "\"2024-03-15\"", 2))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("Email an@example.com đã được nhân viên khác sử dụng"))
				.andExpect(jsonPath("$.fieldErrors.email").value("Email an@example.com đã được nhân viên khác sử dụng"));
	}

	@Test
	void emailUsedByAnotherEmployeeReturns409OnUpdate() throws Exception {
		postEmployee(validBody("an@example.com", "\"2024-03-15\"", 1)).andExpect(status().isCreated());
		String binhLocation = postEmployee(validBody("binh@example.com", "\"2024-03-15\"", 1))
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(put(binhLocation).contentType(MediaType.APPLICATION_JSON)
						.content(validBody("an@example.com", "\"2024-03-15\"", 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Email an@example.com đã được nhân viên khác sử dụng"));
	}

	@Test
	void updateKeepingOwnEmailIsAllowed() throws Exception {
		String anLocation = postEmployee(validBody("an@example.com", "\"2024-03-15\"", 1))
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(put(anLocation).contentType(MediaType.APPLICATION_JSON)
						.content(validBody("an@example.com", "\"2025-01-02\"", 2)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hireDate").value("2025-01-02"));
	}

	@Test
	void unsupportedMethodUsesTheSameShape() throws Exception {
		mockMvc.perform(delete("/api/employees"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.error").value("Method Not Allowed"))
				.andExpect(jsonPath("$.message").value("Phương thức DELETE không được hỗ trợ"))
				.andExpect(jsonPath("$.path").value("/api/employees"))
				.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void unknownRouteUsesTheSameShape() throws Exception {
		mockMvc.perform(get("/api/khong-ton-tai"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Không tìm thấy đường dẫn /api/khong-ton-tai"));
	}

	private static String validBody(String email, String hireDateJson, long departmentId) {
		return """
				{
				  "fullName": "Nguyễn Văn An",
				  "email": "%s",
				  "phone": "0901234567",
				  "position": "Developer",
				  "hireDate": %s,
				  "departmentId": %d
				}
				""".formatted(email, hireDateJson, departmentId);
	}

	private ResultActions postEmployee(String json) throws Exception {
		return mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json));
	}

}
