package com.pfwa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfwa.entity.User;
import com.pfwa.repository.RefreshTokenRepository;
import com.pfwa.repository.UserRepository;
import com.pfwa.repository.VerificationTokenRepository;
import com.pfwa.service.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

/**
 * Base class for integration tests with TestContainers and MockMvc.
 * Provides common setup, utilities, and cleanup for auth-related tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected VerificationTokenRepository verificationTokenRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TokenService tokenService;

    protected static final String TEST_EMAIL = "test@example.com";
    protected static final String TEST_PASSWORD = "SecurePass123!";
    protected static final String VALID_PASSWORD = "ValidPass123!";

    @BeforeEach
    void cleanupDatabase() {
        // Clean up in correct order due to foreign key constraints
        refreshTokenRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Creates a test user with email verified.
     */
    protected User createVerifiedUser(String email, String password) {
        User user = new User(email, passwordEncoder.encode(password));
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    /**
     * Creates a test user with email not verified.
     */
    protected User createUnverifiedUser(String email, String password) {
        User user = new User(email, passwordEncoder.encode(password));
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    /**
     * Creates a verified user with first and last name.
     */
    protected User createVerifiedUserWithName(String email, String password, String firstName, String lastName) {
        User user = new User(email, passwordEncoder.encode(password));
        user.setEmailVerified(true);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return userRepository.save(user);
    }

    /**
     * Extracts a cookie value from MvcResult.
     */
    protected String getCookieValue(MvcResult result, String cookieName) {
        Cookie cookie = result.getResponse().getCookie(cookieName);
        return cookie != null ? cookie.getValue() : null;
    }

    /**
     * Creates a mock HTTP request with the given User-Agent.
     */
    protected MockHttpServletRequest createMockRequest(String userAgent, String ipAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", userAgent);
        request.setRemoteAddr(ipAddress);
        return request;
    }

    /**
     * Generates a refresh token for a user.
     */
    protected TokenService.RefreshTokenDetails createRefreshTokenForUser(User user) {
        MockHttpServletRequest request = createMockRequest(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "192.168.1.100"
        );
        return tokenService.createRefreshToken(user, false, request);
    }

    /**
     * Creates a verification token for a user.
     */
    protected String createVerificationTokenForUser(User user) {
        return tokenService.createVerificationToken(user);
    }

    /**
     * Creates a password reset token for a user.
     */
    protected String createPasswordResetTokenForUser(User user) {
        return tokenService.createPasswordResetToken(user);
    }
}
