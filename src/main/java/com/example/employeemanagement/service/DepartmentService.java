package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.DepartmentResponse;
import com.example.employeemanagement.repository.DepartmentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

	private final DepartmentRepository departmentRepository;

	private final ModelMapper modelMapper;

	public DepartmentService(DepartmentRepository departmentRepository, ModelMapper modelMapper) {
		this.departmentRepository = departmentRepository;
		this.modelMapper = modelMapper;
	}

	public List<DepartmentResponse> findAll() {
		return departmentRepository.findAll(Sort.by("id")).stream()
				.map(department -> modelMapper.map(department, DepartmentResponse.class))
				.toList();
	}

}
