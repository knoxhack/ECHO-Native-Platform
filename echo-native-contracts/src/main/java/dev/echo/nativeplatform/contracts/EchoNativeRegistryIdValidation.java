package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeRegistryIdValidation(
        String validationId,
        boolean valid,
        boolean sandboxOnly,
        boolean minecraftRegistryTouched,
        int registryKindCount,
        int validatedEntryCount,
        List<String> registryKinds,
        List<Map<String, Object>> validatedEntries
) {
}
