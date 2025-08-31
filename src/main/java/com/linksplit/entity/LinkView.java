package com.linksplit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "link_views",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_view_per_hour",
                columnNames = {"link_id", "ip_address", "viewed_at"}
        ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    // Geographic data
    @Column(name = "country")
    private String country;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "region")
    private String region;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    // Device and browser info
    @Column(name = "device_type")
    private String deviceType; // mobile, tablet, desktop
    
    @Column(name = "browser")
    private String browser;
    
    @Column(name = "browser_version")
    private String browserVersion;
    
    @Column(name = "operating_system")
    private String operatingSystem;
    
    @Column(name = "os_version")
    private String osVersion;
    
    // Traffic source
    @Column(name = "referrer")
    private String referrer;
    
    @Column(name = "utm_source")
    private String utmSource;
    
    @Column(name = "utm_medium")
    private String utmMedium;
    
    @Column(name = "utm_campaign")
    private String utmCampaign;
    
    // Engagement metrics
    @Column(name = "time_to_skip")
    private Integer timeToSkip; // seconds before clicking continue
    
    @Column(name = "ad_completed")
    private Boolean adCompleted;
    
    @Column(name = "session_id")
    private String sessionId;

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
    }
}