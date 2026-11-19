package com.example.employeemanagement.security;

import com.example.employeemanagement.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lab 9: what a visitor who is not logged in can and cannot reach. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AnonymousAccessTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void publicEndpointsAreOpen() throws Exception {
		mockMvc.perform(get("/hello")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/css/app.css")).andExpect(status().isOk());
	}

	@Test
	void apiWithoutTokenIs401InTheErrorShape() throws Exception {
		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Bạn cần đăng nhập để sử dụng API"))
				.andExpect(jsonPath("$.path").value("/api/employees"));

		mockMvc.perform(get("/api/departments")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/reports/employee-count")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void pagesRedirectToLogin() throws Exception {
		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void actuatorBeyondHealthNeedsLogin() throws Exception {
		mockMvc.perform(get("/actuator/metrics")).andExpect(status().isFound());
		mockMvc.perform(get("/actuator/info")).andExpect(status().isFound());
	}

}
