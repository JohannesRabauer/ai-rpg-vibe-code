package com.airpg.agents;

import com.airpg.agents.memory.InMemoryChatMemoryStore;
import com.airpg.domain.GameCharacter;
import com.airpg.domain.CombatAction;
import com.airpg.domain.NPC;
import com.airpg.domain.TeamMember;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central service for managing AI agents.
 * Creates and maintains different agent types for world narration, NPCs, and combat.
 */
@ApplicationScoped
public class AgentService {
    
    private static final Logger LOG = Logger.getLogger(AgentService.class);
    
    @Inject
    AIProviderFactory providerFactory;
    
    @Inject
    InMemoryChatMemoryStore memoryStore;
    
    // Agent caches
    private WorldNarratorAgent worldNarratorAgent;
    private WorldNarratorStreamingAgent worldNarratorStreamingAgent;
    private CombatNarratorAgent combatNarratorAgent;
    private final Map<String, NPCAgent> npcAgents = new ConcurrentHashMap<>();
    private final Map<String, CompanionAgent> companionAgents = new ConcurrentHashMap<>();
    
    /**
     * Get or create the world narrator agent
     */
    public WorldNarratorAgent getWorldNarrator() {
        if (worldNarratorAgent == null) {
            ChatLanguageModel model = providerFactory.createChatModel();
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .maxMessages(20)
                    .chatMemoryStore(memoryStore)
                    .id("world-narrator")
                    .build();
            
            worldNarratorAgent = AiServices.builder(WorldNarratorAgent.class)
                    .chatLanguageModel(model)
                    .chatMemory(memory)
                    .build();
            
            LOG.info("World narrator agent created");
        }
        return worldNarratorAgent;
    }
    
    /**
     * Get or create the streaming world narrator agent
     */
    public WorldNarratorStreamingAgent getWorldNarratorStreaming() {
        if (worldNarratorStreamingAgent == null) {
            StreamingChatLanguageModel model = providerFactory.createStreamingChatModel();
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .maxMessages(20)
                    .chatMemoryStore(memoryStore)
                    .id("world-narrator")
                    .build();
            
            worldNarratorStreamingAgent = AiServices.builder(WorldNarratorStreamingAgent.class)
                    .streamingChatLanguageModel(model)
                    .chatMemory(memory)
                    .build();
            
            LOG.info("Streaming world narrator agent created");
        }
        return worldNarratorStreamingAgent;
    }
    
    /**
     * Get or create the combat narrator agent
     */
    public CombatNarratorAgent getCombatNarrator() {
        if (combatNarratorAgent == null) {
            ChatLanguageModel model = providerFactory.createChatModel();
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .maxMessages(30)
                    .chatMemoryStore(memoryStore)
                    .id("combat-narrator")
                    .build();
            
            combatNarratorAgent = AiServices.builder(CombatNarratorAgent.class)
                    .chatLanguageModel(model)
                    .chatMemory(memory)
                    .build();
            
            LOG.info("Combat narrator agent created");
        }
        return combatNarratorAgent;
    }
    
    /**
     * Get or create an NPC agent
     */
    public NPCAgent getNPCAgent(NPC npc) {
        return npcAgents.computeIfAbsent(npc.getId(), id -> {
            ChatLanguageModel model = providerFactory.createChatModel();
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .maxMessages(15)
                    .chatMemoryStore(memoryStore)
                    .id("npc-" + id)
                    .build();
            
            NPCAgent agent = AiServices.builder(NPCAgent.class)
                    .chatLanguageModel(model)
                    .chatMemory(memory)
                    .build();
            
            LOG.infof("NPC agent created for: %s", npc.getName());
            return agent;
        });
    }
    
    /**
     * Get or create a companion agent
     */
    public CompanionAgent getCompanionAgent(TeamMember companion) {
        return companionAgents.computeIfAbsent(companion.getId(), id -> {
            ChatLanguageModel model = providerFactory.createChatModel();
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .maxMessages(15)
                    .chatMemoryStore(memoryStore)
                    .id("companion-" + id)
                    .build();
            
            CompanionAgent agent = AiServices.builder(CompanionAgent.class)
                    .chatLanguageModel(model)
                    .chatMemory(memory)
                    .build();
            
            LOG.infof("Companion agent created for: %s", companion.getName());
            return agent;
        });
    }
    
    /**
     * Clear all agent memories (useful for new game)
     */
    public void clearAllMemories() {
        memoryStore.clearAll();
        worldNarratorAgent = null;
        worldNarratorStreamingAgent = null;
        combatNarratorAgent = null;
        npcAgents.clear();
        companionAgents.clear();
        LOG.info("All agent memories cleared");
    }
}
