package io.github.priyanshu_v1.webhook_gateway.api_keys;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.priyanshu_v1.webhook_gateway.api_keys.dto.ApiKeyAuthProjection;
import io.github.priyanshu_v1.webhook_gateway.auth.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String rawApiKey = request.getHeader("X-API-KEY");

        if (rawApiKey != null && !rawApiKey.isBlank()) {
            String prefix = rawApiKey.substring(0, Math.min(rawApiKey.length(), 16));
            List<ApiKeyAuthProjection> potentialKeys = apiKeyRepository.findAuthDetailsByPrefixAndStatus(prefix, "ACTIVE");

            Optional<ApiKeyAuthProjection> matchedKey = potentialKeys.stream()
                    .filter(k -> passwordEncoder.matches(rawApiKey, k.apiKeyHash()))
                    .findFirst();

            if (matchedKey.isPresent()) {
            	ApiKeyAuthProjection authData = matchedKey.get();
            	apiKeyRepository.updateLastUsedAt(authData.apiKeyHash());
            	
            	UserPrincipal principal = new UserPrincipal(authData.userId(), authData.userEmail());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, List.of());
                                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}