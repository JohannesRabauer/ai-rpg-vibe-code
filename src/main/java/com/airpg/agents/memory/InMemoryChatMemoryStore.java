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

    /**
     * Get all memories as a map for persistence
     */
    public Map<Object, List<ChatMessage>> getAllMemories() {
        Map<Object, List<ChatMessage>> copy = new ConcurrentHashMap<>();
        for (Map.Entry<Object, List<ChatMessage>> entry : messagesByMemoryId.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Restore memories from a map (e.g., loaded from database)
     */
    public void restoreMemories(Map<Object, List<ChatMessage>> memories) {
        messagesByMemoryId.clear();
        for (Map.Entry<Object, List<ChatMessage>> entry : memories.entrySet()) {
            messagesByMemoryId.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
    }
}
