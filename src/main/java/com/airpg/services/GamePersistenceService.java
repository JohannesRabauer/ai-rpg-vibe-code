package com.airpg.services;

import com.airpg.domain.GameState;
import dev.langchain4j.data.message.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for game save/load persistence.
 */
public interface GamePersistenceService {

    /**
     * Save the current game state. Fails if game is in combat.
     *
     * @param state    The game state to save
     * @param saveName Display name for the save
     * @return Result of the save operation
     */
    SaveResult saveGame(GameState state, String saveName);

    /**
     * Load a game state by save ID.
     *
     * @param saveId The ID of the save to load
     * @return The loaded game state, or null if not found
     */
    GameState loadGame(Long saveId);

    /**
     * List all available saves, ordered by most recent first.
     *
     * @return List of save metadata
     */
    List<SaveMetadata> listSaves();

    /**
     * Delete a save by ID.
     *
     * @param saveId The ID of the save to delete
     */
    void deleteSave(Long saveId);

    /**
     * Save agent memories for a game save.
     *
     * @param gameSaveId The ID of the game save
     * @param memories   Map of memory ID to list of chat messages
     */
    void saveAgentMemories(Long gameSaveId, Map<Object, List<ChatMessage>> memories);

    /**
     * Load agent memories for a game save.
     *
     * @param gameSaveId The ID of the game save
     * @return Map of memory ID to list of chat messages
     */
    Map<Object, List<ChatMessage>> loadAgentMemories(Long gameSaveId);

    /**
     * Metadata for a saved game, used for list display.
     */
    record SaveMetadata(
            Long id,
            String gameId,
            String saveName,
            String heroName,
            String heroClass,
            int heroLevel,
            String location,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    /**
     * Result of a save operation.
     */
    record SaveResult(
            boolean success,
            String message,
            Long saveId
    ) {}
}
