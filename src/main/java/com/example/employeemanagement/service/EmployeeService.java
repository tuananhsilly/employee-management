package com.example.employeemanagement.service;

import com.example.employeemanagement.config.CacheConfig;
import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.exception.DuplicateEmailException;
import com.example.employeemanagement.exception.InvalidReferenceException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.repository.EmployeeSpecifications;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

	private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

	private final EmployeeRepository employeeRepository;

	private final DepartmentRepository departmentRepository;

	private final UtilityService utilityService;

	private final ModelMapper modelMapper;

	public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
			UtilityService utilityService, ModelMapper modelMapper) {
		this.employeeRepository = employeeRepository;
		this.departmentRepository = departmentRepository;
		this.utilityService = utilityService;
		this.modelMapper = modelMapper;
	}

	@Transactional
	@CacheEvict(cacheNames = CacheConfig.EMPLOYEE_COUNT, allEntries = true)
	public EmployeeResponse create(EmployeeRequest request) {
		if (employeeRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateEmailException(request.getEmail());
		}
		Employee employee = modelMapper.map(request, Employee.class);
		employee.setDepartment(findDepartment(request.getDepartmentId()));
		employee.setEmployeeCode(utilityService.nextEmployeeCode());
		Employee saved = employeeRepository.save(employee);
		logAfterCommit("Created", saved);
		return toResponse(saved);
	}

	/** Updates every field except the Employee Code. */
	@Transactional
	public EmployeeResponse update(Long id, EmployeeRequest request) {
		Employee employee = findEmployee(id);
		if (employeeRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
			throw new DuplicateEmailException(request.getEmail());
		}
		modelMapper.map(request, employee);
		employee.setDepartment(findDepartment(request.getDepartmentId()));
		logAfterCommit("Updated", employee);
		return toResponse(employee);
	}

	/** Hard-deletes the Employee. */
	@Transactional
	@CacheEvict(cacheNames = CacheConfig.EMPLOYEE_COUNT, allEntries = true)
	public void delete(Long id) {
		Employee employee = findEmployee(id);
		employeeRepository.delete(employee);
		logAfterCommit("Deleted", employee);
	}

	/** One page of Employees matching the optional filters, sorted by Employee Code. */
	public Page<EmployeeResponse> search(String name, Long departmentId, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by("employeeCode"));
		return employeeRepository.findAll(EmployeeSpecifications.matching(name, departmentId), pageRequest)
				.map(this::toResponse);
	}

	public EmployeeResponse findById(Long id) {
		return toResponse(findEmployee(id));
	}

	private Employee findEmployee(Long id) {
		return employeeRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.employee(id));
	}

	/** The request is already validated, so {@code departmentId} is not null. */
	private Department findDepartment(Long departmentId) {
		return departmentRepository.findById(departmentId)
				.orElseThrow(() -> InvalidReferenceException.department(departmentId));
	}

	/**
	 * Logs a change to an Employee once its transaction has committed, so a rolled-back
	 * change never shows up as done. Code and ids only: name and email are personal data
	 * and stay out of the log.
	 */
	private static void logAfterCommit(String change, Employee employee) {
		String code = employee.getEmployeeCode();
		Long id = employee.getId();
		Long departmentId = employee.getDepartment().getId();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				log.info("{} Employee {} (id={}, departmentId={})", change, code, id, departmentId);
			}
		});
	}

	private EmployeeResponse toResponse(Employee employee) {
		return modelMapper.map(employee, EmployeeResponse.class);
	}

}
