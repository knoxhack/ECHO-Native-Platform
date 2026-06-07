package dev.echo.nativeplatform.contracts;

import java.util.Map;
import java.util.Objects;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeModuleHealthTelemetry(
        String moduleId,
        EchoNativeLoadStatus status,
        EchoNativeRuntimeSide side,
        long receiptCount,
        long mutatedReceiptCount,
        long failedReceiptCount,
        Map<String, Object> evidence
) {
    public EchoNativeModuleHealthTelemetry {
        moduleId = optionalText(moduleId);
        status = status == null ? EchoNativeLoadStatus.UNSUPPORTED : status;
        side = side == null ? EchoNativeRuntimeSide.UNKNOWN : side;
        if (receiptCount < 0 || mutatedReceiptCount < 0 || failedReceiptCount < 0) {
            throw new IllegalArgumentException("receipt counts must not be negative");
        }
        evidence = Map.copyOf(Objects.requireNonNullElse(evidence, Map.of()));
    }

    public static EchoNativeModuleHealthTelemetry empty(String moduleId) {
        return new EchoNativeModuleHealthTelemetry(
                moduleId,
                EchoNativeLoadStatus.UNSUPPORTED,
                EchoNativeRuntimeSide.UNKNOWN,
                0,
                0,
                0,
                Map.of()
        );
    }

    private static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }
}
