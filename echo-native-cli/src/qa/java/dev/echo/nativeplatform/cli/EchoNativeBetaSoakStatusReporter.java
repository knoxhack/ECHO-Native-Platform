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

final class EchoNativeBetaSoakStatusReporter {
    private static final int TARGET_INTERNAL_SESSION_COUNT = 3;

    EchoNativeBetaSoakStatusOutcome report(
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
        Map<String, Object> noteStatus = data(reports.get("phase13-m29-note-validation-status.json"));
        Map<String, Object> noteValidation = data(reports.get("native-loader-beta-session-note-validation.json"));
        Map<String, Object> evidenceQuality = data(reports.get("native-loader-beta-evidence-quality.json"));

        int currentQualified = number(noteStatus, "currentM29QualifiedSessionCount", number(m30Readiness, "sessionCount", 0));
        int completeSoakStandard = number(noteStatus, "completeForM30SoakStandardCount", 0);
        int distinctM30LogEvidenceCount = number(noteStatus, "distinctM30LogEvidenceCount", completeSoakStandard);
        int ignoredDraftCount = number(noteStatus, "ignoredDraftCount", 0);
        int remaining = Math.max(0, TARGET_INTERNAL_SESSION_COUNT - currentQualified);
        int remainingM30Evidence = Math.max(
                Math.max(0, TARGET_INTERNAL_SESSION_COUNT - completeSoakStandard),
                Math.max(0, TARGET_INTERNAL_SESSION_COUNT - distinctM30LogEvidenceCount)
        );
        boolean m29Active = bool(m29Completion, "phase13M29Complete");
        boolean rawM30Ready = bool(m30Readiness, "phase13M30Ready");
        boolean m30Ready = rawM30Ready
                && completeSoakStandard >= TARGET_INTERNAL_SESSION_COUNT
                && distinctM30LogEvidenceCount >= TARGET_INTERNAL_SESSION_COUNT;
        boolean publicBetaOpen = false;

        if (!m29Active) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-SOAK-STATUS-M29-INACTIVE",
                    EchoNativeIssueSeverity.ERROR,
                    "M29 beta soak is not active",
                    "The beta soak status dashboard requires an active Phase 13 M29 completion report.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/phase13-m29-completion.json"),
                    "Resolve M29 intake before producing the beta soak status dashboard."
            ));
        }

        if (!m30Ready && remaining > 0) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-SOAK-STATUS-MORE-SESSIONS-REQUIRED",
                    EchoNativeIssueSeverity.WARNING,
                    "More internal beta sessions are required",
                    "The beta soak has " + currentQualified + " current M29-qualified session note(s); " + remaining + " more are required before M30 can pass.",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/native-loader-beta-feedback", "reports/echo-native/" + packId + "/phase13-m30-readiness.json"),
                    "Run additional native-loader beta sessions and capture real non-draft notes."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeBetaSoakStatusOutcome(
                packId,
                dashboard(packId, currentQualified, completeSoakStandard, distinctM30LogEvidenceCount, ignoredDraftCount, remaining, remainingM30Evidence, m29Active, m30Ready, publicBetaOpen, noteValidation, evidenceQuality, sortedDiagnostics),
                checklist(packId, currentQualified, completeSoakStandard, distinctM30LogEvidenceCount, remaining, remainingM30Evidence, m30Ready, publicBetaOpen, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> dashboard(
            String packId,
            int currentQualified,
            int completeSoakStandard,
            int distinctM30LogEvidenceCount,
            int ignoredDraftCount,
            int remaining,
            int remainingM30Evidence,
            boolean m29Active,
            boolean m30Ready,
            boolean publicBetaOpen,
            Map<String, Object> noteValidation,
            Map<String, Object> evidenceQuality,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_soak_status_dashboard", diagnostics);
        data.put("completeForM30SoakStandardCount", completeSoakStandard);
        data.put("currentM29QualifiedSessionCount", currentQualified);
        data.put("distinctM30LogEvidenceCount", distinctM30LogEvidenceCount);
        data.put("ignoredDraftCount", ignoredDraftCount);
        data.put("m29Active", m29Active);
        data.put("m30Ready", m30Ready);
        data.put("nextRequiredSessionIds", nextSessionIds(currentQualified, remaining));
        data.put("nextRequiredM30EvidenceSessionIds", nextEvidenceSessionIds(currentQualified, remainingM30Evidence));
        data.put("noteCount", number(noteValidation, "noteCount", 0));
        data.put("packId", packId);
        data.put("publicBetaOpen", publicBetaOpen);
        data.put("publicReleaseReady", false);
        data.put("remainingM30SoakStandardCount", Math.max(0, TARGET_INTERNAL_SESSION_COUNT - completeSoakStandard));
        data.put("remainingM30DistinctEvidenceCount", Math.max(0, TARGET_INTERNAL_SESSION_COUNT - distinctM30LogEvidenceCount));
        data.put("remainingM30EvidenceCount", remainingM30Evidence);
        data.put("remainingQualifiedSessionCount", remaining);
        data.put("remainingM30EvidencePackets", remainingEvidencePackets(packId, currentQualified, remainingM30Evidence));
        data.put("remainingSessionPackets", remainingSessionPackets(packId, currentQualified, remaining));
        data.put("sessionProofMatrixReady", number(evidenceQuality, "qualifiedSessionCount", 0) >= currentQualified);
        data.put("suggestedRealNotePaths", suggestedRealNotePaths(packId, currentQualified, remaining));
        data.put("suggestedM30EvidenceNotePaths", suggestedEvidenceNotePaths(packId, currentQualified, remainingM30Evidence));
        data.put("targetInternalSessionCount", TARGET_INTERNAL_SESSION_COUNT);
        data.put("summary", m30Ready
                ? "Internal beta soak evidence is ready for M30 public beta candidate verification."
                : "Internal beta soak has enough clean sessions, but M30 remains closed until the stricter note evidence standard is complete.");
        return data;
    }

    private static Map<String, Object> checklist(
            String packId,
            int currentQualified,
            int completeSoakStandard,
            int distinctM30LogEvidenceCount,
            int remaining,
            int remainingM30Evidence,
            boolean m30Ready,
            boolean publicBetaOpen,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_next_session_checklist", diagnostics);
        data.put("checklist", List.of(
                item("launch.native.loader", "Launch through the native loader beta path."),
                item("reach.main.menu", "Confirm the main menu is reached."),
                item("create.or.load.world", "Create or load an Ashfall world."),
                item("reach.spawn", "Reach spawn or a playable in-world state."),
                item("record.no.crash", "Record that no crash occurred during the session."),
                item("preserve.latest.log", "Preserve latest.log for the session."),
                item("write.real.note", "Create a real non-draft note under native-loader-beta-feedback with tester, result, main menu, world, spawn, no-crash, and latest.log evidence."),
                item("rerun.gates", "Rerun beta-feedback intake, M28, beta-soak intake, beta-session-note validation, evidence audit, and M30 verification.")
        ));
        data.put("completeForM30SoakStandardCount", completeSoakStandard);
        data.put("currentM29QualifiedSessionCount", currentQualified);
        data.put("distinctM30LogEvidenceCount", distinctM30LogEvidenceCount);
        data.put("doesNotCountAsEvidence", List.of(
                "Files under native-loader-beta-feedback/_drafts",
                "Filenames containing draft, template, example, or sample",
                "Screenshots without a structured session note",
                "Logs without a structured session note",
                "Duplicate session notes pointing to the same preserved log evidence",
                "A note from a session that was not actually run"
        ));
        data.put("m30Ready", m30Ready);
        data.put("nextRequiredSessionIds", nextSessionIds(currentQualified, remaining));
        data.put("nextRequiredM30EvidenceSessionIds", nextEvidenceSessionIds(currentQualified, remainingM30Evidence));
        data.put("packId", packId);
        data.put("publicBetaOpen", publicBetaOpen);
        data.put("remainingM30SoakStandardCount", Math.max(0, TARGET_INTERNAL_SESSION_COUNT - completeSoakStandard));
        data.put("remainingM30DistinctEvidenceCount", Math.max(0, TARGET_INTERNAL_SESSION_COUNT - distinctM30LogEvidenceCount));
        data.put("remainingM30EvidenceCount", remainingM30Evidence);
        data.put("remainingQualifiedSessionCount", remaining);
        data.put("remainingM30EvidencePackets", remainingEvidencePackets(packId, currentQualified, remainingM30Evidence));
        data.put("remainingSessionPackets", remainingSessionPackets(packId, currentQualified, remaining));
        data.put("rerunCommands", List.of(
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 intake beta-feedback fixtures/" + packId + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 verify m28 fixtures/" + packId + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 intake beta-soak fixtures/" + packId + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 validate beta-session-notes fixtures/" + packId + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 audit beta-soak-evidence fixtures/" + packId + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 verify m30 fixtures/" + packId + "\""
        ));
        data.put("suggestedRealNotePaths", suggestedRealNotePaths(packId, currentQualified, remaining));
        data.put("suggestedM30EvidenceNotePaths", suggestedEvidenceNotePaths(packId, currentQualified, remainingM30Evidence));
        data.put("targetInternalSessionCount", TARGET_INTERNAL_SESSION_COUNT);
        data.put("summary", remaining == 0 && remainingM30Evidence == 0
                ? "No additional session checklist slots are required before rerunning M30."
                : remaining == 0
                ? "Raw M29 session count is met, but run and document " + remainingM30Evidence + " additional distinct M30 evidence session(s)."
                : "Run and document " + remaining + " additional real native-loader beta session(s).");
        return data;
    }

    private static Map<String, Object> item(String id, String summary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("summary", summary);
        return item;
    }

    private static List<String> nextSessionIds(int currentQualified, int remaining) {
        List<String> ids = new ArrayList<>();
        for (int index = 1; index <= remaining; index++) {
            ids.add("internal-beta-session-" + (currentQualified + index));
        }
        return ids;
    }

    private static List<String> nextEvidenceSessionIds(int currentQualified, int remainingM30Evidence) {
        List<String> ids = new ArrayList<>();
        for (int index = 1; index <= remainingM30Evidence; index++) {
            ids.add("internal-beta-session-" + (currentQualified + index));
        }
        return ids;
    }

    private static List<String> suggestedRealNotePaths(String packId, int currentQualified, int remaining) {
        List<String> paths = new ArrayList<>();
        for (String id : nextSessionIds(currentQualified, remaining)) {
            paths.add("fixtures/" + packId + "/native-loader-beta-feedback/" + id + ".md");
        }
        return paths;
    }

    private static List<String> suggestedEvidenceNotePaths(String packId, int currentQualified, int remainingM30Evidence) {
        List<String> paths = new ArrayList<>();
        for (String id : nextEvidenceSessionIds(currentQualified, remainingM30Evidence)) {
            paths.add("fixtures/" + packId + "/native-loader-beta-feedback/" + id + ".md");
        }
        return paths;
    }

    private static List<Map<String, Object>> remainingEvidencePackets(String packId, int currentQualified, int remainingM30Evidence) {
        List<Map<String, Object>> packets = new ArrayList<>();
        for (String id : nextEvidenceSessionIds(currentQualified, remainingM30Evidence)) {
            Map<String, Object> packet = new LinkedHashMap<>();
            packet.put("id", id);
            packet.put("countsAsM30EvidenceWhenNonDraft", true);
            packet.put("realNotePath", "fixtures/" + packId + "/native-loader-beta-feedback/" + id + ".md");
            packet.put("requiredDistinctLogEvidence", true);
            packet.put("requiredLogPathPattern", "fixtures/" + packId + "/isolated-runtime/game/logs/<new-distinct-session>.log or .log.gz");
            packet.put("requiredFields", List.of(
                    "tester",
                    "result=pass",
                    "mainMenuReached=true",
                    "worldCreatedOrLoaded=true",
                    "spawnOrPlayableStateReached=true",
                    "noCrashObserved=true",
                    "latestLogPreserved=true",
                    "fixtureRelativeLogEvidencePath"
            ));
            packet.put("summary", "Run a real additional native-loader beta session and point the note at a distinct preserved log evidence path.");
            packets.add(packet);
        }
        return packets;
    }

    private static List<Map<String, Object>> remainingSessionPackets(String packId, int currentQualified, int remaining) {
        List<Map<String, Object>> packets = new ArrayList<>();
        for (String id : nextSessionIds(currentQualified, remaining)) {
            Map<String, Object> packet = new LinkedHashMap<>();
            packet.put("id", id);
            packet.put("countsAsEvidenceWhenNonDraft", true);
            packet.put("draftPath", "fixtures/" + packId + "/native-loader-beta-feedback/_drafts/" + id + ".draft.md");
            packet.put("realNotePath", "fixtures/" + packId + "/native-loader-beta-feedback/" + id + ".md");
            packet.put("requiredFields", List.of(
                    "tester",
                    "result=pass",
                    "mainMenuReached=true",
                    "worldCreatedOrLoaded=true",
                    "spawnOrPlayableStateReached=true",
                    "noCrashObserved=true",
                    "latestLogPreserved=true"
            ));
            packet.put("rerunAfterWriting", true);
            packet.put("summary", "Run the native loader beta, record observed facts in the real note path, then rerun the M29/M30 evidence loop.");
            packets.add(packet);
        }
        return packets;
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (reportPath == null || !Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-SOAK-STATUS-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta soak status required report missing",
                    "The beta soak status dashboard requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Run M29 beta soak intake, beta session note validation, and beta evidence audit first."
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
                    "ECHO-NATIVE-BETA-SOAK-STATUS-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta soak status upstream report is not accepted",
                    "The beta soak status dashboard requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(reportPath == null ? "reports/echo-native/" + packId + "/" + reportName : relativePath(reportPath)),
                    "Resolve upstream beta soak diagnostics before producing the status dashboard."
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

    private static int number(Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
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
