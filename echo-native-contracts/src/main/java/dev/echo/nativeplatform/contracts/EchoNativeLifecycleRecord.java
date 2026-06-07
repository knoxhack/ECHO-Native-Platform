package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeLifecycleRecord(
        EchoNativeLifecyclePhase phase,
        EchoNativeLoadStatus status,
        String detail,
        boolean failed,
        List<String> failures
) {
    public Map<String, Object> toReport() {
        return Map.of(
                "phase", phase.name(),
                "status", status.name(),
                "detail", detail == null ? "" : detail,
                "failed", failed,
                "failures", failures == null ? List.of() : List.copyOf(failures)
        );
    }
}
