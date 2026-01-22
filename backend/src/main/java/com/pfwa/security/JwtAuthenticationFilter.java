package com.pfwa.security;

import com.pfwa.entity.User;
import com.pfwa.exception.InvalidTokenException;
import com.pfwa.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Filter that validates JWT tokens on each request and sets up authentication context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                authenticateWithToken(token, request);
            }
        } catch (InvalidTokenException e) {
            logger.debug("Invalid token: {}", e.getMessage());
            // Clear security context on invalid token
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            logger.error("Could not set user authentication in security context", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from the request.
     * First checks the Authorization header, then falls back to cookie.
     */
    private String extractToken(HttpServletRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        // Fall back to cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Authenticates the request with the provided token.
     */
    private void authenticateWithToken(String token, HttpServletRequest request) {
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        UUID sessionId = jwtTokenProvider.getSessionIdFromToken(token);

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.debug("User not found for token");
            return;
        }

        User user = userOptional.get();

        // Validate token version
        if (!jwtTokenProvider.validateTokenVersion(token, user.getTokenVersion())) {
            logger.debug("Token version mismatch - token has been revoked");
            throw new InvalidTokenException("Token has been revoked");
        }

        // Check if account is locked
        if (user.isAccountLocked() && !user.isLockExpired()) {
            logger.debug("Account is locked");
            return;
        }

        // Unlock if lock has expired
        if (user.isAccountLocked() && user.isLockExpired()) {
            user.resetFailedLoginAttempts();
            userRepository.save(user);
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user, sessionId);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        logger.debug("Authenticated user: {}", user.getEmail());
    }
}
