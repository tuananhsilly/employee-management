package com.example.employeemanagement.config;

import com.example.employeemanagement.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lab 7: with no profile chosen the app runs as dev. Same annotations as the other
 * HTTP-seam tests so it reuses their cached context instead of starting another.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DevProfileTest {

	@Autowired
	Environment environment;

	@Test
	void devIsTheDefaultProfile() {
		assertThat(environment.getActiveProfiles()).isEmpty();
		assertThat(environment.getDefaultProfiles()).containsExactly("dev");
	}

	@Test
	void devPointsAtLocalPostgresAndLogsAtDebug() {
		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:postgresql://localhost:5432/employee_management");
		// Read the configured level, not the live logger: Logback levels are JVM-wide, so a
		// prod context started earlier in the same test run would have lowered them.
		assertThat(environment.getProperty("logging.level.com.example.employeemanagement")).isEqualTo("DEBUG");
	}

}
