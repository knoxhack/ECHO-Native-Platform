# ECHO Native 1.0.0-RC1 Release Checklist

This checklist is the release-candidate contract for promoting ECHO Native toward a full 1.0.0 release. A stable release must not be announced until every hard gate is green from committed sources and workflow-built artifacts.

## Release Identity

| Field | Value |
| --- | --- |
| Release line | `1.0.0-RC1` |
| Stable target | `1.0.0` after all gates pass |
| Runtime artifact | `echo-native-platform-1.0.0-RC1.zip` loader/runtime archive only |
| Native Loader launcher library | `echo-native-loader-1.0.0.jar` |
| Addon artifact | `<module>-<version>.echo-addon` |
| Descriptor | `META-INF/echo.mod.json` |
| Launcher channel | RC/pre-release until stable gates pass |
| Supported OS for RC1 | Windows 10/11 desktop |
| Conditional OS support | Linux and macOS only after CI evidence passes |

## Public Addon API

Addon authors may compile against:

- `dev.echo.native:echo-native-contracts:1.0.0-RC1`
- `dev.echo.native:echoaddonapi:1.0.0-RC1`
- `dev.echo.native:echoadaptercore:1.0.0-RC1`
- `dev.echo.native:echo-native-testkit:1.0.0-RC1` in test source sets only
- `dev.echo.native:echo-sdk-gradle-plugin:1.0.0-RC1`

`echo-native-loader` and all loader implementation packages remain internal.

## Required Gates

- `.\gradlew.bat check`
- `.\gradlew.bat runNativeDependencyResolutionGate runNativeServiceCollisionGate runNativeMutationTruthGate`
- `.\gradlew.bat runNativeLoaderProofSmoke runNativeLoaderProofTruthGate runNativeLegacyAdapterBridgeSmoke`
- `python ..\ECHO-SDK\tools\validate_echo_sdk_templates.py`
- ECHO Modules native compatibility matrix has no `ready` module backed by local build output fallback.
- Release Index marks player installs locked while pack readiness is `warning` or `blocked`.
- RC1 artifact has checksums, source jars, Javadocs, provenance or attestation, release notes, and rollback instructions.
- Native Platform runtime archive contains no `echo.pack.json`, no `echo-native-product-package.json`, no `modules/`, and no pack-owned module jars.

## Stable Release Blockers

- Missing Linux/macOS CI evidence if those OSes are advertised.
- Any Native-first SDK template imports NeoForge, Forge, Fabric, or `echo-native-loader`.
- Any module marked ready depends on `local_build_output_classpath_fallback`, `source-packaged`, or `--allow-missing-runtime` output.
- Any pack catalog record remains `warning`, `blocked`, `alpha`, or `source-linked` for stable artifacts.
- Gameplay/install evidence is placeholder, dry-run, or locally inferred instead of produced by real tester runs.
