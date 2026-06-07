package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13BoundaryVerificationOutcome(
        String packId,
        Map<String, Object> loaderBoundaryStateMachine,
        Map<String, Object> loaderBoundaryVerification,
        Map<String, Object> classpathClassloaderCompatibility,
        List<EchoNativeDiagnostic> diagnostics
) {
}
