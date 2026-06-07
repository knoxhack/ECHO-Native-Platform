package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeConfigWritePlan(
        String planId,
        boolean ready,
        boolean writePlanOnly,
        boolean installedConfigMutated,
        boolean fixtureConfigMutated,
        boolean filesystemMutated,
        int plannedWriteCount,
        List<Map<String, Object>> plannedWrites
) {
}
