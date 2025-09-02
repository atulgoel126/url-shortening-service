package com.linksplit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "supabase")
@Data
public class SupabaseConfig {
    private String url = "https://vcwireorjflemkupqacv.supabase.co";
    private String publishableKey;
    private String secretKey;
    private Jwks jwks;
    
    @Data
    public static class Jwks {
        private String discoveryUrl;
        private String keyId;
        private PublicKey publicKey;
        
        @Data
        public static class PublicKey {
            private String x;
            private String y;
            private String alg;
            private String crv;
            private boolean ext;
            private String kid;
            private String kty;
        }
    }
}