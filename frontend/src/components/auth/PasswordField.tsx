import React, { useState } from 'react';
import {
  TextField,
  InputAdornment,
  IconButton,
  TextFieldProps,
} from '@mui/material';
import { Visibility, VisibilityOff } from '@mui/icons-material';

type PasswordFieldProps = Omit<TextFieldProps, 'type'> & {
  showToggle?: boolean;
};

/**
 * PasswordField - Password input with show/hide toggle
 * Provides accessible password visibility toggle button
 */
const PasswordField = React.forwardRef<HTMLInputElement, PasswordFieldProps>(
  ({ showToggle = true, ...props }, ref) => {
    const [showPassword, setShowPassword] = useState(false);

    const handleToggleVisibility = () => {
      setShowPassword((prev) => !prev);
    };

    const handleMouseDownPassword = (event: React.MouseEvent<HTMLButtonElement>) => {
      event.preventDefault();
    };

    return (
      <TextField
        {...props}
        ref={ref}
        type={showPassword ? 'text' : 'password'}
        InputProps={{
          ...props.InputProps,
          endAdornment: showToggle ? (
            <InputAdornment position="end">
              <IconButton
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                onClick={handleToggleVisibility}
                onMouseDown={handleMouseDownPassword}
                edge="end"
                size="small"
              >
                {showPassword ? <VisibilityOff /> : <Visibility />}
              </IconButton>
            </InputAdornment>
          ) : undefined,
        }}
      />
    );
  }
);

PasswordField.displayName = 'PasswordField';

export default PasswordField;
