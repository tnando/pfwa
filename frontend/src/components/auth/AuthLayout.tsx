import React from 'react';
import {
  Box,
  Container,
  Paper,
  Typography,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';

interface AuthLayoutProps {
  children: React.ReactNode;
  title: string;
  subtitle?: string;
}

/**
 * AuthLayout - Centered card layout for authentication pages
 * Provides consistent branding and styling for login, register, etc.
 */
const AuthLayout: React.FC<AuthLayoutProps> = ({ children, title, subtitle }) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  return (
    <Box
      component="main"
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: theme.palette.grey[100],
        py: 4,
        px: 2,
      }}
    >
      <Container maxWidth="sm">
        {/* Logo and App Name */}
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            mb: 4,
          }}
        >
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 64,
              height: 64,
              borderRadius: 2,
              backgroundColor: theme.palette.primary.main,
              color: theme.palette.primary.contrastText,
              mb: 2,
            }}
          >
            <AccountBalanceWalletIcon sx={{ fontSize: 36 }} />
          </Box>
          <Typography
            variant="h5"
            component="h1"
            sx={{
              fontWeight: 700,
              color: theme.palette.text.primary,
            }}
          >
            Personal Finance
          </Typography>
          <Typography
            variant="body2"
            sx={{
              color: theme.palette.text.secondary,
              mt: 0.5,
            }}
          >
            Track your income, expenses, and budgets
          </Typography>
        </Box>

        {/* Auth Card */}
        <Paper
          elevation={isMobile ? 0 : 2}
          sx={{
            p: { xs: 3, sm: 4 },
            borderRadius: 2,
            backgroundColor: theme.palette.background.paper,
          }}
        >
          {/* Page Title */}
          <Box sx={{ mb: 3, textAlign: 'center' }}>
            <Typography
              variant="h6"
              component="h2"
              sx={{
                fontWeight: 600,
                color: theme.palette.text.primary,
              }}
            >
              {title}
            </Typography>
            {subtitle && (
              <Typography
                variant="body2"
                sx={{
                  color: theme.palette.text.secondary,
                  mt: 1,
                }}
              >
                {subtitle}
              </Typography>
            )}
          </Box>

          {/* Form Content */}
          {children}
        </Paper>

        {/* Footer */}
        <Typography
          variant="caption"
          sx={{
            display: 'block',
            textAlign: 'center',
            mt: 4,
            color: theme.palette.text.secondary,
          }}
        >
          &copy; {new Date().getFullYear()} PFWA. All rights reserved.
        </Typography>
      </Container>
    </Box>
  );
};

export default AuthLayout;
