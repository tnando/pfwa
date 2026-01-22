import React, { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Box,
  TextField,
  Button,
  Link,
  Alert,
  CircularProgress,
  AlertTitle,
} from '@mui/material';
import { AuthLayout } from '@/components/auth';
import { authApi, getErrorMessage } from '@/api';
import { ROUTES } from '@/constants';
import { forgotPasswordSchema, type ForgotPasswordFormData } from '@/utils/validation';

/**
 * ForgotPasswordPage - Request password reset link
 * Shows generic success message regardless of email existence (security)
 */
const ForgotPasswordPage: React.FC = () => {
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [submittedEmail, setSubmittedEmail] = useState('');
  const [error, setError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: '',
    },
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    try {
      setError(null);
      await authApi.forgotPassword({ email: data.email });
      setSubmittedEmail(data.email);
      setSubmitSuccess(true);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  // Show success message after submission
  if (submitSuccess) {
    return (
      <AuthLayout title="Check your email" subtitle="Password reset instructions sent">
        <Alert severity="info" sx={{ mb: 3 }}>
          <AlertTitle>Request received</AlertTitle>
          If <strong>{submittedEmail}</strong> is registered with us, you will receive
          a password reset link shortly. The link expires in 1 hour.
        </Alert>

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
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      title="Forgot your password?"
      subtitle="Enter your email to receive reset instructions"
    >
      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {/* Error Alert */}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {/* Email Field */}
        <Controller
          name="email"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Email address"
              type="email"
              fullWidth
              margin="normal"
              autoComplete="email"
              autoFocus
              error={!!errors.email}
              helperText={errors.email?.message}
              disabled={isSubmitting}
              inputProps={{
                'aria-label': 'Email address',
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
            'Send reset link'
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

export default ForgotPasswordPage;
