package io.github.priyanshu_v1.webhook_gateway.auth;

import io.github.priyanshu_v1.webhook_gateway.auth.dto.AuthResponse;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.LoginRequest;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.RegisterRequest;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.UserResponse;
import io.github.priyanshu_v1.webhook_gateway.auth.exception.RegistrationClosedException;
import io.github.priyanshu_v1.webhook_gateway.entity.User;
import io.github.priyanshu_v1.webhook_gateway.entity.UserRole;
import io.github.priyanshu_v1.webhook_gateway.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.count() > 0) {
            throw new RegistrationClosedException("Registration is closed. An admin account already exists.");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setOrganizationName(request.organizationName());
        user.setRole(UserRole.ADMIN);

        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.bearer(token, user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(),user.getEmail(), user.getRole().name());
        return AuthResponse.bearer(token, user.getEmail(), user.getRole().name());
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}