package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.Employee;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Combinable search filters for Employees; a null or blank filter is ignored. */
public final class EmployeeSpecifications {

	private EmployeeSpecifications() {
	}

	public static Specification<Employee> matching(String name, Long departmentId) {
		List<Specification<Employee>> filters = new ArrayList<>();
		if (name != null && !name.isBlank()) {
			filters.add(fullNameContains(name.strip()));
		}
		if (departmentId != null) {
			filters.add(inDepartment(departmentId));
		}
		return Specification.allOf(filters);
	}

	private static Specification<Employee> inDepartment(Long departmentId) {
		return (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId);
	}

	private static Specification<Employee> fullNameContains(String name) {
		String pattern = "%" + escapeLike(name.toLowerCase(Locale.ROOT)) + "%";
		return (root, query, cb) -> cb.like(cb.lower(root.get("fullName")), pattern, '\\');
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
