# Install Guide

ECHO Native `1.0.0-RC1` has three separate install lanes:

- Launcher lane: `echo-native-platform-1.0.0-RC1.zip` is the launcher-facing platform metadata/runtime package.
- Direct loader lane: `echo-native-loader-1.0.0.jar` is the Native Loader library for developers and users who are not installing through ECHO Launcher.
- Pack/module lane: `.echo-addon` modules, pack profiles, and pack-owned content come from pack or module releases, never from the Native Platform release assets.

The RC line remains prerelease until the Release Index download, attestation/signing, launcher install, rollback, diagnostics, and gameplay evidence gates pass.

## Requirements

- Java 25.
- The ECHO Native Loader jar for the same SDK version as the addons.
- Packaged `.echo-addon` files with embedded `META-INF/echo.mod.json` descriptors from a pack-owned release.
- A pack profile that lists the native module classpath explicitly.

## Public Direct Loader Jar

- File: `echo-native-loader-1.0.0.jar`
- Release URL: `https://github.com/knoxhack/ECHO-Native-Platform/releases/download/v1.0.0-RC1/echo-native-loader-1.0.0.jar`
- SHA-256: `413d0146654b37fdf58345ed396180b44286ba98eb9e7da495eae1b98ccd98c5`
- Descriptor: `https://github.com/knoxhack/ECHO-Native-Platform/releases/download/v1.0.0-RC1/native-loader-direct-install.json`
- Minecraft library path: `libraries/com/echo/native-loader/1.0.0/native-loader-1.0.0.jar`
- Main class: `com.echo.NativeLoaderClient`

For direct installs, download the public jar, verify its SHA-256, then place it at the Minecraft library path above. The public release filename is `echo-native-loader-1.0.0.jar`; the internal Maven-style library filename stays `native-loader-1.0.0.jar` because that is how Minecraft library coordinates are laid out.

## Pack And Module Placement

Pack-owned `.echo-addon` files belong in the selected launcher's or pack's managed native addon store, normally an `addons/` directory for the active native pack profile. Each addon must contain `META-INF/echo.mod.json` and an `addon.jar` payload.

Do not put `echo-native-loader-1.0.0.jar` in `addons/`, `modules/`, pack manifests, or module dependency lists. It is a loader library, not a module jar. Pack content ships from pack-owned releases such as `ECHO-Ashfall-Native-Edition` or from module release repos.

## Launcher Install Flow

1. Download `echo-native-platform-1.0.0-RC1.zip` through the Release Index product entry.
2. Install the direct loader jar only as the `nativeLoaderLibrary` binary referenced by the product entry.
3. Install `.echo-addon` files from the selected pack release into the launcher-managed addon store.
4. Verify every addon package contains `META-INF/echo.mod.json` and an `addon.jar` payload.
5. Start through the Native Product Launcher entrypoint with the selected pack profile.
6. Review the generated release/readiness reports before joining a world.

Do not launch RC or public builds from Gradle class outputs, IDE classpaths, inferred classpath tokens, or local `build/classes` directories.

## Verification

Run:

```powershell
.\gradlew.bat generatePlan3ReleasePrepReport
.\gradlew.bat checkPlan3ReleasePrep
```

`checkPlan3ReleasePrep` is expected to fail for stable promotion until release artifacts, checksums, SBOM, docs, package evidence, RC smoke evidence, public download-back evidence, and launcher/gameplay proof are all present.
