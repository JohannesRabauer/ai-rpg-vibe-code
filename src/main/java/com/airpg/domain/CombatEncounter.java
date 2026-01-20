package com.airpg.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Represents a combat encounter in the game.
 * Manages turn-based combat between the player's party and enemies.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombatEncounter {
    
    private String id;
    private List<GameCharacter> playerParty; // Hero + team members
    private List<NPC> enemies;
    private CombatStatus status;
    private int currentTurn;
    private String location;
    
    public enum CombatStatus {
        IN_PROGRESS,
        PLAYER_VICTORY,
        PLAYER_DEFEAT,
        FLED
    }
    
    /**
     * Check if combat is ongoing
     */
    public boolean isActive() {
        return status == CombatStatus.IN_PROGRESS;
    }
    
    /**
     * Check if all enemies are defeated
     */
    public boolean areAllEnemiesDefeated() {
        return enemies.stream().noneMatch(GameCharacter::isAlive);
    }
    
    /**
     * Check if all party members are defeated
     */
    public boolean isPartyDefeated() {
        return playerParty.stream().noneMatch(GameCharacter::isAlive);
    }
    
    /**
     * Advance to next turn
     */
    public void nextTurn() {
        currentTurn++;
    }
    
    /**
     * End combat with victory
     */
    public void endWithVictory() {
        this.status = CombatStatus.PLAYER_VICTORY;
    }
    
    /**
     * End combat with defeat
     */
    public void endWithDefeat() {
        this.status = CombatStatus.PLAYER_DEFEAT;
    }
    
    /**
     * End combat by fleeing
     */
    public void endByFleeing() {
        this.status = CombatStatus.FLED;
    }
}
