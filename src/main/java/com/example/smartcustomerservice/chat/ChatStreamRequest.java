package com.example.smartcustomerservice.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatStreamRequest(
        @NotBlank String conversationId,
        @NotBlank String message
) {
}
