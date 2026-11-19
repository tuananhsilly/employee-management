package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.RegisterRequest;
import com.example.employeemanagement.dto.UserAccountResponse;
import com.example.employeemanagement.entity.Role;
import com.example.employeemanagement.entity.UserAccount;
import com.example.employeemanagement.exception.DuplicateUsernameException;
import com.example.employeemanagement.repository.UserAccountRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserAccountService {

	private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

	private final UserAccountRepository userAccountRepository;

	private final PasswordEncoder passwordEncoder;

	private final ModelMapper modelMapper;

	public UserAccountService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
			ModelMapper modelMapper) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.modelMapper = modelMapper;
	}

	/** Self-registration always gives the User Role, so nobody can make themselves Admin. */
	@Transactional
	public UserAccountResponse register(RegisterRequest request) {
		UserAccount account = create(request.getUsername(), request.getPassword(), Role.USER);
		log.info("Registered User Account {} (id={})", account.getUsername(), account.getId());
		return toResponse(account);
	}

	/**
	 * Creates the initial Admin unless an Admin already exists. If the username is already
	 * taken by a User Role account, logs a warning instead of failing startup.
	 */
	@Transactional
	public void createAdminIfNoneExists(String username, String password) {
		if (userAccountRepository.existsByRole(Role.ADMIN)) {
			return;
		}
		if (userAccountRepository.existsByUsername(username)) {
			log.warn("Initial Admin not created: username {} is already used by a User Role account", username);
			return;
		}
		create(username, password, Role.ADMIN);
		log.info("Created initial Admin User Account {}", username);
	}

	private UserAccount create(String username, String password, Role role) {
		if (userAccountRepository.existsByUsername(username)) {
			throw new DuplicateUsernameException(username);
		}
		return userAccountRepository.save(new UserAccount(username, passwordEncoder.encode(password), role));
	}

	private UserAccountResponse toResponse(UserAccount account) {
		return modelMapper.map(account, UserAccountResponse.class);
	}

}
