package com.example.employeemanagement.service;

import com.example.employeemanagement.config.CacheConfig;
import com.example.employeemanagement.dto.EmployeeCountReport;
import com.example.employeemanagement.repository.EmployeeRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

	private final EmployeeRepository employeeRepository;

	public ReportService(EmployeeRepository employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	/** Cached; {@link EmployeeService} evicts it whenever an Employee is created or deleted. */
	@Cacheable(CacheConfig.EMPLOYEE_COUNT)
	public EmployeeCountReport employeeCount() {
		return new EmployeeCountReport(employeeRepository.count());
	}

}
