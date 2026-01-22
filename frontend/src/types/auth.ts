/**
 * User profile data returned from the API
 */
export interface User {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
}

/**
 * Registration request payload
 */
export interface RegisterRequest {
  email: string;
  password: string;
  confirmPassword: string;
}

/**
 * Registration response from the API
 */
export interface RegisterResponse {
  message: string;
  userId: string;
}

/**
 * Login request payload
 */
export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

/**
 * Login response from the API
 */
export interface LoginResponse {
  user: User;
  expiresIn: number;
}

/**
 * Token refresh response from the API
 */
export interface TokenRefreshResponse {
  expiresIn: number;
}

/**
 * Forgot password request payload
 */
export interface ForgotPasswordRequest {
  email: string;
}

/**
 * Reset password request payload
 */
export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * Email verification request payload
 */
export interface VerifyEmailRequest {
  token: string;
}

/**
 * Resend verification email request payload
 */
export interface ResendVerificationRequest {
  email: string;
}

/**
 * Generic message response from the API
 */
export interface MessageResponse {
  message: string;
}

/**
 * Active session data
 */
export interface Session {
  id: string;
  deviceType: string;
  location: string | null;
  ipAddress: string;
  lastActive: string;
  createdAt: string;
  isCurrent: boolean;
}

/**
 * Session list response from the API
 */
export interface SessionListResponse {
  sessions: Session[];
}

/**
 * Field-level validation error
 */
export interface FieldError {
  field: string;
  message: string;
  code: string;
  rejectedValue?: string | null;
}

/**
 * API error response
 */
export interface ApiErrorResponse {
  error: string;
  message: string;
  timestamp: string;
  path: string;
  requestId?: string;
  fieldErrors?: FieldError[];
}

/**
 * Auth context state
 */
export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
