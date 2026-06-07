package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeMinecraftLaunchPreflight(
        String preflightId,
        boolean launchPreflightComplete,
        boolean commandExecuted,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        boolean classloaderCreated,
        boolean transformsEnabled,
        String minecraftVersion,
        List<String> requiredInputs
) {
}
