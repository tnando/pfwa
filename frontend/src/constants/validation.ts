/**
 * Password validation constants
 */
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 128;

/**
 * Password validation regex patterns
 */
export const PASSWORD_PATTERNS = {
  uppercase: /[A-Z]/,
  lowercase: /[a-z]/,
  digit: /[0-9]/,
  special: /[!@#$%^&*()_+\-=[\]{}|;:,.<>?]/,
} as const;

/**
 * Password requirement descriptions
 */
export const PASSWORD_REQUIREMENTS = [
  {
    id: 'length',
    label: `At least ${PASSWORD_MIN_LENGTH} characters`,
    test: (password: string) => password.length >= PASSWORD_MIN_LENGTH,
  },
  {
    id: 'uppercase',
    label: 'One uppercase letter (A-Z)',
    test: (password: string) => PASSWORD_PATTERNS.uppercase.test(password),
  },
  {
    id: 'lowercase',
    label: 'One lowercase letter (a-z)',
    test: (password: string) => PASSWORD_PATTERNS.lowercase.test(password),
  },
  {
    id: 'digit',
    label: 'One number (0-9)',
    test: (password: string) => PASSWORD_PATTERNS.digit.test(password),
  },
  {
    id: 'special',
    label: 'One special character (!@#$%^&*...)',
    test: (password: string) => PASSWORD_PATTERNS.special.test(password),
  },
] as const;

/**
 * Email validation pattern
 */
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Email max length
 */
export const EMAIL_MAX_LENGTH = 255;
