import React, { useState } from 'react';
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TableSortLabel,
  IconButton,
  Chip,
  Typography,
  Tooltip,
  Skeleton,
  Collapse,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import Icon from '@mui/material/Icon';
import type { Transaction, SortField, SortDirection } from '@/types';

interface TransactionListProps {
  transactions: Transaction[];
  loading?: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  sortField: SortField;
  sortDirection: SortDirection;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onSortChange: (field: SortField, direction: SortDirection) => void;
  onEdit: (transaction: Transaction) => void;
  onDelete: (transaction: Transaction) => void;
}

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

/**
 * Format currency with USD symbol
 */
const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
};

/**
 * Format date as MMM DD, YYYY
 */
const formatDate = (dateString: string): string => {
  const date = new Date(dateString + 'T00:00:00');
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
};

/**
 * Truncate text if longer than maxLength
 */
const truncateText = (text: string | null, maxLength: number): string => {
  if (!text) return '-';
  return text.length > maxLength ? `${text.substring(0, maxLength)}...` : text;
};

interface TransactionRowProps {
  transaction: Transaction;
  onEdit: (transaction: Transaction) => void;
  onDelete: (transaction: Transaction) => void;
}

const TransactionRow: React.FC<TransactionRowProps> = ({
  transaction,
  onEdit,
  onDelete,
}) => {
  const [expanded, setExpanded] = useState(false);
  const isIncome = transaction.type === 'INCOME';

  return (
    <>
      <TableRow
        hover
        sx={{ cursor: 'pointer' }}
        onClick={() => setExpanded((prev) => !prev)}
      >
        <TableCell padding="checkbox">
          <IconButton
            size="small"
            aria-label={expanded ? 'Collapse details' : 'Expand details'}
          >
            {expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
          </IconButton>
        </TableCell>
        <TableCell>{formatDate(transaction.date)}</TableCell>
        <TableCell>
          <Chip
            size="small"
            label={transaction.type}
            color={isIncome ? 'success' : 'error'}
            variant="outlined"
          />
        </TableCell>
        <TableCell>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Icon sx={{ color: transaction.category.color, fontSize: 20 }}>
              {transaction.category.icon}
            </Icon>
            <Typography variant="body2">{transaction.category.name}</Typography>
          </Box>
        </TableCell>
        <TableCell>
          <Tooltip title={transaction.description || ''} arrow>
            <Typography variant="body2">
              {truncateText(transaction.description, 50)}
            </Typography>
          </Tooltip>
        </TableCell>
        <TableCell align="right">
          <Typography
            variant="body2"
            fontWeight={600}
            color={isIncome ? 'success.main' : 'error.main'}
          >
            {isIncome ? '+' : '-'}{formatCurrency(transaction.amount)}
          </Typography>
        </TableCell>
        <TableCell align="right">
          <Tooltip title="Edit transaction">
            <IconButton
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                onEdit(transaction);
              }}
              aria-label="Edit transaction"
            >
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete transaction">
            <IconButton
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                onDelete(transaction);
              }}
              aria-label="Delete transaction"
              color="error"
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </TableCell>
      </TableRow>

      {/* Expanded details row */}
      <TableRow>
        <TableCell colSpan={7} sx={{ py: 0, borderBottom: expanded ? undefined : 'none' }}>
          <Collapse in={expanded} timeout="auto" unmountOnExit>
            <Box sx={{ py: 2, px: 3, bgcolor: 'grey.50' }}>
              <Typography variant="subtitle2" gutterBottom>
                Transaction Details
              </Typography>
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Description
                  </Typography>
                  <Typography variant="body2">
                    {transaction.description || '-'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Notes
                  </Typography>
                  <Typography variant="body2">{transaction.notes || '-'}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Created
                  </Typography>
                  <Typography variant="body2">
                    {new Date(transaction.createdAt).toLocaleString()}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Last Updated
                  </Typography>
                  <Typography variant="body2">
                    {new Date(transaction.updatedAt).toLocaleString()}
                  </Typography>
                </Box>
              </Box>
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
};

const LoadingSkeleton: React.FC = () => (
  <>
    {[...Array(5)].map((_, index) => (
      <TableRow key={index}>
        <TableCell padding="checkbox">
          <Skeleton variant="circular" width={24} height={24} />
        </TableCell>
        <TableCell>
          <Skeleton variant="text" width={80} />
        </TableCell>
        <TableCell>
          <Skeleton variant="rounded" width={70} height={24} />
        </TableCell>
        <TableCell>
          <Skeleton variant="text" width={100} />
        </TableCell>
        <TableCell>
          <Skeleton variant="text" width={150} />
        </TableCell>
        <TableCell align="right">
          <Skeleton variant="text" width={80} />
        </TableCell>
        <TableCell align="right">
          <Skeleton variant="circular" width={32} height={32} />
        </TableCell>
      </TableRow>
    ))}
  </>
);

/**
 * TransactionList - Table/list of transactions with actions
 * Supports sorting, pagination, and expandable rows
 */
const TransactionList: React.FC<TransactionListProps> = ({
  transactions,
  loading = false,
  page,
  pageSize,
  totalElements,
  sortField,
  sortDirection,
  onPageChange,
  onPageSizeChange,
  onSortChange,
  onEdit,
  onDelete,
}) => {
  const handleSort = (field: SortField) => {
    const isAsc = sortField === field && sortDirection === 'asc';
    onSortChange(field, isAsc ? 'desc' : 'asc');
  };

  const handleChangePage = (_: unknown, newPage: number) => {
    onPageChange(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    onPageSizeChange(parseInt(event.target.value, 10));
    onPageChange(0);
  };

  const createSortHandler = (field: SortField) => () => {
    handleSort(field);
  };

  // Calculate display range
  const startItem = page * pageSize + 1;
  const endItem = Math.min((page + 1) * pageSize, totalElements);

  return (
    <Paper>
      <TableContainer>
        <Table aria-label="Transactions table">
          <TableHead>
            <TableRow>
              <TableCell padding="checkbox" sx={{ width: 48 }} />
              <TableCell sortDirection={sortField === 'date' ? sortDirection : false}>
                <TableSortLabel
                  active={sortField === 'date'}
                  direction={sortField === 'date' ? sortDirection : 'asc'}
                  onClick={createSortHandler('date')}
                >
                  Date
                </TableSortLabel>
              </TableCell>
              <TableCell>Type</TableCell>
              <TableCell sortDirection={sortField === 'category' ? sortDirection : false}>
                <TableSortLabel
                  active={sortField === 'category'}
                  direction={sortField === 'category' ? sortDirection : 'asc'}
                  onClick={createSortHandler('category')}
                >
                  Category
                </TableSortLabel>
              </TableCell>
              <TableCell>Description</TableCell>
              <TableCell
                align="right"
                sortDirection={sortField === 'amount' ? sortDirection : false}
              >
                <TableSortLabel
                  active={sortField === 'amount'}
                  direction={sortField === 'amount' ? sortDirection : 'asc'}
                  onClick={createSortHandler('amount')}
                >
                  Amount
                </TableSortLabel>
              </TableCell>
              <TableCell align="right" sx={{ width: 100 }}>
                Actions
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <LoadingSkeleton />
            ) : transactions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 8 }}>
                  <Typography variant="h6" color="text.secondary" gutterBottom>
                    No transactions yet
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Create your first transaction to get started.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              transactions.map((transaction) => (
                <TransactionRow
                  key={transaction.id}
                  transaction={transaction}
                  onEdit={onEdit}
                  onDelete={onDelete}
                />
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Pagination */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          px: 2,
          borderTop: 1,
          borderColor: 'divider',
        }}
      >
        <Typography variant="body2" color="text.secondary">
          {totalElements > 0
            ? `Showing ${startItem}-${endItem} of ${totalElements} transactions`
            : 'No transactions'}
        </Typography>
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={handleChangePage}
          rowsPerPage={pageSize}
          onRowsPerPageChange={handleChangeRowsPerPage}
          rowsPerPageOptions={PAGE_SIZE_OPTIONS}
          labelRowsPerPage="Per page:"
        />
      </Box>
    </Paper>
  );
};

export default TransactionList;
