import React, { useState, useEffect, useCallback } from 'react';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';
import {
  Box,
  Button,
  Link,
  Alert,
  CircularProgress,
  AlertTitle,
  TextField,
} from '@mui/material';
import { AuthLayout } from '@/components/auth';
import { authApi, getErrorMessage } from '@/api';
import { ROUTES } from '@/constants';

type VerificationStatus = 'loading' | 'success' | 'error' | 'no-token';

/**
 * VerifyEmailPage - Email verification landing page
 * Auto-verifies on load with token from URL
 * Provides option to resend verification email
 */
const VerifyEmailPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [status, setStatus] = useState<VerificationStatus>('loading');
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [resendEmail, setResendEmail] = useState('');
  const [resendStatus, setResendStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [resendError, setResendError] = useState<string>('');

  /**
   * Verify email with token
   */
  const verifyEmail = useCallback(async (verificationToken: string) => {
    try {
      setStatus('loading');
      await authApi.verifyEmail({ token: verificationToken });
      setStatus('success');
    } catch (err) {
      setStatus('error');
      setErrorMessage(getErrorMessage(err));
    }
  }, []);

  /**
   * Auto-verify on mount if token is present
   */
  useEffect(() => {
    if (token) {
      verifyEmail(token);
    } else {
      setStatus('no-token');
    }
  }, [token, verifyEmail]);

  /**
   * Handle resend verification email
   */
  const handleResendVerification = async () => {
    if (!resendEmail) return;

    try {
      setResendStatus('loading');
      setResendError('');
      await authApi.resendVerification({ email: resendEmail });
      setResendStatus('success');
    } catch (err) {
      setResendStatus('error');
      setResendError(getErrorMessage(err));
    }
  };

  // Loading state
  if (status === 'loading') {
    return (
      <AuthLayout title="Verifying email" subtitle="Please wait...">
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            py: 4,
          }}
        >
          <CircularProgress size={48} />
          <Box sx={{ mt: 2, color: 'text.secondary' }}>
            Verifying your email address...
          </Box>
        </Box>
      </AuthLayout>
    );
  }

  // Success state
  if (status === 'success') {
    return (
      <AuthLayout title="Email verified" subtitle="Your account is now active">
        <Alert severity="success" sx={{ mb: 3 }}>
          <AlertTitle>Verification successful</AlertTitle>
          Your email has been verified. You can now sign in to your account.
        </Alert>

        <Button
          component={RouterLink}
          to={ROUTES.LOGIN}
          fullWidth
          variant="contained"
          size="large"
        >
          Sign in
        </Button>
      </AuthLayout>
    );
  }

  // Error state or no token
  return (
    <AuthLayout
      title={status === 'no-token' ? 'Verification required' : 'Verification failed'}
      subtitle={status === 'no-token' ? 'Enter your email to resend verification' : 'Unable to verify your email'}
    >
      {status === 'error' && (
        <Alert severity="error" sx={{ mb: 3 }}>
          <AlertTitle>Verification failed</AlertTitle>
          {errorMessage}
        </Alert>
      )}

      {status === 'no-token' && (
        <Alert severity="info" sx={{ mb: 3 }}>
          <AlertTitle>No verification token</AlertTitle>
          If you need a new verification email, enter your email address below.
        </Alert>
      )}

      {/* Resend Verification Form */}
      <Box sx={{ mt: 2 }}>
        {resendStatus === 'success' ? (
          <Alert severity="success" sx={{ mb: 2 }}>
            Verification email sent! Please check your inbox.
          </Alert>
        ) : (
          <>
            {resendStatus === 'error' && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {resendError}
              </Alert>
            )}

            <TextField
              label="Email address"
              type="email"
              fullWidth
              margin="normal"
              value={resendEmail}
              onChange={(e) => setResendEmail(e.target.value)}
              disabled={resendStatus === 'loading'}
              inputProps={{
                'aria-label': 'Email address for verification',
              }}
            />

            <Button
              fullWidth
              variant="outlined"
              size="large"
              onClick={handleResendVerification}
              disabled={!resendEmail || resendStatus === 'loading'}
              sx={{ mt: 2 }}
            >
              {resendStatus === 'loading' ? (
                <CircularProgress size={24} color="inherit" />
              ) : (
                'Resend verification email'
              )}
            </Button>
          </>
        )}
      </Box>

      {/* Back to Login Link */}
      <Box sx={{ textAlign: 'center', mt: 3 }}>
        <Link
          component={RouterLink}
          to={ROUTES.LOGIN}
          variant="body2"
          underline="hover"
        >
          Back to sign in
        </Link>
      </Box>
    </AuthLayout>
  );
};

export default VerifyEmailPage;
