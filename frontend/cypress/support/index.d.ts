/// <reference types="cypress" />

declare namespace Cypress {
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

    /**
     * Custom command to press Tab key from currently focused element
     */
    tab(): Chainable<JQuery<HTMLElement>>;
  }
}
