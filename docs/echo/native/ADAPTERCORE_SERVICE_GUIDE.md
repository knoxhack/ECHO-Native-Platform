# AdapterCore Service Guide

AdapterCore is the shared gameplay bridge. It describes the behavior that every runtime lane must implement: Native Loader, NeoForge compatibility, and Standalone harness.

## When To Use AdapterCore

Use `echoadaptercore` when an addon needs gameplay state or cross-runtime feature behavior, such as inventory grants, block placement, world state, HUD messages, save data, structures, capabilities, or block entity data.

Use `echoaddonapi` alone when the addon only declares content or metadata and does not need runtime behavior.

## Host Rule

Addon code talks to AdapterCore contracts. Runtime lanes provide host implementations. The Native Loader implementation is hidden behind those contracts.

Good:

```java
EchoNativeRuntimeHost host = EchoRuntimeHostRegistry.activeHost();
```

Bad:

```java
NativeLoaderAdapterCoreBackend backend = new NativeLoaderAdapterCoreBackend(...);
```

## Mutation Receipts

AdapterCore host calls must be backed by typed mutation receipts when they cross the Native Loader boundary. If a host cannot prove a state change, it should report `REGISTERED`, `UNSUPPORTED`, `FAILED`, or a non-mutating AdapterCore result instead of claiming mutation.

## Runtime Parity

Every AdapterCore surface should have parity evidence for:

- Native Loader
- NeoForge compatibility fallback
- Standalone harness

Use `echo-native-testkit` for fast unit/QA checks and the existing AdapterCore smoke tasks for deeper runtime verification.

