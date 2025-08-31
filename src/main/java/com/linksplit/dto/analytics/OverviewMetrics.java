package com.linksplit.dto.analytics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverviewMetrics {
    private long totalLinks;
    private long totalViews;
    private long completedViews;
    private double completionRate;
    private double averageTimeToSkip;
    private long uniqueVisitors;
}