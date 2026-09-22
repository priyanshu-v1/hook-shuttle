package io.github.priyanshu_v1.webhook_gateway.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.priyanshu_v1.webhook_gateway.auth.dto.AuthResponse;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.AuthResult;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.LoginRequest;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.RegisterRequest;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.UserResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    
    @Value("${webhook-gateway.auth.refresh.absolute-ceiling}")
    private Duration absoluteCeiling;

    @Value("${webhook-gateway.auth.refresh.secure-cookie}")
    private boolean secureCookie;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authService.register(request);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.rawRefreshToken())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/refresh")
                .maxAge(result.cookieMaxAge())
                .sameSite("Strict")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(result.authResponse());
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request);
        
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.rawRefreshToken())
                .httpOnly(true)
                .secure(secureCookie) // Set to false if testing locally over HTTP (or true if using HTTPS)
                .path("/api/v1/auth/refresh")
                .maxAge(absoluteCeiling) // Uses the injected Duration from application.properties
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(result.authResponse());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(authService.getCurrentUser(email));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refreshToken") String rawRefreshToken
    ) {
        AuthResult result = authService.rotateRefreshToken(rawRefreshToken);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.rawRefreshToken())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/refresh")
                .maxAge(result.cookieMaxAge())
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(result.authResponse());
    }
}