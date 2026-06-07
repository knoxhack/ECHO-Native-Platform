package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNpcProfile(String profileId, String factionId, List<EchoInteractionOption> options) {
    public EchoNpcProfile {
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("profileId is required");
        }
        if (factionId == null || factionId.isBlank()) {
            throw new IllegalArgumentException("factionId is required");
        }
        options = List.copyOf(options == null ? List.of() : options);
    }
}
