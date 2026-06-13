# ECHO Native Release Status

The active release line is `1.0.0-RC1`.

This repository has locally built and checksum-indexed RC1 product and Native Loader launcher artifacts, but it is not approved as a stable or player-facing full release yet. The Release Index keeps `echo-native-platform` warning-gated until the RC1 assets are uploaded to the GitHub release, downloaded back, checksum-verified, signed or attested, launcher-installed, rollback-tested, and backed by real pack gameplay evidence.

Public claims should use "release candidate" or "beta/RC" language until those gates pass. Stable `1.0.0` is blocked while any catalog metadata remains `warning`, `blocked`, `alpha`, or `source-linked` for stable-target artifacts.

Current local artifact:

- `build/public-alpha/echo-native-product-1.0.0-RC1.zip`
- `build/public-alpha/native-loader-1.0.0.jar`
- SHA-256 values are recorded in `build/public-alpha/checksums.txt` for the current local build.

Canonical gate document:

- `docs/echo/native/RELEASE_CANDIDATE_CHECKLIST.md`
