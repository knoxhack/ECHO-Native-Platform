# Release Packaging Guide

Release packaging proves an addon runs from declared artifacts, not from a developer workstation classpath.

## Requirements

- Ship `META-INF/echo.mod.json`.
- Include addon jars or module artifacts referenced by native descriptor metadata.
- Do not depend on `echo-native-loader`.
- Do not rely on dev classpath fallback.
- Do not rely on legacy `activateNative(Map)` in release mode.
- Route `CLIENT`, `SERVER`, and `COMMON` descriptors through a matching release host side.
- Ensure all required typed services return real mutation receipts.
- Keep required module dependencies acyclic and present so release load order is deterministic.
- Keep service/content/event identifiers globally unique within the native host.
- Verify shutdown/unload is stable and repeat shutdown attempts fail closed.

## Verification

Before release, run:

```powershell
.\gradlew.bat --no-problems-report runNativeSdkTestkitSmoke
.\gradlew.bat --no-problems-report runNativeLoaderProofSmoke runNativeLoaderProofTruthGate
.\gradlew.bat --no-problems-report runNativeDependencyResolutionGate runNativeServiceCollisionGate
```

For full product packaging, use the existing Native Loader product packaging and release-gate tasks in `echo-native-platform/build.gradle`.

## Evidence To Keep

- Packaged descriptor.
- Artifact classpath list.
- Typed mutation receipt report.
- Service registry snapshot.
- Collision check output.
- Side-gate check output.
- Dependency resolution order.
