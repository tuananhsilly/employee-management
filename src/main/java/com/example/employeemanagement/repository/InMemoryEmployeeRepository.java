package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.Employee;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Lab 3 in-memory store; replaced by Spring Data JPA in Lab 4. */
@Repository
public class InMemoryEmployeeRepository {

	private final Map<Long, Employee> employees = new ConcurrentHashMap<>();

	private final AtomicLong nextId = new AtomicLong(1);

	public Employee save(Employee employee) {
		if (employee.getId() == null) {
			employee.setId(nextId.getAndIncrement());
		}
		employees.put(employee.getId(), employee);
		return employee;
	}

	/** All Employees in creation order. */
	public List<Employee> findAll() {
		return employees.values().stream()
				.sorted(Comparator.comparing(Employee::getId))
				.toList();
	}

	public Optional<Employee> findById(Long id) {
		return Optional.ofNullable(employees.get(id));
	}

}
