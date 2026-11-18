package com.example.employeemanagement;

import org.springframework.boot.SpringApplication;

public class TestEmployeeManagementApplication {

	public static void main(String[] args) {
		SpringApplication.from(EmployeeManagementApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
