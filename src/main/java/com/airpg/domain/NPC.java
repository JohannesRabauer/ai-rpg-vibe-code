package com.airpg.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents an NPC (Non-Player Character) in the game world.
 * Each NPC is controlled by an AI agent and has its own agenda.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class NPC extends GameCharacter {
    
    private String id; // Unique identifier for the NPC
    private String role; // e.g., "Merchant", "Guard", "Wizard", "Villain"
    private String location; // Current location in the world
    private String agenda; // NPC's personal goal or motivation
    private String personality; // Brief personality description for AI agent
    private boolean isHostile; // Whether the NPC is initially hostile
    private boolean isQuestGiver; // Whether this NPC can give quests
    
    /**
     * Create a new NPC with specified attributes
     */
    public static NPC create(String id, String name, String role, String location, 
                             String agenda, String personality, boolean isHostile) {
        NPC npc = NPC.builder()
                .id(id)
                .name(name)
                .role(role)
                .location(location)
                .agenda(agenda)
                .personality(personality)
                .isHostile(isHostile)
                .isQuestGiver(!isHostile) // Non-hostile NPCs can give quests by default
                .armorBonus(0)
                .weaponBonus(0)
                .build();
        
        // Set role-based attributes
        switch (role.toLowerCase()) {
            case "guard":
            case "warrior":
                npc.setStrength(14);
                npc.setIntelligence(10);
                npc.setAgility(12);
                npc.setConstitution(14);
                npc.setCharisma(10);
                npc.setWeaponBonus(1);
                npc.setArmorBonus(2);
                break;
            case "wizard":
            case "mage":
                npc.setStrength(8);
                npc.setIntelligence(16);
                npc.setAgility(10);
                npc.setConstitution(10);
                npc.setCharisma(12);
                break;
            case "merchant":
                npc.setStrength(10);
                npc.setIntelligence(12);
                npc.setAgility(10);
                npc.setConstitution(10);
                npc.setCharisma(16);
                break;
            default:
                npc.setStrength(10);
                npc.setIntelligence(10);
                npc.setAgility(10);
                npc.setConstitution(10);
                npc.setCharisma(10);
        }
        
        npc.initializeDerivedStats();
        return npc;
    }
}
