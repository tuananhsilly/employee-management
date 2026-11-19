package com.example.employeemanagement.exception;

/** The resource addressed by the request URL does not exist → 404. */
public class ResourceNotFoundException extends RuntimeException {

	private ResourceNotFoundException(String message) {
		super(message);
	}

	public static ResourceNotFoundException employee(Long id) {
		return new ResourceNotFoundException("Không tìm thấy nhân viên với id " + id);
	}

}
