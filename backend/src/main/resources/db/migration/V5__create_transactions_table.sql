-- V5__create_transactions_table.sql
-- Creates the transactions table for tracking income and expenses
-- Part of Epic 2: Transactions

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    type transaction_type NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Amount must be positive
    CONSTRAINT transactions_amount_positive CHECK (amount > 0),

    -- Amount maximum check (10,000,000)
    CONSTRAINT transactions_amount_max CHECK (amount <= 10000000),

    -- Foreign key to users table
    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- Foreign key to categories table
    CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE RESTRICT
);

-- Primary index for user's transactions (most common query pattern)
CREATE INDEX idx_transactions_user_id ON transactions (user_id);

-- Index for filtering and sorting by date
CREATE INDEX idx_transactions_date ON transactions (transaction_date);

-- Index for filtering by category
CREATE INDEX idx_transactions_category_id ON transactions (category_id);

-- Index for filtering by type
CREATE INDEX idx_transactions_type ON transactions (type);

-- Composite index for user's transactions sorted by date (pagination)
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date DESC);

-- Composite index for user's transactions by type (filtering)
CREATE INDEX idx_transactions_user_type ON transactions (user_id, type);

-- Composite index for user, date range, and type (common filter combination)
CREATE INDEX idx_transactions_user_date_type ON transactions (user_id, transaction_date, type);

-- Full-text search index on description for search functionality
CREATE INDEX idx_transactions_description_search ON transactions
    USING gin(to_tsvector('english', COALESCE(description, '')));

-- Apply the update_updated_at_column trigger (created in V1)
CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE transactions IS 'Financial transactions (income and expenses) for users';
COMMENT ON COLUMN transactions.id IS 'Primary key - UUID';
COMMENT ON COLUMN transactions.user_id IS 'Reference to the user who owns this transaction';
COMMENT ON COLUMN transactions.category_id IS 'Reference to the transaction category';
COMMENT ON COLUMN transactions.amount IS 'Transaction amount (positive decimal, max 10,000,000)';
COMMENT ON COLUMN transactions.type IS 'Transaction type: INCOME or EXPENSE';
COMMENT ON COLUMN transactions.transaction_date IS 'Date when the transaction occurred';
COMMENT ON COLUMN transactions.description IS 'Brief description of the transaction (max 500 chars)';
COMMENT ON COLUMN transactions.notes IS 'Additional notes about the transaction';
COMMENT ON COLUMN transactions.created_at IS 'Timestamp when transaction was created';
COMMENT ON COLUMN transactions.updated_at IS 'Timestamp when transaction was last updated';
