package com.example.employeemanagement.security;

import com.example.employeemanagement.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Writes the API's 401 and 403 in the same {@link ApiError} shape as every other API
 * failure. Security rejects these requests in a filter, before any controller or
 * {@code @RestControllerAdvice} runs, so it needs its own writer.
 */
@Component
public class ApiErrorResponder {

	private final JsonMapper jsonMapper;

	public ApiErrorResponder(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	/** No token, or a token that is malformed, badly signed or expired. */
	public AuthenticationEntryPoint unauthorized() {
		return (request, response, ex) -> write(request, response, HttpStatus.UNAUTHORIZED,
				"Bạn cần đăng nhập để sử dụng API");
	}

	/** A valid token whose Role may not do this, e.g. a User changing an Employee. */
	public AccessDeniedHandler forbidden() {
		return (request, response, ex) -> write(request, response, HttpStatus.FORBIDDEN,
				"Bạn không có quyền thực hiện thao tác này");
	}

	private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
			throws IOException {
		ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
				request.getRequestURI(), Map.of());
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getOutputStream(), body);
	}

}
