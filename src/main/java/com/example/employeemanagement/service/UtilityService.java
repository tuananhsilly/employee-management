package com.example.employeemanagement.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class UtilityService {

	private static final String EMPLOYEE_CODE_FORMAT = "EMP%05d";

	private final JdbcClient jdbcClient;

	public UtilityService(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	/** Next Employee Code from the database sequence, so codes are never reused. */
	public String nextEmployeeCode() {
		long next = jdbcClient.sql("SELECT nextval('employee_code_seq')")
				.query(Long.class)
				.single();
		return EMPLOYEE_CODE_FORMAT.formatted(next);
	}

	/**
	 * Trims, collapses inner whitespace and capitalises each word: "  nGUYỄN   văn an " → "Nguyễn Văn An".
	 * A null or blank name becomes "" (the Employee API rejects blank names via EmployeeRequest validation).
	 */
	public String formatFullName(String fullName) {
		if (fullName == null || fullName.isBlank()) {
			return "";
		}
		return Arrays.stream(fullName.strip().split("\\s+"))
				.map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT))
				.collect(Collectors.joining(" "));
	}

}
