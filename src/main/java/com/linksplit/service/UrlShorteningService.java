package com.linksplit.service;

import com.linksplit.config.AppConfig;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.repository.LinkRepository;
import com.linksplit.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShorteningService {
    private final LinkRepository linkRepository;
    private final Base62Encoder base62Encoder;
    private final AppConfig appConfig;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Link createShortUrl(String longUrl, User user) {
        validateUrl(longUrl);
        
        String shortCode = generateUniqueShortCode();
        
        Link link = Link.builder()
                .shortCode(shortCode)
                .longUrl(longUrl)
                .user(user)
                .build();
        
        Link savedLink = linkRepository.save(link);
        log.info("Created short URL: {} for long URL: {}", shortCode, longUrl);
        
        return savedLink;
    }

    @Cacheable(value = "shortUrls", key = "#shortCode")
    public Optional<Link> getLinkByShortCode(String shortCode) {
        return linkRepository.findByShortCode(shortCode);
    }

    public String getFullShortUrl(String shortCode) {
        return appConfig.getBaseUrl() + "/" + shortCode;
    }

    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        final int maxAttempts = 10;
        
        do {
            long randomNumber = Math.abs(random.nextLong());
            shortCode = base62Encoder.encode(randomNumber);
            
            if (shortCode.length() > appConfig.getShortcodeLength()) {
                shortCode = shortCode.substring(0, appConfig.getShortcodeLength());
            } else if (shortCode.length() < appConfig.getShortcodeLength()) {
                shortCode = padLeft(shortCode, appConfig.getShortcodeLength());
            }
            
            attempts++;
            if (attempts >= maxAttempts) {
                throw new RuntimeException("Failed to generate unique short code after " + maxAttempts + " attempts");
            }
        } while (linkRepository.existsByShortCode(shortCode));
        
        return shortCode;
    }

    private String padLeft(String str, int length) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }

    private void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
        
        if (url.length() > 2048) {
            throw new IllegalArgumentException("URL is too long (max 2048 characters)");
        }
        
        try {
            new java.net.URL(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        }
    }
}