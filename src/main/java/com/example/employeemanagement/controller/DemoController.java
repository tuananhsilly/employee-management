package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.EmployeePreviewRequest;
import com.example.employeemanagement.dto.EmployeePreviewResponse;
import com.example.employeemanagement.service.UtilityService;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lab 2 demo: shows UtilityService and the ModelMapper bean working together. */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

	private final UtilityService utilityService;

	private final ModelMapper modelMapper;

	public DemoController(UtilityService utilityService, ModelMapper modelMapper) {
		this.utilityService = utilityService;
		this.modelMapper = modelMapper;
	}

	@PostMapping("/employee-preview")
	public EmployeePreviewResponse preview(@RequestBody EmployeePreviewRequest request) {
		EmployeePreviewResponse response = modelMapper.map(request, EmployeePreviewResponse.class);
		response.setEmployeeCode(utilityService.nextEmployeeCode());
		response.setFullName(utilityService.formatFullName(request.getFullName()));
		return response;
	}

}
