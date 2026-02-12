package com.airpg.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents a team member (companion) who travels with the hero.
 * Team members are AI-controlled and act autonomously in combat and conversations.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class TeamMember extends GameCharacter {
    
    private String id; // Unique identifier
    private String characterClass; // e.g., "Warrior", "Healer", "Archer"
    private String personality; // Personality description for AI agent
    private String backstory; // Brief backstory
    private int loyalty; // 0-100, affects willingness to follow orders and stay with party
    
    /**
     * Create a new team member
     */
    public static TeamMember create(String id, String name, String characterClass, 
                                     String personality, String backstory) {
        TeamMember member = TeamMember.builder()
                .id(id)
                .name(name)
                .characterClass(characterClass)
                .personality(personality)
                .backstory(backstory)
                .loyalty(50) // Start at neutral loyalty
                .armorBonus(0)
                .weaponBonus(0)
                .build();
        
        // Set class-based attributes
        switch (characterClass.toLowerCase()) {
            case "warrior":
            case "fighter":
                member.setStrength(15);
                member.setIntelligence(10);
                member.setAgility(12);
                member.setConstitution(14);
                member.setCharisma(10);
                member.setWeaponBonus(2);
                member.setArmorBonus(1);
                break;
            case "mage":
            case "wizard":
                member.setStrength(8);
                member.setIntelligence(16);
                member.setAgility(10);
                member.setConstitution(10);
                member.setCharisma(12);
                break;
            case "healer":
            case "cleric":
                member.setStrength(10);
                member.setIntelligence(14);
                member.setAgility(10);
                member.setConstitution(12);
                member.setCharisma(14);
                break;
            case "rogue":
            case "archer":
                member.setStrength(10);
                member.setIntelligence(12);
                member.setAgility(16);
                member.setConstitution(10);
                member.setCharisma(12);
                break;
            case "bard":
                member.setStrength(10);
                member.setIntelligence(14);
                member.setAgility(12);
                member.setConstitution(12);
                member.setCharisma(16);
                break;
            default:
                member.setStrength(12);
                member.setIntelligence(12);
                member.setAgility(12);
                member.setConstitution(12);
                member.setCharisma(12);
        }
        
        member.initializeDerivedStats();
        return member;
    }
    
    /**
     * Adjust loyalty based on events
     */
    public void adjustLoyalty(int amount) {
        loyalty = Math.max(0, Math.min(100, loyalty + amount));
    }
    
    /**
     * Check if the team member might leave the party (low loyalty)
     */
    public boolean isLikelyToLeave() {
        return loyalty < 20;
    }
}
