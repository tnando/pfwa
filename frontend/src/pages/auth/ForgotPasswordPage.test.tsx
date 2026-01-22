import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import ForgotPasswordPage from './ForgotPasswordPage';

const theme = createTheme();

// Mock the API module
vi.mock('@/api', () => ({
  authApi: {
    forgotPassword: vi.fn(),
  },
  getErrorMessage: vi.fn((err) => err?.response?.data?.message || err?.message || 'An error occurred'),
}));

const renderForgotPasswordPage = () => {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={['/forgot-password']}>
        <Routes>
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>
  );
};

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render forgot password form', () => {
    renderForgotPasswordPage();

    expect(screen.getByRole('heading', { name: /forgot your password/i })).toBeInTheDocument();
    expect(screen.getByLabelText('Email address')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send reset link/i })).toBeInTheDocument();
  });

  it('should have link back to login', () => {
    renderForgotPasswordPage();

    expect(screen.getByRole('link', { name: /back to sign in/i })).toBeInTheDocument();
  });

  it('should show validation error for invalid email', async () => {
    const user = userEvent.setup();
    renderForgotPasswordPage();

    await user.type(screen.getByLabelText('Email address'), 'invalid-email');
    await user.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(screen.getByText('Invalid email format')).toBeInTheDocument();
    });
  });

  it('should show validation error for empty email', async () => {
    const user = userEvent.setup();
    renderForgotPasswordPage();

    await user.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(screen.getByText('Email is required')).toBeInTheDocument();
    });
  });

  it('should call forgot password API with email', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.forgotPassword as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'If the email exists, you will receive a password reset link.',
    });

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(authApi.forgotPassword).toHaveBeenCalledWith({
        email: 'user@example.com',
      });
    });
  });

  it('should show success message after submission', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.forgotPassword as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'If the email exists, you will receive a password reset link.',
    });

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(screen.getByText('Request received')).toBeInTheDocument();
      expect(screen.getByText(/user@example.com/)).toBeInTheDocument();
      expect(screen.getByText(/expires in 1 hour/i)).toBeInTheDocument();
    });
  });

  it('should show generic success message even for unknown email (security)', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    // The API should return success even for non-existent emails
    (authApi.forgotPassword as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'If the email exists, you will receive a password reset link.',
    });

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText('Email address'), 'unknown@example.com');
    await user.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(screen.getByText('Request received')).toBeInTheDocument();
    });
  });

  it('should display error message on API failure', async () => {
    const user = userEvent.setup();
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.forgotPassword as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { message: 'Too many requests' } },
    });
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Too many requests');

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Too many requests');
    });
  });

  it('should disable form during submission', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    let resolveRequest: (value: unknown) => void;
    const requestPromise = new Promise((resolve) => {
      resolveRequest = resolve;
    });
    (authApi.forgotPassword as ReturnType<typeof vi.fn>).mockReturnValue(requestPromise);

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(screen.getByLabelText('Email address')).toBeDisabled();
    });

    resolveRequest!({ message: 'Success' });
  });

  it('should have back to sign in link in success view', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.forgotPassword as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'If the email exists, you will receive a password reset link.',
    });

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText('Email address'), 'user@example.com');
    await user.click(screen.getByRole('button', { name: /send reset link/i }));

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /back to sign in/i })).toBeInTheDocument();
    });
  });
});
