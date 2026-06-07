# Client And Render Guide

Client-only behavior must be side-gated and routed through typed services.

## Surfaces

- `EchoNativeRenderService.registerLayer` for render layers, overlays, HUD passes, and visual hooks.
- `EchoNativeRenderService.renderTick` for host-driven render ticks.
- `EchoNativeScreenService.registerSurface` for menus, screens, route surfaces, and UI ownership.
- `EchoNativeScreenService.open` and `close` for lifecycle transitions.

## Side Gates

Client services must reject server-only mutations on a client host and client-only mutations on a server host. A wrong-side call should return a non-mutating receipt, usually `FAILED` or `UNSUPPORTED`, with side-gate evidence.

## Keybinds And HUD

Keybinds and HUD overlays should register stable targets through screen/render route surfaces. NeoForge key and render events are compatibility adapters only; they must not become the owner of native runtime state.

