package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lab 8: Actuator exposes health, info and metrics, and nothing else. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActuatorTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void healthIsUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void infoDescribesTheApp() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.app.name").value("employee-management"));
	}

	@Test
	void metricsListsJvmAndHttpMetrics() throws Exception {
		mockMvc.perform(get("/actuator/metrics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.names", hasItem("jvm.memory.used")));
	}

	@Test
	void otherEndpointsAreNotExposed() throws Exception {
		mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
		mockMvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
	}

}
