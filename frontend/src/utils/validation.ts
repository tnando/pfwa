import { z } from 'zod';
import {
  PASSWORD_MIN_LENGTH,
  PASSWORD_MAX_LENGTH,
  PASSWORD_PATTERNS,
  EMAIL_MAX_LENGTH,
} from '@/constants/validation';

/**
 * Email validation schema
 */
export const emailSchema = z
  .string()
  .min(1, 'Email is required')
  .max(EMAIL_MAX_LENGTH, `Email must be at most ${EMAIL_MAX_LENGTH} characters`)
  .email('Invalid email format')
  .transform((email) => email.trim().toLowerCase());

/**
 * Password validation schema with security requirements
 */
export const passwordSchema = z
  .string()
  .min(PASSWORD_MIN_LENGTH, `Password must be at least ${PASSWORD_MIN_LENGTH} characters`)
  .max(PASSWORD_MAX_LENGTH, `Password must be at most ${PASSWORD_MAX_LENGTH} characters`)
  .refine(
    (password) => PASSWORD_PATTERNS.uppercase.test(password),
    'Password must contain at least one uppercase letter'
  )
  .refine(
    (password) => PASSWORD_PATTERNS.lowercase.test(password),
    'Password must contain at least one lowercase letter'
  )
  .refine(
    (password) => PASSWORD_PATTERNS.digit.test(password),
    'Password must contain at least one number'
  )
  .refine(
    (password) => PASSWORD_PATTERNS.special.test(password),
    'Password must contain at least one special character'
  );

/**
 * Login form validation schema
 */
export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Password is required'),
  rememberMe: z.boolean().optional().default(false),
});

export type LoginFormData = z.infer<typeof loginSchema>;

/**
 * Registration form validation schema
 */
export const registerSchema = z
  .object({
    email: emailSchema,
    password: passwordSchema,
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

export type RegisterFormData = z.infer<typeof registerSchema>;

/**
 * Forgot password form validation schema
 */
export const forgotPasswordSchema = z.object({
  email: emailSchema,
});

export type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;

/**
 * Reset password form validation schema
 */
export const resetPasswordSchema = z
  .object({
    newPassword: passwordSchema,
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

export type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

/**
 * Calculate password strength (0-5 based on requirements met)
 */
export const calculatePasswordStrength = (password: string): number => {
  if (!password) return 0;

  let strength = 0;

  if (password.length >= PASSWORD_MIN_LENGTH) strength++;
  if (PASSWORD_PATTERNS.uppercase.test(password)) strength++;
  if (PASSWORD_PATTERNS.lowercase.test(password)) strength++;
  if (PASSWORD_PATTERNS.digit.test(password)) strength++;
  if (PASSWORD_PATTERNS.special.test(password)) strength++;

  return strength;
};

/**
 * Get password strength label
 */
export const getPasswordStrengthLabel = (strength: number): string => {
  switch (strength) {
    case 0:
      return '';
    case 1:
      return 'Very Weak';
    case 2:
      return 'Weak';
    case 3:
      return 'Fair';
    case 4:
      return 'Strong';
    case 5:
      return 'Very Strong';
    default:
      return '';
  }
};

/**
 * Get password strength color for Material-UI
 */
export const getPasswordStrengthColor = (
  strength: number
): 'error' | 'warning' | 'info' | 'success' => {
  switch (strength) {
    case 1:
      return 'error';
    case 2:
      return 'error';
    case 3:
      return 'warning';
    case 4:
      return 'info';
    case 5:
      return 'success';
    default:
      return 'error';
  }
};
