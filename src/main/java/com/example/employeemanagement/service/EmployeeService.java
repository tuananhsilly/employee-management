package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.repository.InMemoryEmployeeRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

	private final InMemoryEmployeeRepository employeeRepository;

	private final UtilityService utilityService;

	private final ModelMapper modelMapper;

	public EmployeeService(InMemoryEmployeeRepository employeeRepository, UtilityService utilityService,
			ModelMapper modelMapper) {
		this.employeeRepository = employeeRepository;
		this.utilityService = utilityService;
		this.modelMapper = modelMapper;
	}

	public EmployeeResponse create(EmployeeRequest request) {
		Employee employee = modelMapper.map(request, Employee.class);
		employee.setEmployeeCode(utilityService.nextEmployeeCode());
		return toResponse(employeeRepository.save(employee));
	}

	public List<EmployeeResponse> findAll() {
		return employeeRepository.findAll().stream().map(this::toResponse).toList();
	}

	public Optional<EmployeeResponse> findById(Long id) {
		return employeeRepository.findById(id).map(this::toResponse);
	}

	private EmployeeResponse toResponse(Employee employee) {
		return modelMapper.map(employee, EmployeeResponse.class);
	}

}
