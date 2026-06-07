package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeResourceSourceInventory(
        String inventoryId,
        boolean inventoried,
        boolean localOnly,
        boolean descriptorOnly,
        boolean runtimeResourceAccessAllowed,
        boolean installedPackMutationAllowed,
        boolean fixtureResourceMutationAllowed,
        int resourceSourceCount,
        List<Map<String, Object>> resourceSources
) {
}
