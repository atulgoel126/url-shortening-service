package com.linksplit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linksplit.dto.CreateUrlRequest;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.service.UrlShorteningService;
import com.linksplit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
@AutoConfigureMockMvc(addFilters = false)
class UrlControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private UrlShorteningService urlShorteningService;
    
    @MockBean
    private UserService userService;
    
    private User testUser;
    private Link testLink;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();
        
        testLink = Link.builder()
                .id(1L)
                .shortCode("abc123")
                .longUrl("https://www.example.com")
                .user(testUser)
                .build();
    }
    
    @Test
    @DisplayName("Should create short URL for anonymous user")
    void testCreateShortUrlAnonymous() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .longUrl("https://www.example.com/page")
                .build();
        
        when(urlShorteningService.createShortUrl(anyString(), isNull()))
                .thenReturn(testLink);
        when(urlShorteningService.getFullShortUrl("abc123"))
                .thenReturn("http://localhost:8080/abc123");
        
        mockMvc.perform(post("/api/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc123"))
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.longUrl").value("https://www.example.com"));
    }
    
    @Test
    @DisplayName("Should create short URL for authenticated user")
    @WithMockUser(username = "test@example.com")
    void testCreateShortUrlAuthenticated() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .longUrl("https://www.example.com/page")
                .build();
        
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(urlShorteningService.createShortUrl(anyString(), any()))
                .thenReturn(testLink);
        when(urlShorteningService.getFullShortUrl("abc123"))
                .thenReturn("http://localhost:8080/abc123");
        
        mockMvc.perform(post("/api/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc123"));
    }
    
    @Test
    @DisplayName("Should return bad request for invalid URL")
    void testCreateShortUrlInvalidUrl() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .longUrl("not-a-url")
                .build();
        
        mockMvc.perform(post("/api/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should return bad request for empty URL")
    void testCreateShortUrlEmptyUrl() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .longUrl("")
                .build();
        
        mockMvc.perform(post("/api/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle service exception")
    void testCreateShortUrlServiceException() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .longUrl("https://www.example.com")
                .build();
        
        when(urlShorteningService.createShortUrl(anyString(), isNull()))
                .thenThrow(new IllegalArgumentException("Invalid URL"));
        
        mockMvc.perform(post("/api/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid URL"));
    }
    
    @Test
    @DisplayName("Should handle internal server error")
    void testCreateShortUrlInternalError() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .longUrl("https://www.example.com")
                .build();
        
        when(urlShorteningService.createShortUrl(anyString(), isNull()))
                .thenThrow(new RuntimeException("Database error"));
        
        mockMvc.perform(post("/api/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to create short URL"));
    }
}