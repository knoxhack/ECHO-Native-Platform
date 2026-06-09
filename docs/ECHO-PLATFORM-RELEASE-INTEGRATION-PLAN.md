# ECHO Platform Release/Update Integration Plan

**Version:** 1.0.0  
**Date:** 2026-06-07  
**Status:** Implemented

## Summary

This plan formalizes the updater contract for all ECHO platform apps while keeping private internals separate from public update feeds. It ensures:

- Every public app user update comes from a public repo release feed.
- Every internal app update comes from an internal feed unless explicitly configured to test public updates.
- `echo-sdk` remains non-updater, API contract-only.
- `echo-core-internal` and internal launcher internals never appear in public updater artifacts.
- Two release cycles complete with no accidental cross-feed updates.
- One predictable, non-ambiguous release process for all app repos.

---

## 1. Repository Role Map

| Repository | Visibility | Role | Update Feed | Notes |
|---|---|---|---|---|
| `echo-launcher` | Public | Public launcher app | Public GitHub Releases | Already wired; verified. |
| `ECHO-Addons-Studio` | Public | Public addon creator studio | Public GitHub Releases | Fully implemented end-to-end; stream handling and release validation are in place. |
| `echo-sdk` | Public | API/contracts only | None | No updater; versioned independently. |
| `echo-core-internal` | Private | Core engine/modpack builder | None (internal tag-based artifact flow) | Never in public app artifacts. |
| `ECHO-Launcher` | Private | Internal launcher variants | Private feed | Internal-only update stream. |
| `ECHO-Addons-Studio` | Private | Internal addon tooling | Private feed | Not exposed publicly. |
| `ECHO-Developer-Studio` | Private | Source repo for Developer Studio | None (builds publish elsewhere) | Source stays private. |
| `ECHO-Developer-Studio` | Public | **Updates-only companion repo** | Public GitHub Releases | No source; only release artifacts for installer updates. |
| `echo-release-index` (optional) | Public | Central metadata index | None | Version pointers + hashes; not required for MVP. |

---

## 2. Updater Feed Contract

### Version Channels

| Channel | Format | Meaning |
|---|---|---|
| Stable | `vMAJOR.MINOR.PATCH` | Production-ready release. |
| Beta | `vMAJOR.MINOR.PATCH-beta.N` | Pre-release for early adopters. |

### Branch Policy

| Branch | Purpose |
|---|---|
| `main` | Stable release line. Tags pushed here produce stable releases. |
| `beta` | Beta release line. Tags pushed here produce beta releases. |
| Internal nightly | Separate internal repo/branch; never referenced in public docs. |

### Feed Rules

1. **One feed per app channel.** No feed sharing across public vs internal.
2. **Public updates point to public repos only.**
3. **Internal updates point to private/internal repos only.**
4. **Feed metadata must include:**
   - `latest.yml` (Windows)
   - `latest-linux.yml` (Linux)
   - Installer binaries (.exe, .AppImage)
   - `.blockmap` files

---

## 3. App-Specific Updater Implementation

### ECHO Launcher (Public)

**Current state:** Already implemented.

- `package.json` `build.publish` points to `knoxhack/ECHO-Launcher`.
- Tag format: `v*.*.*`.
- Workflow: `.github/workflows/release.yml`.
- Kill-switch: `UPDATE_DISABLED` env var.
- Platform support: Windows (NSIS), Linux (AppImage).

### ECHO Addon Studio (Public)

**Current state:** Implemented.

- Has `electron-updater` in main process.
- Feed points to `knoxhack/ECHO-Addons-Studio`.
- Tag format: `v*.*.*` and `v*.*.*-*`.
- Feed handling, release workflow, and manifest evidence checks are implemented.

### ECHO Developer Studio (Private Source -> Public Updates)

**Current state:** Already implemented with dual config.

- Internal build: `electron-builder.yml` -> `knoxhack/ECHO-Developer-Studio` (internal)
- Public build: `electron-builder.public.yml` -> `knoxhack/ECHO-Developer-Studio` (public updates-only)
- Kill-switch: `ECHO_UPDATES_DISABLED` / `UPDATE_DISABLED`.
- Fallback feed: Already implemented in `main.ts`.

---

## 4. Cross-Repo Migration Compatibility

If any app has hardcoded old feed URLs:

1. App tries old feed first, then new.
2. Telemetry marker emitted when fallback occurs.
3. After two stable releases with new feed, remove fallback.

**Current assessment:** No legacy feed URLs found in active apps. Fallback code exists in Developer Studio as defensive measure.

---

## 5. Security and Governance

### Secrets Policy

- Signing keys and GH tokens live in **private repo secrets only**.
- Public repos use `GITHUB_TOKEN` (auto-scoped, `contents: write`).
- No PATs with broad scope in public workflows.

### Workflow Permissions

```yaml
permissions:
  contents: write
```

No broader permissions. No `id-token: write` unless OIDC specifically required.

### Rollback Policy

1. Mark bad release with `UPDATE_DISABLED=true` env var in affected app.
2. Publish corrected hotfix on same channel immediately.
3. Update release notes to indicate superseded version.

### Logging

Updater errors logged with:
- App version
- Target repo
- Channel
- Checksum failure details

---

## 6. Test Plan

See `docs/ECHO-PLATFORM-RELEASE-TEST-PLAN.md`.

## 7. Acceptance Criteria

| # | Criteria | Verification |
|---|---|---|
| 1 | Every public app update from public repo | Check `build.publish` in each app's electron-builder config |
| 2 | Every internal app update from internal feed | Check private repo configs |
| 3 | `echo-sdk` has no updater | Verify no `electron-updater` dependency |
| 4 | `echo-core-internal` never in public artifacts | Verify no core-internal files in public release zips |
| 5 | Two release cycles, no cross-feed leaks | Audit release manifests across two tags |
| 6 | One predictable process | All app repos use same tag validation, same CI structure |

---

## 8. Related Documents

- `docs/ECHO-REPOSITORY-ROLES.md` — Detailed role map with ownership.
- `docs/ECHO-UPDATER-FEED-CONTRACT.md` — Feed schema and version contract.
- `docs/ECHO-APP-UPDATER-IMPLEMENTATION.md` — Code patterns for each app.
- `docs/ECHO-RELEASE-WORKFLOWS.md` — CI/CD workflow templates.
- `docs/ECHO-RELEASE-SECURITY.md` — Secrets, signing, rollback policy.
- `docs/ECHO-PLATFORM-RELEASE-TEST-PLAN.md` — Test plan and acceptance verification.
