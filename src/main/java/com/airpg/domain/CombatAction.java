package com.airpg.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of a single combat action.
 * Used to communicate combat outcomes to the AI narrator and UI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombatAction {
    
    private GameCharacter attacker;
    private GameCharacter target;
    private ActionType actionType;
    private int damageDealt;
    private int healingDone;
    private boolean isHit;
    private boolean isCritical;
    private int attackRoll;
    private int defenseValue;
    
    public enum ActionType {
        MELEE_ATTACK,
        MAGIC_ATTACK,
        HEAL,
        DEFEND,
        FLEE
    }
    
    /**
     * Create a melee attack action result
     */
    public static CombatAction meleeAttack(GameCharacter attacker, GameCharacter target, 
                                            int attackRoll, int damageDealt, boolean isHit) {
        return CombatAction.builder()
                .attacker(attacker)
                .target(target)
                .actionType(ActionType.MELEE_ATTACK)
                .attackRoll(attackRoll)
                .defenseValue(target.getDefense())
                .isHit(isHit)
                .damageDealt(damageDealt)
                .build();
    }
    
    /**
     * Create a magic attack action result
     */
    public static CombatAction magicAttack(GameCharacter attacker, GameCharacter target, 
                                            int damageDealt, boolean isHit) {
        return CombatAction.builder()
                .attacker(attacker)
                .target(target)
                .actionType(ActionType.MAGIC_ATTACK)
                .isHit(isHit)
                .damageDealt(damageDealt)
                .build();
    }
    
    /**
     * Create a healing action result
     */
    public static CombatAction heal(GameCharacter healer, GameCharacter target, int healingDone) {
        return CombatAction.builder()
                .attacker(healer)
                .target(target)
                .actionType(ActionType.HEAL)
                .healingDone(healingDone)
                .build();
    }
}
