package com.example.smartcustomerservice.config;

import com.example.smartcustomerservice.chat.ConversationMemoryProvider;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilder;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class LangChain4jConfig {

    @Bean("qwenStreamingChatModel")
    OpenAiStreamingChatModel qwenStreamingChatModel(QwenProperties qwenProperties) {
        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(new SpringRestClientBuilder())
                .apiKey(qwenProperties.apiKey())
                .baseUrl(qwenProperties.baseUrl())
                .modelName(qwenProperties.model())
                .accumulateToolCallId(false)
                .logRequests(qwenProperties.logRequests())
                .logResponses(qwenProperties.logResponses())
                .build();
    }

    @Bean("conversationMemoryProvider")
    ConversationMemoryProvider conversationMemoryProvider(ChatMemoryProperties chatMemoryProperties) {
        return new ConversationMemoryProvider(chatMemoryProperties.maxMessages());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.baidu-map-mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
    McpClient baiduMapMcpClient(BaiduMapMcpProperties baiduMapMcpProperties) {
        Map<String, String> environment = new HashMap<>(System.getenv());
        String baiduMapApiKey = StringUtils.hasText(environment.get("BAIDU_MAPS_AK"))
                ? environment.get("BAIDU_MAPS_AK")
                : environment.get("BAIDU_MAP_API_KEY");
        if (!StringUtils.hasText(baiduMapApiKey)) {
            throw new IllegalStateException(
                    "Missing Baidu Maps API key. Set BAIDU_MAPS_AK or BAIDU_MAP_API_KEY, "
                            + "or disable MCP with BAIDU_MAPS_MCP_ENABLED=false."
            );
        }
        environment.put("BAIDU_MAP_API_KEY", baiduMapApiKey);

        StdioMcpTransport transport = new StdioMcpTransport.Builder()
                .command(baiduMapMcpProperties.command())
                .environment(environment)
                .logEvents(baiduMapMcpProperties.logEvents())
                .build();

        return new DefaultMcpClient.Builder()
                .key("baidu-map")
                .transport(transport)
                .build();
    }

    @Bean
    @ConditionalOnBean(McpClient.class)
    McpToolProvider baiduMapMcpToolProvider(McpClient baiduMapMcpClient) {
        return McpToolProvider.builder()
                .mcpClients(baiduMapMcpClient)
                .build();
    }

    @Bean("conditionalMcpToolProvider")
    ToolProvider conditionalMcpToolProvider(
            BaiduMapMcpProperties baiduMapMcpProperties,
            ObjectProvider<McpToolProvider> mcpToolProviderProvider
    ) {
        return request -> {
            if (!baiduMapMcpProperties.enabled()) {
                return ToolProviderResult.builder().build();
            }

            String userText = request.userMessage() == null ? "" : request.userMessage().singleText();
            if (!StringUtils.hasText(userText)) {
                return ToolProviderResult.builder().build();
            }

            String normalizedText = userText.toLowerCase(Locale.ROOT);
            boolean shouldUseMapTools = baiduMapMcpProperties.triggerKeywords().stream()
                    .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                    .anyMatch(normalizedText::contains);

            if (!shouldUseMapTools) {
                return ToolProviderResult.builder().build();
            }

            McpToolProvider delegate = mcpToolProviderProvider.getIfAvailable();
            return delegate == null ? ToolProviderResult.builder().build() : delegate.provideTools(request);
        };
    }
}
