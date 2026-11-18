package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EmployeeControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void createReturns201WithLocationAndEmployeeCode() throws Exception {
		create("an.nguyen@example.com")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/api/employees/\\d+")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.employeeCode").value(matchesPattern("EMP\\d{5}")))
				.andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"))
				.andExpect(jsonPath("$.email").value("an.nguyen@example.com"))
				.andExpect(jsonPath("$.phone").value("0901234567"))
				.andExpect(jsonPath("$.position").value("Developer"))
				.andExpect(jsonPath("$.hireDate").value("2024-03-15"))
				.andExpect(jsonPath("$.departmentId").value(1));
	}

	@Test
	void getReturnsEmployeeCreatedEarlier() throws Exception {
		MockHttpServletResponse created = create("binh.tran@example.com").andReturn().getResponse();
		String createdCode = JsonPath.read(created.getContentAsString(), "$.employeeCode");

		mockMvc.perform(get(created.getHeader("Location")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.employeeCode").value(createdCode))
				.andExpect(jsonPath("$.email").value("binh.tran@example.com"))
				.andExpect(jsonPath("$.hireDate").value("2024-03-15"));
	}

	@Test
	void getUnknownIdReturns404() throws Exception {
		mockMvc.perform(get("/api/employees/{id}", Long.MAX_VALUE))
				.andExpect(status().isNotFound());
	}

	@Test
	void listIncludesCreatedEmployee() throws Exception {
		create("cuong.le@example.com");

		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[?(@.email == 'cuong.le@example.com')].employeeCode")
						.value(contains(matchesPattern("EMP\\d{5}"))));
	}

	@Test
	void employeesInSameDepartmentGetDistinctIds() throws Exception {
		Integer firstId = JsonPath.read(create("dung.pham@example.com", 3).andReturn().getResponse().getContentAsString(), "$.id");
		Integer secondId = JsonPath.read(create("em.vo@example.com", 3).andReturn().getResponse().getContentAsString(), "$.id");

		assertThat(secondId).isNotEqualTo(firstId);
		mockMvc.perform(get("/api/employees/{id}", firstId))
				.andExpect(jsonPath("$.email").value("dung.pham@example.com"))
				.andExpect(jsonPath("$.departmentId").value(3));
	}

	private ResultActions create(String email) throws Exception {
		return create(email, 1);
	}

	private ResultActions create(String email, long departmentId) throws Exception {
		return mockMvc.perform(post("/api/employees")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Nguyễn Văn An",
						  "email": "%s",
						  "phone": "0901234567",
						  "position": "Developer",
						  "hireDate": "2024-03-15",
						  "departmentId": %d
						}
						""".formatted(email, departmentId)));
	}

}
