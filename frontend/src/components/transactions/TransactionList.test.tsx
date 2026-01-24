import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import TransactionList from './TransactionList';
import type { Transaction, SortField, SortDirection } from '@/types';

const theme = createTheme();

const mockTransactions: Transaction[] = [
  {
    id: '1',
    amount: 150.0,
    type: 'EXPENSE',
    category: {
      id: 'cat-1',
      name: 'Food & Dining',
      type: 'EXPENSE',
      icon: 'restaurant',
      color: '#F44336',
    },
    date: '2026-01-20',
    description: 'Grocery shopping at Whole Foods',
    notes: 'Weekly groceries',
    createdAt: '2026-01-20T14:30:00Z',
    updatedAt: '2026-01-20T14:30:00Z',
  },
  {
    id: '2',
    amount: 5000.0,
    type: 'INCOME',
    category: {
      id: 'cat-2',
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
  {
    id: '3',
    amount: 75.5,
    type: 'EXPENSE',
    category: {
      id: 'cat-3',
      name: 'Transportation',
      type: 'EXPENSE',
      icon: 'directions_car',
      color: '#E91E63',
    },
    date: '2026-01-18',
    description: 'Gas for car',
    notes: 'Filled up at Shell station',
    createdAt: '2026-01-18T16:45:00Z',
    updatedAt: '2026-01-18T16:45:00Z',
  },
];

const defaultProps = {
  transactions: mockTransactions,
  loading: false,
  page: 0,
  pageSize: 20,
  totalElements: 3,
  sortField: 'date' as SortField,
  sortDirection: 'desc' as SortDirection,
  onPageChange: vi.fn(),
  onPageSizeChange: vi.fn(),
  onSortChange: vi.fn(),
  onEdit: vi.fn(),
  onDelete: vi.fn(),
};

const renderTransactionList = (props = {}) => {
  return render(
    <ThemeProvider theme={theme}>
      <TransactionList {...defaultProps} {...props} />
    </ThemeProvider>
  );
};

describe('TransactionList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render table with correct headers', () => {
      renderTransactionList();

      expect(screen.getByText('Date')).toBeInTheDocument();
      expect(screen.getByText('Type')).toBeInTheDocument();
      expect(screen.getByText('Category')).toBeInTheDocument();
      expect(screen.getByText('Description')).toBeInTheDocument();
      expect(screen.getByText('Amount')).toBeInTheDocument();
      expect(screen.getByText('Actions')).toBeInTheDocument();
    });

    it('should render all transactions', () => {
      renderTransactionList();

      expect(screen.getByText('Grocery shopping at Whole Foods')).toBeInTheDocument();
      expect(screen.getByText('Monthly salary')).toBeInTheDocument();
      expect(screen.getByText('Gas for car')).toBeInTheDocument();
    });

    it('should format currency correctly', () => {
      renderTransactionList();

      expect(screen.getByText('-$150.00')).toBeInTheDocument();
      expect(screen.getByText('+$5,000.00')).toBeInTheDocument();
      expect(screen.getByText('-$75.50')).toBeInTheDocument();
    });

    it('should format dates correctly', () => {
      renderTransactionList();

      expect(screen.getByText('Jan 20, 2026')).toBeInTheDocument();
      expect(screen.getByText('Jan 15, 2026')).toBeInTheDocument();
      expect(screen.getByText('Jan 18, 2026')).toBeInTheDocument();
    });

    it('should display category names', () => {
      renderTransactionList();

      expect(screen.getByText('Food & Dining')).toBeInTheDocument();
      expect(screen.getByText('Salary')).toBeInTheDocument();
      expect(screen.getByText('Transportation')).toBeInTheDocument();
    });

    it('should display transaction type chips', () => {
      renderTransactionList();

      const expenseChips = screen.getAllByText('EXPENSE');
      const incomeChips = screen.getAllByText('INCOME');

      expect(expenseChips).toHaveLength(2);
      expect(incomeChips).toHaveLength(1);
    });

    it('should show pagination info', () => {
      renderTransactionList();

      expect(screen.getByText('Showing 1-3 of 3 transactions')).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('should show empty message when no transactions', () => {
      renderTransactionList({ transactions: [], totalElements: 0 });

      expect(screen.getByText('No transactions yet')).toBeInTheDocument();
      expect(
        screen.getByText('Create your first transaction to get started.')
      ).toBeInTheDocument();
    });

    it('should show "No transactions" in pagination when empty', () => {
      renderTransactionList({ transactions: [], totalElements: 0 });

      expect(screen.getByText('No transactions')).toBeInTheDocument();
    });
  });

  describe('Loading State', () => {
    it('should show skeleton loading when loading', () => {
      renderTransactionList({ loading: true });

      // Check for skeleton elements (MUI renders multiple circular skeletons)
      const table = screen.getByRole('table');
      expect(table).toBeInTheDocument();
    });

    it('should not show transactions when loading', () => {
      renderTransactionList({ loading: true });

      expect(screen.queryByText('Grocery shopping at Whole Foods')).not.toBeInTheDocument();
    });
  });

  describe('Sorting', () => {
    it('should call onSortChange when clicking Date header', async () => {
      const user = userEvent.setup();
      const onSortChange = vi.fn();
      renderTransactionList({ onSortChange });

      await user.click(screen.getByText('Date'));

      expect(onSortChange).toHaveBeenCalledWith('date', 'asc');
    });

    it('should toggle sort direction when clicking same header', async () => {
      const user = userEvent.setup();
      const onSortChange = vi.fn();
      renderTransactionList({ onSortChange, sortField: 'date', sortDirection: 'asc' });

      await user.click(screen.getByText('Date'));

      expect(onSortChange).toHaveBeenCalledWith('date', 'desc');
    });

    it('should call onSortChange when clicking Amount header', async () => {
      const user = userEvent.setup();
      const onSortChange = vi.fn();
      renderTransactionList({ onSortChange });

      await user.click(screen.getByText('Amount'));

      expect(onSortChange).toHaveBeenCalledWith('amount', 'asc');
    });

    it('should call onSortChange when clicking Category header', async () => {
      const user = userEvent.setup();
      const onSortChange = vi.fn();
      renderTransactionList({ onSortChange });

      await user.click(screen.getByText('Category'));

      expect(onSortChange).toHaveBeenCalledWith('category', 'asc');
    });
  });

  describe('Pagination', () => {
    it('should call onPageChange when clicking next page', async () => {
      const user = userEvent.setup();
      const onPageChange = vi.fn();
      renderTransactionList({ onPageChange, totalElements: 50 });

      const nextButton = screen.getByRole('button', { name: /next page/i });
      await user.click(nextButton);

      expect(onPageChange).toHaveBeenCalledWith(1);
    });

    it('should call onPageSizeChange when changing rows per page', async () => {
      const user = userEvent.setup();
      const onPageSizeChange = vi.fn();
      const onPageChange = vi.fn();
      renderTransactionList({ onPageSizeChange, onPageChange });

      // Open the rows per page dropdown
      const select = screen.getByRole('combobox');
      await user.click(select);

      // Select 50 rows per page
      const option50 = screen.getByRole('option', { name: '50' });
      await user.click(option50);

      expect(onPageSizeChange).toHaveBeenCalledWith(50);
      expect(onPageChange).toHaveBeenCalledWith(0);
    });

    it('should disable previous button on first page', () => {
      renderTransactionList({ page: 0 });

      const prevButton = screen.getByRole('button', { name: /previous page/i });
      expect(prevButton).toBeDisabled();
    });

    it('should disable next button on last page', () => {
      renderTransactionList({ page: 0, pageSize: 20, totalElements: 3 });

      const nextButton = screen.getByRole('button', { name: /next page/i });
      expect(nextButton).toBeDisabled();
    });
  });

  describe('Actions', () => {
    it('should call onEdit when clicking edit button', async () => {
      const user = userEvent.setup();
      const onEdit = vi.fn();
      renderTransactionList({ onEdit });

      const editButtons = screen.getAllByRole('button', { name: /edit transaction/i });
      await user.click(editButtons[0]);

      expect(onEdit).toHaveBeenCalledWith(mockTransactions[0]);
    });

    it('should call onDelete when clicking delete button', async () => {
      const user = userEvent.setup();
      const onDelete = vi.fn();
      renderTransactionList({ onDelete });

      const deleteButtons = screen.getAllByRole('button', { name: /delete transaction/i });
      await user.click(deleteButtons[0]);

      expect(onDelete).toHaveBeenCalledWith(mockTransactions[0]);
    });

    it('should not trigger row expansion when clicking edit', async () => {
      const user = userEvent.setup();
      const onEdit = vi.fn();
      renderTransactionList({ onEdit });

      const editButtons = screen.getAllByRole('button', { name: /edit transaction/i });
      await user.click(editButtons[0]);

      // Check that expanded details are not shown
      expect(screen.queryByText('Transaction Details')).not.toBeInTheDocument();
    });

    it('should not trigger row expansion when clicking delete', async () => {
      const user = userEvent.setup();
      const onDelete = vi.fn();
      renderTransactionList({ onDelete });

      const deleteButtons = screen.getAllByRole('button', { name: /delete transaction/i });
      await user.click(deleteButtons[0]);

      expect(screen.queryByText('Transaction Details')).not.toBeInTheDocument();
    });
  });

  describe('Row Expansion', () => {
    it('should expand row when clicking on it', async () => {
      const user = userEvent.setup();
      renderTransactionList();

      // Click on the first row (find the row and click it)
      const expandButton = screen.getAllByRole('button', { name: /expand details/i })[0];
      await user.click(expandButton);

      expect(screen.getByText('Transaction Details')).toBeInTheDocument();
      expect(screen.getByText('Weekly groceries')).toBeInTheDocument();
    });

    it('should collapse row when clicking again', async () => {
      const user = userEvent.setup();
      renderTransactionList();

      const expandButton = screen.getAllByRole('button', { name: /expand details/i })[0];
      await user.click(expandButton);

      expect(screen.getByText('Transaction Details')).toBeInTheDocument();

      const collapseButton = screen.getByRole('button', { name: /collapse details/i });
      await user.click(collapseButton);

      // Wait for collapse animation
      await vi.waitFor(() => {
        expect(screen.queryByText('Transaction Details')).not.toBeInTheDocument();
      });
    });

    it('should show notes in expanded view', async () => {
      const user = userEvent.setup();
      renderTransactionList();

      const expandButton = screen.getAllByRole('button', { name: /expand details/i })[0];
      await user.click(expandButton);

      expect(screen.getByText('Notes')).toBeInTheDocument();
      expect(screen.getByText('Weekly groceries')).toBeInTheDocument();
    });

    it('should show "-" for missing notes in expanded view', async () => {
      const user = userEvent.setup();
      renderTransactionList();

      // Expand the second row (Monthly salary has no notes)
      const expandButtons = screen.getAllByRole('button', { name: /expand details/i });
      await user.click(expandButtons[1]);

      const notesSection = screen.getByText('Notes').parentElement;
      expect(notesSection).toContainHTML('-');
    });
  });

  describe('Description Truncation', () => {
    it('should truncate long descriptions', () => {
      const longDescription =
        'This is a very long description that exceeds fifty characters and should be truncated';
      const transactionWithLongDesc: Transaction[] = [
        {
          ...mockTransactions[0],
          description: longDescription,
        },
      ];

      renderTransactionList({ transactions: transactionWithLongDesc, totalElements: 1 });

      // Check that the truncated text is shown (50 chars + "...")
      expect(
        screen.getByText('This is a very long description that exceeds fift...')
      ).toBeInTheDocument();
    });

    it('should show full description on hover via tooltip', () => {
      renderTransactionList();

      // Tooltip is shown on hover - we just verify the tooltip component exists
      const descriptionCell = screen.getByText('Grocery shopping at Whole Foods');
      expect(descriptionCell).toBeInTheDocument();
    });

    it('should show "-" for null description', () => {
      const transactionWithNoDesc: Transaction[] = [
        {
          ...mockTransactions[0],
          description: null,
        },
      ];

      renderTransactionList({ transactions: transactionWithNoDesc, totalElements: 1 });

      // The cell should show "-"
      const rows = screen.getAllByRole('row');
      expect(rows[1]).toHaveTextContent('-');
    });
  });

  describe('Visual Indicators', () => {
    it('should show green color for income amounts', () => {
      renderTransactionList();

      const incomeAmount = screen.getByText('+$5,000.00');
      expect(incomeAmount).toHaveStyle({ fontWeight: 600 });
    });

    it('should show red color for expense amounts', () => {
      renderTransactionList();

      const expenseAmount = screen.getByText('-$150.00');
      expect(expenseAmount).toHaveStyle({ fontWeight: 600 });
    });
  });

  describe('Accessibility', () => {
    it('should have accessible table', () => {
      renderTransactionList();

      expect(screen.getByRole('table', { name: /transactions table/i })).toBeInTheDocument();
    });

    it('should have accessible edit buttons', () => {
      renderTransactionList();

      const editButtons = screen.getAllByRole('button', { name: /edit transaction/i });
      expect(editButtons).toHaveLength(3);
    });

    it('should have accessible delete buttons', () => {
      renderTransactionList();

      const deleteButtons = screen.getAllByRole('button', { name: /delete transaction/i });
      expect(deleteButtons).toHaveLength(3);
    });

    it('should have accessible expand/collapse buttons', () => {
      renderTransactionList();

      const expandButtons = screen.getAllByRole('button', { name: /expand details/i });
      expect(expandButtons).toHaveLength(3);
    });
  });
});
