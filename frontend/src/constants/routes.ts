/**
 * Application route paths
 */
export const ROUTES = {
  // Public routes
  LOGIN: '/login',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',
  RESET_PASSWORD: '/reset-password',
  VERIFY_EMAIL: '/verify-email',

  // Protected routes
  DASHBOARD: '/dashboard',
  SETTINGS: '/settings',
  SESSIONS: '/settings/sessions',

  // Transaction routes
  TRANSACTIONS: '/transactions',
  TRANSACTIONS_NEW: '/transactions/new',
  TRANSACTIONS_EDIT: '/transactions/:id/edit',

  // Root
  HOME: '/',
} as const;

/**
 * Public routes that don't require authentication
 */
export const PUBLIC_ROUTES = [
  ROUTES.LOGIN,
  ROUTES.REGISTER,
  ROUTES.FORGOT_PASSWORD,
  ROUTES.RESET_PASSWORD,
  ROUTES.VERIFY_EMAIL,
] as const;
