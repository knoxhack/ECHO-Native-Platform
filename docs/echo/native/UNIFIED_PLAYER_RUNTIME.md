# Unified ECHO Native Player Runtime

ECHO Native is the host contract for the player-facing runtime. Native Platform owns host SPI, loader behavior, typed services, and conformance evidence. It does not own pack-specific UI or gameplay.

Ashfall remains an integration fixture. Scripts, fixtures, and reports that mention Ashfall must be treated as product evidence examples until they are replaced by generic product conformance tasks.

## Host Responsibilities

Native Platform must provide typed services for:

- Module discovery and `.echo-addon` loading.
- ECHO UI surface hosting for ScreenCore, HUDCore, Terminal, Index, diagnostics, blocker, save/session, and inventory surfaces.
- Input binding adaptation from `echo.input.binding.v1`.
- Inventory and item action adaptation from `echo.inventory.surface.v1`.
- Gameplay action routing through AdapterCore and `echo.gameplay.action.v1`.
- Mutation receipts with proof kinds: `HOST_STATE`, `SAVE_WRITE`, `HUD_EVENT`, and `PACKET_EVENT`.
- Save/session reporting through `echo.save.session.v1`.
- Per-host conformance through `echo.runtime.conformance.v1`.

## Adapter Rule

Native Loader is the reference host. NeoForge, Standalone Runtime, and Standalone Engine must consume the same SDK contracts and module graph evidence. Host-specific internals may differ, but player-visible layout, labels, data, input, actions, diagnostics, and mutation semantics must come from ECHO module contracts.

## Product Fixture Rule

Generic conformance should use `fixtures/<packId>` and `reports/echo-native/<packId>` naming. Existing Ashfall-named gates should be compatibility shims over product-neutral conformance reducers whenever they enforce platform readiness.

## Release Rule

Native Platform may not claim player-ready status from descriptor-only, metadata-only, or dry-run evidence. A required player-facing surface/action must be `supported` or `adapted` in `echo.runtime.conformance.v1`; `fallback` requires explicit approval, and fallback-only required surfaces fail full parity.
