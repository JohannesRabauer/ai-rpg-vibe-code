package com.airpg.domain;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages the current state of the game world.
 * This is the central state container for the entire game session.
 */
@Data
public class GameState {
    
    private String gameId;
    private Hero hero;
    private List<TeamMember> teamMembers;
    private List<NPC> worldNPCs;
    private List<Quest> quests;
    private CombatEncounter currentCombat;
    private String currentLocation;
    private String mainGoal;
    private GameStatus status;
    private List<String> storyHistory; // Record of major events
    
    public enum GameStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        GAME_OVER
    }
    
    /**
     * Create a new game state
     */
    public GameState() {
        this.gameId = UUID.randomUUID().toString();
        this.teamMembers = new ArrayList<>();
        this.worldNPCs = new ArrayList<>();
        this.quests = new ArrayList<>();
        this.storyHistory = new ArrayList<>();
        this.status = GameStatus.NOT_STARTED;
        this.currentLocation = "Village Square";
    }
    
    /**
     * Start a new game with hero and main goal
     */
    public void startGame(Hero hero, String mainGoal) {
        this.hero = hero;
        this.mainGoal = mainGoal;
        this.status = GameStatus.IN_PROGRESS;
        addToHistory("Your quest begins: " + mainGoal);
    }
    
    /**
     * Add a team member to the party
     */
    public boolean addTeamMember(TeamMember member, int maxTeamSize) {
        if (teamMembers.size() >= maxTeamSize) {
            return false;
        }
        teamMembers.add(member);
        addToHistory(member.getName() + " joined your party.");
        return true;
    }
    
    /**
     * Remove a team member from the party
     */
    public void removeTeamMember(TeamMember member) {
        teamMembers.remove(member);
        addToHistory(member.getName() + " left your party.");
    }
    
    /**
     * Add an NPC to the world
     */
    public void addNPC(NPC npc) {
        worldNPCs.add(npc);
    }
    
    /**
     * Find an NPC by ID
     */
    public NPC findNPC(String npcId) {
        return worldNPCs.stream()
                .filter(npc -> npc.getId().equals(npcId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Add a quest
     */
    public void addQuest(Quest quest) {
        quests.add(quest);
        addToHistory("New quest: " + quest.getTitle());
    }
    
    /**
     * Get active quests
     */
    public List<Quest> getActiveQuests() {
        return quests.stream()
                .filter(Quest::isActive)
                .toList();
    }
    
    /**
     * Start combat
     */
    public void startCombat(CombatEncounter combat) {
        this.currentCombat = combat;
        addToHistory("Combat started at " + combat.getLocation());
    }
    
    /**
     * End combat
     */
    public void endCombat() {
        if (currentCombat != null) {
            addToHistory("Combat ended: " + currentCombat.getStatus());
            this.currentCombat = null;
        }
    }
    
    /**
     * Check if currently in combat
     */
    public boolean isInCombat() {
        return currentCombat != null && currentCombat.isActive();
    }
    
    /**
     * Move to a new location
     */
    public void moveTo(String location) {
        this.currentLocation = location;
        addToHistory("Traveled to " + location);
    }
    
    /**
     * Add an event to the story history
     */
    public void addToHistory(String event) {
        storyHistory.add(event);
    }
    
    /**
     * Complete the game
     */
    public void completeGame() {
        this.status = GameStatus.COMPLETED;
        addToHistory("Quest completed: " + mainGoal);
    }
    
    /**
     * Game over (hero died or quest failed)
     */
    public void gameOver() {
        this.status = GameStatus.GAME_OVER;
        addToHistory("Game Over");
    }
    
    /**
     * Get all living party members (hero + team)
     */
    public List<GameCharacter> getLivingPartyMembers() {
        List<GameCharacter> party = new ArrayList<>();
        if (hero != null && hero.isAlive()) {
            party.add(hero);
        }
        teamMembers.stream()
                .filter(GameCharacter::isAlive)
                .forEach(party::add);
        return party;
    }
}
