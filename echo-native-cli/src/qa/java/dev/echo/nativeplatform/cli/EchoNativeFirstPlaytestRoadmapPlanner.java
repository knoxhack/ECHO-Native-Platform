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

final class EchoNativeFirstPlaytestRoadmapPlanner {
    EchoNativeFirstPlaytestRoadmapOutcome plan(
            String packId,
            Path fixture,
            List<EchoNativeDiagnostic> validationDiagnostics,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>(validationDiagnostics);
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            reports.put(entry.getKey(), readReport(entry.getValue(), packId, entry.getKey(), diagnostics));
        }

        Map<String, Object> m17 = data(reports.get("phase13-m17-completion.json"));
        Map<String, Object> m18 = data(reports.get("phase13-m18-readiness-audit.json"));
        Map<String, Object> m18Completion = data(reports.get("phase13-m18-completion.json"));
        Map<String, Object> m19 = data(reports.get("phase13-m19-readiness.json"));
        Map<String, Object> m19Completion = data(reports.get("phase13-m19-completion.json"));
        Map<String, Object> openGate = data(reports.get("first-playtest-open-gate.json"));
        Map<String, Object> playtest = data(reports.get("phase13-first-playtest-blockers.json"));
        Map<String, Object> handoff = data(reports.get("runtime-fixture-handoff.json"));
        Map<String, Object> integrity = data(reports.get("runtime-fixture-integrity-audit.json"));
        Path reportDirectory = requiredReports.values().stream()
                .findFirst()
                .map(Path::getParent)
                .orElse(Path.of("reports", "echo-native", packId));

        boolean fixtureValid = validationDiagnostics.stream().noneMatch(EchoNativeFirstPlaytestRoadmapPlanner::isBlocking);
        boolean m17Complete = Boolean.TRUE.equals(m17.get("phase13M17Complete"));
        boolean m18Ready = Boolean.TRUE.equals(m18.get("phase13M18Ready"));
        boolean m18Complete = Boolean.TRUE.equals(m18Completion.get("phase13M18Complete"));
        boolean m19Ready = Boolean.TRUE.equals(m19.get("phase13M19Ready"));
        boolean m19Complete = Boolean.TRUE.equals(m19Completion.get("phase13M19Complete"));
        boolean firstPlaytestOpen = Boolean.TRUE.equals(openGate.get("firstPlaytestOpen"))
                || Boolean.TRUE.equals(playtest.get("firstPlaytestOpen"));
        long blockedReportCount = asLong(m17.get("blockedReportCount"));
        long blockerCount = asLong(playtest.get("blockerCount"));
        List<Map<String, Object>> handoffItems = objects(handoff.get("handoffItems"));
        List<Map<String, Object>> integrityChecks = objects(integrity.get("integrityChecks"));

        if (fixtureValid && !firstPlaytestOpen) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-FIRST-PLAYTEST-ROADMAP-BLOCKED",
                    EchoNativeIssueSeverity.WARNING,
                    "First playtest is not open yet",
                    "The roadmap is valid, but M17, M18, or M19 gates still block the first playtest.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/phase13-first-playtest-roadmap.json"),
                    "Follow phase13-first-playtest-next-actions.json before attempting M18 or M19."
            ));
        }

        List<Map<String, Object>> milestones = milestones(reportDirectory, m17Complete, m18Ready, m18Complete, m19Complete, firstPlaytestOpen, blockedReportCount, blockerCount);
        List<Map<String, Object>> remainingMilestones = milestones.stream()
                .filter(milestone -> {
                    String id = String.valueOf(milestone.get("id"));
                    return id.startsWith("phase13.m17") || id.startsWith("phase13.m18") || id.startsWith("phase13.m19");
                })
                .toList();
        List<Map<String, Object>> actions = nextActions(handoffItems, integrityChecks, m17Complete, m18Ready, m18Complete, firstPlaytestOpen);
        boolean safeToAttemptLaunch = fixtureValid && m17Complete;
        boolean safeToOpenPlaytest = fixtureValid && firstPlaytestOpen;
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeFirstPlaytestRoadmapOutcome(
                packId,
                roadmap(packId, fixture, fixtureValid, m17Complete, m18Ready, m18Complete, m19Ready, m19Complete, firstPlaytestOpen, safeToAttemptLaunch, safeToOpenPlaytest, remainingMilestones, actions, sortedDiagnostics),
                actionPlan(packId, actions, handoffItems, integrityChecks, sortedDiagnostics),
                fullRoadmap(packId, fixture, milestones, remainingMilestones, actions, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> roadmap(
            String packId,
            Path fixture,
            boolean fixtureValid,
            boolean m17Complete,
            boolean m18Ready,
            boolean m18Complete,
            boolean m19Ready,
            boolean m19Complete,
            boolean firstPlaytestOpen,
            boolean safeToAttemptLaunch,
            boolean safeToOpenPlaytest,
            List<Map<String, Object>> milestones,
            List<Map<String, Object>> actions,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_first_playtest_roadmap", diagnostics);
        data.put("fixture", fixture.toString().replace('\\', '/'));
        data.put("fixtureValid", fixtureValid);
        data.put("milestones", milestones);
        data.put("milestoneCount", milestones.size());
        data.put("completedMilestoneCount", countStatus(milestones, "COMPLETE"));
        data.put("blockedMilestoneCount", countStatus(milestones, "BLOCKED"));
        data.put("nextActionCount", actions.size());
        data.put("packId", packId);
        data.put("phase13M17Complete", m17Complete);
        data.put("phase13M18Ready", m18Ready);
        data.put("phase13M18Complete", m18Complete);
        data.put("phase13M19Ready", m19Ready);
        data.put("phase13M19Complete", m19Complete);
        data.put("firstPlaytestOpen", firstPlaytestOpen);
        data.put("safeToAttemptIsolatedLaunch", safeToAttemptLaunch);
        data.put("safeToOpenFirstPlaytest", safeToOpenPlaytest);
        data.put("summary", firstPlaytestOpen
                ? "First playtest gates are open."
                : "First playtest remains gated; follow the deterministic M17/M18/M19 action runway.");
        return data;
    }

    private static Map<String, Object> fullRoadmap(
            String packId,
            Path fixture,
            List<Map<String, Object>> milestones,
            List<Map<String, Object>> remainingMilestones,
            List<Map<String, Object>> actions,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_first_playtest_full_roadmap", diagnostics);
        data.put("fixture", fixture.toString().replace('\\', '/'));
        data.put("milestones", milestones);
        data.put("milestoneCount", milestones.size());
        data.put("completedMilestoneCount", countStatus(milestones, "COMPLETE"));
        data.put("blockedMilestoneCount", countStatus(milestones, "BLOCKED"));
        data.put("remainingMilestones", remainingMilestones);
        data.put("remainingMilestoneCount", remainingMilestones.size());
        data.put("nextActions", actions);
        data.put("nextActionCount", actions.size());
        data.put("packId", packId);
        data.put("firstIncompleteMilestone", firstIncompleteMilestone(milestones));
        data.put("firstPlaytestOpen", milestones.stream().anyMatch(milestone -> "phase13.m19.first_playtest_candidate".equals(milestone.get("id")) && "COMPLETE".equals(milestone.get("status"))));
        data.put("safeToOpenFirstPlaytest", milestones.stream().anyMatch(milestone -> "phase13.m19.first_playtest_candidate".equals(milestone.get("id")) && "COMPLETE".equals(milestone.get("status"))));
        data.put("summary", firstIncompleteMilestone(milestones).isBlank()
                ? "Full Phase 13 M2-M19 roadmap is complete; first playtest gate is open for the internal tester dry run."
                : "Full Phase 13 M2-M19 roadmap is report-backed; first playtest remains closed until M17, M18, and M19 pass.");
        return data;
    }

    private static Map<String, Object> actionPlan(
            String packId,
            List<Map<String, Object>> actions,
            List<Map<String, Object>> handoffItems,
            List<Map<String, Object>> integrityChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_first_playtest_next_actions", diagnostics);
        data.put("actionCount", actions.size());
        data.put("actions", actions);
        data.put("handoffItemCount", handoffItems.size());
        data.put("integrityCheckCount", integrityChecks.size());
        data.put("packId", packId);
        data.put("validationCommands", List.of(
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 verify runtime-fixtures fixtures/ashfall\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 plan runtime-fixture-intake fixtures/ashfall\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 audit runtime-fixture-approval fixtures/ashfall\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 prepare runtime-fixture-handoff fixtures/ashfall\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 draft runtime-fixture-approval fixtures/ashfall\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 audit runtime-fixture-integrity fixtures/ashfall\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 verify m17 fixtures/ashfall\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 verify m18 fixtures/ashfall\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 package first-playtest fixtures/ashfall\"",
                ".\\gradlew.bat :echo-native-cli:run --args=\"phase13 plan first-playtest fixtures/ashfall\""
        ));
        data.put("summary", actions.isEmpty()
                ? "No first-playtest roadmap actions remain in this report."
                : "First-playtest roadmap actions are ordered by the current safe milestone gate.");
        return data;
    }

    private static List<Map<String, Object>> milestones(
            Path reportDirectory,
            boolean m17Complete,
            boolean m18Ready,
            boolean m18Complete,
            boolean m19Complete,
            boolean firstPlaytestOpen,
            long blockedReportCount,
            long blockerCount
    ) {
        List<Map<String, Object>> milestones = new ArrayList<>();
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m2.minecraft_resolver_planning", "Minecraft Resolver Planning", "minecraft-version-resolver-plan.json", "minecraft-resolver-safety-status.json", "minecraftResolverStarted=false; no network or runtime classes."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m3.library_resolver_planning", "Library Resolver Planning", "library-resolution-plan.json", "library-resolver-safety-status.json", "missing libraries are diagnostics, not actions."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m4.classpath_builder_plan", "Classpath Builder Plan", "classpath-builder-plan.json", "classpath-builder-safety-status.json", "classpath entries remain data-only."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m5.native_library_extraction_plan", "Native Library Extraction Plan", "native-extraction-plan.json", "native-extraction-safety-status.json", "nativeExtractionStarted=false."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m6.launch_argument_planner", "Launch Argument Planner", "launch-argument-builder-plan.json", "launch-argument-safety-status.json", "processLaunched=false."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m7.controlled_test_process_boundary", "Controlled Test Process Boundary", "controlled-dummy-process-result.json", "dummy-process-crash-boundary.json", "real Minecraft launch remains blocked."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m8.addon_descriptor_runtime_discovery", "Addon Descriptor Runtime Discovery", "addon-runtime-descriptors.json", "addon-runtime-discovery-safety-status.json", "no addon code execution."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m9.lifecycle_stub_execution", "Lifecycle Stub Execution", "lifecycle-stub-execution-result.json", "lifecycle-stub-safety-status.json", "stub-only lifecycle handlers."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m10.service_bus_prototype", "Service Bus Prototype", "service-bus-registry.json", "service-bus-safety-status.json", "inert service handles only."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m11.config_system_prototype", "Config System Prototype", "config-validation-result.json", "config-safety-status.json", "no installed pack config mutation."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m12.resource_bridge_prototype", "Resource Bridge Prototype", "resource-source-inventory.json", "resource-bridge-safety-status.json", "no Minecraft runtime resource access."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m13.registry_bridge_prototype", "Registry Bridge Prototype", "sandbox-registry-model.json", "registry-bridge-safety-status.json", "no Minecraft registry injection."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m14.network_bridge_prototype", "Network Bridge Prototype", "network-schema-model.json", "network-bridge-safety-status.json", "no live networking."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m15.transform_pipeline_prototype", "Transform Pipeline Prototype", "transform-pipeline-plan.json", "transform-safety-status.json", "no Minecraft bytecode transforms."));
        milestones.add(reportBackedMilestone(reportDirectory, "phase13.m16.crash_boundary_hardening", "Crash Boundary Hardening", "failure-containment-matrix.json", "phase13-m16-safety-status.json", "deterministic diagnostics and support-bundle plan only."));
        milestones.add(milestone("phase13.m17.first_controlled_minecraft_launch", "First Controlled Minecraft Launch", m17Complete ? "COMPLETE" : "BLOCKED", m17Complete, blockedReportCount, "phase13-m17-completion.json", "Requires verified local runtime fixtures before isolated launch can proceed."));
        milestones.add(milestone("phase13.m18.ashfall_native_smoke_session", "Ashfall Native Smoke Session", m18Complete ? "COMPLETE" : (m18Ready ? "READY" : "BLOCKED"), m18Ready || m18Complete, (m18Ready || m18Complete) ? 0 : blockedReportCount, m18Complete ? "phase13-m18-completion.json" : "phase13-m18-readiness-audit.json", "Requires M17 completion before smoke session."));
        milestones.add(milestone("phase13.m19.first_playtest_candidate", "First Playtest Candidate", (m19Complete && firstPlaytestOpen) ? "COMPLETE" : "BLOCKED", m19Complete && firstPlaytestOpen, (m19Complete && firstPlaytestOpen) ? 0 : Math.max(blockedReportCount, blockerCount), m19Complete ? "phase13-m19-completion.json" : "phase13-first-playtest-blockers.json", "Requires tester-safe package, support bundle, rollback notes, limitations, and experimental label."));
        return List.copyOf(milestones);
    }

    private static Map<String, Object> reportBackedMilestone(
            Path reportDirectory,
            String id,
            String title,
            String evidenceReport,
            String safetyReport,
            String gate
    ) {
        Map<String, Object> report = readIfPresent(reportDirectory.resolve(safetyReport));
        String reportStatus = String.valueOf(report.getOrDefault("status", "MISSING"));
        boolean complete = "PASS".equals(reportStatus);
        Map<String, Object> milestone = milestone(id, title, complete ? "COMPLETE" : "BLOCKED", complete, complete ? 0 : 1, safetyReport, gate);
        milestone.put("evidenceReport", evidenceReport);
        milestone.put("safetyReport", safetyReport);
        milestone.put("reportStatus", reportStatus);
        return milestone;
    }

    private static Map<String, Object> milestone(String id, String title, String status, boolean ready, long blockerCount, String evidenceReport, String gate) {
        Map<String, Object> milestone = new LinkedHashMap<>();
        milestone.put("id", id);
        milestone.put("title", title);
        milestone.put("status", status);
        milestone.put("ready", ready);
        milestone.put("blockerCount", blockerCount);
        milestone.put("evidenceReport", evidenceReport);
        milestone.put("gate", gate);
        milestone.put("downloadAllowed", false);
        milestone.put("nativeExtractionStarted", false);
        milestone.put("processLaunched", false);
        milestone.put("classloaderCreated", false);
        milestone.put("filesystemMutated", false);
        return milestone;
    }

    private static long countStatus(List<Map<String, Object>> milestones, String status) {
        return milestones.stream()
                .filter(milestone -> status.equals(milestone.get("status")))
                .count();
    }

    private static String firstIncompleteMilestone(List<Map<String, Object>> milestones) {
        return milestones.stream()
                .filter(milestone -> !"COMPLETE".equals(milestone.get("status")))
                .map(milestone -> String.valueOf(milestone.get("id")))
                .findFirst()
                .orElse("");
    }

    private static List<Map<String, Object>> nextActions(
            List<Map<String, Object>> handoffItems,
            List<Map<String, Object>> integrityChecks,
            boolean m17Complete,
            boolean m18Ready,
            boolean m18Complete,
            boolean firstPlaytestOpen
    ) {
        List<Map<String, Object>> actions = new ArrayList<>();
        for (Map<String, Object> item : handoffItems) {
            if (!Boolean.TRUE.equals(item.get("handoffComplete"))) {
                actions.add(action(
                        "supply." + String.valueOf(item.get("artifactId")).replace(':', '.'),
                        "M17",
                        "Supply reviewed local runtime fixture artifact",
                        String.valueOf(item.getOrDefault("operatorInstruction", "")),
                        List.of(String.valueOf(item.getOrDefault("targetFile", "")))
                ));
            }
        }
        for (Map<String, Object> check : integrityChecks) {
            if (!Boolean.TRUE.equals(check.get("integrityReady"))) {
                actions.add(action(
                        "verify-integrity." + String.valueOf(check.get("artifactId")).replace(':', '.'),
                        "M17",
                        "Record reviewed byte size and SHA-256 evidence",
                        "After the local file exists, update runtime-fixture-approvals.json with matching byteSize and sha256.",
                        List.of(String.valueOf(check.getOrDefault("expectedFixturePath", "")), "runtime-fixture-approvals.json")
                ));
            }
        }
        if (!m17Complete) {
            actions.add(action(
                    "gate.phase13.m17.closeout",
                    "M17",
                    "Rerun M17 closeout",
                    "Run the full M17 validation chain and require phase13-m17-completion.json to report phase13M17Complete=true.",
                    List.of("reports/echo-native/ashfall/phase13-m17-completion.json")
            ));
        } else if (!m18Ready) {
            actions.add(action(
                    "gate.phase13.m18.smoke-session",
                    "M18",
                    "Begin Ashfall native smoke session",
                    "Run the isolated smoke-session gate only after M17 is complete.",
                    List.of("reports/echo-native/ashfall/phase13-m18-readiness-audit.json")
            ));
        } else if (!m18Complete) {
            actions.add(action(
                    "gate.phase13.m18.closeout",
                    "M18",
                    "Complete Ashfall native smoke session",
                    "Run the isolated smoke-session gate and require phase13-m18-completion.json to report phase13M18Complete=true.",
                    List.of("reports/echo-native/ashfall/phase13-m18-completion.json")
            ));
        } else if (!firstPlaytestOpen) {
            actions.add(action(
                    "gate.phase13.m19.playtest-candidate",
                    "M19",
                    "Package first playtest candidate",
                    "Prepare tester-safe build, support bundle export, limitations, rollback instructions, and experimental native loader label.",
                    List.of("reports/echo-native/ashfall/phase13-first-playtest-blockers.json")
            ));
        }
        return actions.stream()
                .filter(action -> !"".equals(action.get("id")))
                .sorted(Comparator.comparing(action -> String.valueOf(action.get("milestone")) + "|" + action.get("id")))
                .toList();
    }

    private static Map<String, Object> action(String id, String milestone, String title, String summary, List<String> files) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", id);
        action.put("milestone", milestone);
        action.put("title", title);
        action.put("summary", summary);
        action.put("files", files);
        action.put("downloadAllowed", false);
        action.put("nativeExtractionStarted", false);
        action.put("processLaunched", false);
        action.put("classloaderCreated", false);
        action.put("filesystemMutated", false);
        return action;
    }

    private static Map<String, Object> readReport(
            Path reportPath,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-FIRST-PLAYTEST-ROADMAP-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "First playtest roadmap input report is missing",
                    "The roadmap requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(reportPath)),
                    "Generate the Phase 13 M17 report chain before planning first playtest."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static Map<String, Object> readIfPresent(Path reportPath) {
        if (!Files.isRegularFile(reportPath)) {
            return Map.of();
        }
        try {
            return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
        } catch (IOException | RuntimeException ignored) {
            return Map.of();
        }
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report.get("data"));
    }

    private static List<Map<String, Object>> objects(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> object = EchoNativeJson.asObject(item);
            if (!object.isEmpty()) {
                items.add(object);
            }
        }
        return items;
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
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
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("safeToAutoPopulate", false);
        data.put("phase", phase);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
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
