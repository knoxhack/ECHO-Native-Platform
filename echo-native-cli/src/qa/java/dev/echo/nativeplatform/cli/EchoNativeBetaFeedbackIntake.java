package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeBetaFeedbackIntake {
    EchoNativeBetaFeedbackOutcome intake(
            String packId,
            Path fixture,
            List<EchoNativeAddonDescriptor> descriptors,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkAcceptedReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        List<Map<String, Object>> evidenceFiles = evidenceFiles(fixture);
        long feedbackArtifactCount = evidenceFiles.stream()
                .filter(file -> "native_loader_beta_feedback".equals(file.get("kind")) || "native_loader_beta_notes".equals(file.get("kind")))
                .count();
        long screenshotCount = evidenceFiles.stream()
                .filter(file -> "screenshot".equals(file.get("kind")))
                .count();

        Map<String, Object> testerEvidence = data(reports.get("tester-playable-evidence.json"));
        boolean playableBaseline = bool(testerEvidence, "baselinePlayableEvidence");
        int crashReportCount = number(testerEvidence, "crashReportCount");
        int activeCrashReportCount = number(testerEvidence, "activeCrashReportCount");
        boolean crashSignal = bool(testerEvidence, "crashSignalInLatestLog");
        boolean noCrashEvidence = playableBaseline && activeCrashReportCount == 0 && !crashSignal;
        boolean structuredFeedbackPresent = feedbackArtifactCount > 0;
        boolean betaGateOpen = bool(data(reports.get("internal-tester-beta-gate.json")), "internalTesterBetaOpen")
                || bool(data(reports.get("native-loader-playable-beta-readiness.json")), "internalTesterBetaReady");
        boolean m26Complete = bool(data(reports.get("phase13-m26-completion.json")), "phase13M26Complete");
        boolean m27Complete = betaGateOpen && m26Complete && playableBaseline;
        boolean m28Ready = m27Complete && structuredFeedbackPresent && noCrashEvidence;

        if (!structuredFeedbackPresent) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-FEEDBACK-NOTES-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Structured beta tester feedback is not present",
                    "The native loader beta has playable evidence, but no fixture-local tester notes or feedback files were found.",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/native-loader-beta-feedback", "fixtures/" + packId + "/native-loader-beta-notes"),
                    "Add tester notes or structured feedback before widening the beta or starting larger triage work."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeBetaFeedbackOutcome(
                packId,
                feedbackInventory(packId, evidenceFiles, feedbackArtifactCount, screenshotCount, playableBaseline, structuredFeedbackPresent, sortedDiagnostics),
                crashIntake(packId, noCrashEvidence, activeCrashReportCount, crashSignal, testerEvidence, sortedDiagnostics),
                knownIssues(packId, descriptors, structuredFeedbackPresent, sortedDiagnostics),
                nextActionQueue(packId, m27Complete, m28Ready, structuredFeedbackPresent, noCrashEvidence, sortedDiagnostics),
                phase13M27Completion(packId, m27Complete, playableBaseline, betaGateOpen, feedbackArtifactCount, activeCrashReportCount, sortedDiagnostics),
                phase13M28Readiness(packId, m28Ready, structuredFeedbackPresent, noCrashEvidence, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> feedbackInventory(
            String packId,
            List<Map<String, Object>> evidenceFiles,
            long feedbackArtifactCount,
            long screenshotCount,
            boolean playableBaseline,
            boolean structuredFeedbackPresent,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m27_beta_feedback_inventory", diagnostics);
        data.put("evidenceFiles", evidenceFiles);
        data.put("evidenceFileCount", evidenceFiles.size());
        data.put("feedbackArtifactCount", feedbackArtifactCount);
        data.put("packId", packId);
        data.put("playableBaselineEvidence", playableBaseline);
        data.put("screenshotCount", screenshotCount);
        data.put("structuredFeedbackPresent", structuredFeedbackPresent);
        data.put("summary", structuredFeedbackPresent
                ? "Fixture-local beta tester feedback artifacts are present."
                : "Playable baseline evidence is present, but structured beta tester feedback still needs to be added.");
        return data;
    }

    private static Map<String, Object> crashIntake(
            String packId,
            boolean noCrashEvidence,
            int crashReportCount,
            boolean crashSignal,
            Map<String, Object> testerEvidence,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m27_beta_crash_intake", diagnostics);
        data.put("crashReportCount", crashReportCount);
        data.put("crashReports", list(testerEvidence.get("crashReports")));
        data.put("crashSignalInLatestLog", crashSignal);
        data.put("latestLog", string(testerEvidence.get("latestLog")));
        data.put("noCrashEvidence", noCrashEvidence);
        data.put("packId", packId);
        data.put("summary", noCrashEvidence
                ? "The latest internal beta evidence contains no crash reports or crash signals."
                : "Crash evidence requires triage before widening the native loader beta.");
        return data;
    }

    private static Map<String, Object> knownIssues(
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            boolean structuredFeedbackPresent,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m27_beta_known_issues", diagnostics);
        List<Map<String, Object>> issues = new ArrayList<>();
        if (!structuredFeedbackPresent) {
            issues.add(issue(
                    "BETA-FEEDBACK-001",
                    "needs_feedback",
                    "Structured beta tester notes are missing",
                    "Collect tester notes for launch, world creation/load, screenshots, perceived stability, and Ashfall module behavior.",
                    "nonblocking_for_internal_beta",
                    List.of("fixtures/" + packId + "/native-loader-beta-feedback", "fixtures/" + packId + "/native-loader-beta-notes")
            ));
        }
        issues.add(issue(
                "BETA-SCOPE-001",
                "known_limitation",
                "Internal tester beta is not public release",
                "M26 opens only the internal native-loader beta path. Public beta, public release, and standalone runtime work remain closed.",
                "nonblocking_for_internal_beta",
                List.of("reports/echo-native/" + packId + "/internal-tester-beta-gate.json")
        ));
        data.put("descriptorCount", descriptors.size());
        data.put("issueCount", issues.size());
        data.put("issues", issues);
        data.put("packId", packId);
        data.put("summary", issues.isEmpty()
                ? "No beta known issues were identified from fixture-local evidence."
                : "Native loader beta known issues were captured for tester iteration.");
        return data;
    }

    private static Map<String, Object> nextActionQueue(
            String packId,
            boolean m27Complete,
            boolean m28Ready,
            boolean structuredFeedbackPresent,
            boolean noCrashEvidence,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m27_beta_next_action_queue", diagnostics);
        List<Map<String, Object>> actions = new ArrayList<>();
        if (!structuredFeedbackPresent) {
            actions.add(action(
                    "collect.beta.feedback",
                    "Collect structured internal beta feedback",
                    "Add tester notes or feedback files for the successful world-create/load session.",
                    "tester_intake_agent",
                    List.of("fixtures/" + packId + "/native-loader-beta-feedback", "fixtures/" + packId + "/native-loader-beta-notes")
            ));
        }
        if (!noCrashEvidence) {
            actions.add(action(
                    "triage.beta.crashes",
                    "Triage native loader beta crash evidence",
                    "Inspect latest.log and crash reports before widening the beta audience.",
                    "crash_triage_agent",
                    List.of("fixtures/" + packId + "/isolated-runtime/game/logs/latest.log", "fixtures/" + packId + "/isolated-runtime/game/crash-reports")
            ));
        }
        actions.add(action(
                "continue.internal.beta.iteration",
                "Run another internal native loader beta session",
                "Repeat launch, world load, screenshot, shutdown, evidence intake, and playable-beta verification.",
                "native_loader_beta_agent",
                List.of("reports/echo-native/" + packId + "/native-loader-playable-beta-readiness.json")
        ));
        data.put("actionCount", actions.size());
        data.put("actions", actions);
        data.put("m27Complete", m27Complete);
        data.put("m28Ready", m28Ready);
        data.put("packId", packId);
        data.put("summary", m28Ready
                ? "Native loader beta feedback is ready for the next widening/triage milestone."
                : "Native loader beta should continue in internal tester iteration before widening.");
        return data;
    }

    private static Map<String, Object> phase13M27Completion(
            String packId,
            boolean m27Complete,
            boolean playableBaseline,
            boolean betaGateOpen,
            long feedbackArtifactCount,
            int crashReportCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m27_internal_beta_feedback_intake_completion", diagnostics);
        data.put("betaGateOpen", betaGateOpen);
        data.put("crashReportCount", crashReportCount);
        data.put("feedbackArtifactCount", feedbackArtifactCount);
        data.put("internalBetaIterationStarted", m27Complete);
        data.put("packId", packId);
        data.put("phase13M27Complete", m27Complete);
        data.put("phase13M28Ready", m27Complete && feedbackArtifactCount > 0 && crashReportCount == 0);
        data.put("playableBaselineEvidence", playableBaseline);
        data.put("summary", m27Complete
                ? "M27 is active: internal native loader beta tester iteration has captured playable evidence."
                : "M27 remains blocked until M26 beta readiness and tester playable evidence pass.");
        return data;
    }

    private static Map<String, Object> phase13M28Readiness(
            String packId,
            boolean m28Ready,
            boolean structuredFeedbackPresent,
            boolean noCrashEvidence,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m28_native_loader_beta_widening_readiness", diagnostics);
        data.put("noCrashEvidence", noCrashEvidence);
        data.put("packId", packId);
        data.put("phase13M28Ready", m28Ready);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("structuredFeedbackPresent", structuredFeedbackPresent);
        data.put("summary", m28Ready
                ? "M28 may begin: internal tester feedback and no-crash evidence are present."
                : "M28 remains closed until structured tester feedback and clean crash intake are both present.");
        return data;
    }

    private static Map<String, Object> issue(
            String id,
            String category,
            String title,
            String summary,
            String severity,
            List<String> likelyFiles
    ) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("id", id);
        issue.put("category", category);
        issue.put("title", title);
        issue.put("summary", summary);
        issue.put("severity", severity);
        issue.put("likelyFiles", likelyFiles);
        return issue;
    }

    private static Map<String, Object> action(
            String id,
            String title,
            String summary,
            String owner,
            List<String> likelyFiles
    ) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("title", title);
        action.put("summary", summary);
        action.put("owner", owner);
        action.put("likelyFiles", likelyFiles);
        return action;
    }

    private static List<Map<String, Object>> evidenceFiles(Path fixture) throws IOException {
        List<Map<String, Object>> files = new ArrayList<>();
        collectFiles(files, "native_loader_beta_feedback", fixture.resolve("native-loader-beta-feedback"));
        collectFiles(files, "native_loader_beta_notes", fixture.resolve("native-loader-beta-notes"));
        collectFiles(files, "crash_report", fixture.resolve("isolated-runtime/game/crash-reports"));
        collectFiles(files, "screenshot", fixture.resolve("isolated-runtime/game/screenshots"));
        collectFiles(files, "latest_log", fixture.resolve("isolated-runtime/game/logs"));
        files.sort(Comparator.<Map<String, Object>, String>comparing(file -> String.valueOf(file.get("kind")))
                .thenComparing(file -> String.valueOf(file.get("path"))));
        return files;
    }

    private static void collectFiles(List<Map<String, Object>> files, String kind, Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            for (Path file : stream.filter(Files::isRegularFile).filter(EchoNativeBetaFeedbackIntake::isFeedbackEvidenceFile).sorted().toList()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("kind", kind);
                entry.put("path", relativePath(file));
                entry.put("byteSize", Files.size(file));
                files.add(entry);
            }
        }
    }

    private static boolean isFeedbackEvidenceFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return !name.contains("template")
                && !name.contains("draft")
                && !name.contains("example")
                && !name.contains("sample");
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
                    "ECHO-NATIVE-BETA-FEEDBACK-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta feedback intake required report missing",
                    "The beta feedback intake requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Regenerate native loader beta readiness and tester evidence reports before M27 intake."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkAcceptedReport(
            String reportName,
            Path reportPath,
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
                    "ECHO-NATIVE-BETA-FEEDBACK-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta feedback upstream report is not accepted",
                    "The beta feedback intake requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream native loader beta diagnostics before M27 intake."
            ));
        }
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean bool(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof Boolean bool && bool;
    }

    private static int number(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("configsMutated", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("downloadsStarted", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("jarsMutated", false);
        data.put("launcherInstallsMutated", false);
        data.put("libraryDownloadStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("savesMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("userCachesMutated", false);
        return data;
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
