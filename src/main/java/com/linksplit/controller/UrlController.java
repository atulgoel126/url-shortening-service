package com.linksplit.controller;

import com.linksplit.dto.CreateUrlRequest;
import com.linksplit.dto.CreateUrlResponse;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.service.UrlShorteningService;
import com.linksplit.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UrlController {
    private final UrlShorteningService urlShorteningService;
    private final UserService userService;

    @PostMapping("/url")
    public ResponseEntity<CreateUrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request,
            Authentication authentication) {
        
        User user = null;
        if (authentication != null && authentication.isAuthenticated()) {
            user = userService.getUserByEmail(authentication.getName());
        }
        
        try {
            Link link = urlShorteningService.createShortUrl(request.getLongUrl(), user);
            String fullShortUrl = urlShorteningService.getFullShortUrl(link.getShortCode());
            
            CreateUrlResponse response = CreateUrlResponse.builder()
                    .shortUrl(fullShortUrl)
                    .shortCode(link.getShortCode())
                    .longUrl(link.getLongUrl())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid URL provided: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                CreateUrlResponse.builder()
                    .error(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Error creating short URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                CreateUrlResponse.builder()
                    .error("Failed to create short URL")
                    .build()
            );
        }
    }

    @DeleteMapping("/url/{linkId}")
    public ResponseEntity<Void> deleteLink(@PathVariable Long linkId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            User user = userService.getUserByEmail(authentication.getName());
            boolean deleted = urlShorteningService.deleteLink(linkId, user);
            
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error deleting link: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}