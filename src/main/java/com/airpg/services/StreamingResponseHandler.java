package com.airpg.services;

/**
 * Callback interface for handling streaming text tokens from AI responses
 */
public interface StreamingResponseHandler {
    /**
     * Called when a new token is received from the AI model
     * @param token The text token to append to the response
     */
    void onToken(String token);
    
    /**
     * Called when streaming is complete (optional, default is no-op)
     * @param fullResponse The complete response text
     */
    default void onComplete(String fullResponse) {
        // Default: do nothing
    }
    
    /**
     * Called when an error occurs during streaming (optional, default is no-op)
     * @param error The error that occurred
     */
    default void onError(Throwable error) {
        // Default: do nothing
    }
}
