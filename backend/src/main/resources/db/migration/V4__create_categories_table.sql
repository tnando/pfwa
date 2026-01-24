-- V4__create_categories_table.sql
-- Creates the categories table for transaction categorization
-- Part of Epic 2: Transactions

-- Create enum type for transaction types
CREATE TYPE transaction_type AS ENUM ('INCOME', 'EXPENSE');

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    type transaction_type NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(7),
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Unique constraint on name and type combination
    CONSTRAINT categories_name_type_unique UNIQUE (name, type)
);

-- Index for type lookups (filter categories by type)
CREATE INDEX idx_categories_type ON categories (type);

-- Index for active categories ordered by display_order
CREATE INDEX idx_categories_active_order ON categories (display_order)
    WHERE is_active = TRUE;

-- Comments for documentation
COMMENT ON TABLE categories IS 'Predefined categories for transaction classification';
COMMENT ON COLUMN categories.id IS 'Primary key - UUID';
COMMENT ON COLUMN categories.name IS 'Display name of the category';
COMMENT ON COLUMN categories.type IS 'Whether this category is for INCOME or EXPENSE transactions';
COMMENT ON COLUMN categories.icon IS 'Material-UI icon name for display';
COMMENT ON COLUMN categories.color IS 'Hex color code for display (e.g., #4CAF50)';
COMMENT ON COLUMN categories.display_order IS 'Order in which to display categories in UI';
COMMENT ON COLUMN categories.is_active IS 'Whether this category is available for selection';
COMMENT ON COLUMN categories.created_at IS 'Timestamp when category was created';

-- Seed data for income categories
INSERT INTO categories (name, type, icon, color, display_order) VALUES
    ('Salary', 'INCOME', 'payments', '#4CAF50', 1),
    ('Freelance', 'INCOME', 'work', '#8BC34A', 2),
    ('Investments', 'INCOME', 'trending_up', '#66BB6A', 3),
    ('Rental Income', 'INCOME', 'home', '#81C784', 4),
    ('Other Income', 'INCOME', 'attach_money', '#AED581', 5);

-- Seed data for expense categories
INSERT INTO categories (name, type, icon, color, display_order) VALUES
    ('Food & Dining', 'EXPENSE', 'restaurant', '#F44336', 1),
    ('Transportation', 'EXPENSE', 'directions_car', '#E91E63', 2),
    ('Housing', 'EXPENSE', 'house', '#9C27B0', 3),
    ('Utilities', 'EXPENSE', 'bolt', '#673AB7', 4),
    ('Entertainment', 'EXPENSE', 'movie', '#3F51B5', 5),
    ('Healthcare', 'EXPENSE', 'local_hospital', '#2196F3', 6),
    ('Shopping', 'EXPENSE', 'shopping_bag', '#03A9F4', 7),
    ('Education', 'EXPENSE', 'school', '#00BCD4', 8),
    ('Personal Care', 'EXPENSE', 'spa', '#009688', 9),
    ('Other', 'EXPENSE', 'more_horiz', '#607D8B', 10);
