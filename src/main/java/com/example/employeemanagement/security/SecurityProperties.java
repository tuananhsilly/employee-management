package com.example.employeemanagement.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * {@code app.security.*}: the JWT signing secret and lifetime, and the initial Admin
 * User Account. In prod every value comes from an environment variable.
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(Jwt jwt, InitialAdmin initialAdmin) {

	/** HS256 signing secret (at least 32 bytes) and how long an issued token stays valid. */
	public record Jwt(String secret, Duration expiry) {
	}

	/**
	 * Created at startup when no Admin exists. If either value is blank (e.g. ADMIN_USERNAME
	 * not set in prod) none is created and a warning is logged.
	 */
	public record InitialAdmin(String username, String password) {
	}

}
