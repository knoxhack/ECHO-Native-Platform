package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeAddonRuntimeDescriptorSnapshot;
import dev.echo.nativeplatform.contracts.EchoNativeAddonRuntimeDiscoveryPlan;
import dev.echo.nativeplatform.contracts.EchoNativeAddonRuntimeDiscoverySafetyStatus;
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

final class EchoNativeAddonRuntimeDiscoveryPlanner {
    EchoNativeAddonRuntimeDiscoveryOutcome discover(
            String packId,
            Path fixture,
            List<EchoNativeAddonDescriptor> descriptors,
            Path dummyProcessResultPath,
            Path dummyProcessCrashBoundaryPath,
            Path dummyProcessOutputCapturePath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> dummyResult = readRequiredReport(dummyProcessResultPath, fixture, packId, "ECHO-NATIVE-DUMMY-PROCESS-RESULT-MISSING", "Controlled dummy process result missing", diagnostics);
        Map<String, Object> crashBoundary = readRequiredReport(dummyProcessCrashBoundaryPath, fixture, packId, "ECHO-NATIVE-DUMMY-CRASH-BOUNDARY-MISSING", "Dummy process crash boundary missing", diagnostics);
        Map<String, Object> outputCapture = readRequiredReport(dummyProcessOutputCapturePath, fixture, packId, "ECHO-NATIVE-DUMMY-OUTPUT-CAPTURE-MISSING", "Dummy process output capture missing", diagnostics);

        checkDummyProcessResult(dummyResult, EchoNativeJson.asObject(dummyResult.get("data")), dummyProcessResultPath, packId, diagnostics);
        checkDummyCrashBoundary(crashBoundary, EchoNativeJson.asObject(crashBoundary.get("data")), dummyProcessCrashBoundaryPath, packId, diagnostics);
        checkDummyOutputCapture(outputCapture, EchoNativeJson.asObject(outputCapture.get("data")), dummyProcessOutputCapturePath, packId, diagnostics);

        List<EchoNativeAddonDescriptor> orderedDescriptors = descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .toList();
        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();
        List<String> requiredInputs = List.of(
                "controlled-dummy-process-result.json",
                "dummy-process-crash-boundary.json",
                "dummy-process-output-capture.json"
        );
        EchoNativeAddonRuntimeDiscoveryPlan plan = new EchoNativeAddonRuntimeDiscoveryPlan(
                "phase13.m8.addon_runtime_discovery.plan",
                ready,
                true,
                true,
                false,
                false,
                false,
                false,
                ready ? requiredInputs : List.of(),
                ready ? List.of("fixtures/" + fixture.getFileName() + "/modules/**/META-INF/echo.mod.json") : List.of()
        );
        EchoNativeAddonRuntimeDescriptorSnapshot snapshot = new EchoNativeAddonRuntimeDescriptorSnapshot(
                "phase13.m8.addon_runtime_descriptor.snapshot",
                ready,
                true,
                true,
                ready ? orderedDescriptors.size() : 0,
                ready ? orderedDescriptors.stream().map(EchoNativeAddonDescriptor::id).toList() : List.of(),
                ready ? orderedDescriptors.stream().map(descriptor -> descriptorData(fixture, descriptor)).toList() : List.of()
        );
        EchoNativeAddonRuntimeDiscoverySafetyStatus safetyStatus = new EchoNativeAddonRuntimeDiscoverySafetyStatus(
                "phase13.m8.addon_runtime_discovery.safety.status",
                ready,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                ready ? List.of("m7_dummy_process_boundary_pass", "descriptor_snapshot_order_stable", "descriptor_data_only") : List.of()
        );

        return new EchoNativeAddonRuntimeDiscoveryOutcome(
                packId,
                addonRuntimeDiscoveryPlan(packId, plan, diagnostics),
                addonRuntimeDescriptors(packId, snapshot, diagnostics),
                addonRuntimeDiscoverySafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static void checkDummyProcessResult(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        boolean pass = "PASS".equals(report.get("status"));
        boolean dummyProcessOnly = Boolean.TRUE.equals(data.get("dummyProcessOnly"));
        boolean dummyProcessLaunched = Boolean.TRUE.equals(data.get("dummyProcessLaunched"));
        boolean gameProcessLaunched = Boolean.TRUE.equals(data.get("gameProcessLaunched"));
        boolean minecraftLaunched = Boolean.TRUE.equals(data.get("minecraftLaunched"));
        if (!pass || !dummyProcessOnly || !dummyProcessLaunched || gameProcessLaunched || minecraftLaunched || hasUnsafeGameWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-DUMMY-PROCESS-RESULT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Controlled dummy process result is not safe for addon runtime discovery",
                    "Addon runtime discovery requires a PASS dummy process result with dummyProcessOnly=true and no Minecraft/game runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "This legacy discovery gate depends on retired QA-only dummy-process reports; use echo-native launch for the current product loader path."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkDummyCrashBoundary(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        boolean pass = "PASS".equals(report.get("status"));
        boolean verified = Boolean.TRUE.equals(data.get("verified"));
        boolean crashContained = Boolean.TRUE.equals(data.get("crashContained"));
        boolean timeoutContained = Boolean.TRUE.equals(data.get("timeoutContained"));
        if (!pass || !verified || !crashContained || !timeoutContained || hasUnsafeGameWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-DUMMY-CRASH-BOUNDARY-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Dummy process crash boundary is not ready for addon runtime discovery",
                    "Addon runtime discovery requires PASS dummy-process-crash-boundary.json with contained process failures.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate the M7 dummy process boundary before discovering runtime descriptors."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkDummyOutputCapture(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        boolean pass = "PASS".equals(report.get("status"));
        boolean captured = Boolean.TRUE.equals(data.get("captured"));
        boolean deterministic = Boolean.TRUE.equals(data.get("deterministic"));
        boolean secretSafe = Boolean.TRUE.equals(data.get("secretSafe"));
        if (!pass || !captured || !deterministic || !secretSafe || hasUnsafeGameWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-DUMMY-OUTPUT-CAPTURE-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Dummy process output capture is not ready for addon runtime discovery",
                    "Addon runtime discovery requires PASS dummy-process-output-capture.json with deterministic, secret-safe output.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate the M7 dummy process output capture before discovering runtime descriptors."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> addonRuntimeDiscoveryPlan(
            String packId,
            EchoNativeAddonRuntimeDiscoveryPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m8_addon_runtime_discovery_plan", diagnostics);
        data.put("addonCodeExecuted", plan.addonCodeExecuted());
        data.put("classloaderCreated", plan.classloaderCreated());
        data.put("dataOnly", plan.dataOnly());
        data.put("deterministicOrder", plan.deterministicOrder());
        data.put("discoveryRoots", plan.discoveryRoots());
        data.put("filesystemMutated", plan.filesystemMutated());
        data.put("gameClassesResolved", plan.gameClassesResolved());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("ready", plan.ready());
        data.put("requiredInputs", plan.requiredInputs());
        data.put("summary", plan.ready()
                ? "Addon runtime descriptor discovery is ready as deterministic descriptor data only."
                : "Addon runtime descriptor discovery is blocked by M7 dummy process boundary diagnostics.");
        return data;
    }

    private static Map<String, Object> addonRuntimeDescriptors(
            String packId,
            EchoNativeAddonRuntimeDescriptorSnapshot snapshot,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m8_addon_runtime_descriptors", diagnostics);
        data.put("complete", snapshot.complete());
        data.put("descriptorCount", snapshot.descriptorCount());
        data.put("descriptorDataOnly", snapshot.descriptorDataOnly());
        data.put("descriptors", snapshot.descriptors());
        data.put("deterministicOrder", snapshot.deterministicOrder());
        data.put("orderedModuleIds", snapshot.orderedModuleIds());
        data.put("packId", packId);
        data.put("snapshotId", snapshot.snapshotId());
        data.put("summary", snapshot.complete()
                ? "All discovered addon descriptors were mirrored into a deterministic runtime discovery snapshot without executing addon code."
                : "Addon runtime descriptor snapshot is blocked by upstream diagnostics.");
        return data;
    }

    private static Map<String, Object> addonRuntimeDiscoverySafetyStatus(
            String packId,
            EchoNativeAddonRuntimeDiscoverySafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m8_addon_runtime_discovery_safety_status", diagnostics);
        data.put("addonCodeExecuted", status.addonCodeExecuted());
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("dataOnly", status.dataOnly());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameProcessLaunched", status.gameProcessLaunched());
        data.put("minecraftLaunched", status.minecraftLaunched());
        data.put("packId", packId);
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("resolvesRuntimeClasses", status.resolvesRuntimeClasses());
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "M8 addon runtime discovery remained data-only and safe to continue."
                : "M8 addon runtime discovery is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> descriptorData(Path fixture, EchoNativeAddonDescriptor descriptor) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("access", descriptor.access());
        data.put("addonCodeExecuted", false);
        data.put("apiStability", descriptor.apiStability().name());
        data.put("classloaderCreated", false);
        data.put("consumes", descriptor.consumes());
        data.put("descriptorPath", relativeDescriptorPath(fixture, descriptor.descriptorPath()));
        data.put("gameClassesResolved", false);
        data.put("id", descriptor.id());
        data.put("kind", descriptor.kind());
        data.put("name", descriptor.name());
        data.put("official", descriptor.official());
        data.put("optional", descriptor.optional());
        data.put("provides", descriptor.provides());
        data.put("requires", descriptor.requires());
        data.put("role", descriptor.role());
        data.put("schema", descriptor.schema());
        data.put("side", descriptor.side().name());
        data.put("standalone", descriptor.standalone());
        data.put("transforms", descriptor.transforms());
        data.put("trustLevel", descriptor.trustLevel().name());
        data.put("version", descriptor.version());
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("gameProcessLaunched", false);
        data.put("minecraftLaunched", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeGameWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String missingCode,
            String missingTitle,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    missingCode,
                    EchoNativeIssueSeverity.ERROR,
                    missingTitle,
                    "Required M8 addon runtime discovery input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate the M7 controlled dummy process boundary reports before addon runtime discovery."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> EchoNativeJson.asObject(item))
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("code")) + ":" + item.get("summary")))
                .map(item -> new EchoNativeDiagnostic(
                        String.valueOf(item.getOrDefault("code", "ECHO-NATIVE-UPSTREAM-DIAGNOSTIC")),
                        EchoNativeIssueSeverity.ERROR,
                        String.valueOf(item.getOrDefault("title", "Upstream diagnostic")),
                        String.valueOf(item.getOrDefault("summary", "Upstream Phase 13 report is not PASS.")),
                        item.get("moduleId") == null ? null : String.valueOf(item.get("moduleId")),
                        packId,
                        EchoNativeJson.stringList(item.get("likelyFiles")),
                        String.valueOf(item.getOrDefault("suggestedFix", "Resolve upstream diagnostics first."))
                ))
                .toList();
    }

    private static List<EchoNativeDiagnostic> unique(List<EchoNativeDiagnostic> diagnostics) {
        Map<String, EchoNativeDiagnostic> byKey = new LinkedHashMap<>();
        for (EchoNativeDiagnostic diagnostic : diagnostics) {
            byKey.put(diagnostic.code() + "|" + diagnostic.moduleId() + "|" + diagnostic.summary(), diagnostic);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
    }

    private static String relativeDescriptorPath(Path fixture, Path descriptorPath) {
        try {
            Path root = Path.of("").toAbsolutePath().normalize();
            Path normalized = descriptorPath.toAbsolutePath().normalize();
            if (normalized.startsWith(root)) {
                return root.relativize(normalized).toString().replace('\\', '/');
            }
            Path workspaceRoot = root.getParent();
            if (workspaceRoot != null && normalized.startsWith(workspaceRoot)) {
                return workspaceRoot.relativize(normalized).toString().replace('\\', '/');
            }
            return fixture.relativize(descriptorPath).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            Path fileName = descriptorPath.getFileName();
            return fileName == null ? "" : fileName.toString().replace('\\', '/');
        }
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
