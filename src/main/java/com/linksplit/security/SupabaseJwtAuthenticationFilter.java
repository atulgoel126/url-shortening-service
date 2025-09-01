package com.linksplit.security;

import com.linksplit.entity.User;
import com.linksplit.service.SupabaseAuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupabaseJwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final SupabaseAuthService supabaseAuthService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String token = extractToken(request);
        
        if (StringUtils.hasText(token)) {
            Optional<Claims> claimsOpt = supabaseAuthService.validateToken(token);
            
            if (claimsOpt.isPresent()) {
                Claims claims = claimsOpt.get();
                String supabaseId = claims.getSubject();
                String email = claims.get("email", String.class);
                
                if (supabaseId != null && email != null) {
                    // Create or update user from Supabase
                    User user = supabaseAuthService.createOrUpdateUser(supabaseId, email, claims);
                    
                    // Create authentication token
                    UserDetails userDetails = supabaseAuthService.createUserDetails(user);
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Store user in request attribute for easy access
                    request.setAttribute("currentUser", user);
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from request
     * Checks Authorization header first, then cookies
     */
    private String extractToken(HttpServletRequest request) {
        // Check Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Check cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("sb-access-token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}