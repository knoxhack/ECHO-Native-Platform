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

final class EchoNativePublicBetaCandidateVerifier {
    EchoNativePublicBetaCandidateOutcome verify(
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

        int targetSessionCount = targetSessionCount(reports);
        List<Map<String, Object>> gates = new ArrayList<>();
        gates.add(booleanGate(packId, reports, "phase13-m29-completion.json", "phase13M29Complete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m29-completion.json", "phase13M30Ready", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m29-completion.json", "repeatedSoakComplete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m30-readiness.json", "phase13M30Ready", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m30-readiness.json", "publicBetaCandidateReady", true, diagnostics));
        gates.add(minimumGate(packId, reports, "native-loader-beta-session-inventory.json", "sessionCount", targetSessionCount, diagnostics));
        gates.add(minimumGate(packId, reports, "phase13-m29-note-validation-status.json", "completeForM30SoakStandardCount", targetSessionCount, diagnostics));
        gates.add(minimumGate(packId, reports, "phase13-m29-note-validation-status.json", "distinctM30LogEvidenceCount", targetSessionCount, diagnostics));
        gates.add(booleanGate(packId, reports, "native-loader-beta-session-inventory.json", "noCrashEvidence", true, diagnostics));
        gates.add(numberGate(packId, reports, "native-loader-beta-issue-triage.json", "issueCount", 0, diagnostics));
        gates.add(numberGate(packId, reports, "native-loader-beta-issue-triage.json", "crashReportCount", 0, diagnostics));
        gates.add(booleanGate(packId, reports, "native-loader-beta-widening-safety-gate.json", "safeToWidenInternalBeta", true, diagnostics));

        int sessionCount = number(data(reports.get("native-loader-beta-session-inventory.json")), "sessionCount");
        int completeForM30SoakStandardCount = number(data(reports.get("phase13-m29-note-validation-status.json")), "completeForM30SoakStandardCount");
        int distinctM30LogEvidenceCount = number(data(reports.get("phase13-m29-note-validation-status.json")), "distinctM30LogEvidenceCount");
        int issueCount = number(data(reports.get("native-loader-beta-issue-triage.json")), "issueCount");
        boolean gatesPass = gates.stream().allMatch(gate -> Boolean.TRUE.equals(gate.get("pass")));
        boolean candidateReady = diagnostics.stream().noneMatch(EchoNativePublicBetaCandidateVerifier::isBlocking) && gatesPass;
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativePublicBetaCandidateOutcome(
                packId,
                candidateAudit(packId, candidateReady, sessionCount, completeForM30SoakStandardCount, distinctM30LogEvidenceCount, targetSessionCount, issueCount, gates, sortedDiagnostics),
                safetyGate(packId, candidateReady, gates, sortedDiagnostics),
                testerReadiness(packId, candidateReady, sessionCount, completeForM30SoakStandardCount, distinctM30LogEvidenceCount, targetSessionCount, sortedDiagnostics),
                m30Completion(packId, candidateReady, sessionCount, completeForM30SoakStandardCount, distinctM30LogEvidenceCount, targetSessionCount, sortedDiagnostics),
                m31Readiness(packId, candidateReady, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> candidateAudit(
            String packId,
            boolean candidateReady,
            int sessionCount,
            int completeForM30SoakStandardCount,
            int distinctM30LogEvidenceCount,
            int targetSessionCount,
            int issueCount,
            List<Map<String, Object>> gates,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m30_public_beta_candidate_audit", diagnostics);
        data.put("completeForM30SoakStandardCount", completeForM30SoakStandardCount);
        data.put("distinctM30LogEvidenceCount", distinctM30LogEvidenceCount);
        data.put("gateCount", gates.size());
        data.put("gates", gates);
        data.put("issueCount", issueCount);
        data.put("packId", packId);
        data.put("publicBetaCandidateReady", candidateReady);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("sessionCount", sessionCount);
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("summary", candidateReady
                ? "M30 public beta candidate audit passed; a later gate must still explicitly open public beta."
                : "M30 remains blocked until M29 repeated soak and no-crash/no-blocking-issue gates pass.");
        return data;
    }

    private static Map<String, Object> safetyGate(
            String packId,
            boolean candidateReady,
            List<Map<String, Object>> gates,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m30_public_beta_safety_gate", diagnostics);
        data.put("gateCount", gates.size());
        data.put("internalBetaEvidenceRequired", true);
        data.put("packId", packId);
        data.put("publicBetaOpen", false);
        data.put("publicReleaseReady", false);
        data.put("safeForPublicBetaCandidateAudit", candidateReady);
        data.put("summary", candidateReady
                ? "Public beta candidate safety audit may proceed to the next controlled packaging gate."
                : "Public beta candidate safety gate is closed.");
        return data;
    }

    private static Map<String, Object> testerReadiness(
            String packId,
            boolean candidateReady,
            int sessionCount,
            int completeForM30SoakStandardCount,
            int distinctM30LogEvidenceCount,
            int targetSessionCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m30_public_beta_tester_readiness", diagnostics);
        List<Map<String, Object>> checklist = List.of(
                checklist("repeat_internal_soak", "Collect at least " + targetSessionCount + " clean internal native-loader beta sessions."),
                checklist("complete_m30_note_standard", "Confirm all " + targetSessionCount + " clean sessions include main menu, world create/load, spawn/playable state, no-crash, and latest.log evidence."),
                checklist("preserve_evidence", "Preserve fixture-local beta notes, logs, screenshots, and crash/no-crash evidence."),
                checklist("triage_issues", "Confirm no blocking native-loader beta issues remain open."),
                checklist("keep_public_beta_closed", "Do not open public beta until a later explicit public beta gate passes.")
        );
        data.put("checklist", checklist);
        data.put("completeForM30SoakStandardCount", completeForM30SoakStandardCount);
        data.put("distinctM30LogEvidenceCount", distinctM30LogEvidenceCount);
        data.put("packId", packId);
        data.put("publicBetaTesterReadiness", candidateReady);
        data.put("sessionCount", sessionCount);
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("summary", candidateReady
                ? "Tester readiness evidence is sufficient for the next public beta candidate gate."
                : "Tester readiness remains internal-only until repeated soak evidence is complete.");
        return data;
    }

    private static Map<String, Object> m30Completion(
            String packId,
            boolean candidateReady,
            int sessionCount,
            int completeForM30SoakStandardCount,
            int distinctM30LogEvidenceCount,
            int targetSessionCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m30_public_beta_candidate_completion", diagnostics);
        data.put("completeForM30SoakStandardCount", completeForM30SoakStandardCount);
        data.put("distinctM30LogEvidenceCount", distinctM30LogEvidenceCount);
        data.put("packId", packId);
        data.put("phase13M30Complete", candidateReady);
        data.put("phase13M31Ready", candidateReady);
        data.put("publicBetaCandidateReady", candidateReady);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("sessionCount", sessionCount);
        data.put("targetInternalSessionCount", targetSessionCount);
        data.put("summary", candidateReady
                ? "M30 is complete; M31 may audit public beta opening criteria while public beta remains closed."
                : "M30 remains blocked by internal beta soak evidence.");
        return data;
    }

    private static Map<String, Object> m31Readiness(
            String packId,
            boolean candidateReady,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m31_public_beta_open_readiness", diagnostics);
        data.put("packId", packId);
        data.put("phase13M31Ready", candidateReady);
        data.put("publicBetaOpenReady", false);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("summary", candidateReady
                ? "M31 may begin as a separate public beta opening audit; this report does not open public beta."
                : "M31 remains closed until M30 public beta candidate audit passes.");
        return data;
    }

    private static Map<String, Object> checklist(String id, String summary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("summary", summary);
        return item;
    }

    private static Map<String, Object> booleanGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            boolean expected,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Object actual = data(reports.get(reportName)).get(field);
        boolean pass = actual instanceof Boolean bool && bool == expected;
        Map<String, Object> gate = gate(reportName, field, expected, actual, pass);
        if (!pass) {
            diagnostics.add(gateDiagnostic(packId, reportName, field, String.valueOf(expected)));
        }
        return gate;
    }

    private static Map<String, Object> numberGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            int expected,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Object actual = data(reports.get(reportName)).get(field);
        boolean pass = actual instanceof Number number && number.intValue() == expected;
        Map<String, Object> gate = gate(reportName, field, expected, actual, pass);
        if (!pass) {
            diagnostics.add(gateDiagnostic(packId, reportName, field, String.valueOf(expected)));
        }
        return gate;
    }

    private static Map<String, Object> minimumGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            int minimum,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Object actual = data(reports.get(reportName)).get(field);
        boolean pass = actual instanceof Number number && number.intValue() >= minimum;
        Map<String, Object> gate = gate(reportName, field, ">= " + minimum, actual, pass);
        if (!pass) {
            diagnostics.add(gateDiagnostic(packId, reportName, field, ">= " + minimum));
        }
        return gate;
    }

    private static Map<String, Object> gate(String reportName, String field, Object expected, Object actual, boolean pass) {
        Map<String, Object> gate = new LinkedHashMap<>();
        gate.put("actual", actual == null ? "missing" : actual);
        gate.put("expected", expected);
        gate.put("field", field);
        gate.put("pass", pass);
        gate.put("report", reportName);
        return gate;
    }

    private static EchoNativeDiagnostic gateDiagnostic(String packId, String reportName, String field, String expected) {
        return new EchoNativeDiagnostic(
                "ECHO-NATIVE-M30-PUBLIC-BETA-CANDIDATE-GATE-BLOCKED",
                EchoNativeIssueSeverity.ERROR,
                "Phase 13 M30 public beta candidate gate is blocked",
                reportName + " must report " + field + expectedOperator(expected) + expected + " before M30 can complete.",
                null,
                packId,
                List.of("reports/echo-native/" + packId + "/" + reportName),
                "Collect or complete internal beta soak session notes with main menu, world, spawn/playable, no-crash, and distinct fixture-relative latest.log or .log.gz evidence; rerun note validation, M29 intake, and M30 verification."
        );
    }

    private static String expectedOperator(String expected) {
        return expected.startsWith(">") || expected.startsWith("<") ? " " : "=";
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
                    "ECHO-NATIVE-M30-PUBLIC-BETA-CANDIDATE-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M30 required report missing",
                    "The public beta candidate verifier requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Run phase13 intake beta-soak before M30 verification."
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
                    "ECHO-NATIVE-M30-PUBLIC-BETA-CANDIDATE-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M30 upstream report is not accepted",
                    "The public beta candidate verifier requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream beta soak diagnostics before M30 verification."
            ));
        }
    }

    private static int targetSessionCount(Map<String, Map<String, Object>> reports) {
        int target = number(data(reports.get("phase13-m30-readiness.json")), "targetInternalSessionCount");
        return target <= 0 ? 3 : target;
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static int number(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
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
