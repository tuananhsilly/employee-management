package com.example.employeemanagement.exception;

/** The username is already taken by another User Account → 409. */
public class DuplicateUsernameException extends RuntimeException {

	public DuplicateUsernameException(String username) {
		super("Tên đăng nhập " + username + " đã được sử dụng");
	}

}
