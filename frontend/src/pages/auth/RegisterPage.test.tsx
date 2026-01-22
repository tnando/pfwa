import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import RegisterPage from './RegisterPage';
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

const renderRegisterPage = (initialRoute = '/register') => {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[initialRoute]}>
        <AuthProvider>
          <Routes>
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/login" element={<div>Login Page</div>} />
            <Route path="/dashboard" element={<div>Dashboard</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </ThemeProvider>
  );
};

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should render registration form', async () => {
    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Create your account' })).toBeInTheDocument();
    });

    expect(screen.getByLabelText('Email address')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirm password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
  });

  it('should have link to login page', async () => {
    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /sign in/i })).toBeInTheDocument();
    });
  });

  it('should show validation error for invalid email', async () => {
    const user = userEvent.setup();
    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'invalid-email');
    await user.type(screen.getByLabelText('Password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      expect(screen.getByText('Invalid email format')).toBeInTheDocument();
    });
  });

  it('should show validation error for weak password', async () => {
    const user = userEvent.setup();
    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'weak');
    await user.type(screen.getByLabelText('Confirm password'), 'weak');

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      // The helper text error message in the form field
      expect(screen.getByText('Password must be at least 8 characters')).toBeInTheDocument();
    });
  });

  it('should show validation error for mismatched passwords', async () => {
    const user = userEvent.setup();
    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm password'), 'DifferentPass123!');

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    });
  });

  it('should show password strength meter', async () => {
    const user = userEvent.setup();
    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByLabelText('Password')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Password'), 'SecurePass123!');

    await waitFor(() => {
      expect(screen.getByText('Very Strong')).toBeInTheDocument();
    });
  });

  it('should call register API with form data', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.register as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Registration successful',
      userId: '123',
    });

    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      expect(authApi.register).toHaveBeenCalledWith({
        email: 'user@example.com',
        password: 'SecurePass123!',
        confirmPassword: 'SecurePass123!',
      });
    });
  });

  it('should show success message after registration', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.register as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Registration successful',
      userId: '123',
    });

    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      expect(screen.getByText('Registration successful')).toBeInTheDocument();
      expect(screen.getByText(/user@example.com/)).toBeInTheDocument();
      expect(screen.getByText(/verification link/i)).toBeInTheDocument();
    });
  });

  it('should display API error message on registration failure', async () => {
    const user = userEvent.setup();
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.register as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { message: 'Email already exists' } },
    });
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Email already exists');

    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'existing@example.com');
    await user.type(screen.getByLabelText('Password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Email already exists');
    });
  });

  it('should disable confirm password until password is entered', async () => {
    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByLabelText('Confirm password')).toBeDisabled();
    });
  });

  it('should enable confirm password after entering password', async () => {
    const user = userEvent.setup();
    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByLabelText('Password')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Password'), 'test');

    await waitFor(() => {
      expect(screen.getByLabelText('Confirm password')).not.toBeDisabled();
    });
  });

  it('should disable form during submission', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    // Create a promise that won't resolve immediately
    let resolveRegister: (value: unknown) => void;
    const registerPromise = new Promise((resolve) => {
      resolveRegister = resolve;
    });
    (authApi.register as ReturnType<typeof vi.fn>).mockReturnValue(registerPromise);

    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    // Form should be disabled during submission
    await waitFor(() => {
      expect(screen.getByLabelText('Email address')).toBeDisabled();
      expect(screen.getByLabelText('Password')).toBeDisabled();
      expect(screen.getByLabelText('Confirm password')).toBeDisabled();
    });

    // Resolve the registration
    resolveRegister!({
      message: 'Registration successful',
      userId: '123',
    });
  });

  it('should show back to sign in link on success', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.register as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Registration successful',
      userId: '123',
    });

    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /back to sign in/i })).toBeInTheDocument();
    });
  });

  it('should validate password requirements individually', async () => {
    const user = userEvent.setup();
    renderRegisterPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Create account' })).toBeInTheDocument();
    });

    // Password without uppercase
    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.type(screen.getByLabelText('Password'), 'securepass123!');
    await user.type(screen.getByLabelText('Confirm password'), 'securepass123!');

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      // The helper text error message in the form field
      expect(screen.getByText('Password must contain at least one uppercase letter')).toBeInTheDocument();
    });
  });
});
