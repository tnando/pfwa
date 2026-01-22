import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { ApiErrorResponse } from '@/types';

/**
 * Extended Axios request config with retry flag
 */
interface ExtendedAxiosRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

/**
 * Get API base URL - uses VITE_API_BASE_URL if set, otherwise falls back to relative path
 * For Docker: set VITE_API_BASE_URL=http://localhost:8080/api/v1
 */
const getBaseUrl = (): string => {
  // Check for Vite environment variable (available in browser)
  if (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }
  // Default to relative path (works with Vite proxy or same-origin deployment)
  return '/api/v1';
};

/**
 * Base Axios instance with default configuration
 */
const api = axios.create({
  baseURL: getBaseUrl(),
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Flag to prevent multiple simultaneous refresh attempts
 */
let isRefreshing = false;

/**
 * Queue of requests that failed due to 401 and are waiting for token refresh
 */
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason: unknown) => void;
}> = [];

/**
 * Process the queue of failed requests after token refresh
 */
const processQueue = (error: Error | null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else {
      promise.resolve(undefined);
    }
  });
  failedQueue = [];
};

/**
 * Response interceptor to handle 401 errors and auto-refresh tokens
 */
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const originalRequest = error.config as ExtendedAxiosRequestConfig;

    // If no config or request was already retried, reject
    if (!originalRequest) {
      return Promise.reject(error);
    }

    // Only attempt refresh for 401 errors on non-auth endpoints
    const isAuthEndpoint =
      originalRequest.url?.includes('/auth/login') ||
      originalRequest.url?.includes('/auth/register') ||
      originalRequest.url?.includes('/auth/refresh');

    if (error.response?.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      if (isRefreshing) {
        // If already refreshing, queue this request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await api.post('/auth/refresh');
        processQueue(null);
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as Error);

        // Redirect to login on refresh failure
        // Clear any stored auth state
        window.dispatchEvent(new CustomEvent('auth:logout'));

        // Redirect to login page
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }

        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

/**
 * Helper to extract error message from API error response
 */
export const getErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const apiError = error.response?.data as ApiErrorResponse | undefined;
    if (apiError?.message) {
      return apiError.message;
    }
    if (error.response?.status === 429) {
      return 'Too many requests. Please try again later.';
    }
    if (error.response?.status === 500) {
      return 'An unexpected error occurred. Please try again later.';
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred';
};

/**
 * Helper to extract field errors from API error response
 */
export const getFieldErrors = (
  error: unknown
): Record<string, string> => {
  if (axios.isAxiosError(error)) {
    const apiError = error.response?.data as ApiErrorResponse | undefined;
    if (apiError?.fieldErrors) {
      return apiError.fieldErrors.reduce(
        (acc, fieldError) => ({
          ...acc,
          [fieldError.field]: fieldError.message,
        }),
        {}
      );
    }
  }
  return {};
};

export default api;
