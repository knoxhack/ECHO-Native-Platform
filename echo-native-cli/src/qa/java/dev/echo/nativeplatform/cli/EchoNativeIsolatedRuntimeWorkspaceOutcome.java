package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeIsolatedRuntimeWorkspaceOutcome(
        String packId,
        Map<String, Object> isolatedRuntimeWorkspacePlan,
        Map<String, Object> isolatedRuntimeWorkspaceMaterialization,
        Map<String, Object> isolatedRuntimeWorkspaceSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
