package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeRegistrySourceInventory(
        String inventoryId,
        boolean inventoried,
        boolean localOnly,
        boolean sandboxOnly,
        boolean minecraftRegistryAccessAllowed,
        boolean registryInjectionAllowed,
        boolean registryMutationAllowed,
        int registrySourceCount,
        List<Map<String, Object>> registrySources
) {
}
