# ECHO Updater Feed Contract

**Version:** 1.0.0  
**Date:** 2026-06-07  
**Applies to:** `ECHO-Launcher`, `ECHO-Addons-Studio`, `ECHO-Developer-Studio`

## Version Channels

| Channel | Tag Format | SemVer Prerelease | Target Audience |
|---|---|---|---|
| Stable | `vMAJOR.MINOR.PATCH` | No | All public users. |
| Beta | `vMAJOR.MINOR.PATCH-beta.N` | Yes | Early adopters, testers. |

### Channel Behavior

- `autoUpdater.allowPrerelease = true` allows beta updates.
- `autoUpdater.allowPrerelease = false` blocks beta updates.
- Default: match current app's version (beta app receives beta updates, stable receives stable).

## Branch Policy

| Branch | Purpose | Tag Source |
|---|---|---|
| `main` | Stable release line | Stable tags only |
| `beta` | Beta release line | Beta tags only |
| Internal nightly | Internal testing | Separate internal repo/branch |

### Tagging Rules

1. Tag must match `package.json` version exactly.
2. Tag must be SemVer-compliant.
3. No tag may be moved after release artifacts are published.
4. Beta tags never overwrite stable tags.

## Feed Metadata Requirements

Every release must include:

| File | Required | Platform | Purpose |
|---|---|---|---|
| `latest.yml` | Yes | Windows | Updater manifest (Electron auto-updater). |
| `latest-linux.yml` | Yes | Linux | Linux updater manifest. |
| `latest-mac.yml` | Yes | macOS | macOS updater manifest. |
| `.exe` / `.AppImage` / `.dmg` | Yes | All | Installer binary. |
| `.blockmap` | Yes | All | Delta update map. |
| `RELEASES` | Windows only | Windows | Squirrel.Windows compatibility (if applicable). |

### Manifest Content

Each `latest.yml` must contain:

```yaml
version: X.Y.Z
files:
  - url: ECHO-App-X.Y.Z-Setup.exe
    sha512: <sha512-hash>
    size: <bytes>
    blockMapSize: <bytes>
path: ECHO-App-X.Y.Z-Setup.exe
sha512: <sha512-hash>
releaseDate: '2026-06-07T00:00:00.000Z'
```

## Feed Rules

1. **One feed per app channel.** A single GitHub release page per repo per tag.
2. **No feed sharing.** Public app never reads from private repo; internal app never reads from public repo unless explicitly configured for testing.
3. **Public updates point to public repos only.**
4. **Internal updates point to private/internal repos only.**

## Feed URL Resolution

```typescript
function buildUpdateFeedConfig(app: 'launcher' | 'addon-studio' | 'developer-studio', channel: 'public' | 'internal') {
  const feeds = {
    launcher: {
      public: { provider: 'github', owner: 'knoxhack', repo: 'ECHO-Launcher' }
    },
    'addon-studio': {
      public: { provider: 'github', owner: 'knoxhack', repo: 'ECHO-Addons-Studio' }
    },
    'developer-studio': {
      public: { provider: 'github', owner: 'knoxhack', repo: 'ECHO-Developer-Studio' },
      internal: { provider: 'github', owner: 'knoxhack', repo: 'ECHO-Developer-Studio' }
    }
  };
  return feeds[app][channel];
}
```

## Environment Overrides

For testing or emergency redirection:

| Variable | Effect |
|---|---|
| `ECHO_UPDATE_STREAM` / `ECHO_UPDATE_TARGET` | Force update stream to `public` or `internal` when supported |
| `ECHO_UPDATE_FEED_OWNER` | Override updater feed owner |
| `ECHO_UPDATE_FEED_REPO` | Override updater feed repo |
| `ECHO_UPDATE_ALLOW_PRERELEASE` | Force prerelease updates on/off |
| `ECHO_UPDATES_DISABLED` / `UPDATE_DISABLED` | Kill-switch; disable all update checks |

---

## Schema Version

This contract is version `1.0.0`. Breaking changes require a contract revision and migration window of two stable releases.
