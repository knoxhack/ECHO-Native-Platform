package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeCrashHardeningCoverage;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeFailureContainmentMatrix;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativePhase13M16SafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeSupportBundleDryRunPlan;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeCrashHardeningVerifier {
    EchoNativeCrashHardeningOutcome verify(
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
        boolean passed = diagnostics.isEmpty();
        List<String> coveredReports = passed ? List.copyOf(requiredReports.keySet()) : List.of();
        List<Map<String, Object>> failureCases = passed ? failureCases() : List.of();
        List<Map<String, Object>> supportArtifacts = passed ? supportArtifacts() : List.of();
        List<String> completedChecks = passed ? List.of(
                "required_reports_present",
                "required_reports_pass",
                "unsafe_runtime_flags_absent",
                "failure_containment_matrix_planned",
                "support_bundle_dry_run_planned",
                "deterministic_diagnostics_ready"
        ) : List.of();

        EchoNativeCrashHardeningCoverage coverage = new EchoNativeCrashHardeningCoverage(
                "phase13.m16.crash.hardening.coverage",
                passed,
                true,
                true,
                coveredReports.size(),
                coveredReports
        );
        EchoNativeFailureContainmentMatrix matrix = new EchoNativeFailureContainmentMatrix(
                "phase13.m16.failure.containment.matrix",
                passed,
                true,
                true,
                failureCases.size(),
                failureCases
        );
        EchoNativeSupportBundleDryRunPlan supportPlan = new EchoNativeSupportBundleDryRunPlan(
                "phase13.m16.support.bundle.dry_run.plan",
                passed,
                true,
                false,
                false,
                supportArtifacts.size(),
                supportArtifacts
        );
        EchoNativePhase13M16SafetyStatus safetyStatus = new EchoNativePhase13M16SafetyStatus(
                "phase13.m16.safety.status",
                passed,
                true,
                true,
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
                false,
                false,
                completedChecks
        );

        return new EchoNativeCrashHardeningOutcome(
                packId,
                crashHardeningCoverage(packId, coverage, diagnostics),
                failureContainmentMatrix(packId, matrix, diagnostics),
                supportBundleDryRunPlan(packId, supportPlan, diagnostics),
                phase13M16SafetyStatus(packId, safetyStatus, diagnostics),
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
                    "ECHO-NATIVE-M16-UPSTREAM-REPORT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M16 upstream report is not PASS",
                    "Crash-boundary hardening requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate the Phase 13 report chain and resolve upstream diagnostics."
            ));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M16-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M16 upstream report contains unsafe runtime work",
                    reportName + " indicates runtime work that is still blocked during crash-boundary hardening.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep M16 report-only: no launch, classloader, runtime class resolution, registry mutation, network I/O, transforms, downloads, extraction, command execution, or filesystem mutation."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static List<Map<String, Object>> failureCases() {
        List<String> cases = List.of(
                "minecraft_resolver_failure",
                "library_resolver_failure",
                "classpath_builder_failure",
                "launch_argument_failure",
                "service_bus_failure",
                "resource_bridge_failure",
                "registry_bridge_failure",
                "network_bridge_failure",
                "transform_pipeline_failure"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (String id : cases) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("contained", true);
            item.put("diagnosticsCaptured", true);
            item.put("supportBundlePlannedOnly", true);
            item.put("commandExecuted", false);
            item.put("processLaunched", false);
            item.put("classloaderCreated", false);
            item.put("filesystemMutated", false);
            result.add(item);
        }
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> supportArtifacts() {
        List<String> artifacts = List.of(
                "crash-hardening-coverage.json",
                "failure-containment-matrix.json",
                "support-bundle-dry-run-plan.json",
                "phase13-m16-safety-status.json"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (String artifact : artifacts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", "reports/echo-native/<pack-id>/" + artifact);
            item.put("plannedOnly", true);
            item.put("writtenByThisPlan", false);
            item.put("secretSafe", true);
            result.add(item);
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> crashHardeningCoverage(
            String packId,
            EchoNativeCrashHardeningCoverage coverage,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m16_crash_hardening_coverage", diagnostics);
        data.put("coverageId", coverage.coverageId());
        data.put("covered", coverage.covered());
        data.put("coveredReportCount", coverage.coveredReportCount());
        data.put("coveredReports", coverage.coveredReports());
        data.put("diagnosticsCaptured", coverage.diagnosticsCaptured());
        data.put("packId", packId);
        data.put("summary", coverage.covered()
                ? "M16 crash-boundary coverage includes resolver, classpath, launch-plan, service, resource, registry, network, and transform prototype reports."
                : "M16 crash-boundary coverage is blocked by missing, failed, or unsafe upstream reports.");
        data.put("supportBundlePlannedOnly", coverage.supportBundlePlannedOnly());
        return data;
    }

    private static Map<String, Object> failureContainmentMatrix(
            String packId,
            EchoNativeFailureContainmentMatrix matrix,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m16_failure_containment_matrix", diagnostics);
        data.put("contained", matrix.contained());
        data.put("deterministicDiagnostics", matrix.deterministicDiagnostics());
        data.put("failureCaseCount", matrix.failureCaseCount());
        data.put("failureCases", matrix.failureCases());
        data.put("matrixId", matrix.matrixId());
        data.put("packId", packId);
        data.put("summary", matrix.contained()
                ? "M16 failure containment cases are deterministic and report-only."
                : "M16 failure containment is blocked by upstream diagnostics.");
        data.put("supportBundlePlannedOnly", matrix.supportBundlePlannedOnly());
        return data;
    }

    private static Map<String, Object> supportBundleDryRunPlan(
            String packId,
            EchoNativeSupportBundleDryRunPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m16_support_bundle_dry_run_plan", diagnostics);
        data.put("bundleWritten", plan.bundleWritten());
        data.put("filesystemMutated", plan.filesystemMutated());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("planned", plan.planned());
        data.put("plannedArtifactCount", plan.plannedArtifactCount());
        data.put("plannedArtifacts", plan.plannedArtifacts());
        data.put("summary", plan.planned()
                ? "M16 support bundle output is planned only; no bundle file is written by this command."
                : "M16 support bundle dry-run planning is blocked by upstream diagnostics.");
        data.put("supportBundlePlannedOnly", plan.supportBundlePlannedOnly());
        return data;
    }

    private static Map<String, Object> phase13M16SafetyStatus(
            String packId,
            EchoNativePhase13M16SafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m16_safety_status", diagnostics);
        data.put("bytecodeMutated", status.bytecodeMutated());
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("diagnosticsCaptured", status.diagnosticsCaptured());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("gameProcessLaunched", status.gameProcessLaunched());
        data.put("liveNetworkingStarted", status.liveNetworkingStarted());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "M16 crash-boundary hardening stayed deterministic, report-only, and safe to continue."
                : "M16 crash-boundary hardening is blocked by upstream diagnostics.");
        data.put("supportBundlePlannedOnly", status.supportBundlePlannedOnly());
        data.put("transformsEnabled", status.transformsEnabled());
        data.put("transformsPerformed", status.transformsPerformed());
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("liveNetworkingStarted", false);
        data.put("minecraftLaunched", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("supportBundlePlannedOnly", true);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
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
                    "ECHO-NATIVE-M16-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M16 required report missing",
                    "Crash-boundary hardening requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate the Phase 13 report chain through M15 before running M16."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("minecraftResolverStarted"))
                || Boolean.TRUE.equals(data.get("remoteManifestDownloaded"))
                || Boolean.TRUE.equals(data.get("libraryResolverStarted"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("cacheMutated"))
                || Boolean.TRUE.equals(data.get("classpathBuilderStarted"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("nativeFilesExtracted"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("minecraftClassesResolved"))
                || Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("realAddonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
                || Boolean.TRUE.equals(data.get("resourceRuntimeAccessed"))
                || Boolean.TRUE.equals(data.get("minecraftResourceManagerTouched"))
                || Boolean.TRUE.equals(data.get("minecraftRegistryTouched"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("liveNetworkingStarted"))
                || Boolean.TRUE.equals(data.get("socketOpened"))
                || Boolean.TRUE.equals(data.get("clientConnectionOpened"))
                || Boolean.TRUE.equals(data.get("serverConnectionOpened"))
                || Boolean.TRUE.equals(data.get("packetSent"))
                || Boolean.TRUE.equals(data.get("packetReceived"))
                || Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("minecraftBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("addonBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("bytecodeMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
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
