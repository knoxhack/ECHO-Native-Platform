package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EchoNativeProductActivationStatus {
    private EchoNativeProductActivationStatus() {
    }

    static Map<String, Object> productModuleActivationStatus(
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            Map<String, Object> activationMarker,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base(productPhase(packId, "module_activation_status"), diagnostics);
        Map<String, Map<String, Object>> liveModules = liveModulesById(activationMarker);
        Map<String, Map<String, Object>> nativeActivations = nativeActivationsById(activationMarker);
        List<Map<String, Object>> modules = descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> {
                    Map<String, Object> liveModule = liveModules.getOrDefault(descriptor.id(), Map.of());
                    Map<String, Object> nativeActivation = nativeActivations.getOrDefault(descriptor.id(), Map.of());
                    String loadedClassName = String.valueOf(nativeActivation.getOrDefault("loadedClassName", ""));
                    boolean nativeActivated = nativeActivationVerified(nativeActivation);
                    boolean liveGameplayVerified = nativeActivated && Boolean.TRUE.equals(liveModule.get("liveGameplayHookVerified"));
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", descriptor.id());
                    item.put("kind", descriptor.kind());
                    item.put("role", descriptor.role());
                    item.put("activationState", nativeActivated
                            ? liveGameplayVerified ? "native_product_gameplay_verified" : "native_module_class_loaded"
                            : "pending_native_runtime_bridge");
                    item.put("activeInLiveClient", nativeActivated);
                    item.put("classLoadedInLiveClient", nativeActivated);
                    item.put("liveGameplayHookVerified", liveGameplayVerified);
                    item.put("descriptorDiscovered", true);
                    item.put("entrypoint", nativeActivation.getOrDefault("entrypoint", descriptor.entrypoint()));
                    item.put("loadedClassName", loadedClassName);
                    item.put("nativeAdapterCodeExecuted", Boolean.TRUE.equals(nativeActivation.get("nativeAdapterCodeExecuted")));
                    item.put("serviceCodeExecuted", Boolean.TRUE.equals(nativeActivation.get("serviceCodeExecuted")));
                    item.put("reason", nativeActivated
                            ? "Native activation evidence loaded " + loadedClassName + " and executed its adapter entrypoint."
                            : "Native activation marker has not loaded this module class yet.");
                    return item;
                })
                .toList();
        int activeModuleCount = (int) modules.stream()
                .filter(module -> Boolean.TRUE.equals(module.get("activeInLiveClient")))
                .count();
        boolean nativeProductModulesReady = nativeProductModulesReady(activationMarker, descriptors.size());
        Map<String, Object> nativeLoaderLiveProof = EchoNativePlayableModuleGate.nativeLoaderLiveProof(activationMarker);
        data.put("activationMarkerWritten", !activationMarker.isEmpty());
        data.put("activeModuleCount", activeModuleCount);
        data.put("nativeWorldLiveHostHooksVerified", Boolean.TRUE.equals(activationMarker.get("nativeWorldLiveHostHooksVerified")));
        data.put("nativeFirstPlayableLoopReady", Boolean.TRUE.equals(activationMarker.get("nativeFirstPlayableLoopReady")));
        data.put("descriptorCount", descriptors.size());
        data.put("modules", modules);
        data.put("nativeLoaderLiveProofComplete", EchoNativePlayableModuleGate.liveRuntimeProofAccepted(activationMarker));
        data.put("nativeLoaderLiveProofStatus", nativeLoaderLiveProof.getOrDefault("status", ""));
        data.put("nativeLoaderLiveProofMissingTargets", nativeLoaderLiveProof.getOrDefault("missingTargets", List.of()));
        data.put("nativeLoaderLiveProofMutatedSurfaces", nativeLoaderLiveProof.getOrDefault("mutationLedgerMutatedSurfaces", List.of()));
        data.put("nativeLoaderLiveProofRequiredMutationSurfacesMutated",
                Boolean.TRUE.equals(nativeLoaderLiveProof.get("requiredMutationSurfacesMutated")));
        data.put("nativeLiveGameplayHandlersAttached", Boolean.TRUE.equals(activationMarker.get("nativeLiveGameplayHandlersAttached")));
        data.put("packId", packId);
        data.put("pendingBridgeCount", Math.max(0, descriptors.size() - activeModuleCount));
        data.put("playableMinecraftBaseline", true);
        data.put("nativeProductModulesReady", nativeProductModulesReady);
        data.put("playableNativeProductModules", nativeProductModulesReady);
        data.put("summary", nativeProductModulesReady
                ? "Minecraft and native product modules are active through the native client path."
                : "Minecraft is playable through the native path, but native product module playability still needs complete live mutation proof.");
        return data;
    }

    static boolean nativeActivationVerified(Map<String, Object> nativeActivation) {
        return Boolean.TRUE.equals(nativeActivation.get("activated"))
                && Boolean.TRUE.equals(nativeActivation.get("nativeAdapterCodeExecuted"))
                && !String.valueOf(nativeActivation.getOrDefault("entrypoint", "")).isBlank()
                && !String.valueOf(nativeActivation.getOrDefault("loadedClassName", "")).isBlank();
    }

    private static boolean nativeProductModulesReady(Map<String, Object> activationMarker, int descriptorCount) {
        return EchoNativePlayableModuleGate.nativeProductModulesReady(
                activationMarker,
                descriptorCount,
                nativeActivationCount(activationMarker));
    }

    private static int nativeActivationCount(Map<String, Object> activationMarker) {
        return (int) nativeActivationsById(activationMarker).values().stream()
                .filter(EchoNativeProductActivationStatus::nativeActivationVerified)
                .count();
    }

    private static Map<String, Map<String, Object>> liveModulesById(Map<String, Object> activationMarker) {
        Map<String, Map<String, Object>> modulesById = new LinkedHashMap<>();
        Object modules = activationMarker.get("modules");
        if (!(modules instanceof Iterable<?> iterable)) {
            return modulesById;
        }
        for (Object raw : iterable) {
            Map<String, Object> module = EchoNativeJson.asObject(raw);
            String id = String.valueOf(module.getOrDefault("id", ""));
            if (!id.isBlank()) {
                modulesById.put(id, module);
            }
        }
        return modulesById;
    }

    private static Map<String, Map<String, Object>> nativeActivationsById(Map<String, Object> activationMarker) {
        Map<String, Map<String, Object>> activationsById = new LinkedHashMap<>();
        Object activations = activationMarker.get("nativeActivations");
        if (!(activations instanceof Iterable<?> iterable)) {
            return activationsById;
        }
        for (Object raw : iterable) {
            Map<String, Object> activation = EchoNativeJson.asObject(raw);
            String id = String.valueOf(activation.getOrDefault("moduleId", ""));
            if (!id.isBlank()) {
                activationsById.put(id, activation);
            }
        }
        return activationsById;
    }

    private static String productPhase(String packId, String suffix) {
        return "phase13_" + phaseSegment(packId) + "_" + suffix;
    }

    private static String phaseSegment(String value) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean previousSeparator = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                result.append(ch);
                previousSeparator = false;
            } else if (!previousSeparator) {
                result.append('_');
                previousSeparator = true;
            }
        }
        String normalized = result.toString();
        while (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "native_product" : normalized;
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
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }
}
