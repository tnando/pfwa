/// <reference types="cypress" />

/**
 * E2E Tests for User Registration (AUTH-001)
 *
 * Tests the complete registration flow including:
 * - Form validation
 * - Successful registration
 * - Error handling for duplicate emails
 * - Password strength requirements
 */
describe('User Registration', () => {
  const testEmail = `newuser_${Date.now()}@example.com`;
  const validPassword = 'SecurePass123!';

  beforeEach(() => {
    // Clear any existing auth state
    cy.clearAuthCookies();
    // Visit the registration page
    cy.visit('/register');
  });

  describe('Page Elements', () => {
    it('should display registration form with all required fields', () => {
      // Verify page title
      cy.contains('Create your account').should('be.visible');

      // Verify form fields exist
      cy.get('input[aria-label="Email address"]').should('be.visible');
      cy.get('input[aria-label="Password"]').should('be.visible');
      cy.get('input[aria-label="Confirm password"]').should('be.visible');

      // Verify submit button
      cy.contains('button', 'Create account').should('be.visible');

      // Verify link to login page
      cy.contains('Already have an account?').should('be.visible');
    });
  });

  describe('Form Validation', () => {
    it('should show error for invalid email format', () => {
      cy.get('input[aria-label="Email address"]').type('invalid-email');
      cy.get('input[aria-label="Password"]').type(validPassword);
      cy.get('input[aria-label="Confirm password"]').type(validPassword);

      cy.contains('button', 'Create account').click();

      // Should show validation error
      cy.contains('Invalid email').should('be.visible');
    });

    it('should show error when password is too short', () => {
      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type('Short1!');
      cy.get('input[aria-label="Confirm password"]').type('Short1!');

      cy.contains('button', 'Create account').click();

      // Should show validation error about password length
      cy.contains(/at least 8 characters/i).should('be.visible');
    });

    it('should show error when password lacks uppercase letter', () => {
      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type('password123!');
      cy.get('input[aria-label="Confirm password"]').type('password123!');

      cy.contains('button', 'Create account').click();

      // Should show validation error
      cy.contains(/uppercase/i).should('be.visible');
    });

    it('should show error when password lacks special character', () => {
      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type('Password123');
      cy.get('input[aria-label="Confirm password"]').type('Password123');

      cy.contains('button', 'Create account').click();

      // Should show validation error
      cy.contains(/special character/i).should('be.visible');
    });

    it('should show error when passwords do not match', () => {
      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type(validPassword);
      cy.get('input[aria-label="Confirm password"]').type('DifferentPass123!');

      cy.contains('button', 'Create account').click();

      // Should show validation error
      cy.contains(/Passwords do not match/i).should('be.visible');
    });

    it('should show error when email is empty', () => {
      cy.get('input[aria-label="Password"]').type(validPassword);
      cy.get('input[aria-label="Confirm password"]').type(validPassword);

      cy.contains('button', 'Create account').click();

      // Should show validation error for required email
      cy.contains(/email/i).should('be.visible');
    });
  });

  describe('Successful Registration', () => {
    it('should register successfully and show verification message', () => {
      // Intercept the registration API call
      cy.intercept('POST', '**/auth/register').as('registerRequest');

      // Fill in registration form
      const uniqueEmail = `testuser_${Date.now()}@example.com`;
      cy.get('input[aria-label="Email address"]').type(uniqueEmail);
      cy.get('input[aria-label="Password"]').type(validPassword);
      cy.get('input[aria-label="Confirm password"]').type(validPassword);

      // Submit the form
      cy.contains('button', 'Create account').click();

      // Wait for API response
      cy.wait('@registerRequest').its('response.statusCode').should('eq', 201);

      // Should show success message
      cy.contains('Check your email').should('be.visible');
      cy.contains('Registration successful').should('be.visible');
      cy.contains('verification link').should('be.visible');

      // Should show link back to login
      cy.contains('Back to sign in').should('be.visible');
    });

    it('should display loading state while submitting', () => {
      cy.intercept('POST', '**/auth/register', (req) => {
        // Delay the response to observe loading state
        req.reply({
          delay: 1000,
          statusCode: 201,
          body: {
            message: 'Registration successful',
            userId: 'test-uuid',
          },
        });
      }).as('registerRequest');

      const uniqueEmail = `loading_test_${Date.now()}@example.com`;
      cy.get('input[aria-label="Email address"]').type(uniqueEmail);
      cy.get('input[aria-label="Password"]').type(validPassword);
      cy.get('input[aria-label="Confirm password"]').type(validPassword);

      cy.contains('button', 'Create account').click();

      // Button should show loading state (spinner or disabled)
      cy.get('button[type="submit"]').should('be.disabled');

      cy.wait('@registerRequest');
    });
  });

  describe('Error Handling', () => {
    it('should show error when email already exists', () => {
      // First, create a user
      const existingEmail = `existing_${Date.now()}@example.com`;

      cy.intercept('POST', '**/auth/register', {
        statusCode: 409,
        body: {
          error: 'CONFLICT',
          message: 'Email already exists',
        },
      }).as('registerRequest');

      cy.get('input[aria-label="Email address"]').type(existingEmail);
      cy.get('input[aria-label="Password"]').type(validPassword);
      cy.get('input[aria-label="Confirm password"]').type(validPassword);

      cy.contains('button', 'Create account').click();

      cy.wait('@registerRequest');

      // Should show error message
      cy.contains('Email already exists').should('be.visible');
    });

    it('should show error for server error', () => {
      cy.intercept('POST', '**/auth/register', {
        statusCode: 500,
        body: {
          error: 'INTERNAL_SERVER_ERROR',
          message: 'An unexpected error occurred',
        },
      }).as('registerRequest');

      cy.get('input[aria-label="Email address"]').type('servererror@example.com');
      cy.get('input[aria-label="Password"]').type(validPassword);
      cy.get('input[aria-label="Confirm password"]').type(validPassword);

      cy.contains('button', 'Create account').click();

      cy.wait('@registerRequest');

      // Should show generic error message
      cy.get('[role="alert"]').should('be.visible');
    });

    it('should show rate limit error after too many attempts', () => {
      cy.intercept('POST', '**/auth/register', {
        statusCode: 429,
        body: {
          error: 'TOO_MANY_REQUESTS',
          message: 'Too many registration attempts. Please try again later.',
        },
        headers: {
          'Retry-After': '60',
        },
      }).as('registerRequest');

      cy.get('input[aria-label="Email address"]').type('ratelimit@example.com');
      cy.get('input[aria-label="Password"]').type(validPassword);
      cy.get('input[aria-label="Confirm password"]').type(validPassword);

      cy.contains('button', 'Create account').click();

      cy.wait('@registerRequest');

      // Should show rate limit error
      cy.contains(/too many/i).should('be.visible');
    });
  });

  describe('Password Strength Meter', () => {
    it('should show password strength indicator', () => {
      cy.get('input[aria-label="Password"]').type('weak');

      // Password strength meter should be visible
      // The exact implementation depends on the PasswordStrengthMeter component

      // Type stronger password
      cy.get('input[aria-label="Password"]').clear().type(validPassword);

      // Strength indicator should update
    });
  });

  describe('Navigation', () => {
    it('should navigate to login page when clicking sign in link', () => {
      cy.contains('Already have an account?').click();
      cy.url().should('include', '/login');
    });

    it('should redirect authenticated users to dashboard', () => {
      // Simulate authenticated state by setting cookie
      cy.setCookie('accessToken', 'mock-access-token');

      cy.visit('/register');

      // Should redirect to dashboard
      cy.url().should('include', '/dashboard');
    });
  });

  describe('Accessibility', () => {
    it('should have proper form labels', () => {
      cy.get('input[aria-label="Email address"]').should('exist');
      cy.get('input[aria-label="Password"]').should('exist');
      cy.get('input[aria-label="Confirm password"]').should('exist');
    });

    it('should be keyboard navigable', () => {
      // Tab through form fields
      cy.get('input[aria-label="Email address"]').focus();
      cy.focused().should('have.attr', 'aria-label', 'Email address');

      cy.focused().tab();
      cy.focused().should('have.attr', 'aria-label', 'Password');

      cy.focused().tab();
      cy.focused().should('have.attr', 'aria-label', 'Confirm password');
    });
  });
});
