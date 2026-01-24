import React from 'react';
import {
  Box,
  Container,
  Typography,
  AppBar,
  Toolbar,
  IconButton,
  Menu,
  MenuItem,
  Avatar,
  Button,
  Card,
  CardContent,
  CardActions,
  Grid,
} from '@mui/material';
import { AccountCircle, Logout, Settings } from '@mui/icons-material';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context';
import { ROUTES } from '@/constants';

/**
 * DashboardPage - Main authenticated landing page
 * Placeholder implementation for MVP
 */
const DashboardPage: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleSettings = () => {
    handleMenuClose();
    navigate(ROUTES.SETTINGS);
  };

  const handleLogout = async () => {
    handleMenuClose();
    await logout();
  };

  const userInitials = user
    ? `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() || user.email[0].toUpperCase()
    : '';

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* App Bar */}
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar>
          <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1 }}>
            <AccountBalanceWalletIcon sx={{ mr: 1, color: 'primary.main' }} />
            <Typography variant="h6" component="div" sx={{ fontWeight: 600 }}>
              Personal Finance
            </Typography>
          </Box>

          {/* User Menu */}
          <IconButton
            size="large"
            aria-label="Account menu"
            aria-controls="menu-appbar"
            aria-haspopup="true"
            onClick={handleMenuOpen}
            color="inherit"
          >
            {userInitials ? (
              <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
                {userInitials}
              </Avatar>
            ) : (
              <AccountCircle />
            )}
          </IconButton>
          <Menu
            id="menu-appbar"
            anchorEl={anchorEl}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            keepMounted
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
          >
            <MenuItem disabled sx={{ opacity: 1 }}>
              <Typography variant="body2" color="text.secondary">
                {user?.email}
              </Typography>
            </MenuItem>
            <MenuItem onClick={handleSettings}>
              <Settings sx={{ mr: 1, fontSize: 20 }} />
              Settings
            </MenuItem>
            <MenuItem onClick={handleLogout}>
              <Logout sx={{ mr: 1, fontSize: 20 }} />
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      {/* Main Content */}
      <Container maxWidth="lg" sx={{ flexGrow: 1, py: 4 }}>
        <Box sx={{ mb: 4 }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Welcome{user?.firstName ? `, ${user.firstName}` : ''}!
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Your personal finance dashboard. Start tracking your income, expenses, and budgets.
          </Typography>
        </Box>

        {/* Quick Actions */}
        <Grid container spacing={3}>
          {/* Transactions Card */}
          <Grid item xs={12} sm={6} md={4}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <CardContent sx={{ flexGrow: 1 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <ReceiptLongIcon color="primary" sx={{ fontSize: 40, mr: 2 }} />
                  <Typography variant="h6" component="h2">
                    Transactions
                  </Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  View and manage all your income and expense transactions. Filter by date, category, or amount.
                </Typography>
              </CardContent>
              <CardActions sx={{ p: 2, pt: 0 }}>
                <Button
                  variant="contained"
                  onClick={() => navigate(ROUTES.TRANSACTIONS)}
                  fullWidth
                >
                  View Transactions
                </Button>
              </CardActions>
            </Card>
          </Grid>

          {/* Add Transaction Card */}
          <Grid item xs={12} sm={6} md={4}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <CardContent sx={{ flexGrow: 1 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <AddCircleOutlineIcon color="success" sx={{ fontSize: 40, mr: 2 }} />
                  <Typography variant="h6" component="h2">
                    Add Transaction
                  </Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Record a new income or expense transaction to keep your finances up to date.
                </Typography>
              </CardContent>
              <CardActions sx={{ p: 2, pt: 0 }}>
                <Button
                  variant="outlined"
                  color="success"
                  onClick={() => navigate(ROUTES.TRANSACTIONS_NEW)}
                  fullWidth
                >
                  Add New
                </Button>
              </CardActions>
            </Card>
          </Grid>

          {/* Budgets Card (Coming Soon) */}
          <Grid item xs={12} sm={6} md={4}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', opacity: 0.7 }}>
              <CardContent sx={{ flexGrow: 1 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <AccountBalanceWalletIcon color="secondary" sx={{ fontSize: 40, mr: 2 }} />
                  <Typography variant="h6" component="h2">
                    Budgets
                  </Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Set monthly budgets for different categories and track your spending progress.
                </Typography>
              </CardContent>
              <CardActions sx={{ p: 2, pt: 0 }}>
                <Button variant="outlined" disabled fullWidth>
                  Coming Soon
                </Button>
              </CardActions>
            </Card>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
};

export default DashboardPage;
