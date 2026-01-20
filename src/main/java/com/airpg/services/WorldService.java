package com.airpg.services;

import com.airpg.agents.AgentService;
import com.airpg.agents.WorldNarratorAgent;
import com.airpg.domain.GameState;
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
