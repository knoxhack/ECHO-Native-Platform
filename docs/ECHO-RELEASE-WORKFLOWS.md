# ECHO Release Workflows

**Version:** 1.0.0  
**Date:** 2026-06-07

## Shared Workflow Structure

All app release workflows follow this pattern:

```yaml
name: <App> Release

on:
  push:
    tags:
      - '<tag-pattern>'
  workflow_dispatch:
    inputs:
      release_tag:
        description: 'Release tag to publish'
        required: false
        type: string

permissions:
  contents: write

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - validate tag matches package.json version
      - run tests
      - run typecheck / lint
  build-and-publish:
    needs: validate
    strategy:
      matrix:
        os: [windows-latest, ubuntu-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - checkout
      - setup node
      - install dependencies
      - build app
      - publish with electron-builder --publish always
      - upload build evidence artifacts
```

## ECHO Launcher

**File:** `ECHO-Launcher/.github/workflows/release.yml`

| Field | Value |
|---|---|
| Tag pattern | `v*.*.*` |
| Platforms | Windows (NSIS), Linux (AppImage) |
| Publish target | `knoxhack/ECHO-Launcher` |
| Pre-publish checks | Tag format/version validation, tests, perf budget, placeholder audit |
| Post-publish checks | Installer artifact presence, updater manifest presence, SHA-256 evidence |

**Additional launcher publishing entrypoint:**  
`ECHO-Launcher/.github/workflows/publish-launcher.yml` mirrors the same hardening and is used by manual lane control.

## ECHO Addon Studio

**File:** `ECHO-Addons-Studio/.github/workflows/release.yml`

| Field | Value |
|---|---|
| Tag pattern | `v*.*.*`, `v*.*.*-*` |
| Platforms | Windows (NSIS) |
| Publish target | `knoxhack/ECHO-Addons-Studio` |
| Pre-publish checks | Package-tag validation, build/publish execution, release evidence checks, checksum generation |

### Required Alignment

Add `build.publish` block to `ECHO-Addons-Studio/package.json`:

```json
{
  "build": {
    "appId": "com.echoplatform.addon-studio",
    "productName": "ECHO Addon Studio",
    "directories": {
      "output": "release"
    },
    "publish": [
      {
        "provider": "github",
        "owner": "knoxhack",
        "repo": "ECHO-Addons-Studio",
        "releaseType": "release"
      }
    ]
  }
}
```

## ECHO Developer Studio

**File:** `ECHO-Developer-Studio/.github/workflows/release.yml`

| Field | Value |
|---|---|
| Tag pattern | `v*.*.*`, `v*.*.*-*` |
| Platforms | Windows (NSIS, portable, zip), Linux (AppImage, deb, rpm), macOS (dmg) |
| Internal publish target | `knoxhack/ECHO-Developer-Studio` |
| Public publish target | `knoxhack/ECHO-Developer-Studio` |
| Pre-publish checks | Tests, typecheck, lint |

### Dual Publish Config

The workflow already supports `internal`, `public`, and `both` targets via `workflow_dispatch`.

On tag push, only the internal track auto-publishes. Public track requires manual `workflow_dispatch` with `publish_target: public` or `both`.

---

## Validation Steps

All workflows must:

1. **Tag validation:** Tag version must match `package.json` version exactly.
2. **Build artifacts:** All expected installers and updater manifests must be present.
3. **Manifest validation:** `latest*.yml` files must exist and reference correct installer filename.
4. **Artifact upload:** Build evidence uploaded as workflow artifacts even on publish success.

## Emergency Hotfix Workflow

For critical fixes that bypass normal release checks:

1. Create hotfix branch from release tag.
2. Apply minimal fix.
3. Tag with incremented patch version.
4. Run workflow with `workflow_dispatch`.
5. Verify `UPDATE_DISABLED` is not set in release notes.
6. Monitor for 24 hours before announcing.
