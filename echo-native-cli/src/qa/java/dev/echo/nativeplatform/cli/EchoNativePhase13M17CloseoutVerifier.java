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

final class EchoNativePhase13M17CloseoutVerifier {
    EchoNativePhase13M17CloseoutOutcome verify(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        List<Map<String, Object>> blockers = new ArrayList<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics, blockers);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), report, entry.getValue(), packId, diagnostics, blockers);
        }

        checkBooleanReport(reports, "phase13-m17-readiness.json", "phase13M17Ready", true, packId, diagnostics, blockers);
        checkBooleanReport(reports, "launch-safety-gate.json", "safeForIsolatedLaunchAttempt", true, packId, diagnostics, blockers);
        checkNumberReport(reports, "local-runtime-artifact-map.json", "missingArtifactCount", 0, packId, diagnostics, blockers);
        checkBooleanReport(reports, "local-runtime-artifact-map.json", "artifactMappingReady", true, packId, diagnostics, blockers);
        checkBooleanReport(reports, "phase13-m17-artifact-readiness.json", "phase13M17ArtifactReady", true, packId, diagnostics, blockers);
        checkNumberReport(reports, "phase13-m17-artifact-blockers.json", "artifactBlockerCount", 0, packId, diagnostics, blockers);
        checkNumberReport(reports, "phase13-m17-artifact-packaging-audit.json", "artifactBlockerCount", 0, packId, diagnostics, blockers);
        checkBooleanReport(reports, "runtime-fixture-presence.json", "runtimeFixturesPresent", true, packId, diagnostics, blockers);
        checkNumberReport(reports, "runtime-fixture-presence.json", "missingFixtureCount", 0, packId, diagnostics, blockers);
        checkBooleanReport(reports, "runtime-fixture-mapping-readiness.json", "runtimeFixtureMappingsReady", true, packId, diagnostics, blockers);
        checkNumberReport(reports, "runtime-fixture-mapping-readiness.json", "missingOrUnreadyMappingCount", 0, packId, diagnostics, blockers);
        checkBooleanReport(reports, "runtime-fixture-approval-audit.json", "approvalsReady", true, packId, diagnostics, blockers);
        checkNumberReport(reports, "runtime-fixture-approval-audit.json", "approvalAuditCount", 2, packId, diagnostics, blockers);
        checkNumberReport(reports, "runtime-fixture-approval-audit.json", "approvedCount", 2, packId, diagnostics, blockers);
        checkBooleanReport(reports, "runtime-fixture-integrity-audit.json", "integrityReady", true, packId, diagnostics, blockers);
        checkNumberReport(reports, "runtime-fixture-integrity-audit.json", "integrityCheckCount", 2, packId, diagnostics, blockers);
        checkNumberReport(reports, "runtime-fixture-integrity-audit.json", "hashVerifiedCount", 2, packId, diagnostics, blockers);
        checkBooleanReport(reports, "phase13-m18-readiness.json", "phase13M18Ready", true, packId, diagnostics, blockers);

        diagnostics = unique(diagnostics);
        blockers = uniqueBlockers(blockers);
        boolean complete = diagnostics.isEmpty() && blockers.isEmpty();
        List<String> completedReports = complete ? List.copyOf(requiredReports.keySet()) : List.of();

        return new EchoNativePhase13M17CloseoutOutcome(
                packId,
                phase13M17Completion(packId, complete, completedReports, blockers, diagnostics),
                phase13M18ReadinessAudit(packId, complete, completedReports, blockers, diagnostics),
                phase13FirstPlaytestBlockers(packId, blockers, diagnostics),
                diagnostics
        );
    }

    private static void checkReport(
            String reportName,
            Map<String, Object> report,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics,
            List<Map<String, Object>> blockers
    ) {
        if (report.isEmpty()) {
            return;
        }
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        if (!"PASS".equals(report.get("status"))) {
            addBlocker(blockers, reportName, "report_status_not_pass", "Required M17 report is not PASS.");
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M17-REPORT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 report is not PASS",
                    "M17 closeout requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Resolve upstream M17 diagnostics before attempting M18 or first playtest work."
            ));
        }
        if (hasUnsafeRuntimeWork(data)) {
            addBlocker(blockers, reportName, "unsafe_runtime_work", "Required M17 report indicates unsafe runtime work.");
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M17-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 report contains unsafe runtime work",
                    reportName + " indicates runtime work that is not allowed during M17 closeout.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep M17 closeout report-only unless all launch gates explicitly pass."
            ));
        }
    }

    private static void checkBooleanReport(
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            boolean expected,
            String packId,
            List<EchoNativeDiagnostic> diagnostics,
            List<Map<String, Object>> blockers
    ) {
        Map<String, Object> report = reports.getOrDefault(reportName, Map.of());
        if (report.isEmpty()) {
            return;
        }
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        if (!Boolean.valueOf(expected).equals(data.get(field))) {
            addBlocker(blockers, reportName, field, "Expected " + field + "=" + expected + " but found " + data.get(field) + ".");
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M17-GATE-FIELD-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 gate field is not ready",
                    reportName + " must report " + field + "=" + expected + " before M18 can begin.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Regenerate the M17 chain after resolving local runtime artifacts and mappings."
            ));
        }
    }

    private static void checkNumberReport(
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            long expected,
            String packId,
            List<EchoNativeDiagnostic> diagnostics,
            List<Map<String, Object>> blockers
    ) {
        Map<String, Object> report = reports.getOrDefault(reportName, Map.of());
        if (report.isEmpty()) {
            return;
        }
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        if (!Long.valueOf(expected).equals(asLong(data.get(field)))) {
            addBlocker(blockers, reportName, field, "Expected " + field + "=" + expected + " but found " + data.get(field) + ".");
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M17-GATE-COUNT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 gate count is not ready",
                    reportName + " must report " + field + "=" + expected + " before M18 can begin.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Regenerate the M17 chain after resolving local runtime artifacts and mappings."
            ));
        }
    }

    private static Map<String, Object> phase13M17Completion(
            String packId,
            boolean complete,
            List<String> completedReports,
            List<Map<String, Object>> blockers,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_completion_gate", diagnostics);
        data.put("blockedReportCount", blockers.size());
        data.put("blockers", blockers);
        data.put("completedReports", completedReports);
        data.put("packId", packId);
        data.put("phase13M17Complete", complete);
        data.put("phase13M18Ready", complete);
        data.put("playtestCandidateReady", false);
        data.put("summary", complete
                ? "Phase 13 M17 closeout passed; M18 smoke-session work may begin in a later run."
                : "Phase 13 M17 closeout is blocked by missing, failed, or unsafe launch artifact gates.");
        return data;
    }

    private static Map<String, Object> phase13M18ReadinessAudit(
            String packId,
            boolean ready,
            List<String> completedReports,
            List<Map<String, Object>> blockers,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m18_readiness_audit", diagnostics);
        data.put("allowedNextWork", ready ? List.of("phase13.m18.ashfall_native_smoke_session") : List.of());
        data.put("blockedCapabilities", ready ? List.of() : List.of(
                "minecraft.launch",
                "ashfall.native.smoke_session",
                "first.playtest.candidate"
        ));
        data.put("blockedReportCount", blockers.size());
        data.put("completedReports", completedReports);
        data.put("packId", packId);
        data.put("phase13M18Ready", ready);
        data.put("phase13M17Complete", ready);
        data.put("playtestCandidateReady", false);
        data.put("summary", ready
                ? "M18 may begin as the next controlled native smoke-session milestone."
                : "M18 remains blocked until M17 completion and runtime fixture gates pass.");
        return data;
    }

    private static Map<String, Object> phase13FirstPlaytestBlockers(
            String packId,
            List<Map<String, Object>> blockers,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_first_playtest_blockers", diagnostics);
        data.put("blockerCount", blockers.size());
        data.put("blockers", blockers);
        data.put("packId", packId);
        data.put("firstPlaytestOpen", false);
        data.put("firstPlaytestCandidateReady", false);
        data.put("summary", blockers.isEmpty()
                ? "No M17 blockers remain, but M18 smoke session and M19 playtest packaging still need separate validation."
                : "First playtest remains blocked until M17 and later M18/M19 gates pass.");
        return data;
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
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("safeToAutoPopulate", false);
        data.put("phase", phase);
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
            List<EchoNativeDiagnostic> diagnostics,
            List<Map<String, Object>> blockers
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            addBlocker(blockers, reportName, "report_missing", "Required M17 closeout report is missing.");
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M17-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 report missing",
                    "Required M17 closeout input " + reportName + " was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate the full Phase 13 M17 report chain before running M17 closeout."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void addBlocker(List<Map<String, Object>> blockers, String reportName, String gate, String summary) {
        Map<String, Object> blocker = new LinkedHashMap<>();
        blocker.put("report", reportName);
        blocker.put("gate", gate);
        blocker.put("summary", summary);
        blocker.put("downloadAllowed", false);
        blocker.put("nativeExtractionStarted", false);
        blocker.put("processLaunched", false);
        blocker.put("classloaderCreated", false);
        blocker.put("filesystemMutated", false);
        blockers.add(blocker);
    }

    private static List<Map<String, Object>> uniqueBlockers(List<Map<String, Object>> blockers) {
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> blocker : blockers) {
            byKey.put(blocker.get("report") + "|" + blocker.get("gate"), blocker);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("report")) + "|" + item.get("gate")))
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
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("bytecodeMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
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
