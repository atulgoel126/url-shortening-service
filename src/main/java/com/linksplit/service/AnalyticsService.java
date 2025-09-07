package com.linksplit.service;

import com.linksplit.config.AppConfig;
import com.linksplit.entity.Link;
import com.linksplit.entity.LinkView;
import com.linksplit.repository.LinkRepository;
import com.linksplit.repository.LinkViewRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final LinkRepository linkRepository;
    private final LinkViewRepository linkViewRepository;
    private final ViewLimitService viewLimitService;
    private final GeoLocationService geoLocationService;
    private final AppConfig appConfig;
    private final RevenueService revenueService;

    @Transactional
    public boolean recordView(Link link, HttpServletRequest request) {
        return recordView(link, request, null, null);
    }
    
    @Transactional
    public boolean recordView(Link link, HttpServletRequest request, Integer timeToSkip) {
        return recordView(link, request, timeToSkip, null);
    }
    
    @Transactional
    public boolean recordView(Link link, HttpServletRequest request, Integer timeToSkip, String originalReferrer) {
        String ipAddress = extractIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Check multi-stage rate limits
        ViewLimitService.LimitType limitResult = viewLimitService.checkAndRecordView(ipAddress);
        
        if (limitResult != ViewLimitService.LimitType.ALLOWED) {
            log.info("View blocked for IP {}: {}", ipAddress, limitResult.getMessage());
            linkRepository.incrementDuplicateViewCount(link.getId());
            
            // Store the limit message in request for display
            request.setAttribute("rateLimitMessage", limitResult.getMessage());
            return false;
        }
        
        try {
            // Get geographic location from IP
            GeoLocationService.GeoLocation location = geoLocationService.getLocation(ipAddress);
            
            // Parse user agent for device and browser info
            DeviceInfo deviceInfo = parseUserAgent(userAgent);
            
            LinkView view = LinkView.builder()
                    .link(link)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .viewedAt(LocalDateTime.now())
                    .country(location.getCountry())
                    .city(location.getCity())
                    .region(location.getRegion())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .deviceType(deviceInfo.deviceType)
                    .browser(deviceInfo.browser)
                    .operatingSystem(deviceInfo.os)
                    .referrer(originalReferrer != null ? originalReferrer : request.getHeader("Referer"))
                    .timeToSkip(timeToSkip) // Now properly set from frontend
                    .adCompleted(true) // This is set when ad completion is recorded
                    .build();
            
            // Log referrer information for debugging
            String finalReferrer = originalReferrer != null ? originalReferrer : request.getHeader("Referer");
            log.info("Recording view for link {} with referrer: {} (original: {}, current: {})", 
                link.getShortCode(), finalReferrer, originalReferrer, request.getHeader("Referer"));
            
            linkViewRepository.save(view);
            linkRepository.incrementViewCount(link.getId());
            
            // Flush to ensure view count is updated in database
            linkRepository.flush();
            
            // Fetch the updated link to get the correct view count
            Link updatedLink = linkRepository.findById(link.getId())
                    .orElseThrow(() -> new RuntimeException("Link not found after update"));
            
            // Update earnings based on the actual new view count
            Long oldViewCount = link.getViewCount();
            Long newViewCount = updatedLink.getViewCount();
            BigDecimal oldEarnings = link.getEstimatedEarnings();
            BigDecimal newEarnings = link.getUser() != null 
                ? revenueService.calculateEarnings(newViewCount, link.getUser())
                : calculateEarnings(newViewCount);
            linkRepository.updateEarnings(link.getId(), newEarnings);
            
            log.info("Recorded view for link {} from IP {}. Views: {} -> {}, Earnings: {} -> {}", 
                link.getShortCode(), ipAddress, oldViewCount, newViewCount, oldEarnings, newEarnings);
            return true;
        } catch (Exception e) {
            log.error("Failed to record view for link {}: {}", link.getShortCode(), e.getMessage());
            return false;
        }
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void updateEarnings() {
        log.info("Starting earnings calculation job");
        
        List<Link> allLinks = linkRepository.findAll();
        
        for (Link link : allLinks) {
            if (link.getViewCount() > 0) {
                // IMPORTANT: Use user-specific rates if the link has a user
                BigDecimal earnings;
                if (link.getUser() != null) {
                    // Use RevenueService which respects custom user rates
                    earnings = revenueService.calculateEarnings(link.getViewCount(), link.getUser());
                } else {
                    // Only use default rates for links without users (shouldn't happen)
                    earnings = calculateEarnings(link.getViewCount());
                }
                linkRepository.updateEarnings(link.getId(), earnings);
            }
        }
        
        log.info("Completed earnings calculation for {} links", allLinks.size());
    }

    public BigDecimal calculateEarnings(Long viewCount) {
        if (viewCount == null || viewCount == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal views = new BigDecimal(viewCount);
        BigDecimal cpm = new BigDecimal(appConfig.getCpmRate());
        BigDecimal revenueShare = new BigDecimal(appConfig.getRevenueShare());
        
        return views.divide(new BigDecimal(1000), 10, RoundingMode.HALF_UP)
                .multiply(cpm)
                .multiply(revenueShare)
                .setScale(4, RoundingMode.HALF_UP);
    }

    public String extractIpAddress(HttpServletRequest request) {
        // Check CF-Connecting-IP (Cloudflare)
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty()) {
            return cfConnectingIp.trim();
        }
        
        // Check X-Forwarded-For (standard proxy header)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP (nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        
        // Check X-Original-Forwarded-For (some proxies)
        String xOriginalForwardedFor = request.getHeader("X-Original-Forwarded-For");
        if (xOriginalForwardedFor != null && !xOriginalForwardedFor.isEmpty()) {
            return xOriginalForwardedFor.split(",")[0].trim();
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }
    
    private DeviceInfo parseUserAgent(String userAgent) {
        DeviceInfo info = new DeviceInfo();
        
        if (userAgent == null) {
            info.deviceType = "Unknown";
            info.browser = "Unknown";
            info.os = "Unknown";
            return info;
        }
        
        // Detect device type
        if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
            info.deviceType = "Mobile";
        } else if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
            info.deviceType = "Tablet";
        } else {
            info.deviceType = "Desktop";
        }
        
        // Detect browser
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            info.browser = "Chrome";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            info.browser = "Safari";
        } else if (userAgent.contains("Firefox")) {
            info.browser = "Firefox";
        } else if (userAgent.contains("Edg")) {
            info.browser = "Edge";
        } else {
            info.browser = "Other";
        }
        
        // Detect OS
        if (userAgent.contains("Windows")) {
            info.os = "Windows";
        } else if (userAgent.contains("Mac OS")) {
            info.os = "macOS";
        } else if (userAgent.contains("Linux")) {
            info.os = "Linux";
        } else if (userAgent.contains("Android")) {
            info.os = "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            info.os = "iOS";
        } else {
            info.os = "Other";
        }
        
        return info;
    }
    
    private static class DeviceInfo {
        String deviceType;
        String browser;
        String os;
    }
}