import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import { transactionApi } from './transactionApi';
import type {
  Transaction,
  TransactionListResponse,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  TransactionFilter,
  TransactionSummaryResponse,
} from '@/types';

// Mock axios
vi.mock('./axiosConfig', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('transactionApi', () => {
  let mockApi: {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    put: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    const api = await import('./axiosConfig');
    mockApi = api.default as typeof mockApi;
  });

  const mockTransaction: Transaction = {
    id: 'tx-1',
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
    description: 'Grocery shopping',
    notes: 'Weekly groceries',
    createdAt: '2026-01-20T14:30:00Z',
    updatedAt: '2026-01-20T14:30:00Z',
  };

  const mockTransactionListResponse: TransactionListResponse = {
    content: [mockTransaction],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
    summary: {
      totalIncome: 0,
      totalExpenses: 150.0,
      netBalance: -150.0,
    },
  };

  const mockSummaryResponse: TransactionSummaryResponse = {
    period: {
      startDate: '2026-01-01',
      endDate: '2026-01-31',
    },
    totals: {
      income: 5000.0,
      expenses: 3250.5,
      net: 1749.5,
      transactionCount: 47,
    },
    categoryBreakdown: {
      income: [],
      expense: [],
    },
  };

  describe('getTransactions', () => {
    it('should fetch transactions without filters', async () => {
      mockApi.get.mockResolvedValue({ data: mockTransactionListResponse });

      const result = await transactionApi.getTransactions();

      expect(mockApi.get).toHaveBeenCalledWith('/transactions');
      expect(result).toEqual(mockTransactionListResponse);
    });

    it('should fetch transactions with all filters', async () => {
      mockApi.get.mockResolvedValue({ data: mockTransactionListResponse });

      const filter: TransactionFilter = {
        startDate: '2026-01-01',
        endDate: '2026-01-31',
        type: 'EXPENSE',
        categoryIds: ['cat-1', 'cat-2'],
        minAmount: 50,
        maxAmount: 500,
        search: 'grocery',
        page: 0,
        size: 20,
        sort: 'date,desc',
      };

      const result = await transactionApi.getTransactions(filter);

      expect(mockApi.get).toHaveBeenCalledWith(
        expect.stringContaining('/transactions?')
      );
      const callUrl = mockApi.get.mock.calls[0][0];
      expect(callUrl).toContain('startDate=2026-01-01');
      expect(callUrl).toContain('endDate=2026-01-31');
      expect(callUrl).toContain('type=EXPENSE');
      expect(callUrl).toContain('categoryIds=cat-1%2Ccat-2');
      expect(callUrl).toContain('minAmount=50');
      expect(callUrl).toContain('maxAmount=500');
      expect(callUrl).toContain('search=grocery');
      expect(callUrl).toContain('page=0');
      expect(callUrl).toContain('size=20');
      expect(callUrl).toContain('sort=date%2Cdesc');
      expect(result).toEqual(mockTransactionListResponse);
    });

    it('should handle empty filter values', async () => {
      mockApi.get.mockResolvedValue({ data: mockTransactionListResponse });

      const filter: TransactionFilter = {
        startDate: undefined,
        type: undefined,
        page: 0,
      };

      await transactionApi.getTransactions(filter);

      const callUrl = mockApi.get.mock.calls[0][0];
      expect(callUrl).not.toContain('startDate');
      expect(callUrl).not.toContain('type');
      expect(callUrl).toContain('page=0');
    });

    it('should handle API errors', async () => {
      const error = new Error('Network error');
      mockApi.get.mockRejectedValue(error);

      await expect(transactionApi.getTransactions()).rejects.toThrow('Network error');
    });
  });

  describe('getTransaction', () => {
    it('should fetch a single transaction by ID', async () => {
      mockApi.get.mockResolvedValue({ data: mockTransaction });

      const result = await transactionApi.getTransaction('tx-1');

      expect(mockApi.get).toHaveBeenCalledWith('/transactions/tx-1');
      expect(result).toEqual(mockTransaction);
    });

    it('should handle not found error', async () => {
      const error = { response: { status: 404, data: { message: 'Transaction not found' } } };
      mockApi.get.mockRejectedValue(error);

      await expect(transactionApi.getTransaction('invalid-id')).rejects.toEqual(error);
    });
  });

  describe('createTransaction', () => {
    it('should create a new transaction', async () => {
      mockApi.post.mockResolvedValue({ data: mockTransaction });

      const request: CreateTransactionRequest = {
        amount: 150.0,
        type: 'EXPENSE',
        categoryId: 'cat-1',
        date: '2026-01-20',
        description: 'Grocery shopping',
        notes: 'Weekly groceries',
      };

      const result = await transactionApi.createTransaction(request);

      expect(mockApi.post).toHaveBeenCalledWith('/transactions', request);
      expect(result).toEqual(mockTransaction);
    });

    it('should create transaction with null optional fields', async () => {
      mockApi.post.mockResolvedValue({ data: mockTransaction });

      const request: CreateTransactionRequest = {
        amount: 100.0,
        type: 'EXPENSE',
        categoryId: 'cat-1',
        date: '2026-01-20',
        description: null,
        notes: null,
      };

      await transactionApi.createTransaction(request);

      expect(mockApi.post).toHaveBeenCalledWith('/transactions', request);
    });

    it('should handle validation errors', async () => {
      const error = {
        response: {
          status: 400,
          data: {
            error: 'VALIDATION_ERROR',
            fieldErrors: [{ field: 'amount', message: 'Amount must be greater than 0' }],
          },
        },
      };
      mockApi.post.mockRejectedValue(error);

      await expect(
        transactionApi.createTransaction({
          amount: -100,
          type: 'EXPENSE',
          categoryId: 'cat-1',
          date: '2026-01-20',
        })
      ).rejects.toEqual(error);
    });
  });

  describe('updateTransaction', () => {
    it('should update an existing transaction', async () => {
      const updatedTransaction = { ...mockTransaction, amount: 200.0 };
      mockApi.put.mockResolvedValue({ data: updatedTransaction });

      const request: UpdateTransactionRequest = {
        amount: 200.0,
        type: 'EXPENSE',
        categoryId: 'cat-1',
        date: '2026-01-20',
        description: 'Updated description',
        notes: null,
      };

      const result = await transactionApi.updateTransaction('tx-1', request);

      expect(mockApi.put).toHaveBeenCalledWith('/transactions/tx-1', request);
      expect(result.amount).toBe(200.0);
    });

    it('should handle not found on update', async () => {
      const error = { response: { status: 404, data: { message: 'Transaction not found' } } };
      mockApi.put.mockRejectedValue(error);

      await expect(
        transactionApi.updateTransaction('invalid-id', {
          amount: 100,
          type: 'EXPENSE',
          categoryId: 'cat-1',
          date: '2026-01-20',
        })
      ).rejects.toEqual(error);
    });
  });

  describe('deleteTransaction', () => {
    it('should delete a transaction', async () => {
      mockApi.delete.mockResolvedValue({ data: null });

      await transactionApi.deleteTransaction('tx-1');

      expect(mockApi.delete).toHaveBeenCalledWith('/transactions/tx-1');
    });

    it('should handle not found on delete', async () => {
      const error = { response: { status: 404, data: { message: 'Transaction not found' } } };
      mockApi.delete.mockRejectedValue(error);

      await expect(transactionApi.deleteTransaction('invalid-id')).rejects.toEqual(error);
    });
  });

  describe('getSummary', () => {
    it('should fetch transaction summary without dates', async () => {
      mockApi.get.mockResolvedValue({ data: mockSummaryResponse });

      const result = await transactionApi.getSummary();

      expect(mockApi.get).toHaveBeenCalledWith('/transactions/summary');
      expect(result).toEqual(mockSummaryResponse);
    });

    it('should fetch transaction summary with date range', async () => {
      mockApi.get.mockResolvedValue({ data: mockSummaryResponse });

      const result = await transactionApi.getSummary('2026-01-01', '2026-01-31');

      expect(mockApi.get).toHaveBeenCalledWith(
        '/transactions/summary?startDate=2026-01-01&endDate=2026-01-31'
      );
      expect(result).toEqual(mockSummaryResponse);
    });

    it('should fetch summary with only start date', async () => {
      mockApi.get.mockResolvedValue({ data: mockSummaryResponse });

      await transactionApi.getSummary('2026-01-01');

      expect(mockApi.get).toHaveBeenCalledWith('/transactions/summary?startDate=2026-01-01');
    });

    it('should fetch summary with only end date', async () => {
      mockApi.get.mockResolvedValue({ data: mockSummaryResponse });

      await transactionApi.getSummary(undefined, '2026-01-31');

      expect(mockApi.get).toHaveBeenCalledWith('/transactions/summary?endDate=2026-01-31');
    });
  });

  describe('Query Parameter Building', () => {
    it('should not include undefined values in query string', async () => {
      mockApi.get.mockResolvedValue({ data: mockTransactionListResponse });

      await transactionApi.getTransactions({
        type: 'EXPENSE',
        startDate: undefined,
        endDate: undefined,
        categoryIds: undefined,
      });

      const callUrl = mockApi.get.mock.calls[0][0];
      expect(callUrl).toBe('/transactions?type=EXPENSE');
    });

    it('should handle empty categoryIds array', async () => {
      mockApi.get.mockResolvedValue({ data: mockTransactionListResponse });

      await transactionApi.getTransactions({
        categoryIds: [],
      });

      const callUrl = mockApi.get.mock.calls[0][0];
      expect(callUrl).not.toContain('categoryIds');
    });

    it('should handle null amount values', async () => {
      mockApi.get.mockResolvedValue({ data: mockTransactionListResponse });

      await transactionApi.getTransactions({
        minAmount: null as unknown as undefined,
        maxAmount: undefined,
      });

      const callUrl = mockApi.get.mock.calls[0][0];
      expect(callUrl).not.toContain('minAmount');
      expect(callUrl).not.toContain('maxAmount');
    });

    it('should handle zero amount values', async () => {
      mockApi.get.mockResolvedValue({ data: mockTransactionListResponse });

      await transactionApi.getTransactions({
        minAmount: 0,
        maxAmount: 0,
      });

      const callUrl = mockApi.get.mock.calls[0][0];
      expect(callUrl).toContain('minAmount=0');
      expect(callUrl).toContain('maxAmount=0');
    });

    it('should encode special characters in search', async () => {
      mockApi.get.mockResolvedValue({ data: mockTransactionListResponse });

      await transactionApi.getTransactions({
        search: 'coffee & tea',
      });

      const callUrl = mockApi.get.mock.calls[0][0];
      expect(callUrl).toContain('search=coffee+%26+tea');
    });
  });
});
