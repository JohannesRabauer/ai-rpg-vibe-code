-- V8: Add indexes for performance
CREATE INDEX idx_game_saves_game_id ON game_saves(game_id);
CREATE INDEX idx_game_saves_updated_at ON game_saves(updated_at DESC);

CREATE INDEX idx_heroes_game_save_id ON heroes(game_save_id);

CREATE INDEX idx_team_members_game_save_id ON team_members(game_save_id);
CREATE INDEX idx_team_members_member_id ON team_members(member_id);

CREATE INDEX idx_npcs_game_save_id ON npcs(game_save_id);
CREATE INDEX idx_npcs_npc_id ON npcs(npc_id);

CREATE INDEX idx_quests_game_save_id ON quests(game_save_id);
CREATE INDEX idx_quests_quest_id ON quests(quest_id);

CREATE INDEX idx_story_history_game_save_id ON story_history(game_save_id);
CREATE INDEX idx_story_history_event_order ON story_history(game_save_id, event_order);

CREATE INDEX idx_agent_memories_game_save_id ON agent_memories(game_save_id);
CREATE INDEX idx_agent_memories_memory_id ON agent_memories(game_save_id, memory_id);
