package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoInteractionOption(String optionId, String label, String actionId) {
    public EchoInteractionOption {
        if (optionId == null || optionId.isBlank()) {
            throw new IllegalArgumentException("optionId is required");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException("actionId is required");
        }
    }
}
