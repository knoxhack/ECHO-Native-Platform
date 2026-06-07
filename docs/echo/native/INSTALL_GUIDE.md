# Install Guide

ECHO Native public beta installs from a packaged product layout, not from a developer workspace.

## Requirements

- Java 25.
- The ECHO Native Loader package for the same SDK version as the addons.
- Packaged addon jars with `META-INF/echo.mod.json` descriptors.
- A product profile that lists the native module classpath explicitly.

## Install Flow

1. Unpack the native loader runtime package.
2. Copy addon jars into the product profile `modules` or `mods` directory chosen by the package.
3. Verify every addon jar contains `META-INF/echo.mod.json`.
4. Start through the Native Product Launcher entrypoint.
5. Review the generated release/readiness reports before joining a world.

Do not launch public beta builds from Gradle class outputs, IDE classpaths, or local `build/classes` directories.

## Verification

Run:

```powershell
.\gradlew.bat generatePlan3ReleasePrepReport
.\gradlew.bat checkPlan3ReleasePrep
```

`checkPlan3ReleasePrep` is expected to fail until release artifacts, checksums, SBOM, docs, package evidence, and RC smoke evidence are all present.
