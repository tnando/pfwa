-- V2__create_verification_tokens_table.sql
-- Creates the verification_tokens table for email verification and password reset
-- Part of Epic 1: Authentication

-- Create enum type for token types
CREATE TYPE token_type AS ENUM ('EMAIL_VERIFICATION', 'PASSWORD_RESET');

CREATE TABLE verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL,
    token_type token_type NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key to users table
    CONSTRAINT fk_verification_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- Token must be unique
    CONSTRAINT verification_tokens_token_unique UNIQUE (token)
);

-- Index for token lookup (most common query)
CREATE INDEX idx_verification_tokens_token ON verification_tokens (token);

-- Index for finding tokens by user and type (for invalidating old tokens)
CREATE INDEX idx_verification_tokens_user_type ON verification_tokens (user_id, token_type);

-- Index for cleanup of expired tokens
CREATE INDEX idx_verification_tokens_expires_at ON verification_tokens (expires_at)
    WHERE used_at IS NULL;

-- Comments for documentation
COMMENT ON TABLE verification_tokens IS 'Tokens for email verification and password reset';
COMMENT ON COLUMN verification_tokens.id IS 'Primary key - UUID';
COMMENT ON COLUMN verification_tokens.user_id IS 'Reference to the user who owns this token';
COMMENT ON COLUMN verification_tokens.token IS 'The verification/reset token value (hashed for password reset)';
COMMENT ON COLUMN verification_tokens.token_type IS 'Type of token: EMAIL_VERIFICATION or PASSWORD_RESET';
COMMENT ON COLUMN verification_tokens.expires_at IS 'When this token expires (24h for email verification, 1h for password reset)';
COMMENT ON COLUMN verification_tokens.used_at IS 'Timestamp when token was used (NULL if unused)';
COMMENT ON COLUMN verification_tokens.created_at IS 'Timestamp when token was created';
