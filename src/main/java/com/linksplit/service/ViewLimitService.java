package com.linksplit.service;

import com.linksplit.config.AppConfig;
import com.linksplit.entity.IpViewTracker;
import com.linksplit.repository.IpViewTrackerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewLimitService {
    private final IpViewTrackerRepository ipViewTrackerRepository;
    private final AppConfig appConfig;
    
    public enum LimitType {
        FIVE_MINUTE("5-minute limit exceeded: max 5 ads per 5 minutes"),
        HOURLY("Hourly limit exceeded: max 20 ads per hour"),
        DAILY("Daily limit exceeded: max 50 ads per day"),
        ALLOWED("View allowed");
        
        private final String message;
        
        LimitType(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public LimitType checkRateLimit(String ipAddress) {
        if (!appConfig.getViewFraudPrevention().isEnabled()) {
            return LimitType.ALLOWED;
        }
        
        LocalDateTime now = LocalDateTime.now();
        Map<String, AppConfig.ViewFraudPrevention.RateLimit> limits = 
            appConfig.getViewFraudPrevention().getRateLimits();
        
        // Check 5-minute limit
        AppConfig.ViewFraudPrevention.RateLimit fiveMinLimit = limits.get("five-minutes");
        if (fiveMinLimit != null) {
            LocalDateTime fiveMinAgo = now.minusMinutes(fiveMinLimit.getDurationMinutes());
            long fiveMinCount = ipViewTrackerRepository.countViewsSince(ipAddress, fiveMinAgo);
            if (fiveMinCount >= fiveMinLimit.getMaxViews()) {
                log.warn("IP {} would exceed 5-minute limit: {} views", ipAddress, fiveMinCount);
                return LimitType.FIVE_MINUTE;
            }
        }
        
        // Check hourly limit
        AppConfig.ViewFraudPrevention.RateLimit hourlyLimit = limits.get("hourly");
        if (hourlyLimit != null) {
            LocalDateTime oneHourAgo = now.minusMinutes(hourlyLimit.getDurationMinutes());
            long hourlyCount = ipViewTrackerRepository.countViewsSince(ipAddress, oneHourAgo);
            if (hourlyCount >= hourlyLimit.getMaxViews()) {
                log.warn("IP {} would exceed hourly limit: {} views", ipAddress, hourlyCount);
                return LimitType.HOURLY;
            }
        }
        
        // Check daily limit
        AppConfig.ViewFraudPrevention.RateLimit dailyLimit = limits.get("daily");
        if (dailyLimit != null) {
            LocalDateTime oneDayAgo = now.minusMinutes(dailyLimit.getDurationMinutes());
            long dailyCount = ipViewTrackerRepository.countViewsSince(ipAddress, oneDayAgo);
            if (dailyCount >= dailyLimit.getMaxViews()) {
                log.warn("IP {} would exceed daily limit: {} views", ipAddress, dailyCount);
                return LimitType.DAILY;
            }
        }
        
        return LimitType.ALLOWED;
    }
    
    @Transactional
    public LimitType checkAndRecordView(String ipAddress) {
        if (!appConfig.getViewFraudPrevention().isEnabled()) {
            recordIpView(ipAddress);
            return LimitType.ALLOWED;
        }
        
        LocalDateTime now = LocalDateTime.now();
        Map<String, AppConfig.ViewFraudPrevention.RateLimit> limits = 
            appConfig.getViewFraudPrevention().getRateLimits();
        
        // Check 5-minute limit
        AppConfig.ViewFraudPrevention.RateLimit fiveMinLimit = limits.get("five-minutes");
        if (fiveMinLimit != null) {
            LocalDateTime fiveMinAgo = now.minusMinutes(fiveMinLimit.getDurationMinutes());
            long fiveMinCount = ipViewTrackerRepository.countViewsSince(ipAddress, fiveMinAgo);
            if (fiveMinCount >= fiveMinLimit.getMaxViews()) {
                log.warn("IP {} exceeded 5-minute limit: {} views", ipAddress, fiveMinCount);
                return LimitType.FIVE_MINUTE;
            }
        }
        
        // Check hourly limit
        AppConfig.ViewFraudPrevention.RateLimit hourlyLimit = limits.get("hourly");
        if (hourlyLimit != null) {
            LocalDateTime oneHourAgo = now.minusMinutes(hourlyLimit.getDurationMinutes());
            long hourlyCount = ipViewTrackerRepository.countViewsSince(ipAddress, oneHourAgo);
            if (hourlyCount >= hourlyLimit.getMaxViews()) {
                log.warn("IP {} exceeded hourly limit: {} views", ipAddress, hourlyCount);
                return LimitType.HOURLY;
            }
        }
        
        // Check daily limit
        AppConfig.ViewFraudPrevention.RateLimit dailyLimit = limits.get("daily");
        if (dailyLimit != null) {
            LocalDateTime oneDayAgo = now.minusMinutes(dailyLimit.getDurationMinutes());
            long dailyCount = ipViewTrackerRepository.countViewsSince(ipAddress, oneDayAgo);
            if (dailyCount >= dailyLimit.getMaxViews()) {
                log.warn("IP {} exceeded daily limit: {} views", ipAddress, dailyCount);
                return LimitType.DAILY;
            }
        }
        
        // Record the view
        recordIpView(ipAddress);
        return LimitType.ALLOWED;
    }
    
    private void recordIpView(String ipAddress) {
        IpViewTracker tracker = IpViewTracker.builder()
                .ipAddress(ipAddress)
                .viewedAt(LocalDateTime.now())
                .build();
        ipViewTrackerRepository.save(tracker);
    }
    
    @Transactional(readOnly = true)
    public ViewStats getViewStats(String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        
        long fiveMinCount = ipViewTrackerRepository.countViewsSince(
            ipAddress, now.minusMinutes(5));
        long hourlyCount = ipViewTrackerRepository.countViewsSince(
            ipAddress, now.minusHours(1));
        long dailyCount = ipViewTrackerRepository.countViewsSince(
            ipAddress, now.minusDays(1));
        
        return ViewStats.builder()
                .fiveMinuteCount(fiveMinCount)
                .hourlyCount(hourlyCount)
                .dailyCount(dailyCount)
                .build();
    }
    
    @lombok.Builder
    @lombok.Data
    public static class ViewStats {
        private long fiveMinuteCount;
        private long hourlyCount;
        private long dailyCount;
    }
    
    // Clean up old records every hour to prevent table bloat
    @Scheduled(fixedDelay = 3600000) // 1 hour
    @Transactional
    public void cleanupOldRecords() {
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        ipViewTrackerRepository.deleteOldRecords(twoDaysAgo);
        log.info("Cleaned up IP view tracker records older than 2 days");
    }
}