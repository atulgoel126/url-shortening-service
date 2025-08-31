package com.linksplit.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class GeographicData {
    private String country;
    private Long views;
    private Double percentage;
    private Set<String> cities;
}