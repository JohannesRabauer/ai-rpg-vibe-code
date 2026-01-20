package com.airpg.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a quest in the game.
 * Quests can be main quests or side quests given by NPCs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quest {
    
    private String id;
    private String title;
    private String description;
    private String givenBy; // NPC ID who gave the quest
    private QuestStatus status;
    private boolean isMainQuest;
    private int experienceReward;
    
    public enum QuestStatus {
        ACTIVE,
        COMPLETED,
        FAILED
    }
    
    /**
     * Complete the quest
     */
    public void complete() {
        this.status = QuestStatus.COMPLETED;
    }
    
    /**
     * Fail the quest
     */
    public void fail() {
        this.status = QuestStatus.FAILED;
    }
    
    /**
     * Check if the quest is active
     */
    public boolean isActive() {
        return status == QuestStatus.ACTIVE;
    }
}
