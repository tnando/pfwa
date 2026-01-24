import api from './axiosConfig';
import type { CategoriesResponse, TransactionType } from '@/types';

/**
 * Category API service
 */
export const categoryApi = {
  /**
   * Get all categories grouped by type
   * Optionally filter by transaction type
   */
  getCategories: async (type?: TransactionType): Promise<CategoriesResponse> => {
    const url = type ? `/categories?type=${type}` : '/categories';
    const response = await api.get<CategoriesResponse>(url);
    return response.data;
  },
};

export default categoryApi;
