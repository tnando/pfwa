import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Paper,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Button,
  Chip,
  Typography,
  Collapse,
  IconButton,
  Checkbox,
  ListItemText,
  OutlinedInput,
  SelectChangeEvent,
} from '@mui/material';
import FilterListIcon from '@mui/icons-material/FilterList';
import ClearIcon from '@mui/icons-material/Clear';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import type {
  TransactionFilter as TransactionFilterType,
  TransactionType,
  Category,
  CategoriesResponse,
  DateRangePreset,
} from '@/types';
import { categoryApi, getErrorMessage } from '@/api';

interface TransactionFilterProps {
  filter: TransactionFilterType;
  onFilterChange: (filter: TransactionFilterType) => void;
  onClear: () => void;
}

const DATE_PRESETS: { value: DateRangePreset; label: string }[] = [
  { value: 'today', label: 'Today' },
  { value: 'last7days', label: 'Last 7 Days' },
  { value: 'last30days', label: 'Last 30 Days' },
  { value: 'thisMonth', label: 'This Month' },
  { value: 'lastMonth', label: 'Last Month' },
  { value: 'thisYear', label: 'This Year' },
  { value: 'custom', label: 'Custom Range' },
];

/**
 * Calculate date range based on preset
 */
const getDateRange = (
  preset: DateRangePreset
): { startDate: string; endDate: string } | null => {
  const today = new Date();
  const formatDate = (date: Date): string => date.toISOString().split('T')[0];

  switch (preset) {
    case 'today':
      return { startDate: formatDate(today), endDate: formatDate(today) };
    case 'last7days': {
      const start = new Date(today);
      start.setDate(start.getDate() - 6);
      return { startDate: formatDate(start), endDate: formatDate(today) };
    }
    case 'last30days': {
      const start = new Date(today);
      start.setDate(start.getDate() - 29);
      return { startDate: formatDate(start), endDate: formatDate(today) };
    }
    case 'thisMonth': {
      const start = new Date(today.getFullYear(), today.getMonth(), 1);
      return { startDate: formatDate(start), endDate: formatDate(today) };
    }
    case 'lastMonth': {
      const start = new Date(today.getFullYear(), today.getMonth() - 1, 1);
      const end = new Date(today.getFullYear(), today.getMonth(), 0);
      return { startDate: formatDate(start), endDate: formatDate(end) };
    }
    case 'thisYear': {
      const start = new Date(today.getFullYear(), 0, 1);
      return { startDate: formatDate(start), endDate: formatDate(today) };
    }
    case 'custom':
      return null;
    default:
      return null;
  }
};

/**
 * TransactionFilter - Filter panel for transactions
 * Supports date range, type, category, and amount filters
 */
const TransactionFilter: React.FC<TransactionFilterProps> = ({
  filter,
  onFilterChange,
  onClear,
}) => {
  const [expanded, setExpanded] = useState(false);
  const [datePreset, setDatePreset] = useState<DateRangePreset | ''>('');
  const [categories, setCategories] = useState<CategoriesResponse | null>(null);
  const [loadingCategories, setLoadingCategories] = useState(false);

  // Fetch categories on mount
  useEffect(() => {
    const fetchCategories = async () => {
      setLoadingCategories(true);
      try {
        const data = await categoryApi.getCategories();
        setCategories(data);
      } catch (err) {
        console.error('Failed to fetch categories:', getErrorMessage(err));
      } finally {
        setLoadingCategories(false);
      }
    };
    fetchCategories();
  }, []);

  // Get all categories based on selected type
  const getAvailableCategories = useCallback((): Category[] => {
    if (!categories) return [];
    if (filter.type === 'INCOME') return categories.income;
    if (filter.type === 'EXPENSE') return categories.expense;
    return [...categories.income, ...categories.expense];
  }, [categories, filter.type]);

  const availableCategories = getAvailableCategories();

  // Count active filters
  const activeFilterCount = [
    filter.startDate,
    filter.endDate,
    filter.type,
    filter.categoryIds && filter.categoryIds.length > 0,
    filter.minAmount,
    filter.maxAmount,
  ].filter(Boolean).length;

  const handleDatePresetChange = (event: SelectChangeEvent<string>) => {
    const preset = event.target.value as DateRangePreset | '';
    setDatePreset(preset);

    if (preset && preset !== 'custom') {
      const range = getDateRange(preset);
      if (range) {
        onFilterChange({ ...filter, ...range });
      }
    }
  };

  const handleTypeChange = (event: SelectChangeEvent<string>) => {
    const type = event.target.value as TransactionType | '';
    onFilterChange({
      ...filter,
      type: type || undefined,
      categoryIds: [], // Reset categories when type changes
    });
  };

  const handleCategoryChange = (event: SelectChangeEvent<string[]>) => {
    const value = event.target.value;
    const categoryIds = typeof value === 'string' ? value.split(',') : value;
    onFilterChange({ ...filter, categoryIds });
  };

  const handleDateChange =
    (field: 'startDate' | 'endDate') =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setDatePreset('custom');
      onFilterChange({ ...filter, [field]: event.target.value || undefined });
    };

  const handleAmountChange =
    (field: 'minAmount' | 'maxAmount') =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value;
      onFilterChange({
        ...filter,
        [field]: value ? parseFloat(value) : undefined,
      });
    };

  const handleClearFilters = () => {
    setDatePreset('');
    onClear();
  };

  const toggleExpanded = () => {
    setExpanded((prev) => !prev);
  };

  return (
    <Paper sx={{ p: 2 }}>
      {/* Filter header */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
        }}
        onClick={toggleExpanded}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <FilterListIcon color="action" />
          <Typography variant="subtitle1" fontWeight={500}>
            Filters
          </Typography>
          {activeFilterCount > 0 && (
            <Chip
              size="small"
              label={activeFilterCount}
              color="primary"
              sx={{ ml: 1 }}
            />
          )}
        </Box>
        <IconButton size="small" aria-label={expanded ? 'Collapse filters' : 'Expand filters'}>
          {expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
        </IconButton>
      </Box>

      {/* Filter content */}
      <Collapse in={expanded}>
        <Box sx={{ mt: 2 }}>
          <Grid container spacing={2}>
            {/* Date preset */}
            <Grid item xs={12} sm={6} md={3}>
              <FormControl fullWidth size="small">
                <InputLabel>Date Range</InputLabel>
                <Select
                  value={datePreset}
                  label="Date Range"
                  onChange={handleDatePresetChange}
                >
                  <MenuItem value="">
                    <em>All Time</em>
                  </MenuItem>
                  {DATE_PRESETS.map((preset) => (
                    <MenuItem key={preset.value} value={preset.value}>
                      {preset.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            {/* Custom date range */}
            <Grid item xs={6} sm={3} md={2}>
              <TextField
                fullWidth
                size="small"
                label="Start Date"
                type="date"
                value={filter.startDate || ''}
                onChange={handleDateChange('startDate')}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <TextField
                fullWidth
                size="small"
                label="End Date"
                type="date"
                value={filter.endDate || ''}
                onChange={handleDateChange('endDate')}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>

            {/* Type filter */}
            <Grid item xs={12} sm={6} md={2}>
              <FormControl fullWidth size="small">
                <InputLabel>Type</InputLabel>
                <Select
                  value={filter.type || ''}
                  label="Type"
                  onChange={handleTypeChange}
                >
                  <MenuItem value="">
                    <em>All Types</em>
                  </MenuItem>
                  <MenuItem value="INCOME">Income</MenuItem>
                  <MenuItem value="EXPENSE">Expense</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            {/* Category filter */}
            <Grid item xs={12} sm={6} md={3}>
              <FormControl fullWidth size="small" disabled={loadingCategories}>
                <InputLabel>Categories</InputLabel>
                <Select
                  multiple
                  value={filter.categoryIds || []}
                  onChange={handleCategoryChange}
                  input={<OutlinedInput label="Categories" />}
                  renderValue={(selected) => {
                    const selectedNames = selected
                      .map((id) => availableCategories.find((c) => c.id === id)?.name)
                      .filter(Boolean);
                    return selectedNames.join(', ');
                  }}
                >
                  {availableCategories.map((category) => (
                    <MenuItem key={category.id} value={category.id}>
                      <Checkbox
                        checked={(filter.categoryIds || []).includes(category.id)}
                      />
                      <ListItemText primary={category.name} />
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            {/* Amount range */}
            <Grid item xs={6} sm={3} md={2}>
              <TextField
                fullWidth
                size="small"
                label="Min Amount"
                type="number"
                value={filter.minAmount ?? ''}
                onChange={handleAmountChange('minAmount')}
                InputProps={{ inputProps: { min: 0, step: 0.01 } }}
              />
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <TextField
                fullWidth
                size="small"
                label="Max Amount"
                type="number"
                value={filter.maxAmount ?? ''}
                onChange={handleAmountChange('maxAmount')}
                InputProps={{ inputProps: { min: 0, step: 0.01 } }}
              />
            </Grid>

            {/* Clear button */}
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  startIcon={<ClearIcon />}
                  onClick={handleClearFilters}
                  disabled={activeFilterCount === 0}
                >
                  Clear Filters
                </Button>
              </Box>
            </Grid>
          </Grid>
        </Box>
      </Collapse>
    </Paper>
  );
};

export default TransactionFilter;
