package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeSandboxRegistryModel(
        String modelId,
        boolean modeled,
        boolean sandboxOnly,
        boolean minecraftRegistryTouched,
        boolean registryInjected,
        boolean registryMutated,
        int registryKindCount,
        int modeledEntryCount,
        List<Map<String, Object>> registries
) {
}
