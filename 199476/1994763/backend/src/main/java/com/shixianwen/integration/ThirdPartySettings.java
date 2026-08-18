package com.shixianwen.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Component
public class ThirdPartySettings {
    private final Environment environment;
    private final Properties legacy = new Properties();

    public ThirdPartySettings(
        Environment environment,
        @Value("${app.integrations.legacy-properties-file:}") String legacyPropertiesFile
    ) {
        this.environment = environment;
        if (legacyPropertiesFile == null || legacyPropertiesFile.isBlank()) {
            return;
        }
        Path path = Path.of(legacyPropertiesFile).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (InputStream input = Files.newInputStream(path)) {
            legacy.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("第三方配置文件读取失败", exception);
        }
    }

    public String value(String propertyName, String legacyPropertyName) {
        String configured = environment.getProperty(propertyName);
        if (hasText(configured)) {
            return configured.trim();
        }
        String fallback = legacy.getProperty(legacyPropertyName);
        return fallback == null ? "" : fallback.trim();
    }

    public String value(String propertyName) {
        String configured = environment.getProperty(propertyName);
        return configured == null ? "" : configured.trim();
    }

    public boolean has(String propertyName, String legacyPropertyName) {
        return hasText(value(propertyName, legacyPropertyName));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
