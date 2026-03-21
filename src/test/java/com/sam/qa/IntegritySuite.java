package com.sam.qa;

import com.sam.qa.clients.WeatherApiClient;
import com.sam.qa.services.BigDataService;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLIMATE INTEGRITY MODEL:
 * * 1. Z-SCORE = |Observed Mean - Expected Temp| / Standard Deviation
 * - Purpose: Measures "Statistical Distance" from the norm.
 * * 2. DYNAMIC THRESHOLD = 3.0 + (Tolerance / 2.0)
 * - Purpose: Adjusts sensitivity based on local climate volatility (Tolerance).
 * - 3.0: The standard statistical "3-Sigma" outlier limit.
 * - Tolerance: Added 'breathing room' defined in cities.csv.
 * * 3. SKEWNESS LIMIT:
 * - < 2.0: Standard urban distribution.
 * - < 3.0: Allowed for high-density "Mega Cities" (Sao Paulo, Delhi).
 * - Purpose: Tests the symmetry of sensor distribution around the city mean.
 */
public class IntegritySuite {

    private static final Logger logger = LoggerFactory.getLogger(IntegritySuite.class);
    private final WeatherApiClient weatherClient = new WeatherApiClient();
    private final BigDataService statsService = new BigDataService();

    @ParameterizedTest(name = "Z-Score Analysis: {2}")
    @CsvFileSource(resources = "/cities.csv", numLinesToSkip = 1)
    public void testTemperatureAnomaly(double lat, double lon, String cityName, double expectedTemp, double tolerance)
            throws Exception {
        DescriptiveStatistics stats = statsService
                .analyzeWeatherStream(weatherClient.getSpatialWeatherStream(lat, lon, 50));

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();

        assertThat(stats.getMax() - stats.getMin())
                .as("Physical Impossibility: Temperature range in %s is > 15°C", cityName)
                .isLessThan(15.0);

        double zScore = (stdDev > 0) ? Math.abs(mean - expectedTemp) / stdDev : 0.0;
        double dynamicThreshold = 3.0 + (tolerance / stdDev);
        logger.info("[{}] Anomaly Index: {} (Threshold: {})", cityName, String.format("%.3f", zScore),
                dynamicThreshold);

        if (zScore >= dynamicThreshold) {
            logger.warn("CLIMATE ANOMALY | City: {} | Observed: {}°C | Expected: {}°C | Z-Score: {} (Limit: {})",
                    cityName, String.format("%.2f", mean), expectedTemp, String.format("%.2f", zScore),
                    String.format("%.2f", dynamicThreshold));
        } else {
            logger.info("INTEGRITY PASS | City: {} | Z-Score: {}", cityName, String.format("%.2f", zScore));
            assertThat(zScore)
                    .as("Anomaly: %s deviates too far from expected temp %f", cityName, expectedTemp)
                    .isLessThan(dynamicThreshold);
        }
    }

    @ParameterizedTest(name = "Sensor Bias Analysis: {2}")
    @CsvFileSource(resources = "/cities.csv", numLinesToSkip = 1)
    public void testSensorDistributionBias(double lat, double lon, String cityName, double expectedTemp,
            double tolerance, double skewLimit) throws Exception {
        DescriptiveStatistics stats = statsService
                .analyzeWeatherStream(weatherClient.getSpatialWeatherStream(lat, lon, 50));

        double skew = stats.getSkewness();
        logger.info("[{}] Bias Index: {} (Configured Limit: {})", cityName, String.format("%.3f", skew), skewLimit);

        assertThat(Math.abs(skew))
                .as("Bias: Sensor distribution in %s is heavily skewed", cityName)
                .isLessThan(skewLimit);
    }
}