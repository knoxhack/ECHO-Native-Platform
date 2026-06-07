package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeSupportBundleDryRunPlan(
        String planId,
        boolean planned,
        boolean supportBundlePlannedOnly,
        boolean bundleWritten,
        boolean filesystemMutated,
        int plannedArtifactCount,
        List<Map<String, Object>> plannedArtifacts
) {
}
