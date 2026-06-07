# ECHO Runtime Lanes

## Canonical Role Assignment

This document locks the runtime lane architecture. No doc, report, portal page, or launcher page may describe these roles differently.

---

### Native Loader - Primary Future Mod Loader

The ECHO Native Loader is the **main platform lane**. It is not UI-only, not a compatibility-only layer, and not report-only.

Responsibilities:
- Launch or attach the Minecraft client
- Discover ECHO modules by reading `echo.mod.json` descriptors
- Resolve module dependencies and build classpaths
- Create isolated module classloaders
- Load module entrypoint classes
- Invoke the full module lifecycle: DISCOVER -> RESOLVE -> LOAD_CLASSES -> CONSTRUCT -> REGISTER_SERVICES -> REGISTER_CONTENT -> COMMON_SETUP -> CLIENT_SETUP -> SERVER_SETUP -> READY -> SHUTDOWN
- Register AdapterCore services into the active service registry
- Expose item / block / entity / world / event / save hooks
- Run Ashfall gameplay through **real state mutations** (not reports, not fake activation)

Success Rules (enforced):
- `DISCOVERED` = module metadata was found and parsed
- `RESOLVED` = dependencies planned and classpath built
- `LOADED` = module entrypoint class was actually loaded by a classloader
- `REGISTERED` = at least one service or content entry was registered
- `MUTATED` = a runtime/game state change was persisted or emitted
- `FAILED` = an attempt was made and an exception was caught
- `UNSUPPORTED` = the module declares no native entrypoint or incompatible surfaces

A module may only claim `activated` when `LOADED` and `REGISTERED` are both proven.
A runtime may only claim `gameplay ready` when `MUTATED` is proven on all required AdapterCore surfaces.

---

### NeoForge - Compatibility Fallback

NeoForge is the **compatibility backend**. It exists so ECHO modules can also run inside a standard NeoForge mod loader when the Native Loader is not yet deployed or when a user explicitly chooses compatibility mode.

Responsibilities:
- Provide the same AdapterCore contract surfaces through NeoForge-specific implementations
- Accept module descriptors that do not declare a native entrypoint
- Bridge NeoForge registries, capabilities, and events into the ECHO service registry
- Serve as a runtime fallback when `echo.native.runtime.mode=NEOFORGE` or when the Native Loader cannot launch

NeoForge must be described everywhere as **fallback/compatibility**. Do not promote it above Native Loader.

---

### Standalone Runtime - Parity / Runtime Harness

The Standalone Runtime is a **parity and runtime harness**. It hosts a real persisted runtime without requiring a live Minecraft client process.

Responsibilities:
- Provide a disk-persisted runtime for testing, CI, and offline development
- Implement all AdapterCore surfaces using in-memory + JSON-on-disk state
- Validate that Native Loader mutations produce the same observable state as NeoForge
- Serve as the `STANDALONE` runtime mode when no Minecraft process is present

The Standalone Runtime is real. It is not a mock. Every mutation surface writes save data.

---

### Launcher - Product Shell

The Launcher is the **product shell**. It is the user-facing application that selects a pack, chooses a runtime mode, and starts the loading process.

Responsibilities:
- Display pack catalog and module surface catalog
- Allow runtime mode selection (Native Loader first, NeoForge fallback, Standalone harness)
- Pass the selected mode to the bootstrap layer
- Show real mutation feedback (not fake capability reports)

---

### Developer Portal - Ops / Debug Shell

The Developer Portal is the **ops and debug shell**. It provides diagnostics, truth gates, and runtime inspection.

Responsibilities:
- Run truth gates that verify real state mutation (not just report generation)
- Surface module load diagnostics
- Display the mutation ledger
- Allow developers to inspect the service registry and runtime host state

---

### AdapterCore - Shared Gameplay Contract

AdapterCore is the **shared gameplay contract** that all runtime lanes implement.

Surfaces:
- `inventory` - grant, remove, query player items
- `player_state` - health, stats, spawn state, experience
- `world_blocks` - place, break, query blocks
- `world_state` - dimension properties, time, weather
- `structures` - place schematic structures
- `block_entities` - read/write block entity data
- `capabilities` - attach/detach capability values
- `events` - emit and listen to gameplay events
- `packets_hud` - send HUD update packets
- `hud` - display overlay notifications
- `save_data` - persist and load arbitrary save keys

Every runtime lane must implement these surfaces. Native Loader is the canonical reference implementation. NeoForge maps them to Minecraft/NeoForge APIs. Standalone maps them to disk-backed JSON stores.

---

## Lane Priority

1. **Native Loader** - always preferred when available
2. **NeoForge** - used when Native Loader is unavailable or user explicitly selects compatibility
3. **Standalone** - used for CI, headless, or offline workflows

## Forbidden Descriptions

The following descriptions are forbidden in any doc, report, portal, or launcher:

- "Native Loader is UI-only"
- "Native Loader is report-only"
- "Native Loader is compatibility-only"
- "NeoForge owns the main platform lane"
- "Standalone provides fake state"

---

*Version: phase-13-runtime-lanes-v1*
*Locked: 2026-06-03*
