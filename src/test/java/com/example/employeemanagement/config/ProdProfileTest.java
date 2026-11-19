package com.example.employeemanagement.config;

import com.example.employeemanagement.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 7: the prod profile takes its database connection from DB_URL, DB_USERNAME and
 * DB_PASSWORD (set here as stand-ins for real environment variables) and logs at INFO.
 * The actual connection still goes to the Testcontainers database.
 */
@SpringBootTest(properties = { "DB_URL=jdbc:postgresql://db.internal:5432/employees",
		"DB_USERNAME=employee_app", "DB_PASSWORD=from-the-environment" })
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Import(TestcontainersConfiguration.class)
class ProdProfileTest {

	@Autowired
	Environment environment;

	@Autowired
	MockMvc mockMvc;

	@Test
	void prodStartsAndServesRequests() throws Exception {
		mockMvc.perform(get("/api/departments")).andExpect(status().isOk());
	}

	@Test
	void prodTakesTheDatabaseConnectionFromTheEnvironment() {
		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://db.internal:5432/employees");
		assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("employee_app");
		assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("from-the-environment");
	}

	@Test
	void prodLogsAtInfo() {
		assertThat(environment.getProperty("logging.level.com.example.employeemanagement")).isEqualTo("INFO");
	}

}
