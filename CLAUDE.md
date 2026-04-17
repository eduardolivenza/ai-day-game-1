# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

This is a **Gradle multi-module project** (Java 25). All commands run from the repo root.

```bash
# Compile both modules
./gradlew :game:compileJava :server:compileJava

# Run the AI server (terminal 1)
./gradlew :server:bootRun

# Run the game (terminal 2) – server must be running first so NPCs load
./gradlew :game:run

# Clean build
./gradlew clean
```

The game's working directory is `game/` (configured in `game/build.gradle.kts`), so `game/config.properties` and `game/save.json` are resolved relative to that directory.

## Module Overview

| Module | Tech | Entry point |
|--------|------|-------------|
| `game/` | Java Swing, `org.json` | `com.game.Main` → `GameWindow` → `GamePanel` |
| `server/` | Spring Boot 3.3.5, Spring AI 1.0.3 | `com.game.server.Application` |

Spring Boot / dependency-management plugin versions are pinned once in the root `build.gradle.kts` (`apply false`) and referenced without version in `server/build.gradle.kts`.

## Game Architecture

`GamePanel` is the hub — every subsystem holds a reference to it and reads siblings through it. Construction order in `GamePanel()` matters:

```
GamePanel
 ├── KeyHandler             (KeyListener, attached to JPanel)
 ├── TileManager            (generates all 10 tile BufferedImages procedurally at startup)
 ├── MapManager(gp, config) (builds maps, fetches NPCs from server, places them on spawn slots)
 ├── VaultManager           (picks random vault location; randomizes again when vault is found)
 ├── Player(gp)
 ├── DialogBox(gp, cfg)     (holds GameApiClient)
 └── SaveManager(gp)        (loadGame() called at end of constructor; saveGame() on JVM shutdown)
```

**Game loop** (60 FPS fixed-timestep in `GamePanel.run()`): each tick calls `update()` then `repaint()`. The `update()` is gated by `GameState` enum (`PLAYING / DIALOG / TRANSITION / VAULT_FOUND`).

**Rendering** in `paintComponent`: world tiles → vault chest → NPCs → player (all offset by camera), then camera is undone and screen-space UI (HUD, dialog box, fade overlay, vault-found overlay) is drawn on top.

### Tile System

`TileManager` generates all sprites with `Graphics2D` at startup — there are no image files. Tile IDs are:

```
0=GRASS  1=WALL  2=WATER  3=PATH  4=DOOR  5=TREE  6=SAND  7=FLOOR  8=INT_WALL  9=FLOWER
```

Tile size is `48px` (`TileManager.T`). Screen is 16×12 tiles (768×576px).

### Maps & Transitions

Maps are defined as `int[][]` arrays inside `MapManager.buildMaps()`. Each map is a `TileMap` (id, tile data, `List<DoorConnection>`, `List<NPC>`).

A `DoorConnection` links a tile position `(fromCol, fromRow)` on one map to a pixel spawn `(targetX, targetY)` on another. When `Player` steps on a tile-4 (DOOR), it calls `MapManager.startTransition(door)`, which runs a fade-out → swap → fade-in sequence over ~40 frames.

Current maps: `overworld` (25×20), `house1` (12×10), `house2` (12×10), `house3` (12×10).

To **add a new map**: define its `int[][]` in `buildMaps()`, wire `DoorConnection`s on both sides, call `maps.put(id, map)`, and optionally add a spawn slot to `SPAWN_SLOTS`.

### NPC System

NPCs are defined entirely in `server/src/main/resources/application.yml` under `npc.characters`. Each entry has:
- `context`: the character's personality and lore (used as part of the system prompt)
- `shirt-color`: hex color used for sprite rendering and dialog header tint

At startup `MapManager` calls `GET /api/npc/list`, receives all NPC names and colors, shuffles the predefined `SPAWN_SLOTS`, and places one NPC per slot. The game has no hardcoded NPC names or count.

**To add a new NPC**: add an entry to `application.yml` under `npc.characters` with a `shirt-color` and `context`. It will be picked up automatically on next launch.

`SPAWN_SLOTS` in `MapManager.java` defines valid spawn positions (mapId, col, row). Currently 7 slots across the overworld and all 3 houses. Add more slots if you add more NPCs than slots.

### Entity & Collision

`Entity` is the base class with `worldX/worldY`, directional sprite array `[4 directions][3 frames]`, and a `solidArea` hitbox. Sprites are generated procedurally using `NPC.drawFront/drawBack/drawSide` static helpers (shared with `Player`).

Collision in `Player.wouldCollide(dx, dy)` checks the four corners of the shifted hitbox against tile solidity and NPC solid areas. NPC collision is skipped if the player is already overlapping an NPC (prevents post-dialog sticking).

### Vault System

`VaultManager` maintains a randomly selected location from 8 named overworld spots. Each spot has a `(col, row)` position and a human-readable `description` used as NPC hint context.

- The vault is rendered as a small wooden chest in world-space (overworld only).
- When the player approaches, a HUD hint appears; pressing ENTER triggers `GamePanel.openVault()`.
- `openVault()`: clears all NPC conversation histories, calls `vaultManager.randomize()`, switches to `VAULT_FOUND` state which shows a full-screen overlay.
- The current vault location index is included in `save.json` so it persists across sessions.
- The vault description is sent as `vaultContext` in every NPC chat request. The server injects it into the system prompt so NPCs give specific, location-aware clues.

To **add or change vault locations**: edit the `LOCATIONS` list in `VaultManager.java`. Each entry needs a grass tile in the overworld that is not a door or water.

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
- **ESC**: closes the conversation immediately from both `SHOWING_CHOICES` and `TYPING` states.
- NPC conversation history (`List<Map<String,String>>`) is maintained on the `NPC` object and included in every request. It is persisted to `save.json` and cleared when the vault is found.

## Server Architecture

```
NpcController  →  NpcService  →  Spring AI ChatModel
```

### API Endpoints

**`GET /api/npc/list`** — returns all NPC definitions from `application.yml`:
```json
[
  { "name": "Elder Oryn", "shirtColor": "#708090" },
  ...
]
```

**`POST /api/npc/chat`** — runs one turn of NPC dialog:
```
Request:
{
  "npcName": "Elder Oryn",
  "history": [{"role":"assistant","content":"..."},{"role":"user","content":"..."}],
  "playerMessage": null,       (null = start new conversation)
  "includeChoices": true,      (true → message+choices, false → message only)
  "vaultContext": "in the northeast corner..."
}

Response:
{
  "npcMessage": "Greetings, traveler...",
  "choices": ["Option A", "Option B", "Option C"]   (empty when includeChoices=false)
}
```

`NpcService.chat()` builds the system prompt from `NpcProperties.buildSystemPrompt(npcName)` (global background + character context from `application.yml`), then appends the vault location as an additional instruction if `vaultContext` is present.

### NPC Configuration (`application.yml`)

`npc.global-background`: shared world lore and behavioral rules applied to all NPCs.

`npc.characters.<name>`: per-NPC fields:
- `shirt-color` (hex): visual color
- `context`: character personality, backstory, and vault knowledge style

## Persistence

`SaveManager` writes `game/save.json` on JVM shutdown and loads it on startup. It persists:
- Current map ID
- Player pixel position
- Per-NPC conversation history
- Vault location index

The save file and `game/config.properties` are gitignored.

## Controls

| Key | Action |
|-----|--------|
| WASD / Arrow keys | Move |
| ENTER / SPACE | Talk to nearby NPC or open vault; confirm in dialog |
| 1 / 2 / 3 | Select numbered dialog choice |
| T | Open free-text input in dialog |
| ESC | Exit conversation (from choices or typing screen) |
