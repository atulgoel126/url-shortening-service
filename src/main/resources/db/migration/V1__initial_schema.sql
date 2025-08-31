-- LinkSplit Database Schema
-- Version 1.0 - Complete schema matching all entity classes

-- 1. Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create links table
CREATE TABLE links (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    short_code VARCHAR(10) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    duplicate_view_count BIGINT NOT NULL DEFAULT 0,
    estimated_earnings DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Create link_views table with all analytics columns
CREATE TABLE link_views (
    id BIGSERIAL PRIMARY KEY,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    -- Geographic data
    country VARCHAR(255),
    city VARCHAR(255),
    region VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    -- Device and browser info
    device_type VARCHAR(255),
    browser VARCHAR(255),
    browser_version VARCHAR(255),
    operating_system VARCHAR(255),
    os_version VARCHAR(255),
    -- Traffic source
    referrer TEXT,
    utm_source VARCHAR(255),
    utm_medium VARCHAR(255),
    utm_campaign VARCHAR(255),
    -- Engagement metrics
    time_to_skip INTEGER,
    ad_completed BOOLEAN,
    session_id VARCHAR(255)
);

-- 4. Create payment_methods table
CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payment_type VARCHAR(50) NOT NULL,
    upi_id VARCHAR(255),
    account_holder_name VARCHAR(255),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- 5. Create payouts table
CREATE TABLE payouts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payment_method_id BIGINT REFERENCES payment_methods(id),
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reference_number VARCHAR(255) UNIQUE,
    transaction_id VARCHAR(255),
    views_included BIGINT,
    period_start TIMESTAMP WITH TIME ZONE,
    period_end TIMESTAMP WITH TIME ZONE,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    failed_reason VARCHAR(255),
    notes TEXT
);

-- 6. Create click_heatmaps table for heatmap analytics
CREATE TABLE click_heatmaps (
    id BIGSERIAL PRIMARY KEY,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    page_url VARCHAR(255) NOT NULL,
    x_coordinate INTEGER NOT NULL,
    y_coordinate INTEGER NOT NULL,
    viewport_width INTEGER,
    viewport_height INTEGER,
    element_type VARCHAR(255),
    element_text VARCHAR(500),
    element_id VARCHAR(255),
    element_class VARCHAR(255),
    session_id VARCHAR(255) NOT NULL,
    clicked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Create ip_view_tracker table for rate limiting
CREATE TABLE ip_view_tracker (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    viewed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Create indexes for performance optimization
-- User indexes
CREATE INDEX idx_users_email ON users(email);

-- Link indexes
CREATE INDEX idx_links_short_code ON links(short_code);
CREATE INDEX idx_links_user_id ON links(user_id);

-- Link view indexes
CREATE INDEX idx_link_views_link_id ON link_views(link_id);
CREATE INDEX idx_link_views_viewed_at ON link_views(viewed_at);
CREATE INDEX idx_link_views_ip_address ON link_views(ip_address);

-- Payment method indexes
CREATE INDEX idx_payment_methods_user_id ON payment_methods(user_id);

-- Payout indexes
CREATE INDEX idx_payouts_user_id ON payouts(user_id);
CREATE INDEX idx_payouts_status ON payouts(status);
CREATE INDEX idx_payouts_requested_at ON payouts(requested_at);

-- Click heatmap indexes
CREATE INDEX idx_click_heatmaps_link_id ON click_heatmaps(link_id);
CREATE INDEX idx_click_heatmaps_session_id ON click_heatmaps(session_id);
CREATE INDEX idx_click_heatmaps_clicked_at ON click_heatmaps(clicked_at);

-- IP view tracker indexes
CREATE INDEX idx_ip_view_tracker_ip ON ip_view_tracker(ip_address, viewed_at);

-- 9. Optional: Add unique constraint for preventing duplicate views within same hour
-- Uncomment if you want to enforce this at database level
-- ALTER TABLE link_views ADD CONSTRAINT unique_view_per_hour UNIQUE (link_id, ip_address, viewed_at);