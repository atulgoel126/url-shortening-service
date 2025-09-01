package com.linksplit.controller;

import com.linksplit.dto.analytics.AnalyticsDashboard;
import com.linksplit.entity.ClickHeatmap;
import com.linksplit.entity.Link;
import com.linksplit.entity.LinkView;
import com.linksplit.entity.User;
import com.linksplit.repository.ClickHeatmapRepository;
import com.linksplit.repository.LinkRepository;
import com.linksplit.repository.LinkViewRepository;
import com.linksplit.service.CreatorAnalyticsService;
import com.linksplit.service.UserService;
import com.linksplit.service.UrlShorteningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {
    private final CreatorAnalyticsService analyticsService;
    private final ClickHeatmapRepository heatmapRepository;
    private final UrlShorteningService urlShorteningService;
    private final UserService userService;
    private final LinkRepository linkRepository;
    private final LinkViewRepository linkViewRepository;
    
    @GetMapping("/analytics")
    public String showAnalytics(Model model, Authentication authentication,
                                @RequestParam(required = false) String linkId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        User user = userService.getUserByEmail(authentication.getName());
        
        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        
        // Check if we're viewing analytics for a specific link
        Link selectedLink = null;
        if (linkId != null && !linkId.isEmpty()) {
            Optional<Link> linkOpt = urlShorteningService.getLinkByShortCode(linkId);
            if (linkOpt.isPresent() && linkOpt.get().getUser().getId().equals(user.getId())) {
                selectedLink = linkOpt.get();
            }
        }
        
        AnalyticsDashboard dashboard;
        if (selectedLink != null) {
            // Get analytics for specific link
            dashboard = analyticsService.getAnalyticsForLink(selectedLink, startDateTime, endDateTime);
            model.addAttribute("selectedLink", selectedLink);
            model.addAttribute("isLinkSpecific", true);
        } else {
            // Get analytics for all user links
            dashboard = analyticsService.getAnalyticsDashboard(user, startDateTime, endDateTime);
            model.addAttribute("isLinkSpecific", false);
        }
        
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("startDate", startDateTime);
        model.addAttribute("endDate", endDateTime);
        model.addAttribute("userLinks", linkRepository.findByUser(user));
        
        return "analytics";
    }
    
    @PostMapping("/api/analytics/heatmap")
    @ResponseBody
    public ResponseEntity<?> recordHeatmapClick(@RequestBody Map<String, Object> clickData) {
        try {
            String shortCode = (String) clickData.get("shortCode");
            String sessionId = (String) clickData.get("sessionId");
            String pageUrl = (String) clickData.get("pageUrl");
            
            Optional<Link> linkOpt = urlShorteningService.getLinkByShortCode(shortCode);
            if (linkOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ClickHeatmap heatmap = ClickHeatmap.builder()
                    .link(linkOpt.get())
                    .pageUrl(pageUrl)
                    .xCoordinate((Integer) clickData.get("x"))
                    .yCoordinate((Integer) clickData.get("y"))
                    .viewportWidth((Integer) clickData.get("viewportWidth"))
                    .viewportHeight((Integer) clickData.get("viewportHeight"))
                    .elementType((String) clickData.get("elementType"))
                    .elementText((String) clickData.get("elementText"))
                    .elementId((String) clickData.get("elementId"))
                    .elementClass((String) clickData.get("elementClass"))
                    .sessionId(sessionId)
                    .clickedAt(LocalDateTime.now())
                    .build();
            
            heatmapRepository.save(heatmap);
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error recording heatmap click: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/api/analytics/heatmap/{shortCode}")
    @ResponseBody
    public ResponseEntity<?> getHeatmapData(@PathVariable String shortCode,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                           Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        User user = userService.getUserByEmail(authentication.getName());
        Optional<Link> linkOpt = urlShorteningService.getLinkByShortCode(shortCode);
        
        if (linkOpt.isEmpty() || !linkOpt.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        List<ClickHeatmap> heatmapData = heatmapRepository.findByLinkAndClickedAtBetween(
                linkOpt.get(), startDate, endDate);
        
        return ResponseEntity.ok(heatmapData);
    }
    
    @GetMapping("/api/analytics/dashboard")
    @ResponseBody
    public ResponseEntity<?> getAnalyticsDashboard(Authentication authentication,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        User user = userService.getUserByEmail(authentication.getName());
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        AnalyticsDashboard dashboard = analyticsService.getAnalyticsDashboard(user, startDate, endDate);
        
        return ResponseEntity.ok(dashboard);
    }
    
    @GetMapping("/analytics/map")
    public String showMapView(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        return "analytics-map";
    }
    
    @GetMapping("/api/analytics/map-data")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMapData(Authentication authentication,
                                       @RequestParam(defaultValue = "30") int days) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        User user = userService.getUserByEmail(authentication.getName());
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();
        
        List<Link> userLinks = linkRepository.findByUser(user);
        List<LinkView> views = linkViewRepository.findByLinkInAndViewedAtBetween(userLinks, startDate, endDate);
        
        // Aggregate location data
        Map<String, LocationData> locationMap = new HashMap<>();
        
        for (LinkView view : views) {
            String key = view.getCity() + "|" + view.getCountry();
            
            locationMap.computeIfAbsent(key, k -> {
                LocationData loc = new LocationData();
                loc.city = view.getCity();
                loc.country = view.getCountry() != null ? view.getCountry() : "Unknown";
                loc.latitude = view.getLatitude();
                loc.longitude = view.getLongitude();
                loc.views = 0;
                loc.uniqueUsers = new HashSet<>();
                return loc;
            });
            
            LocationData loc = locationMap.get(key);
            loc.views++;
            loc.uniqueUsers.add(view.getIpAddress());
        }
        
        // Convert to list and prepare response
        List<Map<String, Object>> locations = new ArrayList<>();
        for (LocationData loc : locationMap.values()) {
            if (loc.latitude != null && loc.longitude != null) {
                Map<String, Object> locData = new HashMap<>();
                locData.put("city", loc.city);
                locData.put("country", loc.country);
                locData.put("latitude", loc.latitude);
                locData.put("longitude", loc.longitude);
                locData.put("views", loc.views);
                locData.put("uniqueUsers", loc.uniqueUsers.size());
                locations.add(locData);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "locations", locations,
            "totalViews", views.size(),
            "totalCountries", locationMap.values().stream()
                .map(l -> l.country).distinct().count()
        ));
    }
    
    private static class LocationData {
        String city;
        String country;
        Double latitude;
        Double longitude;
        int views;
        Set<String> uniqueUsers;
    }
}