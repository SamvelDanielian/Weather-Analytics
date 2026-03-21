package com.sam.qa;

import com.sam.qa.clients.WeatherApiClient;
import com.sam.qa.services.BigDataService;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GLOBAL RISK SUITE
 * Purpose: Validates the physical plausibility of the entire weather system.
 * Strategy: Aggregates data from all cities in cities.csv to identify system-wide glitches.
 */
public class RiskSuite {
    private static final Logger logger = LoggerFactory.getLogger(RiskSuite.class);
    private static final Map<String, Double> globalCityData = new HashMap<>();

    @BeforeAll
    public static void harvestGlobalData() throws Exception {
        WeatherApiClient client = new WeatherApiClient();
        BigDataService statsService = new BigDataService();
        String csvPath = Paths.get("src", "test", "resources", "cities.csv").toString();
        
        logger.info("INITIATING GLOBAL DATA HARVEST...");

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String cityName = values[2];
                
                DescriptiveStatistics stats = statsService.analyzeWeatherStream(
                    client.getSpatialWeatherStream(Double.parseDouble(values[0]), Double.parseDouble(values[1]), 10)
                );
                
                globalCityData.put(cityName, stats.getMean());
            }
        }
        logger.info("Harvest Complete. {} cities indexed.", globalCityData.size());
    }

    @Test
    public void testGlobalTemperatureSpread() {
        double max = globalCityData.values().stream().max(Double::compare).orElse(0.0);
        double min = globalCityData.values().stream().min(Double::compare).orElse(0.0);
        double spread = max - min;

        logger.info("Global Range: {}°C to {}°C (Spread: {}°C)", min, max, String.format("%.2f", spread));

        assertThat(spread)
            .as("Risk: Global temperature spread is suspiciously narrow (< 5°C)")
            .isGreaterThan(5.0);
    }

    @Test
    public void testAverageGlobalPlausibility() {
        double avg = globalCityData.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        logger.info("Global Average Temperature: {}°C", String.format("%.2f", avg));

        assertThat(avg)
            .as("Risk: Global average temperature is physically impossible")
            .isBetween(-5.0, 25.0);
    }
}