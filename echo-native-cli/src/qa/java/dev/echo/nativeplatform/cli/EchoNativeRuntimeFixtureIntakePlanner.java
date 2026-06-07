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

final class EchoNativeRuntimeFixtureIntakePlanner {
    EchoNativeRuntimeFixtureIntakeOutcome plan(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> presenceReport = readRequiredReport(requiredReports.get("runtime-fixture-presence.json"), packId, "runtime-fixture-presence.json", diagnostics);
        Map<String, Object> mappingReport = readRequiredReport(requiredReports.get("runtime-fixture-mapping-readiness.json"), packId, "runtime-fixture-mapping-readiness.json", diagnostics);

        List<Map<String, Object>> actions = intakeActions(fixture, packId, presenceReport, mappingReport, diagnostics);
        if (actions.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-INTAKE-CONTRACTS-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture intake contracts are missing",
                    "Runtime fixture intake planning found no actionable runtime fixture contracts.",
                    null,
                    packId,
                    List.of(relativeReportPath(requiredReports.get("runtime-fixture-presence.json")), relativeReportPath(requiredReports.get("runtime-fixture-mapping-readiness.json"))),
                    "Regenerate artifact blocker, packaging, and runtime fixture reports before planning intake."
            ));
        }
        boolean intakeReady = !actions.isEmpty() && actions.stream().allMatch(action -> Boolean.TRUE.equals(action.get("complete")));

        return new EchoNativeRuntimeFixtureIntakeOutcome(
                packId,
                intakePlan(packId, intakeReady, actions, diagnostics),
                intakeChecklist(packId, intakeReady, actions, diagnostics),
                diagnostics.stream()
                        .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                                .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                                .thenComparing(EchoNativeDiagnostic::summary))
                        .toList()
        );
    }

    private static List<Map<String, Object>> intakeActions(
            Path fixture,
            String packId,
            Map<String, Object> presenceReport,
            Map<String, Object> mappingReport,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Map<String, Object>> fixtureChecks = checksByArtifact(presenceReport, "fixtureChecks");
        Map<String, Map<String, Object>> mappingChecks = checksByArtifact(mappingReport, "mappingChecks");
        List<String> artifactIds = new ArrayList<>(fixtureChecks.keySet());
        for (String artifactId : mappingChecks.keySet()) {
            if (!artifactIds.contains(artifactId)) {
                artifactIds.add(artifactId);
            }
        }
        artifactIds.sort(String::compareTo);

        List<Map<String, Object>> actions = new ArrayList<>();
        for (String artifactId : artifactIds) {
            Map<String, Object> fixtureCheck = fixtureChecks.getOrDefault(artifactId, Map.of());
            Map<String, Object> mappingCheck = mappingChecks.getOrDefault(artifactId, Map.of());
            boolean fixturePresent = Boolean.TRUE.equals(fixtureCheck.get("fixturePresent"));
            boolean mappingReady = Boolean.TRUE.equals(mappingCheck.get("mappingReady"));
            boolean complete = fixturePresent && mappingReady;

            Map<String, Object> action = new LinkedHashMap<>();
            String expectedFixturePath = String.valueOf(fixtureCheck.getOrDefault("expectedFixturePath",
                    expectedPathFromRequiredMapping(mappingCheck)));
            action.put("actionId", "phase13.m17.runtime_fixture_intake." + safeId(artifactId));
            action.put("artifactId", artifactId);
            action.put("artifactKind", String.valueOf(fixtureCheck.getOrDefault("artifactKind", mappingCheck.getOrDefault("artifactKind", "runtime_fixture"))));
            action.put("approvedLocalArtifactRequired", true);
            action.put("classloaderCreated", false);
            action.put("commandExecuted", false);
            action.put("complete", complete);
            action.put("downloadsAllowed", false);
            action.put("expectedFixturePath", expectedFixturePath);
            action.put("filesystemMutated", false);
            action.put("fixturePresent", fixturePresent);
            action.put("gameClassesResolved", false);
            action.put("localPath", expectedFixturePath);
            action.put("mappingReady", mappingReady);
            action.put("nativeExtractionStarted", false);
            action.put("operatorInstruction", instruction(artifactId, expectedFixturePath, fixturePresent, mappingReady));
            action.put("processLaunched", false);
            action.put("registryInjected", false);
            action.put("registryMutated", false);
            action.put("requiredRuntimeArtifactsJsonEntry", mappingCheck.getOrDefault("requiredRuntimeArtifactsJsonEntry", Map.of()));
            action.put("reviewRequired", true);
            action.put("safeToAutoPopulate", false);
            action.put("targetFile", fixture.resolve(expectedFixturePath).normalize().toString().replace('\\', '/'));
            actions.add(action);

            if (!complete) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-RUNTIME-FIXTURE-INTAKE-REQUIRED",
                        EchoNativeIssueSeverity.WARNING,
                        "Runtime fixture intake action required",
                        artifactId + " requires an approved local fixture file and reviewed runtime-artifacts.json mapping before M17 can close.",
                        null,
                        packId,
                        List.of(fixture.resolve(expectedFixturePath).normalize().toString().replace('\\', '/'), fixture.resolve("runtime-artifacts.json").toString().replace('\\', '/')),
                        "Supply only already-authorized local artifacts outside the CLI, then update runtime-artifacts.json after review."
                ));
            }
        }
        return List.copyOf(actions);
    }

    private static Map<String, Map<String, Object>> checksByArtifact(Map<String, Object> report, String key) {
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        Object rawChecks = data.get(key);
        if (!(rawChecks instanceof List<?> checks)) {
            return Map.of();
        }
        Map<String, Map<String, Object>> byArtifact = new LinkedHashMap<>();
        for (Object raw : checks) {
            Map<String, Object> check = EchoNativeJson.asObject(raw);
            String artifactId = String.valueOf(check.getOrDefault("artifactId", ""));
            if (!artifactId.isBlank()) {
                byArtifact.put(artifactId, check);
            }
        }
        return byArtifact;
    }

    private static String expectedPathFromRequiredMapping(Map<String, Object> mappingCheck) {
        Map<String, Object> required = EchoNativeJson.asObject(mappingCheck.get("requiredRuntimeArtifactsJsonEntry"));
        return String.valueOf(required.getOrDefault("localPath", ""));
    }

    private static String instruction(String artifactId, String expectedFixturePath, boolean fixturePresent, boolean mappingReady) {
        if (fixturePresent && mappingReady) {
            return artifactId + " is present and mapped; no intake action is needed.";
        }
        if (!fixturePresent && !mappingReady) {
            return "Place an already-authorized local artifact at " + expectedFixturePath + ", then add the reviewed non-downloading runtime-artifacts.json mapping.";
        }
        if (!fixturePresent) {
            return "Place an already-authorized local artifact at " + expectedFixturePath + "; the existing mapping must continue to forbid downloads and extraction.";
        }
        return "Review and approve the runtime-artifacts.json mapping for " + artifactId + " without changing or auto-populating the fixture file.";
    }

    private static Map<String, Object> intakePlan(
            String packId,
            boolean intakeReady,
            List<Map<String, Object>> actions,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_runtime_fixture_intake_plan", diagnostics);
        data.put("actionCount", actions.size());
        data.put("actions", actions);
        data.put("completeActionCount", actions.stream().filter(action -> Boolean.TRUE.equals(action.get("complete"))).count());
        data.put("intakeReady", intakeReady);
        data.put("packId", packId);
        data.put("phase13M17LaunchBlocked", !intakeReady);
        data.put("safeToAutoPopulate", false);
        data.put("summary", intakeReady
                ? "Runtime fixture intake actions are complete; rerun runtime fixture verification and M17 closeout."
                : "Runtime fixture intake requires approved local artifacts and reviewed mappings before M17 can close.");
        return data;
    }

    private static Map<String, Object> intakeChecklist(
            String packId,
            boolean intakeReady,
            List<Map<String, Object>> actions,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<Map<String, Object>> checklist = new ArrayList<>();
        for (Map<String, Object> action : actions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("artifactId", action.get("artifactId"));
            item.put("checkId", action.get("actionId") + ".check");
            item.put("complete", action.get("complete"));
            item.put("expectedFixturePath", action.get("expectedFixturePath"));
            item.put("fixturePresent", action.get("fixturePresent"));
            item.put("mappingReady", action.get("mappingReady"));
            item.put("reviewRequired", true);
            item.put("safeToAutoPopulate", false);
            checklist.add(item);
        }
        Map<String, Object> data = base("phase13_m17_runtime_fixture_intake_checklist", diagnostics);
        data.put("checklist", checklist);
        data.put("checklistCount", checklist.size());
        data.put("completeChecklistCount", checklist.stream().filter(item -> Boolean.TRUE.equals(item.get("complete"))).count());
        data.put("intakeReady", intakeReady);
        data.put("packId", packId);
        data.put("summary", intakeReady
                ? "Runtime fixture intake checklist is complete."
                : "Runtime fixture intake checklist remains open.");
        return data;
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (reportPath == null || !Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-INTAKE-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture intake required report missing",
                    "Runtime fixture intake planning requires " + reportName + ".",
                    null,
                    packId,
                    reportPath == null ? List.of() : List.of(relativeReportPath(reportPath)),
                    "Run phase13 verify runtime-fixtures <fixture> before planning runtime fixture intake."
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
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static String safeId(String value) {
        return value.toLowerCase()
                .replace(':', '.')
                .replace('/', '.')
                .replace('\\', '.')
                .replaceAll("[^a-z0-9._-]", "_");
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
