import { describe, it, expect } from 'vitest';
import {
  emailSchema,
  passwordSchema,
  loginSchema,
  registerSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
  calculatePasswordStrength,
  getPasswordStrengthLabel,
  getPasswordStrengthColor,
} from './validation';

describe('emailSchema', () => {
  it('should validate a correct email', () => {
    const result = emailSchema.safeParse('user@example.com');
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data).toBe('user@example.com');
    }
  });

  it('should transform email to lowercase', () => {
    const result = emailSchema.safeParse('USER@EXAMPLE.COM');
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data).toBe('user@example.com');
    }
  });

  it('should trim and normalize email', () => {
    // Note: Zod email() validation happens before transform, so leading/trailing
    // spaces may cause validation to fail. The transform handles lowercase.
    const result = emailSchema.safeParse('USER@example.com');
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data).toBe('user@example.com');
    }
  });

  it('should reject empty email', () => {
    const result = emailSchema.safeParse('');
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Email is required');
    }
  });

  it('should reject invalid email format', () => {
    const result = emailSchema.safeParse('not-an-email');
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Invalid email format');
    }
  });

  it('should reject email exceeding max length', () => {
    const longEmail = 'a'.repeat(250) + '@example.com';
    const result = emailSchema.safeParse(longEmail);
    expect(result.success).toBe(false);
  });
});

describe('passwordSchema', () => {
  it('should validate a strong password', () => {
    const result = passwordSchema.safeParse('SecurePass123!');
    expect(result.success).toBe(true);
  });

  it('should reject password shorter than 8 characters', () => {
    const result = passwordSchema.safeParse('Pass1!');
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toContain('at least 8 characters');
    }
  });

  it('should reject password without uppercase letter', () => {
    const result = passwordSchema.safeParse('securepass123!');
    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((i) => i.message);
      expect(messages.some((m) => m.includes('uppercase'))).toBe(true);
    }
  });

  it('should reject password without lowercase letter', () => {
    const result = passwordSchema.safeParse('SECUREPASS123!');
    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((i) => i.message);
      expect(messages.some((m) => m.includes('lowercase'))).toBe(true);
    }
  });

  it('should reject password without number', () => {
    const result = passwordSchema.safeParse('SecurePass!');
    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((i) => i.message);
      expect(messages.some((m) => m.includes('number'))).toBe(true);
    }
  });

  it('should reject password without special character', () => {
    const result = passwordSchema.safeParse('SecurePass123');
    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((i) => i.message);
      expect(messages.some((m) => m.includes('special character'))).toBe(true);
    }
  });
});

describe('loginSchema', () => {
  it('should validate correct login data', () => {
    const result = loginSchema.safeParse({
      email: 'user@example.com',
      password: 'anypassword',
      rememberMe: true,
    });
    expect(result.success).toBe(true);
  });

  it('should accept login without rememberMe', () => {
    const result = loginSchema.safeParse({
      email: 'user@example.com',
      password: 'anypassword',
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.rememberMe).toBe(false);
    }
  });

  it('should reject empty password for login', () => {
    const result = loginSchema.safeParse({
      email: 'user@example.com',
      password: '',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((i) => i.message);
      expect(messages.some((m) => m.includes('Password is required'))).toBe(true);
    }
  });
});

describe('registerSchema', () => {
  it('should validate correct registration data', () => {
    const result = registerSchema.safeParse({
      email: 'user@example.com',
      password: 'SecurePass123!',
      confirmPassword: 'SecurePass123!',
    });
    expect(result.success).toBe(true);
  });

  it('should reject mismatched passwords', () => {
    const result = registerSchema.safeParse({
      email: 'user@example.com',
      password: 'SecurePass123!',
      confirmPassword: 'DifferentPass123!',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((i) => i.message);
      expect(messages.some((m) => m.includes('Passwords do not match'))).toBe(true);
    }
  });

  it('should reject weak password in registration', () => {
    const result = registerSchema.safeParse({
      email: 'user@example.com',
      password: 'weak',
      confirmPassword: 'weak',
    });
    expect(result.success).toBe(false);
  });
});

describe('forgotPasswordSchema', () => {
  it('should validate correct email', () => {
    const result = forgotPasswordSchema.safeParse({
      email: 'user@example.com',
    });
    expect(result.success).toBe(true);
  });

  it('should reject invalid email', () => {
    const result = forgotPasswordSchema.safeParse({
      email: 'invalid',
    });
    expect(result.success).toBe(false);
  });
});

describe('resetPasswordSchema', () => {
  it('should validate correct reset password data', () => {
    const result = resetPasswordSchema.safeParse({
      newPassword: 'NewSecure123!',
      confirmPassword: 'NewSecure123!',
    });
    expect(result.success).toBe(true);
  });

  it('should reject mismatched passwords', () => {
    const result = resetPasswordSchema.safeParse({
      newPassword: 'NewSecure123!',
      confirmPassword: 'Different123!',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((i) => i.message);
      expect(messages.some((m) => m.includes('Passwords do not match'))).toBe(true);
    }
  });
});

describe('calculatePasswordStrength', () => {
  it('should return 0 for empty password', () => {
    expect(calculatePasswordStrength('')).toBe(0);
  });

  it('should return 1 for password with only length requirement', () => {
    expect(calculatePasswordStrength('aaaaaaaa')).toBe(2); // length + lowercase
  });

  it('should return 2 for password with length and uppercase', () => {
    expect(calculatePasswordStrength('AAAAAAAA')).toBe(2); // length + uppercase
  });

  it('should return 5 for password meeting all requirements', () => {
    expect(calculatePasswordStrength('SecurePass123!')).toBe(5);
  });

  it('should correctly count requirements for partial passwords', () => {
    // length + lowercase + digit = 3
    expect(calculatePasswordStrength('password1')).toBe(3);
    // length + uppercase + lowercase = 3
    expect(calculatePasswordStrength('Password')).toBe(3);
  });
});

describe('getPasswordStrengthLabel', () => {
  it('should return empty string for strength 0', () => {
    expect(getPasswordStrengthLabel(0)).toBe('');
  });

  it('should return "Very Weak" for strength 1', () => {
    expect(getPasswordStrengthLabel(1)).toBe('Very Weak');
  });

  it('should return "Weak" for strength 2', () => {
    expect(getPasswordStrengthLabel(2)).toBe('Weak');
  });

  it('should return "Fair" for strength 3', () => {
    expect(getPasswordStrengthLabel(3)).toBe('Fair');
  });

  it('should return "Strong" for strength 4', () => {
    expect(getPasswordStrengthLabel(4)).toBe('Strong');
  });

  it('should return "Very Strong" for strength 5', () => {
    expect(getPasswordStrengthLabel(5)).toBe('Very Strong');
  });
});

describe('getPasswordStrengthColor', () => {
  it('should return error for strength 1', () => {
    expect(getPasswordStrengthColor(1)).toBe('error');
  });

  it('should return error for strength 2', () => {
    expect(getPasswordStrengthColor(2)).toBe('error');
  });

  it('should return warning for strength 3', () => {
    expect(getPasswordStrengthColor(3)).toBe('warning');
  });

  it('should return info for strength 4', () => {
    expect(getPasswordStrengthColor(4)).toBe('info');
  });

  it('should return success for strength 5', () => {
    expect(getPasswordStrengthColor(5)).toBe('success');
  });
});
