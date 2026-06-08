# ECHO Platform Release Test Plan

**Version:** 1.0.0  
**Date:** 2026-06-07

## Test 1: ECHO Addon Studio Public Stable Flow

### Objective

Verify that a stable tag on `ECHO-Addons-Studio` produces a working public update.

### Steps

1. Tag `v0.2.0` on `ECHO-Addons-Studio` `main` branch.
2. Trigger `.github/workflows/release.yml`.
3. Verify workflow passes.
4. Download installer from GitHub Releases page.
5. Install fresh.
6. Verify `latest.yml` exists in release assets.
7. Tag `v0.2.1`.
8. Launch installed app.
9. Verify updater detects `v0.2.1`.
10. Download and install update.
11. Verify app restarts on `v0.2.1`.

### Expected Results

- Fresh install works.
- Updater manifest exists.
- In-app update to new stable version works.

### Acceptance Criteria

- [ ] Tag build succeeds.
- [ ] Installer downloads and runs.
- [ ] Updater manifest is valid YAML.
- [ ] Auto-update from `v0.2.0` to `v0.2.1` succeeds.

---

## Test 2: ECHO Addon Studio Beta Flow

### Objective

Verify beta channel isolation.

### Steps

1. Tag `v0.3.0-beta.1` on `beta` branch.
2. Install stable `v0.2.0` app.
3. Verify stable app does **not** see `v0.3.0-beta.1`.
4. Install beta app (previously opted into beta).
5. Verify beta app sees `v0.3.0-beta.1`.

### Expected Results

- Stable users remain on stable.
- Beta users receive beta updates.

### Acceptance Criteria

- [ ] Beta tag only updates beta-channel users.
- [ ] Stable app ignores beta releases.

---

## Test 3: ECHO Developer Studio Internal -> Public Update Stream

### Objective

Verify internal source build publishes to public companion repo correctly.

### Steps

1. Push tag `v0.1.0` to `ECHO-Developer-Studio`.
2. Trigger release workflow with `publish_target: both`.
3. Verify internal track publishes to `knoxhack/ECHO-Developer-Studio`.
4. Verify public track publishes to `knoxhack/ECHO-Developer-Studio`.
5. Download public installer from `ECHO-Developer-Studio` releases.
6. Install and check for updates.

### Expected Results

- Internal CI runs on private repo.
- Public release artifacts appear on public repo.
- No source code leaked to public repo.
- Public user receives update from public repo.

### Acceptance Criteria

- [ ] Public repo contains only release artifacts (no source).
- [ ] Internal CI build logs remain private.
- [ ] Public updater fetches from `knoxhack/ECHO-Developer-Studio`.

---

## Test 4: Access Control Verification

### Objective

Verify public/private separation.

### Steps

1. As unauthenticated user, browse `knoxhack/ECHO-Launcher` releases.
2. As unauthenticated user, browse `knoxhack/ECHO-Addons-Studio` releases.
3. As unauthenticated user, browse `knoxhack/ECHO-Developer-Studio` releases.
4. Attempt to access `echo-core-internal` (should 404 or require auth).
5. Attempt to access `ECHO-Developer-Studio` (should 404 or require auth).
6. Inspect public release artifacts for internal code references.

### Expected Results

- All public repos readable without auth.
- All private repos inaccessible without auth.
- Public artifacts contain no internal code paths or secrets.

### Acceptance Criteria

- [ ] External contributor can download public releases.
- [ ] No external access to private repos.
- [ ] Public release zips do not contain `echo-core-internal` files.

---

## Test 5: Negative Cases

### Tampered Asset Hash

1. Modify one byte in published installer.
2. Attempt to install via updater.

**Expected:** Updater rejects download; shows manual fallback.

### Network Failure During Download

1. Start update download.
2. Disconnect network mid-download.

**Expected:** Error state with retry and manual fallback options.

### Down-Versioned Tag

1. Tag `v0.1.0` after `v0.2.0` is published.

**Expected:** Updater ignores down-versioned release; stays on latest.

### Wrong Channel Tag

1. Push stable tag to beta branch.

**Expected:** Updater respects `allowPrerelease` setting; stable users may or may not see it depending on config, but no cross-channel auto-install.

---

## Acceptance Criteria Summary

| # | Criteria | Test |
|---|---|---|
| 1 | Every public app update from public repo | Tests 1, 2, 3 |
| 2 | Every internal app update from internal feed | Test 3 |
| 3 | `echo-sdk` has no updater | Code review |
| 4 | `echo-core-internal` never in public artifacts | Test 4 |
| 5 | Two release cycles, no cross-feed leaks | Tests 1, 2, 3 over two releases |
| 6 | One predictable process | All workflows use same validation pattern |
