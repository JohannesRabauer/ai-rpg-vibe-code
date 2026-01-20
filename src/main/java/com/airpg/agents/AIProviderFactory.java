package com.airpg.agents;

import com.airpg.config.AIConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Factory for creating ChatLanguageModel instances based on configured provider.
 * Supports OpenAI and Ollama with configuration from application.properties.
 */
@ApplicationScoped
public class AIProviderFactory {
    
    private static final Logger LOG = Logger.getLogger(AIProviderFactory.class);
    
    @Inject
    AIConfig aiConfig;
    
    /**
     * Create a ChatLanguageModel based on the configured provider
     */
    public ChatLanguageModel createChatModel() {
        String provider = aiConfig.provider().toLowerCase();
        
        LOG.infof("Creating AI chat model for provider: %s", provider);
        
        return switch (provider) {
            case "openai" -> createOpenAIChatModel();
            case "ollama" -> createOllamaChatModel();
            default -> {
                LOG.warnf("Unknown AI provider '%s', falling back to OpenAI", provider);
                yield createOpenAIChatModel();
            }
        };
    }
    
    /**
     * Create OpenAI chat model
     */
    private ChatLanguageModel createOpenAIChatModel() {
        AIConfig.OpenAIConfig config = aiConfig.openai();
        
        return OpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model())
                .temperature(config.temperature())
                .maxTokens(config.maxTokens())
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
    
    /**
     * Create Ollama chat model (local)
     */
    private ChatLanguageModel createOllamaChatModel() {
        AIConfig.OllamaConfig config = aiConfig.ollama();
        
        return OllamaChatModel.builder()
                .baseUrl(config.baseUrl())
                .modelName(config.model())
                .temperature(config.temperature())
                .timeout(Duration.ofSeconds(120))
                .build();
    }
    
    /**
     * Get the current provider name
     */
    public String getProviderName() {
        return aiConfig.provider();
    }
}
