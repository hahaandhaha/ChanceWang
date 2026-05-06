package com.example.smartcustomerservice;

import com.example.smartcustomerservice.chat.ConversationSessionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.qwen.api-key=test-key",
                "app.qwen.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1",
                "app.qwen.model=qwen3.5-plus",
                "app.baidu-map-mcp.enabled=false"
        }
)
@AutoConfigureWebTestClient
class SmartCustomerServiceApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateAndDeleteConversationSession() {
        ConversationSessionResponse session = webTestClient.post()
                .uri("/api/chat/sessions")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConversationSessionResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(session);
        assertNotNull(session.conversationId());
        assertFalse(session.conversationId().isBlank());

        webTestClient.delete()
                .uri("/api/chat/sessions/{conversationId}", session.conversationId())
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void shouldReturnNotFoundForUnknownConversation() {
        webTestClient.post()
                .uri("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)
                .bodyValue("""
                        {
                          "conversationId": "missing-session",
                          "message": "hello"
                        }
                        """)
                .exchange()
                .expectStatus().isNotFound();
    }
}
