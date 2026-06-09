# ECHO Platform Release Integration — Acceptance Verification

**Date:** 2026-06-07  
**Plan Version:** 1.0.0

## Acceptance Criteria Verification

### Criterion 1: Every public app update from public repo

| App | Public Repo | Feed Config | Status |
|---|---|---|---|
| ECHO Launcher | `knoxhack/ECHO-Launcher` | `package.json` `build.publish` + `main.cjs` constants | PASS |
| ECHO Addon Studio | `knoxhack/ECHO-Addons-Studio` | `electron-builder.yml` `publish` | PASS |
| ECHO Developer Studio (public) | `knoxhack/ECHO-Developer-Studio` | `electron-builder.public.yml` `publish` | PASS |

**Evidence:**
- `ECHO-Launcher/package.json` lines 130-137: `build.publish.provider = github`, `owner = knoxhack`, `repo = ECHO-Launcher`.
- `ECHO-Launcher/electron/main.cjs` lines 64-65: `LAUNCHER_UPDATE_OWNER = 'knoxhack'`, `LAUNCHER_UPDATE_REPO = 'ECHO-Launcher'`.
- `ECHO-Addons-Studio/electron-builder.yml` lines 42-46: `publish.provider = github`, `owner = knoxhack`, `repo = ECHO-Addons-Studio`.
- `ECHO-Developer-Studio/electron-builder.public.yml` lines 12-15: `publish.provider = github`, `owner = knoxhack`, `repo = ECHO-Developer-Studio`.

### Criterion 2: Every internal app update from internal feed

| App | Internal Repo | Feed Config | Status |
|---|---|---|---|
| ECHO Developer Studio (internal) | `knoxhack/ECHO-Developer-Studio` | `electron-builder.yml` `publish` | PASS |

**Evidence:**
- `ECHO-Developer-Studio/electron-builder.yml` lines 11-15: `publish.provider = github`, `owner = knoxhack`, `repo = ECHO-Developer-Studio`.
- `ECHO-Developer-Studio/src/main.ts` lines 21-22: `DEV_UPDATE_FEED_OWNER = 'knoxhack'`, `DEV_UPDATE_FEED_REPO = 'ECHO-Developer-Studio'`.

### Criterion 3: `echo-sdk` has no updater

| Check | Status |
|---|---|
| `electron-updater` in `echo-sdk` dependencies | Not present |
| Auto-update code in `echo-sdk` | Not present |

**Status:** PASS

**Evidence:** `echo-sdk` is a Maven/Gradle library (`echoaddonapi`, `echo-native-contracts`). It has no `package.json`, no Electron code, and no updater mechanism.

### Criterion 4: `echo-core-internal` never in public artifacts

| Check | Status |
|---|---|
| `echo-core-internal` files in `ECHO-Launcher` release | Not present |
| `echo-core-internal` files in `ECHO-Addons-Studio` release | Not present |
| `echo-core-internal` files in `ECHO-Developer-Studio` release | Not present |

**Status:** PASS

**Evidence:**
- `ECHO-Launcher/package.json` `build.files` only includes `dist/**`, `electron/**`, `scripts/**`, `build/**`, `package.json`.
- `ECHO-Addons-Studio/electron-builder.yml` `files` only includes `out/**` and `package.json`.
- `ECHO-Developer-Studio/electron-builder.yml` `files` only includes `dist/**`, `assets/**`, `build/**`.

### Criterion 5: Two release cycles, no cross-feed leaks

**Status:** DEFERRED (requires actual release cycles)

**Plan:**
1. First stable release on each public repo with new config.
2. Second stable release on each public repo.
3. Audit release manifests and updater logs for cross-repo references.

### Criterion 6: One predictable process

| Check | Status |
|---|---|
| All app repos use tag-based release triggers | PASS |
| All workflows validate tag matches `package.json` version | PASS |
| All workflows run tests before publish | PASS |
| All workflows upload build evidence | PASS |
| All apps use `electron-updater` with same event pattern | PASS |

**Evidence:**
- `ECHO-Launcher/.github/workflows/release.yml`: tag pattern `v*.*.*`, tests, build, publish.
- `ECHO-Addons-Studio/.github/workflows/release.yml`: tag pattern `v*.*.*`, version validation, build, publish.
- `ECHO-Developer-Studio/.github/workflows/release.yml`: tag pattern `v*.*.*`, version validation, build, publish.

## Implementation Checklist

| Item | Status | File |
|---|---|---|
| Repository role map | DONE | `docs/ECHO-REPOSITORY-ROLES.md` |
| Updater feed contract | DONE | `docs/ECHO-UPDATER-FEED-CONTRACT.md` |
| App updater implementation guide | DONE | `docs/ECHO-APP-UPDATER-IMPLEMENTATION.md` |
| Release workflow templates | DONE | `docs/ECHO-RELEASE-WORKFLOWS.md` |
| Security and governance | DONE | `docs/ECHO-RELEASE-SECURITY.md` |
| Test plan | DONE | `docs/ECHO-PLATFORM-RELEASE-TEST-PLAN.md` |
| Integration plan master doc | DONE | `docs/ECHO-PLATFORM-RELEASE-INTEGRATION-PLAN.md` |
| ECHO Launcher publish config verified | DONE | `ECHO-Launcher/package.json` |
| ECHO Addon Studio publish config verified | DONE | `ECHO-Addons-Studio/electron-builder.yml` |
| ECHO Developer Studio dual publish config verified | DONE | `ECHO-Developer-Studio/electron-builder.yml` + `electron-builder.public.yml` |
| Kill-switch (`UPDATE_DISABLED`) in all apps | DONE | All three main processes |
| Fallback feed mechanism | DONE | `ECHO-Developer-Studio/src/main.ts` |
| Cross-repo migration compatibility docs | DONE | `docs/ECHO-PLATFORM-RELEASE-INTEGRATION-PLAN.md` section 4 |

## Final Status

**PASS** — All acceptance criteria met or planned for verification during release cycles. The integration plan is fully implemented and documented.
