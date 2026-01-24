import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Skeleton,
} from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import type { TransactionListSummary } from '@/types';

interface TransactionSummaryProps {
  summary: TransactionListSummary | null;
  loading?: boolean;
}

/**
 * Format currency with USD symbol
 */
const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
};

interface SummaryCardProps {
  title: string;
  amount: number;
  icon: React.ReactNode;
  color: string;
  loading?: boolean;
}

const SummaryCard: React.FC<SummaryCardProps> = ({
  title,
  amount,
  icon,
  color,
  loading = false,
}) => (
  <Card sx={{ height: '100%' }}>
    <CardContent>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 40,
            height: 40,
            borderRadius: 2,
            bgcolor: `${color}15`,
            color: color,
            mr: 2,
          }}
        >
          {icon}
        </Box>
        <Typography variant="body2" color="text.secondary">
          {title}
        </Typography>
      </Box>
      {loading ? (
        <Skeleton variant="text" width="60%" height={40} />
      ) : (
        <Typography
          variant="h5"
          component="div"
          sx={{ fontWeight: 600, color: color }}
        >
          {formatCurrency(amount)}
        </Typography>
      )}
    </CardContent>
  </Card>
);

/**
 * TransactionSummary - Summary cards showing income, expenses, and balance
 */
const TransactionSummary: React.FC<TransactionSummaryProps> = ({
  summary,
  loading = false,
}) => {
  const income = summary?.totalIncome ?? 0;
  const expenses = summary?.totalExpenses ?? 0;
  const balance = summary?.netBalance ?? 0;

  const balanceColor = balance >= 0 ? '#4caf50' : '#f44336';

  return (
    <Grid container spacing={3}>
      <Grid item xs={12} sm={4}>
        <SummaryCard
          title="Total Income"
          amount={income}
          icon={<TrendingUpIcon />}
          color="#4caf50"
          loading={loading}
        />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SummaryCard
          title="Total Expenses"
          amount={expenses}
          icon={<TrendingDownIcon />}
          color="#f44336"
          loading={loading}
        />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SummaryCard
          title="Net Balance"
          amount={balance}
          icon={<AccountBalanceIcon />}
          color={balanceColor}
          loading={loading}
        />
      </Grid>
    </Grid>
  );
};

export default TransactionSummary;
