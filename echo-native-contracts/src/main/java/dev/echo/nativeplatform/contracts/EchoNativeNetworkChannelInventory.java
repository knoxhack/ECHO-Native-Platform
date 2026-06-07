package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeNetworkChannelInventory(
        String inventoryId,
        boolean inventoried,
        boolean localOnly,
        boolean descriptorOnly,
        boolean liveNetworkingAllowed,
        boolean socketAllowed,
        boolean clientConnectionAllowed,
        boolean serverConnectionAllowed,
        int channelCount,
        List<Map<String, Object>> channels
) {
}
