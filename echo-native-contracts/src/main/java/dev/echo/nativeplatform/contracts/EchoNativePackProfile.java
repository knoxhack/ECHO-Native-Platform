package dev.echo.nativeplatform.contracts;

import java.nio.file.Path;
import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativePackProfile(
        String schema,
        String id,
        String name,
        String status,
        String rootModule,
        String minecraftVersion,
        String loaderKind,
        String loaderVersion,
        List<String> requiredModules,
        List<String> requiredFeatures,
        List<String> optionalFeatures,
        Path profilePath
) {
}
