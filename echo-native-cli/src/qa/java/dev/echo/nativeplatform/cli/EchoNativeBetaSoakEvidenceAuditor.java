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

final class EchoNativeBetaSoakEvidenceAuditor {
    EchoNativeBetaSoakEvidenceAuditOutcome audit(
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
        Map<String, Object> sessionInventory = data(reports.get("native-loader-beta-session-inventory.json"));
        Map<String, Object> playableEvidence = data(reports.get("tester-playable-evidence.json"));
        int targetSessionCount = firstPositive(
                number(sessionInventory, "targetInternalSessionCount"),
                number(data(reports.get("phase13-m29-completion.json")), "targetInternalSessionCount"),
                3
        );
        List<Map<String, Object>> noteProofs = noteProofs(packId, feedbackInventory, diagnostics);
        int qualifiedSessionCount = (int) noteProofs.stream()
                .filter(proof -> Boolean.TRUE.equals(proof.get("qualifiedCleanSession")))
                .count();
        int remainingSessionCount = Math.max(targetSessionCount - qualifiedSessionCount, 0);
        int screenshotCount = number(feedbackInventory, "screenshotCount");
        int evidenceFileCount = number(feedbackInventory, "evidenceFileCount");
        boolean latestLogPresent = bool(playableEvidence, "latestLogPresent")
                || evidenceKinds(feedbackInventory).contains("latest_log");
        boolean noCrashEvidence = bool(sessionInventory, "noCrashEvidence")
                || number(playableEvidence, "crashReportCount") == 0;
        boolean playableBaselineEvidence = bool(feedbackInventory, "playableBaselineEvidence")
                || bool(playableEvidence, "baselinePlayableEvidence");

        if (qualifiedSessionCount < targetSessionCount) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-SOAK-EVIDENCE-QUALIFIED-SESSION-COUNT-LOW",
                    EchoNativeIssueSeverity.WARNING,
                    "Qualified beta soak session count is below target",
                    "The beta soak evidence audit found " + qualifiedSessionCount + " qualified clean session note(s); target is " + targetSessionCount + ".",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/native-loader-beta-feedback"),
                    "Add one structured note per clean internal beta session, then rerun beta-feedback, beta-soak, this audit, and M30 verification."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeBetaSoakEvidenceAuditOutcome(
                packId,
                evidenceQuality(packId, qualifiedSessionCount, targetSessionCount, remainingSessionCount, evidenceFileCount,
                        screenshotCount, latestLogPresent, noCrashEvidence, playableBaselineEvidence, sortedDiagnostics),
                proofMatrix(packId, noteProofs, qualifiedSessionCount, targetSessionCount, sortedDiagnostics),
                evidenceGap(packId, qualifiedSessionCount, targetSessionCount, remainingSessionCount, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static List<Map<String, Object>> noteProofs(
            String packId,
            Map<String, Object> feedbackInventory,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        List<Map<String, Object>> proofs = new ArrayList<>();
        Object raw = feedbackInventory.get("evidenceFiles");
        if (!(raw instanceof List<?> files)) {
            return List.of();
        }
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
            String lower = text.toLowerCase();
            boolean testerPresent = hasTester(lower);
            boolean passResult = hasPassResult(lower);
            boolean worldCreateOrLoad = hasWorldCreateOrLoad(lower);
            boolean noCrashReported = hasNoCrash(lower);
            boolean qualified = testerPresent && passResult && worldCreateOrLoad && noCrashReported;
            Map<String, Object> proof = new LinkedHashMap<>();
            proof.put("byteSize", file.getOrDefault("byteSize", 0));
            proof.put("evidenceKind", kind);
            proof.put("evidencePath", path);
            proof.put("id", "qualified-beta-session-" + index++);
            proof.put("noCrashReported", noCrashReported);
            proof.put("passResult", passResult);
            proof.put("qualifiedCleanSession", qualified);
            proof.put("testerPresent", testerPresent);
            proof.put("worldCreateOrLoadReported", worldCreateOrLoad);
            proofs.add(proof);
            if (!qualified) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-BETA-SOAK-EVIDENCE-NOTE-INCOMPLETE",
                        EchoNativeIssueSeverity.WARNING,
                        "Beta soak note is not fully qualified",
                        "The beta soak note " + path + " is missing one or more qualification fields.",
                        null,
                        packId,
                        List.of(path),
                        "Record tester, pass result, world create/load observation, and no-crash status in the session note."
                ));
            }
        }
        proofs.sort(Comparator.comparing(proof -> String.valueOf(proof.get("evidencePath"))));
        return List.copyOf(proofs);
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

    private static Map<String, Object> evidenceQuality(
            String packId,
            int qualifiedSessionCount,
            int targetSessionCount,
            int remainingSessionCount,
            int evidenceFileCount,
            int screenshotCount,
            boolean latestLogPresent,
            boolean noCrashEvidence,
            boolean playableBaselineEvidence,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_evidence_quality", diagnostics);
        data.put("evidenceFileCount", evidenceFileCount);
        data.put("latestLogPresent", latestLogPresent);
        data.put("noCrashEvidence", noCrashEvidence);
        data.put("packId", packId);
        data.put("playableBaselineEvidence", playableBaselineEvidence);
        data.put("qualifiedSessionCount", qualifiedSessionCount);
        data.put("remainingSessionCount", remainingSessionCount);
        data.put("screenshotCount", screenshotCount);
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("summary", remainingSessionCount == 0
                ? "Beta soak evidence quality meets the internal session target."
                : "Beta soak evidence quality is good for current evidence but still short of the internal session target.");
        return data;
    }

    private static Map<String, Object> proofMatrix(
            String packId,
            List<Map<String, Object>> noteProofs,
            int qualifiedSessionCount,
            int targetSessionCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_session_proof_matrix", diagnostics);
        data.put("packId", packId);
        data.put("qualifiedSessionCount", qualifiedSessionCount);
        data.put("sessionProofs", noteProofs);
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("summary", "Beta soak session proof matrix counts one structured note per clean internal session.");
        return data;
    }

    private static Map<String, Object> evidenceGap(
            String packId,
            int qualifiedSessionCount,
            int targetSessionCount,
            int remainingSessionCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_evidence_gap", diagnostics);
        data.put("m30StillBlocked", remainingSessionCount > 0);
        data.put("packId", packId);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("qualifiedSessionCount", qualifiedSessionCount);
        data.put("remainingSessionCount", remainingSessionCount);
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("summary", remainingSessionCount == 0
                ? "No beta soak evidence gap remains; rerun M30 candidate verification."
                : "Beta soak evidence gap remains: add " + remainingSessionCount + " more qualified clean session note(s).");
        return data;
    }

    private static List<String> evidenceKinds(Map<String, Object> feedbackInventory) {
        Object raw = feedbackInventory.get("evidenceFiles");
        if (!(raw instanceof List<?> files)) {
            return List.of();
        }
        List<String> kinds = new ArrayList<>();
        for (Object item : files) {
            String kind = String.valueOf(EchoNativeJson.asObject(item).getOrDefault("kind", ""));
            if (!kind.isBlank()) {
                kinds.add(kind);
            }
        }
        return List.copyOf(kinds);
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
                    "ECHO-NATIVE-BETA-SOAK-EVIDENCE-AUDIT-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta soak evidence audit required report missing",
                    "The beta soak evidence audit requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Run beta-feedback and beta-soak intake before auditing evidence quality."
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
                    "ECHO-NATIVE-BETA-SOAK-EVIDENCE-AUDIT-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Beta soak evidence audit upstream report is not accepted",
                    "The beta soak evidence audit requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream beta evidence diagnostics before auditing evidence quality."
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
