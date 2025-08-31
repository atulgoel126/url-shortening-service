package com.linksplit.controller;

import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.repository.LinkRepository;
import com.linksplit.repository.PayoutRepository;
import com.linksplit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
}