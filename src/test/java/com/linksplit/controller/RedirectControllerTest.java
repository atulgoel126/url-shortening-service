package com.linksplit.controller;

import com.linksplit.config.AppConfig;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.service.AnalyticsService;
import com.linksplit.service.UrlShorteningService;
import com.linksplit.service.ViewLimitService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RedirectControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UrlShorteningService urlShorteningService;
    
    @MockBean
    private AnalyticsService analyticsService;
    
    @MockBean
    private ViewLimitService viewLimitService;
    
    @MockBean
    private AppConfig appConfig;
    
    private Link testLink;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();
                
        testLink = Link.builder()
                .id(1L)
                .shortCode("test123")
                .longUrl("https://example.com")
                .user(testUser)
                .viewCount(0L)
                .duplicateViewCount(0L)
                .build();
                
        when(appConfig.getAdDisplaySeconds()).thenReturn(5);
    }
    
    @Test
    @DisplayName("Should show ad page without recording view immediately")
    void testShowAdPage() throws Exception {
        when(urlShorteningService.getLinkByShortCode("test123")).thenReturn(Optional.of(testLink));
        when(analyticsService.extractIpAddress(any())).thenReturn("192.168.1.1");
        when(viewLimitService.checkRateLimit("192.168.1.1")).thenReturn(ViewLimitService.LimitType.ALLOWED);
        
        mockMvc.perform(get("/ad-page")
                .param("id", "test123"))
                .andExpect(status().isOk())
                .andExpect(view().name("ad-page"))
                .andExpect(model().attribute("destinationUrl", "https://example.com"))
                .andExpect(model().attribute("countdownSeconds", 5))
                .andExpect(model().attribute("shortCode", "test123"))
                .andExpect(model().attributeExists("sessionToken"));
        
        // Verify view was NOT recorded yet
        verify(analyticsService, never()).recordView(any(), any());
    }
    
    @Test
    @DisplayName("Should record view only after ad completion")
    void testCompleteAdView() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String sessionToken = UUID.randomUUID().toString();
        session.setAttribute("ad_session_test123", sessionToken);
        
        when(urlShorteningService.getLinkByShortCode("test123")).thenReturn(Optional.of(testLink));
        when(analyticsService.recordView(eq(testLink), any())).thenReturn(true);
        
        mockMvc.perform(post("/api/complete-ad")
                .session(session)
                .param("shortCode", "test123")
                .param("token", sessionToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("View recorded"));
        
        // Verify view was recorded
        verify(analyticsService).recordView(eq(testLink), any());
    }
    
    @Test
    @DisplayName("Should reject completion with invalid session token")
    void testCompleteAdViewInvalidToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("ad_session_test123", "valid-token");
        
        when(urlShorteningService.getLinkByShortCode("test123")).thenReturn(Optional.of(testLink));
        
        mockMvc.perform(post("/api/complete-ad")
                .session(session)
                .param("shortCode", "test123")
                .param("token", "invalid-token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid session"));
        
        // Verify view was NOT recorded
        verify(analyticsService, never()).recordView(any(), any());
    }
    
    @Test
    @DisplayName("Should show rate limit warning on ad page")
    void testShowAdPageWithRateLimit() throws Exception {
        when(urlShorteningService.getLinkByShortCode("test123")).thenReturn(Optional.of(testLink));
        when(analyticsService.extractIpAddress(any())).thenReturn("192.168.1.1");
        when(viewLimitService.checkRateLimit("192.168.1.1"))
                .thenReturn(ViewLimitService.LimitType.HOURLY);
        
        mockMvc.perform(get("/ad-page")
                .param("id", "test123"))
                .andExpect(status().isOk())
                .andExpect(view().name("ad-page"))
                .andExpect(model().attribute("viewBlocked", true))
                .andExpect(model().attribute("rateLimitMessage", 
                        "Hourly limit exceeded: max 20 ads per hour"));
    }
    
    @Test
    @DisplayName("Should prevent reuse of session token")
    void testPreventTokenReuse() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String sessionToken = UUID.randomUUID().toString();
        session.setAttribute("ad_session_test123", sessionToken);
        
        when(urlShorteningService.getLinkByShortCode("test123")).thenReturn(Optional.of(testLink));
        when(analyticsService.recordView(eq(testLink), any())).thenReturn(true);
        
        // First completion should succeed
        mockMvc.perform(post("/api/complete-ad")
                .session(session)
                .param("shortCode", "test123")
                .param("token", sessionToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        
        // Second attempt with same token should fail
        mockMvc.perform(post("/api/complete-ad")
                .session(session)
                .param("shortCode", "test123")
                .param("token", sessionToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid session"));
        
        // Verify view was recorded only once
        verify(analyticsService, times(1)).recordView(eq(testLink), any());
    }
}