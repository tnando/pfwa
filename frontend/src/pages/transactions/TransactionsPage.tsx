import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  Button,
  Snackbar,
  Alert,
  AppBar,
  Toolbar,
  IconButton,
  Menu,
  MenuItem,
  Avatar,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import { AccountCircle, Logout, Settings } from '@mui/icons-material';
import { useAuth } from '@/context';
import { ROUTES } from '@/constants';
import { transactionApi, getErrorMessage } from '@/api';
import type {
  Transaction,
  TransactionFilter as TransactionFilterType,
  TransactionListResponse,
  SortField,
  SortDirection,
} from '@/types';
import {
  TransactionList,
  TransactionSummary,
  TransactionFilter,
  TransactionSearch,
  DeleteConfirmDialog,
} from '@/components/transactions';

const DEFAULT_PAGE_SIZE = 20;
const DEFAULT_SORT_FIELD: SortField = 'date';
const DEFAULT_SORT_DIRECTION: SortDirection = 'desc';

/**
 * Parse URL search params into filter object
 */
const parseSearchParams = (params: URLSearchParams): TransactionFilterType => {
  const filter: TransactionFilterType = {};

  const startDate = params.get('startDate');
  const endDate = params.get('endDate');
  const type = params.get('type');
  const categoryIds = params.get('categoryIds');
  const minAmount = params.get('minAmount');
  const maxAmount = params.get('maxAmount');
  const search = params.get('search');
  const page = params.get('page');
  const size = params.get('size');
  const sort = params.get('sort');

  if (startDate) filter.startDate = startDate;
  if (endDate) filter.endDate = endDate;
  if (type === 'INCOME' || type === 'EXPENSE') filter.type = type;
  if (categoryIds) filter.categoryIds = categoryIds.split(',');
  if (minAmount) filter.minAmount = parseFloat(minAmount);
  if (maxAmount) filter.maxAmount = parseFloat(maxAmount);
  if (search) filter.search = search;
  if (page) filter.page = parseInt(page, 10);
  if (size) filter.size = parseInt(size, 10);
  if (sort) filter.sort = sort;

  return filter;
};

/**
 * Build URL search params from filter object
 */
const buildSearchParams = (filter: TransactionFilterType): URLSearchParams => {
  const params = new URLSearchParams();

  if (filter.startDate) params.set('startDate', filter.startDate);
  if (filter.endDate) params.set('endDate', filter.endDate);
  if (filter.type) params.set('type', filter.type);
  if (filter.categoryIds && filter.categoryIds.length > 0) {
    params.set('categoryIds', filter.categoryIds.join(','));
  }
  if (filter.minAmount !== undefined) params.set('minAmount', filter.minAmount.toString());
  if (filter.maxAmount !== undefined) params.set('maxAmount', filter.maxAmount.toString());
  if (filter.search) params.set('search', filter.search);
  if (filter.page !== undefined && filter.page !== 0) {
    params.set('page', filter.page.toString());
  }
  if (filter.size !== undefined && filter.size !== DEFAULT_PAGE_SIZE) {
    params.set('size', filter.size.toString());
  }
  if (filter.sort && filter.sort !== `${DEFAULT_SORT_FIELD},${DEFAULT_SORT_DIRECTION}`) {
    params.set('sort', filter.sort);
  }

  return params;
};

/**
 * TransactionsPage - Main page with list, filters, summary
 */
const TransactionsPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user, logout } = useAuth();

  // User menu state
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  // Data state
  const [response, setResponse] = useState<TransactionListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filter state from URL
  const [filter, setFilter] = useState<TransactionFilterType>(() =>
    parseSearchParams(searchParams)
  );

  // Sort state
  const [sortField, setSortField] = useState<SortField>(() => {
    const sort = filter.sort || `${DEFAULT_SORT_FIELD},${DEFAULT_SORT_DIRECTION}`;
    return sort.split(',')[0] as SortField;
  });
  const [sortDirection, setSortDirection] = useState<SortDirection>(() => {
    const sort = filter.sort || `${DEFAULT_SORT_FIELD},${DEFAULT_SORT_DIRECTION}`;
    return sort.split(',')[1] as SortDirection;
  });

  // Delete dialog state
  const [deleteTarget, setDeleteTarget] = useState<Transaction | null>(null);
  const [deleting, setDeleting] = useState(false);

  // Snackbar state
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: 'success' | 'error';
  }>({ open: false, message: '', severity: 'success' });

  // Fetch transactions
  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await transactionApi.getTransactions(filter);
      setResponse(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  // Sync filter to URL
  useEffect(() => {
    const params = buildSearchParams(filter);
    setSearchParams(params, { replace: true });
  }, [filter, setSearchParams]);

  // User menu handlers
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

  // Filter handlers
  const handleFilterChange = (newFilter: TransactionFilterType) => {
    setFilter({ ...newFilter, page: 0 }); // Reset to first page on filter change
  };

  const handleClearFilters = () => {
    setFilter({
      page: 0,
      size: filter.size,
      sort: filter.sort,
    });
  };

  const handleSearchChange = (search: string) => {
    setFilter((prev) => ({
      ...prev,
      search: search || undefined,
      page: 0,
    }));
  };

  // Pagination handlers
  const handlePageChange = (page: number) => {
    setFilter((prev) => ({ ...prev, page }));
  };

  const handlePageSizeChange = (size: number) => {
    setFilter((prev) => ({ ...prev, size, page: 0 }));
  };

  // Sort handlers
  const handleSortChange = (field: SortField, direction: SortDirection) => {
    setSortField(field);
    setSortDirection(direction);
    setFilter((prev) => ({
      ...prev,
      sort: `${field},${direction}`,
      page: 0,
    }));
  };

  // Transaction actions
  const handleAddTransaction = () => {
    navigate(ROUTES.TRANSACTIONS_NEW);
  };

  const handleEditTransaction = (transaction: Transaction) => {
    navigate(`${ROUTES.TRANSACTIONS}/${transaction.id}/edit`);
  };

  const handleDeleteClick = (transaction: Transaction) => {
    setDeleteTarget(transaction);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;

    setDeleting(true);
    try {
      await transactionApi.deleteTransaction(deleteTarget.id);
      setSnackbar({
        open: true,
        message: 'Transaction deleted successfully',
        severity: 'success',
      });
      setDeleteTarget(null);
      fetchTransactions();
    } catch (err) {
      setSnackbar({
        open: true,
        message: getErrorMessage(err),
        severity: 'error',
      });
    } finally {
      setDeleting(false);
    }
  };

  const handleDeleteCancel = () => {
    setDeleteTarget(null);
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
      <Container maxWidth="lg" sx={{ flexGrow: 1, py: 4 }}>
        {/* Page Header */}
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            mb: 3,
          }}
        >
          <Typography variant="h4" component="h1" fontWeight={600}>
            Transactions
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleAddTransaction}
          >
            Add Transaction
          </Button>
        </Box>

        {/* Summary Cards */}
        <Box sx={{ mb: 3 }}>
          <TransactionSummary summary={response?.summary || null} loading={loading} />
        </Box>

        {/* Search and Filters */}
        <Box sx={{ mb: 3, display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TransactionSearch
            value={filter.search || ''}
            onChange={handleSearchChange}
          />
          <TransactionFilter
            filter={filter}
            onFilterChange={handleFilterChange}
            onClear={handleClearFilters}
          />
        </Box>

        {/* Error Alert */}
        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {/* Transaction List */}
        <TransactionList
          transactions={response?.content || []}
          loading={loading}
          page={filter.page || 0}
          pageSize={filter.size || DEFAULT_PAGE_SIZE}
          totalElements={response?.totalElements || 0}
          sortField={sortField}
          sortDirection={sortDirection}
          onPageChange={handlePageChange}
          onPageSizeChange={handlePageSizeChange}
          onSortChange={handleSortChange}
          onEdit={handleEditTransaction}
          onDelete={handleDeleteClick}
        />
      </Container>

      {/* Delete Confirmation Dialog */}
      <DeleteConfirmDialog
        open={!!deleteTarget}
        onClose={handleDeleteCancel}
        onConfirm={handleDeleteConfirm}
        loading={deleting}
      />

      {/* Snackbar for notifications */}
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

export default TransactionsPage;
