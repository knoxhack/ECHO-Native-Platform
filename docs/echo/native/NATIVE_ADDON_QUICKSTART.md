# Native Addon Quickstart

Use this path for a new addon that targets the ECHO Native SDK first.

## Dependencies

Production addon source may depend on:

```gradle
dependencies {
    compileOnly project(":echoaddonapi")
    compileOnly project(":echoadaptercore")
    compileOnly project(":echo-native-contracts")
}
```

Test or QA source may also depend on `echo-native-testkit`.

Do not add `echo-native-loader` as a dependency. The loader is the runtime host, not the addon API.

## Descriptor

Add `META-INF/echo.mod.json` with a native entrypoint:

```json
{
  "schema": "echo.native.addon.v1",
  "id": "exampleaddon",
  "name": "Example Addon",
  "version": "0.1.0",
  "kind": "native-addon",
  "role": "addon",
  "entrypoint": "com.example.echo.ExampleNativeAddon",
  "side": "common",
  "requires": [],
  "optional": [],
  "provides": ["exampleaddon"]
}
```

If the addon is packaged as a native module artifact, include `nativeClasspath` entries in descriptor access metadata only for packaged jars that belong to the addon. Do not rely on dev classpath fallback for release packaging.

## Entrypoint Shape

New addons should register through typed services and return typed mutation receipts. Use `echoaddonapi` for addon-facing content models and `echo-native-contracts` for runtime host calls.

```java
EchoNativeServiceMutation mutation = EchoNativeServiceMutation.of(
        "exampleaddon",
        "items",
        "register",
        "exampleaddon:copper_wrench",
        EchoNativeRuntimeSide.COMMON
);
EchoNativeMutationReceipt receipt = registry.register(mutation);
if (!receipt.mutated()) {
    throw new IllegalStateException("Item registration did not mutate: " + receipt.status());
}
```

## Local Test Harness

Use the SDK testkit before loading in a real client:

```java
EchoNativeSdkTestkit.Environment env = EchoNativeSdkTestkit.common("exampleaddon");
env.registry().register(env.mutation("items", "register", "exampleaddon:copper_wrench"));
env.goldenParity().requireMutatedServices("echo.native.registry");
```

Run `runNativeSdkTestkitSmoke` as the reference smoke for the fake-host pattern.

