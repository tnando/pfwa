import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import LoginPage from './LoginPage';
import { AuthProvider } from '@/context';

const theme = createTheme();

// Mock the API module
vi.mock('@/api', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    register: vi.fn(),
    refreshToken: vi.fn(),
  },
  getErrorMessage: vi.fn((err) => err?.response?.data?.message || err?.message || 'An error occurred'),
  getFieldErrors: vi.fn(() => ({})),
}));

const renderLoginPage = (initialRoute = '/login') => {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[initialRoute]}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/dashboard" element={<div>Dashboard</div>} />
            <Route path="/register" element={<div>Register Page</div>} />
            <Route path="/forgot-password" element={<div>Forgot Password Page</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </ThemeProvider>
  );
};

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should render login form', async () => {
    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Sign in to your account' })).toBeInTheDocument();
    });

    expect(screen.getByLabelText('Email address')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: /remember me/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('should have link to register page', async () => {
    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /sign up/i })).toBeInTheDocument();
    });
  });

  it('should have link to forgot password page', async () => {
    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /forgot password/i })).toBeInTheDocument();
    });
  });

  it('should show validation error for empty email', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    });

    // Type password only
    await user.type(screen.getByLabelText('Password'), 'password123');

    // Submit form
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(screen.getByText('Email is required')).toBeInTheDocument();
    });
  });

  it('should show validation error for invalid email format', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'invalid-email');
    await user.type(screen.getByLabelText('Password'), 'password123');

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(screen.getByText('Invalid email format')).toBeInTheDocument();
    });
  });

  it('should show validation error for empty password', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(screen.getByText('Password is required')).toBeInTheDocument();
    });
  });

  it('should call login API with form data', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      user: { id: '1', email: 'user@example.com', firstName: 'Test', lastName: 'User' },
      expiresIn: 900,
    });

    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getByRole('checkbox', { name: /remember me/i }));

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        email: 'user@example.com',
        password: 'password123',
        rememberMe: true,
      });
    });
  });

  it('should redirect to dashboard on successful login', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      user: { id: '1', email: 'user@example.com', firstName: 'Test', lastName: 'User' },
      expiresIn: 900,
    });

    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(screen.getByText('Dashboard')).toBeInTheDocument();
    });
  });

  it('should display API error message on login failure', async () => {
    const user = userEvent.setup();
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.login as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { message: 'Invalid email or password' } },
    });
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Invalid email or password');

    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'wrongpassword');

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Invalid email or password');
    });
  });

  it('should disable form during submission', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    // Create a promise that won't resolve immediately
    let resolveLogin: (value: unknown) => void;
    const loginPromise = new Promise((resolve) => {
      resolveLogin = resolve;
    });
    (authApi.login as ReturnType<typeof vi.fn>).mockReturnValue(loginPromise);

    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    // Form should be disabled during submission
    await waitFor(() => {
      expect(screen.getByLabelText('Email address')).toBeDisabled();
      expect(screen.getByLabelText('Password')).toBeDisabled();
    });

    // Resolve the login
    resolveLogin!({
      user: { id: '1', email: 'user@example.com', firstName: 'Test', lastName: 'User' },
      expiresIn: 900,
    });
  });

  it('should transform email to lowercase', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      user: { id: '1', email: 'user@example.com', firstName: 'Test', lastName: 'User' },
      expiresIn: 900,
    });

    renderLoginPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'USER@EXAMPLE.COM');
    await user.type(screen.getByLabelText('Password'), 'password123');

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(authApi.login).toHaveBeenCalledWith({
        email: 'user@example.com',
        password: 'password123',
        rememberMe: false,
      });
    });
  });
});
