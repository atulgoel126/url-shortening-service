-- Verify Top Performing Links Calculation
-- This script checks if the top performing links are correctly sorted by view_count

-- 1. Show top links across all users
SELECT 
    'All Users Top Links' as category,
    short_code,
    substring(long_url, 1, 50) as url_preview,
    view_count,
    estimated_earnings,
    created_at
FROM links
ORDER BY view_count DESC
LIMIT 10;

-- 2. Show top links for demo user
SELECT 
    'Demo User Top Links' as category,
    l.short_code,
    substring(l.long_url, 1, 50) as url_preview,
    l.view_count,
    l.estimated_earnings,
    l.created_at
FROM links l
JOIN users u ON l.user_id = u.id
WHERE u.email = 'demo@linksplit.com'
ORDER BY l.view_count DESC
LIMIT 10;

-- 3. Verify view counts match actual LinkView records
SELECT 
    'View Count Verification' as category,
    l.short_code,
    l.view_count as stored_count,
    COUNT(lv.id) as actual_count,
    CASE 
        WHEN l.view_count = COUNT(lv.id) THEN 'MATCH'
        ELSE 'MISMATCH'
    END as status
FROM links l
LEFT JOIN link_views lv ON l.id = lv.link_id
GROUP BY l.id, l.short_code, l.view_count
ORDER BY l.view_count DESC
LIMIT 10;

-- 4. Check if earnings calculation is correct (CPM * views / 1000 * revenue_share)
-- Assuming CPM = 1.00 and revenue_share = 0.50
SELECT 
    'Earnings Verification' as category,
    short_code,
    view_count,
    estimated_earnings,
    ROUND((view_count::numeric * 1.00 / 1000 * 0.50)::numeric, 4) as calculated_earnings,
    CASE 
        WHEN ABS(estimated_earnings - (view_count::numeric * 1.00 / 1000 * 0.50)) < 0.01 THEN 'CORRECT'
        ELSE 'INCORRECT'
    END as status
FROM links
WHERE view_count > 0
ORDER BY view_count DESC
LIMIT 10;