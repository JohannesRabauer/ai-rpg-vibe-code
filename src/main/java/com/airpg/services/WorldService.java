package com.airpg.services;

import com.airpg.agents.AgentService;
import com.airpg.agents.WorldNarratorAgent;
import com.airpg.agents.WorldNarratorStreamingAgent;
import com.airpg.domain.GameState;
import dev.langchain4j.service.TokenStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Service for managing world state and story progression.
 * Handles location changes, world events, and narrative generation.
 */
@ApplicationScoped
public class WorldService {
    
    private static final Logger LOG = Logger.getLogger(WorldService.class);
    
    @Inject
    AgentService agentService;
    
    /**
     * Generate an initial scene description for the game start
     */
    public String generateOpeningScene(GameState gameState) {
        WorldNarratorAgent narrator = agentService.getWorldNarrator();
        
        String context = String.format("""
                The game begins. The hero '%s' (a %s) stands in %s.
                Their quest: %s
                The adventure is about to begin. Set the scene and create anticipation.
                """,
                gameState.getHero().getName(),
                gameState.getHero().getCharacterClass(),
                gameState.getCurrentLocation(),
                gameState.getMainGoal()
        );
        
        String narration = narrator.narrateScene(context);
        LOG.debugf("Opening scene generated for hero: %s", gameState.getHero().getName());
        return narration;
    }
    
    /**
     * Describe the current location
     */
    public String describeLocation(GameState gameState) {
        WorldNarratorAgent narrator = agentService.getWorldNarrator();
        
        String context = String.format("""
                Location: %s
                Hero: %s (Level %d %s)
                Party size: %d
                Current quest: %s
                Describe this location in vivid detail.
                """,
                gameState.getCurrentLocation(),
                gameState.getHero().getName(),
                gameState.getHero().getLevel(),
                gameState.getHero().getCharacterClass(),
                gameState.getTeamMembers().size(),
                gameState.getMainGoal()
        );
        
        return narrator.describeLocation(context);
    }
    
    /**
     * Process player's exploration action (movement, investigation, etc.)
     */
    public String processPlayerAction(GameState gameState, String playerAction) {
        WorldNarratorAgent narrator = agentService.getWorldNarrator();
        
        String context = String.format("""
                Location: %s
                Hero: %s
                Player action: "%s"
                
                Respond to this action with narrative.
                If the action involves movement, describe the new location.
                If it involves discovery, reveal appropriate information.
                Consider potential encounters or events.
                """,
                gameState.getCurrentLocation(),
                gameState.getHero().getName(),
                playerAction
        );
        
        String response = narrator.respondToAction(context);
        LOG.debugf("Player action processed: %s", playerAction);
        return response;
    }
    
    /**
     * Generate a random encounter or event
     */
    public String generateRandomEvent(GameState gameState) {
        WorldNarratorAgent narrator = agentService.getWorldNarrator();
        
        String context = String.format("""
                Location: %s
                Hero: %s (Level %d)
                Party size: %d
                
                Generate a random event or encounter appropriate for this location.
                It could be:
                - Meeting an NPC
                - Finding an item or clue
                - Environmental challenge
                - Combat encounter
                - Story development
                """,
                gameState.getCurrentLocation(),
                gameState.getHero().getName(),
                gameState.getHero().getLevel(),
                gameState.getTeamMembers().size()
        );
        
        return narrator.narrateScene(context);
    }
    
    /**
     * Process player's exploration action (movement, investigation, etc.) with streaming
     */
    public void processPlayerActionStreaming(GameState gameState, String playerAction, StreamingResponseHandler handler) {
        WorldNarratorStreamingAgent narrator = agentService.getWorldNarratorStreaming();
        
        String context = String.format("""
                Location: %s
                Hero: %s
                Player action: "%s"
                
                Respond to this action with narrative.
                If the action involves movement, describe the new location.
                If it involves discovery, reveal appropriate information.
                Consider potential encounters or events.
                """,
                gameState.getCurrentLocation(),
                gameState.getHero().getName(),
                playerAction
        );
        
        TokenStream tokenStream = narrator.respondToAction(context);
        tokenStream
                .onNext(handler::onToken)
                .onComplete(response -> {
                    LOG.debugf("Player action streaming completed: %s", playerAction);
                    handler.onComplete(response.content().text());
                })
                .onError(error -> {
                    LOG.errorf(error, "Error during streaming response");
                    handler.onError(error);
                })
                .start();
    }
    
    /**
     * Generate an initial scene description for the game start with streaming
     */
    public void generateOpeningSceneStreaming(GameState gameState, StreamingResponseHandler handler) {
        WorldNarratorStreamingAgent narrator = agentService.getWorldNarratorStreaming();
        
        String context = String.format("""
                The game begins. The hero '%s' (a %s) stands in %s.
                Their quest: %s
                The adventure is about to begin. Set the scene and create anticipation.
                """,
                gameState.getHero().getName(),
                gameState.getHero().getCharacterClass(),
                gameState.getCurrentLocation(),
                gameState.getMainGoal()
        );
        
        TokenStream tokenStream = narrator.narrateScene(context);
        tokenStream
                .onNext(handler::onToken)
                .onComplete(response -> {
                    LOG.debugf("Opening scene streaming completed for hero: %s", gameState.getHero().getName());
                    handler.onComplete(response.content().text());
                })
                .onError(error -> {
                    LOG.errorf(error, "Error during opening scene streaming");
                    handler.onError(error);
                })
                .start();
    }
    
    /**
     * Describe the current location with streaming
     */
    public void describeLocationStreaming(GameState gameState, StreamingResponseHandler handler) {
        WorldNarratorStreamingAgent narrator = agentService.getWorldNarratorStreaming();
        
        String context = String.format("""
                Location: %s
                Hero: %s (Level %d %s)
                Party size: %d
                Current quest: %s
                Describe this location in vivid detail.
                """,
                gameState.getCurrentLocation(),
                gameState.getHero().getName(),
                gameState.getHero().getLevel(),
                gameState.getHero().getCharacterClass(),
                gameState.getTeamMembers().size(),
                gameState.getMainGoal()
        );
        
        TokenStream tokenStream = narrator.describeLocation(context);
        tokenStream
                .onNext(handler::onToken)
                .onComplete(response -> {
                    LOG.debugf("Location description streaming completed");
                    handler.onComplete(response.content().text());
                })
                .onError(error -> {
                    LOG.errorf(error, "Error during location description streaming");
                    handler.onError(error);
                })
                .start();
    }
    
    /**
     * Move to a new location and get description
     */
    public String travelToLocation(GameState gameState, String newLocation) {
        String oldLocation = gameState.getCurrentLocation();
        gameState.moveTo(newLocation);
        
        WorldNarratorAgent narrator = agentService.getWorldNarrator();
        
        String context = String.format("""
                The party travels from %s to %s.
                Hero: %s
                Party size: %d
                
                Describe the journey and arrival at the new location.
                """,
                oldLocation,
                newLocation,
                gameState.getHero().getName(),
                gameState.getTeamMembers().size()
        );
        
        LOG.infof("Party traveled from %s to %s", oldLocation, newLocation);
        return narrator.describeLocation(context);
    }
}
