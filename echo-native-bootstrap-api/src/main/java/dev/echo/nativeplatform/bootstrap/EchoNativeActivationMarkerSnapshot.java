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
        marker.put("nativeActivations", nativeActivations.values());
        marker.put("modules", modules.stream()
                .sorted(String::compareTo)
                .map(moduleId -> NativeLoaderActivationModuleSnapshot.module(
                        moduleId,
                        nativeActivations.get(moduleId),
                        registeredModuleItems.get(moduleId),
                        Boolean.TRUE.equals(registryBridge.get("creativeContentVisible")),
                        productGameplayBridge
                ))
                .toList());
        NativeLoaderJsonSupport.writeAtomically(markerPath, marker);
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
