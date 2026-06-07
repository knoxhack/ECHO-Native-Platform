package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeBoundaryFailureCase(
        String id,
        String source,
        String expectedDiagnosticCode,
        String blockedCapability,
        boolean contained,
        boolean capturedDiagnostic,
        boolean terminatedProcess,
        boolean mutatedState
) {
}
