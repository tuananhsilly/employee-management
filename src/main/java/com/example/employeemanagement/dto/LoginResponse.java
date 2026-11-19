package com.example.employeemanagement.dto;

/** Body of {@code POST /api/auth/login}: send {@code accessToken} as {@code Authorization: Bearer}. */
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
