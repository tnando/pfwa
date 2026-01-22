// ***********************************************************
// This support file is processed and loaded automatically
// before your test files.
//
// You can customize this file to include your own support commands
// ***********************************************************

import './commands';

// Prevent TypeScript errors when using custom commands
declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Custom command to login via API
       * @param email - User email
       * @param password - User password
       */
      login(email: string, password: string): Chainable<void>;

      /**
       * Custom command to register a new user via API
       * @param email - User email
       * @param password - User password
       */
      register(email: string, password: string): Chainable<void>;

      /**
       * Custom command to logout
       */
      logout(): Chainable<void>;

      /**
       * Custom command to create a test user with verified email via API
       * Returns the user credentials
       */
      createVerifiedUser(): Chainable<{ email: string; password: string }>;

      /**
       * Custom command to clear all authentication cookies
       */
      clearAuthCookies(): Chainable<void>;

      /**
       * Custom command to check if user is authenticated
       */
      isAuthenticated(): Chainable<boolean>;

      /**
       * Custom command to wait for network idle
       */
      waitForNetworkIdle(timeout?: number): Chainable<void>;

      /**
       * Custom command to get an element by data-testid
       */
      getByTestId(testId: string): Chainable<JQuery<HTMLElement>>;
    }
  }
}

// Prevent uncaught exception from failing tests
Cypress.on('uncaught:exception', (err, runnable) => {
  // returning false here prevents Cypress from failing the test
  // only ignore known React development errors
  if (err.message.includes('ResizeObserver loop limit exceeded')) {
    return false;
  }
  return true;
});

// Log console errors for debugging
Cypress.on('window:before:load', (win) => {
  cy.stub(win.console, 'error').callsFake((msg) => {
    cy.task('log', `Console Error: ${msg}`);
  });
});
