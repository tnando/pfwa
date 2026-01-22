package com.pfwa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that two password fields match.
 * Applied at the class level to compare password and confirmPassword fields.
 */
@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatch {

    String message() default "Passwords do not match";

    String passwordField();

    String confirmPasswordField();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
