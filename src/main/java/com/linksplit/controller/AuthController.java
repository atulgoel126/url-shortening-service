package com.linksplit.controller;

import com.linksplit.config.AppProperties;
import com.linksplit.config.SupabaseProperties;
import com.linksplit.entity.User;
import com.linksplit.service.SupabaseAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final SupabaseAuthService supabaseAuthService;
    private final AppProperties appProperties;
    private final SupabaseProperties supabaseProperties;
    
    @GetMapping("/login")
    public String showLoginPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "auth/login";
    }
    
    @GetMapping("/register")
    public String showRegisterPage(Authentication authentication, @RequestParam(required = false) String referrerId, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("referrerId", referrerId);
        model.addAttribute("app", appProperties);
        return "auth/register";
    }
    
    @GetMapping("/callback")
    public String handleCallback(
            @RequestParam(required = false) String referrerId,
            HttpServletRequest request,
            Model model) {
        
        if (referrerId != null && !referrerId.isEmpty()) {
            log.info("Referrer ID received in callback: {}", referrerId);
            request.getSession().setAttribute("referrerId", referrerId);
        } else {
            log.info("No Referrer ID in callback.");
        }
        
        model.addAttribute("supabase", supabaseProperties);
        return "auth/callback";
    }
    
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        // Clear authentication
        SecurityContextHolder.clearContext();
        
        // Clear cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().startsWith("sb-")) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
        
        // Return logout page which will handle Supabase signout
        return "auth/logout";
    }
    
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam(required = false) String token, Model model) {
        if (token != null) {
            model.addAttribute("message", "Your email has been verified! You can now log in.");
            model.addAttribute("success", true);
        } else {
            model.addAttribute("message", "Invalid verification link.");
            model.addAttribute("success", false);
        }
        
        return "auth/verify";
    }
}