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

public class StatsTest {
    private static final Logger logger = LoggerFactory.getLogger(StatsTest.class);
    private static final WeatherApiClient weatherClient = new WeatherApiClient();
    private static final BigDataService statsService = new BigDataService();
    private static final Map<String, DescriptiveStatistics> allStats = new java.util.HashMap<>();
    private static final RecommendationService recommender = new RecommendationService();

    @ParameterizedTest
    @CsvFileSource(resources = "/cities.csv", numLinesToSkip = 1)
    public void testWeatherStatsForCities(double lat, double lon, String cityName) throws Exception {
        logger.info("🎬 Processing 100 data points for: {}", cityName);

        InputStream stream = weatherClient.getCurrentWeatherStream(lat, lon);
        DescriptiveStatistics stats = statsService.analyzeWeatherStream(stream);
        allStats.put(cityName, stats);

        assertThat(stats.getMean()).isBetween(-60.0, 60.0);
        assertThat(stats.getStandardDeviation())
                .as("Temperature variation in %s", cityName)
                .isGreaterThan(0.0);

        logger.info("🏁 Result for {}: Mean={}°C, StdDev={}", cityName,
                String.format("%.2f", stats.getMean()),
                String.format("%.2f", stats.getStandardDeviation()));
    }

    @AfterAll
    public static void printFinalReport() {
        String bestCity = recommender.getMostStableCity(allStats);
        logger.info("**************************************************");
        logger.info("🏆 FINAL SYSTEM REPORT");
        logger.info("The most stable city for travel today is: {}", bestCity);
        logger.info("**************************************************");
    }
}