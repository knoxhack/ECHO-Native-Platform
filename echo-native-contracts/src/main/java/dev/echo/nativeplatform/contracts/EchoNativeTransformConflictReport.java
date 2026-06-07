package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeTransformConflictReport(
        String reportId,
        boolean conflictFree,
        boolean transformPlanningOnly,
        boolean transformsEnabled,
        int conflictCount,
        int blockingConflictCount,
        List<Map<String, Object>> conflicts
) {
}
