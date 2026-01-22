import React, { useState, useEffect } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
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
import { AuthLayout, PasswordField, PasswordStrengthMeter } from '@/components/auth';
import { useAuth } from '@/context';
import { ROUTES } from '@/constants';
import { registerSchema, type RegisterFormData } from '@/utils/validation';
import { getFieldErrors } from '@/api';

/**
 * RegisterPage - User registration page
 * Handles new user registration with email verification flow
 */
const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { register: registerUser, isAuthenticated, isLoading, error, clearError } = useAuth();
  const [registrationSuccess, setRegistrationSuccess] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState('');

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated) {
      navigate(ROUTES.DASHBOARD, { replace: true });
    }
  }, [isAuthenticated, navigate]);

  // Clear errors when component unmounts
  useEffect(() => {
    return () => {
      clearError();
    };
  }, [clearError]);

  const {
    control,
    handleSubmit,
    watch,
    setError: setFieldError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      email: '',
      password: '',
      confirmPassword: '',
    },
  });

  const password = watch('password');

  const onSubmit = async (data: RegisterFormData) => {
    try {
      await registerUser({
        email: data.email,
        password: data.password,
        confirmPassword: data.confirmPassword,
      });
      setRegisteredEmail(data.email);
      setRegistrationSuccess(true);
    } catch (err) {
      // Handle field-level errors from API
      const fieldErrors = getFieldErrors(err);
      Object.entries(fieldErrors).forEach(([field, message]) => {
        if (field === 'email' || field === 'password' || field === 'confirmPassword') {
          setFieldError(field, { message });
        }
      });
    }
  };

  // Show success message after registration
  if (registrationSuccess) {
    return (
      <AuthLayout title="Check your email" subtitle="Verification required">
        <Alert severity="success" sx={{ mb: 3 }}>
          <AlertTitle>Registration successful</AlertTitle>
          We have sent a verification link to <strong>{registeredEmail}</strong>.
          Please check your inbox and click the link to verify your account.
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
    <AuthLayout title="Create your account" subtitle="Start managing your finances">
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
              disabled={isSubmitting || isLoading}
              inputProps={{
                'aria-label': 'Email address',
              }}
            />
          )}
        />

        {/* Password Field */}
        <Controller
          name="password"
          control={control}
          render={({ field }) => (
            <>
              <PasswordField
                {...field}
                label="Password"
                fullWidth
                margin="normal"
                autoComplete="new-password"
                error={!!errors.password}
                helperText={errors.password?.message}
                disabled={isSubmitting || isLoading}
                inputProps={{
                  'aria-label': 'Password',
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
              label="Confirm password"
              fullWidth
              margin="normal"
              autoComplete="new-password"
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword?.message}
              disabled={isSubmitting || isLoading || !password}
              inputProps={{
                'aria-label': 'Confirm password',
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
          disabled={isSubmitting || isLoading}
          sx={{ mt: 3, mb: 2 }}
        >
          {isSubmitting || isLoading ? (
            <CircularProgress size={24} color="inherit" />
          ) : (
            'Create account'
          )}
        </Button>

        {/* Login Link */}
        <Box sx={{ textAlign: 'center' }}>
          <Link
            component={RouterLink}
            to={ROUTES.LOGIN}
            variant="body2"
            underline="hover"
          >
            Already have an account? Sign in
          </Link>
        </Box>
      </Box>
    </AuthLayout>
  );
};

export default RegisterPage;
