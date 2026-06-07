package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeResourceConflictReport(
        String reportId,
        boolean conflictFree,
        boolean descriptorOnly,
        boolean resourceRuntimeAccessed,
        int conflictCount,
        int blockingConflictCount,
        List<Map<String, Object>> conflicts
) {
}
