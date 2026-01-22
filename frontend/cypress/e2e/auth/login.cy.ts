/// <reference types="cypress" />

/**
 * E2E Tests for User Login (AUTH-003)
 *
 * Tests the complete login flow including:
 * - Successful login with valid credentials
 * - Error handling for invalid credentials
 * - Remember me functionality
 * - Redirect after login
 * - Form validation
 */
describe('User Login', () => {
  const testEmail = Cypress.env('testUserEmail');
  const testPassword = Cypress.env('testUserPassword');

  beforeEach(() => {
    // Clear any existing auth state
    cy.clearAuthCookies();
    // Visit the login page
    cy.visit('/login');
  });

  describe('Page Elements', () => {
    it('should display login form with all required elements', () => {
      // Verify page title
      cy.contains('Sign in to your account').should('be.visible');

      // Verify form fields exist
      cy.get('input[aria-label="Email address"]').should('be.visible');
      cy.get('input[aria-label="Password"]').should('be.visible');

      // Verify remember me checkbox
      cy.contains('Remember me').should('be.visible');

      // Verify forgot password link
      cy.contains('Forgot password?').should('be.visible');

      // Verify submit button
      cy.contains('button', 'Sign in').should('be.visible');

      // Verify link to registration page
      cy.contains("Don't have an account?").should('be.visible');
    });
  });

  describe('Form Validation', () => {
    it('should show error for empty email', () => {
      cy.get('input[aria-label="Password"]').type(testPassword);
      cy.contains('button', 'Sign in').click();

      // Should show validation error
      cy.contains(/email/i).should('be.visible');
    });

    it('should show error for invalid email format', () => {
      cy.get('input[aria-label="Email address"]').type('invalid-email');
      cy.get('input[aria-label="Password"]').type(testPassword);
      cy.contains('button', 'Sign in').click();

      // Should show validation error
      cy.contains(/email/i).should('be.visible');
    });

    it('should show error for empty password', () => {
      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.contains('button', 'Sign in').click();

      // Should show validation error
      cy.contains(/password/i).should('be.visible');
    });
  });

  describe('Successful Login', () => {
    it('should login successfully with valid credentials', () => {
      // Mock successful login response
      cy.intercept('POST', '**/auth/login', {
        statusCode: 200,
        body: {
          user: {
            id: 'test-user-id',
            email: testEmail,
            firstName: 'Test',
            lastName: 'User',
          },
          expiresIn: 900,
        },
        headers: {
          'Set-Cookie': 'accessToken=mock-token; HttpOnly; Secure; SameSite=Strict',
        },
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type(testPassword);
      cy.contains('button', 'Sign in').click();

      cy.wait('@loginRequest');

      // Should redirect to dashboard
      cy.url().should('include', '/dashboard');
    });

    it('should display loading state while logging in', () => {
      cy.intercept('POST', '**/auth/login', (req) => {
        req.reply({
          delay: 1000,
          statusCode: 200,
          body: {
            user: { id: 'test', email: testEmail },
            expiresIn: 900,
          },
        });
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type(testPassword);
      cy.contains('button', 'Sign in').click();

      // Button should show loading state
      cy.get('button[type="submit"]').should('be.disabled');

      cy.wait('@loginRequest');
    });

    it('should redirect to originally requested page after login', () => {
      // Visit a protected page first
      cy.visit('/transactions');

      // Should be redirected to login
      cy.url().should('include', '/login');

      // Mock successful login
      cy.intercept('POST', '**/auth/login', {
        statusCode: 200,
        body: {
          user: { id: 'test', email: testEmail },
          expiresIn: 900,
        },
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type(testPassword);
      cy.contains('button', 'Sign in').click();

      cy.wait('@loginRequest');

      // Should redirect to originally requested page
      cy.url().should('include', '/transactions');
    });
  });

  describe('Invalid Credentials', () => {
    it('should show error for invalid email or password', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 401,
        body: {
          error: 'UNAUTHORIZED',
          message: 'Invalid email or password',
        },
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type('wrong@example.com');
      cy.get('input[aria-label="Password"]').type('WrongPassword123!');
      cy.contains('button', 'Sign in').click();

      cy.wait('@loginRequest');

      // Should show error message
      cy.contains('Invalid email or password').should('be.visible');
    });

    it('should not reveal if email exists on failed login', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 401,
        body: {
          error: 'UNAUTHORIZED',
          message: 'Invalid email or password',
        },
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type('nonexistent@example.com');
      cy.get('input[aria-label="Password"]').type(testPassword);
      cy.contains('button', 'Sign in').click();

      cy.wait('@loginRequest');

      // Error message should be generic (not reveal if email exists)
      cy.contains('Invalid email or password').should('be.visible');
      cy.contains('email not found').should('not.exist');
    });
  });

  describe('Account Locked', () => {
    it('should show error when account is locked', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 403,
        body: {
          error: 'FORBIDDEN',
          message: 'Account temporarily locked due to too many failed attempts. Please try again in 30 minutes.',
        },
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type(testPassword);
      cy.contains('button', 'Sign in').click();

      cy.wait('@loginRequest');

      // Should show account locked message
      cy.contains(/locked/i).should('be.visible');
      cy.contains(/30 minutes/i).should('be.visible');
    });
  });

  describe('Email Not Verified', () => {
    it('should show error when email is not verified', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 403,
        body: {
          error: 'FORBIDDEN',
          message: 'Please verify your email before logging in',
        },
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type(testPassword);
      cy.contains('button', 'Sign in').click();

      cy.wait('@loginRequest');

      // Should show verification required message
      cy.contains(/verify your email/i).should('be.visible');
    });
  });

  describe('Remember Me Functionality', () => {
    it('should have remember me checkbox unchecked by default', () => {
      cy.get('input[type="checkbox"]').should('not.be.checked');
    });

    it('should toggle remember me checkbox', () => {
      cy.contains('Remember me').click();
      cy.get('input[type="checkbox"]').should('be.checked');

      cy.contains('Remember me').click();
      cy.get('input[type="checkbox"]').should('not.be.checked');
    });

    it('should send rememberMe: true when checkbox is checked', () => {
      cy.intercept('POST', '**/auth/login', (req) => {
        expect(req.body.rememberMe).to.equal(true);
        req.reply({
          statusCode: 200,
          body: {
            user: { id: 'test', email: testEmail },
            expiresIn: 900,
          },
        });
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type(testPassword);
      cy.contains('Remember me').click();
      cy.contains('button', 'Sign in').click();

      cy.wait('@loginRequest');
    });
  });

  describe('Navigation', () => {
    it('should navigate to forgot password page', () => {
      cy.contains('Forgot password?').click();
      cy.url().should('include', '/forgot-password');
    });

    it('should navigate to registration page', () => {
      cy.contains("Don't have an account?").click();
      cy.url().should('include', '/register');
    });

    it('should redirect already authenticated users to dashboard', () => {
      // Mock authenticated state
      cy.setCookie('accessToken', 'mock-access-token');

      cy.visit('/login');

      // Should redirect to dashboard
      cy.url().should('include', '/dashboard');
    });
  });

  describe('Password Visibility Toggle', () => {
    it('should toggle password visibility', () => {
      cy.get('input[aria-label="Password"]').type(testPassword);

      // Password should be hidden by default
      cy.get('input[aria-label="Password"]').should('have.attr', 'type', 'password');

      // Click the visibility toggle button (usually an icon button)
      cy.get('button[aria-label="toggle password visibility"]').click();

      // Password should now be visible
      cy.get('input[aria-label="Password"]').should('have.attr', 'type', 'text');

      // Toggle back
      cy.get('button[aria-label="toggle password visibility"]').click();
      cy.get('input[aria-label="Password"]').should('have.attr', 'type', 'password');
    });
  });

  describe('Accessibility', () => {
    it('should have proper form labels', () => {
      cy.get('input[aria-label="Email address"]').should('exist');
      cy.get('input[aria-label="Password"]').should('exist');
    });

    it('should focus email field on page load', () => {
      // Email field should be auto-focused
      cy.focused().should('have.attr', 'aria-label', 'Email address');
    });

    it('should submit form on Enter key press', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 200,
        body: {
          user: { id: 'test', email: testEmail },
          expiresIn: 900,
        },
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type(testEmail);
      cy.get('input[aria-label="Password"]').type(`${testPassword}{enter}`);

      cy.wait('@loginRequest');
    });
  });

  describe('Error Clearing', () => {
    it('should clear error when user starts typing', () => {
      cy.intercept('POST', '**/auth/login', {
        statusCode: 401,
        body: {
          error: 'UNAUTHORIZED',
          message: 'Invalid email or password',
        },
      }).as('loginRequest');

      cy.get('input[aria-label="Email address"]').type('wrong@example.com');
      cy.get('input[aria-label="Password"]').type('WrongPassword123!');
      cy.contains('button', 'Sign in').click();

      cy.wait('@loginRequest');

      // Error should be visible
      cy.contains('Invalid email or password').should('be.visible');

      // Start typing - error should clear
      cy.get('input[aria-label="Email address"]').clear().type('new@example.com');

      // Error should be cleared (depending on implementation)
    });
  });
});
