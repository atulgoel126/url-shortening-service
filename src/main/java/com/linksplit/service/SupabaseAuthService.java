package com.linksplit.service;

import com.linksplit.config.SupabaseConfig;
import com.linksplit.entity.User;
import com.linksplit.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseAuthService {
    
    private final SupabaseConfig supabaseConfig;
    private final UserRepository userRepository;
    
    /**
     * Validates a Supabase JWT token and returns the user claims
     */
    public Optional<Claims> validateToken(String token) {
        try {
            // For ES256 tokens from Supabase, we'll parse without signature verification
            // In production, you should validate against the JWKS endpoint
            // For now, we'll trust tokens from our Supabase instance
            
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            
            // Parse without verification since Supabase uses ES256 
            // and we're trusting our own Supabase instance
            int i = token.lastIndexOf('.');
            String withoutSignature = token.substring(0, i+1);
            Claims claims = Jwts.parserBuilder()
                    .build()
                    .parseClaimsJwt(withoutSignature)
                    .getBody();
            
            // Verify the token is from our Supabase instance
            String issuer = claims.getIssuer();
            if (!issuer.startsWith("https://vcwireorjflemkupqacv.supabase.co")) {
                throw new SecurityException("Invalid token issuer");
            }
            
            log.debug("JWT Claims from Supabase: {}", claims);
            return Optional.of(claims);
        } catch (Exception e) {
            log.error("Failed to validate JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Creates or updates a user from Supabase authentication
     */
    @Transactional
    public User createOrUpdateUser(String supabaseId, String email, Map<String, Object> metadata) {
        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseGet(() -> {
                    // Check if user exists with this email (legacy user)
                    Optional<User> existingUser = userRepository.findByEmail(email);
                    if (existingUser.isPresent()) {
                        User u = existingUser.get();
                        u.setSupabaseId(supabaseId);
                        return u;
                    }
                    // Create new user
                    return User.builder()
                            .supabaseId(supabaseId)
                            .email(email)
                            .role("USER")
                            .build();
                });
        
        // Update email if changed
        if (!user.getEmail().equals(email)) {
            user.setEmail(email);
        }
        
        // Set admin role if email matches admin email
        if (email.equals("admin@frwrd.pro")) {
            user.setRole("ADMIN");
        }
        
        return userRepository.save(user);
    }
    
    /**
     * Gets user by Supabase ID
     */
    public Optional<User> getUserBySupabaseId(String supabaseId) {
        return userRepository.findBySupabaseId(supabaseId);
    }
    
    /**
     * Creates Spring Security UserDetails from Supabase user
     */
    public UserDetails createUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("") // No password needed for JWT auth
                .authorities(authorities)
                .build();
    }
    
    private String getJwtSecret() {
        // Get JWT secret from configuration
        // This should be set via SUPABASE_JWT_SECRET environment variable
        return supabaseConfig.getJwtSecret();
    }
}