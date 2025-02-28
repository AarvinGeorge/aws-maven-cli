package edu.cmu.cs.cloud.aws.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AWSConfigLoader {

    private static Properties config;

    public static Properties loadConfig() {
        config = new Properties();
        try (InputStream input = AWSConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Unable to find config.properties in the resources folder.");
                System.exit(1);
            }
            config.load(input);
        } catch (IOException e) {
            System.out.println("Failed to load configuration: " + e.getMessage());
            System.exit(1);
        }
        return config;
    }

    public static String getDefaultTagKey() {
        return config.getProperty("default.tag.key", "project");
    }

    public static String getDefaultTagValue() {
        return config.getProperty("default.tag.value", "twitter-phase-1");
    }
}
