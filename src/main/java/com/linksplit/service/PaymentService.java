package com.linksplit.service;

import com.linksplit.entity.PaymentMethod;
import com.linksplit.entity.Payout;
import com.linksplit.entity.User;
import com.linksplit.repository.LinkRepository;
import com.linksplit.repository.PaymentMethodRepository;
import com.linksplit.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentMethodRepository paymentMethodRepository;
    private final PayoutRepository payoutRepository;
    private final LinkRepository linkRepository;
    
    private static final BigDecimal MINIMUM_PAYOUT = new BigDecimal("100.00"); // Minimum ₹100 for payout
    
    @Transactional
    public PaymentMethod addUpiId(User user, String upiId, String accountHolderName) {
        // Check if UPI ID already exists for this user
        Optional<PaymentMethod> existing = paymentMethodRepository.findByUserAndUpiId(user, upiId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("UPI ID already exists");
        }
        
        // If this is the first payment method, make it primary
        boolean isFirstPaymentMethod = paymentMethodRepository.countByUser(user) == 0;
        
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(user)
                .paymentType(PaymentMethod.PaymentType.UPI)
                .upiId(upiId)
                .accountHolderName(accountHolderName)
                .isPrimary(isFirstPaymentMethod)
                .isVerified(false) // In production, you'd verify the UPI ID
                .build();
        
        return paymentMethodRepository.save(paymentMethod);
    }
    
    @Transactional
    public void setPrimaryPaymentMethod(User user, Long paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        
        if (!paymentMethod.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Payment method does not belong to user");
        }
        
        // Clear existing primary
        paymentMethodRepository.clearPrimaryForUser(user);
        
        // Set new primary
        paymentMethod.setIsPrimary(true);
        paymentMethodRepository.save(paymentMethod);
    }
    
    @Transactional
    public void deletePaymentMethod(User user, Long paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        
        if (!paymentMethod.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Payment method does not belong to user");
        }
        
        // Don't delete if it's the only payment method
        if (paymentMethodRepository.countByUser(user) == 1) {
            throw new IllegalArgumentException("Cannot delete the only payment method");
        }
        
        // If deleting primary, make another one primary
        if (paymentMethod.getIsPrimary()) {
            paymentMethodRepository.delete(paymentMethod);
            List<PaymentMethod> remaining = paymentMethodRepository.findByUserOrderByIsPrimaryDescCreatedAtDesc(user);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsPrimary(true);
                paymentMethodRepository.save(remaining.get(0));
            }
        } else {
            paymentMethodRepository.delete(paymentMethod);
        }
    }
    
    public List<PaymentMethod> getUserPaymentMethods(User user) {
        return paymentMethodRepository.findByUserOrderByIsPrimaryDescCreatedAtDesc(user);
    }
    
    @Transactional
    public Payout requestPayout(User user) {
        // Get user's total earnings
        BigDecimal totalEarnings = linkRepository.getTotalEarningsByUser(user);
        if (totalEarnings == null) {
            totalEarnings = BigDecimal.ZERO;
        }
        
        // Get already paid out amount
        BigDecimal totalPaidOut = payoutRepository.getTotalPayoutsByUserAndStatus(user, Payout.PayoutStatus.COMPLETED);
        if (totalPaidOut == null) {
            totalPaidOut = BigDecimal.ZERO;
        }
        
        // Calculate available balance
        BigDecimal availableBalance = totalEarnings.subtract(totalPaidOut);
        
        // Check minimum payout threshold
        if (availableBalance.compareTo(MINIMUM_PAYOUT) < 0) {
            throw new IllegalArgumentException("Minimum payout amount is ₹" + MINIMUM_PAYOUT);
        }
        
        // Get primary payment method
        PaymentMethod primaryPaymentMethod = paymentMethodRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new IllegalArgumentException("No primary payment method found"));
        
        // Get total views for this payout
        Long totalViews = linkRepository.getTotalViewsByUser(user);
        
        // Create payout request
        Payout payout = Payout.builder()
                .user(user)
                .paymentMethod(primaryPaymentMethod)
                .amount(availableBalance)
                .currency("INR")
                .status(Payout.PayoutStatus.PENDING)
                .referenceNumber(generateReferenceNumber())
                .viewsIncluded(totalViews)
                .periodStart(user.getCreatedAt())
                .periodEnd(LocalDateTime.now())
                .notes("Payout requested via " + primaryPaymentMethod.getUpiId())
                .build();
        
        return payoutRepository.save(payout);
    }
    
    @Transactional
    public void cancelPayout(User user, Long payoutId) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found"));
        
        if (!payout.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Payout does not belong to user");
        }
        
        if (payout.getStatus() != Payout.PayoutStatus.PENDING) {
            throw new IllegalArgumentException("Only pending payouts can be cancelled");
        }
        
        payout.setStatus(Payout.PayoutStatus.CANCELLED);
        payout.setProcessedAt(LocalDateTime.now());
        payoutRepository.save(payout);
    }
    
    public BigDecimal getAvailableBalance(User user) {
        BigDecimal totalEarnings = linkRepository.getTotalEarningsByUser(user);
        if (totalEarnings == null) {
            totalEarnings = BigDecimal.ZERO;
        }
        
        BigDecimal totalPaidOut = payoutRepository.getTotalPayoutsByUserAndStatus(user, Payout.PayoutStatus.COMPLETED);
        if (totalPaidOut == null) {
            totalPaidOut = BigDecimal.ZERO;
        }
        
        BigDecimal pendingPayouts = payoutRepository.getTotalPayoutsByUserAndStatus(user, Payout.PayoutStatus.PENDING);
        if (pendingPayouts == null) {
            pendingPayouts = BigDecimal.ZERO;
        }
        
        return totalEarnings.subtract(totalPaidOut).subtract(pendingPayouts);
    }
    
    // Scheduled job to process payouts (runs daily at 2 AM)
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void processPayouts() {
        log.info("Starting payout processing job");
        
        List<Payout> pendingPayouts = payoutRepository.findPendingPayoutsOlderThan(
                Payout.PayoutStatus.PENDING, 
                LocalDateTime.now().minusHours(1) // Process payouts older than 1 hour
        );
        
        for (Payout payout : pendingPayouts) {
            try {
                processPayout(payout);
            } catch (Exception e) {
                log.error("Failed to process payout {}: {}", payout.getId(), e.getMessage());
                payout.setStatus(Payout.PayoutStatus.FAILED);
                payout.setFailedReason(e.getMessage());
                payout.setProcessedAt(LocalDateTime.now());
                payoutRepository.save(payout);
            }
        }
        
        log.info("Completed payout processing for {} payouts", pendingPayouts.size());
    }
    
    private void processPayout(Payout payout) {
        // In production, this would integrate with actual payment gateway
        // For demo, we'll just mark as completed
        payout.setStatus(Payout.PayoutStatus.COMPLETED);
        payout.setTransactionId("TXN" + System.currentTimeMillis());
        payout.setProcessedAt(LocalDateTime.now());
        payoutRepository.save(payout);
        
        log.info("Processed payout {} for user {} amount ₹{}", 
                payout.getReferenceNumber(), 
                payout.getUser().getEmail(), 
                payout.getAmount());
    }
    
    private String generateReferenceNumber() {
        String reference;
        do {
            reference = "PAY" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (payoutRepository.existsByReferenceNumber(reference));
        return reference;
    }
}