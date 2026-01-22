import React from 'react';
import {
  Box,
  Container,
  Typography,
  AppBar,
  Toolbar,
  IconButton,
  Breadcrumbs,
  Link as MuiLink,
} from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { SessionManagement } from '@/components/settings';
import { ROUTES } from '@/constants';

/**
 * SettingsPage - User account settings
 * Currently contains session management
 */
const SettingsPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* App Bar */}
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar>
          <IconButton
            edge="start"
            color="inherit"
            aria-label="Back to dashboard"
            onClick={() => navigate(ROUTES.DASHBOARD)}
            sx={{ mr: 2 }}
          >
            <ArrowBack />
          </IconButton>
          <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1 }}>
            <AccountBalanceWalletIcon sx={{ mr: 1, color: 'primary.main' }} />
            <Typography variant="h6" component="div" sx={{ fontWeight: 600 }}>
              Personal Finance
            </Typography>
          </Box>
        </Toolbar>
      </AppBar>

      {/* Main Content */}
      <Container maxWidth="md" sx={{ flexGrow: 1, py: 4 }}>
        {/* Breadcrumbs */}
        <Breadcrumbs aria-label="breadcrumb" sx={{ mb: 3 }}>
          <MuiLink
            component={RouterLink}
            to={ROUTES.DASHBOARD}
            underline="hover"
            color="inherit"
          >
            Dashboard
          </MuiLink>
          <Typography color="text.primary">Settings</Typography>
        </Breadcrumbs>

        {/* Page Title */}
        <Typography variant="h4" component="h1" sx={{ mb: 4 }}>
          Account Settings
        </Typography>

        {/* Session Management */}
        <SessionManagement />
      </Container>
    </Box>
  );
};

export default SettingsPage;
