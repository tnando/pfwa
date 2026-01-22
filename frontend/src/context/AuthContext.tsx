import React, { createContext, useContext, useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi, getErrorMessage } from '@/api';
import type {
  User,
  LoginRequest,
  RegisterRequest,
  AuthState,
} from '@/types';

/**
 * Auth context type definition
 */
interface AuthContextType extends AuthState {
  login: (data: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  register: (data: RegisterRequest) => Promise<string>;
  refreshAuth: () => Promise<void>;
  clearError: () => void;
  error: string | null;
}

/**
 * Auth context with default values
 */
const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * Local storage key for persisted auth state
 */
const AUTH_STORAGE_KEY = 'pfwa_auth_user';

/**
 * AuthProvider component - wraps app with authentication state
 */
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    // Initialize from localStorage if available
    const stored = localStorage.getItem(AUTH_STORAGE_KEY);
    if (stored) {
      try {
        return JSON.parse(stored) as User;
      } catch {
        localStorage.removeItem(AUTH_STORAGE_KEY);
      }
    }
    return null;
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  /**
   * Clear any authentication error
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  /**
   * Persist user to localStorage
   */
  const persistUser = useCallback((userData: User | null) => {
    if (userData) {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(userData));
    } else {
      localStorage.removeItem(AUTH_STORAGE_KEY);
    }
    setUser(userData);
  }, []);

  /**
   * Handle logout event (from axios interceptor)
   */
  useEffect(() => {
    const handleLogout = () => {
      persistUser(null);
      setError(null);
    };

    window.addEventListener('auth:logout', handleLogout);
    return () => {
      window.removeEventListener('auth:logout', handleLogout);
    };
  }, [persistUser]);

  /**
   * Attempt to refresh authentication on mount
   */
  const refreshAuth = useCallback(async () => {
    try {
      setIsLoading(true);
      await authApi.refreshToken();
      // Token refresh successful, user is still authenticated
      // The stored user data is still valid
    } catch {
      // Token refresh failed, user is not authenticated
      persistUser(null);
    } finally {
      setIsLoading(false);
    }
  }, [persistUser]);

  /**
   * Check authentication status on mount
   */
  useEffect(() => {
    // If we have a stored user, try to refresh the token
    if (user) {
      refreshAuth();
    } else {
      setIsLoading(false);
    }
    // Only run on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /**
   * Login user
   */
  const login = useCallback(
    async (data: LoginRequest) => {
      try {
        setError(null);
        setIsLoading(true);
        const response = await authApi.login(data);
        persistUser(response.user);
        navigate('/dashboard');
      } catch (err) {
        const message = getErrorMessage(err);
        setError(message);
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [navigate, persistUser]
  );

  /**
   * Logout user
   */
  const logout = useCallback(async () => {
    try {
      setIsLoading(true);
      await authApi.logout();
    } catch {
      // Ignore logout errors - we clear state regardless
    } finally {
      persistUser(null);
      setError(null);
      setIsLoading(false);
      navigate('/login');
    }
  }, [navigate, persistUser]);

  /**
   * Register new user
   * Returns user ID on success
   */
  const register = useCallback(async (data: RegisterRequest): Promise<string> => {
    try {
      setError(null);
      setIsLoading(true);
      const response = await authApi.register(data);
      return response.userId;
    } catch (err) {
      const message = getErrorMessage(err);
      setError(message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Memoized context value
   */
  const value = useMemo<AuthContextType>(
    () => ({
      user,
      isAuthenticated: !!user,
      isLoading,
      error,
      login,
      logout,
      register,
      refreshAuth,
      clearError,
    }),
    [user, isLoading, error, login, logout, register, refreshAuth, clearError]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

/**
 * Hook to access auth context
 * Throws error if used outside AuthProvider
 */
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
