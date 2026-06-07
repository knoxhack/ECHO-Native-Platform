# Porting From NeoForge

Porting means moving addon behavior behind ECHO typed contracts while keeping NeoForge only as a compatibility backend.

## Replacement Map

| NeoForge surface | Native SDK surface |
| --- | --- |
| Deferred registers and registries | `EchoNativeRegistryService.register` and `deferredRegister` |
| Mod/event bus listeners | `EchoNativeEventService.subscribe` and `publish` |
| Brigadier command registration | `EchoNativeCommandService.register` |
| Config specs | `EchoNativeConfigService.register`, `write`, and `reload` |
| Custom packets/channels | `EchoNativeNetworkService.registerPacket`, `sendToPlayer`, and `broadcast` |
| Capabilities | `EchoNativeCapabilityService.register`, `mutate`, and `read` |
| Attachments/data attachments | `EchoNativeAttachmentService.attach` and `detach` |
| Data components/block entities/save data | `EchoNativeSaveDataService` and AdapterCore runtime host services |
| Menus/screens/client routes | `EchoNativeScreenService` plus client route contracts |
| Render hooks, HUD overlays, keybinds | `EchoNativeRenderService`, `EchoNativeScreenService`, and client route/input registration |
| Recipes/tags/loot/datagen/resources | Registry/resource/worldgen services plus generated resource packs |
| Game tests | `echo-native-testkit` fake hosts and golden parity assertions |

## Porting Order

1. Move addon identity, descriptors, and plain data models into `echoaddonapi` or addon-owned packages.
2. Replace direct NeoForge imports in common code with `echo-native-contracts` and `echoadaptercore` contracts.
3. Register content through typed services and require `EchoNativeMutationReceipt.mutated()` for success.
4. Keep NeoForge code in compatibility adapters only.
5. Add testkit coverage that proves the same surfaces mutate without a NeoForge runtime.
6. Package an `echo.mod.json` native descriptor and verify release mode does not depend on dev classpath fallback.

## Anti-Patterns

- Do not call `activateNative(Map)` for new work.
- Do not count descriptor metadata as mutation proof.
- Do not import `net.neoforged.*` from addon common/native code.
- Do not import `dev.echo.nativeplatform.loader.*` from an addon.

