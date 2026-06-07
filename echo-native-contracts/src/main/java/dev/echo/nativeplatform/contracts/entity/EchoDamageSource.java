package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoDamageSource(String sourceId, String attackerEntityId, int amount) {
    public EchoDamageSource {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId is required");
        }
        if (attackerEntityId == null || attackerEntityId.isBlank()) {
            throw new IllegalArgumentException("attackerEntityId is required");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }
}
