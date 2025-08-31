package com.linksplit.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalyticsDashboard {
    private OverviewMetrics overview;
    private List<TrafficSource> trafficSources;
    private List<GeographicData> geographicData;
    private DeviceAnalytics deviceAnalytics;
    private TimeAnalytics timeAnalytics;
    private List<LinkPerformance> topPerformingLinks;
}