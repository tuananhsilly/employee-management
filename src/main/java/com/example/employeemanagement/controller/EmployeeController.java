package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

	private static final int DEFAULT_PAGE_SIZE = 10;

	private static final int MAX_PAGE_SIZE = 100;

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	/**
	 * Paginated search. The body stays a plain JSON array (the Lab 3 contract);
	 * pagination metadata travels in the X-Total-Count and X-Total-Pages headers.
	 */
	@GetMapping
	public ResponseEntity<List<EmployeeResponse>> search(@RequestParam(required = false) String name,
			@RequestParam(required = false) Long departmentId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
		Page<EmployeeResponse> result = employeeService.search(name, departmentId,
				Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
		return ResponseEntity.ok()
				.header("X-Total-Count", String.valueOf(result.getTotalElements()))
				.header("X-Total-Pages", String.valueOf(result.getTotalPages()))
				.body(result.getContent());
	}

	@GetMapping("/{id}")
	public ResponseEntity<EmployeeResponse> findById(@PathVariable Long id) {
		return ResponseEntity.of(employeeService.findById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<EmployeeResponse> update(@PathVariable Long id, @RequestBody EmployeeRequest request) {
		return ResponseEntity.of(employeeService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		return employeeService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}

	@PostMapping
	public ResponseEntity<EmployeeResponse> create(@RequestBody EmployeeRequest request) {
		EmployeeResponse created = employeeService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.getId())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

}
