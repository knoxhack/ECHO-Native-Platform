# Example Addon Walkthrough

Use the native addon templates for public beta examples.

## Scaffold

```powershell
python tools/echo_sdk.py addon echo-native-addon --module-id echonativeexample --package-name com.example.echo.nativeexample --force
```

Then add one focused surface template, such as:

```powershell
python tools/echo_sdk.py addon echo-native-registry-example --module-id echonativeexample --package-name com.example.echo.nativeexample --force
```

## Build

Register the addon in `settings.gradle` only after reviewing the generated registration plan. Then run:

```powershell
.\gradlew.bat :echonativeexample:compileJava
.\gradlew.bat validateEchoSdkTemplates
```

## Package

The release package must include the addon jar, source jar, descriptor, docs, and checksums. Do not publish an example that only works from local class output.
