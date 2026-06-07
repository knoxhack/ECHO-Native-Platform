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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EchoNativeBetaSessionDraftPreparer {
    EchoNativeBetaSessionDraftOutcome prepare(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> noteDraftReport = readRequiredReport(
                requiredReports.get("native-loader-beta-session-note-drafts.json"),
                fixture,
                packId,
                "native-loader-beta-session-note-drafts.json",
                diagnostics
        );
        checkAcceptedReport(
                "native-loader-beta-session-note-drafts.json",
                requiredReports.get("native-loader-beta-session-note-drafts.json"),
                noteDraftReport,
                packId,
                diagnostics
        );

        List<Map<String, Object>> draftFiles = new ArrayList<>();
        List<Map<String, Object>> staleDraftFiles = new ArrayList<>();
        if (!hasBlocking(diagnostics)) {
            Path draftDirectory = fixture.resolve("native-loader-beta-feedback").resolve("_drafts");
            Files.createDirectories(draftDirectory);
            Set<String> currentDraftFileNames = new HashSet<>();
            for (Map<String, Object> draft : drafts(noteDraftReport)) {
                String id = sanitize(String.valueOf(draft.getOrDefault("id", "internal-beta-session")));
                String markdown = String.valueOf(draft.getOrDefault("markdown", ""));
                if (markdown.isBlank()) {
                    diagnostics.add(new EchoNativeDiagnostic(
                            "ECHO-NATIVE-BETA-SESSION-DRAFT-BODY-MISSING",
                            EchoNativeIssueSeverity.ERROR,
                            "Beta session draft body missing",
                            "A note draft in native-loader-beta-session-note-drafts.json did not contain markdown.",
                            null,
                            packId,
                            List.of("reports/echo-native/" + packId + "/native-loader-beta-session-note-drafts.json"),
                            "Regenerate the beta soak operator packet before preparing draft note files."
                    ));
                    continue;
                }
                Path draftFile = draftDirectory.resolve(id + ".draft.md");
                currentDraftFileNames.add(draftFile.getFileName().toString());
                Files.writeString(draftFile, markdown + System.lineSeparator());
                Map<String, Object> file = new LinkedHashMap<>();
                file.put("byteSize", Files.size(draftFile));
                file.put("draftOnly", true);
                file.put("id", id);
                file.put("path", relativePath(draftFile));
                file.put("qualifiesAsEvidence", false);
                draftFiles.add(file);
            }
            staleDraftFiles = staleDraftFiles(draftDirectory, currentDraftFileNames);
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeBetaSessionDraftOutcome(
                packId,
                draftFiles(packId, fixture, draftFiles, staleDraftFiles, sortedDiagnostics),
                draftStatus(packId, draftFiles, staleDraftFiles, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> draftFiles(
            String packId,
            Path fixture,
            List<Map<String, Object>> draftFiles,
            List<Map<String, Object>> staleDraftFiles,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_session_draft_files", diagnostics, !draftFiles.isEmpty());
        data.put("draftDirectory", relativePath(fixture.resolve("native-loader-beta-feedback").resolve("_drafts")));
        data.put("draftFileCount", draftFiles.size());
        data.put("draftFiles", draftFiles);
        data.put("evidenceCreated", false);
        data.put("packId", packId);
        data.put("qualifiesAsEvidence", false);
        data.put("staleDraftFileCount", staleDraftFiles.size());
        data.put("staleDraftFiles", staleDraftFiles);
        data.put("summary", draftFiles.isEmpty()
                ? "No beta session draft files were written."
                : "Ignored draft note files were prepared for remaining internal beta sessions.");
        return data;
    }

    private static Map<String, Object> draftStatus(
            String packId,
            List<Map<String, Object>> draftFiles,
            List<Map<String, Object>> staleDraftFiles,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_session_draft_status", diagnostics, !draftFiles.isEmpty());
        data.put("draftFileCount", draftFiles.size());
        data.put("evidenceCreated", false);
        data.put("fixtureDraftFilesPrepared", !draftFiles.isEmpty());
        data.put("m30EvidenceGateChanged", false);
        data.put("packId", packId);
        data.put("publicBetaOpen", false);
        data.put("staleDraftFileCount", staleDraftFiles.size());
        data.put("staleDraftFiles", staleDraftFiles);
        data.put("summary", draftFiles.isEmpty()
                ? "Session draft preparation did not produce draft files."
                : "Session draft preparation is complete; drafts remain ignored until copied into real tester notes after real sessions.");
        return data;
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
                    "ECHO-NATIVE-BETA-SESSION-DRAFT-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta session draft required report missing",
                    "Preparing draft note files requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Run phase13 export beta-soak-packet before preparing beta session drafts."
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
                    "ECHO-NATIVE-BETA-SESSION-DRAFT-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta session draft upstream report is not accepted",
                    "Preparing beta session drafts requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(reportPath == null ? "reports/echo-native/" + packId + "/" + reportName : relativePath(reportPath)),
                    "Resolve upstream beta soak operator packet diagnostics before preparing draft note files."
            ));
        }
    }

    private static List<Map<String, Object>> drafts(Map<String, Object> report) {
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        Object rawDrafts = data.get("drafts");
        if (!(rawDrafts instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> drafts = new ArrayList<>();
        for (Object item : list) {
            drafts.add(EchoNativeJson.asObject(item));
        }
        drafts.sort(Comparator.comparing(draft -> String.valueOf(draft.getOrDefault("id", ""))));
        return drafts;
    }

    private static List<Map<String, Object>> staleDraftFiles(Path draftDirectory, Set<String> currentDraftFileNames) throws IOException {
        if (!Files.isDirectory(draftDirectory)) {
            return List.of();
        }
        List<Map<String, Object>> staleDraftFiles = new ArrayList<>();
        try (var stream = Files.list(draftDirectory)) {
            for (Path path : stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".draft.md"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList()) {
                String fileName = path.getFileName().toString();
                if (currentDraftFileNames.contains(fileName)) {
                    continue;
                }
                Map<String, Object> file = new LinkedHashMap<>();
                file.put("byteSize", Files.size(path));
                file.put("draftOnly", true);
                file.put("path", relativePath(path));
                file.put("qualifiesAsEvidence", false);
                file.put("reason", "stale_ignored_draft_not_in_current_remaining_session_plan");
                staleDraftFiles.add(file);
            }
        }
        return staleDraftFiles;
    }

    private static Map<String, Object> base(
            String phase,
            List<EchoNativeDiagnostic> diagnostics,
            boolean filesystemMutated
    ) {
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
        data.put("filesystemMutated", filesystemMutated);
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
        data.put("savesMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("userCachesMutated", false);
        return data;
    }

    private static boolean hasBlocking(List<EchoNativeDiagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL);
    }

    private static String sanitize(String value) {
        String sanitized = value.toLowerCase().replaceAll("[^a-z0-9._-]+", "-");
        return sanitized.isBlank() ? "internal-beta-session" : sanitized;
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
