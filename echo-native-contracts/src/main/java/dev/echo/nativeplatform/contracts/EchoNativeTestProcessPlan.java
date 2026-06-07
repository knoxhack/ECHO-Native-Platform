package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeTestProcessPlan(
        String planId,
        boolean processLaunchAllowed,
        boolean gameLaunchAllowed,
        boolean subprocessCreated,
        List<String> allowedTargets,
        List<String> blockedTargets
) {
}
