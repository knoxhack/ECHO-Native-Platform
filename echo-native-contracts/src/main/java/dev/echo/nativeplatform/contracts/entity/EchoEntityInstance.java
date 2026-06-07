package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoEntityInstance(
        String entityId,
        EchoEntityType type,
        String profileId,
        int x,
        int z,
        EchoCombatStats stats
) {
    public EchoEntityInstance {
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("entityId is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("profileId is required");
        }
        if (stats == null) {
            throw new IllegalArgumentException("stats is required");
        }
    }

    public EchoEntityInstance withStats(EchoCombatStats nextStats) {
        return new EchoEntityInstance(entityId, type, profileId, x, z, nextStats);
    }
}
