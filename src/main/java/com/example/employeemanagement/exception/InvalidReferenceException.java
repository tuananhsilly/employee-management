package com.example.employeemanagement.exception;

/**
 * A request-body field points at something that does not exist, e.g. an unknown
 * Department id → 400 with that field in {@code fieldErrors}.
 */
public class InvalidReferenceException extends RuntimeException {

	private final String field;

	private InvalidReferenceException(String field, String message) {
		super(message);
		this.field = field;
	}

	public static InvalidReferenceException department(Long id) {
		return new InvalidReferenceException("departmentId", "Phòng ban với id " + id + " không tồn tại");
	}

	public String getField() {
		return field;
	}

}
