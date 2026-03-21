package com.sam.qa.clients;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import com.sam.qa.config.ConfigReader;
import io.restassured.RestAssured;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicReference;

public class WeatherApiClient {
    private static final Logger logger = LoggerFactory.getLogger(WeatherApiClient.class);
    private final ConfigReader config = new ConfigReader();

    public InputStream getSpatialWeatherStream(double lat, double lon, int stations) {
        String url = String.format("%s?lat=%f&lon=%f&cnt=%d&units=metric&appid=%s",
                config.getProperty("api.url"), lat, lon, stations, config.getProperty("api.key"));
                
        AtomicReference<InputStream> responseStream = new AtomicReference<>();

        await()
                .atMost(5, SECONDS)
                .pollInterval(2, SECONDS)
                .ignoreExceptions()
                .until(() -> {
                    logger.info("📡 Requesting spatial cluster ({} stations) for [{}, {}]", stations, lat, lon);
                    responseStream.set(RestAssured.get(url).asInputStream());
                    return responseStream.get() != null;
                });

        return responseStream.get();
    }
}