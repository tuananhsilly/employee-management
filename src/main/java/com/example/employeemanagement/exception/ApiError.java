package com.example.employeemanagement.exception;

import java.time.Instant;
import java.util.Map;

/**
 * The one JSON shape every API failure comes back in. {@code fieldErrors} maps a
 * request field to its message and is empty when the failure is not about a field.
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
		Map<String, String> fieldErrors) {
}
