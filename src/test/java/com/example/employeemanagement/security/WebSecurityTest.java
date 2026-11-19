package com.example.employeemanagement.security;

import com.example.employeemanagement.TestcontainersConfiguration;
import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.service.EmployeeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Lab 9: login, logout and registration pages, and what each Role sees and may do on the pages. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WebSecurityTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcClient jdbcClient;

	@Autowired
	EmployeeService employeeService;

	@BeforeEach
	@AfterEach
	void cleanUp() {
		jdbcClient.sql("DELETE FROM employee").update();
		jdbcClient.sql("DELETE FROM user_account WHERE role = 'USER'").update();
	}

	@Test
	void loginPageIsPublic() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/login"))
				.andExpect(content().string(containsString("Đăng nhập")));
	}

	@Test
	void formLoginStartsASessionAndLogoutEndsIt() throws Exception {
		mockMvc.perform(formLogin().user("admin").password("admin12345"))
				.andExpect(redirectedUrl("/employees/list"))
				.andExpect(authenticated().withUsername("admin").withRoles("ADMIN"));

		mockMvc.perform(logout())
				.andExpect(redirectedUrl("/login?logout"))
				.andExpect(unauthenticated());
	}

	@Test
	void wrongPasswordReturnsToLoginWithAnError() throws Exception {
		mockMvc.perform(formLogin().user("admin").password("sai-mat-khau"))
				.andExpect(redirectedUrl("/login?error"))
				.andExpect(unauthenticated());

		mockMvc.perform(get("/login").param("error", ""))
				.andExpect(content().string(containsString("Tên đăng nhập hoặc mật khẩu không đúng")));
	}

	@Test
	void registeringOnThePageCreatesAUserRoleAccountThatCanLogIn() throws Exception {
		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/register"));

		mockMvc.perform(post("/register").with(csrf())
						.param("username", "hr.lan")
						.param("password", "matkhau123"))
				.andExpect(redirectedUrl("/login?registered"));

		mockMvc.perform(formLogin().user("hr.lan").password("matkhau123"))
				.andExpect(authenticated().withUsername("hr.lan").withRoles("USER"));
	}

	@Test
	void invalidOrTakenUsernameIsShownOnTheRegisterForm() throws Exception {
		mockMvc.perform(post("/register").with(csrf()).param("username", "").param("password", "ngan"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/register"))
				.andExpect(model().attributeHasFieldErrors("registration", "username", "password"))
				.andExpect(content().string(containsString("Mật khẩu phải có ít nhất 8 ký tự")));

		mockMvc.perform(post("/register").with(csrf()).param("username", "admin").param("password", "matkhau123"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("registration", "username"))
				.andExpect(content().string(containsString("Tên đăng nhập admin đã được sử dụng")));
	}

	@Test
	@WithMockUser(username = "hr.lan", roles = "USER")
	void userSeesTheListWithoutChangeControls() throws Exception {
		EmployeeResponse an = createAn();

		mockMvc.perform(get("/employees/list"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nguyễn Văn An")))
				.andExpect(content().string(containsString("hr.lan")))
				.andExpect(content().string(not(containsString("href=\"/employees/add\""))))
				.andExpect(content().string(not(containsString("/employees/" + an.getId() + "/edit"))))
				.andExpect(content().string(not(containsString("/employees/" + an.getId() + "/delete"))));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminSeesChangeControls() throws Exception {
		EmployeeResponse an = createAn();

		mockMvc.perform(get("/employees/list"))
				.andExpect(content().string(containsString("href=\"/employees/add\"")))
				.andExpect(content().string(containsString("/employees/" + an.getId() + "/edit")))
				.andExpect(content().string(containsString("/employees/" + an.getId() + "/delete")));
	}

	@Test
	@WithMockUser(username = "hr.lan", roles = "USER")
	void userIsForbiddenFromChangePages() throws Exception {
		EmployeeResponse an = createAn();

		mockMvc.perform(get("/employees/add")).andExpect(status().isForbidden());
		mockMvc.perform(get("/employees/{id}/edit", an.getId())).andExpect(status().isForbidden());
		mockMvc.perform(post("/employees/add").with(csrf()).param("fullName", "Trần Thị Bình"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/employees/{id}/edit", an.getId()).with(csrf()).param("fullName", "Đổi Tên"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/employees/{id}/delete", an.getId()).with(csrf())).andExpect(status().isForbidden());
		mockMvc.perform(get("/employees/search")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminCanOpenTheChangePages() throws Exception {
		EmployeeResponse an = createAn();

		mockMvc.perform(get("/employees/add")).andExpect(status().isOk());
		mockMvc.perform(get("/employees/{id}/edit", an.getId())).andExpect(status().isOk());
		mockMvc.perform(post("/employees/{id}/delete", an.getId()).with(csrf()))
				.andExpect(redirectedUrl("/employees/list"));
	}

	@Test
	void anonymousVisitorIsSentToLoginFromEveryPage() throws Exception {
		for (String page : new String[] { "/employees/list", "/employees/search", "/employees/add" }) {
			mockMvc.perform(get(page)).andExpect(redirectedUrl("/login"));
		}
	}

	private EmployeeResponse createAn() {
		EmployeeRequest request = new EmployeeRequest();
		request.setFullName("Nguyễn Văn An");
		request.setEmail("an@example.com");
		request.setHireDate(LocalDate.of(2024, 3, 1));
		request.setDepartmentId(1L);
		return employeeService.create(request);
	}

}
