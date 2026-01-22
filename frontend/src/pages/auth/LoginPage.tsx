import React, { useEffect } from 'react';
import { Link as RouterLink, useNavigate, useLocation } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Box,
  TextField,
  Button,
  Link,
  Alert,
  FormControlLabel,
  Checkbox,
  CircularProgress,
} from '@mui/material';
import { AuthLayout, PasswordField } from '@/components/auth';
import { useAuth } from '@/context';
import { ROUTES } from '@/constants';
import { loginSchema, type LoginFormData } from '@/utils/validation';
import { getFieldErrors } from '@/api';

/**
 * LoginPage - User authentication page
 * Handles email/password login with "Remember me" option
 */
const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated, isLoading, error, clearError } = useAuth();

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated) {
      const from = (location.state as { from?: Location })?.from?.pathname || ROUTES.DASHBOARD;
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, location]);

  // Clear errors when component unmounts
  useEffect(() => {
    return () => {
      clearError();
    };
  }, [clearError]);

  const {
    control,
    handleSubmit,
    setError: setFieldError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
      rememberMe: false,
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await login({
        email: data.email,
        password: data.password,
        rememberMe: data.rememberMe,
      });
    } catch (err) {
      // Handle field-level errors from API
      const fieldErrors = getFieldErrors(err);
      Object.entries(fieldErrors).forEach(([field, message]) => {
        if (field === 'email' || field === 'password') {
          setFieldError(field, { message });
        }
      });
    }
  };

  return (
    <AuthLayout title="Sign in to your account" subtitle="Welcome back">
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
            <PasswordField
              {...field}
              label="Password"
              fullWidth
              margin="normal"
              autoComplete="current-password"
              error={!!errors.password}
              helperText={errors.password?.message}
              disabled={isSubmitting || isLoading}
              inputProps={{
                'aria-label': 'Password',
              }}
            />
          )}
        />

        {/* Remember Me & Forgot Password */}
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            mt: 1,
            mb: 2,
          }}
        >
          <Controller
            name="rememberMe"
            control={control}
            render={({ field }) => (
              <FormControlLabel
                control={
                  <Checkbox
                    {...field}
                    checked={field.value}
                    color="primary"
                    size="small"
                    disabled={isSubmitting || isLoading}
                  />
                }
                label="Remember me"
                slotProps={{
                  typography: { variant: 'body2' },
                }}
              />
            )}
          />
          <Link
            component={RouterLink}
            to={ROUTES.FORGOT_PASSWORD}
            variant="body2"
            underline="hover"
          >
            Forgot password?
          </Link>
        </Box>

        {/* Submit Button */}
        <Button
          type="submit"
          fullWidth
          variant="contained"
          size="large"
          disabled={isSubmitting || isLoading}
          sx={{ mt: 1, mb: 2 }}
        >
          {isSubmitting || isLoading ? (
            <CircularProgress size={24} color="inherit" />
          ) : (
            'Sign in'
          )}
        </Button>

        {/* Register Link */}
        <Box sx={{ textAlign: 'center' }}>
          <Link
            component={RouterLink}
            to={ROUTES.REGISTER}
            variant="body2"
            underline="hover"
          >
            {"Don't have an account? Sign up"}
          </Link>
        </Box>
      </Box>
    </AuthLayout>
  );
};

export default LoginPage;
