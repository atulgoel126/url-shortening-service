package com.linksplit.controller;

import com.linksplit.config.AppConfig;
import com.linksplit.entity.Link;
import com.linksplit.service.AnalyticsService;
import com.linksplit.service.UrlShorteningService;
import com.linksplit.service.ViewLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RedirectController {
    private final UrlShorteningService urlShorteningService;
    private final AnalyticsService analyticsService;
    private final ViewLimitService viewLimitService;
    private final AppConfig appConfig;

    @GetMapping("/{shortCode}")
    public String handleRedirect(@PathVariable String shortCode) {
        Optional<Link> linkOpt = urlShorteningService.getLinkByShortCode(shortCode);
        
        if (linkOpt.isEmpty()) {
            log.warn("Short code not found: {}", shortCode);
            return "error/404";
        }
        
        return "redirect:/ad-page?id=" + shortCode;
    }

    @GetMapping("/ad-page")
    public String showAdPage(@RequestParam String id, Model model, HttpServletRequest request, HttpSession session) {
        Optional<Link> linkOpt = urlShorteningService.getLinkByShortCode(id);
        
        if (linkOpt.isEmpty()) {
            log.warn("Short code not found for ad page: {}", id);
            return "error/404";
        }
        
        Link link = linkOpt.get();
        String ipAddress = analyticsService.extractIpAddress(request);
        
        // Check rate limits but don't record view yet
        ViewLimitService.LimitType limitCheck = viewLimitService.checkRateLimit(ipAddress);
        
        // Generate a unique session token for this ad view
        String sessionToken = java.util.UUID.randomUUID().toString();
        session.setAttribute("ad_session_" + id, sessionToken);
        
        model.addAttribute("destinationUrl", link.getLongUrl());
        model.addAttribute("countdownSeconds", appConfig.getAdDisplaySeconds());
        model.addAttribute("shortCode", id);
        model.addAttribute("sessionToken", sessionToken);
        
        // Add rate limit message if view would be blocked
        if (limitCheck != ViewLimitService.LimitType.ALLOWED) {
            model.addAttribute("rateLimitMessage", limitCheck.getMessage());
            model.addAttribute("viewBlocked", true);
        }
        
        return "ad-page";
    }
    
    @PostMapping("/api/complete-ad")
    @ResponseBody
    public ResponseEntity<?> completeAdView(@RequestParam String shortCode, 
                                           @RequestParam String token,
                                           HttpServletRequest request,
                                           HttpSession session) {
        // Verify session token to prevent fraud
        String sessionToken = (String) session.getAttribute("ad_session_" + shortCode);
        if (sessionToken == null || !sessionToken.equals(token)) {
            log.warn("Invalid session token for ad completion: {}", shortCode);
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid session"));
        }
        
        // Remove token to prevent reuse
        session.removeAttribute("ad_session_" + shortCode);
        
        Optional<Link> linkOpt = urlShorteningService.getLinkByShortCode(shortCode);
        if (linkOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Link link = linkOpt.get();
        boolean viewRecorded = analyticsService.recordView(link, request);
        
        if (viewRecorded) {
            log.info("Ad view completed and recorded for link: {}", shortCode);
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "View recorded"));
        } else {
            String rateLimitMessage = (String) request.getAttribute("rateLimitMessage");
            if (rateLimitMessage == null) {
                rateLimitMessage = "View not recorded";
            }
            return ResponseEntity.ok(java.util.Map.of("success", false, "message", rateLimitMessage));
        }
    }
}