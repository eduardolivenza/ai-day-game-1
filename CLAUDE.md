# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

This is a **Gradle multi-module project** (Java 25). All commands run from the repo root.

```bash
# Compile both modules
./gradlew :game:compileJava :server:compileJava

# Run the AI server (terminal 1) – requires ANTHROPIC_API_KEY
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew :server:bootRun

# Run the game (terminal 2) – server must be running first
./gradlew :game:run

# Clean build
./gradlew clean
```

The game's working directory is `game/` (configured in `game/build.gradle.kts`), so `game/config.properties` and `game/save.json` are resolved relative to that directory.

## Module Overview

| Module | Tech | Entry point |
|--------|------|-------------|
| `game/` | Java Swing, `org.json` | `com.game.Main` → `GameWindow` → `GamePanel` |
| `server/` | Spring Boot 3.3.5, Spring AI 1.0.3 (Anthropic) | `com.game.server.Application` |

Spring Boot / dependency-management plugin versions are pinned once in the root `build.gradle.kts` (`apply false`) and referenced without version in `server/build.gradle.kts`.

## Game Architecture

`GamePanel` is the hub — every subsystem holds a reference to it and reads siblings through it. Construction order in `GamePanel()` matters:

```
GamePanel
 ├── KeyHandler          (KeyListener, attached to JPanel)
 ├── TileManager         (generates all 10 tile BufferedImages procedurally at startup)
 ├── MapManager(gp)      (builds maps + NPCs, owns npcRegistry)
 ├── Player(gp)
 ├── DialogBox(gp, cfg)  (holds GameApiClient)
 └── SaveManager(gp)     (loadGame() called at end of constructor; saveGame() on JVM shutdown)
```

**Game loop** (60 FPS fixed-timestep in `GamePanel.run()`): each tick calls `update()` then `repaint()`. The `update()` is gated by `GameState` enum (`PLAYING / DIALOG / TRANSITION`).

**Rendering** in `paintComponent`: world tiles + NPCs + player are drawn offset by `(cameraX, cameraY)`, then the camera translation is undone and screen-space UI (HUD, dialog box, fade overlay) is drawn on top.

### Tile System

`TileManager` generates all sprites with `Graphics2D` at startup — there are no image files. Tile IDs are:

```
0=GRASS  1=WALL  2=WATER  3=PATH  4=DOOR  5=TREE  6=SAND  7=FLOOR  8=INT_WALL  9=FLOWER
```

Tile size is `48px` (`TileManager.T`). Screen is 16×12 tiles (768×576px).

### Maps & Transitions

Maps are defined as `int[][]` arrays inside `MapManager.buildMaps()`. Each map is a `TileMap` (id, tile data, `List<DoorConnection>`, `List<NPC>`).

A `DoorConnection` links a tile position `(fromCol, fromRow)` on one map to a pixel spawn `(targetX, targetY)` on another. When `Player` steps on a tile-4 (DOOR), it calls `MapManager.startTransition(door)`, which runs a fade-out → swap → fade-in sequence over ~40 frames.

Current maps: `overworld` (25×20), `house1` (12×10), `house2` (12×10).

To **add a new map**: define its `int[][]` in `buildMaps()`, wire `DoorConnection`s on both sides, add any NPCs, and `maps.put(id, map)`.

### Entity & Collision

`Entity` is the base class with `worldX/worldY`, directional sprite array `[4 directions][3 frames]`, and a `solidArea` hitbox. Sprites are generated procedurally using `NPC.drawFront/drawBack/drawSide` static helpers (shared with `Player`).

Collision in `Player.wouldCollide(dx, dy)` checks the four corners of the shifted hitbox against tile solidity and NPC solid areas. NPC collision is skipped if the player is already overlapping an NPC (prevents post-dialog sticking).

### Dialog Flow

`DialogBox` is a state machine:

```
LOADING_INITIAL → SHOWING_CHOICES ←→ TYPING
                         ↓
                  LOADING_RESPONSE → SHOWING_FINAL → (close)
```

- `LOADING_*` states: async `CompletableFuture` calls `GameApiClient`; result lands in an `AtomicReference` polled each frame.
- **Numbered choice [1–3]**: sends reply with `includeChoices=false` → `SHOWING_FINAL` (ends conversation).
- **[T] custom message**: sends reply with `includeChoices=true` → back to `SHOWING_CHOICES` (conversation loops).
- NPC conversation history (`List<Map<String,String>>`) is maintained on the `NPC` object and included in every request so Claude has context.

## Server Architecture

```
NpcController  →  NpcService  →  Spring AI ChatModel (Anthropic)
```

`NpcService.chat(NpcChatRequest)` reconstructs a `List<Message>` (`SystemMessage` + history + new `UserMessage`) and calls `chatModel.call(Prompt)`. Claude is instructed to respond with JSON only; the service strips markdown fences if present and parses with Jackson.

**API contract** — `POST /api/npc/chat`:

```json
// Request
{
  "npcName": "Elder Oryn",
  "systemPrompt": "You are...",
  "history": [{"role":"assistant","content":"..."},{"role":"user","content":"..."}],
  "playerMessage": null,      // null = start new conversation
  "includeChoices": true       // true → return message+choices; false → message only
}

// Response
{
  "npcMessage": "Greetings, traveler...",
  "choices": ["Option A", "Option B", "Option C"]   // empty when includeChoices=false
}
```

Spring AI version: `spring-ai-bom:1.0.3`. The correct starter artifact name is `spring-ai-starter-model-anthropic` (renamed in 1.0.x; old name `spring-ai-anthropic-spring-boot-starter` no longer exists).

## NPC Definitions

All NPCs are defined in `MapManager.buildMaps()`. Each NPC has a name, a system prompt (persona), spawn tile `(col, row)`, and a shirt `Color` used for both sprite rendering and the dialog box header tint.

Current NPCs: `Elder Oryn` and `Mira` on the overworld; `Brom` in `house1`; `Aelara` in `house2`.

## Persistence

`SaveManager` writes `game/save.json` on JVM shutdown and loads it on startup. It persists: current map ID, player pixel position, and per-NPC conversation history. The save file and `game/config.properties` are gitignored.

## Controls

| Key | Action |
|-----|--------|
| WASD / Arrow keys | Move |
| ENTER / SPACE | Talk to nearby NPC; confirm in dialog |
| 1 / 2 / 3 | Select numbered dialog choice |
| T | Open free-text input in dialog |
| ESC | Cancel typing, back to choices |
