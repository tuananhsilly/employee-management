package com.example.employeemanagement.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Turns every failure into an {@link ApiError}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			// A field can break several constraints at once; report the first one.
			fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return build(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", request, fieldErrors);
	}

	/** Body is not JSON at all, or a value cannot be read as its field's type. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
		if (ex.getCause() instanceof MismatchedInputException mismatch) {
			String field = fieldPath(mismatch);
			if (!field.isEmpty()) {
				return build(HttpStatus.BAD_REQUEST, "Trường '" + field + "' có giá trị sai kiểu dữ liệu", request,
						Map.of(field, "Giá trị sai kiểu dữ liệu"));
			}
		}
		return build(HttpStatus.BAD_REQUEST, "Thiếu dữ liệu hoặc dữ liệu JSON không đúng cú pháp", request);
	}

	/** A path variable or query parameter cannot be converted, e.g. a non-numeric id. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Tham số '" + ex.getName() + "' có giá trị sai kiểu dữ liệu", request);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidReferenceException.class)
	ResponseEntity<ApiError> handleInvalidReference(InvalidReferenceException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of(ex.getField(), ex.getMessage()));
	}

	@ExceptionHandler(DuplicateEmailException.class)
	ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of("email", ex.getMessage()));
	}

	/**
	 * Login with a wrong username or password; never says which of the two was wrong. Other
	 * authentication failures (e.g. the database is down) stay 500s.
	 */
	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<ApiError> handleBadLogin(BadCredentialsException ex, HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng", request);
	}

	@ExceptionHandler(DuplicateUsernameException.class)
	ResponseEntity<ApiError> handleDuplicateUsername(DuplicateUsernameException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of("username", ex.getMessage()));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		return build(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức " + ex.getMethod() + " không được hỗ trợ", request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> handleNoRoute(NoResourceFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, "Không tìm thấy đường dẫn " + request.getRequestURI(), request);
	}

	/**
	 * Everything else. Spring MVC exceptions (415, missing parameter, ...) keep their own
	 * status; anything unexpected is a 500 whose details stay in the log, not the response.
	 */
	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest request) {
		HttpStatus status = ex instanceof ErrorResponse errorResponse
				? HttpStatus.valueOf(errorResponse.getStatusCode().value())
				: HttpStatus.INTERNAL_SERVER_ERROR;
		if (status.is5xxServerError()) {
			log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
			return build(status, "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau", request);
		}
		return build(status, "Yêu cầu không hợp lệ", request);
	}

	/**
	 * A unique constraint fired after the service's own checks passed, e.g. two requests
	 * racing to use the same email.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
		log.warn("Data integrity violation on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return build(HttpStatus.CONFLICT, "Dữ liệu trùng với bản ghi đã có", request);
	}

	/** Dotted JSON property path of the value Jackson could not read, e.g. "hireDate". */
	private static String fieldPath(JacksonException ex) {
		return ex.getPath().stream()
				.map(JacksonException.Reference::getPropertyName)
				.filter(Objects::nonNull)
				.collect(Collectors.joining("."));
	}

	private static ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
		return build(status, message, request, Map.of());
	}

	private static ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
			Map<String, String> fieldErrors) {
		ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
				request.getRequestURI(), fieldErrors);
		return ResponseEntity.status(status).body(body);
	}

}
