package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeFirstPlaytestRoadmapOutcome(
        String packId,
        Map<String, Object> phase13FirstPlaytestRoadmap,
        Map<String, Object> phase13FirstPlaytestNextActions,
        Map<String, Object> phase13FirstPlaytestFullRoadmap,
        List<EchoNativeDiagnostic> diagnostics
) {
}
