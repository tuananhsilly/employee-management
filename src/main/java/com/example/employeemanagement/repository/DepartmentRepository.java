package com.example.employeemanagement.repository;

import com.example.employeemanagement.dto.DepartmentEmployeeCount;
import com.example.employeemanagement.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

	/** Every Department with its Employee count; the left join keeps Departments with none. */
	@Query("""
			SELECT new com.example.employeemanagement.dto.DepartmentEmployeeCount(d.id, d.name, COUNT(e))
			FROM Department d LEFT JOIN Employee e ON e.department = d
			GROUP BY d.id, d.name
			ORDER BY d.id""")
	List<DepartmentEmployeeCount> countEmployeesByDepartment();

}
