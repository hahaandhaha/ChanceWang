package com.example.smartcustomerservice.chat;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final CustomerSupportAssistant customerSupportAssistant;
    private final ConversationMemoryProvider conversationMemoryProvider;
    private final Set<String> inFlightConversationIds = ConcurrentHashMap.newKeySet();

    public ChatService(
            CustomerSupportAssistant customerSupportAssistant,
            ConversationMemoryProvider conversationMemoryProvider
    ) {
        this.customerSupportAssistant = customerSupportAssistant;
        this.conversationMemoryProvider = conversationMemoryProvider;
    }

    public ConversationSessionResponse createSession() {
        return new ConversationSessionResponse(conversationMemoryProvider.createSession());
    }

    public void deleteSession(String conversationId) {
        if (!conversationMemoryProvider.hasSession(conversationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "conversationId not found");
        }
        if (inFlightConversationIds.contains(conversationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "conversation is currently streaming");
        }
        conversationMemoryProvider.deleteSession(conversationId);
    }

    public Flux<ChatStreamEvent> stream(ChatStreamRequest request) {
        String conversationId = request.conversationId();
        if (!conversationMemoryProvider.hasSession(conversationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "conversationId not found");
        }
        if (!inFlightConversationIds.add(conversationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "conversation is currently streaming");
        }

        return Flux.create(sink -> {
            AtomicReference<dev.langchain4j.model.output.TokenUsage> aggregateUsage = new AtomicReference<>();
            Runnable release = () -> inFlightConversationIds.remove(conversationId);

            sink.onDispose(release::run);

            Schedulers.boundedElastic().schedule(() -> startStreaming(request, aggregateUsage, sink, release));
        });
    }

    private void startStreaming(
            ChatStreamRequest request,
            AtomicReference<dev.langchain4j.model.output.TokenUsage> aggregateUsage,
            FluxSink<ChatStreamEvent> sink,
            Runnable release
    ) {
        try {
            TokenStream tokenStream = customerSupportAssistant.chat(request.conversationId(), request.message())
                    .onPartialResponse(partialResponse -> emit(
                            sink,
                            new ChatStreamEvent("delta", partialResponse, null, null, null)
                    ))
                    .onIntermediateResponse(response -> mergeUsage(aggregateUsage, response))
                    .onCompleteResponse(response -> {
                        mergeUsage(aggregateUsage, response);
                        emit(sink, new ChatStreamEvent(
                                "done",
                                null,
                                response.id(),
                                toUsage(aggregateUsage.get()),
                                null
                        ));
                        release.run();
                        sink.complete();
                    })
                    .onError(error -> {
                        log.warn("Streaming chat failed for conversationId={}: {}", request.conversationId(), error.getMessage(), error);
                        emit(sink, new ChatStreamEvent(
                                "error",
                                null,
                                null,
                                null,
                                error.getMessage() == null ? "Unknown streaming error" : error.getMessage()
                        ));
                        release.run();
                        sink.complete();
                    });

            tokenStream.start();
        } catch (Throwable error) {
            log.error("Unable to start chat stream for conversationId={}: {}", request.conversationId(), error.getMessage(), error);
            emit(sink, new ChatStreamEvent(
                    "error",
                    null,
                    null,
                    null,
                    error.getMessage() == null ? "Unknown streaming error" : error.getMessage()
            ));
            release.run();
            sink.complete();
        }
    }

    private void mergeUsage(AtomicReference<dev.langchain4j.model.output.TokenUsage> aggregateUsage, ChatResponse response) {
        dev.langchain4j.model.output.TokenUsage usage = response == null ? null : response.tokenUsage();
        if (usage == null) {
            return;
        }

        aggregateUsage.updateAndGet(existing -> existing == null ? usage : existing.add(usage));
    }

    private TokenUsage toUsage(dev.langchain4j.model.output.TokenUsage usage) {
        if (usage == null) {
            return null;
        }

        return new TokenUsage(
                usage.inputTokenCount(),
                usage.outputTokenCount(),
                usage.totalTokenCount()
        );
    }

    private void emit(FluxSink<ChatStreamEvent> sink, ChatStreamEvent event) {
        if (!sink.isCancelled()) {
            sink.next(event);
        }
    }
}
