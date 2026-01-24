import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import TransactionFilter from './TransactionFilter';
import type { TransactionFilter as TransactionFilterType } from '@/types';

const theme = createTheme();

// Mock the categoryApi
vi.mock('@/api', () => ({
  categoryApi: {
    getCategories: vi.fn().mockResolvedValue({
      income: [
        { id: 'cat-salary', name: 'Salary', type: 'INCOME', icon: 'payments', color: '#4CAF50' },
        { id: 'cat-freelance', name: 'Freelance', type: 'INCOME', icon: 'work', color: '#8BC34A' },
      ],
      expense: [
        { id: 'cat-food', name: 'Food & Dining', type: 'EXPENSE', icon: 'restaurant', color: '#F44336' },
        { id: 'cat-transport', name: 'Transportation', type: 'EXPENSE', icon: 'directions_car', color: '#E91E63' },
      ],
    }),
  },
  getErrorMessage: vi.fn((err) => err?.message || 'An error occurred'),
}));

const emptyFilter: TransactionFilterType = {};

const defaultProps = {
  filter: emptyFilter,
  onFilterChange: vi.fn(),
  onClear: vi.fn(),
};

const renderTransactionFilter = (props = {}) => {
  return render(
    <ThemeProvider theme={theme}>
      <TransactionFilter {...defaultProps} {...props} />
    </ThemeProvider>
  );
};

describe('TransactionFilter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render filter header', () => {
      renderTransactionFilter();

      expect(screen.getByText('Filters')).toBeInTheDocument();
    });

    it('should be collapsed by default', () => {
      renderTransactionFilter();

      // Filter content should not be visible
      expect(screen.queryByLabelText(/date range/i)).not.toBeInTheDocument();
    });

    it('should expand when clicking header', async () => {
      const user = userEvent.setup();
      renderTransactionFilter();

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/date range/i)).toBeInTheDocument();
      });
    });

    it('should show all filter options when expanded', async () => {
      const user = userEvent.setup();
      renderTransactionFilter();

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/date range/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/type/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/categories/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/min amount/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/max amount/i)).toBeInTheDocument();
      });
    });

    it('should show Clear Filters button when expanded', async () => {
      const user = userEvent.setup();
      renderTransactionFilter();

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /clear filters/i })).toBeInTheDocument();
      });
    });
  });

  describe('Active Filter Count', () => {
    it('should not show badge when no filters are active', () => {
      renderTransactionFilter({ filter: {} });

      expect(screen.queryByText('0')).not.toBeInTheDocument();
    });

    it('should show badge with count when filters are active', () => {
      renderTransactionFilter({
        filter: {
          startDate: '2026-01-01',
          type: 'EXPENSE',
        },
      });

      // Should show count of 2 active filters
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    it('should count category filter as one even with multiple categories', () => {
      renderTransactionFilter({
        filter: {
          categoryIds: ['cat-food', 'cat-transport'],
        },
      });

      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });

  describe('Date Range Filter', () => {
    it('should call onFilterChange when selecting date preset', async () => {
      const user = userEvent.setup();
      const onFilterChange = vi.fn();
      renderTransactionFilter({ onFilterChange });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/date range/i)).toBeInTheDocument();
      });

      // Open date range dropdown
      await user.click(screen.getByLabelText(/date range/i));
      await user.click(screen.getByRole('option', { name: /last 7 days/i }));

      expect(onFilterChange).toHaveBeenCalled();
      const callArgs = onFilterChange.mock.calls[0][0];
      expect(callArgs.startDate).toBeDefined();
      expect(callArgs.endDate).toBeDefined();
    });

    it('should allow custom date range', async () => {
      const user = userEvent.setup();
      const onFilterChange = vi.fn();
      renderTransactionFilter({ onFilterChange });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
      });

      const startDateInput = screen.getByLabelText(/start date/i);
      await user.type(startDateInput, '2026-01-01');

      expect(onFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({
          startDate: '2026-01-01',
        })
      );
    });

    it('should update end date filter', async () => {
      const user = userEvent.setup();
      const onFilterChange = vi.fn();
      renderTransactionFilter({ onFilterChange });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
      });

      const endDateInput = screen.getByLabelText(/end date/i);
      await user.type(endDateInput, '2026-01-31');

      expect(onFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({
          endDate: '2026-01-31',
        })
      );
    });
  });

  describe('Type Filter', () => {
    it('should call onFilterChange when selecting type', async () => {
      const user = userEvent.setup();
      const onFilterChange = vi.fn();
      renderTransactionFilter({ onFilterChange });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/type/i)).toBeInTheDocument();
      });

      await user.click(screen.getByLabelText(/type/i));
      await user.click(screen.getByRole('option', { name: /expense/i }));

      expect(onFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'EXPENSE',
          categoryIds: [], // Categories reset when type changes
        })
      );
    });

    it('should reset category filter when changing type', async () => {
      const user = userEvent.setup();
      const onFilterChange = vi.fn();
      renderTransactionFilter({
        onFilterChange,
        filter: {
          type: 'EXPENSE',
          categoryIds: ['cat-food'],
        },
      });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/type/i)).toBeInTheDocument();
      });

      await user.click(screen.getByLabelText(/type/i));
      await user.click(screen.getByRole('option', { name: /income/i }));

      expect(onFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'INCOME',
          categoryIds: [],
        })
      );
    });

    it('should clear type filter when selecting All Types', async () => {
      const user = userEvent.setup();
      const onFilterChange = vi.fn();
      renderTransactionFilter({
        onFilterChange,
        filter: { type: 'EXPENSE' },
      });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/type/i)).toBeInTheDocument();
      });

      await user.click(screen.getByLabelText(/type/i));
      await user.click(screen.getByRole('option', { name: /all types/i }));

      expect(onFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({
          type: undefined,
        })
      );
    });
  });

  describe('Category Filter', () => {
    it('should load categories on mount', async () => {
      const user = userEvent.setup();
      renderTransactionFilter();

      await user.click(screen.getByText('Filters'));

      // Wait for categories to load
      await waitFor(() => {
        expect(screen.getByLabelText(/categories/i)).toBeInTheDocument();
      });
    });

    it('should show all categories when no type is selected', async () => {
      const user = userEvent.setup();
      renderTransactionFilter();

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/categories/i)).toBeInTheDocument();
      });

      // Open categories dropdown
      await user.click(screen.getByLabelText(/categories/i));

      await waitFor(() => {
        expect(screen.getByText('Salary')).toBeInTheDocument();
        expect(screen.getByText('Freelance')).toBeInTheDocument();
        expect(screen.getByText('Food & Dining')).toBeInTheDocument();
        expect(screen.getByText('Transportation')).toBeInTheDocument();
      });
    });

    it('should show only expense categories when EXPENSE type is selected', async () => {
      const user = userEvent.setup();
      renderTransactionFilter({ filter: { type: 'EXPENSE' } });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/categories/i)).toBeInTheDocument();
      });

      await user.click(screen.getByLabelText(/categories/i));

      await waitFor(() => {
        expect(screen.getByText('Food & Dining')).toBeInTheDocument();
        expect(screen.getByText('Transportation')).toBeInTheDocument();
        expect(screen.queryByText('Salary')).not.toBeInTheDocument();
      });
    });
  });

  describe('Amount Filter', () => {
    it('should call onFilterChange when setting min amount', async () => {
      const user = userEvent.setup();
      const onFilterChange = vi.fn();
      renderTransactionFilter({ onFilterChange });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/min amount/i)).toBeInTheDocument();
      });

      const minAmountInput = screen.getByLabelText(/min amount/i);
      await user.type(minAmountInput, '50');

      expect(onFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({
          minAmount: 50,
        })
      );
    });

    it('should call onFilterChange when setting max amount', async () => {
      const user = userEvent.setup();
      const onFilterChange = vi.fn();
      renderTransactionFilter({ onFilterChange });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/max amount/i)).toBeInTheDocument();
      });

      const maxAmountInput = screen.getByLabelText(/max amount/i);
      await user.type(maxAmountInput, '500');

      expect(onFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({
          maxAmount: 500,
        })
      );
    });

    it('should clear amount filter when input is emptied', async () => {
      const user = userEvent.setup();
      const onFilterChange = vi.fn();
      renderTransactionFilter({
        onFilterChange,
        filter: { minAmount: 50 },
      });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/min amount/i)).toBeInTheDocument();
      });

      const minAmountInput = screen.getByLabelText(/min amount/i) as HTMLInputElement;
      await user.clear(minAmountInput);

      expect(onFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({
          minAmount: undefined,
        })
      );
    });
  });

  describe('Clear Filters', () => {
    it('should call onClear when clicking Clear Filters button', async () => {
      const user = userEvent.setup();
      const onClear = vi.fn();
      renderTransactionFilter({
        onClear,
        filter: { type: 'EXPENSE' },
      });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /clear filters/i })).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /clear filters/i }));

      expect(onClear).toHaveBeenCalled();
    });

    it('should disable Clear Filters button when no filters are active', async () => {
      const user = userEvent.setup();
      renderTransactionFilter({ filter: {} });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        const clearButton = screen.getByRole('button', { name: /clear filters/i });
        expect(clearButton).toBeDisabled();
      });
    });

    it('should enable Clear Filters button when filters are active', async () => {
      const user = userEvent.setup();
      renderTransactionFilter({ filter: { type: 'EXPENSE' } });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        const clearButton = screen.getByRole('button', { name: /clear filters/i });
        expect(clearButton).not.toBeDisabled();
      });
    });
  });

  describe('Collapse/Expand', () => {
    it('should collapse when clicking header again', async () => {
      const user = userEvent.setup();
      renderTransactionFilter();

      // Expand
      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByLabelText(/date range/i)).toBeInTheDocument();
      });

      // Collapse
      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.queryByLabelText(/date range/i)).not.toBeInTheDocument();
      });
    });

    it('should have accessible expand/collapse button', async () => {
      const user = userEvent.setup();
      renderTransactionFilter();

      expect(screen.getByRole('button', { name: /expand filters/i })).toBeInTheDocument();

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /collapse filters/i })).toBeInTheDocument();
      });
    });
  });

  describe('Preserved Filter Values', () => {
    it('should show current filter values when expanded', async () => {
      const user = userEvent.setup();
      renderTransactionFilter({
        filter: {
          startDate: '2026-01-01',
          endDate: '2026-01-31',
          minAmount: 50,
          maxAmount: 500,
        },
      });

      await user.click(screen.getByText('Filters'));

      await waitFor(() => {
        const startDateInput = screen.getByLabelText(/start date/i) as HTMLInputElement;
        const endDateInput = screen.getByLabelText(/end date/i) as HTMLInputElement;
        const minAmountInput = screen.getByLabelText(/min amount/i) as HTMLInputElement;
        const maxAmountInput = screen.getByLabelText(/max amount/i) as HTMLInputElement;

        expect(startDateInput.value).toBe('2026-01-01');
        expect(endDateInput.value).toBe('2026-01-31');
        expect(minAmountInput.value).toBe('50');
        expect(maxAmountInput.value).toBe('500');
      });
    });
  });
});
