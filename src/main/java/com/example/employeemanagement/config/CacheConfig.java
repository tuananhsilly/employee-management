package com.example.employeemanagement.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Caffeine caches, configured in application.yml (spring.cache.*).
 *
 * <p>The caching advice runs outside the transaction advice (highest precedence), so a
 * {@code @CacheEvict} on a transactional method evicts only after the commit. Evicting
 * before the commit would let a concurrent read cache the old count again.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig {

	/** Total number of Employees; 1 minute expire-after-write, evicted on create and delete. */
	public static final String EMPLOYEE_COUNT = "employee-count";

}
