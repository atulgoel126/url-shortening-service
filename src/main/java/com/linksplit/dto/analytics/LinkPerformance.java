package com.linksplit.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LinkPerformance {
    private String shortCode;
    private String longUrl;
    private Long views;
    private BigDecimal earnings;
    private Double completionRate;
}