-- Add custom revenue rate fields to users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS custom_cpm_rate DECIMAL(10, 4),
ADD COLUMN IF NOT EXISTS custom_revenue_share DECIMAL(5, 4);

-- Add comments for documentation
COMMENT ON COLUMN users.custom_cpm_rate IS 'Custom CPM rate in INR per 1000 views for this user';
COMMENT ON COLUMN users.custom_revenue_share IS 'Custom revenue share percentage (0.0 to 1.0) for this user';

-- Create index for users with custom rates for faster queries
CREATE INDEX IF NOT EXISTS idx_users_custom_rates 
ON users(custom_cpm_rate, custom_revenue_share) 
WHERE custom_cpm_rate IS NOT NULL OR custom_revenue_share IS NOT NULL;