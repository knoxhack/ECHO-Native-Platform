package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderRuntimeBridgeEnricher {
    private NativeLoaderRuntimeBridgeEnricher() {
    }

    public static Map<String, Object> enrich(
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations,
            NativeLoaderRuntimeBridgeAggregator.Config aggregatorConfig,
            String productGameplayBridgeKey,
            GameplayHandlerAttacher gameplayHandlerAttacher
    ) {
        Map<String, Object> bridge = new LinkedHashMap<>(runtimeBridge);
        Map<String, Object> lifecycleBridge = NativeLoaderRuntimeBridgeAggregator.aggregateLifecycleBridge(
                nativeActivations,
                aggregatorConfig
        );
        Map<String, Object> eventBridge = NativeLoaderRuntimeBridgeAggregator.aggregateEventBridge(nativeActivations);
        Map<String, Object> serviceBridge = NativeLoaderRuntimeBridgeAggregator.aggregateServiceBridge(
                nativeActivations,
                aggregatorConfig
        );
        Map<String, Object> transformBridge =
                NativeLoaderRuntimeBridgeAggregator.aggregateTransformBridge(nativeActivations);
        Map<String, Object> lifecycleEventHost =
                NativeLoaderRuntimeBridgeAggregator.nativeLoaderLifecycleEventHost(nativeActivations);
        bridge.put("lifecycleBridge", lifecycleBridge);
        bridge.put("eventBridge", eventBridge);
        bridge.put("serviceBridge", serviceBridge);
        bridge.put("transformBridge", transformBridge);
        bridge.put("nativeLoaderLifecycleEventHost", lifecycleEventHost);

        Map<String, Object> productGameplayBridge = object(bridge.get(productGameplayBridgeKey));
        Map<String, Object> attachedEventBridge =
                gameplayHandlerAttacher.attach(eventBridge, productGameplayBridge);
        bridge.put("eventBridge", attachedEventBridge);
        bridge.putAll(NativeLoaderAdapterCoreMarkerFields.enrichedFields(
                object(bridge.get("resourceBridge")),
                object(bridge.get("registryBridge")),
                productGameplayBridge,
                lifecycleBridge,
                attachedEventBridge,
                serviceBridge
        ));
        return bridge;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    public interface GameplayHandlerAttacher {
        Map<String, Object> attach(Map<String, Object> eventBridge, Map<String, Object> productGameplayBridge);
    }
}
