package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreMarkerFields;
import dev.echo.nativeplatform.loader.NativeLoaderAgent7LiveHookEvidence;
import dev.echo.nativeplatform.loader.NativeLoaderActivationModuleSnapshot;
import dev.echo.nativeplatform.loader.NativeLoaderClientUiHost;
import dev.echo.nativeplatform.loader.NativeLoaderClientWindowPump;
import dev.echo.nativeplatform.loader.NativeLoaderLifecycleEventHost;
import dev.echo.nativeplatform.loader.NativeLoaderLiveProofService;
import dev.echo.nativeplatform.loader.NativeLoaderLiveProofSidecar;
import dev.echo.nativeplatform.loader.NativeLoaderProductGameplayMarkerFields;
import dev.echo.nativeplatform.loader.NativeLoaderRegistryMarkerFields;
import dev.echo.nativeplatform.loader.NativeLoaderResourceHost;
import dev.echo.nativeplatform.loader.NativeLoaderServiceBridge;
import dev.echo.nativeplatform.loader.NativeLoaderTransformMarkerFields;
import dev.echo.nativeplatform.loader.NativeLoaderJsonSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

final class EchoNativeActivationMarkerSnapshot {
    private static final List<String> COMPACT_ACTIVATION_KEYS = List.of(
            "moduleId",
            "id",
            "activated",
            "nativeActivationLoaded",
            "nativeModuleActivated",
            "activationClaimAllowed",
            "activationClaimBlocker",
            "activationStage",
            "attempted",
            "entrypoint",
            "loadedClassName",
            "loadedClassLoader",
            "loadedByModuleClassLoader",
            "nativeAdapterCodeExecuted",
            "serviceCodeExecuted",
            "addonServiceCodeExecuted",
            "nativeLoaderLifecycleAttempted",
            "nativeLoaderLifecycleFallback",
            "nativeHostMutationClaimAllowed",
            "nativeHostMutationClaimBlocker",
            "gameplayReadyClaimAllowed",
            "gameplayReadyClaimBlocker",
            "status",
            "phaseLifecycleFailed",
            "phaseLifecycleError",
            "phaseLifecycleErrorClass",
            "registeredServiceCount",
            "registeredContentCount",
            "registeredFeatureContracts",
            "adapterDomains",
            "runtimeTargets",
            "lifecyclePhaseHistory",
            "loadedModuleStateWritten",
            "loadedModuleStatePath",
            "typedHostMutationReceiptCount",
            "typedMutationReceiptCount",
            "diagnostics"
    );

    private static final List<String> HEAVY_ACTIVATION_KEYS = List.of(
            "registeredServices",
            "registeredContent",
            "nativeLifecycleDispatch",
            "lifecycleBridge",
            "serviceBridge",
            "registryBridge",
            "eventBridge",
            "nativeLoaderLifecycleEventHost",
            "loadedModuleState",
            "nativeLoadedModuleState",
            "classpath"
    );

    private static final List<String> HEAVY_MODULE_KEYS = List.of(
            "registeredServices",
            "registeredContent",
            "nativeLifecycleDispatch",
            "lifecycleBridge",
            "serviceBridge",
            "registryBridge",
            "eventBridge",
            "loadedModuleState",
            "nativeLoadedModuleState"
    );

    private EchoNativeActivationMarkerSnapshot() {
    }

    static void write(
            Context context,
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints,
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations
    ) throws IOException {
        Map<String, Object> resourceBridge = object(runtimeBridge.get("resourceBridge"));
        Map<String, Object> registryBridge = object(runtimeBridge.get("registryBridge"));
        Map<String, String> registeredModuleItems = registeredModuleItems(registryBridge.get("registeredModuleItems"));
        Map<String, Object> marker = new LinkedHashMap<>();
        marker.put("schema", "echo.native.live_activation_marker.v1");
        marker.put("generatedAt", "1970-01-01T00:00:00Z");
        marker.put("bootstrapMain", context.bootstrapMain());
        marker.put("packId", packId);
        Map<String, Object> liveClientProbe = object(runtimeBridge.get("liveClientProbe"));
        boolean preservedLiveHandoffEvidence = realMainClass.isBlank()
                && Boolean.TRUE.equals(liveClientProbe.get("preservedExistingLiveEvidence"))
                && Boolean.TRUE.equals(liveClientProbe.get("executed"));
        marker.put("realMainClassSource", realMainClass.isBlank()
                ? preservedLiveHandoffEvidence ? "preserved_live_handoff_evidence" : "not_handed_off"
                : "authorized_argument");
        marker.put("handoffRequested", !realMainClass.isBlank() || preservedLiveHandoffEvidence);
        marker.put("preservedLiveHandoffEvidence", preservedLiveHandoffEvidence);
        marker.put("classloaderCreated", false);
        Map<String, Object> transformBridge = object(runtimeBridge.get("transformBridge"));
        marker.putAll(NativeLoaderTransformMarkerFields.markerFields(transformBridge));
        marker.putAll(NativeLoaderAdapterCoreMarkerFields.markerFields(runtimeBridge));
        marker.putAll(NativeLoaderRegistryMarkerFields.markerFields(registryBridge, registeredModuleItems.size()));
        marker.putAll(NativeLoaderResourceHost.markerFields(resourceBridge));
        Map<String, Object> lifecycleBridge = object(runtimeBridge.get("lifecycleBridge"));
        Map<String, Object> eventBridge = object(runtimeBridge.get("eventBridge"));
        Map<String, Object> serviceBridge = object(runtimeBridge.get("serviceBridge"));
        marker.putAll(NativeLoaderLifecycleEventHost.markerFields(lifecycleBridge, eventBridge));
        marker.putAll(NativeLoaderServiceBridge.markerFields(serviceBridge));
        marker.putAll(NativeLoaderClientWindowPump.liveClientProbeMarkerFields(
                liveClientProbe,
                context.nativeLoaderClientLabel().get()
        ));
        Map<String, Object> nativeClientUiBridge = object(runtimeBridge.get("nativeClientUiBridge"));
        marker.putAll(NativeLoaderClientUiHost.markerFields(nativeClientUiBridge));
        Map<String, Object> productGameplayBridge = NativeLoaderAgent7LiveHookEvidence.applyExactWorldHookEvidence(
                object(runtimeBridge.get(context.productGameplayBridgeKey())),
                markerPath,
                context.agent7DirectEvidencePathProperty(),
                context.nativeModuleClassLoader().get(),
                NativeLoaderJsonSupport::parse
        );
        Path agent7DirectLiveHookEvidencePath =
                NativeLoaderAgent7LiveHookEvidence.directEvidencePath(context.agent7DirectEvidencePathProperty());
        productGameplayBridge.put("agent7DirectLiveHookEvidencePath", agent7DirectLiveHookEvidencePath.toString());
        productGameplayBridge.put("agent7DirectLiveHookEvidencePresent", Files.isRegularFile(agent7DirectLiveHookEvidencePath));
        runtimeBridge.put(context.productGameplayBridgeKey(), productGameplayBridge);
        Map<String, Object> nativeLoaderLiveProof = context.nativeLoaderLiveProof().create(
                realMainClass,
                liveClientProbe,
                nativeClientUiBridge,
                productGameplayBridge,
                serviceBridge,
                nativeActivations
        );
        Path nativeLoaderLiveProofPath = NativeLoaderLiveProofSidecar.proofPath(markerPath);
        nativeLoaderLiveProof = context.writeNativeLoaderLiveProof().write(markerPath, nativeLoaderLiveProof);
        runtimeBridge.put("nativeLoaderLiveProof", nativeLoaderLiveProof);
        marker.putAll(NativeLoaderLiveProofService.liveProofMarkerFields(
                nativeLoaderLiveProofPath.toString(),
                nativeLoaderLiveProof
        ));
        marker.putAll(NativeLoaderProductGameplayMarkerFields.markerFields(
                productGameplayBridge,
                agent7DirectLiveHookEvidencePath.toString(),
                Files.isRegularFile(agent7DirectLiveHookEvidencePath),
                integer(registryBridge.get("registeredCreativeTabCount")) > 0
        ));
        marker.put("runtimeBridge", runtimeBridge);
        marker.put("adapterCore", context.adapterCoreProbe().get());
        marker.put("nativeEntrypointCount", nativeEntrypoints.size());
        marker.put("nativeActivationCount", nativeActivations.values().stream()
                .filter(NativeLoaderActivationModuleSnapshot::nativeActivationLoaded)
                .count());
        marker.put("nativeActivationMarkerCompacted", true);
        marker.put("nativeActivationInlineMode", "summary");
        marker.put("nativeActivationDetailSidecars", true);
        marker.put("nativeActivationDetailDirectory", markerPath.getParent().resolve("loaded-modules").toString());
        marker.put("nativeActivations", nativeActivations.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> compactNativeActivation(entry.getKey(), entry.getValue()))
                .toList());
        marker.put("modules", modules.stream()
                .sorted(String::compareTo)
                .map(moduleId -> NativeLoaderActivationModuleSnapshot.module(
                        moduleId,
                        nativeActivations.get(moduleId),
                        registeredModuleItems.get(moduleId),
                        Boolean.TRUE.equals(registryBridge.get("creativeContentVisible")),
                        productGameplayBridge
                ))
                .map(EchoNativeActivationMarkerSnapshot::compactModuleSnapshot)
                .toList());
        NativeLoaderJsonSupport.writeAtomically(markerPath, marker);
    }

    private static Map<String, Object> compactNativeActivation(String moduleId, Map<String, Object> activation) {
        Map<String, Object> source = activation == null ? Map.of() : activation;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleId", !moduleId.isBlank()
                ? moduleId
                : String.valueOf(source.getOrDefault("moduleId", source.getOrDefault("id", ""))));
        summary.put("activationDetailInlineMode", "summary");
        for (String key : COMPACT_ACTIVATION_KEYS) {
            if (source.containsKey(key)) {
                summary.put(key, source.get(key));
            }
        }
        for (String key : HEAVY_ACTIVATION_KEYS) {
            Object value = source.get(key);
            int count = count(value);
            if (count >= 0) {
                summary.put(key + "Count", count);
            }
        }
        Object sidecarPath = source.get("loadedModuleStatePath");
        if (sidecarPath != null) {
            summary.put("activationDetailSidecarPath", sidecarPath);
        }
        if (!source.isEmpty()) {
            summary.put("nativeActivationLoaded", NativeLoaderActivationModuleSnapshot.nativeActivationLoaded(source));
        }
        return summary;
    }

    private static Map<String, Object> compactModuleSnapshot(Map<String, Object> module) {
        Map<String, Object> summary = new LinkedHashMap<>(module);
        summary.put("activationDetailInlineMode", "summary");
        for (String key : HEAVY_MODULE_KEYS) {
            Object removed = summary.remove(key);
            int count = count(removed);
            if (count >= 0) {
                summary.put(key + "Count", count);
            }
        }
        Object sidecarPath = summary.get("loadedModuleStatePath");
        if (sidecarPath != null) {
            summary.put("activationDetailSidecarPath", sidecarPath);
        }
        return summary;
    }

    private static Map<String, String> registeredModuleItems(Object value) {
        Map<String, String> items = new TreeMap<>();
        if (!(value instanceof Iterable<?> iterable)) {
            return items;
        }
        for (Object item : iterable) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object moduleId = map.get("moduleId");
            Object itemId = map.get("itemId");
            if (moduleId != null && itemId != null) {
                items.put(String.valueOf(moduleId), String.valueOf(itemId));
            }
        }
        return items;
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static int count(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        if (value instanceof Iterable<?> iterable) {
            int count = 0;
            for (Object ignored : iterable) {
                count++;
            }
            return count;
        }
        return -1;
    }

    record Context(
            String bootstrapMain,
            String productGameplayBridgeKey,
            String agent7DirectEvidencePathProperty,
            Supplier<String> nativeLoaderClientLabel,
            Supplier<ClassLoader> nativeModuleClassLoader,
            NativeLoaderLiveProofFactory nativeLoaderLiveProof,
            NativeLoaderLiveProofWriter writeNativeLoaderLiveProof,
            Supplier<Map<String, Object>> adapterCoreProbe
    ) {
    }

    interface NativeLoaderLiveProofFactory {
        Map<String, Object> create(
                String realMainClass,
                Map<String, Object> liveClientProbe,
                Map<String, Object> nativeClientUiBridge,
                Map<String, Object> productGameplayBridge,
                Map<String, Object> serviceBridge,
                Map<String, Map<String, Object>> nativeActivations
        );
    }

    interface NativeLoaderLiveProofWriter {
        Map<String, Object> write(Path markerPath, Map<String, Object> proof) throws IOException;
    }
}
