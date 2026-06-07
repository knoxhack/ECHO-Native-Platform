package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativePhase13M1Completion;
import dev.echo.nativeplatform.contracts.EchoNativePhase13M2Readiness;
import dev.echo.nativeplatform.contracts.EchoNativePhase13PrototypeSafetyGate;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativePhase13M1CloseoutVerifier {
    EchoNativePhase13M1CloseoutOutcome verify(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), report, entry.getValue(), packId, diagnostics);
        }

        diagnostics = unique(diagnostics);
        boolean complete = diagnostics.isEmpty();
        List<String> completedReports = complete ? List.copyOf(requiredReports.keySet()) : List.of();
        List<String> safetyGates = complete
                ? List.of(
                "phase13_plan_pass",
                "lifecycle_simulation_pass",
                "service_attach_simulation_pass",
                "crash_boundary_simulation_pass",
                "loader_boundary_verification_pass",
                "classpath_classloader_compatibility_pass",
                "test_process_boundary_pass",
                "m1_safety_status_pass",
                "resource_bridge_rehearsal_pass",
                "registry_bridge_rehearsal_pass",
                "bridge_safety_status_pass"
        )
                : List.of();
        List<String> blockedCapabilities = List.of(
                "minecraft.runtime.resolution",
                "network.download",
                "native.library.extraction",
                "production.classloader",
                "runtime.class.resolution",
                "process.launch",
                "command.execution",
                "registry.injection",
                "registry.mutation",
                "bytecode.transforms",
                "filesystem.mutation"
        );

        EchoNativePhase13M1Completion completion = new EchoNativePhase13M1Completion(
                "phase13.m1.completion",
                complete,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                completedReports
        );
        EchoNativePhase13M2Readiness m2Readiness = new EchoNativePhase13M2Readiness(
                "phase13.m2.readiness",
                complete,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                complete ? List.of("phase13.m2.minecraft_version_resolver.planning_only") : List.of(),
                blockedCapabilities
        );
        EchoNativePhase13PrototypeSafetyGate safetyGate = new EchoNativePhase13PrototypeSafetyGate(
                "phase13.prototype.safety.gate",
                complete,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                safetyGates
        );

        return new EchoNativePhase13M1CloseoutOutcome(
                packId,
                phase13M1Completion(packId, completion, diagnostics),
                phase13M2Readiness(packId, m2Readiness, diagnostics),
                phase13PrototypeSafetyGate(packId, safetyGate, diagnostics),
                diagnostics
        );
    }

    private static void checkReport(
            String reportName,
            Map<String, Object> report,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        if (!"PASS".equals(report.get("status"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M1-REPORT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M1 report is not PASS",
                    "M1 closeout requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate the Phase 13 M1 report chain and resolve upstream diagnostics."
            ));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M1-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M1 report contains unsafe runtime work",
                    reportName + " indicates runtime work that is still blocked during M1 closeout.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep M1 closeout report-only: no launch, classloader, runtime class resolution, registry mutation, download, extraction, command execution, or filesystem mutation."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("minecraftResolverStarted"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("mutatedFilesystem"))
                || Boolean.TRUE.equals(data.get("executedServiceCode"))
                || Boolean.TRUE.equals(data.get("resourceRuntimeAccessed"))
                || Boolean.TRUE.equals(data.get("prototypeRuntimeStarted"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"))
                || Boolean.TRUE.equals(data.get("subprocessCreated"))
                || Boolean.TRUE.equals(data.get("processLaunchAllowed"))
                || Boolean.TRUE.equals(data.get("gameLaunchAllowed"));
    }

    private static Map<String, Object> phase13M1Completion(
            String packId,
            EchoNativePhase13M1Completion completion,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m1_completion_gate", diagnostics);
        data.put("classloaderCreated", completion.classloaderCreated());
        data.put("commandExecuted", completion.commandExecuted());
        data.put("completedReports", completion.completedReports());
        data.put("completionId", completion.completionId());
        data.put("filesystemMutated", completion.filesystemMutated());
        data.put("gameClassesResolved", completion.gameClassesResolved());
        data.put("libraryDownloadStarted", completion.libraryDownloadStarted());
        data.put("minecraftResolverStarted", completion.minecraftResolverStarted());
        data.put("nativeExtractionStarted", completion.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("phase13M1Complete", completion.phase13M1Complete());
        data.put("processLaunched", completion.processLaunched());
        data.put("registryInjected", completion.registryInjected());
        data.put("registryMutated", completion.registryMutated());
        data.put("summary", completion.phase13M1Complete()
                ? "Phase 13 M1 closeout passed for this fixture; all report-only safety gates are complete."
                : "Phase 13 M1 closeout is blocked by missing, failed, or unsafe M1 reports.");
        return data;
    }

    private static Map<String, Object> phase13M2Readiness(
            String packId,
            EchoNativePhase13M2Readiness readiness,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m2_readiness_audit", diagnostics);
        data.put("allowedNextWork", readiness.allowedNextWork());
        data.put("blockedCapabilities", readiness.blockedCapabilities());
        data.put("classloaderCreated", readiness.classloaderCreated());
        data.put("commandExecuted", readiness.commandExecuted());
        data.put("filesystemMutated", readiness.filesystemMutated());
        data.put("gameClassesResolved", readiness.gameClassesResolved());
        data.put("libraryDownloadStarted", readiness.libraryDownloadStarted());
        data.put("minecraftResolverStarted", readiness.minecraftResolverStarted());
        data.put("nativeExtractionStarted", readiness.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("phase13M2Ready", readiness.phase13M2Ready());
        data.put("processLaunched", readiness.processLaunched());
        data.put("readinessId", readiness.readinessId());
        data.put("registryInjected", readiness.registryInjected());
        data.put("registryMutated", readiness.registryMutated());
        data.put("summary", readiness.phase13M2Ready()
                ? "Phase 13 M2 may begin as planning-only Minecraft version resolver work in a later run."
                : "Phase 13 M2 remains blocked until M1 closeout passes.");
        return data;
    }

    private static Map<String, Object> phase13PrototypeSafetyGate(
            String packId,
            EchoNativePhase13PrototypeSafetyGate gate,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_prototype_safety_gate", diagnostics);
        data.put("classloaderCreated", gate.classloaderCreated());
        data.put("commandExecuted", gate.commandExecuted());
        data.put("filesystemMutated", gate.filesystemMutated());
        data.put("gameClassesResolved", gate.gameClassesResolved());
        data.put("gateId", gate.gateId());
        data.put("libraryDownloadStarted", gate.libraryDownloadStarted());
        data.put("minecraftResolverStarted", gate.minecraftResolverStarted());
        data.put("nativeExtractionStarted", gate.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("passed", gate.passed());
        data.put("processLaunched", gate.processLaunched());
        data.put("registryInjected", gate.registryInjected());
        data.put("registryMutated", gate.registryMutated());
        data.put("safetyGates", gate.safetyGates());
        data.put("summary", gate.passed()
                ? "Phase 13 prototype safety gate passed; M1 stayed report-only."
                : "Phase 13 prototype safety gate failed because one or more M1 inputs failed.");
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("gameProcessLaunched", false);
        data.put("phase", phase);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M1-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M1 report missing",
                    "Required M1 closeout input " + reportName + " was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate the full Phase 13 M1 report chain before running M1 closeout."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> EchoNativeJson.asObject(item))
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("code")) + ":" + item.get("summary")))
                .map(item -> new EchoNativeDiagnostic(
                        String.valueOf(item.getOrDefault("code", "ECHO-NATIVE-UPSTREAM-DIAGNOSTIC")),
                        EchoNativeIssueSeverity.ERROR,
                        String.valueOf(item.getOrDefault("title", "Upstream diagnostic")),
                        String.valueOf(item.getOrDefault("summary", "Upstream Phase 13 report is not PASS.")),
                        item.get("moduleId") == null ? null : String.valueOf(item.get("moduleId")),
                        packId,
                        EchoNativeJson.stringList(item.get("likelyFiles")),
                        String.valueOf(item.getOrDefault("suggestedFix", "Resolve upstream diagnostics first."))
                ))
                .toList();
    }

    private static List<EchoNativeDiagnostic> unique(List<EchoNativeDiagnostic> diagnostics) {
        Map<String, EchoNativeDiagnostic> byKey = new LinkedHashMap<>();
        for (EchoNativeDiagnostic diagnostic : diagnostics) {
            byKey.put(diagnostic.code() + "|" + diagnostic.moduleId() + "|" + diagnostic.summary(), diagnostic);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
