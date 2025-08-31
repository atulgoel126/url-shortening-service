package com.linksplit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoLocationService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Using ip-api.com (free, no API key required)
    private static final String IP_API_URL = "http://ip-api.com/json/";
    
    @Cacheable(value = "geoLocation", key = "#ipAddress")
    public GeoLocation getLocation(String ipAddress) {
        try {
            // Skip localhost/private IPs
            if (isPrivateIP(ipAddress)) {
                return GeoLocation.unknown();
            }
            
            String url = IP_API_URL + ipAddress;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(response);
            
            if ("success".equals(json.get("status").asText())) {
                return GeoLocation.builder()
                        .country(json.get("country").asText())
                        .city(json.get("city").asText())
                        .region(json.get("regionName").asText())
                        .latitude(json.get("lat").asDouble())
                        .longitude(json.get("lon").asDouble())
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to get location for IP {}: {}", ipAddress, e.getMessage());
        }
        
        return GeoLocation.unknown();
    }
    
    private boolean isPrivateIP(String ip) {
        return ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               ip.startsWith("172.") ||
               ip.equals("127.0.0.1") ||
               ip.equals("0:0:0:0:0:0:0:1") ||
               ip.equals("::1");
    }
    
    @lombok.Builder
    @lombok.Data
    public static class GeoLocation {
        private String country;
        private String city;
        private String region;
        private Double latitude;
        private Double longitude;
        
        public static GeoLocation unknown() {
            return GeoLocation.builder()
                    .country("Unknown")
                    .city("Unknown")
                    .region("Unknown")
                    .build();
        }
    }
}