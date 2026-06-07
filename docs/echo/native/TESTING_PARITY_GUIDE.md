# Testing And Parity Guide

Use `echo-native-testkit` for fast SDK tests and runtime smoke tasks for deeper proof.

## Fake Hosts

`EchoNativeSdkTestkit` provides fake typed hosts for:

- registry
- lifecycle
- events
- commands
- config
- network
- resources
- capabilities
- attachments
- worldgen
- render
- screens
- save data

## Example

```java
EchoNativeSdkTestkit.Environment env = EchoNativeSdkTestkit.client("exampleaddon");
env.network().registerPacket(env.mutation("network", "registerPacket", "exampleaddon:sync"));
env.render().registerLayer(env.mutation("render", "registerLayer", "exampleaddon:hud"));
env.goldenParity().requireMutatedServices("echo.native.network", "echo.native.render");
```

## What To Assert

- Required services returned typed receipts.
- Required surfaces returned `MUTATED` where state changes are expected.
- Wrong-side calls are rejected.
- Duplicate content IDs or packet IDs fail.
- Module fixtures produce descriptors with native entrypoints.

## Reference Task

Run:

```powershell
.\gradlew.bat --no-problems-report runNativeSdkTestkitSmoke
```

