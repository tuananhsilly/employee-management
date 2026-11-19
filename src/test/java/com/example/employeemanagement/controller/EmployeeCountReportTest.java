package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import com.example.employeemanagement.config.CacheConfig;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lab 8: cached Employee count report. Each test starts and ends with no Employees and an empty cache. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@WithMockUser(roles = "ADMIN")
class EmployeeCountReportTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcClient jdbcClient;

	@Autowired
	CacheManager cacheManager;

	/** Other tests delete rows with plain SQL, which the cache cannot see, so start clean. */
	@BeforeEach
	@AfterEach
	void reset() {
		jdbcClient.sql("DELETE FROM employee").update();
		cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
	}

	@Test
	void countIsUpdatedRightAfterCreateAndDelete() throws Exception {
		expectCount(0);

		String an = create("Nguyễn Văn An", "an@example.com");
		expectCount(1);

		create("Trần Thị Bình", "binh@example.com");
		expectCount(2);

		mockMvc.perform(delete(an)).andExpect(status().isNoContent());
		expectCount(1);
	}

	@Test
	void countIsServedFromTheCacheBetweenChanges() throws Exception {
		create("Nguyễn Văn An", "an@example.com");
		create("Trần Thị Bình", "binh@example.com");
		expectCount(2);

		// A change that bypasses EmployeeService (and so the eviction) is not seen until
		// the next create/delete or the 1-minute expiry: proof the count is cached.
		jdbcClient.sql("DELETE FROM employee").update();
		expectCount(2);

		// The next create evicts the cache, so the fresh count shows the bypassed delete too.
		create("Lê Văn Cường", "cuong@example.com");
		expectCount(1);
	}

	/** Not observable over HTTP in reasonable time, so this one checks the configured cache. */
	@Test
	@SuppressWarnings("unchecked")
	void cachedCountExpiresOneMinuteAfterWrite() {
		Cache<Object, Object> cache = (Cache<Object, Object>) cacheManager.getCache(CacheConfig.EMPLOYEE_COUNT).getNativeCache();

		assertThat(cache.policy().expireAfterWrite())
				.hasValueSatisfying(expiry -> assertThat(expiry.getExpiresAfter()).isEqualTo(Duration.ofMinutes(1)));
	}

	private void expectCount(long expected) throws Exception {
		mockMvc.perform(get("/api/reports/employee-count"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalEmployees").value(expected));
	}

	/** Creates an Employee over the REST API; returns its Location. */
	private String create(String fullName, String email) throws Exception {
		return mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"%s","email":"%s","position":"Developer",
								 "hireDate":"2024-03-01","departmentId":1}""".formatted(fullName, email)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
	}

}
