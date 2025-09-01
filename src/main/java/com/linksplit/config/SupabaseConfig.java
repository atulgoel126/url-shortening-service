package com.linksplit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "supabase")
@Data
public class SupabaseConfig {
    private String url = "https://vcwireorjflemkupqacv.supabase.co";
    private String anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZjd2lyZW9yamZsZW1rdXBxYWN2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY3MDgxMjUsImV4cCI6MjA3MjI4NDEyNX0.KX1Wm_YfOf8LJkYa8e8rQmt4_mJyep4aDyswzYmpYEE";
    private String jwtSecret;
}