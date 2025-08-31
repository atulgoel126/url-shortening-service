package com.linksplit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linksplit.dto.CreateUrlRequest;
import com.linksplit.entity.User;
import com.linksplit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UrlShorteningIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private LinkRepository linkRepository;
    
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    
    @Autowired
    private PayoutRepository payoutRepository;
    
    @Autowired
    private LinkViewRepository linkViewRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Clean up in proper order to avoid foreign key constraints
        linkViewRepository.deleteAll();
        linkRepository.deleteAll();
        payoutRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        userRepository.deleteAll();
        
        testUser = User.builder()
                .email("integration@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        testUser = userRepository.save(testUser);
    }
    
    @Test
    @DisplayName("Should create and redirect short URL")
    void testCreateAndRedirect() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .longUrl("https://www.google.com")
                .build();
        
        String response = mockMvc.perform(post("/api/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").exists())
                .andExpect(jsonPath("$.shortCode").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String shortCode = objectMapper.readTree(response).get("shortCode").asText();
        
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ad-page?id=" + shortCode));
        
        mockMvc.perform(get("/ad-page")
                .param("id", shortCode))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("https://www.google.com")));
        
        assertEquals(1, linkRepository.count());
        assertTrue(linkRepository.findByShortCode(shortCode).isPresent());
    }
    
    @Test
    @DisplayName("Should handle non-existent short code")
    void testNonExistentShortCode() throws Exception {
        mockMvc.perform(get("/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/404"));
        
        mockMvc.perform(get("/ad-page")
                .param("id", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/404"));
    }
    
    @Test
    @DisplayName("Should register and login user")
    void testUserRegistrationAndLogin() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("email", "newuser@test.com")
                .param("password", "securepass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("message", "Registration successful! Please log in."));
        
        assertTrue(userRepository.existsByEmail("newuser@test.com"));
        
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "newuser@test.com")
                .param("password", "securepass123"))
                .andExpect(status().is3xxRedirection());
    }
    
    @Test
    @DisplayName("Should prevent duplicate email registration")
    void testDuplicateEmailRegistration() throws Exception {
        // First register a user
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("email", "duplicate@test.com")
                .param("password", "firstpass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
        
        // Try to register with the same email again
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("email", "duplicate@test.com")
                .param("password", "anotherpass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }
    
    @Test
    @DisplayName("Should validate URL format in API")
    void testUrlValidation() throws Exception {
        CreateUrlRequest invalidRequest = CreateUrlRequest.builder()
                .longUrl("invalid-url")
                .build();
        
        mockMvc.perform(post("/api/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest());
        
        CreateUrlRequest emptyRequest = CreateUrlRequest.builder()
                .longUrl("")
                .build();
        
        mockMvc.perform(post("/api/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should create multiple short URLs")
    void testCreateMultipleUrls() throws Exception {
        String[] urls = {
            "https://www.example1.com",
            "https://www.example2.com",
            "https://www.example3.com"
        };
        
        for (String url : urls) {
            CreateUrlRequest request = CreateUrlRequest.builder()
                    .longUrl(url)
                    .build();
            
            mockMvc.perform(post("/api/url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shortCode").exists());
        }
        
        assertEquals(3, linkRepository.count());
    }
}