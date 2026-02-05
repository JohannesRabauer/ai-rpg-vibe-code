# Copilot Instructions - AI RPG Vibe

This is a text-based fantasy RPG powered by **Quarkus**, **Vaadin**, and **LangChain4j AI agents**.

## Build & Run Commands

### Development
```bash
# Start in dev mode with hot reload
mvn quarkus:dev

# Access at http://localhost:8080
```

### Production Build
```bash
# Create uber-jar (single executable JAR)
mvn clean package -Puber-jar
java -jar target/ai-rpg-vibe-1.0.0-SNAPSHOT-runner.jar

# Native image (requires GraalVM)
mvn clean package -Pnative
```

### Testing
```bash
# Run tests
mvn test

# Run tests + integration tests
mvn verify
```

### Database Code Generation
```bash
# Regenerate jOOQ classes after schema changes
mvn clean generate-sources
```

## Architecture Overview

### AI Agent System
The game uses **multiple specialized AI agents**, each with isolated memory:

- **WorldNarratorAgent** - Scene descriptions and story progression
- **WorldNarratorStreamingAgent** - Streaming variant for real-time token display
- **CombatNarratorAgent** - Combat narration (turn results → dramatic descriptions)
- **NPCAgent** - Individual NPC personalities (one instance per NPC)
- **CompanionAgent** - Autonomous party member behavior

**Key Pattern:** Agents are interfaces annotated with `@SystemMessage` and `@UserMessage`. They're instantiated via `AiServices.builder()` and cached in `AgentService`.

### Memory Architecture
- Each agent type has **separate memory** identified by ID (e.g., `"world-narrator"`, `"npc-{npcId}"`)
- Uses `MessageWindowChatMemory` with configurable max messages
- Memory is stored in `InMemoryChatMemoryStore` (CDI singleton)
- Call `AgentService.clearAllMemories()` on new game to reset all agents

### Streaming Pattern
For real-time AI responses:
1. Use `*Streaming` agent variants (return `TokenStream` instead of `String`)
2. Implement `StreamingResponseHandler` interface with callbacks: `onToken()`, `onComplete()`, `onError()`
3. Vaadin UI uses `@Push` annotation + `UI.access()` for thread-safe updates

### Dependency Injection
- Uses CDI via Quarkus
- `@ApplicationScoped` for singleton beans (services, agents)
- `@Inject` for constructor/field injection
- `@PostConstruct` for initialization after injection
- **Never instantiate services manually** - always inject

### Configuration
Uses **SmallRye ConfigMapping** (not `@ConfigProperty`):
```java
@ConfigMapping(prefix = "ai")
public interface AIConfig {
    String provider();
    OpenAIConfig openai();
    OllamaConfig ollama();
}
```
All config comes from `application.properties` - **don't hardcode values**.

## Key Conventions

### Domain Model Hierarchy
```
GameCharacter (abstract)
├── Hero (player character)
├── NPC (non-player characters)
└── TeamMember (party companions)
```

**D&D-Inspired Stats:**
- Attributes: 3-18, Modifiers = `(attribute - 10) / 2`
- Derived: Health = CON × 10, Mana = INT × 5, Defense = 10 + AGI modifier + armor

### Combat System
- Turn-based with initiative: d20 + Agility modifier
- Melee: d20 + STR vs Defense, damage = STR + weapon + d6
- Magic: Auto-hit, damage = INT + d8 (resistible)
- **Companions act autonomously** via `CompanionAgent.decideAction()`

### Code Style
- **Lombok annotations everywhere:** `@Data`, `@Builder`, `@SuperBuilder`, `@NoArgsConstructor`
- **Logging:** `private static final Logger LOG = Logger.getLogger(ClassName.class);` (JBoss Logger)
- **Null safety:** Always check game state before operations (`gameState` may be null before game starts)
- **String formatting:** Use `String.format()` or text blocks for multi-line

### Vaadin UI Patterns
- Single-page app in `GameView.java`
- **Thread safety:** Use `UI.access(() -> {...})` for background thread UI updates
- **Streaming updates:** Requires `@Push` annotation on view class
- Server-side rendering - no JavaScript needed

## Critical Patterns & Pitfalls

### ✅ DO
- Inject all services via `@Inject`
- Use streaming agent variants for better UX
- Clear agent memories on new game: `AgentService.clearAllMemories()`
- Check `gameState.isInCombat()` before combat operations
- Use `UI.access()` for UI updates from callbacks
- Follow the agent pattern for new AI behaviors (create interface → register in `AgentService`)

### ❌ DON'T
- Instantiate services manually (use CDI)
- Hardcode configuration values
- Forget `@Push` annotation when streaming
- Assume game state exists (may be null)
- Skip `handler.onComplete()` in streaming callbacks

## AI Provider Configuration

Set provider in `application.properties`:
```properties
# OpenAI
ai.provider=openai
ai.openai.api-key=${OPENAI_API_KEY}
ai.openai.model=gpt-4o

# OR Ollama (local)
ai.provider=ollama
ai.ollama.base-url=http://localhost:11434
ai.ollama.model=llama3.2
```

Both providers support regular and streaming models via `AIProviderFactory`.

## File Map for Common Tasks

| Task | Files |
|------|-------|
| Add new AI agent | `agents/` - Create interface, register in `AgentService.java` |
| Modify game commands | `services/GameEngine.java` → `processPlayerInput()` |
| Change combat mechanics | `services/CombatService.java` |
| Update UI layout | `ui/GameView.java` |
| Add character class | `domain/Hero.java` → `createDefault()` |
| Configure AI settings | `config/AIConfig.java`, `application.properties` |
| Modify NPC behavior | `agents/NPCAgent.java`, `services/NPCService.java` |
| Database schema | `src/main/resources/db/migration/` (Flyway) |

## Game Commands

| Command | Handler | Description |
|---------|---------|-------------|
| `help` | `GameEngine` | Show available commands |
| `stats` | `GameEngine` | Display character stats |
| `quests` | `GameEngine` | View active quests |
| `team` | `GameEngine` | Show party members |
| `location` | `WorldService` | Describe current location |
| `talk [msg]` | `NPCService` | NPC dialogue |
| `attack` | `CombatService` | Execute combat round |
| `flee` | `CombatService` | Escape combat |
| `combat test` | `CombatService` | Debug: start test combat |
| *Natural language* | `WorldNarratorAgent` | AI interprets and responds |

## Database & Persistence

- **H2 file-based database:** `./data/airpg-saves`
- **Flyway migrations:** `src/main/resources/db/migration/`
- **jOOQ code generation:** Runs during `mvn generate-sources`
- **Persistence service:** `persistence/` package (save/load game state)

**Schema changes workflow:**
1. Add migration in `db/migration/V{version}__{description}.sql`
2. Run `mvn clean generate-sources` to regenerate jOOQ classes
3. Flyway auto-migrates on next `mvn quarkus:dev`

## Release Process

GitHub Actions auto-builds on git tags:
```bash
git tag v1.0.0
git push origin v1.0.0
```
Produces platform-specific packages (Windows, Linux, macOS) in Releases.
