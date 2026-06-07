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

final class EchoNativeRuntimeFixtureApprovalAuditor {
    EchoNativeRuntimeFixtureApprovalAuditOutcome audit(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> intakePlan = readRequiredReport(requiredReports.get("runtime-fixture-intake-plan.json"), packId, "runtime-fixture-intake-plan.json", diagnostics);
        List<Map<String, Object>> actions = actions(intakePlan);
        if (actions.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-APPROVAL-ACTIONS-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture approval actions are missing",
                    "Runtime fixture approval audit requires runtime fixture intake actions.",
                    null,
                    packId,
                    List.of(relativeReportPath(requiredReports.get("runtime-fixture-intake-plan.json"))),
                    "Run phase13 verify runtime-fixtures and phase13 plan runtime-fixture-intake first."
            ));
        }

        Path approvalsPath = fixture.resolve("runtime-fixture-approvals.json");
        Map<String, Map<String, Object>> approvals = approvals(approvalsPath, packId, diagnostics);
        List<Map<String, Object>> auditItems = new ArrayList<>();
        List<Map<String, Object>> templateItems = new ArrayList<>();
        for (Map<String, Object> action : actions) {
            String artifactId = String.valueOf(action.getOrDefault("artifactId", ""));
            String expectedFixturePath = String.valueOf(action.getOrDefault("expectedFixturePath", ""));
            Map<String, Object> approval = approvals.getOrDefault(artifactId, Map.of());
            boolean approvalPresent = !approval.isEmpty();
            boolean approvalReady = approvalReady(approval, expectedFixturePath);
            boolean fixturePresent = Boolean.TRUE.equals(action.get("fixturePresent"));
            boolean intakeComplete = Boolean.TRUE.equals(action.get("complete"));
            boolean approvedForM17 = approvalReady && fixturePresent && intakeComplete;

            Map<String, Object> item = baseItem(artifactId, expectedFixturePath);
            item.put("approvalPresent", approvalPresent);
            item.put("approvalReady", approvalReady);
            item.put("approvedForM17", approvedForM17);
            item.put("fixturePresent", fixturePresent);
            item.put("intakeComplete", intakeComplete);
            item.put("reviewedApproval", approvalPresent ? approval : Map.of());
            auditItems.add(item);

            templateItems.add(templateItem(artifactId, expectedFixturePath, String.valueOf(action.getOrDefault("artifactKind", "runtime_fixture"))));
            if (!approvedForM17) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-RUNTIME-FIXTURE-APPROVAL-MISSING",
                        EchoNativeIssueSeverity.ERROR,
                        "Runtime fixture approval is not ready",
                        artifactId + " requires a present fixture file, completed intake, and reviewed fixture-local approval before M17 can close.",
                        null,
                        packId,
                        List.of(fixture.resolve(expectedFixturePath).normalize().toString().replace('\\', '/'), approvalsPath.toString().replace('\\', '/')),
                        "Provide a fixture-local runtime-fixture-approvals.json entry only after the local artifact source is reviewed."
                ));
            }
        }
        auditItems.sort(Comparator.comparing(item -> String.valueOf(item.get("artifactId"))));
        templateItems.sort(Comparator.comparing(item -> String.valueOf(item.get("artifactId"))));

        boolean approvalsReady = !auditItems.isEmpty() && auditItems.stream().allMatch(item -> Boolean.TRUE.equals(item.get("approvedForM17")));
        return new EchoNativeRuntimeFixtureApprovalAuditOutcome(
                packId,
                approvalAudit(packId, approvalsReady, auditItems, approvalsPath, diagnostics),
                approvalTemplate(packId, approvalsReady, templateItems, diagnostics),
                diagnostics.stream()
                        .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                                .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                                .thenComparing(EchoNativeDiagnostic::summary))
                        .toList()
        );
    }

    private static Map<String, Object> approvalAudit(
            String packId,
            boolean approvalsReady,
            List<Map<String, Object>> auditItems,
            Path approvalsPath,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_runtime_fixture_approval_audit", diagnostics);
        data.put("approvalAuditCount", auditItems.size());
        data.put("approvalFile", approvalsPath.toString().replace('\\', '/'));
        data.put("approvedCount", auditItems.stream().filter(item -> Boolean.TRUE.equals(item.get("approvedForM17"))).count());
        data.put("approvals", auditItems);
        data.put("approvalsReady", approvalsReady);
        data.put("packId", packId);
        data.put("phase13M17LaunchBlocked", !approvalsReady);
        data.put("summary", approvalsReady
                ? "Runtime fixture approvals are complete for M17."
                : "Runtime fixture approvals are not ready; M17 remains blocked.");
        return data;
    }

    private static Map<String, Object> approvalTemplate(
            String packId,
            boolean approvalsReady,
            List<Map<String, Object>> templateItems,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_runtime_fixture_approval_template", diagnostics);
        data.put("approvalTemplate", Map.of(
                "schema", "echo.native.runtime_fixture_approvals.v1",
                "packId", packId,
                "downloadsAllowed", false,
                "extractionAllowed", false,
                "filesystemMutated", false,
                "approvals", templateItems
        ));
        data.put("approvalTemplateCount", templateItems.size());
        data.put("approvalsReady", approvalsReady);
        data.put("packId", packId);
        data.put("summary", "Template data for future reviewed runtime-fixture-approvals.json entries.");
        return data;
    }

    private static List<Map<String, Object>> actions(Map<String, Object> intakePlan) {
        Map<String, Object> data = EchoNativeJson.asObject(intakePlan.get("data"));
        Object rawActions = data.get("actions");
        if (!(rawActions instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> actions = new ArrayList<>();
        for (Object raw : list) {
            Map<String, Object> action = EchoNativeJson.asObject(raw);
            if (!String.valueOf(action.getOrDefault("artifactId", "")).isBlank()) {
                actions.add(action);
            }
        }
        actions.sort(Comparator.comparing(action -> String.valueOf(action.get("artifactId"))));
        return List.copyOf(actions);
    }

    private static Map<String, Map<String, Object>> approvals(
            Path approvalsPath,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(approvalsPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-APPROVAL-FILE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture approval file is missing",
                    "runtime-fixture-approvals.json is required before M17 can treat supplied runtime fixtures as reviewed.",
                    null,
                    packId,
                    List.of(approvalsPath.toString().replace('\\', '/')),
                    "Create this file only after approved local runtime artifacts are supplied and reviewed."
            ));
            return Map.of();
        }
        Map<String, Object> root = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(approvalsPath)));
        Object rawApprovals = root.get("approvals");
        if (!(rawApprovals instanceof List<?> list)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-APPROVAL-FILE-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture approval file is invalid",
                    "runtime-fixture-approvals.json must contain an approvals array.",
                    null,
                    packId,
                    List.of(approvalsPath.toString().replace('\\', '/')),
                    "Use the generated runtime-fixture-approval-template.json data as the review starting point."
            ));
            return Map.of();
        }
        Map<String, Map<String, Object>> approvals = new LinkedHashMap<>();
        for (Object raw : list) {
            Map<String, Object> approval = EchoNativeJson.asObject(raw);
            String artifactId = String.valueOf(approval.getOrDefault("artifactId", ""));
            if (!artifactId.isBlank()) {
                approvals.put(artifactId, approval);
            }
        }
        return approvals;
    }

    private static boolean approvalReady(Map<String, Object> approval, String expectedFixturePath) {
        return !approval.isEmpty()
                && Boolean.TRUE.equals(approval.get("reviewed"))
                && Boolean.TRUE.equals(approval.get("approved"))
                && "approved".equals(approval.get("reviewStatus"))
                && expectedFixturePath.equals(String.valueOf(approval.getOrDefault("localPath", "")))
                && Boolean.FALSE.equals(approval.get("downloadsAllowed"))
                && Boolean.FALSE.equals(approval.get("extractionAllowed"));
    }

    private static Map<String, Object> baseItem(String artifactId, String expectedFixturePath) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("artifactId", artifactId);
        item.put("classloaderCreated", false);
        item.put("commandExecuted", false);
        item.put("downloadsAllowed", false);
        item.put("expectedFixturePath", expectedFixturePath);
        item.put("filesystemMutated", false);
        item.put("gameClassesResolved", false);
        item.put("nativeExtractionStarted", false);
        item.put("processLaunched", false);
        item.put("registryInjected", false);
        item.put("registryMutated", false);
        item.put("safeToAutoPopulate", false);
        return item;
    }

    private static Map<String, Object> templateItem(String artifactId, String expectedFixturePath, String artifactKind) {
        Map<String, Object> item = baseItem(artifactId, expectedFixturePath);
        item.put("artifactKind", artifactKind);
        item.put("approved", true);
        item.put("localPath", expectedFixturePath);
        item.put("reviewStatus", "approved");
        item.put("reviewed", true);
        item.put("source", artifactId.startsWith("native:") ? "minecraft-native-fixture" : "minecraft-runtime-fixture");
        return item;
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (reportPath == null || !Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-APPROVAL-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture approval audit required report missing",
                    "Runtime fixture approval audit requires " + reportName + ".",
                    null,
                    packId,
                    reportPath == null ? List.of() : List.of(relativeReportPath(reportPath)),
                    "Run phase13 plan runtime-fixture-intake <fixture> before auditing fixture approvals."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
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
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("safeToAutoPopulate", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static String relativeReportPath(Path path) {
        if (path == null) {
            return "";
        }
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
