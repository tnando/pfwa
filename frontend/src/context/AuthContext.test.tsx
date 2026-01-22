import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider, useAuth } from './AuthContext';

// Mock the API module
vi.mock('@/api', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    register: vi.fn(),
    refreshToken: vi.fn(),
  },
  getErrorMessage: vi.fn((err) => err?.message || 'An error occurred'),
}));

// Test component that uses the auth context
const TestComponent: React.FC = () => {
  const { user, isAuthenticated, isLoading, error, login, logout, register, clearError } = useAuth();

  const handleLogin = async () => {
    try {
      await login({ email: 'test@example.com', password: 'password' });
    } catch {
      // Error is handled by context
    }
  };

  const handleRegister = async () => {
    try {
      await register({ email: 'new@example.com', password: 'Password123!', confirmPassword: 'Password123!' });
    } catch {
      // Error is handled by context
    }
  };

  return (
    <div>
      <div data-testid="loading">{isLoading ? 'loading' : 'not-loading'}</div>
      <div data-testid="authenticated">{isAuthenticated ? 'authenticated' : 'not-authenticated'}</div>
      <div data-testid="user">{user ? user.email : 'no-user'}</div>
      <div data-testid="error">{error || 'no-error'}</div>
      <button onClick={handleLogin}>
        Login
      </button>
      <button onClick={() => logout()}>Logout</button>
      <button onClick={handleRegister}>
        Register
      </button>
      <button onClick={clearError}>Clear Error</button>
    </div>
  );
};

const renderWithProvider = () => {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    // Reset location mock
    window.location.pathname = '/';
  });

  it('should throw error when useAuth is used outside AuthProvider', () => {
    // Suppress console.error for this test
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => {
      render(
        <MemoryRouter>
          <TestComponent />
        </MemoryRouter>
      );
    }).toThrow('useAuth must be used within an AuthProvider');

    consoleSpy.mockRestore();
  });

  it('should have initial unauthenticated state', async () => {
    const { authApi } = await import('@/api');
    (authApi.refreshToken as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Not authenticated'));

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('not-loading');
    });

    expect(screen.getByTestId('authenticated')).toHaveTextContent('not-authenticated');
    expect(screen.getByTestId('user')).toHaveTextContent('no-user');
    expect(screen.getByTestId('error')).toHaveTextContent('no-error');
  });

  it('should login successfully', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      user: { id: '1', email: 'test@example.com', firstName: 'Test', lastName: 'User' },
      expiresIn: 900,
    });

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('not-loading');
    });

    await user.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('authenticated');
      expect(screen.getByTestId('user')).toHaveTextContent('test@example.com');
    });
  });

  it('should handle login error', async () => {
    const user = userEvent.setup();
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.login as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Invalid credentials'));
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Invalid credentials');

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('not-loading');
    });

    await user.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(screen.getByTestId('error')).toHaveTextContent('Invalid credentials');
      expect(screen.getByTestId('authenticated')).toHaveTextContent('not-authenticated');
    });
  });

  it('should logout successfully', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    // First login
    (authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      user: { id: '1', email: 'test@example.com', firstName: 'Test', lastName: 'User' },
      expiresIn: 900,
    });
    (authApi.logout as ReturnType<typeof vi.fn>).mockResolvedValue({ message: 'Logged out' });

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('not-loading');
    });

    // Login first
    await user.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('authenticated');
    });

    // Then logout
    await user.click(screen.getByRole('button', { name: 'Logout' }));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('not-authenticated');
      expect(screen.getByTestId('user')).toHaveTextContent('no-user');
    });
  });

  it('should register successfully', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.register as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Registration successful',
      userId: '123',
    });

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('not-loading');
    });

    await user.click(screen.getByRole('button', { name: 'Register' }));

    // Registration should call the API but not log the user in
    await waitFor(() => {
      expect(authApi.register).toHaveBeenCalledWith({
        email: 'new@example.com',
        password: 'Password123!',
        confirmPassword: 'Password123!',
      });
    });
  });

  it('should clear error', async () => {
    const user = userEvent.setup();
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.login as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('Invalid credentials'));
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Invalid credentials');

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('not-loading');
    });

    // Trigger error
    await user.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(screen.getByTestId('error')).toHaveTextContent('Invalid credentials');
    });

    // Clear error
    await user.click(screen.getByRole('button', { name: 'Clear Error' }));

    await waitFor(() => {
      expect(screen.getByTestId('error')).toHaveTextContent('no-error');
    });
  });

  it('should persist user to localStorage on login', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    const mockUser = { id: '1', email: 'test@example.com', firstName: 'Test', lastName: 'User' };
    (authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      user: mockUser,
      expiresIn: 900,
    });

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('not-loading');
    });

    await user.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(localStorage.setItem).toHaveBeenCalledWith('pfwa_auth_user', JSON.stringify(mockUser));
    });
  });

  it('should remove user from localStorage on logout', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      user: { id: '1', email: 'test@example.com', firstName: 'Test', lastName: 'User' },
      expiresIn: 900,
    });
    (authApi.logout as ReturnType<typeof vi.fn>).mockResolvedValue({ message: 'Logged out' });

    renderWithProvider();

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('not-loading');
    });

    await user.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('authenticated');
    });

    await user.click(screen.getByRole('button', { name: 'Logout' }));

    await waitFor(() => {
      expect(localStorage.removeItem).toHaveBeenCalledWith('pfwa_auth_user');
    });
  });

  it('should handle auth:logout event', async () => {
    const { authApi } = await import('@/api');

    const mockUser = { id: '1', email: 'test@example.com', firstName: 'Test', lastName: 'User' };
    (authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      user: mockUser,
      expiresIn: 900,
    });

    renderWithProvider();

    // Simulate login by setting initial state
    localStorage.getItem = vi.fn().mockReturnValue(JSON.stringify(mockUser));
    (authApi.refreshToken as ReturnType<typeof vi.fn>).mockResolvedValue({ expiresIn: 900 });

    // Dispatch auth:logout event
    await act(async () => {
      window.dispatchEvent(new CustomEvent('auth:logout'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('not-authenticated');
    });
  });
});
