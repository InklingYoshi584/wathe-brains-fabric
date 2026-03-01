# WatheBrains AI Mod

AI mod for Harpy Express (Wathe) using LLM-powered decision making.

## Status: 🔧 In Development

This is a skeleton implementation. The following needs to be done:

### ✅ Done
- Core AI classes (AiProfile, AiManager, AiMemory)
- GameState serialization for LLM
- LlmEngine (Siliconflow/DeepSeek integration)
- AiActionExecutor (action routing)
- AiTickHandler (decision loop)
- Basic build setup

### ⏳ TODO: Wathe API Integration

The following needs integration with Wathe/Harpy Express game:

1. **Coins** - Get player coin count
2. **Mood System** - Get civilian mood, mood prompts
3. **Room Keys** - Get player's room ID, check if in own room
4. **Items** - Check inventory for knife, gun, grenade, lockpick, poison, scorpion
5. **Shop** - Buy items from killer shop
6. **Kill Methods** - Knife stab, gun shoot, grenade throw
7. **Psycho Mode** - Activate, track timer, bat attack
8. **Poison** - Poison food platters/trays, place scorpions on beds
9. **Blackout** - Use blackout item
10. **Car System** - Get player car, region (front/middle/back)
11. **Bodies** - Detect bodies, track body positions
12. **Vision/LOS** - Line of sight calculation

### 📋 Dependencies

- **Fabric API** - Already included
- **Carpet Mod** - For fake players (adds `/player` command)
- **Simple Voice Chat** - For TTS audio broadcasting (optional)

### 🔑 Environment Variables

Set before running:
```bash
export SILICONFLOW_API_KEY=your_api_key_here
```

### 🎮 Usage

1. Install Wathe mod + Carpet mod on Fabric server
2. Build this mod: `./gradlew build`
3. Add to server mods folder
4. Set API key
5. Spawn AI: `/player AI_Killer_1 spawn` (or use in-game spawner)

## Architecture

```
User Message (plan.md)
        │
        ▼
┌───────────────────┐
│   AiTickHandler   │ ← Server tick loop
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  GameStateBuilder │ ← Build game snapshot
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│    LlmEngine      │ ← DeepSeek 3.2 via Siliconflow
│  (Siliconflow)    │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│ AiActionExecutor  │ ← Execute in-game actions
└───────────────────┘
```
