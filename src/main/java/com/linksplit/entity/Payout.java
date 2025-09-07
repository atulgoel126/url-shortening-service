package com.linksplit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "INR";
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;
    
    @Column(name = "reference_number", unique = true)
    private String referenceNumber;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "views_included")
    private Long viewsIncluded;
    
    @Column(name = "period_start")
    private LocalDateTime periodStart;
    
    @Column(name = "period_end")
    private LocalDateTime periodEnd;
    
    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "failed_reason")
    private String failedReason;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    public enum PayoutStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}