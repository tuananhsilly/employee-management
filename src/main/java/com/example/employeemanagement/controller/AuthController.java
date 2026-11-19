package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.LoginRequest;
import com.example.employeemanagement.dto.LoginResponse;
import com.example.employeemanagement.dto.RegisterRequest;
import com.example.employeemanagement.dto.UserAccountResponse;
import com.example.employeemanagement.security.JwtTokenService;
import com.example.employeemanagement.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public: registering a User Account and logging in for a JWT. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserAccountService userAccountService;

	private final AuthenticationManager authenticationManager;

	private final JwtTokenService jwtTokenService;

	public AuthController(UserAccountService userAccountService, AuthenticationManager authenticationManager,
			JwtTokenService jwtTokenService) {
		this.userAccountService = userAccountService;
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserAccountResponse register(@Valid @RequestBody RegisterRequest request) {
		return userAccountService.register(request);
	}

	/** Wrong username or password → 401 (see GlobalExceptionHandler). */
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword()));
		return jwtTokenService.issue(authentication);
	}

}
