package com.example.employeemanagement.security;

import com.example.employeemanagement.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 9: what a User and an Admin may do over the API, using real tokens from
 * {@code POST /api/auth/login}; and which tokens are rejected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiRoleAccessTest {

	private static final String EMPLOYEE = """
			{"fullName":"Nguyễn Văn An","email":"an@example.com","position":"Developer",
			 "hireDate":"2024-03-01","departmentId":1}""";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcClient jdbcClient;

	/** The app's own encoder, used only to forge an already-expired token. */
	@Autowired
	JwtEncoder jwtEncoder;

	String userToken;

	String adminToken;

	@BeforeEach
	void logIn() throws Exception {
		cleanUp();
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"hr.lan","password":"matkhau123"}"""))
				.andExpect(status().isCreated());
		userToken = login("hr.lan", "matkhau123");
		adminToken = login("admin", "admin12345");
	}

	@AfterEach
	void cleanUp() {
		jdbcClient.sql("DELETE FROM employee").update();
		jdbcClient.sql("DELETE FROM user_account WHERE role = 'USER'").update();
	}

	@Test
	void userCanReadEverything() throws Exception {
		String location = createAsAdmin();

		for (String url : List.of("/api/employees", location, "/api/departments", "/api/reports/employee-count")) {
			mockMvc.perform(get(url).header("Authorization", "Bearer " + userToken)).andExpect(status().isOk());
		}
	}

	@Test
	void userCannotChangeEmployees() throws Exception {
		String location = createAsAdmin();

		expectForbidden(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE));
		expectForbidden(put(location).contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE));
		expectForbidden(delete(location));
	}

	@Test
	void adminCanCreateUpdateAndDelete() throws Exception {
		String location = createAsAdmin();

		mockMvc.perform(put(location).header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE))
				.andExpect(status().isOk());
		mockMvc.perform(delete(location).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
	}

	@Test
	void garbageTokenIs401() throws Exception {
		expectUnauthorized("not-a-jwt");
	}

	@Test
	void tokenSignedWithAnotherSecretIs401() throws Exception {
		JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
				"some-other-secret-that-is-long-enough-0123".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
		Instant now = Instant.now();

		expectUnauthorized(sign(otherEncoder, now, now.plusSeconds(3600)));
	}

	@Test
	void expiredTokenIs401() throws Exception {
		Instant twoHoursAgo = Instant.now().minusSeconds(7200);

		expectUnauthorized(sign(jwtEncoder, twoHoursAgo, twoHoursAgo.plusSeconds(3600)));
	}

	private void expectForbidden(MockHttpServletRequestBuilder request) throws Exception {
		mockMvc.perform(request.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
	}

	private void expectUnauthorized(String token) throws Exception {
		mockMvc.perform(get("/api/employees").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	/** An Admin token for {@code admin}, otherwise valid, with the given lifetime. */
	private static String sign(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("admin")
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("roles", List.of("ADMIN"))
				.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	private String createAsAdmin() throws Exception {
		return mockMvc.perform(post("/api/employees").header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
	}

	private String login(String username, String password) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"%s","password":"%s"}""".formatted(username, password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

}
