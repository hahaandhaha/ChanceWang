package com.example.smartcustomerservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chat.memory")
public record ChatMemoryProperties(int maxMessages) {

    public ChatMemoryProperties {
        maxMessages = maxMessages <= 0 ? 20 : maxMessages;
    }
}
