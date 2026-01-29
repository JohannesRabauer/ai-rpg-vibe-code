# CLAUDE.md - AI RPG Vibe Codebase Guide

This document provides guidance for AI assistants working with the AI RPG Vibe codebase.

## Project Overview

AI RPG Vibe is a text-based fantasy RPG powered by **Quarkus**, **Vaadin**, and **LangChain4j AI agents**. The game features dynamic AI-driven storytelling, NPC interactions with memory, autonomous companion behavior, and D&D-inspired stat-based combat.

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Backend Framework | Quarkus | 3.17.4 |
| Frontend/UI | Vaadin (server-side) | 24.5.5 |
| AI Framework | LangChain4j | 0.36.2 |
| Java Version | Java | 21 |
| Build Tool | Maven | 3.8+ |
| Boilerplate Reduction | Lombok | 1.18.36 |

## Project Structure

```
src/main/java/com/airpg/
├── agents/                    # LangChain4j AI agent definitions
│   ├── memory/               # Chat memory storage
│   │   └── InMemoryChatMemoryStore.java
│   ├── AIProviderFactory.java    # Factory for OpenAI/Ollama models
│   ├── AgentService.java         # Central agent management
│   ├── WorldNarratorAgent.java   # Scene/world narration
│   ├── WorldNarratorStreamingAgent.java  # Streaming variant
│   ├── CombatNarratorAgent.java  # Combat narration
│   ├── NPCAgent.java             # Individual NPC behavior
│   └── CompanionAgent.java       # Autonomous team member AI
├── config/                   # Configuration mapping interfaces
│   ├── AIConfig.java            # AI provider settings (SmallRye ConfigMapping)
│   └── GameConfig.java          # Game settings
├── domain/                   # Game entities and state
│   ├── GameCharacter.java       # Abstract base for all characters
│   ├── Hero.java                # Player character
│   ├── NPC.java                 # Non-player characters
│   ├── TeamMember.java          # Party companions
│   ├── Quest.java               # Quest data
│   ├── CombatEncounter.java     # Combat state
│   ├── CombatAction.java        # Single combat action
│   └── GameState.java           # Central game state container
├── services/                 # Business logic services
│   ├── GameEngine.java          # Main game coordinator
│   ├── WorldService.java        # World/narrative management
│   ├── NPCService.java          # NPC behavior and dialogue
│   ├── TeamService.java         # Companion management
│   ├── CombatService.java       # Combat system
│   └── StreamingResponseHandler.java  # Streaming callback interface
└── ui/                       # Vaadin UI components
    └── GameView.java            # Main game interface

src/main/resources/
├── application.properties       # Main configuration
├── application.properties.example-openai
└── application.properties.example-ollama

src/main/frontend/
└── index.html                   # Vaadin frontend entry

.github/
├── workflows/
│   └── release.yml              # CI/CD for multi-platform builds
└── prompts/
    └── plan-quarkusVaadinAiRpg.prompt.md  # Original design prompt
```

## Architecture Patterns

### Dependency Injection
The project uses **CDI (Contexts and Dependency Injection)** via Quarkus:
- `@ApplicationScoped` - Singleton beans (services, agents)
- `@Inject` - Constructor/field injection
- `@PostConstruct` - Initialization after injection

### AI Agent Pattern
Agents are defined as interfaces with LangChain4j annotations:
```java
public interface WorldNarratorAgent {
    @SystemMessage("You are the World Narrator...")
    String narrateScene(@UserMessage String sceneContext);
}
```

Agents are instantiated via `AiServices.builder()`:
```java
AiServices.builder(WorldNarratorAgent.class)
    .chatLanguageModel(model)
    .chatMemory(memory)
    .build();
```

### Streaming Support
AI responses can stream tokens via `TokenStream`:
- `WorldNarratorStreamingAgent` returns `TokenStream` instead of `String`
- `StreamingResponseHandler` interface provides callbacks: `onToken()`, `onComplete()`, `onError()`
- Vaadin UI uses `@Push` annotation for real-time updates

### Configuration Mapping
Uses SmallRye ConfigMapping:
```java
@ConfigMapping(prefix = "ai")
public interface AIConfig {
    String provider();
    OpenAIConfig openai();
    OllamaConfig ollama();
}
```

## Key Conventions

### Code Style
- **Lombok annotations** are used extensively: `@Data`, `@Builder`, `@SuperBuilder`, `@NoArgsConstructor`
- **Logging** via JBoss Logger: `private static final Logger LOG = Logger.getLogger(ClassName.class)`
- **Null safety**: Check for null before operations, especially with game state
- **String formatting**: Use `String.format()` or text blocks for multi-line strings

### Domain Model
- `GameCharacter` is the abstract base class for Hero, NPC, and TeamMember
- Stats follow D&D conventions: attributes 3-18, modifiers = (attribute - 10) / 2
- Derived stats: Health = Constitution × 10, Mana = Intelligence × 5
- Defense = 10 + Agility modifier + armor bonus

### Agent Memory
- Each agent type has separate memory identified by ID (e.g., "world-narrator", "npc-{id}")
- Memory uses `MessageWindowChatMemory` with configurable max messages
- `AgentService.clearAllMemories()` resets all agent memories for new games

### Combat System
- Turn-based with initiative (d20 + Agility modifier)
- Melee attacks: d20 + STR modifier vs Defense, damage = STR modifier + weapon + d6
- Magic attacks: Auto-hit, damage = INT modifier + d8 (can be resisted)
- Companions act autonomously based on AI decisions

## Development Workflows

### Running Locally
```bash
# Development mode with hot reload
mvn quarkus:dev

# Production build (uber-jar)
mvn clean package -Puber-jar
java -jar target/*-runner.jar
```

### Configuration
Set AI provider in `application.properties`:
```properties
# For OpenAI
ai.provider=openai
ai.openai.api-key=${OPENAI_API_KEY}
ai.openai.model=gpt-4o

# For Ollama (local)
ai.provider=ollama
ai.ollama.base-url=http://localhost:11434
ai.ollama.model=llama3.2
```

### Creating Releases
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions automatically builds for Windows, Linux, and macOS.

### Testing
```bash
mvn test                    # Run unit tests
mvn verify                  # Run integration tests
```

## Important Files for Common Tasks

| Task | Key Files |
|------|-----------|
| Add new AI agent | `agents/` - Create interface, register in `AgentService.java` |
| Modify game commands | `services/GameEngine.java` - `processPlayerInput()` |
| Change combat mechanics | `services/CombatService.java` |
| Update UI layout | `ui/GameView.java` |
| Add new character class | `domain/Hero.java` - `createDefault()` switch statement |
| Configure AI settings | `config/AIConfig.java`, `application.properties` |
| Modify NPC behavior | `agents/NPCAgent.java`, `services/NPCService.java` |

## AI Provider Switching

The `AIProviderFactory` supports:
- **OpenAI**: GPT-4, GPT-3.5-turbo, etc.
- **Ollama**: Local models (llama3.2, mistral, etc.)

Both regular and streaming model variants are supported.

## Game Commands Reference

| Command | Description |
|---------|-------------|
| `help` | Show available commands |
| `stats` | View character statistics |
| `quests` | View active quests |
| `team` | View party members |
| `location` | Describe current location |
| `talk [message]` | Talk to NPCs |
| `attack` | Execute combat round |
| `flee` | Escape from combat |
| `combat test` | Start test combat (debug) |

Natural language input is processed by the WorldNarratorAgent.

## Notes for AI Assistants

1. **Always inject services** - Don't instantiate services manually; use `@Inject`
2. **Check game state** - `GameEngine.getGameState()` may return null before game starts
3. **Use streaming for AI responses** - Provides better UX; use `*Streaming` variants of agents
4. **Preserve Lombok annotations** - The codebase relies heavily on Lombok for boilerplate
5. **Follow the agent pattern** - New AI behaviors should be separate agent interfaces
6. **Memory management** - Clear memories on new game via `AgentService.clearAllMemories()`
7. **UI thread safety** - Use `UI.access()` for UI updates from background threads
8. **Configuration via properties** - Don't hardcode; use ConfigMapping interfaces

## Common Pitfalls

- **Vaadin Push**: Ensure `@Push` annotation is present for streaming updates
- **Agent initialization**: Agents are lazily created; null checks may be needed
- **Combat state**: Always check `gameState.isInCombat()` before combat operations
- **Streaming completion**: Always call `handler.onComplete()` even for non-streaming paths
- **Memory IDs**: Use consistent naming for agent memory IDs to maintain conversation context

## Build Profiles

- `default` - Standard development build
- `uber-jar` - Creates single executable JAR (`-Puber-jar`)
- `native` - GraalVM native image (requires setup) (`-Pnative`)
