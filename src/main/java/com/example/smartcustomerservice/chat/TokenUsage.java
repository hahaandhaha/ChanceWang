package com.example.smartcustomerservice.chat;

public record TokenUsage(
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens
) {
}
