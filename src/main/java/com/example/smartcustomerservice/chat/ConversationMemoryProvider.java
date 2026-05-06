package com.example.smartcustomerservice.chat;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConversationMemoryProvider implements ChatMemoryProvider {

    private final int maxMessages;
    private final ConcurrentMap<String, ChatMemory> memories = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> sessions = new ConcurrentHashMap<>();

    public ConversationMemoryProvider(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public String createSession() {
        String conversationId = UUID.randomUUID().toString();
        sessions.put(conversationId, Boolean.TRUE);
        return conversationId;
    }

    public boolean hasSession(String conversationId) {
        return sessions.containsKey(conversationId);
    }

    public boolean deleteSession(String conversationId) {
        if (sessions.remove(conversationId) == null) {
            return false;
        }

        ChatMemory memory = memories.remove(conversationId);
        if (memory != null) {
            memory.clear();
        }
        return true;
    }

    @Override
    public ChatMemory get(Object memoryId) {
        String conversationId = String.valueOf(memoryId);
        if (!hasSession(conversationId)) {
            throw new IllegalArgumentException("Unknown conversationId: " + conversationId);
        }

        return memories.computeIfAbsent(
                conversationId,
                ignored -> MessageWindowChatMemory.withMaxMessages(maxMessages)
        );
    }
}
