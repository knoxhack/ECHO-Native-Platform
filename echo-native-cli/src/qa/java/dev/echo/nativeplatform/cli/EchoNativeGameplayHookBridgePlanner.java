package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
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

final class EchoNativeGameplayHookBridgePlanner {
    EchoNativeGameplayHookBridgeOutcome bridge(
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
            checkAcceptedReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, Object> m23Completion = data(reports.get("phase13-m23-completion.json"));
        Map<String, Object> m24Readiness = data(reports.get("phase13-m24-readiness.json"));
        Map<String, Object> hookEvidence = data(reports.get("native-product-gameplay-hook-evidence.json"));
        Map<String, Object> moduleHookStatus = data(reports.get("native-module-gameplay-hook-status.json"));

        boolean m23Complete = bool(m23Completion, "phase13M23Complete");
        boolean m24Ready = bool(m24Readiness, "phase13M24Ready");
        boolean baselinePlayable = bool(hookEvidence, "vanillaPlayLoopObserved") || bool(m23Completion, "minecraftBaselinePlayable");
        int markedModuleCount = intValue(moduleHookStatus, "markedModuleCount");
        int upstreamHookCount = intValue(moduleHookStatus, "gameplayHookVerifiedCount");

        Path signalPath = fixture.resolve("isolated-runtime/game/echo-native/gameplay-hooks.json").normalize();
        Map<String, Object> signalFile = Files.isRegularFile(signalPath)
                ? EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(signalPath)))
                : Map.of();
        List<String> signalModules = signalModules(signalFile);
        List<ModuleHookBridge> modules = descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> new ModuleHookBridge(
                        descriptor.id(),
                        descriptor.kind(),
                        descriptor.role(),
                        marked(descriptor.id(), moduleHookStatus),
                        signalModules.contains(descriptor.id()),
                        signalModules.contains(descriptor.id())
                                ? "gameplay_hook_signal_seen"
                                : "gameplay_hook_signal_required"
                ))
                .toList();
        int signalCount = (int) modules.stream().filter(ModuleHookBridge::gameplayHookSignalPresent).count();

        if (m23Complete && m24Ready && baselinePlayable && signalCount < descriptors.size()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-GAMEPLAY-HOOK-SIGNALS-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Gameplay hook bridge contract exists but live signals are missing",
                    "M24 defines the fixture-local gameplay hook signal contract, but the isolated runtime has not emitted module-specific gameplay hook signals yet.",
                    null,
                    packId,
                    List.of(relativePath(signalPath), "reports/echo-native/" + packId + "/gameplay-hook-signal-status.json"),
                    "Instrument the controlled native module hook path to write reviewed gameplay hook signals, then rerun this bridge."
            ));
        }

        boolean prerequisitesReady = m23Complete && m24Ready && baselinePlayable && markedModuleCount == descriptors.size();
        boolean m24Complete = prerequisitesReady && diagnostics.stream().noneMatch(EchoNativeGameplayHookBridgePlanner::isBlocking);
        boolean nativeProductPlayableReady = m24Complete && signalCount == descriptors.size();
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeGameplayHookBridgeOutcome(
                packId,
                bridgePlan(packId, signalPath, descriptors.size(), markedModuleCount, upstreamHookCount, signalCount, prerequisitesReady, sortedDiagnostics),
                signalContract(packId, signalPath, descriptors, sortedDiagnostics),
                signalStatus(packId, signalPath, modules, signalCount, sortedDiagnostics),
                moduleActivation(packId, modules, markedModuleCount, signalCount, sortedDiagnostics),
                m24Completion(packId, m24Complete, baselinePlayable, markedModuleCount, descriptors.size(), signalCount, nativeProductPlayableReady, sortedDiagnostics),
                playableBetaGate(packId, nativeProductPlayableReady, baselinePlayable, markedModuleCount, descriptors.size(), signalCount, sortedDiagnostics),
                m25Readiness(packId, m24Complete, nativeProductPlayableReady, descriptors.size() - signalCount, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> bridgePlan(
            String packId,
            Path signalPath,
            int descriptorCount,
            int markedModuleCount,
            int upstreamHookCount,
            int signalCount,
            boolean prerequisitesReady,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m24_gameplay_hook_bridge_plan", diagnostics);
        data.put("bridgeMode", "fixture_local_signal_contract");
        data.put("descriptorCount", descriptorCount);
        data.put("gameplayHookSignalCount", signalCount);
        data.put("markedModuleCount", markedModuleCount);
        data.put("packId", packId);
        data.put("prerequisitesReady", prerequisitesReady);
        data.put("signalPath", relativePath(signalPath));
        data.put("upstreamGameplayHookCount", upstreamHookCount);
        data.put("summary", prerequisitesReady
                ? "M24 defines a deterministic fixture-local gameplay hook signal bridge for the next controlled native module slice."
                : "M24 gameplay hook bridge remains blocked by missing prerequisite evidence.");
        return data;
    }

    private static Map<String, Object> signalContract(
            String packId,
            Path signalPath,
            List<EchoNativeAddonDescriptor> descriptors,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m24_gameplay_hook_signal_contract", diagnostics);
        data.put("contractSchema", "echo.native.gameplay_hooks.v1");
        data.put("packId", packId);
        data.put("requiredFields", List.of("generatedAt", "packId", "schema", "signals"));
        data.put("signalPath", relativePath(signalPath));
        data.put("signalsWriteAuthorizedByThisCommand", false);
        data.put("signalWriters", List.of("controlled_native_module_hook_bridge"));
        data.put("modules", descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", descriptor.id());
                    item.put("requiredSignal", "module_gameplay_hook_seen");
                    return item;
                })
                .toList());
        data.put("summary", "Gameplay hook evidence must come from a fixture-local signal file written by a later controlled hook bridge, not from this verifier.");
        return data;
    }

    private static Map<String, Object> signalStatus(
            String packId,
            Path signalPath,
            List<ModuleHookBridge> modules,
            int signalCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m24_gameplay_hook_signal_status", diagnostics);
        data.put("gameplayHookSignalCount", signalCount);
        data.put("moduleCount", modules.size());
        data.put("packId", packId);
        data.put("signalFilePresent", Files.isRegularFile(signalPath));
        data.put("signalPath", relativePath(signalPath));
        data.put("signalsAcceptedAsEvidence", signalCount == modules.size());
        data.put("summary", signalCount == modules.size()
                ? "All required gameplay hook signals are present."
                : "Gameplay hook signal file is not yet present or does not cover all modules.");
        return data;
    }

    private static Map<String, Object> moduleActivation(
            String packId,
            List<ModuleHookBridge> modules,
            int markedModuleCount,
            int signalCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m24_native_product_module_gameplay_activation", diagnostics);
        data.put("gameplayHookSignalCount", signalCount);
        data.put("markedModuleCount", markedModuleCount);
        data.put("moduleCount", modules.size());
        data.put("modules", modules.stream().map(EchoNativeGameplayHookBridgePlanner::moduleData).toList());
        data.put("packId", packId);
        data.put("gameplayHookSignalsAccepted", signalCount == modules.size());
        data.put("playableNativeProductModules", false);
        data.put("summary", signalCount == modules.size()
                ? "All native product modules have gameplay hook signals; live runtime host mutation proof is still required before marking the product playable."
                : "Native product modules remain marker-visible only until gameplay hook signals are emitted.");
        return data;
    }

    private static Map<String, Object> m24Completion(
            String packId,
            boolean complete,
            boolean baselinePlayable,
            int markedModuleCount,
            int descriptorCount,
            int signalCount,
            boolean nativeProductPlayableReady,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m24_gameplay_hook_bridge_completion", diagnostics);
        data.put("nativeProductPlayableReady", nativeProductPlayableReady);
        data.put("gameplayHookSignalCount", signalCount);
        data.put("markedModuleCount", markedModuleCount);
        data.put("minecraftBaselinePlayable", baselinePlayable);
        data.put("moduleCount", descriptorCount);
        data.put("packId", packId);
        data.put("phase13M24Complete", complete);
        data.put("phase13M25Ready", complete && !nativeProductPlayableReady);
        data.put("summary", complete
                ? nativeProductPlayableReady
                ? "M24 is complete: the gameplay hook signal bridge is specified and all required fixture-local signals are present."
                : "M24 is complete with warnings: the gameplay hook signal bridge is specified and checked, but gameplay hook signals still need instrumentation."
                : "M24 remains blocked until M23 evidence and marker visibility are present.");
        return data;
    }

    private static Map<String, Object> playableBetaGate(
            String packId,
            boolean nativeProductPlayableReady,
            boolean baselinePlayable,
            int markedModuleCount,
            int descriptorCount,
            int signalCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m24_native_product_playable_gate", diagnostics);
        data.put("nativeProductPlayableReady", nativeProductPlayableReady);
        data.put("gameplayHookSignalCount", signalCount);
        data.put("markedModuleCount", markedModuleCount);
        data.put("minecraftBaselinePlayable", baselinePlayable);
        data.put("moduleCount", descriptorCount);
        data.put("packId", packId);
        data.put("remainingGameplayHookCount", descriptorCount - signalCount);
        data.put("summary", nativeProductPlayableReady
                ? "Native product playability is ready through the native loader."
                : "Native product playability remains closed until module gameplay hook signals are emitted and verified.");
        return data;
    }

    private static Map<String, Object> m25Readiness(
            String packId,
            boolean m24Complete,
            boolean nativeProductPlayableReady,
            int remainingGameplayHookCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m25_readiness", diagnostics);
        data.put("nativeProductPlayableReady", nativeProductPlayableReady);
        data.put("nextCommand", "phase13 instrument gameplay-hooks <fixture>");
        data.put("nextMilestone", "phase13.m25.controlled_gameplay_hook_signal_instrumentation");
        data.put("packId", packId);
        data.put("phase13M25Ready", m24Complete && !nativeProductPlayableReady);
        data.put("remainingGameplayHookCount", remainingGameplayHookCount);
        data.put("summary", !m24Complete
                ? "M25 remains blocked until M24 gameplay hook bridge completion."
                : nativeProductPlayableReady
                ? "M25 instrumentation is already satisfied; proceed to M26 playable beta closeout."
                : "M25 may start: add controlled instrumentation that emits real module gameplay hook signals.");
        return data;
    }

    private static Map<String, Object> moduleData(ModuleHookBridge module) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activationMarkerWritten", module.activationMarkerWritten());
        data.put("gameplayHookSignalPresent", module.gameplayHookSignalPresent());
        data.put("id", module.id());
        data.put("kind", module.kind());
        data.put("liveGameplayHookVerified", module.gameplayHookSignalPresent());
        data.put("role", module.role());
        data.put("state", module.state());
        return data;
    }

    private static List<String> signalModules(Map<String, Object> signalFile) {
        Object raw = signalFile.get("signals");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> modules = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> signal = EchoNativeJson.asObject(item);
            String moduleId = String.valueOf(signal.getOrDefault("moduleId", ""));
            String signalKind = String.valueOf(signal.getOrDefault("signal", ""));
            if (!moduleId.isBlank() && "module_gameplay_hook_seen".equals(signalKind)) {
                modules.add(moduleId);
            }
        }
        modules.sort(String::compareTo);
        return List.copyOf(modules);
    }

    private static boolean marked(String id, Map<String, Object> moduleHookStatus) {
        Object raw = moduleHookStatus.get("modules");
        if (!(raw instanceof List<?> list)) {
            return false;
        }
        for (Object item : list) {
            Map<String, Object> module = EchoNativeJson.asObject(item);
            if (id.equals(String.valueOf(module.getOrDefault("id", "")))
                    && Boolean.TRUE.equals(module.get("activationMarkerWritten"))) {
                return true;
            }
        }
        return false;
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
                    "ECHO-NATIVE-GAMEPLAY-HOOK-BRIDGE-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook bridge required report missing",
                    "M24 requires " + reportName + " before the bridge can be evaluated.",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Regenerate M23 gameplay hook evidence reports before running phase13 bridge gameplay-hooks."
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
                    "ECHO-NATIVE-GAMEPLAY-HOOK-BRIDGE-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook bridge upstream report is not accepted",
                    "M24 requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream M23 diagnostics before bridging gameplay hook signals."
            ));
        }
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean bool(Map<String, Object> data, String key) {
        return Boolean.TRUE.equals(data.get(key));
    }

    private static int intValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : 0;
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
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("libraryDownloadStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("signalsWrittenByThisCommand", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record ModuleHookBridge(
            String id,
            String kind,
            String role,
            boolean activationMarkerWritten,
            boolean gameplayHookSignalPresent,
            String state
    ) {
    }
}
