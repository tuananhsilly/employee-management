package com.example.employeemanagement.exception;

/** The email is already used by another Employee → 409. */
public class DuplicateEmailException extends RuntimeException {

	public DuplicateEmailException(String email) {
		super("Email " + email + " đã được nhân viên khác sử dụng");
	}

}
