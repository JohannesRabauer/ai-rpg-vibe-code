package com.airpg.agents.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of ChatMemoryStore for NPC conversations.
 * Stores conversation histories for each NPC/agent.
 * Future: Can be replaced with JOOQ-based persistent storage.
 */
@ApplicationScoped
public class InMemoryChatMemoryStore implements ChatMemoryStore {
    
    private final Map<Object, List<ChatMessage>> messagesByMemoryId = new ConcurrentHashMap<>();
    
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return new ArrayList<>(messagesByMemoryId.getOrDefault(memoryId, new ArrayList<>()));
    }
    
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        messagesByMemoryId.put(memoryId, new ArrayList<>(messages));
    }
    
    @Override
    public void deleteMessages(Object memoryId) {
        messagesByMemoryId.remove(memoryId);
    }
    
    /**
     * Clear all conversation memories (e.g., when starting a new game)
     */
    public void clearAll() {
        messagesByMemoryId.clear();
    }
    
    /**
     * Get number of stored conversations
     */
    public int getConversationCount() {
        return messagesByMemoryId.size();
    }
}
