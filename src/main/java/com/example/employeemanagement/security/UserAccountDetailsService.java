package com.example.employeemanagement.security;

import com.example.employeemanagement.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lets Spring Security check a username and password against the User Accounts. */
@Service
public class UserAccountDetailsService implements UserDetailsService {

	private final UserAccountRepository userAccountRepository;

	public UserAccountDetailsService(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		return userAccountRepository.findByUsername(username)
				.map(account -> User.withUsername(account.getUsername())
						.password(account.getPasswordHash())
						.roles(account.getRole().name())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException(username));
	}

}
