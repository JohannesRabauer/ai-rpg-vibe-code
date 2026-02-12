package com.airpg.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents the player's hero character.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Hero extends GameCharacter {
    
    private int experience;
    private int level;
    private String characterClass; // e.g., "Warrior", "Mage", "Rogue"
    
    /**
     * Create a new hero with default starting stats
     */
    public static Hero createDefault(String name, String characterClass) {
        Hero hero = Hero.builder()
                .name(name)
                .characterClass(characterClass)
                .level(1)
                .experience(0)
                .armorBonus(0)
                .weaponBonus(0)
                .build();
        
        // Set class-based starting attributes
        switch (characterClass.toLowerCase()) {
            case "warrior":
                hero.setStrength(16);
                hero.setIntelligence(10);
                hero.setAgility(12);
                hero.setConstitution(14);
                hero.setCharisma(10);
                hero.setWeaponBonus(2);
                break;
            case "mage":
                hero.setStrength(8);
                hero.setIntelligence(16);
                hero.setAgility(10);
                hero.setConstitution(10);
                hero.setCharisma(14);
                break;
            case "rogue":
                hero.setStrength(10);
                hero.setIntelligence(12);
                hero.setAgility(16);
                hero.setConstitution(10);
                hero.setCharisma(14);
                break;
            case "bard":
                hero.setStrength(10);
                hero.setIntelligence(14);
                hero.setAgility(12);
                hero.setConstitution(12);
                hero.setCharisma(16);
                break;
            default:
                // Balanced default
                hero.setStrength(12);
                hero.setIntelligence(12);
                hero.setAgility(12);
                hero.setConstitution(12);
                hero.setCharisma(12);
        }
        
        hero.initializeDerivedStats();
        return hero;
    }
    
    /**
     * Gain experience points
     */
    public void gainExperience(int amount) {
        experience += amount;
        checkLevelUp();
    }
    
    /**
     * Check if hero should level up (simple formula: 100 XP per level)
     */
    private void checkLevelUp() {
        int requiredXp = level * 100;
        if (experience >= requiredXp) {
            levelUp();
        }
    }
    
    /**
     * Level up the hero
     */
    private void levelUp() {
        level++;
        // Increase stats slightly on level up
        setMaxHealth(getMaxHealth() + 10);
        setCurrentHealth(getMaxHealth());
        setMaxMana(getMaxMana() + 5);
        setCurrentMana(getMaxMana());
    }
}
