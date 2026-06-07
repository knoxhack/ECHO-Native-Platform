package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeFirstPlaytestCandidateOutcome(
        String packId,
        Map<String, Object> firstPlaytestCandidatePackage,
        Map<String, Object> firstPlaytestSupportBundle,
        Map<String, Object> firstPlaytestRollbackNotes,
        Map<String, Object> firstPlaytestKnownLimitations,
        Map<String, Object> experimentalNativeLoaderLabel,
        Map<String, Object> firstPlaytestCrashReportCollection,
        Map<String, Object> phase13M19Completion,
        Map<String, Object> firstPlaytestOpenGate,
        List<EchoNativeDiagnostic> diagnostics
) {
}
