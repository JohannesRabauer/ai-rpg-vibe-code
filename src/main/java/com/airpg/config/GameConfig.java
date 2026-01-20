package com.airpg.config;

import io.smallrye.config.ConfigMapping;
import java.util.List;

/**
 * Configuration for game mechanics and rules.
 * Maps to game.* properties in application.properties.
 */
@ConfigMapping(prefix = "game")
public interface GameConfig {
    
    /**
     * Maximum number of team members allowed
     */
    Integer maxTeamSize();
    
    /**
     * List of possible initial game goals (one selected randomly)
     */
    List<String> initialGoals();
}
