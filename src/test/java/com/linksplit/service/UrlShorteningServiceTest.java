package com.linksplit.service;

import com.linksplit.config.AppConfig;
import com.linksplit.entity.Link;
import com.linksplit.entity.User;
import com.linksplit.repository.LinkRepository;
import com.linksplit.util.Base62Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShorteningServiceTest {
    
    @Mock
    private LinkRepository linkRepository;
    
    @Mock
    private Base62Encoder base62Encoder;
    
    @Mock
    private AppConfig appConfig;
    
    @InjectMocks
    private UrlShorteningService urlShorteningService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .build();
    }
    
    @Test
    @DisplayName("Should create short URL successfully")
    void testCreateShortUrl() {
        String longUrl = "https://www.example.com/very/long/url";
        when(appConfig.getShortcodeLength()).thenReturn(6);
        when(base62Encoder.encode(anyLong())).thenReturn("abc123");
        when(linkRepository.existsByShortCode(anyString())).thenReturn(false);
        when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> {
            Link link = invocation.getArgument(0);
            link.setId(1L);
            return link;
        });
        
        Link result = urlShorteningService.createShortUrl(longUrl, testUser);
        
        assertNotNull(result);
        assertEquals(longUrl, result.getLongUrl());
        assertNotNull(result.getShortCode());
        assertEquals(testUser, result.getUser());
        
        verify(linkRepository).save(any(Link.class));
    }
    
    @Test
    @DisplayName("Should create anonymous short URL")
    void testCreateAnonymousShortUrl() {
        String longUrl = "https://www.example.com/page";
        when(appConfig.getShortcodeLength()).thenReturn(6);
        when(base62Encoder.encode(anyLong())).thenReturn("xyz789");
        when(linkRepository.existsByShortCode(anyString())).thenReturn(false);
        when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Link result = urlShorteningService.createShortUrl(longUrl, null);
        
        assertNotNull(result);
        assertNull(result.getUser());
        assertEquals(longUrl, result.getLongUrl());
    }
    
    @Test
    @DisplayName("Should validate URL format")
    void testUrlValidation() {
        assertThrows(IllegalArgumentException.class, 
            () -> urlShorteningService.createShortUrl("", testUser));
        
        assertThrows(IllegalArgumentException.class, 
            () -> urlShorteningService.createShortUrl("not-a-url", testUser));
        
        assertThrows(IllegalArgumentException.class, 
            () -> urlShorteningService.createShortUrl("ftp://example.com", testUser));
        
        String tooLongUrl = "https://example.com/" + "a".repeat(2050);
        assertThrows(IllegalArgumentException.class, 
            () -> urlShorteningService.createShortUrl(tooLongUrl, testUser));
    }
    
    @Test
    @DisplayName("Should handle duplicate short codes")
    void testHandleDuplicateShortCodes() {
        String longUrl = "https://www.example.com";
        when(appConfig.getShortcodeLength()).thenReturn(6);
        when(base62Encoder.encode(anyLong()))
            .thenReturn("dup123")
            .thenReturn("unique");
        
        when(linkRepository.existsByShortCode("dup123")).thenReturn(true);
        when(linkRepository.existsByShortCode("unique")).thenReturn(false);
        when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Link result = urlShorteningService.createShortUrl(longUrl, testUser);
        
        assertNotNull(result);
        verify(linkRepository, times(2)).existsByShortCode(anyString());
    }
    
    @Test
    @DisplayName("Should get link by short code")
    void testGetLinkByShortCode() {
        String shortCode = "test123";
        Link expectedLink = Link.builder()
                .id(1L)
                .shortCode(shortCode)
                .longUrl("https://example.com")
                .build();
        
        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(expectedLink));
        
        Optional<Link> result = urlShorteningService.getLinkByShortCode(shortCode);
        
        assertTrue(result.isPresent());
        assertEquals(expectedLink, result.get());
    }
    
    @Test
    @DisplayName("Should return empty for non-existent short code")
    void testGetLinkByShortCodeNotFound() {
        when(linkRepository.findByShortCode("notfound")).thenReturn(Optional.empty());
        
        Optional<Link> result = urlShorteningService.getLinkByShortCode("notfound");
        
        assertFalse(result.isPresent());
    }
    
    @Test
    @DisplayName("Should generate full short URL")
    void testGetFullShortUrl() {
        String shortCode = "abc123";
        when(appConfig.getBaseUrl()).thenReturn("http://localhost:8080");
        String fullUrl = urlShorteningService.getFullShortUrl(shortCode);
        
        assertEquals("http://localhost:8080/abc123", fullUrl);
    }
    
    @Test
    @DisplayName("Should throw exception after max attempts")
    void testMaxAttemptsExceeded() {
        String longUrl = "https://www.example.com";
        when(appConfig.getShortcodeLength()).thenReturn(6);
        when(base62Encoder.encode(anyLong())).thenReturn("always");
        when(linkRepository.existsByShortCode(anyString())).thenReturn(true);
        
        assertThrows(RuntimeException.class, 
            () -> urlShorteningService.createShortUrl(longUrl, testUser));
    }
}