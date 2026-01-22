package com.pfwa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;

/**
 * Validator for the PasswordMatch annotation.
 * Compares two password fields to ensure they match.
 */
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        this.passwordField = constraintAnnotation.passwordField();
        this.confirmPasswordField = constraintAnnotation.confirmPasswordField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            String password = getFieldValue(value, passwordField);
            String confirmPassword = getFieldValue(value, confirmPasswordField);

            if (password == null && confirmPassword == null) {
                return true;
            }

            boolean matches = password != null && password.equals(confirmPassword);

            if (!matches) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Passwords do not match")
                        .addPropertyNode(confirmPasswordField)
                        .addConstraintViolation();
            }

            return matches;
        } catch (Exception e) {
            return false;
        }
    }

    private String getFieldValue(Object object, String fieldName) throws Exception {
        // Handle Java records
        if (object.getClass().isRecord()) {
            for (RecordComponent component : object.getClass().getRecordComponents()) {
                if (component.getName().equals(fieldName)) {
                    Method accessor = component.getAccessor();
                    return (String) accessor.invoke(object);
                }
            }
        }

        // Handle regular classes
        Method getter = object.getClass().getMethod(
                "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1)
        );
        return (String) getter.invoke(object);
    }
}
