package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativePhase13M18SmokeSessionVerifier {
    EchoNativePhase13M18SmokeSessionOutcome verify(
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

        List<Map<String, Object>> gates = gates(packId, reports, diagnostics);
        boolean gatesPass = diagnostics.stream().noneMatch(EchoNativePhase13M18SmokeSessionVerifier::isBlocking)
                && gates.stream().allMatch(gate -> Boolean.TRUE.equals(gate.get("pass")));
        List<String> completedChecks = gatesPass ? List.of(
                "m17_complete",
                "m18_readiness_confirmed",
                "local_runtime_artifacts_integrity_verified",
                "isolated_launch_status_pass",
                "smoke_session_workspace_planned",
                "no_downloads_or_mutation",
                "no_process_launch_in_verifier"
        ) : List.of();
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativePhase13M18SmokeSessionOutcome(
                packId,
                smokeSessionPlan(packId, fixture, gatesPass, gates, completedChecks, sortedDiagnostics),
                smokeSessionSafetyGate(packId, gatesPass, gates, completedChecks, sortedDiagnostics),
                smokeSessionResult(packId, gatesPass, reports, completedChecks, sortedDiagnostics),
                smokeSessionDiagnostics(packId, gatesPass, gates, sortedDiagnostics),
                phase13M18Completion(packId, gatesPass, completedChecks, sortedDiagnostics),
                phase13M19Readiness(packId, gatesPass, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static List<Map<String, Object>> gates(
            String packId,
            Map<String, Map<String, Object>> reports,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<Map<String, Object>> gates = new ArrayList<>();
        gates.add(booleanGate(packId, reports, "phase13-m17-completion.json", "phase13M17Complete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m17-completion.json", "phase13M18Ready", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m18-readiness.json", "phase13M18Ready", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m17-launch-status.json", "localArtifactsReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m17-launch-status.json", "phase13M17AttemptComplete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "local-runtime-artifact-map.json", "artifactMappingReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "runtime-fixture-integrity-audit.json", "integrityReady", true, diagnostics));
        gates.add(numberGate(packId, reports, "runtime-fixture-integrity-audit.json", "hashVerifiedCount", 2, diagnostics));
        gates.add(numberGate(packId, reports, "phase13-m17-completion.json", "blockedReportCount", 0, diagnostics));
        return gates.stream()
                .sorted(Comparator.comparing(gate -> String.valueOf(gate.get("report")) + ":" + gate.get("field")))
                .toList();
    }

    private static Map<String, Object> booleanGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            boolean expected,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Object actual = EchoNativeJson.asObject(reports.getOrDefault(reportName, Map.of()).get("data")).get(field);
        boolean pass = Boolean.valueOf(expected).equals(actual);
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M18-GATE-FIELD-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M18 smoke-session gate field is not ready",
                    reportName + " must report " + field + "=" + expected + " before the smoke session can complete.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Regenerate the M17 chain and resolve runtime fixture evidence before M18."
            ));
        }
        return gate(reportName, field, expected, actual, pass);
    }

    private static Map<String, Object> numberGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            long expected,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Object actual = EchoNativeJson.asObject(reports.getOrDefault(reportName, Map.of()).get("data")).get(field);
        boolean pass = Long.valueOf(expected).equals(asLong(actual));
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M18-GATE-COUNT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M18 smoke-session count is not ready",
                    reportName + " must report " + field + "=" + expected + " before the smoke session can complete.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Regenerate the M17 runtime fixture integrity reports before M18."
            ));
        }
        return gate(reportName, field, expected, actual, pass);
    }

    private static Map<String, Object> gate(String reportName, String field, Object expected, Object actual, boolean pass) {
        Map<String, Object> gate = new LinkedHashMap<>();
        gate.put("report", reportName);
        gate.put("field", field);
        gate.put("expected", expected);
        gate.put("actual", actual == null ? "" : actual);
        gate.put("pass", pass);
        gate.put("downloadAllowed", false);
        gate.put("nativeExtractionStarted", false);
        gate.put("processLaunched", false);
        gate.put("classloaderCreated", false);
        gate.put("filesystemMutated", false);
        return gate;
    }

    private static Map<String, Object> smokeSessionPlan(
            String packId,
            Path fixture,
            boolean ready,
            List<Map<String, Object>> gates,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m18_smoke_session_plan", diagnostics);
        data.put("bounded", true);
        data.put("completedChecks", completedChecks);
        data.put("fixture", fixture.toString().replace('\\', '/'));
        data.put("gateCount", gates.size());
        data.put("gates", gates);
        data.put("isolatedWorkspace", "tmp/echo-native/phase13/m18/" + safePackId(packId));
        data.put("packId", packId);
        data.put("phase13M18Ready", ready);
        data.put("processLaunchAllowed", false);
        data.put("smokeSessionMode", "report_only_artifact_smoke");
        data.put("smokeSessionPlanReady", ready);
        data.put("summary", ready
                ? "Ashfall native smoke-session plan is ready under report-only M18 controls."
                : "Ashfall native smoke-session plan is blocked by M17 or runtime fixture evidence.");
        return data;
    }

    private static Map<String, Object> smokeSessionSafetyGate(
            String packId,
            boolean ready,
            List<Map<String, Object>> gates,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m18_smoke_session_safety_gate", diagnostics);
        data.put("completedChecks", completedChecks);
        data.put("gateCount", gates.size());
        data.put("m18SmokeSessionAllowed", ready);
        data.put("packId", packId);
        data.put("processLaunchAllowed", false);
        data.put("runtimeClassResolutionAllowed", false);
        data.put("smokeSessionSafetyGatePassed", ready);
        data.put("summary", ready
                ? "M18 smoke-session safety gate passed without enabling unsafe runtime work."
                : "M18 smoke-session safety gate is blocked.");
        return data;
    }

    private static Map<String, Object> smokeSessionResult(
            String packId,
            boolean ready,
            Map<String, Map<String, Object>> reports,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> launchStatus = EchoNativeJson.asObject(reports.getOrDefault("phase13-m17-launch-status.json", Map.of()).get("data"));
        Map<String, Object> artifactMap = EchoNativeJson.asObject(reports.getOrDefault("local-runtime-artifact-map.json", Map.of()).get("data"));
        Map<String, Object> data = base("phase13_m18_smoke_session_result", diagnostics);
        data.put("checkedArtifactCount", artifactMap.getOrDefault("mappedArtifactCount", 0));
        data.put("completedChecks", completedChecks);
        data.put("launchAttemptEvidenceConsumed", Boolean.TRUE.equals(launchStatus.get("phase13M17AttemptComplete")));
        data.put("localArtifactsReady", Boolean.TRUE.equals(launchStatus.get("localArtifactsReady")));
        data.put("mainMenuReached", false);
        data.put("packId", packId);
        data.put("phase13M18Complete", ready);
        data.put("smokeSessionComplete", ready);
        data.put("smokeSessionMode", "report_only_artifact_smoke");
        data.put("summary", ready
                ? "Ashfall native smoke session completed as a bounded report-only M18 gate."
                : "Ashfall native smoke session did not complete.");
        return data;
    }

    private static Map<String, Object> smokeSessionDiagnostics(
            String packId,
            boolean ready,
            List<Map<String, Object>> gates,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m18_smoke_session_diagnostics", diagnostics);
        data.put("diagnostics", diagnostics.stream().map(EchoNativePhase13M18SmokeSessionVerifier::diagnosticData).toList());
        data.put("gateCount", gates.size());
        data.put("packId", packId);
        data.put("smokeSessionDiagnosticsReady", ready);
        data.put("summary", ready
                ? "M18 smoke-session diagnostics are clean."
                : "M18 smoke-session diagnostics captured blocking evidence.");
        return data;
    }

    private static Map<String, Object> phase13M18Completion(
            String packId,
            boolean complete,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m18_completion", diagnostics);
        data.put("blockedReportCount", diagnostics.stream().filter(EchoNativePhase13M18SmokeSessionVerifier::isBlocking).count());
        data.put("completedChecks", completedChecks);
        data.put("firstPlaytestOpen", false);
        data.put("packId", packId);
        data.put("phase13M18Complete", complete);
        data.put("phase13M19Ready", complete);
        data.put("playtestCandidateReady", false);
        data.put("summary", complete
                ? "Phase 13 M18 smoke-session gate passed; M19 playtest candidate packaging may begin."
                : "Phase 13 M18 remains blocked.");
        return data;
    }

    private static Map<String, Object> phase13M19Readiness(
            String packId,
            boolean ready,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m19_readiness", diagnostics);
        data.put("firstPlaytestOpen", false);
        data.put("packId", packId);
        data.put("phase13M19Ready", ready);
        data.put("playtestCandidateReady", false);
        data.put("requiredNextWork", ready ? List.of(
                "tester_safe_package",
                "support_bundle_export",
                "rollback_notes",
                "known_limitations",
                "experimental_native_loader_label"
        ) : List.of());
        data.put("summary", ready
                ? "M19 may begin; first playtest remains closed until tester-safe packaging is complete."
                : "M19 remains blocked until M18 passes.");
        return data;
    }

    private static Map<String, Object> diagnosticData(EchoNativeDiagnostic diagnostic) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", diagnostic.code());
        data.put("severity", diagnostic.severity().name());
        data.put("title", diagnostic.title());
        data.put("summary", diagnostic.summary());
        data.put("moduleId", diagnostic.moduleId());
        data.put("packId", diagnostic.packId());
        data.put("likelyFiles", diagnostic.likelyFiles());
        data.put("suggestedFix", diagnostic.suggestedFix());
        return data;
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
                    "ECHO-NATIVE-PHASE13-M18-UPSTREAM-REPORT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M18 upstream report is not PASS",
                    "M18 requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate and resolve M17 reports before completing the M18 smoke session."
            ));
            diagnostics.addAll(reportDiagnostics(report, packId));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M18-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M18 upstream report contains unsafe runtime work",
                    reportName + " indicates work that is not allowed during the M18 smoke-session gate.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep M18 bounded and isolated: no downloads, extraction, classloader, runtime class resolution, registry mutation, or user-cache mutation."
            ));
        }
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
                    "ECHO-NATIVE-PHASE13-M18-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M18 required report missing",
                    "M18 requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate the M17 closeout and runtime fixture reports before M18."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("downloadsAllowed", false);
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("safeToAutoPopulate", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("remoteManifestDownloaded"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("cacheMutated"))
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

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(EchoNativeJson::asObject)
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

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String safePackId(String packId) {
        return packId.replaceAll("[^A-Za-z0-9_.-]", "_");
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
