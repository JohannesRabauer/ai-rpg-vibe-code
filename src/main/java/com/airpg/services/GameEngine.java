package com.airpg.services;

import com.airpg.agents.AgentService;
import com.airpg.config.GameConfig;
import com.airpg.domain.*;
import dev.langchain4j.data.message.ChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Main game engine that coordinates all game systems.
 * Entry point for player actions and game progression.
 */
@ApplicationScoped
public class GameEngine {
    
    private static final Logger LOG = Logger.getLogger(GameEngine.class);
    private final Random random = new Random();
    
    @Inject
    GameConfig gameConfig;
    
    @Inject
    WorldService worldService;
    
    @Inject
    NPCService npcService;
    
    @Inject
    TeamService teamService;
    
    @Inject
    CombatService combatService;

    @Inject
    GamePersistenceService persistenceService;

    @Inject
    AgentService agentService;

    private GameState gameState;
    
    /**
     * Initialize a new game
     */
    public String startNewGame(String heroName, String heroClass) {
        gameState = new GameState();

        // Create hero
        Hero hero = Hero.createDefault(heroName, heroClass);

        // Select random main goal
        List<String> goals = gameConfig.initialGoals();
        String mainGoal = goals.get(random.nextInt(goals.size()));

        // Start the game
        gameState.startGame(hero, mainGoal);

        // Create initial NPCs
        NPC sage = npcService.createSampleNPC(gameState.getCurrentLocation());
        gameState.addNPC(sage);

        // Generate opening scene
        String opening = worldService.generateOpeningScene(gameState);

        LOG.infof("New game started: %s the %s - Quest: %s", heroName, heroClass, mainGoal);

        return String.format("""
                %s

                Your stats:
                - HP: %d/%d
                - Mana: %d/%d
                - Strength: %d, Intelligence: %d, Agility: %d

                Type 'help' to see available commands.
                """,
                opening,
                hero.getCurrentHealth(), hero.getMaxHealth(),
                hero.getCurrentMana(), hero.getMaxMana(),
                hero.getStrength(), hero.getIntelligence(), hero.getAgility()
        );
    }

    /**
     * Initialize a new game with streaming support for the opening scene
     */
    public void startNewGameStreaming(String heroName, String heroClass, StreamingResponseHandler handler) {
        gameState = new GameState();

        // Create hero
        Hero hero = Hero.createDefault(heroName, heroClass);

        // Select random main goal
        List<String> goals = gameConfig.initialGoals();
        String mainGoal = goals.get(random.nextInt(goals.size()));

        // Start the game
        gameState.startGame(hero, mainGoal);

        // Create initial NPCs
        NPC sage = npcService.createSampleNPC(gameState.getCurrentLocation());
        gameState.addNPC(sage);

        LOG.infof("New game started: %s the %s - Quest: %s", heroName, heroClass, mainGoal);

        // Generate opening scene with streaming
        worldService.generateOpeningSceneStreaming(gameState, new StreamingResponseHandler() {
            @Override
            public void onToken(String token) {
                handler.onToken(token);
            }

            @Override
            public void onComplete(String fullResponse) {
                // Append stats info after the opening scene
                String statsInfo = String.format("""


                        Your stats:
                        - HP: %d/%d
                        - Mana: %d/%d
                        - Strength: %d, Intelligence: %d, Agility: %d

                        Type 'help' to see available commands.
                        """,
                        hero.getCurrentHealth(), hero.getMaxHealth(),
                        hero.getCurrentMana(), hero.getMaxMana(),
                        hero.getStrength(), hero.getIntelligence(), hero.getAgility()
                );
                handler.onToken(statsInfo);
                handler.onComplete(fullResponse + statsInfo);
            }

            @Override
            public void onError(Throwable error) {
                handler.onError(error);
            }
        });
    }
    
    /**
     * Process player input/action with streaming support
     */
    public void processPlayerInputStreaming(String input, StreamingResponseHandler handler) {
        if (gameState == null || gameState.getStatus() == GameState.GameStatus.NOT_STARTED) {
            String message = "No active game. Please start a new game first.";
            handler.onToken(message);
            handler.onComplete(message);
            return;
        }
        
        if (gameState.getStatus() != GameState.GameStatus.IN_PROGRESS) {
            String message = "Game has ended. Start a new game to continue.";
            handler.onToken(message);
            handler.onComplete(message);
            return;
        }
        
        String trimmedInput = input.trim().toLowerCase();
        
        // Command handling - non-streaming commands return immediately
        if (trimmedInput.equals("help")) {
            String response = getHelpText();
            handler.onToken(response);
            handler.onComplete(response);
            return;
        } else if (trimmedInput.equals("stats")) {
            String response = getStatsDisplay();
            handler.onToken(response);
            handler.onComplete(response);
            return;
        } else if (trimmedInput.equals("quests")) {
            String response = getQuestsDisplay();
            handler.onToken(response);
            handler.onComplete(response);
            return;
        } else if (trimmedInput.equals("team")) {
            String response = getTeamDisplay();
            handler.onToken(response);
            handler.onComplete(response);
            return;
        } else if (trimmedInput.equals("location")) {
            worldService.describeLocationStreaming(gameState, handler);
            return;
        } else if (trimmedInput.startsWith("talk ")) {
            String response = handleTalkCommand(trimmedInput.substring(5));
            handler.onToken(response);
            handler.onComplete(response);
            return;
        } else if (trimmedInput.equals("combat test")) {
            String response = handleTestCombat();
            handler.onToken(response);
            handler.onComplete(response);
            return;
        } else if (gameState.isInCombat()) {
            String response = handleCombatInput(trimmedInput);
            handler.onToken(response);
            handler.onComplete(response);
            return;
        } else {
            // General action processing - use streaming
            worldService.processPlayerActionStreaming(gameState, input, handler);
        }
    }
    
    /**
     * Process player input/action
     */
    public String processPlayerInput(String input) {
        if (gameState == null || gameState.getStatus() == GameState.GameStatus.NOT_STARTED) {
            return "No active game. Please start a new game first.";
        }
        
        if (gameState.getStatus() != GameState.GameStatus.IN_PROGRESS) {
            return "Game has ended. Start a new game to continue.";
        }
        
        String trimmedInput = input.trim().toLowerCase();
        
        // Command handling
        if (trimmedInput.equals("help")) {
            return getHelpText();
        } else if (trimmedInput.equals("stats")) {
            return getStatsDisplay();
        } else if (trimmedInput.equals("quests")) {
            return getQuestsDisplay();
        } else if (trimmedInput.equals("team")) {
            return getTeamDisplay();
        } else if (trimmedInput.equals("location")) {
            return worldService.describeLocation(gameState);
        } else if (trimmedInput.startsWith("talk ")) {
            return handleTalkCommand(trimmedInput.substring(5));
        } else if (trimmedInput.equals("combat test")) {
            return handleTestCombat();
        } else if (gameState.isInCombat()) {
            return handleCombatInput(trimmedInput);
        } else {
            // General action processing
            return worldService.processPlayerAction(gameState, input);
        }
    }
    
    /**
     * Handle talking to NPCs
     */
    private String handleTalkCommand(String npcIdentifier) {
        // Find NPC (simplified - just get first NPC for demo)
        if (gameState.getWorldNPCs().isEmpty()) {
            return "There's no one here to talk to.";
        }
        
        NPC npc = gameState.getWorldNPCs().get(0);
        String dialogue = npcService.getNPCDialogue(npc, npcIdentifier, gameState);
        
        return String.format("%s: \"%s\"", npc.getName(), dialogue);
    }
    
    /**
     * Handle test combat command
     */
    private String handleTestCombat() {
        // Create test enemy
        NPC goblin = NPC.create(
                UUID.randomUUID().toString(),
                "Goblin Scout",
                "Warrior",
                gameState.getCurrentLocation(),
                "Defend goblin territory",
                "Aggressive, cunning",
                true
        );
        
        List<NPC> enemies = List.of(goblin);
        return combatService.startCombat(enemies, gameState);
    }
    
    /**
     * Handle combat-specific input
     */
    private String handleCombatInput(String input) {
        if (input.equals("attack") || input.equals("fight") || input.equals("next turn")) {
            return combatService.executeCombatRound(gameState);
        } else if (input.equals("flee")) {
            gameState.getCurrentCombat().endByFleeing();
            gameState.endCombat();
            return "You fled from combat!";
        } else {
            return "In combat. Use 'attack' to fight or 'flee' to escape.";
        }
    }
    
    /**
     * Get help text
     */
    private String getHelpText() {
        return """
                Available Commands:
                
                General:
                - help: Show this help
                - stats: View character stats
                - quests: View active quests
                - team: View team members
                - location: Describe current location
                - talk [message]: Talk to NPCs
                
                Combat:
                - attack: Execute combat round
                - flee: Run from combat
                
                Actions:
                - Type any action in natural language to interact with the world
                - Examples: "explore the forest", "search for clues", "rest at the inn"
                
                Debug:
                - combat test: Start a test combat encounter
                """;
    }
    
    /**
     * Get character stats display
     */
    private String getStatsDisplay() {
        Hero hero = gameState.getHero();
        return String.format("""
                === %s (Level %d %s) ===
                
                Health: %d/%d
                Mana: %d/%d
                
                Attributes:
                - Strength: %d (modifier: %+d)
                - Intelligence: %d (modifier: %+d)
                - Agility: %d (modifier: %+d)
                - Constitution: %d (modifier: %+d)
                - Charisma: %d (modifier: %+d)
                
                Combat Stats:
                - Defense: %d
                - Weapon Bonus: %+d
                - Armor Bonus: %+d
                
                Experience: %d XP
                """,
                hero.getName(), hero.getLevel(), hero.getCharacterClass(),
                hero.getCurrentHealth(), hero.getMaxHealth(),
                hero.getCurrentMana(), hero.getMaxMana(),
                hero.getStrength(), hero.getStrengthModifier(),
                hero.getIntelligence(), hero.getIntelligenceModifier(),
                hero.getAgility(), hero.getAgilityModifier(),
                hero.getConstitution(), hero.getConstitutionModifier(),
                hero.getCharisma(), hero.getCharismaModifier(),
                hero.getDefense(),
                hero.getWeaponBonus(),
                hero.getArmorBonus(),
                hero.getExperience()
        );
    }
    
    /**
     * Get quests display
     */
    private String getQuestsDisplay() {
        StringBuilder display = new StringBuilder("=== Quests ===\n\n");
        display.append(String.format("Main Quest: %s\n\n", gameState.getMainGoal()));
        
        List<Quest> activeQuests = gameState.getActiveQuests();
        if (activeQuests.isEmpty()) {
            display.append("No active side quests.");
        } else {
            display.append("Active Quests:\n");
            for (Quest quest : activeQuests) {
                display.append(String.format("- %s: %s\n", quest.getTitle(), quest.getDescription()));
            }
        }
        
        return display.toString();
    }
    
    /**
     * Get team display
     */
    private String getTeamDisplay() {
        List<TeamMember> team = gameState.getTeamMembers();
        
        if (team.isEmpty()) {
            return "You have no companions. You travel alone.";
        }
        
        StringBuilder display = new StringBuilder("=== Your Team ===\n\n");
        for (TeamMember member : team) {
            display.append(String.format("""
                    %s - %s
                    - HP: %d/%d
                    - Loyalty: %d/100
                    - Stats: STR %d, INT %d, AGI %d
                    
                    """,
                    member.getName(),
                    member.getCharacterClass(),
                    member.getCurrentHealth(),
                    member.getMaxHealth(),
                    member.getLoyalty(),
                    member.getStrength(),
                    member.getIntelligence(),
                    member.getAgility()
            ));
        }
        
        return display.toString();
    }
    
    /**
     * Get current game state (for UI)
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Save the current game
     *
     * @param saveName Display name for the save
     * @return Result of the save operation
     */
    public GamePersistenceService.SaveResult saveGame(String saveName) {
        if (gameState == null) {
            return new GamePersistenceService.SaveResult(false, "No active game to save", null);
        }

        if (gameState.isInCombat()) {
            return new GamePersistenceService.SaveResult(false, "Cannot save during combat", null);
        }

        // Save game state
        GamePersistenceService.SaveResult result = persistenceService.saveGame(gameState, saveName);

        // If successful, also save agent memories
        if (result.success() && result.saveId() != null) {
            Map<Object, List<ChatMessage>> memories = agentService.exportMemories();
            persistenceService.saveAgentMemories(result.saveId(), memories);
            LOG.infof("Game saved with %d agent memories", memories.size());
        }

        return result;
    }

    /**
     * Load a saved game
     *
     * @param saveId The ID of the save to load
     * @return true if loaded successfully
     */
    public boolean loadGame(Long saveId) {
        // Load game state
        GameState loadedState = persistenceService.loadGame(saveId);
        if (loadedState == null) {
            LOG.warnf("Failed to load game: %d", saveId);
            return false;
        }

        // Load and restore agent memories
        Map<Object, List<ChatMessage>> memories = persistenceService.loadAgentMemories(saveId);
        agentService.importMemories(memories);

        // Set the loaded state as current
        this.gameState = loadedState;

        LOG.infof("Game loaded successfully: %d with %d agent memories", saveId, memories.size());
        return true;
    }

    /**
     * Get list of available saves
     */
    public List<GamePersistenceService.SaveMetadata> listSaves() {
        return persistenceService.listSaves();
    }

    /**
     * Delete a saved game
     */
    public void deleteSave(Long saveId) {
        persistenceService.deleteSave(saveId);
    }

    /**
     * Check if game is in combat (for UI to disable save button)
     */
    public boolean isInCombat() {
        return gameState != null && gameState.isInCombat();
    }
}
