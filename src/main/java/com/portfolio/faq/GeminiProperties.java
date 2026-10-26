package com.portfolio.faq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("gemini")
public record GeminiProperties(String apiKey, String model) {
    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
