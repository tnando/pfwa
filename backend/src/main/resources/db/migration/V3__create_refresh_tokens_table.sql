-- V3__create_refresh_tokens_table.sql
-- Creates the refresh_tokens table for JWT refresh token management with rotation
-- Part of Epic 1: Authentication

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    family_id UUID NOT NULL,
    device_info JSONB,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key to users table
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- Token hash must be unique
    CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash)
);

-- Index for token hash lookup (primary lookup method)
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- Index for finding all tokens by user (for session listing)
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Index for finding all tokens in a family (for rotation and revocation)
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);

-- Index for cleanup of expired tokens
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- Index for finding active (non-revoked) sessions by user
CREATE INDEX idx_refresh_tokens_active_sessions ON refresh_tokens (user_id, created_at DESC)
    WHERE revoked_at IS NULL;

-- Comments for documentation
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens with rotation tracking for session management';
COMMENT ON COLUMN refresh_tokens.id IS 'Primary key - UUID';
COMMENT ON COLUMN refresh_tokens.user_id IS 'Reference to the user who owns this token';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of the refresh token value';
COMMENT ON COLUMN refresh_tokens.family_id IS 'Token family ID for rotation tracking - all rotated tokens share the same family';
COMMENT ON COLUMN refresh_tokens.device_info IS 'JSON object containing browser, OS, IP address for session display';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'When this token expires (7 days normal, 30 days with remember-me)';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Timestamp when token was revoked (NULL if active)';
COMMENT ON COLUMN refresh_tokens.created_at IS 'Timestamp when token was created';

-- Example device_info structure:
-- {
--   "browser": "Chrome 120",
--   "os": "macOS 14.2",
--   "ip": "192.168.1.1",
--   "userAgent": "Mozilla/5.0 ..."
-- }
