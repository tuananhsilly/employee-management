package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DepartmentControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void listReturnsSeededDepartments() throws Exception {
		mockMvc.perform(get("/api/departments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name").value(contains("IT", "HR", "Sales", "Finance", "Marketing")))
				.andExpect(jsonPath("$[0].id").isNumber());
	}

}
