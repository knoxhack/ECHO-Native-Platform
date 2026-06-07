package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoFactionRelation(String factionId, int reputation, boolean hostile) {
    public EchoFactionRelation {
        if (factionId == null || factionId.isBlank()) {
            throw new IllegalArgumentException("factionId is required");
        }
    }
}
