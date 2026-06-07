package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeResourceNamespaceValidation(
        String validationId,
        boolean valid,
        boolean localOnly,
        boolean descriptorOnly,
        boolean resourceRuntimeAccessed,
        int namespaceCount,
        int validatedResourceCount,
        List<String> namespaces,
        List<Map<String, Object>> validatedResources
) {
}
