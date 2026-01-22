-- Initial database setup for PFWA
-- This runs automatically when the PostgreSQL container is first created

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant privileges (if additional setup needed)
-- The main schema will be created by Flyway migrations

-- Log that initialization completed
DO $$
BEGIN
    RAISE NOTICE 'PFWA database initialized successfully';
END $$;
