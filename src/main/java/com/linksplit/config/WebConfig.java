package com.linksplit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Handle static resources
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // Handle well-known URIs (for Chrome DevTools, Apple app association, etc.)
        registry.addResourceHandler("/.well-known/**")
                .addResourceLocations("classpath:/.well-known/")
                .resourceChain(false);
    }
}