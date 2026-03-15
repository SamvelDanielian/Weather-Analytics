package com.sam.qa.config;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private Properties props = new Properties();

    public ConfigReader() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Could not load config.properties");
        }
    }

    public String getProperty(String key) {
        String envValue = System.getenv(key.replace(".", "_").toUpperCase());
        if (envValue != null) return envValue;
        return props.getProperty(key);
    }
}