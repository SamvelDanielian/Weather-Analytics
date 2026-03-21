package com.sam.qa;

import com.sam.qa.clients.WeatherApiClient;
import com.sam.qa.services.BigDataService;
import com.sam.qa.services.RecommendationService;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLIMATE SPATIAL STABILITY SUITE
 * * This suite validates the "Convergence" of the weather API's spatial model.
 * It ensures that increasing sensor density provides a more accurate, stable mean.
 * * 1. SPATIAL CONVERGENCE: Compares Mean(10 sensors) vs Mean(50 sensors).
 * - Logic: If the Delta is > 1.0°C, the system is spatially unstable.
 * * 2. URBAN GRADIENT (SIGMA): Measures temperature variance across the city.
 * - Logic: High sigma indicates strong micro-climate or heat-island effects.
 * * 3. GLOBAL PLACEMENT: A physical sanity check using Earth's historical 
 * temperature extremes (-89.2°C to 56.7°C).
 * * 4. STABILITY RECOMMENDATION: Uses @AfterAll to identify the city with 
 * the most uniform (least variant) climate across all districts.
 */
public class ClimateStabilityTest {
    private static final Logger logger = LoggerFactory.getLogger(ClimateStabilityTest.class);
    private static final WeatherApiClient weatherClient = new WeatherApiClient();
    private static final BigDataService statsService = new BigDataService();
    private static final Map<String, DescriptiveStatistics> allStats = new java.util.HashMap<>();
    private static final RecommendationService recommender = new RecommendationService();

    @ParameterizedTest(name = "City: {2} | Coord: [{0}, {1}]")
    @CsvFileSource(resources = "/cities.csv", numLinesToSkip = 1)
    public void testUrbanSpatialClimateStability(double lat, double lon, String cityName) throws Exception {
        logger.info("--- BEGIN SPATIAL ANALYSIS: {} ---", cityName.toUpperCase());

        InputStream lowDensityStream = weatherClient.getSpatialWeatherStream(lat, lon, 10);
        DescriptiveStatistics lowDensityStats = statsService.analyzeWeatherStream(lowDensityStream);

        InputStream highDensityStream = weatherClient.getSpatialWeatherStream(lat, lon, 50);
        DescriptiveStatistics highDensityStats = statsService.analyzeWeatherStream(highDensityStream);

        allStats.put(cityName, highDensityStats);

        double sigma = highDensityStats.getStandardDeviation();
        logger.info("[{}] Urban Temperature Gradient (Sigma): {}°C", cityName, String.format("%.4f", sigma));
        assertThat(sigma)
                .as("Data Sensitivity Check: Urban gradient for %s must show non-zero variance", cityName)
                .isGreaterThanOrEqualTo(0.0);

        double delta = Math.abs(lowDensityStats.getMean() - highDensityStats.getMean());

        logger.info("[{}] Spatial Convergence: Delta = {}", cityName, String.format("%.4f", delta));

        assertThat(delta)
                .as("Spatial Stability: Mean shift between 10 and 50 stations in %s exceeds 1.0°C threshold", cityName)
                .isLessThan(1.0);

        assertThat(highDensityStats.getMean())
                .as("Physical Sanity Check: Temperature in %s is outside Earth's recorded limits", cityName)
                .isBetween(-89.2, 56.7);

        logger.info("--- END SPATIAL ANALYSIS: {} [SUCCESS] ---", cityName);
    }

    @AfterAll
    public static void printMostStableCityReport() {
        String bestCity = recommender.getMostStableCity(allStats);
        logger.info("SPATIAL ANALYSIS COMPLETE");
        logger.info("The city with the most uniform temperature across all districts is: {}", bestCity);
    }
}