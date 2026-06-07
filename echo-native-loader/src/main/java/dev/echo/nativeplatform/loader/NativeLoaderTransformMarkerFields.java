package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderTransformMarkerFields {
    public static final String SERVICE_ID = "echo.native.transform_marker_fields";

    private NativeLoaderTransformMarkerFields() {
    }

    public static Map<String, Object> markerFields(Map<String, Object> transformBridge) {
        Map<String, Object> bridge = transformBridge == null ? Map.of() : transformBridge;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeTransformMarkerServiceId", SERVICE_ID);
        fields.put("transformsPerformed", Boolean.TRUE.equals(bridge.get("bytecodeMutated")));
        fields.put("nativeTransformCompatibilityApplied", Boolean.TRUE.equals(bridge.get("applied")));
        fields.put("nativeTransformCompatibilityReady", Boolean.TRUE.equals(bridge.get("compatible")));
        fields.put("nativeTransformReplacementPlanned",
                Boolean.TRUE.equals(bridge.get("nativeProjectionReplacementPlanned")));
        fields.put("nativeTransformReplacementCount", intValue(bridge.get("plannedNativeProjectionCount")));
        fields.put("nativeTransformBytecodeMutated", Boolean.TRUE.equals(bridge.get("bytecodeMutated")));
        fields.put("nativeTransformSupportedDeclarations",
                bridge.getOrDefault("supportedNativeDeclarations", List.of()));
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
