package com.linksplit.service;

import com.linksplit.entity.User;
import com.linksplit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .build();
    }
    
    @Test
    @DisplayName("Should load user by username successfully")
    void testLoadUserByUsername() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        UserDetails result = userService.loadUserByUsername("test@example.com");
        
        assertNotNull(result);
        assertEquals("test@example.com", result.getUsername());
        assertEquals("$2a$10$hashedpassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
    }
    
    @Test
    @DisplayName("Should throw exception when user not found")
    void testLoadUserByUsernameNotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        assertThrows(UsernameNotFoundException.class, 
            () -> userService.loadUserByUsername("nonexistent@example.com"));
    }
    
    @Test
    @DisplayName("Should register new user successfully")
    void testRegisterUser() {
        String email = "newuser@example.com";
        String password = "securePassword123";
        String hashedPassword = "$2a$10$newhashedpassword";
        
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        
        User result = userService.registerUser(email, password);
        
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(hashedPassword, result.getPasswordHash());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should reject duplicate email registration")
    void testRegisterDuplicateEmail() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser("existing@example.com", "password123"));
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("Should validate email format")
    void testEmailValidation() {
        assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser("", "password123"));
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser("invalid-email", "password123"));
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser("@example.com", "password123"));
        
        String longEmail = "a".repeat(250) + "@example.com";
        assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser(longEmail, "password123"));
    }
    
    @Test
    @DisplayName("Should validate password requirements")
    void testPasswordValidation() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser("test@example.com", "short"));
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser("test@example.com", null));
        
        String longPassword = "a".repeat(101);
        assertThrows(IllegalArgumentException.class, 
            () -> userService.registerUser("test@example.com", longPassword));
    }
    
    @Test
    @DisplayName("Should get user by email successfully")
    void testGetUserByEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        User result = userService.getUserByEmail("test@example.com");
        
        assertNotNull(result);
        assertEquals(testUser, result);
    }
    
    @Test
    @DisplayName("Should throw exception when getting non-existent user")
    void testGetUserByEmailNotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        assertThrows(UsernameNotFoundException.class, 
            () -> userService.getUserByEmail("nonexistent@example.com"));
    }
    
    @Test
    @DisplayName("Should accept valid email formats")
    void testValidEmailFormats() {
        String[] validEmails = {
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.co.uk",
            "user123@test-domain.org"
        };
        
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        for (String email : validEmails) {
            assertDoesNotThrow(() -> userService.registerUser(email, "password123"));
        }
    }
}