package com.airpg.services;

/**
 * Callback interface for handling streaming text tokens from AI responses
 */
@FunctionalInterface
public interface StreamingResponseHandler {
    /**
     * Called when a new token is received from the AI model
     * @param token The text token to append to the response
     */
    void onToken(String token);
}
