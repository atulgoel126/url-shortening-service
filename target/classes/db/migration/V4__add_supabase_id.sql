-- Add supabase_id column to users table for Supabase authentication
ALTER TABLE users ADD COLUMN IF NOT EXISTS supabase_id VARCHAR(255) UNIQUE;

-- Create index on supabase_id for performance
CREATE INDEX IF NOT EXISTS idx_users_supabase_id ON users(supabase_id);

-- Make password_hash nullable since Supabase handles authentication
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;