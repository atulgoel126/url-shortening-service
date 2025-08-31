package com.linksplit.config;

import com.linksplit.entity.*;
import com.linksplit.repository.*;
import com.linksplit.service.PaymentService;
import com.linksplit.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                  LinkRepository linkRepository,
                                  LinkViewRepository linkViewRepository,
                                  PaymentMethodRepository paymentMethodRepository,
                                  PayoutRepository payoutRepository,
                                  PasswordEncoder passwordEncoder,
                                  PaymentService paymentService) {
        
        return args -> {
            // Check if test user already exists
            if (userRepository.findByEmail("demo@linksplit.com").isPresent()) {
                log.info("Demo data already exists, skipping initialization");
                return;
            }
            
            log.info("Creating demo user and sample data...");
            
            // Create super admin user
            User adminUser = User.builder()
                    .email("admin@cli.p")
                    .passwordHash(passwordEncoder.encode("admin@2024"))
                    .role("ADMIN")
                    .build();
            adminUser = userRepository.save(adminUser);
            log.info("Created super admin user: admin@cli.p / admin@2024");
            
            // Create demo user
            User demoUser = User.builder()
                    .email("demo@linksplit.com")
                    .passwordHash(passwordEncoder.encode("demo123"))
                    .role("USER")
                    .build();
            demoUser = userRepository.save(demoUser);
            log.info("Created demo user: demo@linksplit.com / demo123");
            
            // Add payment methods
            PaymentMethod upi1 = PaymentMethod.builder()
                    .user(demoUser)
                    .paymentType(PaymentMethod.PaymentType.UPI)
                    .upiId("demo@paytm")
                    .accountHolderName("Demo User")
                    .isPrimary(true)
                    .isVerified(true)
                    .build();
            paymentMethodRepository.save(upi1);
            
            PaymentMethod upi2 = PaymentMethod.builder()
                    .user(demoUser)
                    .paymentType(PaymentMethod.PaymentType.UPI)
                    .upiId("demo.user@phonepe")
                    .accountHolderName("Demo User")
                    .isPrimary(false)
                    .isVerified(true)
                    .build();
            paymentMethodRepository.save(upi2);
            log.info("Added payment methods for demo user");
            
            // Create sample links with various stats
            String[] sampleUrls = {
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://github.com/spring-projects/spring-boot",
                "https://www.amazon.in/dp/B08N5WRWNW",
                "https://medium.com/@demo/my-first-article",
                "https://www.flipkart.com/mobiles/pr?sid=tyy,4io",
                "https://twitter.com/elonmusk/status/1234567890",
                "https://www.linkedin.com/in/demo-user"
            };
            
            String[] shortCodes = {"DEMO01", "DEMO02", "DEMO03", "DEMO04", "DEMO05", "DEMO06", "DEMO07"};
            Long[] viewCounts = {1250L, 850L, 2100L, 450L, 3200L, 1800L, 650L};
            Long[] duplicateCounts = {45L, 23L, 89L, 12L, 156L, 67L, 28L};
            
            Random random = new Random();
            
            for (int i = 0; i < sampleUrls.length; i++) {
                Link link = Link.builder()
                        .shortCode(shortCodes[i])
                        .longUrl(sampleUrls[i])
                        .user(demoUser)
                        .viewCount(viewCounts[i])
                        .duplicateViewCount(duplicateCounts[i])
                        .estimatedEarnings(BigDecimal.valueOf(viewCounts[i] * 0.0005)) // $0.50 per 1000 views
                        .createdAt(LocalDateTime.now().minusDays(30 - i * 4))
                        .build();
                linkRepository.save(link);
                
                // Add sample views for analytics
                for (int j = 0; j < Math.min(10, viewCounts[i]); j++) {
                    String[] countries = {"India", "United States", "United Kingdom", "Canada", "Australia"};
                    String[] cities = {"Mumbai", "New York", "London", "Toronto", "Sydney"};
                    String[] devices = {"Mobile", "Desktop", "Tablet"};
                    String[] browsers = {"Chrome", "Safari", "Firefox", "Edge"};
                    
                    int countryIndex = random.nextInt(countries.length);
                    
                    LinkView view = LinkView.builder()
                            .link(link)
                            .ipAddress("192.168.1." + random.nextInt(255))
                            .viewedAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                            .country(countries[countryIndex])
                            .city(cities[countryIndex])
                            .deviceType(devices[random.nextInt(devices.length)])
                            .browser(browsers[random.nextInt(browsers.length)])
                            .adCompleted(random.nextBoolean())
                            .timeToSkip(5 + random.nextInt(3))
                            .build();
                    linkViewRepository.save(view);
                }
            }
            log.info("Created {} sample links with analytics data", sampleUrls.length);
            
            // Create sample payout history
            Payout completedPayout = Payout.builder()
                    .user(demoUser)
                    .paymentMethod(upi1)
                    .amount(new BigDecimal("500.00"))
                    .currency("INR")
                    .status(Payout.PayoutStatus.COMPLETED)
                    .referenceNumber("PAYDEMO001")
                    .transactionId("TXN123456789")
                    .viewsIncluded(1000L)
                    .periodStart(LocalDateTime.now().minusDays(60))
                    .periodEnd(LocalDateTime.now().minusDays(30))
                    .requestedAt(LocalDateTime.now().minusDays(25))
                    .processedAt(LocalDateTime.now().minusDays(24))
                    .notes("First payout - processed successfully")
                    .build();
            payoutRepository.save(completedPayout);
            
            Payout pendingPayout = Payout.builder()
                    .user(demoUser)
                    .paymentMethod(upi1)
                    .amount(new BigDecimal("350.00"))
                    .currency("INR")
                    .status(Payout.PayoutStatus.PENDING)
                    .referenceNumber("PAYDEMO002")
                    .viewsIncluded(700L)
                    .periodStart(LocalDateTime.now().minusDays(30))
                    .periodEnd(LocalDateTime.now())
                    .requestedAt(LocalDateTime.now().minusDays(2))
                    .notes("Pending payout - will be processed within 24-48 hours")
                    .build();
            payoutRepository.save(pendingPayout);
            
            log.info("Created sample payout history");
            
            // Create another test user
            User testUser = User.builder()
                    .email("test@linksplit.com")
                    .passwordHash(passwordEncoder.encode("test123"))
                    .build();
            userRepository.save(testUser);
            
            // Add payment method for test user
            PaymentMethod testUpi = PaymentMethod.builder()
                    .user(testUser)
                    .paymentType(PaymentMethod.PaymentType.UPI)
                    .upiId("test@googlepay")
                    .accountHolderName("Test User")
                    .isPrimary(true)
                    .isVerified(true)
                    .build();
            paymentMethodRepository.save(testUpi);
            
            log.info("Created test user: test@linksplit.com / test123");
            
            log.info("==============================================");
            log.info("Demo data initialization complete!");
            log.info("Demo User: demo@linksplit.com / demo123");
            log.info("Test User: test@linksplit.com / test123");
            log.info("==============================================");
        };
    }
}