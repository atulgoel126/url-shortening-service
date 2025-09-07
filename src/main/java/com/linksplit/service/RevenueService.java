package com.linksplit.service;

import com.linksplit.config.AppConfig;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {
    
    private final LinkRepository linkRepository;
    private final AppConfig appConfig;
    
    /**
     * Calculate earnings for a specific number of views based on user's custom rates or defaults
     */
    public BigDecimal calculateEarnings(Long viewCount, User user) {
        if (viewCount == null || viewCount == 0) {
            return BigDecimal.ZERO;
        }
        
        // Get CPM rate (custom or default)
        BigDecimal cpmRate = user.getCustomCpmRate() != null 
                ? user.getCustomCpmRate() 
                : BigDecimal.valueOf(appConfig.getCpmRate());
        
        // Get revenue share (custom or default)
        BigDecimal revenueShare = user.getCustomRevenueShare() != null 
                ? user.getCustomRevenueShare() 
                : BigDecimal.valueOf(appConfig.getRevenueShare());
        
        // Calculate: (views / 1000) * cpmRate * revenueShare
        BigDecimal earnings = BigDecimal.valueOf(viewCount)
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                .multiply(cpmRate)
                .multiply(revenueShare)
                .setScale(4, RoundingMode.HALF_UP);
        
        log.debug("Calculated earnings for {} views: CPM={}, Share={}, Earnings={}", 
                viewCount, cpmRate, revenueShare, earnings);
        
        return earnings;
    }
    
    /**
     * Recalculate all earnings for a user based on their current rates
     */
    @Transactional
    public void recalculateUserEarnings(User user) {
        log.info("Recalculating earnings for user: {}", user.getEmail());
        
        List<Link> userLinks = linkRepository.findByUser(user);
        BigDecimal totalRecalculated = BigDecimal.ZERO;
        
        for (Link link : userLinks) {
            BigDecimal oldEarnings = link.getEstimatedEarnings();
            BigDecimal newEarnings = calculateEarnings(link.getViewCount(), user);
            
            if (oldEarnings.compareTo(newEarnings) != 0) {
                link.setEstimatedEarnings(newEarnings);
                linkRepository.save(link);
                
                log.debug("Updated link {} earnings from {} to {}", 
                    link.getShortCode(), oldEarnings, newEarnings);
            }
            
            totalRecalculated = totalRecalculated.add(newEarnings);
        }
        
        log.info("Recalculation complete for user {}. Total earnings: {}", 
            user.getEmail(), totalRecalculated);
    }
    
    /**
     * Get effective CPM rate for a user
     */
    public BigDecimal getEffectiveCpmRate(User user) {
        return user.getCustomCpmRate() != null 
                ? user.getCustomCpmRate() 
                : BigDecimal.valueOf(appConfig.getCpmRate());
    }
    
    /**
     * Get effective revenue share for a user
     */
    public BigDecimal getEffectiveRevenueShare(User user) {
        return user.getCustomRevenueShare() != null 
                ? user.getCustomRevenueShare() 
                : BigDecimal.valueOf(appConfig.getRevenueShare());
    }
}