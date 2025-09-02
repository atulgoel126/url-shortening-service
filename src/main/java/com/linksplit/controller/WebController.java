package com.linksplit.controller;

import com.linksplit.dto.DashboardStats;
import com.linksplit.dto.LoginRequest;
import com.linksplit.dto.RegisterRequest;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.repository.LinkRepository;
import com.linksplit.repository.LinkViewRepository;
import com.linksplit.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {
    private final UserService userService;
    private final LinkRepository linkRepository;
    private final LinkViewRepository linkViewRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("isLoggedIn", 
            SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
            !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser"));
        return "index";
    }

    // Legacy registration and login methods - now handled by Supabase
    @GetMapping("/register")
    public String showRegistrationForm() {
        return "redirect:/auth/register";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "redirect:/auth/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
    
    @GetMapping("/logout")
    public String logout() {
        return "redirect:/auth/logout";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication,
                           @RequestParam(defaultValue = "0") int page) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        User user = userService.getUserByEmail(authentication.getName());
        
        // Redirect admin users to admin dashboard
        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin";
        }
        
        Page<Link> userLinks = linkRepository.findByUserWithCommentsOrderByCreatedAtDesc(
            user, PageRequest.of(page, 10)
        );
        
        Long totalViews = linkRepository.getTotalViewsByUser(user);
        BigDecimal totalEarnings = linkRepository.getTotalEarningsByUser(user);
        
        // Calculate today's clicks
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        Long todayClicks = linkViewRepository.getTodayClicksByLinks(
            linkRepository.findByUser(user), 
            startOfDay, 
            endOfDay
        );
        
        DashboardStats stats = DashboardStats.builder()
            .totalLinks(userLinks.getTotalElements())
            .totalViews(totalViews != null ? totalViews : 0L)
            .todayClicks(todayClicks != null ? todayClicks : 0L)
            .totalEarnings(totalEarnings != null ? totalEarnings : BigDecimal.ZERO)
            .build();
        
        model.addAttribute("stats", stats);
        model.addAttribute("links", userLinks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userLinks.getTotalPages());
        
        return "dashboard";
    }
}