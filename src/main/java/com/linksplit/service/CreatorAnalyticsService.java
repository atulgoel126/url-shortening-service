package com.linksplit.service;

import com.linksplit.dto.analytics.*;
import com.linksplit.entity.Link;
import com.linksplit.entity.LinkView;
import com.linksplit.entity.User;
import com.linksplit.repository.ClickHeatmapRepository;
import com.linksplit.repository.LinkRepository;
import com.linksplit.repository.LinkViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreatorAnalyticsService {
    private final LinkRepository linkRepository;
    private final LinkViewRepository linkViewRepository;
    private final ClickHeatmapRepository clickHeatmapRepository;
    
    public AnalyticsDashboard getAnalyticsDashboard(User user, LocalDateTime startDate, LocalDateTime endDate) {
        List<Link> userLinks = linkRepository.findByUser(user);
        List<LinkView> views = linkViewRepository.findByLinkInAndViewedAtBetween(userLinks, startDate, endDate);
        
        return AnalyticsDashboard.builder()
                .overview(calculateOverview(userLinks, views))
                .trafficSources(analyzeTrafficSources(views))
                .geographicData(analyzeGeographicData(views))
                .deviceAnalytics(analyzeDevices(views))
                .timeAnalytics(analyzeTimePatterns(views))
                .topPerformingLinks(getTopPerformingLinks(userLinks))
                .build();
    }
    
    public AnalyticsDashboard getAnalyticsForLink(Link link, LocalDateTime startDate, LocalDateTime endDate) {
        List<Link> singleLinkList = Collections.singletonList(link);
        List<LinkView> views = linkViewRepository.findByLinkAndViewedAtBetween(link, startDate, endDate);
        
        return AnalyticsDashboard.builder()
                .overview(calculateOverview(singleLinkList, views))
                .trafficSources(analyzeTrafficSources(views))
                .geographicData(analyzeGeographicData(views))
                .deviceAnalytics(analyzeDevices(views))
                .timeAnalytics(analyzeTimePatterns(views))
                .topPerformingLinks(Collections.singletonList(
                    LinkPerformance.builder()
                        .shortCode(link.getShortCode())
                        .longUrl(link.getLongUrl())
                        .views(link.getViewCount())
                        .earnings(link.getEstimatedEarnings())
                        .completionRate(calculateLinkCompletionRate(link))
                        .build()
                ))
                .build();
    }
    
    private OverviewMetrics calculateOverview(List<Link> links, List<LinkView> views) {
        long totalViews = views.size();
        long completedViews = views.stream().filter(v -> Boolean.TRUE.equals(v.getAdCompleted())).count();
        double completionRate = totalViews > 0 ? (double) completedViews / totalViews * 100 : 0;
        
        OptionalDouble avgTimeToSkip = views.stream()
                .filter(v -> v.getTimeToSkip() != null)
                .mapToInt(LinkView::getTimeToSkip)
                .average();
        
        return OverviewMetrics.builder()
                .totalLinks(links.size())
                .totalViews(totalViews)
                .completedViews(completedViews)
                .completionRate(completionRate)
                .averageTimeToSkip(avgTimeToSkip.orElse(0))
                .uniqueVisitors(views.stream().map(LinkView::getIpAddress).distinct().count())
                .build();
    }
    
    private List<TrafficSource> analyzeTrafficSources(List<LinkView> views) {
        Map<String, Long> sourceCount = new HashMap<>();
        
        for (LinkView view : views) {
            String source = "Direct";
            if (view.getUtmSource() != null) {
                source = view.getUtmSource();
            } else if (view.getReferrer() != null && !view.getReferrer().isEmpty()) {
                source = extractDomain(view.getReferrer());
            }
            sourceCount.merge(source, 1L, Long::sum);
        }
        
        return sourceCount.entrySet().stream()
                .map(entry -> TrafficSource.builder()
                        .source(entry.getKey())
                        .visits(entry.getValue())
                        .percentage((double) entry.getValue() / views.size() * 100)
                        .build())
                .sorted((a, b) -> Long.compare(b.getVisits(), a.getVisits()))
                .limit(10)
                .collect(Collectors.toList());
    }
    
    private List<GeographicData> analyzeGeographicData(List<LinkView> views) {
        Map<String, GeographicData.GeographicDataBuilder> countryMap = new HashMap<>();
        
        for (LinkView view : views) {
            String country = view.getCountry() != null ? view.getCountry() : "Unknown";
            countryMap.computeIfAbsent(country, k -> GeographicData.builder()
                    .country(k)
                    .views(0L)
                    .cities(new HashSet<>()))
                    .views(countryMap.get(country).build().getViews() + 1);
            
            if (view.getCity() != null) {
                countryMap.get(country).build().getCities().add(view.getCity());
            }
        }
        
        return countryMap.values().stream()
                .map(builder -> {
                    GeographicData data = builder.build();
                    data.setPercentage((double) data.getViews() / views.size() * 100);
                    return data;
                })
                .sorted((a, b) -> Long.compare(b.getViews(), a.getViews()))
                .limit(20)
                .collect(Collectors.toList());
    }
    
    private DeviceAnalytics analyzeDevices(List<LinkView> views) {
        Map<String, Long> deviceTypes = new HashMap<>();
        Map<String, Long> browsers = new HashMap<>();
        Map<String, Long> operatingSystems = new HashMap<>();
        
        for (LinkView view : views) {
            deviceTypes.merge(view.getDeviceType() != null ? view.getDeviceType() : "Unknown", 1L, Long::sum);
            browsers.merge(view.getBrowser() != null ? view.getBrowser() : "Unknown", 1L, Long::sum);
            operatingSystems.merge(view.getOperatingSystem() != null ? view.getOperatingSystem() : "Unknown", 1L, Long::sum);
        }
        
        return DeviceAnalytics.builder()
                .deviceTypes(convertToPercentageMap(deviceTypes, views.size()))
                .browsers(convertToPercentageMap(browsers, views.size()))
                .operatingSystems(convertToPercentageMap(operatingSystems, views.size()))
                .build();
    }
    
    private TimeAnalytics analyzeTimePatterns(List<LinkView> views) {
        Map<Integer, Long> hourlyDistribution = new TreeMap<>();
        Map<Integer, Long> dailyDistribution = new TreeMap<>();
        Map<String, Long> weeklyTrend = new TreeMap<>();
        
        for (LinkView view : views) {
            LocalDateTime viewTime = view.getViewedAt();
            hourlyDistribution.merge(viewTime.getHour(), 1L, Long::sum);
            dailyDistribution.merge(viewTime.getDayOfWeek().getValue(), 1L, Long::sum);
            
            String weekKey = viewTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
            weeklyTrend.merge(weekKey, 1L, Long::sum);
        }
        
        return TimeAnalytics.builder()
                .hourlyDistribution(hourlyDistribution)
                .dailyDistribution(dailyDistribution)
                .weeklyTrend(weeklyTrend)
                .peakHour(findPeakHour(hourlyDistribution))
                .peakDay(findPeakDay(dailyDistribution))
                .build();
    }
    
    private List<LinkPerformance> getTopPerformingLinks(List<Link> links) {
        return links.stream()
                .map(link -> LinkPerformance.builder()
                        .shortCode(link.getShortCode())
                        .longUrl(link.getLongUrl())
                        .views(link.getViewCount())
                        .earnings(link.getEstimatedEarnings())
                        .completionRate(calculateLinkCompletionRate(link))
                        .build())
                .sorted((a, b) -> Long.compare(b.getViews(), a.getViews()))
                .limit(10)
                .collect(Collectors.toList());
    }
    
    private double calculateLinkCompletionRate(Link link) {
        List<LinkView> views = linkViewRepository.findByLink(link);
        if (views.isEmpty()) return 0;
        
        long completed = views.stream().filter(v -> Boolean.TRUE.equals(v.getAdCompleted())).count();
        return (double) completed / views.size() * 100;
    }
    
    private String extractDomain(String url) {
        try {
            java.net.URL netUrl = new java.net.URL(url);
            return netUrl.getHost().replaceFirst("^www\\.", "");
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private Map<String, Double> convertToPercentageMap(Map<String, Long> countMap, long total) {
        return countMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (double) e.getValue() / total * 100
                ));
    }
    
    private Integer findPeakHour(Map<Integer, Long> hourlyDistribution) {
        return hourlyDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
    }
    
    private String findPeakDay(Map<Integer, Long> dailyDistribution) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        Integer peakDayNum = dailyDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(1);
        return days[peakDayNum - 1];
    }
}