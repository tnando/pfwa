package com.pfwa.exception;

import com.pfwa.dto.auth.ErrorResponse;
import com.pfwa.dto.auth.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses across the API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid annotated request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        BindingResult result = ex.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors().stream()
                .map(error -> FieldError.of(
                        error.getField(),
                        error.getDefaultMessage(),
                        resolveErrorCode(error.getField(), error.getCode())
                ))
                .collect(Collectors.toList());

        logger.debug("Validation failed for request to {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity.badRequest()
                .body(ErrorResponse.withFieldErrors(
                        "VALIDATION_ERROR",
                        "Validation failed",
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    /**
     * Handles email already exists exception.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        logger.debug("Email already exists attempt on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONFLICT",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles invalid credentials exception.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        logger.debug("Invalid credentials attempt on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        "UNAUTHORIZED",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles account locked exception.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request) {

        logger.info("Locked account access attempt on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        "FORBIDDEN",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles email not verified exception.
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(
            EmailNotVerifiedException ex,
            HttpServletRequest request) {

        logger.debug("Unverified email login attempt on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        "FORBIDDEN",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles invalid token exception.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request) {

        logger.debug("Invalid token used on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles token expired exception.
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(
            TokenExpiredException ex,
            HttpServletRequest request) {

        logger.debug("Expired token used on {}", request.getRequestURI());

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        "BAD_REQUEST",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles token already used exception.
     */
    @ExceptionHandler(TokenAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleTokenAlreadyUsed(
            TokenAlreadyUsedException ex,
            HttpServletRequest request) {

        logger.debug("Already used token attempted on {}", request.getRequestURI());

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        "BAD_REQUEST",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles token reuse detection (security breach).
     */
    @ExceptionHandler(TokenReuseException.class)
    public ResponseEntity<ErrorResponse> handleTokenReuse(
            TokenReuseException ex,
            HttpServletRequest request) {

        logger.warn("Token reuse detected on {} - potential security breach", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        "UNAUTHORIZED",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles rate limit exceeded exception.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        logger.info("Rate limit exceeded on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(ErrorResponse.of(
                        "TOO_MANY_REQUESTS",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles session not found exception.
     */
    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(
            SessionNotFoundException ex,
            HttpServletRequest request) {

        logger.debug("Session not found on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles transaction not found exception.
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(
            TransactionNotFoundException ex,
            HttpServletRequest request) {

        logger.debug("Transaction not found on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles category not found exception.
     */
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(
            CategoryNotFoundException ex,
            HttpServletRequest request) {

        logger.debug("Category not found on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles category type mismatch exception.
     */
    @ExceptionHandler(CategoryTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleCategoryTypeMismatch(
            CategoryTypeMismatchException ex,
            HttpServletRequest request) {

        logger.debug("Category type mismatch on {}", request.getRequestURI());

        List<FieldError> fieldErrors = List.of(
                FieldError.of("categoryId", ex.getMessage(), "category.type.mismatch")
        );

        return ResponseEntity.badRequest()
                .body(ErrorResponse.withFieldErrors(
                        "VALIDATION_ERROR",
                        "Validation failed",
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    /**
     * Handles transaction access denied exception.
     */
    @ExceptionHandler(TransactionAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTransactionAccessDenied(
            TransactionAccessDeniedException ex,
            HttpServletRequest request) {

        logger.debug("Transaction access denied on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        "FORBIDDEN",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles method argument type mismatch (e.g., invalid UUID format).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        logger.debug("Type mismatch on {}: {}", request.getRequestURI(), ex.getMessage());

        String message = String.format("Invalid value for parameter '%s'", ex.getName());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        "BAD_REQUEST",
                        message,
                        request.getRequestURI()
                ));
    }

    /**
     * Handles Spring Security authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        logger.debug("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        "UNAUTHORIZED",
                        "Authentication required",
                        request.getRequestURI()
                ));
    }

    /**
     * Handles Spring Security bad credentials exception.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        logger.debug("Bad credentials on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        "UNAUTHORIZED",
                        "Invalid email or password",
                        request.getRequestURI()
                ));
    }

    /**
     * Handles access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        logger.debug("Access denied on {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        "FORBIDDEN",
                        "You do not have permission to perform this action",
                        request.getRequestURI()
                ));
    }

    /**
     * Handles illegal argument exceptions (bad request parameters).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        logger.debug("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        "BAD_REQUEST",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles illegal state exceptions (invalid operations).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {

        logger.debug("Illegal state on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        "BAD_REQUEST",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles all other exceptions as internal server errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String requestId = request.getHeader("X-Request-ID");
        logger.error("Unexpected error on {} (requestId: {})", request.getRequestURI(), requestId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred. Please try again later.",
                        request.getRequestURI(),
                        requestId
                ));
    }

    /**
     * Resolves a standardized error code based on field and validation error.
     */
    private String resolveErrorCode(String field, String code) {
        if (code == null) {
            return field + ".invalid";
        }
        return switch (code) {
            case "NotBlank", "NotNull", "NotEmpty" -> field + ".required";
            case "Email" -> "email.format.invalid";
            case "Size" -> field + ".length.invalid";
            case "Min" -> field + ".length.min";
            case "Max" -> field + ".length.max";
            case "Pattern" -> field + ".format.invalid";
            default -> field + "." + code.toLowerCase();
        };
    }
}
