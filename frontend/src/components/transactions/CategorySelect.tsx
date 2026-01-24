import React, { useEffect, useState, useCallback, useMemo } from 'react';
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormHelperText,
  ListItemIcon,
  ListItemText,
  Box,
  CircularProgress,
  SelectProps,
} from '@mui/material';
import Icon from '@mui/material/Icon';
import type { Category, TransactionType, CategoriesResponse } from '@/types';
import { categoryApi } from '@/api';
import { getErrorMessage } from '@/api';

interface CategorySelectProps extends Omit<SelectProps, 'onChange'> {
  transactionType: TransactionType;
  value: string;
  onChange: (categoryId: string) => void;
  error?: boolean;
  helperText?: string;
}

/**
 * CategorySelect - Dropdown for selecting transaction category
 * Filters categories based on transaction type (INCOME/EXPENSE)
 */
const CategorySelect: React.FC<CategorySelectProps> = ({
  transactionType,
  value,
  onChange,
  error = false,
  helperText,
  disabled,
  ...props
}) => {
  const [categories, setCategories] = useState<CategoriesResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const fetchCategories = useCallback(async () => {
    setLoading(true);
    setFetchError(null);
    try {
      const data = await categoryApi.getCategories();
      setCategories(data);
    } catch (err) {
      setFetchError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  // Get categories for the selected type (memoized to prevent dependency issues)
  const filteredCategories = useMemo<Category[]>(() => {
    return categories?.[transactionType === 'INCOME' ? 'income' : 'expense'] || [];
  }, [categories, transactionType]);

  // Reset value if category doesn't match type
  useEffect(() => {
    if (value && categories) {
      const categoryExists = filteredCategories.some((cat) => cat.id === value);
      if (!categoryExists) {
        onChange('');
      }
    }
  }, [transactionType, value, categories, filteredCategories, onChange]);

  const handleChange = (event: { target: { value: unknown } }) => {
    onChange(event.target.value as string);
  };

  const displayHelperText = fetchError || helperText;

  return (
    <FormControl fullWidth error={error || !!fetchError} disabled={disabled || loading}>
      <InputLabel id="category-select-label">Category</InputLabel>
      <Select
        labelId="category-select-label"
        id="category-select"
        value={value}
        label="Category"
        onChange={handleChange}
        startAdornment={
          loading ? (
            <Box sx={{ display: 'flex', alignItems: 'center', mr: 1 }}>
              <CircularProgress size={20} />
            </Box>
          ) : null
        }
        {...props}
      >
        {filteredCategories.map((category) => (
          <MenuItem key={category.id} value={category.id}>
            <ListItemIcon sx={{ minWidth: 40 }}>
              <Icon sx={{ color: category.color }}>{category.icon}</Icon>
            </ListItemIcon>
            <ListItemText primary={category.name} />
          </MenuItem>
        ))}
        {filteredCategories.length === 0 && !loading && (
          <MenuItem disabled>
            <ListItemText primary="No categories available" />
          </MenuItem>
        )}
      </Select>
      {displayHelperText && <FormHelperText>{displayHelperText}</FormHelperText>}
    </FormControl>
  );
};

export default CategorySelect;
