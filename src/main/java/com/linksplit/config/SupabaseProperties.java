package com.linksplit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "supabase")
@Getter
@Setter
public class SupabaseProperties {
    private String url;
    private String publishableKey;
}