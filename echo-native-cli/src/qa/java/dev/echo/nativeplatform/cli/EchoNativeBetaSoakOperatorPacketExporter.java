package dev.echo.nativeplatform.cli;

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

final class EchoNativeBetaSoakOperatorPacketExporter {
    EchoNativeBetaSoakOperatorPacketOutcome export(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkAcceptedReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, Object> m29Completion = data(reports.get("phase13-m29-completion.json"));
        Map<String, Object> m30Readiness = data(reports.get("phase13-m30-readiness.json"));
        Map<String, Object> sessionInventory = data(reports.get("native-loader-beta-session-inventory.json"));
        Map<String, Object> issueTriage = data(reports.get("native-loader-beta-issue-triage.json"));
        Map<String, Object> regressionWatchlist = data(reports.get("native-loader-beta-regression-watchlist.json"));
        Map<String, Object> m30Completion = data(reports.get("phase13-m30-completion.json"));
        Map<String, Object> noteStatus = data(reports.get("phase13-m29-note-validation-status.json"));

        int sessionCount = firstPositive(
                number(sessionInventory, "sessionCount"),
                number(m29Completion, "sessionCount"),
                number(m30Readiness, "sessionCount"),
                number(m30Completion, "sessionCount")
        );
        int targetSessionCount = firstPositive(
                number(sessionInventory, "targetInternalSessionCount"),
                number(m29Completion, "targetInternalSessionCount"),
                number(m30Readiness, "targetInternalSessionCount"),
                number(m30Completion, "targetInternalSessionCount"),
                3
        );
        int remainingSessionCount = Math.max(targetSessionCount - sessionCount, 0);
        int completeM30EvidenceCount = number(noteStatus, "completeForM30SoakStandardCount");
        int distinctM30LogEvidenceCount = number(noteStatus, "distinctM30LogEvidenceCount");
        int remainingM30EvidenceCount = Math.max(
                Math.max(targetSessionCount - completeM30EvidenceCount, 0),
                Math.max(targetSessionCount - distinctM30LogEvidenceCount, 0)
        );
        int issueCount = number(issueTriage, "issueCount");
        boolean m29Started = bool(m29Completion, "internalBetaSoakStarted")
                || bool(m29Completion, "phase13M29Complete");
        boolean m30Ready = bool(m30Readiness, "phase13M30Ready")
                && bool(m30Readiness, "publicBetaCandidateReady")
                && bool(m30Completion, "phase13M30Complete");

        if (!m29Started) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-SOAK-PACKET-M29-NOT-STARTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta soak operator packet cannot start before M29",
                    "The operator packet requires a started M29 internal beta soak.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/phase13-m29-completion.json"),
                    "Run phase13 intake beta-soak after internal beta feedback evidence exists."
            ));
        }
        if (remainingSessionCount > 0 && m29Started) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-SOAK-PACKET-SESSIONS-REMAINING",
                    EchoNativeIssueSeverity.WARNING,
                    "Internal beta soak still needs more sessions",
                    "The operator packet found " + sessionCount + " clean session(s); target is " + targetSessionCount + ", leaving " + remainingSessionCount + " session(s) before M30 can pass.",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/native-loader-beta-feedback", "reports/echo-native/" + packId + "/native-loader-beta-session-inventory.json"),
                    "Run the remaining internal native-loader sessions, save fixture-local notes/logs/screenshots, then rerun beta intake and M30 verification."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeBetaSoakOperatorPacketOutcome(
                packId,
                operatorPacket(packId, fixture, sessionCount, targetSessionCount, remainingSessionCount, issueCount,
                        completeM30EvidenceCount, distinctM30LogEvidenceCount, remainingM30EvidenceCount, m29Started, m30Ready, m30Completion, regressionWatchlist, sortedDiagnostics),
                sessionTemplate(packId, sessionCount, targetSessionCount, remainingSessionCount, remainingM30EvidenceCount, sortedDiagnostics),
                sessionNoteDrafts(packId, sessionCount, targetSessionCount, remainingSessionCount, remainingM30EvidenceCount, sortedDiagnostics),
                evidenceChecklist(packId, remainingSessionCount, remainingM30EvidenceCount, sortedDiagnostics),
                remainingSessionPlan(packId, sessionCount, targetSessionCount, remainingSessionCount, remainingM30EvidenceCount, sortedDiagnostics),
                operatorStatus(packId, sessionCount, targetSessionCount, remainingSessionCount, remainingM30EvidenceCount, m29Started, m30Ready, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> operatorPacket(
            String packId,
            Path fixture,
            int sessionCount,
            int targetSessionCount,
            int remainingSessionCount,
            int issueCount,
            int completeM30EvidenceCount,
            int distinctM30LogEvidenceCount,
            int remainingM30EvidenceCount,
            boolean m29Started,
            boolean m30Ready,
            Map<String, Object> m30Completion,
            Map<String, Object> regressionWatchlist,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_soak_operator_packet", diagnostics);
        data.put("completeForM30SoakStandardCount", completeM30EvidenceCount);
        data.put("distinctM30LogEvidenceCount", distinctM30LogEvidenceCount);
        data.put("feedbackDirectory", fixture.resolve("native-loader-beta-feedback").toString().replace('\\', '/'));
        data.put("issueCount", issueCount);
        data.put("m29Started", m29Started);
        data.put("m30CompletionStatus", m30Completion.isEmpty() ? "MISSING" : String.valueOf(m30Completion.getOrDefault("summary", "available")));
        data.put("m30Ready", m30Ready);
        data.put("nextCommands", List.of(
                "phase13 intake beta-feedback fixtures/" + packId,
                "phase13 verify m28 fixtures/" + packId,
                "phase13 intake beta-soak fixtures/" + packId,
                "phase13 verify m30 fixtures/" + packId,
                "phase13 export beta-soak-packet fixtures/" + packId
        ));
        data.put("operatorPacketReady", m29Started);
        data.put("packId", packId);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("remainingM30EvidenceCount", remainingM30EvidenceCount);
        data.put("remainingM30EvidenceNotePaths", evidenceNotePaths(packId, sessionCount, remainingM30EvidenceCount));
        data.put("remainingSessionCount", remainingSessionCount);
        data.put("regressionWatchlist", regressionWatchlist.getOrDefault("watchlist", List.of()));
        data.put("sessionCount", sessionCount);
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("summary", remainingSessionCount == 0 && remainingM30EvidenceCount == 0 && m30Ready
                ? "Internal beta soak packet is complete; rerun M30 public beta candidate verification."
                : remainingSessionCount == 0
                ? "Internal beta session target is met, but M30 remains blocked by incomplete stricter evidence."
                : "Internal beta soak packet is ready for more tester sessions; public beta remains closed.");
        return data;
    }

    private static Map<String, Object> sessionTemplate(
            String packId,
            int sessionCount,
            int targetSessionCount,
            int remainingSessionCount,
            int remainingM30EvidenceCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_session_template", diagnostics);
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("crashReported", false);
        template.put("latestLogPreserved", false);
        template.put("launchPath", "native-loader");
        template.put("mainMenuReached", false);
        template.put("notes", "");
        template.put("result", "pass|pass_with_warnings|failed");
        template.put("screenshotCaptured", false);
        template.put("sessionId", "internal-beta-session-N");
        template.put("spawnReached", false);
        template.put("tester", "");
        template.put("worldCreateOrLoad", "created|loaded|both|not_reached");
        data.put("packId", packId);
        data.put("remainingM30EvidenceCount", remainingM30EvidenceCount);
        data.put("remainingSessionCount", remainingSessionCount);
        data.put("sessionCount", sessionCount);
        data.put("suggestedPathPattern", "fixtures/" + packId + "/native-loader-beta-feedback/internal-beta-session-N.md");
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("template", template);
        data.put("summary", "Use this data template for each internal native-loader beta soak note.");
        return data;
    }

    private static Map<String, Object> sessionNoteDrafts(
            String packId,
            int sessionCount,
            int targetSessionCount,
            int remainingSessionCount,
            int remainingM30EvidenceCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_session_note_drafts", diagnostics);
        List<Map<String, Object>> drafts = new ArrayList<>();
        int draftCount = Math.max(remainingSessionCount, remainingM30EvidenceCount);
        for (int index = sessionCount + 1; index <= sessionCount + draftCount; index++) {
            String sessionId = "internal-beta-session-" + index;
            String suggestedPath = realSessionNotePath(packId, index);
            Map<String, Object> draft = new LinkedHashMap<>();
            draft.put("draftOnly", true);
            draft.put("id", sessionId);
            draft.put("qualifiesAsEvidence", false);
            draft.put("suggestedFeedbackPath", suggestedPath);
            draft.put("markdown", String.join("\n",
                    "# Ashfall Native Loader Beta Session " + index,
                    "",
                    "Tester: ",
                    "Session: " + sessionId,
                    "Result: pass",
                    "LaunchPath: native-loader",
                    "MainMenuReached: true",
                    "WorldCreateOrLoad: created",
                    "SpawnReached: true",
                    "CrashReported: false",
                    "LatestLogPreserved: true",
                    "ScreenshotCaptured: false",
                    "SupportBundle: ",
                    "",
                    "Evidence:",
                    "- Preserved fixture log: fixtures/" + packId + "/isolated-runtime/game/logs/<new-distinct-session>.log",
                    "",
                    "Notes:",
                    "- Describe the world create/load result, spawn state, visible errors, and anything unusual."
            ));
            drafts.add(draft);
        }
        data.put("drafts", drafts);
        data.put("packId", packId);
        data.put("publicBetaOpen", false);
        data.put("remainingM30EvidenceCount", remainingM30EvidenceCount);
        data.put("remainingSessionCount", remainingSessionCount);
        data.put("sessionCount", sessionCount);
        data.put("summary", remainingSessionCount == 0 && remainingM30EvidenceCount == 0
                ? "No session note drafts are required before rerunning M30."
                : "Copy these draft bodies into real tester notes after each session, then replace placeholders with observed facts and distinct fixture-relative log evidence.");
        data.put("targetInternalSessionCount", targetSessionCount);
        return data;
    }

    private static Map<String, Object> evidenceChecklist(
            String packId,
            int remainingSessionCount,
            int remainingM30EvidenceCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_evidence_checklist", diagnostics);
        data.put("checklist", List.of(
                item("launch_native_loader", "Launch the native loader through the approved isolated tester path."),
                item("create_or_load_world", "Create a new Ashfall world or load an existing Ashfall world."),
                item("reach_spawn", "Reach playable spawn or document the exact failure boundary."),
                item("capture_screenshot", "Capture at least one screenshot for the session when possible."),
                item("close_cleanly", "Close the game cleanly or preserve crash evidence if it fails."),
                item("preserve_latest_log", "Preserve the latest log in fixture-local beta evidence or support bundle notes."),
                item("preserve_crash_reports", "Preserve crash reports if any appear; otherwise record no-crash evidence."),
                item("write_feedback_note", "Add one fixture-local markdown note under native-loader-beta-feedback."),
                item("rerun_intake", "Rerun beta-feedback, M28, beta-soak, M30, and this operator packet.")
        ));
        data.put("packId", packId);
        data.put("remainingM30EvidenceCount", remainingM30EvidenceCount);
        data.put("remainingM30EvidenceNotePaths", evidenceNotePaths(packId, 3, remainingM30EvidenceCount));
        data.put("remainingSessionCount", remainingSessionCount);
        data.put("summary", "Evidence checklist for remaining internal native-loader beta soak sessions.");
        return data;
    }

    private static Map<String, Object> remainingSessionPlan(
            String packId,
            int sessionCount,
            int targetSessionCount,
            int remainingSessionCount,
            int remainingM30EvidenceCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_remaining_session_plan", diagnostics);
        List<Map<String, Object>> remainingSessions = new ArrayList<>();
        int plannedCount = Math.max(remainingSessionCount, remainingM30EvidenceCount);
        for (int index = sessionCount + 1; index <= sessionCount + plannedCount; index++) {
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("id", "internal-beta-session-" + index);
            session.put("required", true);
            session.put("requiredForM30Evidence", index > sessionCount + remainingSessionCount);
            session.put("suggestedFeedbackPath", realSessionNotePath(packId, index));
            session.put("requiredSignals", List.of(
                    "tester",
                    "result=pass or pass_with_warnings",
                    "launchPath=native-loader",
                    "mainMenuReached=true",
                    "worldCreateOrLoad=created, loaded, or both",
                    "spawnReached=true",
                    "crashReported=false",
                    "latestLogPreserved=true",
                    "fixtureRelativeDistinctLogEvidencePath"
            ));
            session.put("optionalEvidence", List.of(
                    "screenshotCaptured=true",
                    "notes describing spawn/world state",
                    "support bundle reference"
            ));
            remainingSessions.add(session);
        }
        data.put("packId", packId);
        data.put("publicBetaOpen", false);
        data.put("remainingM30EvidenceCount", remainingM30EvidenceCount);
        data.put("remainingSessionCount", remainingSessionCount);
        data.put("remainingSessions", remainingSessions);
        data.put("sessionCount", sessionCount);
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("summary", remainingSessionCount == 0 && remainingM30EvidenceCount == 0
                ? "No remaining internal beta soak sessions are required before rerunning M30."
                : "Run the listed internal beta soak or evidence sessions and write one structured fixture-local note per session.");
        return data;
    }

    private static String realSessionNotePath(String packId, int index) {
        return "fixtures/" + packId + "/native-loader-beta-feedback/internal-beta-session-" + index + ".md";
    }

    private static List<String> evidenceNotePaths(String packId, int sessionCount, int remainingM30EvidenceCount) {
        List<String> paths = new ArrayList<>();
        for (int index = sessionCount + 1; index <= sessionCount + remainingM30EvidenceCount; index++) {
            paths.add(realSessionNotePath(packId, index));
        }
        return paths;
    }

    private static Map<String, Object> operatorStatus(
            String packId,
            int sessionCount,
            int targetSessionCount,
            int remainingSessionCount,
            int remainingM30EvidenceCount,
            boolean m29Started,
            boolean m30Ready,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_soak_operator_status", diagnostics);
        data.put("m30StillBlocked", !m30Ready);
        data.put("operatorPacketReady", m29Started);
        data.put("packId", packId);
        data.put("phase13M29OperatorPacketReady", m29Started);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("remainingM30EvidenceCount", remainingM30EvidenceCount);
        data.put("remainingSessionCount", remainingSessionCount);
        data.put("sessionCount", sessionCount);
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("summary", remainingSessionCount == 0 && remainingM30EvidenceCount == 0
                ? (m30Ready
                ? "Internal beta session target is met; rerun M30 candidate verification."
                : "Internal beta session target is met, but M30 remains blocked by incomplete stricter evidence.")
                : "Internal beta soak operator packet is ready and M30 remains blocked by remaining session count.");
        return data;
    }

    private static Map<String, Object> item(String id, String summary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("summary", summary);
        return item;
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
                    "ECHO-NATIVE-BETA-SOAK-PACKET-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta soak operator packet required report missing",
                    "The beta soak operator packet requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Run beta-feedback, M28, beta-soak, and M30 verification before exporting the soak packet."
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
        if ("native-loader-public-beta-candidate-audit.json".equals(reportName)
                || "phase13-m30-completion.json".equals(reportName)) {
            return;
        }
        if (!"PASS".equals(status) && !"PASS_WITH_WARNINGS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-SOAK-PACKET-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta soak operator packet upstream report is not accepted",
                    "The beta soak operator packet requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream beta feedback, widening, or soak diagnostics before exporting the packet."
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

    private static int firstPositive(int... values) {
        for (int value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
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
        data.put("gameProcessLaunched", false);
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("jarsMutated", false);
        data.put("launcherInstallsMutated", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("publicBetaOpen", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
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
