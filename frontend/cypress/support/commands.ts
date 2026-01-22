// ***********************************************
// Custom commands for authentication E2E tests
// ***********************************************

const apiUrl = Cypress.env('apiUrl') || 'http://localhost:8080/api/v1';

/**
 * Login via API and store tokens
 */
Cypress.Commands.add('login', (email: string, password: string) => {
  cy.request({
    method: 'POST',
    url: `${apiUrl}/auth/login`,
    body: {
      email,
      password,
      rememberMe: false,
    },
    failOnStatusCode: false,
  }).then((response) => {
    if (response.status === 200) {
      // Tokens are set via cookies automatically
      cy.log('Login successful');
    } else {
      cy.log(`Login failed with status ${response.status}`);
    }
  });
});

/**
 * Register a new user via API
 */
Cypress.Commands.add('register', (email: string, password: string) => {
  cy.request({
    method: 'POST',
    url: `${apiUrl}/auth/register`,
    body: {
      email,
      password,
      confirmPassword: password,
    },
    failOnStatusCode: false,
  }).then((response) => {
    if (response.status === 201) {
      cy.log('Registration successful');
    } else {
      cy.log(`Registration failed with status ${response.status}`);
    }
  });
});

/**
 * Logout via API
 */
Cypress.Commands.add('logout', () => {
  cy.request({
    method: 'POST',
    url: `${apiUrl}/auth/logout`,
    failOnStatusCode: false,
  }).then(() => {
    cy.clearAuthCookies();
  });
});

/**
 * Create a verified test user
 * Note: In a real environment, this would use a test API endpoint
 * For now, it returns mock credentials
 */
Cypress.Commands.add('createVerifiedUser', () => {
  const timestamp = Date.now();
  const email = `testuser_${timestamp}@example.com`;
  const password = 'SecurePass123!';

  // Register the user
  cy.register(email, password);

  // In a real test environment, you would:
  // 1. Either use a test API to auto-verify the email
  // 2. Or mock the email verification step
  // For E2E tests, we assume a test endpoint or database seeding

  return cy.wrap({ email, password });
});

/**
 * Clear all authentication cookies
 */
Cypress.Commands.add('clearAuthCookies', () => {
  cy.clearCookie('accessToken');
  cy.clearCookie('refreshToken');
  cy.clearAllLocalStorage();
  cy.clearAllSessionStorage();
});

/**
 * Check if user is authenticated by checking for access token cookie
 */
Cypress.Commands.add('isAuthenticated', () => {
  cy.getCookie('accessToken').then((cookie) => {
    return cy.wrap(cookie !== null);
  });
});

/**
 * Wait for network to be idle
 */
Cypress.Commands.add('waitForNetworkIdle', (timeout = 5000) => {
  cy.intercept('**/*').as('networkRequest');
  cy.wait(timeout);
});

/**
 * Get element by data-testid attribute
 */
Cypress.Commands.add('getByTestId', (testId: string) => {
  return cy.get(`[data-testid="${testId}"]`);
});

/**
 * Custom tab command to simulate pressing Tab key
 */
Cypress.Commands.add('tab', { prevSubject: 'element' }, (subject) => {
  cy.wrap(subject).trigger('keydown', { key: 'Tab', keyCode: 9, which: 9 });
  return cy.focused();
});

// Export empty object to make this a module
export {};
