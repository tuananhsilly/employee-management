package com.example.employeemanagement.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

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
	public EmployeeResponse create(EmployeeRequest request) {
		if (employeeRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateEmailException(request.getEmail());
		}
		Employee employee = modelMapper.map(request, Employee.class);
		employee.setDepartment(findDepartment(request.getDepartmentId()));
		employee.setEmployeeCode(utilityService.nextEmployeeCode());
		return toResponse(employeeRepository.save(employee));
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
		return toResponse(employee);
	}

	/** Hard-deletes the Employee. */
	@Transactional
	public void delete(Long id) {
		employeeRepository.delete(findEmployee(id));
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

	private EmployeeResponse toResponse(Employee employee) {
		return modelMapper.map(employee, EmployeeResponse.class);
	}

}
