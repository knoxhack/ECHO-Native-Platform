package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeNetworkPacketValidation(
        String validationId,
        boolean valid,
        boolean descriptorOnly,
        boolean liveNetworkingStarted,
        int packetCount,
        int channelCount,
        List<String> directions,
        List<Map<String, Object>> packets
) {
}
