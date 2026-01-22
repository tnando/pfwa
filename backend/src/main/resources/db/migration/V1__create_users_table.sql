-- V1__create_users_table.sql
-- Creates the core users table for authentication
-- Part of Epic 1: Authentication

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    account_locked_until TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    token_version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Unique constraint on email (case-insensitive)
    CONSTRAINT users_email_unique UNIQUE (email)
);

-- Index for email lookups (case-insensitive)
CREATE UNIQUE INDEX idx_users_email_lower ON users (LOWER(email));

-- Index for finding locked accounts that can be unlocked
CREATE INDEX idx_users_account_locked_until ON users (account_locked_until)
    WHERE account_locked = TRUE;

-- Trigger function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to users table
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE users IS 'Core user table for authentication and profile data';
COMMENT ON COLUMN users.id IS 'Primary key - UUID for security and distribution';
COMMENT ON COLUMN users.email IS 'User email address - unique identifier for login';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password (strength 12)';
COMMENT ON COLUMN users.first_name IS 'User first name for display';
COMMENT ON COLUMN users.last_name IS 'User last name for display';
COMMENT ON COLUMN users.email_verified IS 'Whether user has verified their email address';
COMMENT ON COLUMN users.account_locked IS 'Whether account is currently locked due to failed login attempts';
COMMENT ON COLUMN users.account_locked_until IS 'Timestamp when account lock expires (NULL if not locked)';
COMMENT ON COLUMN users.failed_login_attempts IS 'Count of consecutive failed login attempts';
COMMENT ON COLUMN users.token_version IS 'Version number for global token invalidation (logout all sessions)';
COMMENT ON COLUMN users.created_at IS 'Timestamp when user registered';
COMMENT ON COLUMN users.updated_at IS 'Timestamp of last profile update';
