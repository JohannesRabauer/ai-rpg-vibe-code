package com.airpg.config;

import io.smallrye.config.ConfigMapping;

/**
 * Configuration for AI provider settings.
 * Maps to ai.* properties in application.properties.
 */
@ConfigMapping(prefix = "ai")
public interface AIConfig {
    
    /**
     * The AI provider to use: "openai" or "ollama"
     */
    String provider();
    
    OpenAIConfig openai();
    
    OllamaConfig ollama();
    
    interface OpenAIConfig {
        String apiKey();
        String model();
        Double temperature();
        Integer maxTokens();
    }
    
    interface OllamaConfig {
        String baseUrl();
        String model();
        Double temperature();
        Integer maxTokens();
    }
}
