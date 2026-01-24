import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import TransactionForm from './TransactionForm';
import type { Transaction, CreateTransactionRequest } from '@/types';

const theme = createTheme();

// Mock the CategorySelect component
vi.mock('./CategorySelect', () => ({
  default: ({
    transactionType,
    value,
    onChange,
    error,
    helperText,
  }: {
    transactionType: string;
    value: string;
    onChange: (id: string) => void;
    error: boolean;
    helperText?: string;
  }) => (
    <div data-testid="category-select">
      <select
        data-testid="category-dropdown"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label="Category"
      >
        <option value="">Select category</option>
        {transactionType === 'EXPENSE' ? (
          <>
            <option value="cat-food">Food & Dining</option>
            <option value="cat-transport">Transportation</option>
          </>
        ) : (
          <>
            <option value="cat-salary">Salary</option>
            <option value="cat-freelance">Freelance</option>
          </>
        )}
      </select>
      {error && <span data-testid="category-error">{helperText}</span>}
    </div>
  ),
}));

const mockTransaction: Transaction = {
  id: '1',
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
  notes: 'Weekly groceries',
  createdAt: '2026-01-20T14:30:00Z',
  updatedAt: '2026-01-20T14:30:00Z',
};

const defaultProps = {
  transaction: null,
  onSubmit: vi.fn(),
  onCancel: vi.fn(),
  loading: false,
  error: null,
};

const renderTransactionForm = (props = {}) => {
  return render(
    <ThemeProvider theme={theme}>
      <TransactionForm {...defaultProps} {...props} />
    </ThemeProvider>
  );
};

describe('TransactionForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering - Create Mode', () => {
    it('should render all form fields', () => {
      renderTransactionForm();

      expect(screen.getByLabelText(/expense/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/income/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/amount/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/date/i)).toBeInTheDocument();
      expect(screen.getByTestId('category-select')).toBeInTheDocument();
      expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/notes/i)).toBeInTheDocument();
    });

    it('should default to EXPENSE type', () => {
      renderTransactionForm();

      const expenseRadio = screen.getByLabelText(/expense/i) as HTMLInputElement;
      const incomeRadio = screen.getByLabelText(/income/i) as HTMLInputElement;

      expect(expenseRadio.checked).toBe(true);
      expect(incomeRadio.checked).toBe(false);
    });

    it('should default date to today', () => {
      renderTransactionForm();

      const dateInput = screen.getByLabelText(/date/i) as HTMLInputElement;
      const today = new Date().toISOString().split('T')[0];

      expect(dateInput.value).toBe(today);
    });

    it('should show Create Transaction button', () => {
      renderTransactionForm();

      expect(screen.getByRole('button', { name: /create transaction/i })).toBeInTheDocument();
    });

    it('should show Cancel button', () => {
      renderTransactionForm();

      expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
    });
  });

  describe('Rendering - Edit Mode', () => {
    it('should pre-fill form with transaction data', () => {
      renderTransactionForm({ transaction: mockTransaction });

      const amountInput = screen.getByLabelText(/amount/i) as HTMLInputElement;
      expect(amountInput.value).toBe('150');

      const expenseRadio = screen.getByLabelText(/expense/i) as HTMLInputElement;
      expect(expenseRadio.checked).toBe(true);

      const dateInput = screen.getByLabelText(/date/i) as HTMLInputElement;
      expect(dateInput.value).toBe('2026-01-20');

      const descriptionInput = screen.getByLabelText(/description/i) as HTMLInputElement;
      expect(descriptionInput.value).toBe('Grocery shopping');

      const notesInput = screen.getByLabelText(/notes/i) as HTMLInputElement;
      expect(notesInput.value).toBe('Weekly groceries');
    });

    it('should show Update Transaction button in edit mode', () => {
      renderTransactionForm({ transaction: mockTransaction });

      expect(screen.getByRole('button', { name: /update transaction/i })).toBeInTheDocument();
    });
  });

  describe('Validation', () => {
    it('should show error when amount is empty', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      // Fill in other required fields
      await user.type(screen.getByLabelText(/date/i), '2026-01-20');

      // Try to submit
      await user.click(screen.getByRole('button', { name: /create transaction/i }));

      await waitFor(() => {
        expect(screen.getByText(/amount is required/i)).toBeInTheDocument();
      });
    });

    it('should show error when amount is zero', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      const amountInput = screen.getByLabelText(/amount/i);
      await user.type(amountInput, '0');

      await user.click(screen.getByRole('button', { name: /create transaction/i }));

      await waitFor(() => {
        expect(screen.getByText(/amount must be greater than 0/i)).toBeInTheDocument();
      });
    });

    it('should show error when amount is negative', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      const amountInput = screen.getByLabelText(/amount/i);
      await user.type(amountInput, '-100');

      await user.click(screen.getByRole('button', { name: /create transaction/i }));

      await waitFor(() => {
        expect(screen.getByText(/amount must be greater than 0/i)).toBeInTheDocument();
      });
    });

    it('should show error when amount exceeds maximum', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      const amountInput = screen.getByLabelText(/amount/i);
      await user.type(amountInput, '10000001');

      await user.click(screen.getByRole('button', { name: /create transaction/i }));

      await waitFor(() => {
        expect(screen.getByText(/must not exceed/i)).toBeInTheDocument();
      });
    });

    it('should show error when category is not selected', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      const amountInput = screen.getByLabelText(/amount/i);
      await user.type(amountInput, '100');

      await user.click(screen.getByRole('button', { name: /create transaction/i }));

      await waitFor(() => {
        expect(screen.getByTestId('category-error')).toBeInTheDocument();
      });
    });

    it('should show error when description exceeds 500 characters', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      const longDescription = 'a'.repeat(501);
      const descriptionInput = screen.getByLabelText(/description/i);
      await user.type(descriptionInput, longDescription);

      await user.click(screen.getByRole('button', { name: /create transaction/i }));

      await waitFor(() => {
        expect(screen.getByText(/description must be 500 characters or less/i)).toBeInTheDocument();
      });
    });

    it('should show error when notes exceeds 1000 characters', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      const longNotes = 'a'.repeat(1001);
      const notesInput = screen.getByLabelText(/notes/i);
      await user.type(notesInput, longNotes);

      await user.click(screen.getByRole('button', { name: /create transaction/i }));

      await waitFor(() => {
        expect(screen.getByText(/notes must be 1000 characters or less/i)).toBeInTheDocument();
      });
    });

    it('should show warning for future date', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      const dateInput = screen.getByLabelText(/date/i);
      await user.clear(dateInput);
      await user.type(dateInput, '2030-12-31');

      await waitFor(() => {
        expect(screen.getByText(/transaction date is in the future/i)).toBeInTheDocument();
      });
    });
  });

  describe('Form Submission', () => {
    it('should call onSubmit with correct data for create', async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn().mockResolvedValue(undefined);
      renderTransactionForm({ onSubmit });

      // Fill in the form
      await user.type(screen.getByLabelText(/amount/i), '150');
      await user.clear(screen.getByLabelText(/date/i));
      await user.type(screen.getByLabelText(/date/i), '2026-01-20');

      // Select category
      const categoryDropdown = screen.getByTestId('category-dropdown');
      await user.selectOptions(categoryDropdown, 'cat-food');

      await user.type(screen.getByLabelText(/description/i), 'Test description');
      await user.type(screen.getByLabelText(/notes/i), 'Test notes');

      // Submit
      await user.click(screen.getByRole('button', { name: /create transaction/i }));

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith({
          amount: 150,
          type: 'EXPENSE',
          categoryId: 'cat-food',
          date: '2026-01-20',
          description: 'Test description',
          notes: 'Test notes',
        });
      });
    });

    it('should call onSubmit with null for empty optional fields', async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn().mockResolvedValue(undefined);
      renderTransactionForm({ onSubmit });

      // Fill in required fields only
      await user.type(screen.getByLabelText(/amount/i), '100');
      await user.clear(screen.getByLabelText(/date/i));
      await user.type(screen.getByLabelText(/date/i), '2026-01-20');

      const categoryDropdown = screen.getByTestId('category-dropdown');
      await user.selectOptions(categoryDropdown, 'cat-food');

      await user.click(screen.getByRole('button', { name: /create transaction/i }));

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith({
          amount: 100,
          type: 'EXPENSE',
          categoryId: 'cat-food',
          date: '2026-01-20',
          description: null,
          notes: null,
        });
      });
    });

    it('should call onCancel when clicking cancel button', async () => {
      const user = userEvent.setup();
      const onCancel = vi.fn();
      renderTransactionForm({ onCancel });

      await user.click(screen.getByRole('button', { name: /cancel/i }));

      expect(onCancel).toHaveBeenCalled();
    });
  });

  describe('Type Switching', () => {
    it('should switch to INCOME type when selected', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      await user.click(screen.getByLabelText(/income/i));

      const incomeRadio = screen.getByLabelText(/income/i) as HTMLInputElement;
      expect(incomeRadio.checked).toBe(true);

      // Category options should change to income categories
      const categoryDropdown = screen.getByTestId('category-dropdown');
      expect(categoryDropdown).toContainHTML('Salary');
      expect(categoryDropdown).toContainHTML('Freelance');
    });

    it('should switch back to EXPENSE type', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      await user.click(screen.getByLabelText(/income/i));
      await user.click(screen.getByLabelText(/expense/i));

      const expenseRadio = screen.getByLabelText(/expense/i) as HTMLInputElement;
      expect(expenseRadio.checked).toBe(true);
    });
  });

  describe('Loading State', () => {
    it('should disable form during loading', () => {
      renderTransactionForm({ loading: true });

      expect(screen.getByLabelText(/amount/i)).toBeDisabled();
      expect(screen.getByRole('button', { name: /creating/i })).toBeDisabled();
      expect(screen.getByRole('button', { name: /cancel/i })).toBeDisabled();
    });

    it('should show Creating... text during create submission', () => {
      renderTransactionForm({ loading: true });

      expect(screen.getByRole('button', { name: /creating/i })).toBeInTheDocument();
    });

    it('should show Updating... text during update submission', () => {
      renderTransactionForm({ loading: true, transaction: mockTransaction });

      expect(screen.getByRole('button', { name: /updating/i })).toBeInTheDocument();
    });
  });

  describe('Error Display', () => {
    it('should show error alert when error prop is set', () => {
      renderTransactionForm({ error: 'Failed to create transaction' });

      expect(screen.getByRole('alert')).toHaveTextContent('Failed to create transaction');
    });

    it('should not show error alert when error prop is null', () => {
      renderTransactionForm({ error: null });

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  describe('Character Counters', () => {
    it('should show character counter for description', () => {
      renderTransactionForm();

      expect(screen.getByText('0/500')).toBeInTheDocument();
    });

    it('should show character counter for notes', () => {
      renderTransactionForm();

      expect(screen.getByText('0/1000')).toBeInTheDocument();
    });

    it('should update character counter as user types', async () => {
      const user = userEvent.setup();
      renderTransactionForm();

      const descriptionInput = screen.getByLabelText(/description/i);
      await user.type(descriptionInput, 'Test');

      expect(screen.getByText('4/500')).toBeInTheDocument();
    });
  });

  describe('Edit Mode - No Changes', () => {
    it('should disable submit button when no changes made in edit mode', () => {
      renderTransactionForm({ transaction: mockTransaction });

      const submitButton = screen.getByRole('button', { name: /update transaction/i });
      expect(submitButton).toBeDisabled();
    });

    it('should enable submit button when changes are made in edit mode', async () => {
      const user = userEvent.setup();
      renderTransactionForm({ transaction: mockTransaction });

      const amountInput = screen.getByLabelText(/amount/i);
      await user.clear(amountInput);
      await user.type(amountInput, '200');

      await waitFor(() => {
        const submitButton = screen.getByRole('button', { name: /update transaction/i });
        expect(submitButton).not.toBeDisabled();
      });
    });
  });
});
