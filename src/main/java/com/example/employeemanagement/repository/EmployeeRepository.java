package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	@Query("SELECT COUNT(e) FROM Employee e")
	long countEmployees();

	/** Employees hired per calendar month, for Hire Dates in {@code [from, to)}. */
	@Query("""
			SELECT new com.example.employeemanagement.repository.HiresInMonth(
			    YEAR(e.hireDate), MONTH(e.hireDate), COUNT(e))
			FROM Employee e
			WHERE e.hireDate >= :from AND e.hireDate < :to
			GROUP BY YEAR(e.hireDate), MONTH(e.hireDate)""")
	List<HiresInMonth> countHiresByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

}
