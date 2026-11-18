package com.example.employeemanagement.controller;

import com.example.employeemanagement.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DemoControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void previewAssignsEmployeeCodeZeroPaddedToFiveDigits() throws Exception {
		preview("Nguyen Van A")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.employeeCode").value(matchesPattern("EMP\\d{5}")));
	}

	@Test
	void previewNormalisesFullName() throws Exception {
		preview("  nGUYỄN   văn   an ")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"));
	}

	@Test
	void previewTreatsBlankFullNameAsEmpty() throws Exception {
		preview("   ")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value(""));
	}

	@Test
	void successiveEmployeeCodesAreSequential() throws Exception {
		int first = codeNumber(preview("Tran Thi B"));
		int second = codeNumber(preview("Le Van C"));

		assertThat(second).isEqualTo(first + 1);
	}

	private int codeNumber(ResultActions result) throws Exception {
		String code = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.employeeCode");
		return Integer.parseInt(code.substring("EMP".length()));
	}

	private ResultActions preview(String fullName) throws Exception {
		return mockMvc.perform(post("/api/demo/employee-preview")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fullName\":\"" + fullName + "\"}"));
	}

}
