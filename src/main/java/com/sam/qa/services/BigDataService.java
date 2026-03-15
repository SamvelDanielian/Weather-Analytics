package com.sam.qa.services;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import java.io.InputStream;

public class BigDataService {

    public DescriptiveStatistics analyzeWeatherStream(InputStream jsonStream) throws Exception {
        JsonFactory factory = new JsonFactory();
        DescriptiveStatistics stats = new DescriptiveStatistics();

        try (JsonParser parser = factory.createParser(jsonStream)) {
            while (parser.nextToken() != null) {
                if (JsonToken.FIELD_NAME.equals(parser.currentToken()) && "temp".equals(parser.currentName())) {
                    parser.nextToken();
                    stats.addValue(parser.getValueAsDouble());
                }
            }
        }
        return stats;
    }
}