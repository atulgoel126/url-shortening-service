package com.linksplit.dto.analytics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrafficSource {
    private String source;
    private Long visits;
    private Double percentage;
}