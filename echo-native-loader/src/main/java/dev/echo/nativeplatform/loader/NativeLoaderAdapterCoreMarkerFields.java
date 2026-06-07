package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderAdapterCoreMarkerFields {
    public static final String SERVICE_ID = "echo.native.adaptercore_marker_fields";
    public static final String CONTENT_BRIDGE_ACTIVE = "adapterCoreContentBridgeActive";
    public static final String RUNTIME_BRIDGE_ACTIVE = "adapterCoreRuntimeBridgeActive";

    private NativeLoaderAdapterCoreMarkerFields() {
    }

    public static Map<String, Object> markerFields(Map<String, Object> runtimeBridge) {
        Map<String, Object> bridge = runtimeBridge == null ? Map.of() : runtimeBridge;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeAdapterCoreMarkerServiceId", SERVICE_ID);
        fields.put(CONTENT_BRIDGE_ACTIVE, Boolean.TRUE.equals(bridge.get(CONTENT_BRIDGE_ACTIVE)));
        fields.put(RUNTIME_BRIDGE_ACTIVE, Boolean.TRUE.equals(bridge.get(RUNTIME_BRIDGE_ACTIVE)));
        return Map.copyOf(fields);
    }

    public static Map<String, Object> seedFields(
            Map<String, Object> resourceBridge,
            Map<String, Object> registryBridge,
            Map<String, Object> productGameplayBridge
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(CONTENT_BRIDGE_ACTIVE, contentBridgeActive(resourceBridge, registryBridge, productGameplayBridge));
        fields.put(RUNTIME_BRIDGE_ACTIVE, false);
        return Map.copyOf(fields);
    }

    public static Map<String, Object> enrichedFields(
            Map<String, Object> resourceBridge,
            Map<String, Object> registryBridge,
            Map<String, Object> productGameplayBridge,
            Map<String, Object> lifecycleBridge,
            Map<String, Object> eventBridge,
            Map<String, Object> serviceBridge
    ) {
        boolean liveGameplayHandlersExecuted = Boolean.TRUE.equals(object(productGameplayBridge).get("liveGameplayHandlersAttached"))
                && Boolean.TRUE.equals(object(productGameplayBridge).get("gameplayHandlerExecuted"));
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(CONTENT_BRIDGE_ACTIVE, contentBridgeActive(resourceBridge, registryBridge, productGameplayBridge));
        fields.put(RUNTIME_BRIDGE_ACTIVE,
                Boolean.TRUE.equals(object(lifecycleBridge).get("lifecycleCodeExecuted"))
                        || Boolean.TRUE.equals(object(eventBridge).get("handlerExecuted"))
                        || Boolean.TRUE.equals(object(serviceBridge).get("serviceCodeExecuted"))
                        || liveGameplayHandlersExecuted);
        return Map.copyOf(fields);
    }

    public static Map<String, Object> withContentBridgeActive(Map<String, Object> runtimeBridge) {
        Map<String, Object> enriched = new LinkedHashMap<>(object(runtimeBridge));
        enriched.put(CONTENT_BRIDGE_ACTIVE, true);
        return Map.copyOf(enriched);
    }

    private static boolean contentBridgeActive(
            Map<String, Object> resourceBridge,
            Map<String, Object> registryBridge,
            Map<String, Object> productGameplayBridge
    ) {
        return Boolean.TRUE.equals(object(resourceBridge).get("applied"))
                || Boolean.TRUE.equals(object(registryBridge).get("applied"))
                || Boolean.TRUE.equals(object(productGameplayBridge).get("dataDiscovered"));
    }

    private static Map<String, Object> object(Map<String, Object> value) {
        if (value == null) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        value.forEach((key, item) -> object.put(String.valueOf(key), item));
        return Map.copyOf(object);
    }
}
