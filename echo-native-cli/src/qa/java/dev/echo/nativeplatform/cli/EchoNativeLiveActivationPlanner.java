package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
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

final class EchoNativeLiveActivationPlanner {
    EchoNativeLiveActivationPlanOutcome plan(
            String packId,
            Path fixture,
            List<EchoNativeAddonDescriptor> descriptors,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, Object> m21 = data(reports.get("phase13-m21-readiness.json"));
        Map<String, Object> baseline = data(reports.get("minecraft-baseline-playability.json"));
        Map<String, Object> testerProcess = data(reports.get("tester-launch-process.json"));
        Map<String, Object> bootstrap = data(reports.get("native-module-bootstrap-status.json"));

        boolean m21Ready = bool(m21, "phase13M21Ready") || bool(m21, "moduleBridgeReadyToStart");
        boolean baselinePlayable = bool(baseline, "minecraftBaselinePlayable") || bool(baseline, "baselinePlayable");
        boolean testerProcessStarted = bool(testerProcess, "minecraftProcessStarted") || bool(testerProcess, "processLaunched");
        long descriptorCount = number(bootstrap.get("descriptorCount"), descriptors.size());
        long bootstrapVisibleCount = number(bootstrap.get("bootstrapVisibleCount"), 0);
        long nativeBootstrapVisibleCount = number(bootstrap.get("nativeBootstrapVisibleCount"), 0);
        List<String> requiredNativeModules = requiredNativeActivationModules(packId, descriptors);
        List<String> visibleRequiredNativeModules = visibleRequiredNativeModules(bootstrap, requiredNativeModules);
        boolean requiredNativeModulesVisible = !requiredNativeModules.isEmpty()
                && visibleRequiredNativeModules.size() == requiredNativeModules.size();
        boolean ready = m21Ready
                && baselinePlayable
                && testerProcessStarted
                && requiredNativeModulesVisible
                && diagnostics.stream().noneMatch(EchoNativeLiveActivationPlanner::isBlocking);

        if (!ready && diagnostics.stream().noneMatch(EchoNativeLiveActivationPlanner::isBlocking)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIVE-ACTIVATION-PLAN-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                "Live activation planning is missing prerequisite evidence",
                    "The M22 live activation plan requires M21 readiness, baseline playability, tester process evidence, and bootstrap-visible required native module evidence.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/phase13-m21-readiness.json"),
                    "Regenerate tester evidence and phase13 bridge modules before planning the live activation wrapper."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeLiveActivationPlanOutcome(
                packId,
                wrapperPlan(packId, descriptors, bootstrapVisibleCount, nativeBootstrapVisibleCount, requiredNativeModules, visibleRequiredNativeModules, testerProcessStarted, ready, sortedDiagnostics),
                safetyGate(packId, ready, requiredNativeModules, visibleRequiredNativeModules, sortedDiagnostics),
                markerContract(packId, descriptors, ready, sortedDiagnostics),
                m22Readiness(packId, ready, requiredNativeModules, visibleRequiredNativeModules, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> wrapperPlan(
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            long bootstrapVisibleCount,
            long nativeBootstrapVisibleCount,
            List<String> requiredNativeModules,
            List<String> visibleRequiredNativeModules,
            boolean testerProcessStarted,
            boolean ready,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m22_native_live_activation_wrapper_plan", diagnostics);
        data.put("activationPlanReady", ready);
        data.put("bootstrapVisibleCount", bootstrapVisibleCount);
        data.put("descriptorCount", descriptors.size());
        data.put("nativeBootstrapVisibleCount", nativeBootstrapVisibleCount);
        data.put("plannedBootstrapMainClass", "dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain");
        data.put("plannedHandoffMode", "reflective_main_handoff_after_marker_write");
        data.put("plannedRealMainSource", "fixture-local version manifest");
        data.put("plannedReports", List.of(
                "native-live-activation-wrapper-plan.json",
                "native-live-activation-safety-gate.json",
                "native-live-activation-marker-contract.json",
                "phase13-m22-readiness.json"
        ));
        data.put("requiredNativeModuleCount", requiredNativeModules.size());
        data.put("requiredNativeModules", requiredNativeModules);
        data.put("testerProcessEvidencePresent", testerProcessStarted);
        data.put("visibleRequiredNativeModuleCount", visibleRequiredNativeModules.size());
        data.put("visibleRequiredNativeModules", visibleRequiredNativeModules);
        data.put("wrapperImplementationStarted", false);
        data.put("summary", ready
                ? "M22 may implement an authorized bootstrap wrapper that writes live activation markers before game main handoff."
                : "M22 live activation wrapper planning is blocked by missing required native module visibility or launch evidence.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> safetyGate(
            String packId,
            boolean ready,
            List<String> requiredNativeModules,
            List<String> visibleRequiredNativeModules,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m22_native_live_activation_safety_gate", diagnostics);
        data.put("activationPlanningReady", ready);
        data.put("authorizedCommandRequired", true);
        data.put("bootstrapWrapperAllowedNext", ready);
        data.put("descriptorMarkersOnly", true);
        data.put("directAddonExecutionAllowed", false);
        data.put("gameMainHandoffAllowedOnlyAfterMarkerWrite", ready);
        data.put("registryMutationAllowed", false);
        data.put("requiredNativeModules", requiredNativeModules);
        data.put("transformMutationAllowed", false);
        data.put("userInstallMutationAllowed", false);
        data.put("visibleRequiredNativeModules", visibleRequiredNativeModules);
        data.put("summary", ready
                ? "Safety gate allows the next slice to add a bounded bootstrap wrapper, not transforms or registry mutation."
                : "Safety gate blocks live activation wrapper implementation until prerequisite reports pass.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> markerContract(
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            boolean ready,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<Map<String, Object>> modules = descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", descriptor.id());
                    item.put("entrypoint", String.valueOf(descriptor.access().getOrDefault("nativeEntrypoint", descriptor.entrypoint())));
                    item.put("expectedMarkerState", "activation_marker_pending");
                    item.put("kind", descriptor.kind());
                    item.put("role", descriptor.role());
                    return item;
                })
                .toList();
        Map<String, Object> data = base("phase13_m22_native_live_activation_marker_contract", diagnostics);
        data.put("markerContractReady", ready);
        data.put("markerPath", "isolated-runtime/game/echo-native/module-activation.json");
        data.put("moduleCount", modules.size());
        data.put("modules", modules);
        data.put("requiredMarkerFields", List.of("packId", "generatedAt", "bootstrapMain", "realMainClassSource", "modules"));
        data.put("summary", "Live activation markers will prove bootstrap visibility inside the tester process without claiming registry-backed gameplay hooks.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> m22Readiness(
            String packId,
            boolean ready,
            List<String> requiredNativeModules,
            List<String> visibleRequiredNativeModules,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m22_readiness", diagnostics);
        data.put("phase13M22Ready", ready);
        data.put("phase13M22Complete", false);
        data.put("nativeProductPlayableReady", false);
        data.put("nextCommand", "phase13 activate bootstrap --authorized <fixture>");
        data.put("nextMilestone", "phase13.m22.authorized_bootstrap_activation_marker");
        data.put("requiredNativeModuleCount", requiredNativeModules.size());
        data.put("requiredNativeModules", requiredNativeModules);
        data.put("visibleRequiredNativeModuleCount", visibleRequiredNativeModules.size());
        data.put("visibleRequiredNativeModules", visibleRequiredNativeModules);
        data.put("summary", ready
                ? "Next slice may add the authorized bootstrap activation marker wrapper."
                : "M22 remains blocked until native launch evidence and required native module bootstrap evidence pass.");
        data.put("packId", packId);
        return data;
    }

    private static List<String> requiredNativeActivationModules(String packId, List<EchoNativeAddonDescriptor> descriptors) {
        List<String> ids = descriptors.stream().map(EchoNativeAddonDescriptor::id).sorted().toList();
        List<String> bootstrapProfileModules = descriptors.stream()
                .filter(descriptor -> hasNativeBootstrapProfile(descriptor.access()))
                .map(EchoNativeAddonDescriptor::id)
                .sorted()
                .toList();
        if (!bootstrapProfileModules.isEmpty()) {
            return bootstrapProfileModules;
        }
        if (ids.contains(packId)) {
            return List.of(packId);
        }
        String normalized = "echo" + packId.replace("_", "").replace("-", "");
        for (String id : ids) {
            if (id.equals(normalized) || id.startsWith(normalized)) {
                return List.of(id);
            }
        }
        return ids.isEmpty() ? List.of() : List.of(ids.getFirst());
    }

    private static boolean hasNativeBootstrapProfile(Map<String, Object> access) {
        Object value = access == null ? null : access.get("nativeBootstrapProfile");
        return value != null && !String.valueOf(value).isBlank();
    }

    private static List<String> visibleRequiredNativeModules(Map<String, Object> bootstrap, List<String> requiredNativeModules) {
        Object rawModules = bootstrap.get("modules");
        if (!(rawModules instanceof List<?> modules)) {
            return List.of();
        }
        List<String> visible = new ArrayList<>();
        for (Object raw : modules) {
            Map<String, Object> module = EchoNativeJson.asObject(raw);
            String id = String.valueOf(module.getOrDefault("id", ""));
            if (requiredNativeModules.contains(id) && Boolean.TRUE.equals(module.get("nativeEntrypointClassPresent"))) {
                visible.add(id);
            }
        }
        visible.sort(String::compareTo);
        return List.copyOf(visible);
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
                    "ECHO-NATIVE-LIVE-ACTIVATION-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Live activation planning required report missing",
                    "Live activation planning requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Regenerate Phase 13 tester evidence and module bridge reports before planning live activation."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkReport(
            String reportName,
            Path path,
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
                    "ECHO-NATIVE-LIVE-ACTIVATION-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Live activation planning upstream report is not PASS",
                    "Live activation planning requires PASS or accepted PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Resolve upstream tester evidence and module bridge reports before activation planning."
            ));
        }
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
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("libraryDownloadStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean bool(Map<String, Object> data, String key) {
        return Boolean.TRUE.equals(data.get(key));
    }

    private static long number(Object value, long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
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
