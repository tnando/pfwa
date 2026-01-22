import React, { useMemo } from 'react';
import {
  Box,
  LinearProgress,
  Typography,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Collapse,
  useTheme,
} from '@mui/material';
import { Check, Close } from '@mui/icons-material';
import { PASSWORD_REQUIREMENTS } from '@/constants/validation';
import {
  calculatePasswordStrength,
  getPasswordStrengthLabel,
  getPasswordStrengthColor,
} from '@/utils/validation';

interface PasswordStrengthMeterProps {
  password: string;
  showRequirements?: boolean;
}

/**
 * PasswordStrengthMeter - Visual indicator of password strength
 * Shows progress bar and optional checklist of requirements
 */
const PasswordStrengthMeter: React.FC<PasswordStrengthMeterProps> = ({
  password,
  showRequirements = true,
}) => {
  const theme = useTheme();

  const strength = useMemo(() => calculatePasswordStrength(password), [password]);
  const strengthLabel = useMemo(() => getPasswordStrengthLabel(strength), [strength]);
  const strengthColor = useMemo(() => getPasswordStrengthColor(strength), [strength]);

  const requirements = useMemo(
    () =>
      PASSWORD_REQUIREMENTS.map((req) => ({
        ...req,
        met: req.test(password),
      })),
    [password]
  );

  // Calculate progress percentage (0-100)
  const progressValue = (strength / 5) * 100;

  if (!password) {
    return null;
  }

  return (
    <Box sx={{ mt: 1 }}>
      {/* Strength Bar */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Box sx={{ flexGrow: 1 }}>
          <LinearProgress
            variant="determinate"
            value={progressValue}
            color={strengthColor}
            sx={{
              height: 8,
              borderRadius: 4,
              backgroundColor: theme.palette.grey[200],
            }}
          />
        </Box>
        <Typography
          variant="caption"
          sx={{
            minWidth: 80,
            textAlign: 'right',
            color: theme.palette[strengthColor].main,
            fontWeight: 500,
          }}
        >
          {strengthLabel}
        </Typography>
      </Box>

      {/* Requirements Checklist */}
      <Collapse in={showRequirements && password.length > 0}>
        <List dense sx={{ py: 1 }}>
          {requirements.map((req) => (
            <ListItem key={req.id} sx={{ py: 0, px: 0 }}>
              <ListItemIcon sx={{ minWidth: 28 }}>
                {req.met ? (
                  <Check
                    sx={{
                      fontSize: 18,
                      color: theme.palette.success.main,
                    }}
                  />
                ) : (
                  <Close
                    sx={{
                      fontSize: 18,
                      color: theme.palette.error.main,
                    }}
                  />
                )}
              </ListItemIcon>
              <ListItemText
                primary={req.label}
                primaryTypographyProps={{
                  variant: 'caption',
                  sx: {
                    color: req.met
                      ? theme.palette.success.main
                      : theme.palette.text.secondary,
                  },
                }}
              />
            </ListItem>
          ))}
        </List>
      </Collapse>
    </Box>
  );
};

export default PasswordStrengthMeter;
