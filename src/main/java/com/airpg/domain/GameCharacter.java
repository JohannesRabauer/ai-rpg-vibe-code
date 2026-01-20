package com.airpg.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all characters in the game (Hero, NPCs, Team Members).
 * Uses D&D-inspired attribute system with derived stats.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class GameCharacter {
    
    private String name;
    
    // Core attributes (3-18 range, D&D style)
    private int strength;
    private int intelligence;
    private int agility;
    private int constitution;
    private int charisma;
    
    // Derived stats
    private int currentHealth;
    private int maxHealth;
    private int currentMana;
    private int maxMana;
    
    // Combat stats
    private int armorBonus;
    private int weaponBonus;
    
    /**
     * Calculate modifier for an attribute (D&D formula: (attribute - 10) / 2)
     */
    public int getModifier(int attribute) {
        return (attribute - 10) / 2;
    }
    
    public int getStrengthModifier() {
        return getModifier(strength);
    }
    
    public int getIntelligenceModifier() {
        return getModifier(intelligence);
    }
    
    public int getAgilityModifier() {
        return getModifier(agility);
    }
    
    public int getConstitutionModifier() {
        return getModifier(constitution);
    }
    
    public int getCharismaModifier() {
        return getModifier(charisma);
    }
    
    /**
     * Calculate defense value: 10 + agility modifier + armor bonus
     */
    public int getDefense() {
        return 10 + getAgilityModifier() + armorBonus;
    }
    
    /**
     * Initialize derived stats based on core attributes
     */
    public void initializeDerivedStats() {
        this.maxHealth = constitution * 10;
        this.currentHealth = maxHealth;
        this.maxMana = intelligence * 5;
        this.currentMana = maxMana;
    }
    
    /**
     * Check if character is alive
     */
    public boolean isAlive() {
        return currentHealth > 0;
    }
    
    /**
     * Take damage and reduce health
     */
    public void takeDamage(int damage) {
        currentHealth = Math.max(0, currentHealth - damage);
    }
    
    /**
     * Heal the character
     */
    public void heal(int amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }
    
    /**
     * Use mana for spells/abilities
     */
    public boolean useMana(int amount) {
        if (currentMana >= amount) {
            currentMana -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * Restore mana
     */
    public void restoreMana(int amount) {
        currentMana = Math.min(maxMana, currentMana + amount);
    }
}
