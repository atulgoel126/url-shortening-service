package com.linksplit.controller;

import com.linksplit.entity.Link;
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
        
        model.addAttribute("user", user);
        model.addAttribute("links", userLinks);
        model.addAttribute("totalViews", totalViews != null ? totalViews : 0L);
        model.addAttribute("totalEarnings", totalEarnings != null ? totalEarnings : BigDecimal.ZERO);
        model.addAttribute("linkCount", userLinks.size());
        
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
}