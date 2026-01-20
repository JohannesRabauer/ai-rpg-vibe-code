package com.airpg.services;

import com.airpg.agents.AgentService;
import com.airpg.agents.CombatNarratorAgent;
import com.airpg.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Service for managing combat encounters.
 * Implements D&D-inspired stat-based combat with AI narration.
 */
@ApplicationScoped
public class CombatService {
    
    private static final Logger LOG = Logger.getLogger(CombatService.class);
    private final Random random = new Random();
    
    @Inject
    AgentService agentService;
    
    @Inject
    TeamService teamService;
    
    /**
     * Start a combat encounter
     */
    public String startCombat(List<NPC> enemies, GameState gameState) {
        // Build party (hero + team members)
        List<GameCharacter> party = new ArrayList<>();
        party.add(gameState.getHero());
        party.addAll(gameState.getTeamMembers());
        
        CombatEncounter combat = CombatEncounter.builder()
                .id(UUID.randomUUID().toString())
                .playerParty(party)
                .enemies(enemies)
                .status(CombatEncounter.CombatStatus.IN_PROGRESS)
                .currentTurn(1)
                .location(gameState.getCurrentLocation())
                .build();
        
        gameState.startCombat(combat);
        
        // Generate opening narration
        CombatNarratorAgent narrator = agentService.getCombatNarrator();
        
        String context = String.format("""
                Combat begins!
                Location: %s
                Party: %s (level %d) + %d companions
                Enemies: %s
                
                Set the scene for this battle.
                """,
                combat.getLocation(),
                gameState.getHero().getName(),
                gameState.getHero().getLevel(),
                gameState.getTeamMembers().size(),
                enemies.stream().map(NPC::getName).toList()
        );
        
        String narration = narrator.narrateCombatStart(context);
        LOG.infof("Combat started against %d enemies", enemies.size());
        return narration;
    }
    
    /**
     * Execute one round of combat with all party members and enemies acting
     */
    public String executeCombatRound(GameState gameState) {
        CombatEncounter combat = gameState.getCurrentCombat();
        if (combat == null || !combat.isActive()) {
            return "No active combat.";
        }
        
        StringBuilder roundNarration = new StringBuilder();
        roundNarration.append(String.format("--- Turn %d ---\n", combat.getCurrentTurn()));
        
        // Party actions (player party acts first based on initiative - simplified)
        for (GameCharacter character : combat.getPlayerParty()) {
            if (!character.isAlive()) continue;
            
            String actionNarration = executeCharacterAction(character, combat, true);
            roundNarration.append(actionNarration).append("\n");
        }
        
        // Enemy actions
        for (NPC enemy : combat.getEnemies()) {
            if (!enemy.isAlive()) continue;
            
            String actionNarration = executeCharacterAction(enemy, combat, false);
            roundNarration.append(actionNarration).append("\n");
        }
        
        // Check for combat end
        if (combat.areAllEnemiesDefeated()) {
            combat.endWithVictory();
            roundNarration.append("\n").append(endCombat(gameState, true));
        } else if (combat.isPartyDefeated()) {
            combat.endWithDefeat();
            roundNarration.append("\n").append(endCombat(gameState, false));
        } else {
            combat.nextTurn();
        }
        
        return roundNarration.toString();
    }
    
    /**
     * Execute a single character's combat action
     */
    private String executeCharacterAction(GameCharacter character, CombatEncounter combat, boolean isPlayerSide) {
        // Determine action (simplified - always attack for enemies, companions decide autonomously)
        CombatAction action;
        
        if (character instanceof TeamMember companion) {
            // Companion decides autonomously
            action = executeCompanionAction(companion, combat);
        } else if (character instanceof Hero) {
            // Hero attacks strongest enemy (simplified)
            action = executeAttack(character, getStrongestEnemy(combat), combat);
        } else {
            // Enemy attacks random party member
            action = executeAttack(character, getRandomTarget(combat.getPlayerParty()), combat);
        }
        
        // Narrate the action
        return narrateAction(action);
    }
    
    /**
     * Execute companion's autonomous action
     */
    private CombatAction executeCompanionAction(TeamMember companion, CombatEncounter combat) {
        // Build combat context for companion
        String context = buildCombatContext(combat);
        
        // Get companion's decision
        String decision = teamService.getCompanionCombatDecision(companion, context);
        
        // Parse decision (format: "ACTION: [ATTACK/HEAL/DEFEND] | TARGET: [name] | REASON: [reason]")
        return parseAndExecuteCompanionDecision(companion, decision, combat);
    }
    
    /**
     * Parse companion decision and execute it
     */
    private CombatAction parseAndExecuteCompanionDecision(TeamMember companion, 
                                                           String decision, 
                                                           CombatEncounter combat) {
        try {
            String actionType = extractValue(decision, "ACTION");
            String targetName = extractValue(decision, "TARGET");
            
            return switch (actionType.toUpperCase()) {
                case "HEAL" -> {
                    GameCharacter target = findCharacterByName(targetName, combat.getPlayerParty());
                    yield executeHeal(companion, target != null ? target : companion);
                }
                case "DEFEND" -> {
                    // Simplified: defending gives bonus to armor for next turn
                    companion.setArmorBonus(companion.getArmorBonus() + 2);
                    yield CombatAction.builder()
                            .attacker(companion)
                            .target(companion)
                            .actionType(CombatAction.ActionType.DEFEND)
                            .build();
                }
                default -> { // ATTACK or default
                    GameCharacter target = findCharacterByName(targetName, combat.getEnemies());
                    yield executeAttack(companion, target != null ? target : getRandomTarget(combat.getEnemies()), combat);
                }
            };
        } catch (Exception e) {
            LOG.errorf("Failed to parse companion decision: %s", decision);
            // Fallback: attack random enemy
            return executeAttack(companion, getRandomTarget(combat.getEnemies()), combat);
        }
    }
    
    /**
     * Execute a melee or magic attack
     */
    private CombatAction executeAttack(GameCharacter attacker, GameCharacter target, CombatEncounter combat) {
        if (target == null || !target.isAlive()) {
            target = getRandomTarget(combat.getEnemies());
        }
        
        // Determine if magic attack (high intelligence) or melee
        boolean isMagicAttack = attacker.getIntelligence() > attacker.getStrength();
        
        if (isMagicAttack && attacker.getCurrentMana() >= 10) {
            return executeMagicAttack(attacker, target);
        } else {
            return executeMeleeAttack(attacker, target);
        }
    }
    
    /**
     * Execute melee attack (d20 + modifier vs defense)
     */
    private CombatAction executeMeleeAttack(GameCharacter attacker, GameCharacter target) {
        int attackRoll = rollD20();
        int attackTotal = attackRoll + attacker.getStrengthModifier();
        int defense = target.getDefense();
        
        boolean isHit = attackTotal >= defense;
        int damage = 0;
        
        if (isHit) {
            // Damage: strength modifier + weapon bonus + 1d6
            damage = Math.max(1, attacker.getStrengthModifier() + attacker.getWeaponBonus() + rollD6());
            target.takeDamage(damage);
        }
        
        return CombatAction.meleeAttack(attacker, target, attackRoll, damage, isHit);
    }
    
    /**
     * Execute magic attack
     */
    private CombatAction executeMagicAttack(GameCharacter attacker, GameCharacter target) {
        attacker.useMana(10);
        
        // Magic attacks auto-hit but can be resisted
        int damage = Math.max(1, attacker.getIntelligenceModifier() + rollD8());
        
        // Target can resist with intelligence
        if (rollD20() + target.getIntelligenceModifier() > 15) {
            damage = damage / 2; // Resisted, half damage
        }
        
        target.takeDamage(damage);
        return CombatAction.magicAttack(attacker, target, damage, true);
    }
    
    /**
     * Execute healing action
     */
    private CombatAction executeHeal(GameCharacter healer, GameCharacter target) {
        if (healer.useMana(15)) {
            int healing = healer.getIntelligenceModifier() + rollD8();
            target.heal(healing);
            return CombatAction.heal(healer, target, healing);
        } else {
            // Not enough mana, fallback to attack
            return executeMeleeAttack(healer, getRandomTarget(List.of(target)));
        }
    }
    
    /**
     * Narrate a combat action
     */
    private String narrateAction(CombatAction action) {
        CombatNarratorAgent narrator = agentService.getCombatNarrator();
        
        String context = String.format("""
                Attacker: %s
                Target: %s
                Action: %s
                Hit: %s
                Damage dealt: %d
                Healing done: %d
                Attack roll: %d
                Target defense: %d
                Target remaining HP: %d/%d
                
                Narrate this combat action vividly.
                """,
                action.getAttacker().getName(),
                action.getTarget().getName(),
                action.getActionType(),
                action.isHit(),
                action.getDamageDealt(),
                action.getHealingDone(),
                action.getAttackRoll(),
                action.getDefenseValue(),
                action.getTarget().getCurrentHealth(),
                action.getTarget().getMaxHealth()
        );
        
        return narrator.narrateCombatAction(context);
    }
    
    /**
     * End combat and generate conclusion narration
     */
    private String endCombat(GameState gameState, boolean victory) {
        CombatEncounter combat = gameState.getCurrentCombat();
        CombatNarratorAgent narrator = agentService.getCombatNarrator();
        
        String context = String.format("""
                Combat ended with %s!
                Hero: %s (HP: %d/%d)
                Party survivors: %d
                
                Narrate the outcome.
                """,
                victory ? "VICTORY" : "DEFEAT",
                gameState.getHero().getName(),
                gameState.getHero().getCurrentHealth(),
                gameState.getHero().getMaxHealth(),
                gameState.getLivingPartyMembers().size()
        );
        
        String narration = narrator.narrateCombatEnd(context);
        
        if (victory) {
            // Award experience
            gameState.getHero().gainExperience(50 * combat.getEnemies().size());
        } else {
            gameState.gameOver();
        }
        
        gameState.endCombat();
        LOG.infof("Combat ended with %s", victory ? "victory" : "defeat");
        return narration;
    }
    
    // Utility methods
    
    private String buildCombatContext(CombatEncounter combat) {
        StringBuilder context = new StringBuilder("Combat situation:\n");
        context.append("Allies:\n");
        for (GameCharacter ally : combat.getPlayerParty()) {
            if (ally.isAlive()) {
                context.append(String.format("- %s (HP: %d/%d)\n", 
                    ally.getName(), ally.getCurrentHealth(), ally.getMaxHealth()));
            }
        }
        context.append("Enemies:\n");
        for (NPC enemy : combat.getEnemies()) {
            if (enemy.isAlive()) {
                context.append(String.format("- %s (HP: %d/%d)\n", 
                    enemy.getName(), enemy.getCurrentHealth(), enemy.getMaxHealth()));
            }
        }
        return context.toString();
    }
    
    private String extractValue(String text, String key) {
        int start = text.indexOf(key + ":") + key.length() + 1;
        int end = text.indexOf("|", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }
    
    private GameCharacter findCharacterByName(String name, List<? extends GameCharacter> characters) {
        return characters.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }
    
    private GameCharacter getRandomTarget(List<? extends GameCharacter> targets) {
        List<? extends GameCharacter> alive = targets.stream().filter(GameCharacter::isAlive).toList();
        return alive.isEmpty() ? null : alive.get(random.nextInt(alive.size()));
    }
    
    private GameCharacter getStrongestEnemy(CombatEncounter combat) {
        return combat.getEnemies().stream()
                .filter(GameCharacter::isAlive)
                .max((e1, e2) -> Integer.compare(e1.getCurrentHealth(), e2.getCurrentHealth()))
                .orElse(null);
    }
    
    private int rollD20() {
        return random.nextInt(20) + 1;
    }
    
    private int rollD8() {
        return random.nextInt(8) + 1;
    }
    
    private int rollD6() {
        return random.nextInt(6) + 1;
    }
}
