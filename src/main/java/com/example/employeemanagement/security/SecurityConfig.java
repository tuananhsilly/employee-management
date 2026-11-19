package com.example.employeemanagement.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Two filter chains: {@code /api/**} is stateless and authenticated by a JWT in
 * {@code Authorization: Bearer}; everything else (the pages) uses form login with a session.
 * Users may read; only Admins may create, update or delete Employees.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

	static final String ROLES_CLAIM = "roles";

	@Bean
	@Order(1)
	SecurityFilterChain apiSecurity(HttpSecurity http, ApiErrorResponder errors) throws Exception {
		http.securityMatcher("/api/**")
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers("/api/employees/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// No cookies on this chain, so there is no session to forge requests with.
				.csrf(csrf -> csrf.disable())
				.oauth2ResourceServer(oauth2 -> oauth2
						.bearerTokenResolver(bearerTokenResolver())
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(errors.unauthorized())
						.accessDeniedHandler(errors.forbidden()))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(errors.unauthorized())
						.accessDeniedHandler(errors.forbidden()));
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
						.requestMatchers("/hello", "/login", "/register", "/error").permitAll()
						.requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/actuator/**").hasRole("ADMIN")
						.requestMatchers("/employees/add", "/employees/*/edit", "/employees/*/delete").hasRole("ADMIN")
						.anyRequest().authenticated())
				.formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/employees/list"))
				.logout(logout -> logout.logoutSuccessUrl("/login?logout"));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** Checks username + password against the User Accounts: for API login and form login alike. */
	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	JwtEncoder jwtEncoder(SecurityProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(properties)));
	}

	@Bean
	JwtDecoder jwtDecoder(SecurityProperties properties) {
		return NimbusJwtDecoder.withSecretKey(signingKey(properties)).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/**
	 * Reads {@code Authorization: Bearer}, except on {@code /api/auth/**}: a client still
	 * sending its expired token must be able to log in again rather than get a 401.
	 */
	private static BearerTokenResolver bearerTokenResolver() {
		DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
		return request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/auth/")
				? null
				: resolver.resolve(request);
	}

	/** Roles travel in the token's {@code roles} claim, e.g. {@code ["ADMIN"]} → ROLE_ADMIN. */
	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(ROLES_CLAIM);
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	private static SecretKey signingKey(SecurityProperties properties) {
		byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < 32) {
			throw new IllegalStateException("app.security.jwt.secret must be at least 32 bytes for HS256");
		}
		return new SecretKeySpec(secret, "HmacSHA256");
	}

}
