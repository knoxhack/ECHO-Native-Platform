package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoCreatureBrain(String brainId, boolean hostile, int aggroRange) {
    public EchoCreatureBrain {
        if (brainId == null || brainId.isBlank()) {
            throw new IllegalArgumentException("brainId is required");
        }
        if (aggroRange < 0) {
            throw new IllegalArgumentException("aggroRange must not be negative");
        }
    }
}
