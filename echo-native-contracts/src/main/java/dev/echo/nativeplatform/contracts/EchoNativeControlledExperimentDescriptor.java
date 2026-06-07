package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeControlledExperimentDescriptor(
        String id,
        String phase,
        String summary,
        boolean dryRunOnly,
        boolean enabled,
        List<String> allowedOperations,
        List<String> blockedOperations
) {
}
