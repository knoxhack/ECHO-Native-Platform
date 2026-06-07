package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeResourcePackOrderPlan(
        String planId,
        boolean planned,
        boolean descriptorOnly,
        boolean resourceRuntimeAccessed,
        boolean filesystemMutated,
        int orderedResourceCount,
        List<Map<String, Object>> resourceOrder
) {
}
