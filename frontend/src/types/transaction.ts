/**
 * Transaction type enum
 */
export type TransactionType = 'INCOME' | 'EXPENSE';

/**
 * Category data returned from the API
 */
export interface Category {
  id: string;
  name: string;
  type: TransactionType;
  icon: string;
  color: string;
}

/**
 * Transaction data returned from the API
 */
export interface Transaction {
  id: string;
  amount: number;
  type: TransactionType;
  category: Category;
  date: string;
  description: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Create transaction request payload
 */
export interface CreateTransactionRequest {
  amount: number;
  type: TransactionType;
  categoryId: string;
  date: string;
  description?: string | null;
  notes?: string | null;
}

/**
 * Update transaction request payload
 */
export interface UpdateTransactionRequest {
  amount: number;
  type: TransactionType;
  categoryId: string;
  date: string;
  description?: string | null;
  notes?: string | null;
}

/**
 * Transaction list summary
 */
export interface TransactionListSummary {
  totalIncome: number;
  totalExpenses: number;
  netBalance: number;
}

/**
 * Applied filters in the response
 */
export interface AppliedFilters {
  startDate: string | null;
  endDate: string | null;
  type: TransactionType | null;
  categoryIds: string[];
  minAmount: number | null;
  maxAmount: number | null;
  search: string | null;
}

/**
 * Paginated transaction list response
 */
export interface TransactionListResponse {
  content: Transaction[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  summary: TransactionListSummary;
  appliedFilters?: AppliedFilters;
}

/**
 * Transaction filter parameters for API requests
 */
export interface TransactionFilter {
  startDate?: string;
  endDate?: string;
  type?: TransactionType;
  categoryIds?: string[];
  minAmount?: number;
  maxAmount?: number;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Categories response grouped by type
 */
export interface CategoriesResponse {
  income: Category[];
  expense: Category[];
}

/**
 * Category summary in breakdown
 */
export interface CategorySummary {
  id: string;
  name: string;
  icon: string;
  color: string;
}

/**
 * Category breakdown item in summary
 */
export interface CategoryBreakdownItem {
  category: CategorySummary;
  total: number;
  percentage: number;
  transactionCount: number;
}

/**
 * Category breakdown grouped by type
 */
export interface CategoryBreakdown {
  income: CategoryBreakdownItem[];
  expense: CategoryBreakdownItem[];
}

/**
 * Summary period
 */
export interface SummaryPeriod {
  startDate: string;
  endDate: string;
}

/**
 * Summary totals
 */
export interface SummaryTotals {
  income: number;
  expenses: number;
  net: number;
  transactionCount: number;
}

/**
 * Previous period summary
 */
export interface PreviousPeriodSummary {
  startDate: string;
  endDate: string;
  income: number;
  expenses: number;
  net: number;
}

/**
 * Period change percentages
 */
export interface PeriodChange {
  income: string;
  expenses: string;
  net: string;
}

/**
 * Period comparison
 */
export interface PeriodComparison {
  previousPeriod: PreviousPeriodSummary;
  change: PeriodChange;
}

/**
 * Full transaction summary response
 */
export interface TransactionSummaryResponse {
  period: SummaryPeriod;
  totals: SummaryTotals;
  categoryBreakdown: CategoryBreakdown;
  comparison?: PeriodComparison;
}

/**
 * Date range preset options
 */
export type DateRangePreset =
  | 'today'
  | 'last7days'
  | 'last30days'
  | 'thisMonth'
  | 'lastMonth'
  | 'thisYear'
  | 'custom';

/**
 * Sort direction
 */
export type SortDirection = 'asc' | 'desc';

/**
 * Sortable fields
 */
export type SortField = 'date' | 'amount' | 'category' | 'createdAt';
