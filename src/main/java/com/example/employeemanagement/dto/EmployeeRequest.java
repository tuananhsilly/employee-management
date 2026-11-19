package com.example.employeemanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class EmployeeRequest {

	@NotBlank(message = "Họ tên không được để trống")
	@Size(max = 255, message = "Họ tên tối đa 255 ký tự")
	private String fullName;

	@NotBlank(message = "Email không được để trống")
	@Email(message = "Email không đúng định dạng")
	@Size(max = 255, message = "Email tối đa 255 ký tự")
	private String email;

	@Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
	private String phone;

	@Size(max = 100, message = "Chức vụ tối đa 100 ký tự")
	private String position;

	@NotNull(message = "Ngày vào làm không được để trống")
	private LocalDate hireDate;

	@NotNull(message = "Phòng ban không được để trống")
	private Long departmentId;

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public LocalDate getHireDate() {
		return hireDate;
	}

	public void setHireDate(LocalDate hireDate) {
		this.hireDate = hireDate;
	}

	public Long getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(Long departmentId) {
		this.departmentId = departmentId;
	}

}
