import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import VerifyEmailPage from './VerifyEmailPage';

const theme = createTheme();

// Mock the API module
vi.mock('@/api', () => ({
  authApi: {
    verifyEmail: vi.fn(),
    resendVerification: vi.fn(),
  },
  getErrorMessage: vi.fn((err) => err?.response?.data?.message || err?.message || 'An error occurred'),
}));

const renderVerifyEmailPage = (token?: string) => {
  const route = token ? `/verify-email?token=${token}` : '/verify-email';
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>
  );
};

describe('VerifyEmailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should show no token message when token is not provided', () => {
    renderVerifyEmailPage();

    expect(screen.getByRole('heading', { name: /verification required/i })).toBeInTheDocument();
    expect(screen.getByText(/no verification token/i)).toBeInTheDocument();
  });

  it('should show loading state while verifying', async () => {
    const { authApi } = await import('@/api');

    // Create a promise that doesn't resolve immediately
    let resolveVerify: (value: unknown) => void;
    const verifyPromise = new Promise((resolve) => {
      resolveVerify = resolve;
    });
    (authApi.verifyEmail as ReturnType<typeof vi.fn>).mockReturnValue(verifyPromise);

    renderVerifyEmailPage('valid-token');

    expect(screen.getByRole('heading', { name: /verifying email/i })).toBeInTheDocument();
    expect(screen.getByText(/verifying your email address/i)).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();

    // Cleanup
    resolveVerify!({ message: 'Success' });
  });

  it('should auto-verify when token is provided', async () => {
    const { authApi } = await import('@/api');

    (authApi.verifyEmail as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Email verified successfully.',
    });

    renderVerifyEmailPage('valid-token');

    await waitFor(() => {
      expect(authApi.verifyEmail).toHaveBeenCalledWith({ token: 'valid-token' });
    });
  });

  it('should show success message on successful verification', async () => {
    const { authApi } = await import('@/api');

    (authApi.verifyEmail as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Email verified successfully.',
    });

    renderVerifyEmailPage('valid-token');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /email verified/i })).toBeInTheDocument();
      expect(screen.getByText('Verification successful')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /sign in/i })).toBeInTheDocument();
    });
  });

  it('should show error message on verification failure', async () => {
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.verifyEmail as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { message: 'Verification link has expired.' } },
    });
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Verification link has expired.');

    renderVerifyEmailPage('expired-token');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /verification failed/i })).toBeInTheDocument();
      expect(screen.getByText('Verification link has expired.')).toBeInTheDocument();
    });
  });

  it('should show resend verification form on error', async () => {
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.verifyEmail as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { message: 'Verification link has expired.' } },
    });
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Verification link has expired.');

    renderVerifyEmailPage('expired-token');

    await waitFor(() => {
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /resend verification email/i })).toBeInTheDocument();
    });
  });

  it('should show resend verification form when no token', () => {
    renderVerifyEmailPage();

    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /resend verification email/i })).toBeInTheDocument();
  });

  it('should call resend verification API', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.resendVerification as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Verification email sent.',
    });

    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/email address/i), 'user@example.com');
    await user.click(screen.getByRole('button', { name: /resend verification email/i }));

    await waitFor(() => {
      expect(authApi.resendVerification).toHaveBeenCalledWith({ email: 'user@example.com' });
    });
  });

  it('should show success message after resending verification', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    (authApi.resendVerification as ReturnType<typeof vi.fn>).mockResolvedValue({
      message: 'Verification email sent.',
    });

    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/email address/i), 'user@example.com');
    await user.click(screen.getByRole('button', { name: /resend verification email/i }));

    await waitFor(() => {
      expect(screen.getByText(/verification email sent/i)).toBeInTheDocument();
    });
  });

  it('should show error message when resend fails', async () => {
    const user = userEvent.setup();
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.resendVerification as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { message: 'Too many requests.' } },
    });
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Too many requests.');

    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/email address/i), 'user@example.com');
    await user.click(screen.getByRole('button', { name: /resend verification email/i }));

    await waitFor(() => {
      expect(screen.getByText('Too many requests.')).toBeInTheDocument();
    });
  });

  it('should disable resend button when email is empty', () => {
    renderVerifyEmailPage();

    expect(screen.getByRole('button', { name: /resend verification email/i })).toBeDisabled();
  });

  it('should enable resend button when email is entered', async () => {
    const user = userEvent.setup();
    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/email address/i), 'user@example.com');

    expect(screen.getByRole('button', { name: /resend verification email/i })).not.toBeDisabled();
  });

  it('should have back to sign in link', () => {
    renderVerifyEmailPage();

    expect(screen.getByRole('link', { name: /back to sign in/i })).toBeInTheDocument();
  });

  it('should disable resend form during submission', async () => {
    const user = userEvent.setup();
    const { authApi } = await import('@/api');

    let resolveRequest: (value: unknown) => void;
    const requestPromise = new Promise((resolve) => {
      resolveRequest = resolve;
    });
    (authApi.resendVerification as ReturnType<typeof vi.fn>).mockReturnValue(requestPromise);

    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/email address/i), 'user@example.com');
    await user.click(screen.getByRole('button', { name: /resend verification email/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/email address/i)).toBeDisabled();
    });

    resolveRequest!({ message: 'Success' });
  });

  it('should handle already verified email error', async () => {
    const { authApi, getErrorMessage } = await import('@/api');

    (authApi.verifyEmail as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { message: 'Email has already been verified.' } },
    });
    (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Email has already been verified.');

    renderVerifyEmailPage('already-verified-token');

    await waitFor(() => {
      expect(screen.getByText('Email has already been verified.')).toBeInTheDocument();
    });
  });
});
