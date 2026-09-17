package io.github.priyanshu_v1.webhook_gateway.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "Email is required")
		@Email(message = "Invalid email format")
		String email,

		@NotBlank(message = "Password is required")
		@Size(min = 8, message = "Password must be at least 8 characters")
		String password,
		
		@NotBlank(message = "Organization name is required")
        String organizationName)
{}