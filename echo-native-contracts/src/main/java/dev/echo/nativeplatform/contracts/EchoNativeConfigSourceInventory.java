package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeConfigSourceInventory(
        String inventoryId,
        boolean read,
        boolean localOnly,
        boolean writePlanOnly,
        boolean installedConfigMutationAllowed,
        boolean fixtureConfigMutationAllowed,
        int configSourceCount,
        List<Map<String, Object>> configSources
) {
}
