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

final class EchoNativeBetaWideningVerifier {
    EchoNativeBetaWideningOutcome verify(
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

        List<Map<String, Object>> gates = new ArrayList<>();
        gates.add(booleanGate(packId, reports, "phase13-m27-completion.json", "phase13M27Complete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m27-completion.json", "phase13M28Ready", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m28-readiness.json", "phase13M28Ready", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m28-readiness.json", "structuredFeedbackPresent", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m28-readiness.json", "noCrashEvidence", true, diagnostics));
        gates.add(booleanGate(packId, reports, "native-loader-beta-feedback-inventory.json", "structuredFeedbackPresent", true, diagnostics));
        gates.add(minimumGate(packId, reports, "native-loader-beta-feedback-inventory.json", "feedbackArtifactCount", 1, diagnostics));
        gates.add(booleanGate(packId, reports, "native-loader-beta-crash-intake.json", "noCrashEvidence", true, diagnostics));
        gates.add(numberGate(packId, reports, "native-loader-beta-crash-intake.json", "crashReportCount", 0, diagnostics));
        gates.add(booleanGate(packId, reports, "internal-tester-beta-gate.json", "internalTesterBetaOpen", true, diagnostics));

        boolean gatesPass = gates.stream().allMatch(gate -> Boolean.TRUE.equals(gate.get("pass")));
        boolean safe = diagnostics.stream().noneMatch(EchoNativeBetaWideningVerifier::isBlocking) && gatesPass;
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeBetaWideningOutcome(
                packId,
                wideningPlan(packId, safe, gates, sortedDiagnostics),
                safetyGate(packId, safe, gates, sortedDiagnostics),
                cohortPlan(packId, safe, sortedDiagnostics),
                m28Completion(packId, safe, gates, sortedDiagnostics),
                m29Readiness(packId, safe, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> wideningPlan(
            String packId,
            boolean safe,
            List<Map<String, Object>> gates,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m28_beta_widening_plan", diagnostics);
        data.put("gateCount", gates.size());
        data.put("gates", gates);
        data.put("internalOnly", true);
        data.put("packId", packId);
        data.put("plannedAudience", safe ? "small_internal_native_loader_tester_cohort" : "single_operator_iteration");
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("wideningPlanReady", safe);
        data.put("summary", safe
                ? "M28 can widen from one operator to a small internal native-loader tester cohort."
                : "M28 remains blocked until feedback and safety gates pass.");
        return data;
    }

    private static Map<String, Object> safetyGate(
            String packId,
            boolean safe,
            List<Map<String, Object>> gates,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m28_beta_widening_safety_gate", diagnostics);
        data.put("gateCount", gates.size());
        data.put("internalBetaOnly", true);
        data.put("packId", packId);
        data.put("publicBetaOpened", false);
        data.put("safeToWidenInternalBeta", safe);
        data.put("summary", safe
                ? "Internal beta widening safety gate passed; public beta remains closed."
                : "Internal beta widening safety gate is blocked.");
        return data;
    }

    private static Map<String, Object> cohortPlan(
            String packId,
            boolean safe,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m28_beta_tester_cohort_plan", diagnostics);
        List<Map<String, Object>> checks = List.of(
                checklist("launch_native_loader", "Launch through the native loader tester command."),
                checklist("create_or_load_world", "Create or load an Ashfall world and wait for spawn."),
                checklist("capture_screenshot", "Capture at least one screenshot from the loaded world."),
                checklist("shutdown_cleanly", "Close the client and preserve fixture-local logs."),
                checklist("rerun_intake", "Run phase13 intake tester-evidence and phase13 intake beta-feedback."),
                checklist("report_notes", "Add tester notes under fixtures/ashfall/tester-notes or playtest-feedback.")
        );
        data.put("checklist", checks);
        data.put("cohortMaxTesterCount", safe ? 3 : 1);
        data.put("cohortPlanReady", safe);
        data.put("packId", packId);
        data.put("publicBetaReady", false);
        data.put("summary", safe
                ? "Small internal tester cohort plan is ready."
                : "Tester cohort plan remains single-operator until M28 passes.");
        return data;
    }

    private static Map<String, Object> m28Completion(
            String packId,
            boolean safe,
            List<Map<String, Object>> gates,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m28_internal_beta_widening_completion", diagnostics);
        data.put("gateCount", gates.size());
        data.put("internalBetaWideningReady", safe);
        data.put("packId", packId);
        data.put("phase13M28Complete", safe);
        data.put("phase13M29Ready", safe);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("summary", safe
                ? "M28 is complete: Ashfall native loader beta may widen to a small internal tester cohort."
                : "M28 remains blocked by beta widening gates.");
        return data;
    }

    private static Map<String, Object> m29Readiness(
            String packId,
            boolean safe,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_internal_beta_soak_readiness", diagnostics);
        data.put("internalBetaSoakReady", safe);
        data.put("packId", packId);
        data.put("phase13M29Ready", safe);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("recommendedNextCommand", "phase13 intake beta-feedback <fixture>");
        data.put("summary", safe
                ? "M29 may begin as repeated internal beta soak and issue triage."
                : "M29 remains closed until M28 completes.");
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
                "ECHO-NATIVE-M28-BETA-WIDENING-GATE-BLOCKED",
                EchoNativeIssueSeverity.ERROR,
                "Phase 13 M28 beta widening gate is blocked",
                reportName + " must report " + field + "=" + expected + " before internal beta widening can complete.",
                null,
                packId,
                List.of("reports/echo-native/" + packId + "/" + reportName),
                "Regenerate M27 intake reports and resolve beta feedback/crash evidence before M28."
        );
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
                    "ECHO-NATIVE-M28-BETA-WIDENING-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M28 required report missing",
                    "The beta widening verifier requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Run phase13 intake beta-feedback before M28 verification."
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
                    "ECHO-NATIVE-M28-BETA-WIDENING-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M28 upstream report is not accepted",
                    "The beta widening verifier requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream beta feedback diagnostics before M28 verification."
            ));
        }
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR;
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
