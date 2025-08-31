package com.linksplit.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DeviceAnalytics {
    private Map<String, Double> deviceTypes;
    private Map<String, Double> browsers;
    private Map<String, Double> operatingSystems;
}