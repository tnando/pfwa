package com.pfwa.controller;

import com.pfwa.config.AppProperties;
import com.pfwa.dto.auth.ForgotPasswordRequest;
import com.pfwa.dto.auth.LoginRequest;
import com.pfwa.dto.auth.LoginResponse;
import com.pfwa.dto.auth.MessageResponse;
import com.pfwa.dto.auth.RegisterRequest;
import com.pfwa.dto.auth.RegisterResponse;
import com.pfwa.dto.auth.ResendVerificationRequest;
import com.pfwa.dto.auth.ResetPasswordRequest;
import com.pfwa.dto.auth.SessionDto;
import com.pfwa.dto.auth.SessionListResponse;
import com.pfwa.dto.auth.TokenRefreshResponse;
import com.pfwa.dto.auth.VerifyEmailRequest;
import com.pfwa.security.UserPrincipal;
import com.pfwa.service.AuthService;
import com.pfwa.service.SessionService;
import com.pfwa.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final SessionService sessionService;
    private final AppProperties appProperties;

    public AuthController(AuthService authService,
                          SessionService sessionService,
                          AppProperties appProperties) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.appProperties = appProperties;
    }

    /**
     * POST /auth/register - Register a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        RegisterResponse response = authService.register(request, clientIp);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /auth/login - Authenticate user and return tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthService.LoginResult result = authService.login(request, httpRequest);

        // Set cookies
        setAccessTokenCookie(httpResponse, result.accessToken());
        setRefreshTokenCookie(httpResponse, result.refreshToken().token(),
                request.isRememberMe());

        return ResponseEntity.ok(result.response());
    }

    /**
     * POST /auth/logout - Logout user and clear tokens.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String refreshToken = getRefreshTokenFromCookie(httpRequest);
        authService.logout(refreshToken);

        // Clear cookies
        clearAuthCookies(httpResponse);

        return ResponseEntity.ok(MessageResponse.of("Logged out successfully"));
    }

    /**
     * POST /auth/refresh - Refresh access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String refreshToken = getRefreshTokenFromCookie(httpRequest);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthService.RefreshResult result = authService.refreshToken(refreshToken, httpRequest);

        // Set new cookies
        setAccessTokenCookie(httpResponse, result.accessToken());
        setRefreshTokenCookie(httpResponse, result.refreshToken().token(), false);

        return ResponseEntity.ok(result.response());
    }

    /**
     * POST /auth/password/forgot - Request password reset email.
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        authService.requestPasswordReset(request.email(), clientIp);

        // Always return success to prevent email enumeration
        return ResponseEntity.ok(MessageResponse.of(
                "If the email exists in our system, you will receive a password reset link."
        ));
    }

    /**
     * POST /auth/password/reset - Complete password reset.
     */
    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIpAddress(httpRequest);
        authService.resetPassword(request.token(), request.newPassword(), clientIp);

        return ResponseEntity.ok(MessageResponse.of(
                "Password has been reset successfully. Please log in with your new password."
        ));
    }

    /**
     * POST /auth/verify-email - Verify email address.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        authService.verifyEmail(request.token());

        return ResponseEntity.ok(MessageResponse.of(
                "Email verified successfully. You can now log in."
        ));
    }

    /**
     * POST /auth/verify-email/resend - Resend verification email.
     */
    @PostMapping("/verify-email/resend")
    public ResponseEntity<MessageResponse> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request) {

        authService.resendVerificationEmail(request.email());

        return ResponseEntity.ok(MessageResponse.of("Verification email sent."));
    }

    /**
     * GET /auth/sessions - Get all active sessions for the authenticated user.
     */
    @GetMapping("/sessions")
    public ResponseEntity<SessionListResponse> getActiveSessions(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<SessionDto> sessions = sessionService.getActiveSessions(
                userPrincipal.getId(),
                userPrincipal.getSessionId()
        );

        return ResponseEntity.ok(new SessionListResponse(sessions));
    }

    /**
     * DELETE /auth/sessions - Logout from all sessions.
     */
    @DeleteMapping("/sessions")
    public ResponseEntity<MessageResponse> logoutAllSessions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletResponse httpResponse) {

        sessionService.revokeAllSessions(userPrincipal.getId());

        // Clear current session cookies
        clearAuthCookies(httpResponse);

        return ResponseEntity.ok(MessageResponse.of(
                "All sessions have been terminated. You will need to log in again on all devices."
        ));
    }

    /**
     * DELETE /auth/sessions/{sessionId} - Revoke a specific session.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<MessageResponse> revokeSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        sessionService.revokeSession(
                userPrincipal.getId(),
                sessionId,
                userPrincipal.getSessionId()
        );

        return ResponseEntity.ok(MessageResponse.of("Session revoked successfully"));
    }

    // ==================== Helper Methods ====================

    /**
     * Sets the access token as an HttpOnly cookie.
     */
    private void setAccessTokenCookie(HttpServletResponse response, String token) {
        boolean secure = appProperties.getSecurity().isSecureCookies();
        String sameSite = appProperties.getSecurity().getCookieSameSite();

        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofMinutes(appProperties.getJwt().getAccessTokenExpirationMinutes()))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Sets the refresh token as an HttpOnly cookie.
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String token, boolean rememberMe) {
        int expirationDays = rememberMe ?
                appProperties.getJwt().getRefreshTokenRememberMeDays() :
                appProperties.getJwt().getRefreshTokenExpirationDays();

        boolean secure = appProperties.getSecurity().isSecureCookies();
        String sameSite = appProperties.getSecurity().getCookieSameSite();

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(expirationDays))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Clears authentication cookies.
     */
    private void clearAuthCookies(HttpServletResponse response) {
        boolean secure = appProperties.getSecurity().isSecureCookies();
        String sameSite = appProperties.getSecurity().getCookieSameSite();

        ResponseCookie clearAccessToken = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie clearRefreshToken = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/v1/auth")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", clearAccessToken.toString());
        response.addHeader("Set-Cookie", clearRefreshToken.toString());
    }

    /**
     * Extracts the refresh token from cookies.
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Gets the client IP address from the request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
