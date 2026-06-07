package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeStaticSafetyScan(
        Map<String, Object> data,
        List<EchoNativeDiagnostic> diagnostics
) {
}
