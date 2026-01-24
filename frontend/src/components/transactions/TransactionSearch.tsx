import React, { useState, useEffect, useCallback } from 'react';
import {
  TextField,
  InputAdornment,
  IconButton,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';

interface TransactionSearchProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  debounceMs?: number;
}

/**
 * TransactionSearch - Search input with debounce
 * Debounces input to prevent excessive API calls
 */
const TransactionSearch: React.FC<TransactionSearchProps> = ({
  value,
  onChange,
  placeholder = 'Search transactions...',
  debounceMs = 300,
}) => {
  const [inputValue, setInputValue] = useState(value);

  // Sync internal state with external value
  useEffect(() => {
    setInputValue(value);
  }, [value]);

  // Debounce the onChange callback
  useEffect(() => {
    const timer = setTimeout(() => {
      if (inputValue !== value) {
        // Only trigger search if minimum 2 characters or empty
        if (inputValue.length >= 2 || inputValue.length === 0) {
          onChange(inputValue);
        }
      }
    }, debounceMs);

    return () => clearTimeout(timer);
  }, [inputValue, value, onChange, debounceMs]);

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(event.target.value);
  };

  const handleClear = useCallback(() => {
    setInputValue('');
    onChange('');
  }, [onChange]);

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Escape') {
      handleClear();
    }
  };

  return (
    <TextField
      fullWidth
      size="small"
      placeholder={placeholder}
      value={inputValue}
      onChange={handleChange}
      onKeyDown={handleKeyDown}
      aria-label="Search transactions"
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchIcon color="action" />
          </InputAdornment>
        ),
        endAdornment: inputValue ? (
          <InputAdornment position="end">
            <IconButton
              size="small"
              onClick={handleClear}
              aria-label="Clear search"
            >
              <ClearIcon fontSize="small" />
            </IconButton>
          </InputAdornment>
        ) : null,
      }}
      sx={{
        '& .MuiOutlinedInput-root': {
          bgcolor: 'background.paper',
        },
      }}
    />
  );
};

export default TransactionSearch;
