package com.example.employeemanagement.security;

import com.example.employeemanagement.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** At startup, creates the Admin from {@code app.security.initial-admin} if no Admin exists yet. */
@Component
public class InitialAdminRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(InitialAdminRunner.class);

	private final UserAccountService userAccountService;

	private final SecurityProperties.InitialAdmin initialAdmin;

	public InitialAdminRunner(UserAccountService userAccountService, SecurityProperties properties) {
		this.userAccountService = userAccountService;
		this.initialAdmin = properties.initialAdmin();
	}

	@Override
	public void run(ApplicationArguments args) {
		if (initialAdmin == null || !StringUtils.hasText(initialAdmin.username())
				|| !StringUtils.hasText(initialAdmin.password())) {
			log.warn("app.security.initial-admin is not configured; no initial Admin created");
			return;
		}
		userAccountService.createAdminIfNoneExists(initialAdmin.username(), initialAdmin.password());
	}

}
