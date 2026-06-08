# ECHO App Updater Implementation

**Version:** 1.0.0  
**Date:** 2026-06-07

## Shared Patterns

All ECHO Electron apps use `electron-updater` with the following shared patterns:

### 1. Kill-Switch

```typescript
function isUpdateDisabled(): boolean {
  const disable = process.env['ECHO_UPDATES_DISABLED'] || process.env['UPDATE_DISABLED'];
  return disable === '1' || (disable || '').toLowerCase() === 'true';
}
```

If `isUpdateDisabled()` returns `true`, skip all update initialization.

### 2. Feed URL Resolution

```typescript
autoUpdater.setFeedURL({
  provider: 'github',
  owner: resolveUpdateFeedOwner(),
  repo: resolveUpdateFeedRepo(),
  releaseType: 'release'
});
```

### 3. Event Handling

| Event | Renderer IPC | UI Action |
|---|---|---|
| `checking-for-update` | `{ status: 'checking' }` | Show spinner |
| `update-available` | `{ status: 'available', version }` | Show update prompt |
| `update-not-available` | `{ status: 'not-available' }` | Hide update UI |
| `download-progress` | `{ status: 'downloading', percent }` | Show progress bar |
| `update-downloaded` | `{ status: 'downloaded', version }` | Show restart button |
| `error` | `{ status: 'error', message }` | Show error + manual fallback link |

### 4. Manual Fallback

On update failure, the renderer must display:
- Error message
- Link to manual download page
- Instructions to replace the app manually

### 5. Install Trigger

```typescript
ipcMain.on('update:install', () => {
  autoUpdater.quitAndInstall(false, true);
});
```

---

## Per-App Implementation

### ECHO Launcher

**File:** `electron/main.cjs`

Already implemented with:
- `LAUNCHER_UPDATE_OWNER = 'knoxhack'`
- `LAUNCHER_UPDATE_REPO = 'ECHO-Launcher'`
- Platform-gated updates (Windows NSIS, Linux AppImage only)
- `autoUpdater.autoDownload = false` (user must confirm)
- `autoUpdater.autoInstallOnAppQuit = false`

**Verification:** `package.json` `build.publish` matches constants in `main.cjs`.

### ECHO Addon Studio

**File:** `src/main/index.ts`

Current state: Basic updater with `knoxhack/ECHO-Addons-Studio` feed.

**Missing:** `package.json` does not have `build.publish` block for CI alignment.

**Required change:** Add `build.publish` to `package.json` so `electron-builder --publish always` targets the correct repo.

### ECHO Developer Studio

**File:** `src/main.ts`

Already implemented with:
- Internal feed: `knoxhack/ECHO-Developer-Studio`
- Public feed: `knoxhack/ECHO-Developer-Studio`
- Fallback mechanism: primary fails -> try fallback
- `resolveTargetChannel()` for channel selection
- `buildUpdateFeedConfig()` / `buildFallbackUpdateFeedConfig()`

**Verification:** `electron-builder.yml` publishes to internal; `electron-builder.public.yml` publishes to public companion.

---

## Startup Update UX

All apps must implement this flow on startup:

```
App launches
  -> If UPDATE_DISABLED: skip
  -> If not packaged (dev): skip
  -> Check for updates
    -> If available: notify user
      -> User clicks "Download"
        -> Show progress
        -> On complete: show "Restart to install"
          -> User clicks "Restart" -> quitAndInstall
      -> Or user dismisses -> update on next launch
    -> If not available: silent
    -> If error: show manual fallback link
```

---

## Channel Override for Testing

Internal builds can test public updates by setting:

```bash
ECHO_UPDATE_FEED_OWNER=echolabs
ECHO_UPDATE_FEED_REPO=ECHO-Developer-Studio
```

This is useful for verifying the public feed before releasing to users.
