package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lab 7: every change to an Employee leaves a log line naming its Employee Code. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class EmployeeChangeLogTest {

	private static final Pattern LOG_LINE = Pattern.compile(" (TRACE|DEBUG|INFO|WARN|ERROR) ");

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
	void createLogsTheEmployeeCode(CapturedOutput output) throws Exception {
		String code = createAn().code();

		assertThat(output).containsPattern("INFO .*Created Employee " + code + " ");
	}

	@Test
	void updateLogsTheEmployeeCode(CapturedOutput output) throws Exception {
		CreatedEmployee an = createAn();

		mockMvc.perform(put(an.location()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Nguyễn Văn An","email":"an.nguyen@example.com","position":"Team Lead",
								 "hireDate":"2024-03-01","departmentId":2}"""))
				.andExpect(status().isOk());

		assertThat(output).containsPattern("INFO .*Updated Employee " + an.code() + " ");
	}

	@Test
	void deleteLogsTheEmployeeCode(CapturedOutput output) throws Exception {
		CreatedEmployee an = createAn();

		mockMvc.perform(delete(an.location())).andExpect(status().isNoContent());

		assertThat(output).containsPattern("INFO .*Deleted Employee " + an.code() + " ");
	}

	@Test
	void changeLogKeepsPersonalDataOut(CapturedOutput output) throws Exception {
		createAn();

		// Only log lines count: MockMvc's own request/response dump is test tooling.
		assertThat(output.getAll().lines().filter(line -> LOG_LINE.matcher(line).find()))
				.isNotEmpty()
				.noneMatch(line -> line.contains("an@example.com") || line.contains("Nguyễn Văn An"));
	}

	record CreatedEmployee(String code, String location) {
	}

	/** Creates "Nguyễn Văn An" over the REST API. */
	private CreatedEmployee createAn() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(post("/api/employees")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fullName":"Nguyễn Văn An","email":"an@example.com","position":"Developer",
								 "hireDate":"2024-03-01","departmentId":1}"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse();
		return new CreatedEmployee(JsonPath.read(response.getContentAsString(), "$.employeeCode"),
				response.getHeader("Location"));
	}

}
