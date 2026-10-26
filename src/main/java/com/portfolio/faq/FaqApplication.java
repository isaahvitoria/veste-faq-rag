package com.portfolio.faq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties.class)
public class FaqApplication {
    public static void main(String[] args) {
        SpringApplication.run(FaqApplication.class, args);
    }
}
