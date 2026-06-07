package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderProductGameplayMarkerFields {
    public static final String SERVICE_ID = "echo.native.product_gameplay_marker_fields";

    private NativeLoaderProductGameplayMarkerFields() {
    }

    public static Map<String, Object> markerFields(
            Map<String, Object> productGameplayBridge,
            String directLiveHookEvidencePath,
            boolean directLiveHookEvidencePresent,
            boolean customCreativeTabsRegistered
    ) {
        Map<String, Object> bridge = productGameplayBridge == null ? Map.of() : productGameplayBridge;
        Map<String, Object> worldLiveHostEvidence = object(bridge.get("agent7WorldLiveHostHookEvidence"));
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeProductGameplayMarkerServiceId", SERVICE_ID);
        fields.put("nativeProductGameplayDataDiscovered", Boolean.TRUE.equals(bridge.get("dataDiscovered")));
        fields.put("nativeProductGameplayContentApplied", Boolean.TRUE.equals(bridge.get("applied")));
        fields.put("nativeFirstPlayableLoopReady", Boolean.TRUE.equals(bridge.get("firstPlayableLoopReady")));
        fields.put("nativeLiveGameplayHandlersAttached", Boolean.TRUE.equals(bridge.get("liveGameplayHandlersAttached")));
        fields.put("nativeDirectLiveHookEvidencePath", directLiveHookEvidencePath == null ? "" : directLiveHookEvidencePath);
        fields.put("nativeDirectLiveHookEvidencePresent", directLiveHookEvidencePresent);
        fields.put("nativeWorldLiveHostHooksVerified",
                Boolean.TRUE.equals(worldLiveHostEvidence.get("allRequiredHooksVerified")));
        fields.put("nativeWorldLiveHostHookVerifiedCount", intValue(worldLiveHostEvidence.get("verifiedHookCount")));
        fields.put("nativeWorldLiveHostRequiredHookCount", intValue(worldLiveHostEvidence.get("requiredHookCount")));
        fields.put("nativeWorldLiveHostCandidateSignalCount",
                intValue(worldLiveHostEvidence.get("candidateLiveSignalCount")));
        fields.put("nativeGameplayHandlerExecuted", Boolean.TRUE.equals(bridge.get("gameplayHandlerExecuted")));
        fields.put("nativeCustomCreativeTabsRegistered", customCreativeTabsRegistered);
        fields.put("nativeProductMissionDefinitionCount", intValue(bridge.get("missionDefinitionCount")));
        fields.put("nativeProductWorldRegionCount", intValue(bridge.get("worldRegionCount")));
        fields.put("nativeProductProgressionAdvancementCount", intValue(bridge.get("progressionAdvancementCount")));
        fields.put("nativeProductPlayableRuntimeAttempted",
                Boolean.TRUE.equals(bridge.get("playableBetaRuntimeAttempted")));
        fields.put("nativeProductPlayableStarterItemsGranted",
                Boolean.TRUE.equals(bridge.get("playableBetaStarterItemsGranted")));
        fields.put("nativeProductPlayableStarterRegionMaterialized",
                Boolean.TRUE.equals(bridge.get("playableBetaStarterRegionMaterialized")));
        fields.put("nativeProductPlayableCrashZoneMaterialized",
                Boolean.TRUE.equals(bridge.getOrDefault(
                        "playableBetaCrashZoneMaterialized",
                        bridge.get("playableBetaStarterRegionMaterialized"))));
        fields.put("nativeProductPlayableServerBlocksPlaced", intValue(bridge.get("playableBetaServerBlocksPlaced")));
        fields.put("nativeProductPlayableClientBlocksPlaced", intValue(bridge.get("playableBetaClientBlocksPlaced")));
        fields.put("nativeProductPlayableServerCommandsSent", intValue(bridge.get("playableBetaServerCommandsSent")));
        fields.put("nativeProductPlayableSaveDataWritten",
                Boolean.TRUE.equals(bridge.get("playableBetaSaveDataWritten")));
        fields.put("nativeProductPlayableSaveDataKey", bridge.getOrDefault("playableBetaSaveDataKey", ""));
        fields.put("nativeProductPlayableHudNotificationEmitted",
                Boolean.TRUE.equals(bridge.get("playableBetaHudNotificationEmitted")));
        fields.put("nativeProductPlayableTerminalLensIndexHudRoutesReady",
                Boolean.TRUE.equals(bridge.get("playableBetaTerminalLensIndexHudRoutesReady")));
        return Map.copyOf(fields);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
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
