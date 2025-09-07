package com.linksplit.controller;

import com.linksplit.config.AppConfig;
import com.linksplit.entity.Link;
import com.linksplit.entity.Payout;
import com.linksplit.entity.User;
import com.linksplit.repository.LinkRepository;
import com.linksplit.repository.PayoutRepository;
import com.linksplit.repository.UserRepository;
import com.linksplit.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final PayoutRepository payoutRepository;
    private final RevenueService revenueService;
    private final AppConfig appConfig;
    
    @GetMapping
    public String adminDashboard(Model model) {
        // Overall statistics
        long totalUsers = userRepository.count();
        long totalLinks = linkRepository.count();
        Long totalViews = linkRepository.findAll().stream()
                .mapToLong(Link::getViewCount)
                .sum();
        
        BigDecimal totalEarnings = linkRepository.findAll().stream()
                .map(Link::getEstimatedEarnings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Recent users
        List<User> recentUsers = userRepository.findTop10ByOrderByCreatedAtDesc();
        
        // Top creators by earnings
        List<Object[]> topCreators = userRepository.findTopCreatorsByEarnings(PageRequest.of(0, 10));
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalLinks", totalLinks);
        model.addAttribute("totalViews", totalViews);
        model.addAttribute("totalEarnings", totalEarnings);
        model.addAttribute("recentUsers", recentUsers);
        model.addAttribute("topCreators", topCreators);
        
        return "admin/dashboard";
    }
    
    @GetMapping("/users")
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        Page<User> users = userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        
        // Calculate stats for each user
        Map<Long, Map<String, Object>> userStats = new HashMap<>();
        for (User user : users.getContent()) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("linkCount", linkRepository.countByUser(user));
            stats.put("totalViews", linkRepository.getTotalViewsByUser(user));
            stats.put("totalEarnings", linkRepository.getTotalEarningsByUser(user));
            userStats.put(user.getId(), stats);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("userStats", userStats);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        
        return "admin/users";
    }
    
    @GetMapping("/user/{id}")
    public String userDetails(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<Link> userLinks = linkRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 50)).getContent();
        
        Long totalViews = linkRepository.getTotalViewsByUser(user);
        BigDecimal totalEarnings = linkRepository.getTotalEarningsByUser(user);
        
        // Fetch user's payouts
        List<Payout> userPayouts = payoutRepository.findByUserOrderByRequestedAtDesc(user, PageRequest.of(0, 10)).getContent();
        BigDecimal totalPaidOut = payoutRepository.getTotalPayoutsByUserAndStatus(user, Payout.PayoutStatus.COMPLETED);
        BigDecimal pendingPayouts = payoutRepository.getTotalPayoutsByUserAndStatus(user, Payout.PayoutStatus.PENDING);
        
        model.addAttribute("user", user);
        model.addAttribute("links", userLinks);
        model.addAttribute("totalViews", totalViews != null ? totalViews : 0L);
        model.addAttribute("totalEarnings", totalEarnings != null ? totalEarnings : BigDecimal.ZERO);
        model.addAttribute("linkCount", userLinks.size());
        model.addAttribute("appConfig", appConfig);
        model.addAttribute("payouts", userPayouts);
        model.addAttribute("totalPaidOut", totalPaidOut != null ? totalPaidOut : BigDecimal.ZERO);
        model.addAttribute("pendingPayouts", pendingPayouts != null ? pendingPayouts : BigDecimal.ZERO);
        
        return "admin/user-details";
    }
    
    @GetMapping("/links")
    public String listLinks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {
        
        Page<Link> links = linkRepository.findAll(PageRequest.of(page, size));
        
        model.addAttribute("links", links);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", links.getTotalPages());
        
        return "admin/links";
    }
    
    @PostMapping("/user/{id}/revenue")
    public String updateUserRevenue(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal cpmRate,
            @RequestParam(required = false) BigDecimal revenueShare,
            @RequestParam(required = false) boolean retroactive,
            @RequestParam(required = false) String reset,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            if ("true".equals(reset)) {
                // Reset to default rates
                user.setCustomCpmRate(null);
                user.setCustomRevenueShare(null);
                userRepository.save(user);
                
                if (retroactive) {
                    revenueService.recalculateUserEarnings(user);
                }
                
                redirectAttributes.addFlashAttribute("success", "Revenue settings reset to default");
            } else {
                // Update custom rates
                if (cpmRate != null && cpmRate.compareTo(BigDecimal.ZERO) > 0) {
                    user.setCustomCpmRate(cpmRate);
                }
                
                if (revenueShare != null && revenueShare.compareTo(BigDecimal.ZERO) >= 0 
                        && revenueShare.compareTo(new BigDecimal("100")) <= 0) {
                    user.setCustomRevenueShare(revenueShare.divide(new BigDecimal("100")));
                }
                
                userRepository.save(user);
                
                if (retroactive) {
                    revenueService.recalculateUserEarnings(user);
                }
                
                redirectAttributes.addFlashAttribute("success", 
                    retroactive ? "Revenue settings updated and applied retroactively" 
                                : "Revenue settings updated for future earnings");
            }
        } catch (Exception e) {
            log.error("Error updating revenue settings for user {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update revenue settings: " + e.getMessage());
        }
        
        return "redirect:/admin/user/" + id;
    }
    
    @GetMapping("/payouts")
    public String listPayouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Model model) {
        
        Page<Payout> payouts;
        if (status != null && !status.isEmpty()) {
            try {
                Payout.PayoutStatus payoutStatus = Payout.PayoutStatus.valueOf(status.toUpperCase());
                payouts = payoutRepository.findByStatusOrderByRequestedAtDesc(payoutStatus, PageRequest.of(page, size));
            } catch (IllegalArgumentException e) {
                // Invalid status, show all
                payouts = payoutRepository.findAllByOrderByRequestedAtDesc(PageRequest.of(page, size));
            }
        } else {
            payouts = payoutRepository.findAllByOrderByRequestedAtDesc(PageRequest.of(page, size));
        }
        
        // Calculate statistics
        long pendingCount = payoutRepository.countByStatus(Payout.PayoutStatus.PENDING);
        long processingCount = payoutRepository.countByStatus(Payout.PayoutStatus.PROCESSING);
        long completedCount = payoutRepository.countByStatus(Payout.PayoutStatus.COMPLETED);
        long failedCount = payoutRepository.countByStatus(Payout.PayoutStatus.FAILED);
        
        BigDecimal totalPending = payoutRepository.sumAmountByStatus(Payout.PayoutStatus.PENDING);
        BigDecimal totalProcessing = payoutRepository.sumAmountByStatus(Payout.PayoutStatus.PROCESSING);
        BigDecimal totalCompleted = payoutRepository.sumAmountByStatus(Payout.PayoutStatus.COMPLETED);
        
        model.addAttribute("payouts", payouts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", payouts.getTotalPages());
        model.addAttribute("currentStatus", status);
        
        // Statistics
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("processingCount", processingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("totalPending", totalPending != null ? totalPending : BigDecimal.ZERO);
        model.addAttribute("totalProcessing", totalProcessing != null ? totalProcessing : BigDecimal.ZERO);
        model.addAttribute("totalCompleted", totalCompleted != null ? totalCompleted : BigDecimal.ZERO);
        
        return "admin/payouts";
    }
    
    @PostMapping("/payout/{id}/status")
    public String updatePayoutStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String failedReason,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        
        log.info("Updating payout status for ID: {}, new status: {}", id, status);
        
        try {
            Payout payout = payoutRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Payout not found with ID: " + id));
            
            Payout.PayoutStatus newStatus = Payout.PayoutStatus.valueOf(status.toUpperCase());
            Payout.PayoutStatus oldStatus = payout.getStatus();
            payout.setStatus(newStatus);
            
            if (newStatus == Payout.PayoutStatus.COMPLETED) {
                payout.setProcessedAt(LocalDateTime.now());
                if (transactionId != null && !transactionId.isEmpty()) {
                    payout.setTransactionId(transactionId);
                }
            } else if (newStatus == Payout.PayoutStatus.FAILED) {
                payout.setProcessedAt(LocalDateTime.now());
                if (failedReason != null && !failedReason.isEmpty()) {
                    payout.setFailedReason(failedReason);
                }
            } else if (newStatus == Payout.PayoutStatus.PROCESSING) {
                payout.setProcessedAt(LocalDateTime.now());
            }
            
            if (notes != null && !notes.isEmpty()) {
                payout.setNotes(notes);
            }
            
            payoutRepository.save(payout);
            
            log.info("Successfully updated payout {} from {} to {}", id, oldStatus, newStatus);
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Payout #%d status updated from %s to %s", id, oldStatus, newStatus));
            
        } catch (Exception e) {
            log.error("Error updating payout status for id {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", 
                "Failed to update payout status: " + e.getMessage());
        }
        
        return "redirect:/admin/payouts";
    }
}