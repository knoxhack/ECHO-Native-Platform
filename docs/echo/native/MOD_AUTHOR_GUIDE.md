# Mod Author Guide

Native addons should depend on the public SDK/API layer, not the loader implementation.

Use:

- `echoaddonapi` for addon-facing author APIs.
- `echoadaptercore` for typed feature/runtime bridge services.
- `echo-native-contracts` for loader/runtime SPI contracts.
- `echo-native-testkit` for fake host testing.

Avoid:

- Importing `echo-native-loader` from addon runtime code.
- Importing NeoForge runtime classes from addon runtime code.
- Treating descriptor metadata as mutation proof.
- Shipping `nativeClasspath` entries that point at local build output.

## Minimum Addon Shape

An addon should provide:

- A jar artifact.
- `META-INF/echo.mod.json`.
- A native entrypoint or native module descriptor.
- Typed service registrations through AdapterCore or SDK contracts.
- Focused parity tests or smoke evidence.

## Release Expectations

Before publishing, compile the addon from clean sources, run the SDK template/testkit checks for example code, attach parity evidence, and include the addon in the Plan 3 release-prep report.
