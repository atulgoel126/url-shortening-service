package com.linksplit.controller;

import com.linksplit.dto.DashboardStats;
import com.linksplit.dto.LoginRequest;
import com.linksplit.dto.RegisterRequest;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.repository.LinkRepository;
import com.linksplit.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {
    private final UserService userService;
    private final LinkRepository linkRepository;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("isLoggedIn", 
            SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
            !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser"));
        return "index";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute RegisterRequest request,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(request.getEmail(), request.getPassword());
            model.addAttribute("message", "Registration successful! Please log in.");
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication,
                           @RequestParam(defaultValue = "0") int page) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userService.getUserByEmail(authentication.getName());
        
        Page<Link> userLinks = linkRepository.findByUserOrderByCreatedAtDesc(
            user, PageRequest.of(page, 10)
        );
        
        Long totalViews = linkRepository.getTotalViewsByUser(user);
        BigDecimal totalEarnings = linkRepository.getTotalEarningsByUser(user);
        
        DashboardStats stats = DashboardStats.builder()
            .totalLinks(userLinks.getTotalElements())
            .totalViews(totalViews != null ? totalViews : 0L)
            .totalEarnings(totalEarnings != null ? totalEarnings : BigDecimal.ZERO)
            .build();
        
        model.addAttribute("stats", stats);
        model.addAttribute("links", userLinks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userLinks.getTotalPages());
        
        return "dashboard";
    }
}