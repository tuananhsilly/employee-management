package com.example.employeemanagement.config;

import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.entity.Employee;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

	@Bean
	public ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();
		// STRICT: only map properties whose names match exactly. The default (STANDARD)
		// fuzzy-matches tokens, e.g. request "departmentId" onto entity "id".
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

		// The service resolves the Department itself; never let the mapper touch the association,
		// or it would write departmentId into the id of a managed Department.
		modelMapper.typeMap(EmployeeRequest.class, Employee.class)
				.addMappings(mapping -> mapping.skip(Employee::setDepartment));
		modelMapper.typeMap(Employee.class, EmployeeResponse.class)
				.addMappings(mapping -> mapping.map(employee -> employee.getDepartment().getId(),
						EmployeeResponse::setDepartmentId));
		return modelMapper;
	}

}
