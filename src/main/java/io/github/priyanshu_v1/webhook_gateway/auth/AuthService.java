package io.github.priyanshu_v1.webhook_gateway.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.priyanshu_v1.webhook_gateway.auth.dto.AuthResponse;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.AuthResult;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.LoginRequest;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.RegisterRequest;
import io.github.priyanshu_v1.webhook_gateway.auth.dto.UserResponse;
import io.github.priyanshu_v1.webhook_gateway.auth.exception.RegistrationClosedException;
import io.github.priyanshu_v1.webhook_gateway.entity.User;
import io.github.priyanshu_v1.webhook_gateway.entity.UserRole;
import io.github.priyanshu_v1.webhook_gateway.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    @Value("${hook-shuttle.auth.refresh.sliding-window}")
    private Duration slidingWindow;

    @Value("${hook-shuttle.auth.refresh.absolute-ceiling}")
    private Duration absoluteCeiling;
    
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.count() > 0) {
            throw new RegistrationClosedException("Registration is closed. An admin account already exists.");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setOrganizationName(request.organizationName());
        user.setRole(UserRole.ADMIN);

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        AuthResponse authResponse = AuthResponse.bearer(accessToken, user.getEmail(), user.getRole().name());
        
        String rawRefreshToken = createRefreshTokenSession(user.getId());

        return new AuthResult(authResponse, rawRefreshToken, absoluteCeiling);    
    }

    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String accessToken = jwtService.generateToken(user.getId(),user.getEmail(), user.getRole().name());
        AuthResponse authResponse = AuthResponse.bearer(accessToken, user.getEmail(), user.getRole().name());
        
        String rawRefreshToken = createRefreshTokenSession(user.getId());
       
        return new AuthResult(authResponse, rawRefreshToken, absoluteCeiling);
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
    
    
    @Transactional
    public String createRefreshTokenSession(UUID userId) {
        String rawToken = generateSecureToken();
        String prefix = rawToken.substring(0, 16);
        String hashedToken = passwordEncoder.encode(rawToken);

        Instant now = Instant.now();
        UUID familyId = UUID.randomUUID();

        RefreshToken tokenEntity = RefreshToken.builder()
                .userId(userId)
                .tokenPrefix(prefix)
                .tokenHash(hashedToken)
                .familyId(familyId)
                .expiresAt(now.plus(slidingWindow))
                .absoluteExpiresAt(now.plus(absoluteCeiling))
                .revoked(false)
                .build();

        refreshTokenRepository.save(tokenEntity);
        return rawToken;
    }

    @Transactional
    public AuthResult rotateRefreshToken(String rawIncomingToken) {
        if (rawIncomingToken == null || rawIncomingToken.length() < 16) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        String prefix = rawIncomingToken.substring(0, 16);
        Instant now = Instant.now();

        RefreshToken existingToken = refreshTokenRepository.findByTokenPrefix(prefix)
                .orElseThrow(() -> new SecurityException("Invalid refresh token"));

        if (existingToken.isRevoked()) {
            refreshTokenRepository.revokeFamily(existingToken.getFamilyId());
            throw new SecurityException("Security Alert: Token reuse detected. Session family revoked.");
        }

        if (existingToken.getExpiresAt().isBefore(now) || existingToken.getAbsoluteExpiresAt().isBefore(now)) {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            throw new SecurityException("Refresh token has expired");
        }

        if (!passwordEncoder.matches(rawIncomingToken, existingToken.getTokenHash())) {
            throw new SecurityException("Invalid refresh token credentials");
        }

        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        String newRawToken = generateSecureToken();
        String newPrefix = newRawToken.substring(0, 16);
        String newHashedToken = passwordEncoder.encode(newRawToken);

        Instant nextSlidingExpiry = now.plus(slidingWindow);
        Instant absoluteCeiling = existingToken.getAbsoluteExpiresAt();
        Instant finalExpiresAt = nextSlidingExpiry.isBefore(absoluteCeiling) ? nextSlidingExpiry : absoluteCeiling;

        RefreshToken newTokenEntity = RefreshToken.builder()
                .userId(existingToken.getUserId())
                .tokenPrefix(newPrefix)
                .tokenHash(newHashedToken)
                .familyId(existingToken.getFamilyId())
                .expiresAt(finalExpiresAt)
                .absoluteExpiresAt(absoluteCeiling)
                .revoked(false)
                .build();

        refreshTokenRepository.save(newTokenEntity);
        
        
        // Fetch user details to generate the new JWT access token
        User user = userRepository.findById(existingToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        AuthResponse authResponse = AuthResponse.bearer(accessToken, user.getEmail(), user.getRole().name());
        
        Duration cookieMaxAge = Duration.between(now, finalExpiresAt);

        return new AuthResult(authResponse, newRawToken, cookieMaxAge);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }
}