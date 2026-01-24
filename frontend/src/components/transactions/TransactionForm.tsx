import React, { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import {
  Box,
  TextField,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
  Button,
  Grid,
  Alert,
  CircularProgress,
  FormHelperText,
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import type {
  TransactionType,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  Transaction,
} from '@/types';
import CategorySelect from './CategorySelect';

interface TransactionFormData {
  amount: string;
  type: TransactionType;
  categoryId: string;
  date: string;
  description: string;
  notes: string;
}

interface TransactionFormProps {
  transaction?: Transaction | null;
  onSubmit: (data: CreateTransactionRequest | UpdateTransactionRequest) => Promise<void>;
  onCancel: () => void;
  loading?: boolean;
  error?: string | null;
}

const MAX_AMOUNT = 10000000;
const MAX_DESCRIPTION_LENGTH = 500;
const MAX_NOTES_LENGTH = 1000;

/**
 * Get today's date in YYYY-MM-DD format
 */
const getTodayDate = (): string => {
  return new Date().toISOString().split('T')[0];
};

/**
 * TransactionForm - Form for creating/editing transactions
 * Handles validation and submission of transaction data
 */
const TransactionForm: React.FC<TransactionFormProps> = ({
  transaction,
  onSubmit,
  onCancel,
  loading = false,
  error = null,
}) => {
  const isEditing = !!transaction;

  const {
    control,
    handleSubmit,
    watch,
    reset,
    setValue,
    formState: { errors, isValid, isDirty },
  } = useForm<TransactionFormData>({
    mode: 'onChange',
    defaultValues: {
      amount: transaction?.amount?.toString() || '',
      type: transaction?.type || 'EXPENSE',
      categoryId: transaction?.category?.id || '',
      date: transaction?.date || getTodayDate(),
      description: transaction?.description || '',
      notes: transaction?.notes || '',
    },
  });

  const transactionType = watch('type');

  // Reset form when transaction changes
  useEffect(() => {
    if (transaction) {
      reset({
        amount: transaction.amount.toString(),
        type: transaction.type,
        categoryId: transaction.category.id,
        date: transaction.date,
        description: transaction.description || '',
        notes: transaction.notes || '',
      });
    }
  }, [transaction, reset]);

  const handleFormSubmit = async (data: TransactionFormData) => {
    const requestData: CreateTransactionRequest | UpdateTransactionRequest = {
      amount: parseFloat(data.amount),
      type: data.type,
      categoryId: data.categoryId,
      date: data.date,
      description: data.description || null,
      notes: data.notes || null,
    };

    await onSubmit(requestData);
  };

  const handleCategoryChange = (categoryId: string) => {
    setValue('categoryId', categoryId, { shouldValidate: true, shouldDirty: true });
  };

  // Check if date is in the future
  const dateValue = watch('date');
  const isFutureDate = dateValue && new Date(dateValue) > new Date();

  return (
    <Box component="form" onSubmit={handleSubmit(handleFormSubmit)} noValidate>
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Transaction Type */}
        <Grid item xs={12}>
          <FormControl component="fieldset">
            <FormLabel component="legend">Transaction Type</FormLabel>
            <Controller
              name="type"
              control={control}
              render={({ field }) => (
                <RadioGroup row {...field}>
                  <FormControlLabel
                    value="EXPENSE"
                    control={<Radio color="error" />}
                    label="Expense"
                  />
                  <FormControlLabel
                    value="INCOME"
                    control={<Radio color="success" />}
                    label="Income"
                  />
                </RadioGroup>
              )}
            />
          </FormControl>
        </Grid>

        {/* Amount */}
        <Grid item xs={12} sm={6}>
          <Controller
            name="amount"
            control={control}
            rules={{
              required: 'Amount is required',
              validate: {
                positive: (value) => {
                  const num = parseFloat(value);
                  return num > 0 || 'Amount must be greater than 0';
                },
                max: (value) => {
                  const num = parseFloat(value);
                  return num <= MAX_AMOUNT || `Amount must not exceed ${MAX_AMOUNT.toLocaleString()}`;
                },
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label="Amount"
                type="number"
                required
                error={!!errors.amount}
                helperText={errors.amount?.message}
                InputProps={{
                  startAdornment: <Box sx={{ mr: 0.5 }}>$</Box>,
                  inputProps: { min: 0.01, step: 0.01, max: MAX_AMOUNT },
                }}
              />
            )}
          />
        </Grid>

        {/* Date */}
        <Grid item xs={12} sm={6}>
          <Controller
            name="date"
            control={control}
            rules={{
              required: 'Date is required',
            }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label="Date"
                type="date"
                required
                error={!!errors.date}
                helperText={errors.date?.message}
                InputLabelProps={{ shrink: true }}
              />
            )}
          />
          {isFutureDate && (
            <FormHelperText sx={{ color: 'warning.main' }}>
              Note: This transaction date is in the future.
            </FormHelperText>
          )}
        </Grid>

        {/* Category */}
        <Grid item xs={12}>
          <Controller
            name="categoryId"
            control={control}
            rules={{
              required: 'Category is required',
            }}
            render={({ field }) => (
              <CategorySelect
                transactionType={transactionType}
                value={field.value}
                onChange={handleCategoryChange}
                error={!!errors.categoryId}
                helperText={errors.categoryId?.message}
              />
            )}
          />
        </Grid>

        {/* Description */}
        <Grid item xs={12}>
          <Controller
            name="description"
            control={control}
            rules={{
              maxLength: {
                value: MAX_DESCRIPTION_LENGTH,
                message: `Description must be ${MAX_DESCRIPTION_LENGTH} characters or less`,
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label="Description"
                placeholder="e.g., Grocery shopping at Whole Foods"
                error={!!errors.description}
                helperText={
                  errors.description?.message ||
                  `${field.value.length}/${MAX_DESCRIPTION_LENGTH}`
                }
              />
            )}
          />
        </Grid>

        {/* Notes */}
        <Grid item xs={12}>
          <Controller
            name="notes"
            control={control}
            rules={{
              maxLength: {
                value: MAX_NOTES_LENGTH,
                message: `Notes must be ${MAX_NOTES_LENGTH} characters or less`,
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                label="Notes"
                multiline
                rows={3}
                placeholder="Additional notes about this transaction..."
                error={!!errors.notes}
                helperText={
                  errors.notes?.message || `${field.value.length}/${MAX_NOTES_LENGTH}`
                }
              />
            )}
          />
        </Grid>

        {/* Actions */}
        <Grid item xs={12}>
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
            <Button
              variant="outlined"
              onClick={onCancel}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={loading || !isValid || (!isDirty && isEditing)}
              startIcon={
                loading ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />
              }
            >
              {loading
                ? isEditing
                  ? 'Updating...'
                  : 'Creating...'
                : isEditing
                  ? 'Update Transaction'
                  : 'Create Transaction'}
            </Button>
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
};

export default TransactionForm;
