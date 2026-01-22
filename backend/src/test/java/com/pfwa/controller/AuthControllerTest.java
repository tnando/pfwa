package com.pfwa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfwa.config.AppProperties;
import com.pfwa.dto.auth.LoginRequest;
import com.pfwa.dto.auth.LoginResponse;
import com.pfwa.dto.auth.RegisterRequest;
import com.pfwa.dto.auth.RegisterResponse;
import com.pfwa.dto.auth.UserProfile;
import com.pfwa.exception.EmailAlreadyExistsException;
import com.pfwa.exception.GlobalExceptionHandler;
import com.pfwa.exception.InvalidCredentialsException;
import com.pfwa.security.JwtAuthenticationFilter;
import com.pfwa.service.AuthService;
import com.pfwa.service.SessionService;
import com.pfwa.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AuthController.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private SessionService sessionService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // Setup default app properties
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        jwt.setAccessTokenExpirationMinutes(15);
        jwt.setRefreshTokenExpirationDays(7);
        jwt.setRefreshTokenRememberMeDays(30);
        when(appProperties.getJwt()).thenReturn(jwt);
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should return 201 CREATED on successful registration")
        void register_success() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "user@example.com",
                    "SecurePass123!",
                    "SecurePass123!"
            );

            UUID userId = UUID.randomUUID();
            RegisterResponse response = RegisterResponse.success(userId);

            when(authService.register(any(RegisterRequest.class), anyString())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when email is invalid")
        void register_invalidEmail() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "invalid-email",
                    "SecurePass123!",
                    "SecurePass123!"
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when password is weak")
        void register_weakPassword() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "user@example.com",
                    "weak",  // Too short, missing required characters
                    "weak"
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when passwords do not match")
        void register_passwordMismatch() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "user@example.com",
                    "SecurePass123!",
                    "DifferentPass123!"
            );

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when email already exists")
        void register_emailExists() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "existing@example.com",
                    "SecurePass123!",
                    "SecurePass123!"
            );

            when(authService.register(any(RegisterRequest.class), anyString()))
                    .thenThrow(new EmailAlreadyExistsException("Email already exists"));

            // When/Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"))
                    .andExpect(jsonPath("$.message").value("Email already exists"));
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should return 200 OK on successful login")
        void login_success() throws Exception {
            // Given
            LoginRequest request = new LoginRequest(
                    "user@example.com",
                    "SecurePass123!",
                    false
            );

            UUID userId = UUID.randomUUID();
            UserProfile userProfile = new UserProfile(userId, "user@example.com", "John", "Doe");
            LoginResponse loginResponse = new LoginResponse(userProfile, 900);

            TokenService.RefreshTokenDetails refreshToken = new TokenService.RefreshTokenDetails(
                    "refresh-token",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    Instant.now().plus(7, ChronoUnit.DAYS)
            );

            AuthService.LoginResult result = new AuthService.LoginResult(
                    loginResponse, "access-token", refreshToken
            );

            when(authService.login(any(LoginRequest.class), any())).thenReturn(result);

            // When/Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.id").value(userId.toString()))
                    .andExpect(jsonPath("$.user.email").value("user@example.com"))
                    .andExpect(jsonPath("$.expiresIn").value(900));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED on invalid credentials")
        void login_invalidCredentials() throws Exception {
            // Given
            LoginRequest request = new LoginRequest(
                    "user@example.com",
                    "wrongPassword",
                    false
            );

            when(authService.login(any(LoginRequest.class), any()))
                    .thenThrow(new InvalidCredentialsException());

            // When/Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when email is missing")
        void login_missingEmail() throws Exception {
            // Given
            String requestJson = """
                    {
                        "password": "SecurePass123!",
                        "rememberMe": false
                    }
                    """;

            // When/Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should return 200 OK on successful logout")
        void logout_success() throws Exception {
            // When/Then
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }

    @Nested
    @DisplayName("POST /auth/password/forgot")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should return 200 OK regardless of email existence")
        void forgotPassword_success() throws Exception {
            // Given
            String requestJson = """
                    {
                        "email": "user@example.com"
                    }
                    """;

            // When/Then
            mockMvc.perform(post("/auth/password/forgot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            "If the email exists in our system, you will receive a password reset link."
                    ));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when email is invalid")
        void forgotPassword_invalidEmail() throws Exception {
            // Given
            String requestJson = """
                    {
                        "email": "invalid-email"
                    }
                    """;

            // When/Then
            mockMvc.perform(post("/auth/password/forgot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }
}
