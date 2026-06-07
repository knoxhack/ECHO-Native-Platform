package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeNetworkSchemaModel(
        String modelId,
        boolean modeled,
        boolean descriptorOnly,
        boolean liveNetworkingStarted,
        boolean packetSent,
        boolean packetReceived,
        int schemaCount,
        List<Map<String, Object>> schemas
) {
}
