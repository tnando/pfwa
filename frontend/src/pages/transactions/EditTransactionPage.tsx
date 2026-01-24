import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  Paper,
  Breadcrumbs,
  Link,
  AppBar,
  Toolbar,
  IconButton,
  Menu,
  MenuItem,
  Avatar,
  Snackbar,
  Alert,
  CircularProgress,
} from '@mui/material';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import { AccountCircle, Logout, Settings } from '@mui/icons-material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import { useAuth } from '@/context';
import { ROUTES } from '@/constants';
import { transactionApi, getErrorMessage } from '@/api';
import type { Transaction, UpdateTransactionRequest } from '@/types';
import { TransactionForm } from '@/components/transactions';

/**
 * EditTransactionPage - Form page for editing transaction
 */
const EditTransactionPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { user, logout } = useAuth();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [transaction, setTransaction] = useState<Transaction | null>(null);
  const [fetchLoading, setFetchLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: 'success' | 'error';
  }>({ open: false, message: '', severity: 'success' });

  // Fetch transaction on mount
  useEffect(() => {
    const fetchTransaction = async () => {
      if (!id) {
        setFetchError('Transaction ID is required');
        setFetchLoading(false);
        return;
      }

      try {
        const data = await transactionApi.getTransaction(id);
        setTransaction(data);
      } catch (err) {
        setFetchError(getErrorMessage(err));
      } finally {
        setFetchLoading(false);
      }
    };

    fetchTransaction();
  }, [id]);

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

  const handleDashboard = () => {
    navigate(ROUTES.DASHBOARD);
  };

  const handleSubmit = async (data: UpdateTransactionRequest) => {
    if (!id) return;

    setSubmitLoading(true);
    setSubmitError(null);
    try {
      await transactionApi.updateTransaction(id, data);
      setSnackbar({
        open: true,
        message: 'Transaction updated successfully',
        severity: 'success',
      });
      // Navigate back to transactions list after short delay
      setTimeout(() => {
        navigate(ROUTES.TRANSACTIONS);
      }, 1000);
    } catch (err) {
      setSubmitError(getErrorMessage(err));
      setSubmitLoading(false);
    }
  };

  const handleCancel = () => {
    navigate(ROUTES.TRANSACTIONS);
  };

  const handleCloseSnackbar = () => {
    setSnackbar((prev) => ({ ...prev, open: false }));
  };

  const userInitials = user
    ? `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() ||
      user.email[0].toUpperCase()
    : '';

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* App Bar */}
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              flexGrow: 1,
              cursor: 'pointer',
            }}
            onClick={handleDashboard}
          >
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
      <Container maxWidth="md" sx={{ flexGrow: 1, py: 4 }}>
        {/* Breadcrumbs */}
        <Breadcrumbs
          separator={<NavigateNextIcon fontSize="small" />}
          sx={{ mb: 3 }}
        >
          <Link
            component="button"
            variant="body1"
            onClick={() => navigate(ROUTES.TRANSACTIONS)}
            underline="hover"
            color="inherit"
          >
            Transactions
          </Link>
          <Typography color="text.primary">Edit Transaction</Typography>
        </Breadcrumbs>

        {/* Page Header */}
        <Typography variant="h4" component="h1" fontWeight={600} sx={{ mb: 4 }}>
          Edit Transaction
        </Typography>

        {/* Loading State */}
        {fetchLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        )}

        {/* Error State */}
        {fetchError && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {fetchError}
          </Alert>
        )}

        {/* Form Card */}
        {!fetchLoading && !fetchError && transaction && (
          <Paper sx={{ p: 4 }}>
            <TransactionForm
              transaction={transaction}
              onSubmit={handleSubmit}
              onCancel={handleCancel}
              loading={submitLoading}
              error={submitError}
            />
          </Paper>
        )}
      </Container>

      {/* Snackbar for success message */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={handleCloseSnackbar}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default EditTransactionPage;
