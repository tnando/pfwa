import api from './axiosConfig';
import type {
  Transaction,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  TransactionListResponse,
  TransactionFilter,
  TransactionSummaryResponse,
} from '@/types';

/**
 * Build query string from transaction filter parameters
 */
const buildQueryParams = (filter: TransactionFilter): URLSearchParams => {
  const params = new URLSearchParams();

  if (filter.startDate) {
    params.append('startDate', filter.startDate);
  }
  if (filter.endDate) {
    params.append('endDate', filter.endDate);
  }
  if (filter.type) {
    params.append('type', filter.type);
  }
  if (filter.categoryIds && filter.categoryIds.length > 0) {
    params.append('categoryIds', filter.categoryIds.join(','));
  }
  if (filter.minAmount !== undefined && filter.minAmount !== null) {
    params.append('minAmount', filter.minAmount.toString());
  }
  if (filter.maxAmount !== undefined && filter.maxAmount !== null) {
    params.append('maxAmount', filter.maxAmount.toString());
  }
  if (filter.search) {
    params.append('search', filter.search);
  }
  if (filter.page !== undefined) {
    params.append('page', filter.page.toString());
  }
  if (filter.size !== undefined) {
    params.append('size', filter.size.toString());
  }
  if (filter.sort) {
    params.append('sort', filter.sort);
  }

  return params;
};

/**
 * Transaction API service
 */
export const transactionApi = {
  /**
   * Get paginated list of transactions with optional filtering
   */
  getTransactions: async (filter: TransactionFilter = {}): Promise<TransactionListResponse> => {
    const params = buildQueryParams(filter);
    const queryString = params.toString();
    const url = queryString ? `/transactions?${queryString}` : '/transactions';
    const response = await api.get<TransactionListResponse>(url);
    return response.data;
  },

  /**
   * Get a single transaction by ID
   */
  getTransaction: async (id: string): Promise<Transaction> => {
    const response = await api.get<Transaction>(`/transactions/${id}`);
    return response.data;
  },

  /**
   * Create a new transaction
   */
  createTransaction: async (data: CreateTransactionRequest): Promise<Transaction> => {
    const response = await api.post<Transaction>('/transactions', data);
    return response.data;
  },

  /**
   * Update an existing transaction
   */
  updateTransaction: async (id: string, data: UpdateTransactionRequest): Promise<Transaction> => {
    const response = await api.put<Transaction>(`/transactions/${id}`, data);
    return response.data;
  },

  /**
   * Delete a transaction
   */
  deleteTransaction: async (id: string): Promise<void> => {
    await api.delete(`/transactions/${id}`);
  },

  /**
   * Get transaction summary and statistics
   */
  getSummary: async (startDate?: string, endDate?: string): Promise<TransactionSummaryResponse> => {
    const params = new URLSearchParams();
    if (startDate) {
      params.append('startDate', startDate);
    }
    if (endDate) {
      params.append('endDate', endDate);
    }
    const queryString = params.toString();
    const url = queryString ? `/transactions/summary?${queryString}` : '/transactions/summary';
    const response = await api.get<TransactionSummaryResponse>(url);
    return response.data;
  },
};

export default transactionApi;
