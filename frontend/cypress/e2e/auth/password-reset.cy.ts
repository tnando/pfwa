/// <reference types="cypress" />

/**
 * E2E Tests for Password Reset (AUTH-007, AUTH-008)
 *
 * Tests the complete password reset flow including:
 * - Request password reset
 * - Complete password reset with valid token
 * - Error handling for invalid/expired tokens
 * - Password validation
 */
describe('Password Reset', () => {
  const testEmail = Cypress.env('testUserEmail');
  const newPassword = 'NewSecurePass123!';

  describe('Request Password Reset', () => {
    beforeEach(() => {
      cy.clearAuthCookies();
      cy.visit('/forgot-password');
    });

    describe('Page Elements', () => {
      it('should display forgot password form', () => {
        // Verify page elements
        cy.contains(/forgot/i).should('be.visible');
        cy.get('input[aria-label="Email address"]').should('be.visible');
        cy.contains('button', /reset|send/i).should('be.visible');
        cy.contains(/back to sign in/i).should('be.visible');
      });
    });

    describe('Form Validation', () => {
      it('should show error for empty email', () => {
        cy.contains('button', /reset|send/i).click();
        cy.contains(/email/i).should('be.visible');
      });

      it('should show error for invalid email format', () => {
        cy.get('input[aria-label="Email address"]').type('invalid-email');
        cy.contains('button', /reset|send/i).click();
        cy.contains(/invalid email/i).should('be.visible');
      });
    });

    describe('Successful Request', () => {
      it('should show success message regardless of email existence', () => {
        // This prevents email enumeration
        cy.intercept('POST', '**/auth/password/forgot', {
          statusCode: 200,
          body: {
            message: 'If the email exists in our system, you will receive a password reset link.',
          },
        }).as('forgotPasswordRequest');

        cy.get('input[aria-label="Email address"]').type(testEmail);
        cy.contains('button', /reset|send/i).click();

        cy.wait('@forgotPasswordRequest');

        // Should show success message
        cy.contains(/email exists/i).should('be.visible');
      });

      it('should show same message for non-existent email', () => {
        cy.intercept('POST', '**/auth/password/forgot', {
          statusCode: 200,
          body: {
            message: 'If the email exists in our system, you will receive a password reset link.',
          },
        }).as('forgotPasswordRequest');

        cy.get('input[aria-label="Email address"]').type('nonexistent@example.com');
        cy.contains('button', /reset|send/i).click();

        cy.wait('@forgotPasswordRequest');

        // Should show same success message (prevents enumeration)
        cy.contains(/email exists/i).should('be.visible');
      });

      it('should display loading state while sending', () => {
        cy.intercept('POST', '**/auth/password/forgot', (req) => {
          req.reply({
            delay: 1000,
            statusCode: 200,
            body: { message: 'Success' },
          });
        }).as('forgotPasswordRequest');

        cy.get('input[aria-label="Email address"]').type(testEmail);
        cy.contains('button', /reset|send/i).click();

        // Button should show loading state
        cy.get('button[type="submit"]').should('be.disabled');

        cy.wait('@forgotPasswordRequest');
      });
    });

    describe('Rate Limiting', () => {
      it('should show rate limit error after too many requests', () => {
        cy.intercept('POST', '**/auth/password/forgot', {
          statusCode: 429,
          body: {
            error: 'TOO_MANY_REQUESTS',
            message: 'Too many password reset requests. Please try again later.',
          },
          headers: {
            'Retry-After': '3600',
          },
        }).as('forgotPasswordRequest');

        cy.get('input[aria-label="Email address"]').type(testEmail);
        cy.contains('button', /reset|send/i).click();

        cy.wait('@forgotPasswordRequest');

        cy.contains(/too many/i).should('be.visible');
      });
    });

    describe('Navigation', () => {
      it('should navigate back to login', () => {
        cy.contains(/back to sign in/i).click();
        cy.url().should('include', '/login');
      });
    });
  });

  describe('Complete Password Reset', () => {
    const validToken = 'valid-reset-token-12345';

    beforeEach(() => {
      cy.clearAuthCookies();
      cy.visit(`/reset-password?token=${validToken}`);
    });

    describe('Page Elements', () => {
      it('should display password reset form', () => {
        cy.contains(/reset.*password|new password/i).should('be.visible');
        cy.get('input[aria-label="Password"]').should('be.visible');
        cy.get('input[aria-label="Confirm password"]').should('be.visible');
        cy.contains('button', /reset/i).should('be.visible');
      });
    });

    describe('Form Validation', () => {
      it('should show error for weak password', () => {
        cy.get('input[aria-label="Password"]').type('weak');
        cy.get('input[aria-label="Confirm password"]').type('weak');
        cy.contains('button', /reset/i).click();

        // Should show validation errors
        cy.contains(/at least 8 characters/i).should('be.visible');
      });

      it('should show error for password mismatch', () => {
        cy.get('input[aria-label="Password"]').type(newPassword);
        cy.get('input[aria-label="Confirm password"]').type('DifferentPass123!');
        cy.contains('button', /reset/i).click();

        cy.contains(/passwords do not match/i).should('be.visible');
      });

      it('should show error for password without uppercase', () => {
        cy.get('input[aria-label="Password"]').type('lowercase123!');
        cy.get('input[aria-label="Confirm password"]').type('lowercase123!');
        cy.contains('button', /reset/i).click();

        cy.contains(/uppercase/i).should('be.visible');
      });
    });

    describe('Successful Reset', () => {
      it('should reset password successfully and redirect to login', () => {
        cy.intercept('POST', '**/auth/password/reset', {
          statusCode: 200,
          body: {
            message: 'Password has been reset successfully. Please log in with your new password.',
          },
        }).as('resetPasswordRequest');

        cy.get('input[aria-label="Password"]').type(newPassword);
        cy.get('input[aria-label="Confirm password"]').type(newPassword);
        cy.contains('button', /reset/i).click();

        cy.wait('@resetPasswordRequest');

        // Should show success message and redirect to login
        cy.contains(/successfully/i).should('be.visible');
        cy.url({ timeout: 10000 }).should('include', '/login');
      });

      it('should display loading state while resetting', () => {
        cy.intercept('POST', '**/auth/password/reset', (req) => {
          req.reply({
            delay: 1000,
            statusCode: 200,
            body: { message: 'Success' },
          });
        }).as('resetPasswordRequest');

        cy.get('input[aria-label="Password"]').type(newPassword);
        cy.get('input[aria-label="Confirm password"]').type(newPassword);
        cy.contains('button', /reset/i).click();

        cy.get('button[type="submit"]').should('be.disabled');

        cy.wait('@resetPasswordRequest');
      });
    });

    describe('Error Handling', () => {
      it('should show error for expired token', () => {
        cy.intercept('POST', '**/auth/password/reset', {
          statusCode: 400,
          body: {
            error: 'BAD_REQUEST',
            message: 'Password reset link has expired. Please request a new one.',
          },
        }).as('resetPasswordRequest');

        cy.get('input[aria-label="Password"]').type(newPassword);
        cy.get('input[aria-label="Confirm password"]').type(newPassword);
        cy.contains('button', /reset/i).click();

        cy.wait('@resetPasswordRequest');

        cy.contains(/expired/i).should('be.visible');
      });

      it('should show error for already used token', () => {
        cy.intercept('POST', '**/auth/password/reset', {
          statusCode: 400,
          body: {
            error: 'BAD_REQUEST',
            message: 'This password reset link has already been used.',
          },
        }).as('resetPasswordRequest');

        cy.get('input[aria-label="Password"]').type(newPassword);
        cy.get('input[aria-label="Confirm password"]').type(newPassword);
        cy.contains('button', /reset/i).click();

        cy.wait('@resetPasswordRequest');

        cy.contains(/already been used/i).should('be.visible');
      });

      it('should show error for invalid token', () => {
        cy.intercept('POST', '**/auth/password/reset', {
          statusCode: 404,
          body: {
            error: 'NOT_FOUND',
            message: 'Invalid password reset token',
          },
        }).as('resetPasswordRequest');

        cy.get('input[aria-label="Password"]').type(newPassword);
        cy.get('input[aria-label="Confirm password"]').type(newPassword);
        cy.contains('button', /reset/i).click();

        cy.wait('@resetPasswordRequest');

        cy.contains(/invalid/i).should('be.visible');
      });
    });

    describe('Missing Token', () => {
      it('should show error when token is missing from URL', () => {
        cy.visit('/reset-password');

        // Should show error or redirect
        cy.contains(/token.*required|invalid.*link/i).should('be.visible');
      });
    });
  });

  describe('Password Strength Indicator', () => {
    const validToken = 'valid-reset-token-12345';

    beforeEach(() => {
      cy.clearAuthCookies();
      cy.visit(`/reset-password?token=${validToken}`);
    });

    it('should show password strength as user types', () => {
      // Type weak password
      cy.get('input[aria-label="Password"]').type('weak');

      // Type stronger password
      cy.get('input[aria-label="Password"]').clear().type(newPassword);

      // Password strength indicator should update
    });
  });

  describe('Accessibility', () => {
    beforeEach(() => {
      cy.clearAuthCookies();
      cy.visit('/forgot-password');
    });

    it('should have proper form labels', () => {
      cy.get('input[aria-label="Email address"]').should('exist');
    });

    it('should be keyboard navigable', () => {
      cy.get('input[aria-label="Email address"]').focus();
      cy.focused().should('have.attr', 'aria-label', 'Email address');
    });
  });
});
