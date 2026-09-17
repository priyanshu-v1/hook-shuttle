package io.github.priyanshu_v1.webhook_gateway.auth.dto;

public record AuthResponse(String token, String tokenType, String email, String role) {
	public static AuthResponse bearer(String token, String email, String role) {
		return new AuthResponse(token, "Bearer", email, role);
	}
}