package com.airpg.services;

import com.airpg.agents.AgentService;
import com.airpg.agents.CompanionAgent;
import com.airpg.config.GameConfig;
import com.airpg.domain.GameState;
import com.airpg.domain.TeamMember;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Service for managing team members (companions).
 * Handles recruitment, dialogue, and autonomous companion behavior.
 */
@ApplicationScoped
public class TeamService {
    
    private static final Logger LOG = Logger.getLogger(TeamService.class);
    
    @Inject
    AgentService agentService;
    
    @Inject
    GameConfig gameConfig;
    
    /**
     * Recruit a team member to the party
     */
    public boolean recruitTeamMember(TeamMember member, GameState gameState) {
        boolean added = gameState.addTeamMember(member, gameConfig.maxTeamSize());
        
        if (added) {
            // Get initial greeting from the companion
            String greeting = getCompanionDialogue(member, 
                String.format("You've just joined %s's party. Introduce yourself briefly.", 
                    gameState.getHero().getName()),
                gameState);
            
            LOG.infof("Team member %s recruited. Greeting: %s", member.getName(), greeting);
            return true;
        } else {
            LOG.warnf("Failed to recruit %s - party full (max: %d)", 
                member.getName(), gameConfig.maxTeamSize());
            return false;
        }
    }
    
    /**
     * Get dialogue from a companion
     */
    public String getCompanionDialogue(TeamMember companion, String context, GameState gameState) {
        CompanionAgent agent = agentService.getCompanionAgent(companion);
        
        String fullContext = String.format("""
                You are %s, a %s in the party.
                Your personality: %s
                Your backstory: %s
                Your loyalty to the party: %d/100
                Current location: %s
                Party leader: %s
                
                Context: %s
                
                Speak as this character.
                """,
                companion.getName(),
                companion.getCharacterClass(),
                companion.getPersonality(),
                companion.getBackstory(),
                companion.getLoyalty(),
                gameState.getCurrentLocation(),
                gameState.getHero().getName(),
                context
        );
        
        return agent.speak(fullContext);
    }
    
    /**
     * Have companion react to an event or decision
     */
    public String getCompanionReaction(TeamMember companion, String event, GameState gameState) {
        CompanionAgent agent = agentService.getCompanionAgent(companion);
        
        String context = String.format("""
                You are %s (loyalty: %d/100).
                Personality: %s
                
                Something happened: %s
                
                React to this event. Your loyalty may be affected by your response.
                """,
                companion.getName(),
                companion.getLoyalty(),
                companion.getPersonality(),
                event
        );
        
        String reaction = agent.reactToEvent(context);
        
        // Adjust loyalty based on reaction sentiment (simplified)
        adjustLoyaltyFromReaction(companion, reaction);
        
        return reaction;
    }
    
    /**
     * Have all companions react to a major event
     */
    public String getAllCompanionsReactions(String event, GameState gameState) {
        StringBuilder reactions = new StringBuilder();
        
        for (TeamMember companion : gameState.getTeamMembers()) {
            String reaction = getCompanionReaction(companion, event, gameState);
            reactions.append(companion.getName())
                    .append(": ")
                    .append(reaction)
                    .append("\n");
        }
        
        return reactions.toString();
    }
    
    /**
     * Get autonomous combat decision from companion
     */
    public String getCompanionCombatDecision(TeamMember companion, String combatContext) {
        CompanionAgent agent = agentService.getCompanionAgent(companion);
        
        String context = String.format("""
                You are %s, a %s in combat.
                Your stats: HP %d/%d, Strength %d, Intelligence %d, Agility %d
                
                Combat situation: %s
                
                Decide your action.
                """,
                companion.getName(),
                companion.getCharacterClass(),
                companion.getCurrentHealth(),
                companion.getMaxHealth(),
                companion.getStrength(),
                companion.getIntelligence(),
                companion.getAgility(),
                combatContext
        );
        
        return agent.decideCombatAction(context);
    }
    
    /**
     * Simple loyalty adjustment based on reaction sentiment
     */
    private void adjustLoyaltyFromReaction(TeamMember companion, String reaction) {
        // This is a simplified approach - in a full implementation,
        // you might use sentiment analysis or more sophisticated methods
        String lower = reaction.toLowerCase();
        
        if (lower.contains("glad") || lower.contains("happy") || lower.contains("good")) {
            companion.adjustLoyalty(5);
        } else if (lower.contains("angry") || lower.contains("disagree") || 
                   lower.contains("wrong") || lower.contains("shouldn't")) {
            companion.adjustLoyalty(-5);
        }
        
        LOG.debugf("Companion %s loyalty adjusted to %d", 
            companion.getName(), companion.getLoyalty());
    }
    
    /**
     * Check if any companions are considering leaving
     */
    public String checkLoyaltyIssues(GameState gameState) {
        StringBuilder warnings = new StringBuilder();
        
        for (TeamMember companion : gameState.getTeamMembers()) {
            if (companion.isLikelyToLeave()) {
                String warning = getCompanionDialogue(companion,
                    "Your loyalty is low. Express your concerns about staying with the party.",
                    gameState);
                
                warnings.append(companion.getName())
                        .append(": ")
                        .append(warning)
                        .append("\n");
            }
        }
        
        return warnings.toString();
    }
}
