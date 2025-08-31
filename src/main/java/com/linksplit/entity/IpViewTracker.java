package com.linksplit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ip_view_tracker",
        indexes = {
            @Index(name = "idx_ip_viewed_at", columnList = "ip_address, viewed_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpViewTracker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
    
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
    
    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
    }
}