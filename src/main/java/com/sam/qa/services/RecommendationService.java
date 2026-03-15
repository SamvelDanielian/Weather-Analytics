package com.sam.qa.services;

import java.util.Map;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class RecommendationService {

    public String getMostStableCity(Map<String, DescriptiveStatistics> cityStats) {
        return cityStats.entrySet().stream()
            .min((e1, e2) -> Double.compare(
                e1.getValue().getStandardDeviation(), 
                e2.getValue().getStandardDeviation()))
            .map(Map.Entry::getKey)
            .orElse("Unknown");
    }
}