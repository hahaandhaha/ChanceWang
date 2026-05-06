package com.example.smartcustomerservice.chat;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "conversationMemoryProvider",
        toolProvider = "conditionalMcpToolProvider"
)
public interface CustomerSupportAssistant {

    @SystemMessage(fromResource = "/prompts/customer-support-system-prompt.txt")
    TokenStream chat(@MemoryId String conversationId, @UserMessage String message);
}
