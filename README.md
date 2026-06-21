# ECHO Native Platform

Native runtime, loader, platform services, and adapter support for `.echo-addon` modules.

## Purpose

Native runtime, loader, platform services, and adapter support for `.echo-addon` modules.

## What Lives Here

Gradle platform code, runtime loader code, platform docs, release workflow notes, native compatibility docs, `.ECHO Content Graph` planning evidence, and acceptance evidence.

## Release And Update Role

Owns native platform runtime releases consumed by Native Edition and developer tooling.

## Current Release Line

The active release line is `1.0.0-RC1`. Local build/check and external addon proof are green, and the loader/runtime ZIP is checksum-indexed in the Release Index as a warning-gated RC asset. It is not a stable `1.0.0` or launcher-approved full release until GitHub upload/download-back, signing or attestation, launcher install/rollback/diagnostics, and real pack gameplay evidence pass.

Use `docs/echo/native/RELEASE_CANDIDATE_CHECKLIST.md` as the promotion contract.

## Public Or Private

Public is recommended once native addon developers or testers need source, schemas, and runtime docs. Keep signing/private release keys outside the repo.

## Build And Dev Commands

Run commands from the repository root.

- `.\gradlew.bat build`
- `node scripts/generate-ashfall-native-code-gate.mjs`
- `node scripts/test-generate-ashfall-native-code-gate.mjs`
- `node scripts/generate-ashfall-native-public-beta-evidence.mjs`
- `node scripts/test-generate-ashfall-native-public-beta-evidence.mjs`
- `node scripts/generate-ashfall-gameplay-qa-evidence.mjs`
- `node scripts/test-generate-ashfall-gameplay-qa-evidence.mjs`
- `.\gradlew.bat check packageNativePlatformLayout packagePublicAlphaRelease`

## Content Graph Planning

`echo-native-loader` includes `EchoNativeContentGraphPlanner` and `EchoNativeGraphPlanner`. During a dry-run plan, the loader reads per-module `content-graph.json`, `features.json`, `export-plans/standalone_engine.json`, and `export-plans/hytale.json` files from the directory configured by the `echo.content.graph.root` system property (`<root>/<module>/<version>/.echo/content-graph/content-graph.json`) and produces an `echo.content_graph.evidence.v1` summary of module count, node count, edge count, feature count, export-plan count, Hytale blocker count, and diagnostics. This is optional evidence: a missing graph emits a warning but does not block native loading.

Unified ECHO Native player runtime conformance is pack-neutral. Native Platform owns host services and evidence; modules own menus, HUDs, input, inventory, terminal, index, diagnostics, and gameplay action contracts. Ashfall-named evidence remains fixture coverage, not platform ownership.

`echo-native-cli` writes the content graph evidence to `content-graph.json` alongside module, feature, service, and lifecycle plans when running QA graph reports.

Use `.\gradlew.bat runNativeContentGraphEvidenceGate -PechoModulesRepoRoot=..\ECHO-Modules --console=plain` after generating ECHO-Modules release outputs to prove the native planner can read the canonical graph release tree.

## Artifact Ownership

Native platform runtime binaries and platform metadata belong here. Native pack releases belong to `ECHO-Ashfall-Native-Edition`.

The Native Platform release owns two runtime-facing artifacts for RC1:

- `echo-native-platform-1.0.0-RC1.zip` is the launcher-facing platform package.
- `echo-native-loader-1.0.0.jar` is the direct Native Loader library for developers and users who do not use ECHO Launcher.

`native-loader-direct-install.json` declares the direct jar as `artifactRole: native-loader-library` with `manualInstall: true`. The jar is not a `.echo-addon`, not pack content, and not a module dependency. Pack/module files still ship from pack or module releases.

## Release Index Product Routing

Runtime update metadata is routed through the canonical Release Index product entry `echo-native-platform`. Run `node scripts/verify-release-index-product.mjs` to audit the indexed product record, add `--check-urls` to prove indexed GitHub artifact URLs are reachable, or add `--strict` in release gates once the entry has approved artifacts. The Gradle gate accepts `-PechoReleaseIndexCheckUrls=true` or `ECHO_RELEASE_INDEX_CHECK_URLS=true` for the same live URL check.

`scripts/generate-ashfall-native-code-gate.mjs` writes the Phase 5 Ashfall Native code gate evidence consumed by the Release Index readiness gate. It runs `gradlew check`, records the command output tail, and only passes when the command exits successfully. After Gradle finishes, it refreshes the Phase 7/8 reducer outputs so older Gradle-generated placeholder reports do not overwrite the Release Index-facing evidence files.

`scripts/generate-ashfall-gameplay-qa-evidence.mjs` writes the Phase 8 Ashfall gameplay QA evidence consumed by the Release Index readiness gate. It fails closed unless upstream Native tester reports are current, non-dry-run, and passing, and unless `fixtures/ashfall/gameplay-qa/manual-evidence.json` proves first-hour client play, fresh world creation, save/reload, route, dedicated server, server/client export, ending, and no-crash checks with supporting files.

`scripts/generate-ashfall-native-public-beta-evidence.mjs` writes the Phase 7 Ashfall Native public beta reports consumed by the Release Index readiness gate: `native-loader-beta-session-proof-matrix.json`, `native-loader-beta-crash-intake.json`, and `public-beta-tester-package-readiness.json`. It fails closed unless `fixtures/ashfall/native-public-beta/manual-evidence.json` cites three clean internal beta sessions with logs and notes, no-crash crash review evidence, a checksum-verified tester package, support runbook, rollback plan, and published limitations.

`packagePublicAlphaRelease` emits the RC1 Native Platform loader/runtime ZIP under `build/public-alpha/echo-native-platform-1.0.0-RC1.zip`. The archive must not contain `echo.pack.json`, `echo-native-product-package.json`, `modules/`, pack-specific metadata, or embedded module jars. Pack content ships from pack-owned releases such as `ECHO-Ashfall-Native-Edition`.

Use `fixtures/ashfall/RELEASE_EVIDENCE_RUNBOOK.md` plus the `manual-evidence.template.json` files under `fixtures/ashfall/native-public-beta/` and `fixtures/ashfall/gameplay-qa/` when collecting real Phase 7/8 evidence. The templates are intentionally report-only placeholders and must not be treated as passing evidence.

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
- [docs/echo/native/RELEASE_CANDIDATE_CHECKLIST.md](docs/echo/native/RELEASE_CANDIDATE_CHECKLIST.md)
- [docs/echo/native/UNIFIED_PLAYER_RUNTIME.md](docs/echo/native/UNIFIED_PLAYER_RUNTIME.md)
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
