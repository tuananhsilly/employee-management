package com.example.employeemanagement.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * The string is at most {@link #value()} bytes in UTF-8. {@code @Size} counts characters,
 * but BCrypt's limit is 72 bytes and Vietnamese letters take 2–3 bytes each.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxUtf8Bytes.Validator.class)
public @interface MaxUtf8Bytes {

	int value();

	String message();

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<MaxUtf8Bytes, String> {

		private int max;

		@Override
		public void initialize(MaxUtf8Bytes annotation) {
			this.max = annotation.value();
		}

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
		}

	}

}
