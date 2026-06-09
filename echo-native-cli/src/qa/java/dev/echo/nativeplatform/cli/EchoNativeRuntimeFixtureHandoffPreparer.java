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

final class EchoNativeRuntimeFixtureHandoffPreparer {
    EchoNativeRuntimeFixtureHandoffOutcome prepare(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> intakePlan = readRequiredReport(requiredReports.get("runtime-fixture-intake-plan.json"), packId, "runtime-fixture-intake-plan.json", diagnostics);
        Map<String, Object> approvalAudit = readRequiredReport(requiredReports.get("runtime-fixture-approval-audit.json"), packId, "runtime-fixture-approval-audit.json", diagnostics);
        Map<String, Object> approvalTemplate = readRequiredReport(requiredReports.get("runtime-fixture-approval-template.json"), packId, "runtime-fixture-approval-template.json", diagnostics);
        Map<String, Object> m17Completion = readOptionalReport(requiredReports.get("phase13-m17-completion.json"));

        List<Map<String, Object>> actions = actions(intakePlan);
        List<Map<String, Object>> approvalTemplateItems = approvalTemplateItems(approvalTemplate);
        if (actions.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-HANDOFF-ACTIONS-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture handoff actions are missing",
                    "Runtime fixture handoff requires runtime fixture intake actions.",
                    null,
                    packId,
                    List.of(relativeReportPath(requiredReports.get("runtime-fixture-intake-plan.json"))),
                    "Run phase13 verify runtime-fixtures and phase13 plan runtime-fixture-intake before preparing the handoff."
            ));
        }

        Map<String, Map<String, Object>> templatesByArtifact = new LinkedHashMap<>();
        for (Map<String, Object> item : approvalTemplateItems) {
            templatesByArtifact.put(String.valueOf(item.getOrDefault("artifactId", "")), item);
        }

        List<Map<String, Object>> handoffItems = new ArrayList<>();
        for (Map<String, Object> action : actions) {
            String artifactId = String.valueOf(action.getOrDefault("artifactId", ""));
            String expectedFixturePath = String.valueOf(action.getOrDefault("expectedFixturePath", ""));
            boolean complete = Boolean.TRUE.equals(action.get("complete"));
            Map<String, Object> item = baseItem(artifactId, expectedFixturePath);
            item.put("actionId", action.getOrDefault("actionId", ""));
            item.put("artifactKind", action.getOrDefault("artifactKind", "runtime_fixture"));
            item.put("handoffComplete", complete);
            item.put("operatorInstruction", action.getOrDefault("operatorInstruction", ""));
            item.put("requiredRuntimeArtifactsJsonEntry", action.getOrDefault("requiredRuntimeArtifactsJsonEntry", Map.of()));
            item.put("requiredRuntimeFixtureApprovalEntry", templatesByArtifact.getOrDefault(artifactId, Map.of()));
            item.put("targetFile", action.getOrDefault("targetFile", fixture.resolve(expectedFixturePath).toString().replace('\\', '/')));
            handoffItems.add(item);
            if (!complete) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-RUNTIME-FIXTURE-HANDOFF-PENDING",
                        EchoNativeIssueSeverity.WARNING,
                        "Runtime fixture handoff item is pending",
                        artifactId + " still needs a reviewed local fixture file, runtime-artifacts.json mapping, and runtime-fixture-approvals.json evidence.",
                        null,
                        packId,
                        List.of(fixture.resolve(expectedFixturePath).normalize().toString().replace('\\', '/'), fixture.resolve("runtime-fixture-approvals.json").toString().replace('\\', '/')),
                        "Complete this item outside the CLI with already-authorized local artifacts only."
                ));
            }
        }
        handoffItems.sort(Comparator.comparing(item -> String.valueOf(item.get("artifactId"))));

        boolean approvalsReady = Boolean.TRUE.equals(EchoNativeJson.asObject(approvalAudit.get("data")).get("approvalsReady"));
        boolean handoffReady = !handoffItems.isEmpty()
                && handoffItems.stream().allMatch(item -> Boolean.TRUE.equals(item.get("handoffComplete")))
                && approvalsReady;

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        return new EchoNativeRuntimeFixtureHandoffOutcome(
                packId,
                handoff(packId, fixture, handoffReady, handoffItems, approvalAudit, m17Completion, sortedDiagnostics),
                runbook(packId, fixture, handoffReady, handoffItems, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> handoff(
            String packId,
            Path fixture,
            boolean handoffReady,
            List<Map<String, Object>> handoffItems,
            Map<String, Object> approvalAudit,
            Map<String, Object> m17Completion,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_runtime_fixture_handoff", diagnostics);
        data.put("approvalFile", fixture.resolve("runtime-fixture-approvals.json").toString().replace('\\', '/'));
        data.put("approvalsReady", Boolean.TRUE.equals(EchoNativeJson.asObject(approvalAudit.get("data")).get("approvalsReady")));
        data.put("blockedReportCount", EchoNativeJson.asObject(m17Completion.get("data")).getOrDefault("blockedReportCount", 0));
        data.put("handoffItemCount", handoffItems.size());
        data.put("handoffItems", handoffItems);
        data.put("handoffReady", handoffReady);
        data.put("packId", packId);
        data.put("phase13M17Complete", Boolean.TRUE.equals(EchoNativeJson.asObject(m17Completion.get("data")).get("phase13M17Complete")));
        data.put("phase13M17LaunchBlocked", !handoffReady);
        data.put("summary", handoffReady
                ? "Runtime fixture handoff is complete for M17."
                : "Runtime fixture handoff is pending reviewed local artifacts, mappings, approvals, or M17 closeout.");
        return data;
    }

    private static Map<String, Object> runbook(
            String packId,
            Path fixture,
            boolean handoffReady,
            List<Map<String, Object>> handoffItems,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_runtime_fixture_validation_runbook", diagnostics);
        data.put("handoffReady", handoffReady);
        data.put("manualIntakeOnly", true);
        data.put("packId", packId);
        data.put("phase13M18BlockedUntilM17Passes", !handoffReady);
        data.put("requiredCommands", List.of(
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 verify runtime-fixtures " + fixture.toString().replace('\\', '/') + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 plan runtime-fixture-intake " + fixture.toString().replace('\\', '/') + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 audit runtime-fixture-approval " + fixture.toString().replace('\\', '/') + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 prepare runtime-fixture-handoff " + fixture.toString().replace('\\', '/') + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 draft runtime-fixture-approval " + fixture.toString().replace('\\', '/') + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 audit runtime-fixture-integrity " + fixture.toString().replace('\\', '/') + "\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 verify m17 " + fixture.toString().replace('\\', '/') + "\""
        ));
        data.put("requiredFiles", handoffItems.stream()
                .map(item -> item.get("expectedFixturePath"))
                .toList());
        data.put("requiredApprovalFile", "runtime-fixture-approvals.json");
        data.put("requiredRuntimeArtifactsJsonEntries", handoffItems.stream()
                .map(item -> item.getOrDefault("requiredRuntimeArtifactsJsonEntry", Map.of()))
                .filter(entry -> entry instanceof Map<?, ?> map && !map.isEmpty())
                .toList());
        data.put("requiredRuntimeArtifactsJsonEntryCount", handoffItems.stream()
                .filter(item -> item.get("requiredRuntimeArtifactsJsonEntry") instanceof Map<?, ?> map && !map.isEmpty())
                .count());
        data.put("requiredRuntimeFixtureApprovalEntries", handoffItems.stream()
                .map(item -> item.getOrDefault("requiredRuntimeFixtureApprovalEntry", Map.of()))
                .filter(entry -> entry instanceof Map<?, ?> map && !map.isEmpty())
                .toList());
        data.put("requiredRuntimeFixtureApprovalEntryCount", handoffItems.stream()
                .filter(item -> item.get("requiredRuntimeFixtureApprovalEntry") instanceof Map<?, ?> map && !map.isEmpty())
                .count());
        data.put("approvalEvidenceWorkflow", List.of(
                "Supply already-authorized local runtime files outside the CLI.",
                "Rerun phase13 draft runtime-fixture-approval to compute byteSize and SHA-256.",
                "Review the generated runtime-fixture-approval-draft.json entries.",
                "Write runtime-fixture-approvals.json manually only after the reviewed values are accepted.",
                "Rerun phase13 audit runtime-fixture-integrity and phase13 verify m17."
        ));
        data.put("reviewChecklist", List.of(
                "Use only already-authorized local runtime artifacts.",
                "Do not download, extract, or generate Minecraft artifacts from the CLI.",
                "Update runtime-artifacts.json only after reviewing each local artifact source.",
                "Create runtime-fixture-approvals.json only after the expected local files are present and reviewed.",
                "Run phase13 draft runtime-fixture-approval after the files are present to compute byteSize and SHA-256 evidence.",
                "Include byteSize and sha256 in runtime-fixture-approvals.json after the local file hash is reviewed.",
                "Regenerate M17 reports and require phase13-m17-completion.json to PASS before M18."
        ));
        data.put("summary", "Manual validation sequence for Phase 13 M17 runtime fixture intake.");
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

    private static List<Map<String, Object>> approvalTemplateItems(Map<String, Object> approvalTemplateReport) {
        Map<String, Object> data = EchoNativeJson.asObject(approvalTemplateReport.get("data"));
        Map<String, Object> template = EchoNativeJson.asObject(data.get("approvalTemplate"));
        Object rawApprovals = template.get("approvals");
        if (!(rawApprovals instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> approvals = new ArrayList<>();
        for (Object raw : list) {
            Map<String, Object> approval = EchoNativeJson.asObject(raw);
            if (!String.valueOf(approval.getOrDefault("artifactId", "")).isBlank()) {
                approvals.add(approval);
            }
        }
        approvals.sort(Comparator.comparing(action -> String.valueOf(action.get("artifactId"))));
        return List.copyOf(approvals);
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

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (reportPath == null || !Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-HANDOFF-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture handoff required report missing",
                    "Runtime fixture handoff requires " + reportName + ".",
                    null,
                    packId,
                    reportPath == null ? List.of() : List.of(relativeReportPath(reportPath)),
                    "Regenerate the M17 runtime fixture intake, approval, and closeout reports first."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static Map<String, Object> readOptionalReport(Path reportPath) throws IOException {
        if (reportPath == null || !Files.isRegularFile(reportPath)) {
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
