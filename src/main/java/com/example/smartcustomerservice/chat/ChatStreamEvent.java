package com.example.smartcustomerservice.chat;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatStreamEvent(
        String type,
        String text,
        String responseId,
        TokenUsage usage,
        String message
) {
}
