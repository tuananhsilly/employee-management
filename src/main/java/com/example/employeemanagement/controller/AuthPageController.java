package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.RegisterRequest;
import com.example.employeemanagement.exception.DuplicateUsernameException;
import com.example.employeemanagement.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** Login and registration pages. Spring Security itself handles {@code POST /login} and {@code /logout}. */
@Controller
public class AuthPageController {

	private final UserAccountService userAccountService;

	public AuthPageController(UserAccountService userAccountService) {
		this.userAccountService = userAccountService;
	}

	@GetMapping("/login")
	public String login() {
		return "auth/login";
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("registration", new RegisterRequest());
		return "auth/register";
	}

	/** Always creates a User Account with the User Role; on success the visitor goes on to log in. */
	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registration") RegisterRequest registration,
			BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return "auth/register";
		}
		try {
			userAccountService.register(registration);
		}
		catch (DuplicateUsernameException ex) {
			bindingResult.rejectValue("username", "duplicate", ex.getMessage());
			return "auth/register";
		}
		return "redirect:/login?registered";
	}

}
