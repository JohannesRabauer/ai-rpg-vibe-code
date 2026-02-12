package com.airpg.services.impl;

import com.airpg.config.PersistenceConfig;
import com.airpg.domain.*;
import com.airpg.services.GamePersistenceService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import java.time.LocalDateTime;
import java.util.*;

import static org.jooq.impl.DSL.*;

/**
 * jOOQ-based implementation of GamePersistenceService.
 * Handles save/load operations for game state with transactional support.
 */
@ApplicationScoped
public class GamePersistenceServiceImpl implements GamePersistenceService {

    private static final Logger LOG = Logger.getLogger(GamePersistenceServiceImpl.class);

    @Inject
    DSLContext dsl;

    @Inject
    PersistenceConfig config;

    @Override
    public SaveResult saveGame(GameState state, String saveName) {
        if (!config.enabled()) {
            return new SaveResult(false, "Persistence is disabled", null);
        }

        if (state == null) {
            return new SaveResult(false, "No game state to save", null);
        }

        if (state.isInCombat()) {
            return new SaveResult(false, "Cannot save during combat", null);
        }

        try {
            return dsl.transactionResult(ctx -> {
                DSLContext txDsl = ctx.dsl();

                // Check if save with this gameId already exists
                Record existingRecord = txDsl.select()
                        .from(table("game_saves"))
                        .where(field("game_id").eq(state.getGameId()))
                        .fetchOne();

                Long saveId;
                if (existingRecord != null) {
                    // Update existing save
                    saveId = existingRecord.get("ID", Long.class);
                    txDsl.update(table("game_saves"))
                            .set(field("save_name"), saveName)
                            .set(field("hero_name"), state.getHero().getName())
                            .set(field("hero_class"), state.getHero().getCharacterClass())
                            .set(field("hero_level"), state.getHero().getLevel())
                            .set(field("current_location"), state.getCurrentLocation())
                            .set(field("main_goal"), state.getMainGoal())
                            .set(field("game_status"), state.getStatus().name())
                            .set(field("updated_at"), LocalDateTime.now())
                            .where(field("id").eq(saveId))
                            .execute();

                    // Delete old related records (cascade would handle this, but explicit is clearer)
                    deleteRelatedRecords(txDsl, saveId);
                } else {
                    // Insert new save
                    saveId = txDsl.insertInto(table("game_saves"))
                            .columns(
                                    field("game_id"),
                                    field("save_name"),
                                    field("hero_name"),
                                    field("hero_class"),
                                    field("hero_level"),
                                    field("current_location"),
                                    field("main_goal"),
                                    field("game_status"),
                                    field("created_at"),
                                    field("updated_at")
                            )
                            .values(
                                    state.getGameId(),
                                    saveName,
                                    state.getHero().getName(),
                                    state.getHero().getCharacterClass(),
                                    state.getHero().getLevel(),
                                    state.getCurrentLocation(),
                                    state.getMainGoal(),
                                    state.getStatus().name(),
                                    LocalDateTime.now(),
                                    LocalDateTime.now()
                            )
                            .returning(field("id", Long.class))
                            .fetchOne()
                            .get("ID", Long.class);
                }

                // Save hero
                saveHero(txDsl, saveId, state.getHero());

                // Save team members
                for (TeamMember member : state.getTeamMembers()) {
                    saveTeamMember(txDsl, saveId, member);
                }

                // Save NPCs
                for (NPC npc : state.getWorldNPCs()) {
                    saveNpc(txDsl, saveId, npc);
                }

                // Save quests
                for (Quest quest : state.getQuests()) {
                    saveQuest(txDsl, saveId, quest);
                }

                // Save story history
                List<String> history = state.getStoryHistory();
                for (int i = 0; i < history.size(); i++) {
                    saveStoryEvent(txDsl, saveId, i, history.get(i));
                }

                LOG.infof("Game saved successfully: %s (ID: %d)", saveName, saveId);
                return new SaveResult(true, "Game saved successfully", saveId);
            });
        } catch (Exception e) {
            LOG.errorf(e, "Failed to save game: %s", saveName);
            return new SaveResult(false, "Failed to save: " + e.getMessage(), null);
        }
    }

    @Override
    public GameState loadGame(Long saveId) {
        if (!config.enabled()) {
            return null;
        }

        try {
            // Load main save record
            Record saveRecord = dsl.select()
                    .from(table("game_saves"))
                    .where(field("id").eq(saveId))
                    .fetchOne();

            if (saveRecord == null) {
                LOG.warnf("Save not found: %d", saveId);
                return null;
            }

            GameState state = new GameState();

            // Restore basic fields - need to use reflection or setter since gameId is set in constructor
            // For simplicity, we'll create a new state and set values
            setGameId(state, saveRecord.get("GAME_ID", String.class));
            state.setCurrentLocation(saveRecord.get("CURRENT_LOCATION", String.class));
            state.setMainGoal(saveRecord.get("MAIN_GOAL", String.class));
            state.setStatus(GameState.GameStatus.valueOf(saveRecord.get("GAME_STATUS", String.class)));

            // Load hero
            Hero hero = loadHero(saveId);
            state.setHero(hero);

            // Load team members
            List<TeamMember> teamMembers = loadTeamMembers(saveId);
            state.setTeamMembers(teamMembers);

            // Load NPCs
            List<NPC> npcs = loadNpcs(saveId);
            state.setWorldNPCs(npcs);

            // Load quests
            List<Quest> quests = loadQuests(saveId);
            state.setQuests(quests);

            // Load story history
            List<String> history = loadStoryHistory(saveId);
            state.setStoryHistory(history);

            LOG.infof("Game loaded successfully: %d", saveId);
            return state;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to load game: %d", saveId);
            return null;
        }
    }

    @Override
    public List<SaveMetadata> listSaves() {
        if (!config.enabled()) {
            return Collections.emptyList();
        }

        try {
            Result<Record> records = dsl.select()
                    .from(table("game_saves"))
                    .orderBy(field("updated_at").desc())
                    .fetch();

            List<SaveMetadata> saves = new ArrayList<>();
            for (Record record : records) {
                saves.add(new SaveMetadata(
                        record.get("ID", Long.class),
                        record.get("GAME_ID", String.class),
                        record.get("SAVE_NAME", String.class),
                        record.get("HERO_NAME", String.class),
                        record.get("HERO_CLASS", String.class),
                        record.get("HERO_LEVEL", Integer.class),
                        record.get("CURRENT_LOCATION", String.class),
                        record.get("CREATED_AT", LocalDateTime.class),
                        record.get("UPDATED_AT", LocalDateTime.class)
                ));
            }
            return saves;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to list saves");
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteSave(Long saveId) {
        if (!config.enabled()) {
            return;
        }

        try {
            int deleted = dsl.deleteFrom(table("game_saves"))
                    .where(field("id").eq(saveId))
                    .execute();

            if (deleted > 0) {
                LOG.infof("Save deleted: %d", saveId);
            } else {
                LOG.warnf("Save not found for deletion: %d", saveId);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to delete save: %d", saveId);
        }
    }

    @Override
    public void saveAgentMemories(Long gameSaveId, Map<Object, List<ChatMessage>> memories) {
        if (!config.enabled() || memories == null || memories.isEmpty()) {
            return;
        }

        try {
            // Delete existing memories for this save
            dsl.deleteFrom(table("agent_memories"))
                    .where(field("game_save_id").eq(gameSaveId))
                    .execute();

            // Insert new memories
            for (Map.Entry<Object, List<ChatMessage>> entry : memories.entrySet()) {
                String memoryId = entry.getKey().toString();
                List<ChatMessage> messages = entry.getValue();

                for (int i = 0; i < messages.size(); i++) {
                    ChatMessage message = messages.get(i);
                    String messageType = getMessageType(message);
                    String content = getMessageContent(message);

                    dsl.insertInto(table("agent_memories"))
                            .columns(
                                    field("game_save_id"),
                                    field("memory_id"),
                                    field("message_index"),
                                    field("message_type"),
                                    field("content")
                            )
                            .values(gameSaveId, memoryId, i, messageType, content)
                            .execute();
                }
            }

            LOG.infof("Saved %d agent memories for save %d", memories.size(), gameSaveId);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to save agent memories for save: %d", gameSaveId);
        }
    }

    @Override
    public Map<Object, List<ChatMessage>> loadAgentMemories(Long gameSaveId) {
        if (!config.enabled()) {
            return Collections.emptyMap();
        }

        try {
            Result<Record> records = dsl.select()
                    .from(table("agent_memories"))
                    .where(field("game_save_id").eq(gameSaveId))
                    .orderBy(field("memory_id"), field("message_index"))
                    .fetch();

            Map<Object, List<ChatMessage>> memories = new HashMap<>();
            for (Record record : records) {
                String memoryId = record.get("MEMORY_ID", String.class);
                String messageType = record.get("MESSAGE_TYPE", String.class);
                String content = record.get("CONTENT", String.class);

                ChatMessage message = createChatMessage(messageType, content);
                memories.computeIfAbsent(memoryId, k -> new ArrayList<>()).add(message);
            }

            LOG.infof("Loaded %d agent memories for save %d", memories.size(), gameSaveId);
            return memories;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to load agent memories for save: %d", gameSaveId);
            return Collections.emptyMap();
        }
    }

    // === Private Helper Methods ===

    private void deleteRelatedRecords(DSLContext txDsl, Long saveId) {
        txDsl.deleteFrom(table("heroes")).where(field("game_save_id").eq(saveId)).execute();
        txDsl.deleteFrom(table("team_members")).where(field("game_save_id").eq(saveId)).execute();
        txDsl.deleteFrom(table("npcs")).where(field("game_save_id").eq(saveId)).execute();
        txDsl.deleteFrom(table("quests")).where(field("game_save_id").eq(saveId)).execute();
        txDsl.deleteFrom(table("story_history")).where(field("game_save_id").eq(saveId)).execute();
        txDsl.deleteFrom(table("agent_memories")).where(field("game_save_id").eq(saveId)).execute();
    }

    private void saveHero(DSLContext txDsl, Long saveId, Hero hero) {
        txDsl.insertInto(table("heroes"))
                .columns(
                        field("game_save_id"), field("name"), field("character_class"),
                        field("level"), field("experience"),
                        field("strength"), field("intelligence"), field("agility"),
                        field("constitution"), field("charisma"),
                        field("current_health"), field("max_health"),
                        field("current_mana"), field("max_mana"),
                        field("armor_bonus"), field("weapon_bonus")
                )
                .values(
                        saveId, hero.getName(), hero.getCharacterClass(),
                        hero.getLevel(), hero.getExperience(),
                        hero.getStrength(), hero.getIntelligence(), hero.getAgility(),
                        hero.getConstitution(), hero.getCharisma(),
                        hero.getCurrentHealth(), hero.getMaxHealth(),
                        hero.getCurrentMana(), hero.getMaxMana(),
                        hero.getArmorBonus(), hero.getWeaponBonus()
                )
                .execute();
    }

    private Hero loadHero(Long saveId) {
        Record record = dsl.select()
                .from(table("heroes"))
                .where(field("game_save_id").eq(saveId))
                .fetchOne();

        if (record == null) {
            return null;
        }

        Hero hero = Hero.builder()
                .name(record.get("NAME", String.class))
                .characterClass(record.get("CHARACTER_CLASS", String.class))
                .level(record.get("LEVEL", Integer.class))
                .experience(record.get("EXPERIENCE", Integer.class))
                .strength(record.get("STRENGTH", Integer.class))
                .intelligence(record.get("INTELLIGENCE", Integer.class))
                .agility(record.get("AGILITY", Integer.class))
                .constitution(record.get("CONSTITUTION", Integer.class))
                .charisma(record.get("CHARISMA", Integer.class))
                .currentHealth(record.get("CURRENT_HEALTH", Integer.class))
                .maxHealth(record.get("MAX_HEALTH", Integer.class))
                .currentMana(record.get("CURRENT_MANA", Integer.class))
                .maxMana(record.get("MAX_MANA", Integer.class))
                .armorBonus(record.get("ARMOR_BONUS", Integer.class))
                .weaponBonus(record.get("WEAPON_BONUS", Integer.class))
                .build();

        return hero;
    }

    private void saveTeamMember(DSLContext txDsl, Long saveId, TeamMember member) {
        txDsl.insertInto(table("team_members"))
                .columns(
                        field("game_save_id"), field("member_id"), field("name"),
                        field("character_class"), field("personality"), field("backstory"),
                        field("loyalty"),
                        field("strength"), field("intelligence"), field("agility"),
                        field("constitution"), field("charisma"),
                        field("current_health"), field("max_health"),
                        field("current_mana"), field("max_mana"),
                        field("armor_bonus"), field("weapon_bonus")
                )
                .values(
                        saveId, member.getId(), member.getName(),
                        member.getCharacterClass(), member.getPersonality(), member.getBackstory(),
                        member.getLoyalty(),
                        member.getStrength(), member.getIntelligence(), member.getAgility(),
                        member.getConstitution(), member.getCharisma(),
                        member.getCurrentHealth(), member.getMaxHealth(),
                        member.getCurrentMana(), member.getMaxMana(),
                        member.getArmorBonus(), member.getWeaponBonus()
                )
                .execute();
    }

    private List<TeamMember> loadTeamMembers(Long saveId) {
        Result<Record> records = dsl.select()
                .from(table("team_members"))
                .where(field("game_save_id").eq(saveId))
                .fetch();

        List<TeamMember> members = new ArrayList<>();
        for (Record record : records) {
            TeamMember member = TeamMember.builder()
                    .id(record.get("MEMBER_ID", String.class))
                    .name(record.get("NAME", String.class))
                    .characterClass(record.get("CHARACTER_CLASS", String.class))
                    .personality(record.get("PERSONALITY", String.class))
                    .backstory(record.get("BACKSTORY", String.class))
                    .loyalty(record.get("LOYALTY", Integer.class))
                    .strength(record.get("STRENGTH", Integer.class))
                    .intelligence(record.get("INTELLIGENCE", Integer.class))
                    .agility(record.get("AGILITY", Integer.class))
                    .constitution(record.get("CONSTITUTION", Integer.class))
                    .charisma(record.get("CHARISMA", Integer.class))
                    .currentHealth(record.get("CURRENT_HEALTH", Integer.class))
                    .maxHealth(record.get("MAX_HEALTH", Integer.class))
                    .currentMana(record.get("CURRENT_MANA", Integer.class))
                    .maxMana(record.get("MAX_MANA", Integer.class))
                    .armorBonus(record.get("ARMOR_BONUS", Integer.class))
                    .weaponBonus(record.get("WEAPON_BONUS", Integer.class))
                    .build();
            members.add(member);
        }
        return members;
    }

    private void saveNpc(DSLContext txDsl, Long saveId, NPC npc) {
        txDsl.insertInto(table("npcs"))
                .columns(
                        field("game_save_id"), field("npc_id"), field("name"),
                        field("role"), field("location"), field("agenda"), field("personality"),
                        field("is_hostile"), field("is_quest_giver"),
                        field("strength"), field("intelligence"), field("agility"),
                        field("constitution"), field("charisma"),
                        field("current_health"), field("max_health"),
                        field("current_mana"), field("max_mana"),
                        field("armor_bonus"), field("weapon_bonus")
                )
                .values(
                        saveId, npc.getId(), npc.getName(),
                        npc.getRole(), npc.getLocation(), npc.getAgenda(), npc.getPersonality(),
                        npc.isHostile(), npc.isQuestGiver(),
                        npc.getStrength(), npc.getIntelligence(), npc.getAgility(),
                        npc.getConstitution(), npc.getCharisma(),
                        npc.getCurrentHealth(), npc.getMaxHealth(),
                        npc.getCurrentMana(), npc.getMaxMana(),
                        npc.getArmorBonus(), npc.getWeaponBonus()
                )
                .execute();
    }

    private List<NPC> loadNpcs(Long saveId) {
        Result<Record> records = dsl.select()
                .from(table("npcs"))
                .where(field("game_save_id").eq(saveId))
                .fetch();

        List<NPC> npcs = new ArrayList<>();
        for (Record record : records) {
            NPC npc = NPC.builder()
                    .id(record.get("NPC_ID", String.class))
                    .name(record.get("NAME", String.class))
                    .role(record.get("ROLE", String.class))
                    .location(record.get("LOCATION", String.class))
                    .agenda(record.get("AGENDA", String.class))
                    .personality(record.get("PERSONALITY", String.class))
                    .isHostile(record.get("IS_HOSTILE", Boolean.class))
                    .isQuestGiver(record.get("IS_QUEST_GIVER", Boolean.class))
                    .strength(record.get("STRENGTH", Integer.class))
                    .intelligence(record.get("INTELLIGENCE", Integer.class))
                    .agility(record.get("AGILITY", Integer.class))
                    .constitution(record.get("CONSTITUTION", Integer.class))
                    .charisma(record.get("CHARISMA", Integer.class))
                    .currentHealth(record.get("CURRENT_HEALTH", Integer.class))
                    .maxHealth(record.get("MAX_HEALTH", Integer.class))
                    .currentMana(record.get("CURRENT_MANA", Integer.class))
                    .maxMana(record.get("MAX_MANA", Integer.class))
                    .armorBonus(record.get("ARMOR_BONUS", Integer.class))
                    .weaponBonus(record.get("WEAPON_BONUS", Integer.class))
                    .build();
            npcs.add(npc);
        }
        return npcs;
    }

    private void saveQuest(DSLContext txDsl, Long saveId, Quest quest) {
        txDsl.insertInto(table("quests"))
                .columns(
                        field("game_save_id"), field("quest_id"), field("title"),
                        field("description"), field("given_by"), field("status"),
                        field("is_main_quest"), field("experience_reward")
                )
                .values(
                        saveId, quest.getId(), quest.getTitle(),
                        quest.getDescription(), quest.getGivenBy(), quest.getStatus().name(),
                        quest.isMainQuest(), quest.getExperienceReward()
                )
                .execute();
    }

    private List<Quest> loadQuests(Long saveId) {
        Result<Record> records = dsl.select()
                .from(table("quests"))
                .where(field("game_save_id").eq(saveId))
                .fetch();

        List<Quest> quests = new ArrayList<>();
        for (Record record : records) {
            Quest quest = Quest.builder()
                    .id(record.get("QUEST_ID", String.class))
                    .title(record.get("TITLE", String.class))
                    .description(record.get("DESCRIPTION", String.class))
                    .givenBy(record.get("GIVEN_BY", String.class))
                    .status(Quest.QuestStatus.valueOf(record.get("STATUS", String.class)))
                    .isMainQuest(record.get("IS_MAIN_QUEST", Boolean.class))
                    .experienceReward(record.get("EXPERIENCE_REWARD", Integer.class))
                    .build();
            quests.add(quest);
        }
        return quests;
    }

    private void saveStoryEvent(DSLContext txDsl, Long saveId, int order, String event) {
        txDsl.insertInto(table("story_history"))
                .columns(field("game_save_id"), field("event_order"), field("event_text"))
                .values(saveId, order, event)
                .execute();
    }

    private List<String> loadStoryHistory(Long saveId) {
        Result<Record> records = dsl.select()
                .from(table("story_history"))
                .where(field("game_save_id").eq(saveId))
                .orderBy(field("event_order"))
                .fetch();

        List<String> history = new ArrayList<>();
        for (Record record : records) {
            history.add(record.get("EVENT_TEXT", String.class));
        }
        return history;
    }

    private void setGameId(GameState state, String gameId) {
        // GameState generates a random UUID in constructor, we need to override it
        // Using reflection since there's no setter
        try {
            var field = GameState.class.getDeclaredField("gameId");
            field.setAccessible(true);
            field.set(state, gameId);
        } catch (Exception e) {
            LOG.warnf("Could not set gameId via reflection, using generated ID");
        }
    }

    private String getMessageType(ChatMessage message) {
        if (message instanceof UserMessage) {
            return "USER";
        } else if (message instanceof AiMessage) {
            return "AI";
        } else if (message instanceof SystemMessage) {
            return "SYSTEM";
        }
        return "USER";
    }

    private String getMessageContent(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return userMessage.singleText();
        } else if (message instanceof AiMessage aiMessage) {
            return aiMessage.text();
        } else if (message instanceof SystemMessage systemMessage) {
            return systemMessage.text();
        }
        return "";
    }

    private ChatMessage createChatMessage(String type, String content) {
        return switch (type) {
            case "AI" -> AiMessage.from(content);
            case "SYSTEM" -> SystemMessage.from(content);
            default -> UserMessage.from(content);
        };
    }
}
