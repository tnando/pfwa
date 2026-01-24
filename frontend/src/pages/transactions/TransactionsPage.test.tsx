import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import TransactionsPage from './TransactionsPage';
import { AuthProvider } from '@/context';
import type { TransactionListResponse } from '@/types';

const theme = createTheme();

// Mock the API module
vi.mock('@/api', () => ({
  transactionApi: {
    getTransactions: vi.fn(),
    deleteTransaction: vi.fn(),
    createTransaction: vi.fn(),
    updateTransaction: vi.fn(),
  },
  categoryApi: {
    getCategories: vi.fn().mockResolvedValue({
      income: [
        { id: 'cat-salary', name: 'Salary', type: 'INCOME', icon: 'payments', color: '#4CAF50' },
      ],
      expense: [
        { id: 'cat-food', name: 'Food & Dining', type: 'EXPENSE', icon: 'restaurant', color: '#F44336' },
      ],
    }),
  },
  getErrorMessage: vi.fn((err) => err?.response?.data?.message || err?.message || 'An error occurred'),
}));

// Mock the AuthContext
vi.mock('@/context', async () => {
  const actual = await vi.importActual('@/context');
  return {
    ...actual,
    useAuth: vi.fn(() => ({
      user: {
        id: 'user-1',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User',
      },
      isAuthenticated: true,
      isLoading: false,
      logout: vi.fn(),
    })),
    AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  };
});

const mockTransactionListResponse: TransactionListResponse = {
  content: [
    {
      id: 'tx-1',
      amount: 150.0,
      type: 'EXPENSE',
      category: {
        id: 'cat-food',
        name: 'Food & Dining',
        type: 'EXPENSE',
        icon: 'restaurant',
        color: '#F44336',
      },
      date: '2026-01-20',
      description: 'Grocery shopping',
      notes: null,
      createdAt: '2026-01-20T14:30:00Z',
      updatedAt: '2026-01-20T14:30:00Z',
    },
    {
      id: 'tx-2',
      amount: 5000.0,
      type: 'INCOME',
      category: {
        id: 'cat-salary',
        name: 'Salary',
        type: 'INCOME',
        icon: 'payments',
        color: '#4CAF50',
      },
      date: '2026-01-15',
      description: 'Monthly salary',
      notes: null,
      createdAt: '2026-01-15T09:00:00Z',
      updatedAt: '2026-01-15T09:00:00Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
  first: true,
  last: true,
  summary: {
    totalIncome: 5000.0,
    totalExpenses: 150.0,
    netBalance: 4850.0,
  },
};

const emptyTransactionListResponse: TransactionListResponse = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
  summary: {
    totalIncome: 0,
    totalExpenses: 0,
    netBalance: 0,
  },
};

const renderTransactionsPage = (initialRoute = '/transactions') => {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[initialRoute]}>
        <Routes>
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/transactions/new" element={<div>Create Transaction Page</div>} />
          <Route path="/transactions/:id/edit" element={<div>Edit Transaction Page</div>} />
          <Route path="/dashboard" element={<div>Dashboard</div>} />
          <Route path="/settings" element={<div>Settings</div>} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>
  );
};

describe('TransactionsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Initial Rendering', () => {
    it('should render page title', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      expect(screen.getByText('Transactions')).toBeInTheDocument();
    });

    it('should render Add Transaction button', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      expect(screen.getByRole('button', { name: /add transaction/i })).toBeInTheDocument();
    });

    it('should fetch transactions on mount', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(transactionApi.getTransactions).toHaveBeenCalled();
      });
    });

    it('should display transactions after loading', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText('Grocery shopping')).toBeInTheDocument();
        expect(screen.getByText('Monthly salary')).toBeInTheDocument();
      });
    });
  });

  describe('Summary Display', () => {
    it('should display transaction summary', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        // Summary component should show totals
        expect(screen.getByText('$5,000.00')).toBeInTheDocument();
        expect(screen.getByText('$150.00')).toBeInTheDocument();
      });
    });
  });

  describe('Empty State', () => {
    it('should show empty message when no transactions', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        emptyTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText(/no transactions yet/i)).toBeInTheDocument();
      });
    });
  });

  describe('Error Handling', () => {
    it('should display error message on fetch failure', async () => {
      const { transactionApi, getErrorMessage } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Network error')
      );
      (getErrorMessage as ReturnType<typeof vi.fn>).mockReturnValue('Failed to load transactions');

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent('Failed to load transactions');
      });
    });
  });

  describe('Navigation', () => {
    it('should navigate to create page when clicking Add Transaction', async () => {
      const user = userEvent.setup();
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /add transaction/i })).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /add transaction/i }));

      await waitFor(() => {
        expect(screen.getByText('Create Transaction Page')).toBeInTheDocument();
      });
    });

    it('should navigate to edit page when clicking edit button', async () => {
      const user = userEvent.setup();
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText('Grocery shopping')).toBeInTheDocument();
      });

      const editButtons = screen.getAllByRole('button', { name: /edit transaction/i });
      await user.click(editButtons[0]);

      await waitFor(() => {
        expect(screen.getByText('Edit Transaction Page')).toBeInTheDocument();
      });
    });

    it('should navigate to dashboard when clicking logo', async () => {
      const user = userEvent.setup();
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText('Personal Finance')).toBeInTheDocument();
      });

      await user.click(screen.getByText('Personal Finance'));

      await waitFor(() => {
        expect(screen.getByText('Dashboard')).toBeInTheDocument();
      });
    });
  });

  describe('Delete Transaction', () => {
    it('should show delete confirmation dialog', async () => {
      const user = userEvent.setup();
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText('Grocery shopping')).toBeInTheDocument();
      });

      const deleteButtons = screen.getAllByRole('button', { name: /delete transaction/i });
      await user.click(deleteButtons[0]);

      await waitFor(() => {
        expect(screen.getByText(/are you sure/i)).toBeInTheDocument();
      });
    });

    it('should delete transaction on confirm', async () => {
      const user = userEvent.setup();
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );
      (transactionApi.deleteTransaction as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText('Grocery shopping')).toBeInTheDocument();
      });

      const deleteButtons = screen.getAllByRole('button', { name: /delete transaction/i });
      await user.click(deleteButtons[0]);

      await waitFor(() => {
        expect(screen.getByText(/are you sure/i)).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /confirm/i }));

      await waitFor(() => {
        expect(transactionApi.deleteTransaction).toHaveBeenCalledWith('tx-1');
      });
    });

    it('should close dialog on cancel', async () => {
      const user = userEvent.setup();
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText('Grocery shopping')).toBeInTheDocument();
      });

      const deleteButtons = screen.getAllByRole('button', { name: /delete transaction/i });
      await user.click(deleteButtons[0]);

      await waitFor(() => {
        expect(screen.getByText(/are you sure/i)).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /cancel/i }));

      await waitFor(() => {
        expect(screen.queryByText(/are you sure/i)).not.toBeInTheDocument();
      });
    });

    it('should show success snackbar after delete', async () => {
      const user = userEvent.setup();
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );
      (transactionApi.deleteTransaction as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText('Grocery shopping')).toBeInTheDocument();
      });

      const deleteButtons = screen.getAllByRole('button', { name: /delete transaction/i });
      await user.click(deleteButtons[0]);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /confirm/i })).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /confirm/i }));

      await waitFor(() => {
        expect(screen.getByText(/deleted successfully/i)).toBeInTheDocument();
      });
    });
  });

  describe('Filtering', () => {
    it('should fetch with filter parameters', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage('/transactions?type=EXPENSE');

      await waitFor(() => {
        expect(transactionApi.getTransactions).toHaveBeenCalledWith(
          expect.objectContaining({
            type: 'EXPENSE',
          })
        );
      });
    });

    it('should persist filters in URL', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage('/transactions?startDate=2026-01-01&endDate=2026-01-31');

      await waitFor(() => {
        expect(transactionApi.getTransactions).toHaveBeenCalledWith(
          expect.objectContaining({
            startDate: '2026-01-01',
            endDate: '2026-01-31',
          })
        );
      });
    });
  });

  describe('Pagination', () => {
    it('should fetch with pagination parameters from URL', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage('/transactions?page=2&size=50');

      await waitFor(() => {
        expect(transactionApi.getTransactions).toHaveBeenCalledWith(
          expect.objectContaining({
            page: 2,
            size: 50,
          })
        );
      });
    });
  });

  describe('Search', () => {
    it('should fetch with search parameter from URL', async () => {
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage('/transactions?search=grocery');

      await waitFor(() => {
        expect(transactionApi.getTransactions).toHaveBeenCalledWith(
          expect.objectContaining({
            search: 'grocery',
          })
        );
      });
    });
  });

  describe('User Menu', () => {
    it('should show user email in menu', async () => {
      const user = userEvent.setup();
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText('Transactions')).toBeInTheDocument();
      });

      // Find and click user avatar/menu button
      const menuButton = screen.getByRole('button', { name: /account menu/i });
      await user.click(menuButton);

      await waitFor(() => {
        expect(screen.getByText('test@example.com')).toBeInTheDocument();
      });
    });

    it('should navigate to settings from menu', async () => {
      const user = userEvent.setup();
      const { transactionApi } = await import('@/api');
      (transactionApi.getTransactions as ReturnType<typeof vi.fn>).mockResolvedValue(
        mockTransactionListResponse
      );

      renderTransactionsPage();

      await waitFor(() => {
        expect(screen.getByText('Transactions')).toBeInTheDocument();
      });

      const menuButton = screen.getByRole('button', { name: /account menu/i });
      await user.click(menuButton);

      await waitFor(() => {
        expect(screen.getByText('Settings')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('menuitem', { name: /settings/i }));

      await waitFor(() => {
        expect(screen.getByText('Settings')).toBeInTheDocument();
      });
    });
  });
});
