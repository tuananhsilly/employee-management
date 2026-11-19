package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.DepartmentResponse;
import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.dto.EmployeeResponse;
import com.example.employeemanagement.exception.DuplicateEmailException;
import com.example.employeemanagement.exception.InvalidReferenceException;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.service.DepartmentService;
import com.example.employeemanagement.service.EmployeeService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thymeleaf pages for managing Employees in the browser, over the same service layer as the REST API. */
@Controller
@RequestMapping("/employees")
public class EmployeePageController {

	private static final int PAGE_SIZE = 10;

	private final EmployeeService employeeService;

	private final DepartmentService departmentService;

	private final ModelMapper modelMapper;

	public EmployeePageController(EmployeeService employeeService, DepartmentService departmentService,
			ModelMapper modelMapper) {
		this.employeeService = employeeService;
		this.departmentService = departmentService;
		this.modelMapper = modelMapper;
	}

	/**
	 * Every page gets the Departments (for dropdowns) and an id → name map (for showing an
	 * Employee's Department in tables).
	 */
	@ModelAttribute
	void addDepartments(Model model) {
		List<DepartmentResponse> departments = departmentService.findAll();
		Map<Long, String> names = new LinkedHashMap<>();
		departments.forEach(department -> names.put(department.getId(), department.getName()));
		model.addAttribute("departments", departments);
		model.addAttribute("departmentNames", names);
	}

	@GetMapping("/list")
	public String list(@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("employees", employeeService.search(null, null, Math.max(page, 0), PAGE_SIZE));
		model.addAttribute("pageUrl", "/employees/list");
		return "employees/list";
	}

	/** Search by name and/or Department; with no filter it lists every Employee. */
	@GetMapping("/search")
	public String search(@RequestParam(required = false) String name, @RequestParam(required = false) Long departmentId,
			@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("name", name);
		model.addAttribute("departmentId", departmentId);
		model.addAttribute("employees", employeeService.search(name, departmentId, Math.max(page, 0), PAGE_SIZE));
		model.addAttribute("pageUrl", UriComponentsBuilder.fromPath("/employees/search")
				.queryParamIfPresent("name", Optional.ofNullable(name))
				.queryParamIfPresent("departmentId", Optional.ofNullable(departmentId))
				.encode()
				.toUriString());
		return "employees/search";
	}

	@GetMapping("/add")
	public String addForm(Model model) {
		model.addAttribute("employee", new EmployeeRequest());
		return "employees/form";
	}

	@PostMapping("/add")
	public String add(@Valid @ModelAttribute("employee") EmployeeRequest employee, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "employees/form";
		}
		EmployeeResponse created;
		try {
			created = employeeService.create(employee);
		}
		catch (DuplicateEmailException | InvalidReferenceException ex) {
			rejectField(bindingResult, ex);
			return "employees/form";
		}
		redirectAttributes.addFlashAttribute("message", "Đã thêm nhân viên " + created.getFullName());
		return "redirect:/employees/list";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		EmployeeResponse employee = employeeService.findById(id);
		model.addAttribute("employee", modelMapper.map(employee, EmployeeRequest.class));
		addEditTarget(employee, model);
		return "employees/form";
	}

	@PostMapping("/{id}/edit")
	public String edit(@PathVariable Long id, @Valid @ModelAttribute("employee") EmployeeRequest employee,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			addEditTarget(employeeService.findById(id), model);
			return "employees/form";
		}
		EmployeeResponse updated;
		try {
			updated = employeeService.update(id, employee);
		}
		catch (DuplicateEmailException | InvalidReferenceException ex) {
			rejectField(bindingResult, ex);
			addEditTarget(employeeService.findById(id), model);
			return "employees/form";
		}
		redirectAttributes.addFlashAttribute("message", "Đã cập nhật nhân viên " + updated.getFullName());
		return "redirect:/employees/list";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		String fullName = employeeService.findById(id).getFullName();
		employeeService.delete(id);
		redirectAttributes.addFlashAttribute("message", "Đã xoá nhân viên " + fullName);
		return "redirect:/employees/list";
	}

	/**
	 * Errors on these pages render an HTML page instead of the REST API's JSON error
	 * (a controller's own handlers take precedence over the global advice).
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ModelAndView notFound(ResourceNotFoundException ex) {
		return errorPage(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/** A non-numeric Employee id is an unknown Employee; any other bad parameter is a bad request. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ModelAndView typeMismatch(MethodArgumentTypeMismatchException ex) {
		if ("id".equals(ex.getName())) {
			return errorPage(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên");
		}
		return errorPage(HttpStatus.BAD_REQUEST, "Tham số '" + ex.getName() + "' có giá trị sai kiểu dữ liệu");
	}

	private static ModelAndView errorPage(HttpStatus status, String message) {
		return new ModelAndView("employees/error", Map.of("message", message), status);
	}

	/** The form posts to the edit URL of this Employee and shows its (read-only) Employee Code. */
	private static void addEditTarget(EmployeeResponse employee, Model model) {
		model.addAttribute("employeeId", employee.getId());
		model.addAttribute("employeeCode", employee.getEmployeeCode());
	}

	/** Shows a rule the service enforces (unique email, existing Department) next to its field. */
	private static void rejectField(BindingResult bindingResult, RuntimeException ex) {
		String field = ex instanceof InvalidReferenceException invalid ? invalid.getField() : "email";
		bindingResult.rejectValue(field, "invalid", ex.getMessage());
	}

}
