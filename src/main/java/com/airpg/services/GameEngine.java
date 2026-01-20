package com.airpg.services;

import com.airpg.config.GameConfig;
import com.airpg.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
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
     * Process player input/action with streaming support
     */
    public void processPlayerInputStreaming(String input, StreamingResponseHandler handler) {
        if (gameState == null || gameState.getStatus() == GameState.GameStatus.NOT_STARTED) {
            handler.onToken("No active game. Please start a new game first.");
            return;
        }
        
        if (gameState.getStatus() != GameState.GameStatus.IN_PROGRESS) {
            handler.onToken("Game has ended. Start a new game to continue.");
            return;
        }
        
        String trimmedInput = input.trim().toLowerCase();
        
        // Command handling - non-streaming commands return immediately
        if (trimmedInput.equals("help")) {
            handler.onToken(getHelpText());
            return;
        } else if (trimmedInput.equals("stats")) {
            handler.onToken(getStatsDisplay());
            return;
        } else if (trimmedInput.equals("quests")) {
            handler.onToken(getQuestsDisplay());
            return;
        } else if (trimmedInput.equals("team")) {
            handler.onToken(getTeamDisplay());
            return;
        } else if (trimmedInput.equals("location")) {
            worldService.describeLocationStreaming(gameState, handler);
            return;
        } else if (trimmedInput.startsWith("talk ")) {
            handler.onToken(handleTalkCommand(trimmedInput.substring(5)));
            return;
        } else if (trimmedInput.equals("combat test")) {
            handler.onToken(handleTestCombat());
            return;
        } else if (gameState.isInCombat()) {
            handler.onToken(handleCombatInput(trimmedInput));
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
}
