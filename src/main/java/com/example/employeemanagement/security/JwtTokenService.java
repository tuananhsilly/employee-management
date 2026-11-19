package com.example.employeemanagement.security;

import com.example.employeemanagement.dto.LoginResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Issues the HS256 access token returned by {@code POST /api/auth/login}. */
@Service
public class JwtTokenService {

	private static final String ROLE_PREFIX = "ROLE_";

	private final JwtEncoder jwtEncoder;

	private final Duration expiry;

	public JwtTokenService(JwtEncoder jwtEncoder, SecurityProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.expiry = properties.jwt().expiry();
	}

	/** A token for an already authenticated User Account: subject = username, roles = its Role. */
	public LoginResponse issue(Authentication authentication) {
		Instant now = Instant.now();
		// Only Roles: Spring Security also adds non-role authorities such as FACTOR_PASSWORD.
		List<String> roles = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith(ROLE_PREFIX))
				.map(authority -> authority.substring(ROLE_PREFIX.length()))
				.toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(authentication.getName())
				.issuedAt(now)
				.expiresAt(now.plus(expiry))
				.claim(SecurityConfig.ROLES_CLAIM, roles)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new LoginResponse(token, "Bearer", expiry.toSeconds());
	}

}
