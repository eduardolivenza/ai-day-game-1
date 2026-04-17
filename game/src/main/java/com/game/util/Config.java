package com.game.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private final Properties props = new Properties();

    public Config() {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.out.println("[Config] config.properties not found – using defaults.");
        }
    }

    /** Base URL of the AI_Day_Game_Server (no trailing slash). */
    public String getServerUrl() {
        return props.getProperty("serverUrl", "http://localhost:8080").trim();
    }
}
