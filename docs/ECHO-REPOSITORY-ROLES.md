# ECHO Repository Roles

**Version:** 2.0.0
**Date:** 2026-06-08

All canonical release and source repositories live under `knoxhack`.

## Application Repositories

| Repo | Visibility | Role | Update feed |
| --- | --- | --- | --- |
| `knoxhack/ECHO-Launcher` | Public | Desktop launcher source, installer builds, launcher self-update releases, and official pack catalog integration. | GitHub Releases on `ECHO-Launcher` |
| `knoxhack/ECHO-Developer-Studio` | Public | Developer operations app source and installer/update releases. | GitHub Releases on `ECHO-Developer-Studio` |
| `knoxhack/ECHO-Addons-Studio` | Public | Addon authoring app source and installer/update releases. | GitHub Releases on `ECHO-Addons-Studio` |
| `knoxhack/ECHO-Platform-Website` | Public | Official website, download hub, and public docs surface. | None |

## Platform Repositories

| Repo | Visibility | Role |
| --- | --- | --- |
| `knoxhack/ECHO-Native-Platform` | Public | Native runtime/platform code, loader contracts, diagnostics, and platform reports. |
| `knoxhack/ECHO-Standalone-Runtime` | Public | Standalone runtime shell/engine used by Ashfall Standalone Edition. |
| `knoxhack/ECHO-SDK` | Public | SDK, schemas, templates, samples, Gradle/plugin tooling, and developer docs. |
| `knoxhack/ECHO-Modules` | Public | Shared module source and per-module artifacts for Native, NeoForge, Standalone, and sources. |
| `knoxhack/ECHO-Release-Index` | Public | Channel JSON, pack descriptors, module release schema, download metadata, and release catalog. |

## Ashfall Edition Repositories

| Repo | Edition | Consumes from `ECHO-Modules` |
| --- | --- | --- |
| `knoxhack/ECHO-Ashfall-Native-Edition` | Ashfall Native Edition | `<module>-<version>.echo-addon` |
| `knoxhack/ECHO-Ashfall-NeoForge-Edition` | Ashfall NeoForge Edition | `<module>-<version>-neoforge.jar` |
| `knoxhack/ECHO-Ashfall-Standalone-Edition` | Ashfall Standalone Edition | `<module>-<version>-standalone.jar` |

Each Ashfall edition owns its pack composition, pack manifest, full zip fallback, release notes, and checksums. `ECHO-Modules` owns the reusable module artifacts.
