# Install Guide

ECHO Native `1.0.0-RC1` installs from a packaged product layout, not from a developer workspace. The local RC1 artifact exists, but public launcher approval remains blocked until the Release Index download, attestation/signing, launcher install, rollback, diagnostics, and gameplay evidence gates pass.

## Requirements

- Java 25.
- The ECHO Native Loader package for the same SDK version as the addons.
- Packaged `.echo-addon` files with embedded `META-INF/echo.mod.json` descriptors.
- A product profile that lists the native module classpath explicitly.

## Install Flow

1. Unpack the native loader runtime package.
2. Install `.echo-addon` files into the product profile `modules` directory or the launcher-managed addon store.
3. Verify every addon package contains `META-INF/echo.mod.json` and an `addon.jar` payload.
4. Start through the Native Product Launcher entrypoint.
5. Review the generated release/readiness reports before joining a world.

Do not launch RC or public builds from Gradle class outputs, IDE classpaths, inferred classpath tokens, or local `build/classes` directories.

## Verification

Run:

```powershell
.\gradlew.bat generatePlan3ReleasePrepReport
.\gradlew.bat checkPlan3ReleasePrep
```

`checkPlan3ReleasePrep` is expected to fail for stable promotion until release artifacts, checksums, SBOM, docs, package evidence, RC smoke evidence, public download-back evidence, and launcher/gameplay proof are all present.
