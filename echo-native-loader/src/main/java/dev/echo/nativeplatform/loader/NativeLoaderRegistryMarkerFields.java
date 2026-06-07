package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderRegistryMarkerFields {
    public static final String SERVICE_ID = "echo.native.registry_marker_fields";

    private NativeLoaderRegistryMarkerFields() {
    }

    public static Map<String, Object> markerFields(
            Map<String, Object> registryBridge,
            int registeredModuleItemCount
    ) {
        Map<String, Object> bridge = registryBridge == null ? Map.of() : registryBridge;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeRegistryMarkerServiceId", SERVICE_ID);
        fields.put("registryInjected", false);
        fields.put("registryMutated", Boolean.TRUE.equals(bridge.get("registryMutated")));
        fields.put("creativeContentVisible", Boolean.TRUE.equals(bridge.get("creativeContentVisible")));
        fields.put("creativeContentRegistered", Boolean.TRUE.equals(bridge.get("applied")));
        fields.put("nativeCreativeModuleTabContentVisible",
                Boolean.TRUE.equals(bridge.get("nativeCreativeModuleTabContentVisible")));
        fields.put("nativeCreativeModuleTabRegistryBacked",
                Boolean.TRUE.equals(bridge.get("nativeCreativeModuleTabRegistryBacked")));
        fields.put("nativeCreativeModuleTabVisibleItemCount",
                intValue(bridge.get("nativeCreativeModuleTabVisibleItemCount")));
        fields.put("nativeVisibleCreativeTabPathCount", intValue(bridge.get("visibleCreativeTabPathCount")));
        fields.put("registeredModuleItemCount", Math.max(registeredModuleItemCount, 0));
        return Map.copyOf(fields);
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
