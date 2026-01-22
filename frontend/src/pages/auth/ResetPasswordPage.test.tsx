import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import ResetPasswordPage from './ResetPasswordPage';

const theme = createTheme();

// Mock the API module
vi.mock('@/api', () => ({
  authApi: {
    resetPassword: vi.fn(),
  },
  getErrorMessage: vi.fn((err) => err?.response?.data?.message || err?.message || 'An error occurred'),
  getFieldErrors: vi.fn(() => ({})),
}));

const renderResetPasswordPage = (token?: string) => {
  const route = token ? `/reset-password?token=${token}` : '/reset-password';
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/login" element={<div>Login Page</div>} />
          <Route path="/forgot-password" element={<div>Forgot Password Page</div>} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>
  );
};

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should show error when no token is provided', () => {
    renderResetPasswordPage();

    expect(screen.getByRole('heading', { name: /invalid link/i })).toBeInTheDocument();
    expect(screen.getByText(/invalid password reset link/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /request a new password reset link/i })).toBeInTheDocument();
  });

  it('should render reset password form when token is provided', () => {
    renderResetPasswordPage('valid-token');

    expect(screen.getByRole('heading', { name: /reset your password/i })).toBeInTheDocument();
    expect(screen.getByLabelText('New password')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirm new password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /reset password/i })).toBeInTheDocument();
  });

  it('should show validation error for weak password', async () => {
    const user = userEvent.setup();
    renderResetPasswordPage('valid-token');

    await user.type(screen.getByLabelText('New password'), 'weak');
    await user.type(screen.getByLabelText('Confirm new password'), 'weak');

    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      // The helper text error message in the form field
      expect(screen.getByText('Password must be at least 8 characters')).toBeInTheDocument();
    });
  });

  it('should show validation error for mismatched passwords', async () => {
    const user = userEvent.setup();
    renderResetPasswordPage('valid-token');

    await user.type(screen.getByLabelText('New password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm new password'), 'DifferentPass123!');

    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    });
  });

  it('should show password strength meter', async () => {
    const user = userEvent.setup();
    renderResetPasswordPage('valid-token');

    await user.type(screen.getByLabelText('New password'), 'SecurePass123!');

    await waitFor(() => {
      expect(screen.getByText('Very Strong')).toBeInTheDocument();
    });
  });

  it('should call reset password API with form data', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.resetPassword as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Password has been reset successfully.',
    });

    renderResetPasswordPage('valid-token');

    await user.type(screen.getByLabelText('New password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm new password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(authApi.resetPassword).toHaveBeenCalledWith({
        token: 'valid-token',
        newPassword: 'SecurePass123!',
        confirmPassword: 'SecurePass123!',
      });
    });
  });

  it('should show success message after reset', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.resetPassword as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Password has been reset successfully.',
    });

    renderResetPasswordPage('valid-token');

    await user.type(screen.getByLabelText('New password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm new password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /password reset/i })).toBeInTheDocument();
      expect(screen.getByText('Password reset successful')).toBeInTheDocument();
    });
  });

  it('should display error for expired token', async () => {
    const user = userEvent.setup();
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.resetPassword as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { message: 'Password reset link has expired. Please request a new one.' } },
    });
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue(
      'Password reset link has expired. Please request a new one.'
    );

    renderResetPasswordPage('expired-token');

    await user.type(screen.getByLabelText('New password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm new password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/expired/i);
    });
  });

  it('should display error for already used token', async () => {
    const user = userEvent.setup();
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.resetPassword as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { message: 'This password reset link has already been used.' } },
    });
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue(
      'This password reset link has already been used.'
    );

    renderResetPasswordPage('used-token');

    await user.type(screen.getByLabelText('New password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm new password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/already been used/i);
    });
  });

  it('should disable confirm password until new password is entered', async () => {
    renderResetPasswordPage('valid-token');

    expect(screen.getByLabelText('Confirm new password')).toBeDisabled();
  });

  it('should enable confirm password after entering new password', async () => {
    const user = userEvent.setup();
    renderResetPasswordPage('valid-token');

    await user.type(screen.getByLabelText('New password'), 'test');

    await waitFor(() => {
      expect(screen.getByLabelText('Confirm new password')).not.toBeDisabled();
    });
  });

  it('should have sign in link on success page', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.resetPassword as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Password has been reset successfully.',
    });

    renderResetPasswordPage('valid-token');

    await user.type(screen.getByLabelText('New password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm new password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /sign in now/i })).toBeInTheDocument();
    });
  });

  it('should have back to sign in link in form view', () => {
    renderResetPasswordPage('valid-token');

    expect(screen.getByRole('link', { name: /back to sign in/i })).toBeInTheDocument();
  });

  it('should disable form during submission', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    let resolveRequest: (value: unknown) => void;
    const requestPromise = new Promise((resolve) => {
      resolveRequest = resolve;
    });
    (authApi.resetPassword as ReturnType<typeof vi.fn>).mockReturnValue(requestPromise);

    renderResetPasswordPage('valid-token');

    await user.type(screen.getByLabelText('New password'), 'SecurePass123!');
    await user.type(screen.getByLabelText('Confirm new password'), 'SecurePass123!');

    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByLabelText('New password')).toBeDisabled();
      expect(screen.getByLabelText('Confirm new password')).toBeDisabled();
    });

    resolveRequest!({ message: 'Success' });
  });
});
