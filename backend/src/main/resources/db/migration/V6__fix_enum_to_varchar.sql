-- V6__fix_enum_to_varchar.sql
-- Convert PostgreSQL ENUM types to VARCHAR for JPA compatibility
-- JPA's @Enumerated(EnumType.STRING) expects VARCHAR, not PostgreSQL ENUM

-- First, alter the categories table to use VARCHAR
ALTER TABLE categories
    ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- Alter the transactions table (if it exists and uses the enum)
ALTER TABLE transactions
    ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- Drop the enum type as it's no longer needed
DROP TYPE IF EXISTS transaction_type;
