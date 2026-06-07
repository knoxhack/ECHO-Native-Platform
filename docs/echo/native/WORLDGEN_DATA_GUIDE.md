# Worldgen And Data Guide

Use typed services for worldgen and data/resource ownership.

## Worldgen

- `EchoNativeWorldgenService.registerFeature` declares a feature, placed feature, biome modifier, or structure hook.
- `EchoNativeWorldgenService.placeStructure` requests runtime placement through the active host.

## Resources

- `EchoNativeResourceService.registerReloadListener` declares reload ownership.
- `EchoNativeResourceService.reload` performs a reload pass.
- `EchoNativeResourceService.applyResourcePack` applies a generated or packaged resource pack.

## Save Data

Use `EchoNativeSaveDataService` for arbitrary world or module state. Runtime hosts must make save writes deterministic and visible to parity checks.

## Datagen, Recipes, Tags, Loot

Datagen output should become resource/data pack content owned by the addon. Recipes, tags, and loot modifiers should be represented as deterministic generated resources and registry/resource receipts, not as loader implementation calls.

