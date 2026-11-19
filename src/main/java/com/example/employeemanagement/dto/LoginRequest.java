package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

	@NotBlank(message = "Tên đăng nhập không được để trống")
	private String username;

	@NotBlank(message = "Mật khẩu không được để trống")
	@MaxUtf8Bytes(value = 72, message = RegisterRequest.PASSWORD_TOO_LONG)
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
