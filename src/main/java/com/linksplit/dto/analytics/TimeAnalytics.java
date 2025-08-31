package com.linksplit.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TimeAnalytics {
    private Map<Integer, Long> hourlyDistribution;
    private Map<Integer, Long> dailyDistribution;
    private Map<String, Long> weeklyTrend;
    private Integer peakHour;
    private String peakDay;
}