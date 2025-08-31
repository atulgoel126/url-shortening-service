package com.linksplit.service;

import com.linksplit.config.AppConfig;
import com.linksplit.entity.Link;
import com.linksplit.entity.LinkView;
import com.linksplit.repository.LinkRepository;
import com.linksplit.repository.LinkViewRepository;
import com.linksplit.service.ViewLimitService;
import com.linksplit.service.GeoLocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {
    
    @Mock
    private LinkRepository linkRepository;
    
    @Mock
    private LinkViewRepository linkViewRepository;
    
    @Mock
    private ViewLimitService viewLimitService;
    
    @Mock
    private GeoLocationService geoLocationService;
    
    @Mock
    private AppConfig appConfig;
    
    @Mock
    private AppConfig.ViewFraudPrevention viewFraudPrevention;
    
    @Mock
    private HttpServletRequest request;
    
    @InjectMocks
    private AnalyticsService analyticsService;
    
    private Link testLink;
    
    @BeforeEach
    void setUp() {
        testLink = Link.builder()
                .id(1L)
                .shortCode("test123")
                .longUrl("https://example.com")
                .viewCount(0L)
                .build();
    }
    
    @Test
    @DisplayName("Should record view successfully")
    void testRecordView() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("Referer")).thenReturn(null);
        when(viewLimitService.checkAndRecordView("192.168.1.1")).thenReturn(ViewLimitService.LimitType.ALLOWED);
        when(geoLocationService.getLocation("192.168.1.1")).thenReturn(
            GeoLocationService.GeoLocation.builder()
                .country("USA")
                .city("New York")
                .region("NY")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build()
        );
        when(linkViewRepository.save(any(LinkView.class))).thenReturn(new LinkView());
        
        boolean result = analyticsService.recordView(testLink, request);
        
        assertTrue(result);
        verify(linkViewRepository).save(any(LinkView.class));
        verify(linkRepository).incrementViewCount(testLink.getId());
        verify(linkRepository).updateEarnings(eq(testLink.getId()), any());
    }
    
    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header")
    void testExtractIpFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getHeader("Referer")).thenReturn(null);
        when(viewLimitService.checkAndRecordView("203.0.113.1")).thenReturn(ViewLimitService.LimitType.ALLOWED);
        when(geoLocationService.getLocation("203.0.113.1")).thenReturn(GeoLocationService.GeoLocation.unknown());
        when(linkViewRepository.save(any(LinkView.class))).thenAnswer(invocation -> {
            LinkView view = invocation.getArgument(0);
            assertEquals("203.0.113.1", view.getIpAddress());
            return view;
        });
        
        analyticsService.recordView(testLink, request);
        
        verify(linkViewRepository).save(any(LinkView.class));
        verify(linkRepository).updateEarnings(eq(testLink.getId()), any());
    }
    
    @Test
    @DisplayName("Should prevent duplicate views when rate limit exceeded")
    void testPreventDuplicateViews() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(viewLimitService.checkAndRecordView("192.168.1.1")).thenReturn(ViewLimitService.LimitType.HOURLY);
        
        boolean result = analyticsService.recordView(testLink, request);
        
        assertFalse(result);
        verify(linkViewRepository, never()).save(any(LinkView.class));
        verify(linkRepository, never()).incrementViewCount(anyLong());
    }
    
    @Test
    @DisplayName("Should allow view when no rate limit exceeded")
    void testAllowViewWhenNoRateLimitExceeded() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getHeader("Referer")).thenReturn(null);
        when(viewLimitService.checkAndRecordView("192.168.1.1")).thenReturn(ViewLimitService.LimitType.ALLOWED);
        when(geoLocationService.getLocation("192.168.1.1")).thenReturn(GeoLocationService.GeoLocation.unknown());
        when(linkViewRepository.save(any(LinkView.class))).thenReturn(new LinkView());
        
        boolean result = analyticsService.recordView(testLink, request);
        
        assertTrue(result);
        verify(linkViewRepository).save(any(LinkView.class));
        verify(linkRepository).incrementViewCount(testLink.getId());
        verify(linkRepository).updateEarnings(eq(testLink.getId()), any());
    }
    
    @Test
    @DisplayName("Should calculate earnings correctly")
    void testCalculateEarnings() {
        when(appConfig.getCpmRate()).thenReturn(1.50);
        when(appConfig.getRevenueShare()).thenReturn(0.70);
        BigDecimal earnings1000Views = analyticsService.calculateEarnings(1000L);
        assertEquals(new BigDecimal("1.0500"), earnings1000Views);
        
        BigDecimal earnings5000Views = analyticsService.calculateEarnings(5000L);
        assertEquals(new BigDecimal("5.2500"), earnings5000Views);
        
        BigDecimal earnings0Views = analyticsService.calculateEarnings(0L);
        assertEquals(BigDecimal.ZERO, earnings0Views);
        
        BigDecimal earningsNull = analyticsService.calculateEarnings(null);
        assertEquals(BigDecimal.ZERO, earningsNull);
    }
    
    @Test
    @DisplayName("Should handle decimal precision in earnings calculation")
    void testEarningsDecimalPrecision() {
        when(appConfig.getCpmRate()).thenReturn(1.50);
        when(appConfig.getRevenueShare()).thenReturn(0.70);
        BigDecimal earnings = analyticsService.calculateEarnings(1234L);
        assertEquals(4, earnings.scale());
        assertTrue(earnings.compareTo(BigDecimal.ZERO) > 0);
    }
    
    @Test
    @DisplayName("Should handle exception when recording view")
    void testRecordViewException() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getHeader("Referer")).thenReturn(null);
        when(viewLimitService.checkAndRecordView("192.168.1.1")).thenReturn(ViewLimitService.LimitType.ALLOWED);
        when(geoLocationService.getLocation("192.168.1.1")).thenReturn(GeoLocationService.GeoLocation.unknown());
        when(linkViewRepository.save(any(LinkView.class))).thenThrow(new RuntimeException("DB Error"));
        
        boolean result = analyticsService.recordView(testLink, request);
        
        assertFalse(result);
        verify(linkRepository, never()).incrementViewCount(anyLong());
    }
}