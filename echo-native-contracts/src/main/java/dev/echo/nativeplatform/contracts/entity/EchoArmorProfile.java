package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoArmorProfile(String armorId, int reduction) {
    public EchoArmorProfile {
        if (armorId == null || armorId.isBlank()) {
            throw new IllegalArgumentException("armorId is required");
        }
        if (reduction < 0) {
            throw new IllegalArgumentException("reduction must not be negative");
        }
    }
}
