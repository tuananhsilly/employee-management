package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.entity.Department;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.repository.EmployeeSpecifications;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

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
		Employee employee = modelMapper.map(request, Employee.class);
		employee.setDepartment(findDepartment(request.getDepartmentId()));
		employee.setEmployeeCode(utilityService.nextEmployeeCode());
		return toResponse(employeeRepository.save(employee));
	}

	/** Updates every field except the Employee Code; empty if no Employee has this id. */
	@Transactional
	public Optional<EmployeeResponse> update(Long id, EmployeeRequest request) {
		return employeeRepository.findById(id).map(employee -> {
			modelMapper.map(request, employee);
			employee.setDepartment(findDepartment(request.getDepartmentId()));
			return toResponse(employee);
		});
	}

	/** Hard-deletes the Employee; false if no Employee has this id. */
	@Transactional
	public boolean delete(Long id) {
		return employeeRepository.findById(id).map(employee -> {
			employeeRepository.delete(employee);
			return true;
		}).orElse(false);
	}

	/** One page of Employees matching the optional filters, sorted by Employee Code. */
	public Page<EmployeeResponse> search(String name, Long departmentId, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by("employeeCode"));
		return employeeRepository.findAll(EmployeeSpecifications.matching(name, departmentId), pageRequest)
				.map(this::toResponse);
	}

	public Optional<EmployeeResponse> findById(Long id) {
		return employeeRepository.findById(id).map(this::toResponse);
	}

	// Lab 5 replaces this with proper validation and the global error shape.
	private Department findDepartment(Long departmentId) {
		if (departmentId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departmentId is required");
		}
		return departmentRepository.findById(departmentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Department " + departmentId + " does not exist"));
	}

	private EmployeeResponse toResponse(Employee employee) {
		return modelMapper.map(employee, EmployeeResponse.class);
	}

}
