package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Self-registration. There is deliberately no Role field: every registered account is a User. */
public class RegisterRequest {

	/** BCrypt only takes the first 72 bytes of a password and refuses longer ones. */
	static final String PASSWORD_TOO_LONG = "Mật khẩu quá dài: tối đa 72 byte (chữ có dấu tính 2–3 byte)";

	// Each rule rejects a different input, so a bad value always gets exactly one message.
	@NotBlank(message = "Tên đăng nhập không được để trống")
	@Pattern(regexp = "\\s*|[A-Za-z0-9._-]{3,50}",
			message = "Tên đăng nhập gồm 3 đến 50 ký tự: chữ không dấu, số và . _ -")
	private String username;

	@NotNull(message = "Mật khẩu không được để trống")
	@Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
	@MaxUtf8Bytes(value = 72, message = PASSWORD_TOO_LONG)
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
