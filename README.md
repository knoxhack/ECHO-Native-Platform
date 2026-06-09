# ECHO Native Platform

Native runtime, loader, platform services, and adapter support for `.echo-addon` modules.

## Purpose

Native runtime, loader, platform services, and adapter support for `.echo-addon` modules.

## What Lives Here

Gradle platform code, runtime loader code, platform docs, release workflow notes, native compatibility docs, and acceptance evidence.

## Release And Update Role

Owns native platform runtime releases consumed by Native Edition and developer tooling.

## Public Or Private

Public is recommended once native addon developers or testers need source, schemas, and runtime docs. Keep signing/private release keys outside the repo.

## Build And Dev Commands

Run commands from the repository root.

- `.\gradlew.bat build`

## Artifact Ownership

Native platform runtime binaries and platform metadata belong here. Native pack releases belong to `ECHO-Ashfall-Native-Edition`.

## Release Index Product Routing

Runtime update metadata is routed through the canonical Release Index product entry `echo-native-platform`. Run `node scripts/verify-release-index-product.mjs` to audit the indexed product record, add `--check-urls` to prove indexed GitHub artifact URLs are reachable, or add `--strict` in release gates once the entry has approved artifacts. The Gradle gate accepts `-PechoReleaseIndexCheckUrls=true` or `ECHO_RELEASE_INDEX_CHECK_URLS=true` for the same live URL check.

## Docs Index

- [docs/ECHO-PLATFORM-RELEASE-ACCEPTANCE.md](docs/ECHO-PLATFORM-RELEASE-ACCEPTANCE.md)
- [docs/ECHO-RELEASE-WORKFLOWS.md](docs/ECHO-RELEASE-WORKFLOWS.md)
- [docs/echo/implementation/ECHO_NATIVE_CORE_AUDIT.md](docs/echo/implementation/ECHO_NATIVE_CORE_AUDIT.md)
- [docs/echo/native/ADAPTERCORE_SERVICE_GUIDE.md](docs/echo/native/ADAPTERCORE_SERVICE_GUIDE.md)
- [docs/echo/native/API_REFERENCE.md](docs/echo/native/API_REFERENCE.md)
- [docs/echo/native/CLIENT_RENDER_GUIDE.md](docs/echo/native/CLIENT_RENDER_GUIDE.md)
- [docs/echo/native/COMPATIBILITY_MATRIX.md](docs/echo/native/COMPATIBILITY_MATRIX.md)
- [docs/echo/native/CONFIG_GUIDE.md](docs/echo/native/CONFIG_GUIDE.md)
- [docs/echo/native/EVENTS_GUIDE.md](docs/echo/native/EVENTS_GUIDE.md)
- [docs/echo/native/EXAMPLE_ADDON_WALKTHROUGH.md](docs/echo/native/EXAMPLE_ADDON_WALKTHROUGH.md)
- [docs/echo/native/INSTALL_GUIDE.md](docs/echo/native/INSTALL_GUIDE.md)
- [docs/echo/native/KNOWN_LIMITATIONS.md](docs/echo/native/KNOWN_LIMITATIONS.md)
- [docs/echo/native/MOD_AUTHOR_GUIDE.md](docs/echo/native/MOD_AUTHOR_GUIDE.md)
- [docs/echo/native/NATIVE_ADDON_QUICKSTART.md](docs/echo/native/NATIVE_ADDON_QUICKSTART.md)
- [PUBLIC_ALPHA_RELEASE_STATUS.md](PUBLIC_ALPHA_RELEASE_STATUS.md)

## Related Repos

- [knoxhack/ECHO-Launcher](https://github.com/knoxhack/ECHO-Launcher)
- [knoxhack/ECHO-Modules](https://github.com/knoxhack/ECHO-Modules)
- [knoxhack/ECHO-Ashfall-Native-Edition](https://github.com/knoxhack/ECHO-Ashfall-Native-Edition)
- [knoxhack/ECHO-Ashfall-NeoForge-Edition](https://github.com/knoxhack/ECHO-Ashfall-NeoForge-Edition)
- [knoxhack/ECHO-Ashfall-Standalone-Edition](https://github.com/knoxhack/ECHO-Ashfall-Standalone-Edition)
- [knoxhack/ECHO-Release-Index](https://github.com/knoxhack/ECHO-Release-Index)
- [knoxhack/ECHO-Standalone-Runtime](https://github.com/knoxhack/ECHO-Standalone-Runtime)
- [knoxhack/ECHO-SDK](https://github.com/knoxhack/ECHO-SDK)
- [knoxhack/ECHO-Developer-Studio](https://github.com/knoxhack/ECHO-Developer-Studio)
- [knoxhack/ECHO-Addons-Studio](https://github.com/knoxhack/ECHO-Addons-Studio)
- [knoxhack/ECHO-Platform-Website](https://github.com/knoxhack/ECHO-Platform-Website)
