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

final class EchoNativeBetaSoakIntake {
    private static final int TARGET_INTERNAL_SESSION_COUNT = 3;

    EchoNativeBetaSoakOutcome intake(
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

        Map<String, Object> feedbackInventory = data(reports.get("native-loader-beta-feedback-inventory.json"));
        Map<String, Object> crashIntake = data(reports.get("native-loader-beta-crash-intake.json"));
        Map<String, Object> m28Completion = data(reports.get("phase13-m28-completion.json"));
        List<Map<String, Object>> sessionEvidence = sessionEvidence(packId, feedbackInventory, diagnostics);
        List<Map<String, Object>> issues = issueRecords(reports);

        int sessionCount = sessionEvidence.size();
        int crashReportCount = number(crashIntake, "crashReportCount");
        boolean noCrashEvidence = bool(crashIntake, "noCrashEvidence");
        boolean m29Ready = bool(data(reports.get("phase13-m29-readiness.json")), "phase13M29Ready");
        boolean m28Complete = bool(m28Completion, "phase13M28Complete");
        boolean soakStarted = m28Complete && m29Ready && sessionCount > 0 && noCrashEvidence;
        boolean repeatedSoakComplete = soakStarted && sessionCount >= TARGET_INTERNAL_SESSION_COUNT && issues.stream().noneMatch(EchoNativeBetaSoakIntake::isBlockingIssue);

        if (sessionCount < TARGET_INTERNAL_SESSION_COUNT) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M29-SOAK-SESSION-COUNT-LOW",
                    EchoNativeIssueSeverity.WARNING,
                    "Internal beta soak needs more repeated sessions",
                    "The beta soak has " + sessionCount + " session evidence item(s); target is " + TARGET_INTERNAL_SESSION_COUNT + " before the next widening milestone.",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/native-loader-beta-feedback", "reports/echo-native/" + packId + "/native-loader-beta-session-inventory.json"),
                    "Run more internal beta sessions, preserve logs/screenshots, and rerun beta-feedback plus beta-soak intake."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeBetaSoakOutcome(
                packId,
                soakPlan(packId, soakStarted, repeatedSoakComplete, sessionCount, issues, sortedDiagnostics),
                sessionInventory(packId, sessionEvidence, sessionCount, noCrashEvidence, crashReportCount, sortedDiagnostics),
                issueTriage(packId, issues, noCrashEvidence, crashReportCount, sortedDiagnostics),
                regressionWatchlist(packId, sessionCount, issues, sortedDiagnostics),
                m29Completion(packId, soakStarted, repeatedSoakComplete, sessionCount, sortedDiagnostics),
                m30Readiness(packId, repeatedSoakComplete, sessionCount, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> soakPlan(
            String packId,
            boolean soakStarted,
            boolean repeatedSoakComplete,
            int sessionCount,
            List<Map<String, Object>> issues,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_soak_plan", diagnostics);
        data.put("issueCount", issues.size());
        data.put("packId", packId);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("repeatedSoakComplete", repeatedSoakComplete);
        data.put("sessionCount", sessionCount);
        data.put("soakStarted", soakStarted);
        data.put("targetInternalSessionCount", TARGET_INTERNAL_SESSION_COUNT);
        data.put("summary", soakStarted
                ? "Internal beta soak has started and is being tracked."
                : "Internal beta soak is blocked until M28 and session evidence are ready.");
        return data;
    }

    private static Map<String, Object> sessionInventory(
            String packId,
            List<Map<String, Object>> sessionEvidence,
            int sessionCount,
            boolean noCrashEvidence,
            int crashReportCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_session_inventory", diagnostics);
        data.put("crashReportCount", crashReportCount);
        data.put("noCrashEvidence", noCrashEvidence);
        data.put("packId", packId);
        data.put("sessionCount", sessionCount);
        data.put("sessions", sessionEvidence);
        data.put("targetInternalSessionCount", TARGET_INTERNAL_SESSION_COUNT);
        data.put("summary", sessionCount > 0
                ? "Internal beta session evidence was inventoried."
                : "No internal beta session evidence is available for soak.");
        return data;
    }

    private static Map<String, Object> issueTriage(
            String packId,
            List<Map<String, Object>> issues,
            boolean noCrashEvidence,
            int crashReportCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_issue_triage", diagnostics);
        data.put("crashReportCount", crashReportCount);
        data.put("issues", issues);
        data.put("issueCount", issues.size());
        data.put("noCrashEvidence", noCrashEvidence);
        data.put("packId", packId);
        data.put("summary", issues.isEmpty()
                ? "No blocking native loader beta issues were found in current soak evidence."
                : "Native loader beta issues were triaged for internal soak.");
        return data;
    }

    private static Map<String, Object> regressionWatchlist(
            String packId,
            int sessionCount,
            List<Map<String, Object>> issues,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_regression_watchlist", diagnostics);
        List<Map<String, Object>> watchlist = new ArrayList<>();
        watchlist.add(watch("launch.startup", "Native loader starts the isolated Minecraft client without new crash reports."));
        watchlist.add(watch("world.create_load", "Tester can create or load an Ashfall world and reach spawn."));
        watchlist.add(watch("evidence.capture", "Each tester preserves logs, screenshots, and fixture-local beta notes."));
        watchlist.add(watch("public_scope", "Public beta and public release remain closed during internal soak."));
        data.put("issueCount", issues.size());
        data.put("packId", packId);
        data.put("sessionCount", sessionCount);
        data.put("watchlist", watchlist);
        data.put("watchlistCount", watchlist.size());
        data.put("summary", "Regression watchlist is ready for repeated internal native-loader beta soak.");
        return data;
    }

    private static Map<String, Object> m29Completion(
            String packId,
            boolean soakStarted,
            boolean repeatedSoakComplete,
            int sessionCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_internal_beta_soak_completion", diagnostics);
        data.put("internalBetaSoakStarted", soakStarted);
        data.put("packId", packId);
        data.put("phase13M29Complete", soakStarted);
        data.put("phase13M30Ready", repeatedSoakComplete);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("repeatedSoakComplete", repeatedSoakComplete);
        data.put("sessionCount", sessionCount);
        data.put("targetInternalSessionCount", TARGET_INTERNAL_SESSION_COUNT);
        data.put("summary", soakStarted
                ? "M29 is active: internal beta soak and issue triage are running."
                : "M29 remains blocked until M28 and session evidence pass.");
        return data;
    }

    private static Map<String, Object> m30Readiness(
            String packId,
            boolean repeatedSoakComplete,
            int sessionCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m30_public_beta_candidate_readiness", diagnostics);
        data.put("packId", packId);
        data.put("phase13M30Ready", repeatedSoakComplete);
        data.put("publicBetaCandidateReady", repeatedSoakComplete);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("sessionCount", sessionCount);
        data.put("targetInternalSessionCount", TARGET_INTERNAL_SESSION_COUNT);
        data.put("summary", repeatedSoakComplete
                ? "M30 may begin as a public beta candidate audit; public beta remains closed until that gate passes."
                : "M30 remains closed until repeated internal beta soak reaches the target session count.");
        return data;
    }

    private static List<Map<String, Object>> sessionEvidence(
            String packId,
            Map<String, Object> feedbackInventory,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Object raw = feedbackInventory.get("evidenceFiles");
        if (!(raw instanceof List<?> files)) {
            return List.of();
        }
        List<Map<String, Object>> sessions = new ArrayList<>();
        int index = 1;
        for (Object item : files) {
            Map<String, Object> file = EchoNativeJson.asObject(item);
            String kind = String.valueOf(file.getOrDefault("kind", ""));
            if (!"native_loader_beta_feedback".equals(kind) && !"native_loader_beta_notes".equals(kind)) {
                continue;
            }
            String path = String.valueOf(file.getOrDefault("path", ""));
            Path notePath = Path.of("").toAbsolutePath().normalize().resolve(path).normalize();
            String text = Files.isRegularFile(notePath) ? Files.readString(notePath) : "";
            if (!isQualifiedCleanSession(text)) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-M29-BETA-SOAK-NOTE-INCOMPLETE",
                        EchoNativeIssueSeverity.WARNING,
                        "Internal beta soak note is not fully qualified",
                        "The beta soak note " + path + " is missing tester, pass result, world create/load, or no-crash evidence.",
                        null,
                        packId,
                        List.of(path),
                        "Use a real completed session note, not a draft/template, and record tester, result, world create/load, and no-crash status."
                ));
                continue;
            }
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("id", "beta-session-" + index++);
            session.put("evidencePath", path);
            session.put("evidenceKind", kind);
            session.put("byteSize", file.getOrDefault("byteSize", 0));
            session.put("worldCreateOrLoadReported", true);
            session.put("tester", testerName(text));
            sessions.add(session);
        }
        return sessions;
    }

    private static boolean isQualifiedCleanSession(String text) {
        String lower = text.toLowerCase();
        return hasTester(lower)
                && hasPassResult(lower)
                && hasWorldCreateOrLoad(lower)
                && hasNoCrash(lower);
    }

    private static boolean hasTester(String lower) {
        return lower.contains("tester:") && !lower.contains("tester: \n") && !lower.contains("tester: <");
    }

    private static boolean hasPassResult(String lower) {
        return lower.contains("result: pass") || lower.contains("result=pass");
    }

    private static boolean hasWorldCreateOrLoad(String lower) {
        if (lower.contains("not_reached")) {
            return false;
        }
        return lower.contains("world was created")
                || lower.contains("new world was created")
                || lower.contains("world loaded")
                || lower.contains("create and load")
                || lower.contains("created and loaded")
                || lower.contains("worldcreateorload: created")
                || lower.contains("worldcreateorload: loaded")
                || lower.contains("worldcreateorload: both")
                || lower.contains("worldcreateorload=created")
                || lower.contains("worldcreateorload=loaded")
                || lower.contains("worldcreateorload=both");
    }

    private static boolean hasNoCrash(String lower) {
        return lower.contains("no crash") || lower.contains("crashreported=false");
    }

    private static String testerName(String text) {
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase().startsWith("tester:")) {
                String value = trimmed.substring("tester:".length()).trim();
                return value.isBlank() ? "unknown" : value;
            }
        }
        return "unknown";
    }

    private static List<Map<String, Object>> issueRecords(Map<String, Map<String, Object>> reports) {
        List<Map<String, Object>> issues = new ArrayList<>();
        collectIssueData(issues, reports.get("native-loader-beta-known-issues.json"));
        Map<String, Object> crashIntake = data(reports.get("native-loader-beta-crash-intake.json"));
        if (number(crashIntake, "crashReportCount") > 0) {
            Map<String, Object> issue = new LinkedHashMap<>();
            issue.put("id", "BETA-CRASH-001");
            issue.put("category", "crash");
            issue.put("severity", "blocking_for_widening");
            issue.put("summary", "Crash reports are present in beta crash intake.");
            issues.add(issue);
        }
        issues.sort(Comparator.comparing(issue -> String.valueOf(issue.get("id"))));
        return issues;
    }

    private static void collectIssueData(List<Map<String, Object>> issues, Map<String, Object> report) {
        Object rawData = report == null ? null : report.get("data");
        Map<String, Object> data = EchoNativeJson.asObject(rawData);
        Object rawIssues = data.get("issues");
        if (!(rawIssues instanceof List<?> list)) {
            return;
        }
        for (Object raw : list) {
            Map<String, Object> issue = EchoNativeJson.asObject(raw);
            String severity = String.valueOf(issue.getOrDefault("severity", ""));
            if ("nonblocking_for_internal_beta".equals(severity)) {
                continue;
            }
            issues.add(new LinkedHashMap<>(issue));
        }
    }

    private static boolean isBlockingIssue(Map<String, Object> issue) {
        String severity = String.valueOf(issue.getOrDefault("severity", ""));
        return severity.contains("blocking");
    }

    private static Map<String, Object> watch(String id, String summary) {
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
                    "ECHO-NATIVE-M29-BETA-SOAK-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M29 required report missing",
                    "The beta soak intake requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Run phase13 verify m28 and phase13 intake beta-feedback before M29 intake."
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
                    "ECHO-NATIVE-M29-BETA-SOAK-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M29 upstream report is not accepted",
                    "The beta soak intake requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream beta widening and feedback diagnostics before M29 intake."
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
