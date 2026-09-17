package io.github.priyanshu_v1.webhook_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import io.github.priyanshu_v1.webhook_gateway.api_keys.ApiKeyAuthenticationFilter;
import io.github.priyanshu_v1.webhook_gateway.api_keys.ApiKeyRepository;
import io.github.priyanshu_v1.webhook_gateway.auth.JwtAuthenticationFilter;
import io.github.priyanshu_v1.webhook_gateway.auth.JwtService;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(
            JwtService jwtService,
            ApiKeyRepository apiKeyRepository,
            PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    
    @Bean
    @Order(0)
    public SecurityFilterChain publicMockSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/test-webhook/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
    
 // 1. Ingestion / Dispatch Engine Chain (Strictly API Key Only)
    @Bean
    @Order(1)
    public SecurityFilterChain dispatchSecurityFilterChain(HttpSecurity http) throws Exception {
        ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(apiKeyRepository, passwordEncoder);

        return http
                .securityMatcher("/api/v1/webhooks/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    
    // 2. Management API Chain (Dual Auth: API Key OR JWT for Endpoints & API Keys)
    @Bean
    @Order(2)
    public SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
        ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(apiKeyRepository, passwordEncoder);
        JwtAuthenticationFilter jwtAuthFilter = new JwtAuthenticationFilter(jwtService);

        return http
                .securityMatcher("/api/v1/endpoints/**", "/api/v1/api-keys/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                // Add API key filter first, then JWT filter right after
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthFilter, ApiKeyAuthenticationFilter.class)
                .build();
    }

    // 3. Dashboard / Auth API Chain (Strictly JWT)
    @Bean
    @Order(3)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthFilter = new JwtAuthenticationFilter(jwtService);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    

}