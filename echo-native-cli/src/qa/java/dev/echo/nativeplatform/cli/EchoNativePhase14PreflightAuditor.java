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
import java.util.stream.Stream;

final class EchoNativePhase14PreflightAuditor {
    EchoNativePhase14PreflightOutcome audit(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        List<Map<String, Object>> evidenceLocations = evidenceLocations(fixture);
        List<Map<String, Object>> evidenceFiles = evidenceFiles(evidenceLocations);
        long feedbackArtifactCount = evidenceFiles.stream()
                .filter(file -> "tester_feedback".equals(file.get("kind")) || "tester_notes".equals(file.get("kind")))
                .count();
        long crashArtifactCount = evidenceFiles.stream()
                .filter(file -> "crash_report".equals(file.get("kind")))
                .count();

        boolean firstPlaytestOpen = Boolean.TRUE.equals(nestedDataValue(reports, "first-playtest-open-gate.json", "firstPlaytestOpen"));
        boolean safeToOpenFirstPlaytest = Boolean.TRUE.equals(nestedDataValue(reports, "first-playtest-open-gate.json", "safeToOpenFirstPlaytest"));
        boolean m19Complete = Boolean.TRUE.equals(nestedDataValue(reports, "phase13-m19-completion.json", "phase13M19Complete"));
        boolean testerFeedbackPresent = feedbackArtifactCount > 0;
        boolean crashDataPresent = crashArtifactCount > 0;
        if (diagnostics.stream().noneMatch(EchoNativePhase14PreflightAuditor::isBlocking) && !testerFeedbackPresent && !crashDataPresent) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE14-WAITING-FOR-TESTER-EVIDENCE",
                    EchoNativeIssueSeverity.WARNING,
                    "Phase 14 preflight is waiting for first-playtest evidence",
                    "The internal first-playtest gate is open, but no fixture-local tester feedback, notes, or crash-report evidence was found.",
                    null,
                    packId,
                    evidenceLocations.stream().map(location -> String.valueOf(location.get("path"))).toList(),
                    "Collect tester feedback or crash/no-crash evidence in the fixture-local intake paths before starting Phase 14 implementation."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        boolean gatesPass = sortedDiagnostics.stream().noneMatch(EchoNativePhase14PreflightAuditor::isBlocking)
                && firstPlaytestOpen
                && safeToOpenFirstPlaytest
                && m19Complete;
        boolean feedbackReady = testerFeedbackPresent || crashDataPresent;
        boolean phase14Ready = gatesPass && feedbackReady;
        List<Map<String, Object>> actions = nextActions(phase14Ready, feedbackReady);

        return new EchoNativePhase14PreflightOutcome(
                packId,
                postOpenIntake(packId, fixture, firstPlaytestOpen, phase14Ready, feedbackReady, evidenceFiles, sortedDiagnostics),
                feedbackInventory(packId, evidenceLocations, evidenceFiles, feedbackArtifactCount, crashArtifactCount, sortedDiagnostics),
                waitingChecklist(packId, feedbackReady, actions, sortedDiagnostics),
                preflightAudit(packId, gatesPass, feedbackReady, phase14Ready, reports, sortedDiagnostics),
                phase14Readiness(packId, phase14Ready, feedbackReady, firstPlaytestOpen, sortedDiagnostics),
                nextActions(packId, actions, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> postOpenIntake(
            String packId,
            Path fixture,
            boolean firstPlaytestOpen,
            boolean phase14Ready,
            boolean feedbackReady,
            List<Map<String, Object>> evidenceFiles,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase14_first_playtest_post_open_intake", diagnostics);
        data.put("fixture", fixture.toString().replace('\\', '/'));
        data.put("firstPlaytestOpen", firstPlaytestOpen);
        data.put("phase14Blocked", !phase14Ready);
        data.put("phase14Ready", phase14Ready);
        data.put("postOpenEvidenceReady", feedbackReady);
        data.put("evidenceArtifactCount", evidenceFiles.size());
        data.put("packId", packId);
        data.put("publicPlaytestOpen", false);
        data.put("summary", phase14Ready
                ? "First-playtest evidence is present; Phase 14 preflight may proceed."
                : "First playtest is open, but Phase 14 remains blocked until tester evidence is captured.");
        return data;
    }

    private static Map<String, Object> feedbackInventory(
            String packId,
            List<Map<String, Object>> evidenceLocations,
            List<Map<String, Object>> evidenceFiles,
            long feedbackArtifactCount,
            long crashArtifactCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase14_first_playtest_feedback_inventory", diagnostics);
        data.put("crashArtifactCount", crashArtifactCount);
        data.put("evidenceArtifactCount", evidenceFiles.size());
        data.put("evidenceArtifacts", evidenceFiles);
        data.put("feedbackArtifactCount", feedbackArtifactCount);
        data.put("intakeLocations", evidenceLocations);
        data.put("intakeLocationCount", evidenceLocations.size());
        data.put("packId", packId);
        data.put("testerFeedbackPresent", feedbackArtifactCount > 0);
        data.put("crashDataPresent", crashArtifactCount > 0);
        data.put("summary", evidenceFiles.isEmpty()
                ? "No fixture-local tester feedback or crash intake artifacts were found."
                : "Fixture-local tester feedback/crash intake artifacts were found.");
        return data;
    }

    private static Map<String, Object> waitingChecklist(
            String packId,
            boolean feedbackReady,
            List<Map<String, Object>> actions,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase14_first_playtest_waiting_checklist", diagnostics);
        data.put("actionCount", actions.size());
        data.put("actions", actions);
        data.put("checklistReady", !feedbackReady);
        data.put("packId", packId);
        data.put("phase14Blocked", !feedbackReady);
        data.put("summary", feedbackReady
                ? "Tester evidence exists; use the Phase 14 preflight audit before implementation."
                : "Waiting checklist is active until tester feedback or crash/no-crash evidence is captured.");
        return data;
    }

    private static Map<String, Object> preflightAudit(
            String packId,
            boolean gatesPass,
            boolean feedbackReady,
            boolean phase14Ready,
            Map<String, Map<String, Object>> reports,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase14_preflight_audit", diagnostics);
        data.put("firstPlaytestOpen", Boolean.TRUE.equals(nestedDataValue(reports, "first-playtest-open-gate.json", "firstPlaytestOpen")));
        data.put("m19Complete", Boolean.TRUE.equals(nestedDataValue(reports, "phase13-m19-completion.json", "phase13M19Complete")));
        data.put("phase14Blocked", !phase14Ready);
        data.put("phase14Ready", phase14Ready);
        data.put("postOpenEvidenceReady", feedbackReady);
        data.put("preflightGatePassed", gatesPass && feedbackReady);
        data.put("requiredReportCount", reports.size());
        data.put("packId", packId);
        data.put("summary", phase14Ready
                ? "Phase 14 preflight gates pass with tester evidence."
                : "Phase 14 preflight is blocked until tester evidence is inspected.");
        return data;
    }

    private static Map<String, Object> phase14Readiness(
            String packId,
            boolean phase14Ready,
            boolean feedbackReady,
            boolean firstPlaytestOpen,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase14_readiness", diagnostics);
        data.put("firstPlaytestOpen", firstPlaytestOpen);
        data.put("phase14Blocked", !phase14Ready);
        data.put("phase14Ready", phase14Ready);
        data.put("postOpenEvidenceReady", feedbackReady);
        data.put("standaloneRuntimeImplementationStarted", false);
        data.put("packId", packId);
        data.put("summary", phase14Ready
                ? "Phase 14 may be planned from inspected tester evidence."
                : "Phase 14 standalone runtime implementation remains blocked.");
        return data;
    }

    private static Map<String, Object> nextActions(
            String packId,
            List<Map<String, Object>> actions,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase14_next_actions", diagnostics);
        data.put("actionCount", actions.size());
        data.put("actions", actions);
        data.put("packId", packId);
        data.put("summary", actions.isEmpty()
                ? "No Phase 14 preflight actions remain."
                : "Phase 14 preflight actions are waiting on local tester evidence.");
        return data;
    }

    private static List<Map<String, Object>> nextActions(boolean phase14Ready, boolean feedbackReady) {
        if (phase14Ready) {
            return List.of();
        }
        List<Map<String, Object>> actions = new ArrayList<>();
        if (!feedbackReady) {
            actions.add(action(
                    "collect.first-playtest.feedback",
                    "Collect fixture-local tester feedback",
                    "Add tester notes or structured feedback under fixtures/ashfall/playtest-feedback or fixtures/ashfall/tester-notes.",
                    List.of("fixtures/ashfall/playtest-feedback", "fixtures/ashfall/tester-notes")
            ));
            actions.add(action(
                    "collect.first-playtest.crash-evidence",
                    "Collect crash or no-crash evidence",
                    "Add crash reports, support-bundle notes, or an explicit no-crash tester note under the fixture-local intake paths.",
                    List.of("fixtures/ashfall/crash-reports", "fixtures/ashfall/support-intake")
            ));
            actions.add(action(
                    "gate.phase14.preflight",
                    "Rerun Phase 14 preflight",
                    "Run echo-native phase14 preflight fixtures/ashfall after evidence is present.",
                    List.of("reports/echo-native/ashfall/phase14-preflight-audit.json")
            ));
        }
        return actions.stream()
                .sorted(Comparator.comparing(action -> String.valueOf(action.get("id"))))
                .toList();
    }

    private static Map<String, Object> action(String id, String title, String summary, List<String> files) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("title", title);
        action.put("summary", summary);
        action.put("files", files);
        action.put("downloadAllowed", false);
        action.put("nativeExtractionStarted", false);
        action.put("processLaunched", false);
        action.put("classloaderCreated", false);
        action.put("filesystemMutated", false);
        return action;
    }

    private static List<Map<String, Object>> evidenceLocations(Path fixture) {
        return List.of(
                location("tester_feedback", fixture.resolve("playtest-feedback")),
                location("tester_notes", fixture.resolve("tester-notes")),
                location("crash_report", fixture.resolve("crash-reports")),
                location("support_intake", fixture.resolve("support-intake"))
        );
    }

    private static Map<String, Object> location(String kind, Path path) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("kind", kind);
        item.put("path", path.toString().replace('\\', '/'));
        item.put("exists", Files.isDirectory(path));
        return item;
    }

    private static List<Map<String, Object>> evidenceFiles(List<Map<String, Object>> evidenceLocations) throws IOException {
        List<Map<String, Object>> files = new ArrayList<>();
        for (Map<String, Object> location : evidenceLocations) {
            Path directory = Path.of(String.valueOf(location.get("path")));
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(directory)) {
                List<Path> paths = stream
                        .filter(Files::isRegularFile)
                        .filter(EchoNativePhase14PreflightAuditor::isEvidenceFile)
                        .sorted()
                        .toList();
                for (Path path : paths) {
                    Map<String, Object> file = new LinkedHashMap<>();
                    file.put("byteSize", Files.size(path));
                    file.put("kind", location.get("kind"));
                    file.put("path", path.toString().replace('\\', '/'));
                    files.add(file);
                }
            }
        }
        return files.stream()
                .sorted(Comparator.comparing(file -> String.valueOf(file.get("kind")) + "|" + file.get("path")))
                .toList();
    }

    private static boolean isEvidenceFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".json")
                || name.endsWith(".md")
                || name.endsWith(".txt")
                || name.endsWith(".log");
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
                    "ECHO-NATIVE-PHASE14-PREFLIGHT-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 14 preflight required report missing",
                    "Phase 14 preflight requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Regenerate the Phase 13 M19 first-playtest gate before Phase 14 preflight."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkReport(
            String reportName,
            Path path,
            Map<String, Object> report,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        String status = String.valueOf(report.getOrDefault("status", "MISSING"));
        if (!"PASS".equals(status) && !"PASS_WITH_WARNINGS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE14-UPSTREAM-REPORT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 14 upstream report is not PASS",
                    "Phase 14 preflight requires PASS or accepted PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Resolve M19 candidate and first-playtest open-gate reports before Phase 14."
            ));
        }
        if (hasUnsafeRuntimeWork(EchoNativeJson.asObject(report.get("data")))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE14-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 14 upstream report contains unsafe runtime work",
                    reportName + " indicates work that is not allowed during Phase 14 preflight.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep Phase 14 preflight report-only until tester evidence is inspected."
            ));
        }
    }

    private static Object nestedDataValue(Map<String, Map<String, Object>> reports, String reportName, String field) {
        Map<String, Object> report = reports.getOrDefault(reportName, Map.of());
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        Object current = data;
        for (String part : field.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
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
        data.put("publicPlaytestOpen", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("safeToAutoPopulate", false);
        data.put("phase", phase);
        data.put("standaloneRuntimeImplementationStarted", false);
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

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
