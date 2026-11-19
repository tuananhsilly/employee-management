package com.example.employeemanagement.security;

import com.example.employeemanagement.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lab 9: registering a User Account and logging in over the API. Keeps only the initial Admin between tests. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcClient jdbcClient;

	@BeforeEach
	@AfterEach
	void deleteRegisteredAccounts() {
		jdbcClient.sql("DELETE FROM user_account WHERE role = 'USER'").update();
	}

	@Test
	void registerCreatesAUserRoleAccount() throws Exception {
		register("""
				{"username":"hr.lan","password":"matkhau123"}""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.username").value("hr.lan"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void registerIgnoresARequestedRole() throws Exception {
		register("""
				{"username":"hr.lan","password":"matkhau123","role":"ADMIN"}""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	void takenUsernameIs409() throws Exception {
		register("""
				{"username":"hr.lan","password":"matkhau123"}""").andExpect(status().isCreated());

		register("""
				{"username":"hr.lan","password":"khac12345"}""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.fieldErrors.username").value("Tên đăng nhập hr.lan đã được sử dụng"));
	}

	@Test
	void invalidRegistrationIs400WithFieldErrors() throws Exception {
		register("""
				{"username":"","password":"ngan"}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.username").value("Tên đăng nhập không được để trống"))
				.andExpect(jsonPath("$.fieldErrors.password").value("Mật khẩu phải có ít nhất 8 ký tự"));
	}

	@Test
	void loginReturnsABearerTokenThatUnlocksTheApi() throws Exception {
		register("""
				{"username":"hr.lan","password":"matkhau123"}""").andExpect(status().isCreated());

		String body = login("hr.lan", "matkhau123")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(body, "$.accessToken");

		mockMvc.perform(get("/api/employees").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void tokenNamesTheAccountAndCarriesOnlyItsRole() throws Exception {
		register("""
				{"username":"hr.lan","password":"matkhau123"}""").andExpect(status().isCreated());

		String body = login("hr.lan", "matkhau123").andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(body, "$.accessToken");
		String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

		assertThat(JsonPath.<String>read(payload, "$.sub")).isEqualTo("hr.lan");
		assertThat(JsonPath.<List<String>>read(payload, "$.roles")).containsExactly("USER");
		assertThat(JsonPath.<Integer>read(payload, "$.exp") - JsonPath.<Integer>read(payload, "$.iat")).isEqualTo(3600);
	}

	@Test
	void wrongPasswordOrUnknownUsernameIs401() throws Exception {
		register("""
				{"username":"hr.lan","password":"matkhau123"}""").andExpect(status().isCreated());

		login("hr.lan", "sai-mat-khau")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
		login("khong.ton.tai", "matkhau123")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
	}

	@Test
	void initialAdminFromConfigurationCanLogIn() throws Exception {
		// application-dev.yml: app.security.initial-admin = admin / admin12345
		login("admin", "admin12345").andExpect(status().isOk());
	}

	@Test
	void loginStillWorksWhileSendingAnExpiredOrBrokenToken() throws Exception {
		mockMvc.perform(post("/api/auth/login").header("Authorization", "Bearer not-a-valid-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"admin12345"}"""))
				.andExpect(status().isOk());
	}

	@Test
	void passwordOverBcryptsByteLimitIs400NotAServerError() throws Exception {
		// 30 characters but 90 bytes in UTF-8: BCrypt only takes 72 bytes.
		String longPassword = "ệ".repeat(30);

		register("""
				{"username":"hr.lan","password":"%s"}""".formatted(longPassword))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.password").value(
						"Mật khẩu quá dài: tối đa 72 byte (chữ có dấu tính 2–3 byte)"));
		login("admin", longPassword).andExpect(status().isBadRequest());
	}

	private ResultActions login(String username, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"%s","password":"%s"}""".formatted(username, password)));
	}

	private ResultActions register(String body) throws Exception {
		return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
