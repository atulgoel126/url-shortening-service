package com.linksplit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    private String baseUrl;
    private int shortcodeLength = 6;
    private int adDisplaySeconds = 5;
    private double cpmRate = 1.00;
    private double revenueShare = 0.50;
    private ViewFraudPrevention viewFraudPrevention = new ViewFraudPrevention();

    @Data
    public static class ViewFraudPrevention {
        private boolean enabled = true;
        private Map<String, RateLimit> rateLimits = new HashMap<>();
        
        @Data
        public static class RateLimit {
            private int durationMinutes;
            private int maxViews;
        }
    }
}