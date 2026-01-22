import React, { useState, useEffect } from 'react';
import { Link as RouterLink, useSearchParams, useNavigate } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Box,
  Button,
  Link,
  Alert,
  CircularProgress,
  AlertTitle,
} from '@mui/material';
import { AuthLayout, PasswordField, PasswordStrengthMeter } from '@/components/auth';
import { authApi, getErrorMessage, getFieldErrors } from '@/api';
import { ROUTES } from '@/constants';
import { resetPasswordSchema, type ResetPasswordFormData } from '@/utils/validation';

/**
 * ResetPasswordPage - Set new password using reset token
 * Validates token from URL and allows setting new password
 */
const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [resetSuccess, setResetSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tokenError, setTokenError] = useState<string | null>(null);

  // Validate token presence
  useEffect(() => {
    if (!token) {
      setTokenError('Invalid password reset link. Please request a new one.');
    }
  }, [token]);

  const {
    control,
    handleSubmit,
    watch,
    setError: setFieldError,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      newPassword: '',
      confirmPassword: '',
    },
  });

  const newPassword = watch('newPassword');

  const onSubmit = async (data: ResetPasswordFormData) => {
    if (!token) return;

    try {
      setError(null);
      await authApi.resetPassword({
        token,
        newPassword: data.newPassword,
        confirmPassword: data.confirmPassword,
      });
      setResetSuccess(true);
      // Redirect to login after 3 seconds
      setTimeout(() => {
        navigate(ROUTES.LOGIN, { replace: true });
      }, 3000);
    } catch (err) {
      const message = getErrorMessage(err);
      setError(message);

      // Handle field-level errors
      const fieldErrors = getFieldErrors(err);
      Object.entries(fieldErrors).forEach(([field, errorMsg]) => {
        if (field === 'newPassword' || field === 'confirmPassword') {
          setFieldError(field, { message: errorMsg });
        }
      });
    }
  };

  // Show token error
  if (tokenError) {
    return (
      <AuthLayout title="Invalid link" subtitle="Unable to reset password">
        <Alert severity="error" sx={{ mb: 3 }}>
          <AlertTitle>Invalid reset link</AlertTitle>
          {tokenError}
        </Alert>

        <Box sx={{ textAlign: 'center' }}>
          <Link
            component={RouterLink}
            to={ROUTES.FORGOT_PASSWORD}
            variant="body2"
            underline="hover"
          >
            Request a new password reset link
          </Link>
        </Box>
      </AuthLayout>
    );
  }

  // Show success message
  if (resetSuccess) {
    return (
      <AuthLayout title="Password reset" subtitle="Your password has been updated">
        <Alert severity="success" sx={{ mb: 3 }}>
          <AlertTitle>Password reset successful</AlertTitle>
          Your password has been updated. You will be redirected to the login page
          shortly.
        </Alert>

        <Box sx={{ textAlign: 'center' }}>
          <Link
            component={RouterLink}
            to={ROUTES.LOGIN}
            variant="body2"
            underline="hover"
          >
            Sign in now
          </Link>
        </Box>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Reset your password" subtitle="Enter your new password">
      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {/* Error Alert */}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {/* New Password Field */}
        <Controller
          name="newPassword"
          control={control}
          render={({ field }) => (
            <>
              <PasswordField
                {...field}
                label="New password"
                fullWidth
                margin="normal"
                autoComplete="new-password"
                autoFocus
                error={!!errors.newPassword}
                helperText={errors.newPassword?.message}
                disabled={isSubmitting}
                inputProps={{
                  'aria-label': 'New password',
                }}
              />
              <PasswordStrengthMeter password={field.value} />
            </>
          )}
        />

        {/* Confirm Password Field */}
        <Controller
          name="confirmPassword"
          control={control}
          render={({ field }) => (
            <PasswordField
              {...field}
              label="Confirm new password"
              fullWidth
              margin="normal"
              autoComplete="new-password"
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword?.message}
              disabled={isSubmitting || !newPassword}
              inputProps={{
                'aria-label': 'Confirm new password',
              }}
            />
          )}
        />

        {/* Submit Button */}
        <Button
          type="submit"
          fullWidth
          variant="contained"
          size="large"
          disabled={isSubmitting}
          sx={{ mt: 3, mb: 2 }}
        >
          {isSubmitting ? (
            <CircularProgress size={24} color="inherit" />
          ) : (
            'Reset password'
          )}
        </Button>

        {/* Back to Login Link */}
        <Box sx={{ textAlign: 'center' }}>
          <Link
            component={RouterLink}
            to={ROUTES.LOGIN}
            variant="body2"
            underline="hover"
          >
            Back to sign in
          </Link>
        </Box>
      </Box>
    </AuthLayout>
  );
};

export default ResetPasswordPage;
