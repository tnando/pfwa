import api from './axiosConfig';
import type {
  RegisterRequest,
  RegisterResponse,
  LoginRequest,
  LoginResponse,
  TokenRefreshResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  VerifyEmailRequest,
  ResendVerificationRequest,
  MessageResponse,
  SessionListResponse,
} from '@/types';

/**
 * Authentication API service
 */
export const authApi = {
  /**
   * Register a new user
   */
  register: async (data: RegisterRequest): Promise<RegisterResponse> => {
    const response = await api.post<RegisterResponse>('/auth/register', data);
    return response.data;
  },

  /**
   * Login user with email and password
   */
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/auth/login', data);
    return response.data;
  },

  /**
   * Logout current user (invalidates current session)
   */
  logout: async (): Promise<MessageResponse> => {
    const response = await api.post<MessageResponse>('/auth/logout');
    return response.data;
  },

  /**
   * Refresh access token using refresh token cookie
   */
  refreshToken: async (): Promise<TokenRefreshResponse> => {
    const response = await api.post<TokenRefreshResponse>('/auth/refresh');
    return response.data;
  },

  /**
   * Request password reset email
   */
  forgotPassword: async (data: ForgotPasswordRequest): Promise<MessageResponse> => {
    const response = await api.post<MessageResponse>('/auth/password/forgot', data);
    return response.data;
  },

  /**
   * Complete password reset with token
   */
  resetPassword: async (data: ResetPasswordRequest): Promise<MessageResponse> => {
    const response = await api.post<MessageResponse>('/auth/password/reset', data);
    return response.data;
  },

  /**
   * Verify email address with token
   */
  verifyEmail: async (data: VerifyEmailRequest): Promise<MessageResponse> => {
    const response = await api.post<MessageResponse>('/auth/verify-email', data);
    return response.data;
  },

  /**
   * Resend email verification
   */
  resendVerification: async (data: ResendVerificationRequest): Promise<MessageResponse> => {
    const response = await api.post<MessageResponse>('/auth/verify-email/resend', data);
    return response.data;
  },

  /**
   * Get list of active sessions
   */
  getSessions: async (): Promise<SessionListResponse> => {
    const response = await api.get<SessionListResponse>('/auth/sessions');
    return response.data;
  },

  /**
   * Revoke a specific session
   */
  revokeSession: async (sessionId: string): Promise<MessageResponse> => {
    const response = await api.delete<MessageResponse>(`/auth/sessions/${sessionId}`);
    return response.data;
  },

  /**
   * Logout all sessions (all devices)
   */
  revokeAllSessions: async (): Promise<MessageResponse> => {
    const response = await api.delete<MessageResponse>('/auth/sessions');
    return response.data;
  },
};

export default authApi;
