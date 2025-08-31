package com.linksplit.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .recordStats());
        
        // Register all cache names
        cacheManager.setCacheNames(java.util.List.of(
                "shortUrls",
                "geoLocation",
                "userAgent"
        ));
        
        return cacheManager;
    }
}