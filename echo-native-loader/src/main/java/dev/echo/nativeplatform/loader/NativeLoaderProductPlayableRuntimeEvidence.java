package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class NativeLoaderProductPlayableRuntimeEvidence {
    public static final String SERVICE_ID = "echo.native.product_playable_runtime_evidence";

    private NativeLoaderProductPlayableRuntimeEvidence() {
    }

    public static Map<String, Object> applyLiveEvidence(
            Map<String, Object> existing,
            Map<String, Object> liveClientProbe,
            String runtimeKey
    ) {
        Map<String, Object> playableRuntime = object(liveClientProbe.get(runtimeKey));
        if (playableRuntime.isEmpty()) {
            return existing;
        }
        Map<String, Object> bridge = new LinkedHashMap<>(existing);
        bridge.put("nativeProductPlayableRuntimeEvidenceServiceId", SERVICE_ID);
        boolean attempted = Boolean.TRUE.equals(playableRuntime.get("attempted"));
        boolean starterItemsGranted = Boolean.TRUE.equals(playableRuntime.get("starterItemsGranted"));
        boolean starterRegionMaterialized = Boolean.TRUE.equals(playableRuntime.get("starterRegionMaterialized"))
                || Boolean.TRUE.equals(playableRuntime.get("crashZoneMaterialized"));
        int serverBlocksPlaced = integer(playableRuntime.get("serverBlocksPlaced"));
        int clientBlocksPlaced = integer(playableRuntime.get("clientBlocksPlaced"));
        int serverCommandsSent = integer(playableRuntime.get("serverCommandsSent"));
        boolean uiRoutesReady = Boolean.TRUE.equals(playableRuntime.get("terminalLensIndexHudRoutesReady"));
        boolean saveDataWritten = Boolean.TRUE.equals(playableRuntime.get("saveDataWritten"));
        boolean hudNotificationEmitted = Boolean.TRUE.equals(playableRuntime.get("hudNotificationEmitted"));
        boolean firstLoopRuntimeReady = attempted
                && starterItemsGranted
                && starterRegionMaterialized
                && serverBlocksPlaced > 0
                && clientBlocksPlaced > 0
                && uiRoutesReady;
        bridge.put("playableBetaRuntimeAttempted", attempted);
        bridge.put("playableBetaStarterItemsGranted", starterItemsGranted);
        bridge.put("playableBetaStarterRegionMaterialized", starterRegionMaterialized);
        bridge.put("playableBetaCrashZoneMaterialized", starterRegionMaterialized);
        bridge.put("playableBetaServerBlocksPlaced", serverBlocksPlaced);
        bridge.put("playableBetaClientBlocksPlaced", clientBlocksPlaced);
        bridge.put("playableBetaServerCommandsSent", serverCommandsSent);
        bridge.put("playableBetaSaveDataWritten", saveDataWritten);
        bridge.put("playableBetaSaveDataScope", playableRuntime.getOrDefault("saveDataScope", ""));
        bridge.put("playableBetaSaveDataKey", playableRuntime.getOrDefault("saveDataKey", ""));
        bridge.put("playableBetaHudNotificationEmitted", hudNotificationEmitted);
        bridge.put("playableBetaHudNotificationEvidence", playableRuntime.getOrDefault("hudNotificationEvidence", Map.of()));
        bridge.put("playableBetaTerminalLensIndexHudRoutesReady", uiRoutesReady);
        bridge.put("playableBetaServerBlockPlacementSource", playableRuntime.getOrDefault("serverBlockPlacementSource", ""));
        bridge.put("playableBetaClientBlockPlacementSource", playableRuntime.getOrDefault("clientBlockPlacementSource", ""));
        bridge.put("playableBetaStarterToolItems", playableRuntime.getOrDefault("starterToolItems", List.of()));
        bridge.put("playableBetaStarterRegionBlocks", playableRuntime.getOrDefault(
                "starterRegionBlocks",
                playableRuntime.getOrDefault("starterCrashZoneBlocks", List.of())));
        bridge.put("playableBetaStarterCrashZoneBlocks", playableRuntime.getOrDefault(
                "starterCrashZoneBlocks",
                playableRuntime.getOrDefault("starterRegionBlocks", List.of())));
        bridge.put("playableBetaRuntimeSummary", playableRuntime.getOrDefault("summary", ""));
        if (firstLoopRuntimeReady) {
            bridge.put("firstPlayableLoopReady", true);
            bridge.put("firstPlayableLoopBlockedReason", "");
        }
        return Map.copyOf(bridge);
    }

    public static List<Map<String, Object>> mutationLedger(
            Map<String, Object> result,
            Map<String, Object> hudNotificationEvidence,
            Config config
    ) {
        Config safeConfig = Config.safe(config);
        Map<String, Object> hudEvidence = hudNotificationEvidence == null ? Map.of() : hudNotificationEvidence;
        List<Map<String, Object>> ledger = new ArrayList<>();
        ledger.add(liveMutationRecord(
                ledger.size() + 1,
                "inventory",
                "grant_starter_tools",
                "player.inventory",
                Boolean.TRUE.equals(result.get("hostInventoryMutated")),
                "before=" + object(result.get("hostInventoryMutationEvidence")).getOrDefault("beforeSummary", "live_inventory_before_probe"),
                "after=" + object(result.get("hostInventoryMutationEvidence")).getOrDefault("resultSnapshot", Map.of()),
                "EchoNativeRuntimeHost.Inventory",
                object(result.get("hostInventoryMutationEvidence")),
                safeConfig));
        ledger.add(liveMutationRecord(
                ledger.size() + 1,
                "world_blocks",
                "materialize_starter_region",
                "integrated_server.player_origin",
                Boolean.TRUE.equals(result.get("hostWorldBlockMutated")),
                "before=" + object(result.get("hostWorldBlockMutationEvidence")).getOrDefault("beforeSummary", "live_world_before_probe"),
                "after=" + object(result.get("hostWorldBlockMutationEvidence")).getOrDefault("resultSnapshot", Map.of()),
                "EchoNativeRuntimeHost.WorldBlocks",
                object(result.get("hostWorldBlockMutationEvidence")),
                safeConfig));
        ledger.add(liveMutationRecord(
                ledger.size() + 1,
                "save_data",
                "write",
                result.getOrDefault("saveDataScope", "") + "/" + result.getOrDefault("saveDataKey", ""),
                Boolean.TRUE.equals(result.get("saveDataWritten")),
                "before=unknown_save_scope",
                "after=" + object(result.get("saveDataWriteEvidence")).getOrDefault("resultSnapshot", Map.of()),
                "EchoNativeRuntimeHost.SaveData",
                object(result.get("saveDataWriteEvidence")),
                safeConfig));
        ledger.add(liveMutationRecord(
                ledger.size() + 1,
                "hud",
                "publish_notification",
                safeConfig.hudLedgerTarget(),
                Boolean.TRUE.equals(hudEvidence.get("mutated")),
                "before=unknown_hud_state",
                "after=" + hudEvidence.getOrDefault("resultSnapshot", Map.of()),
                "EchoNativeRuntimeHost.Hud",
                hudEvidence,
                safeConfig));
        return List.copyOf(ledger);
    }

    public static Set<String> mutatedSurfaces(Object value, Config config) {
        return mutatedSurfaces(objectList(value), config);
    }

    public static Set<String> mutatedSurfaces(List<Map<String, Object>> ledger, Config config) {
        Set<String> surfaces = new TreeSet<>();
        for (Map<String, Object> record : ledger) {
            if ("MUTATED".equals(String.valueOf(record.getOrDefault("status", "")))
                    && mutationRecordResolved(record, config)) {
                String surface = String.valueOf(record.getOrDefault("surface", ""));
                if (!surface.isBlank()) {
                    surfaces.add(surface);
                }
            }
        }
        return surfaces;
    }

    public static boolean mutationRecordResolved(Map<String, Object> record, Config config) {
        return mutationRecordBackendResolved(record, config)
                && mutationRecordUsesNativeMinecraftDelegate(record, config);
    }

    public static boolean mutationRecordBackendResolved(Map<String, Object> record, Config config) {
        Config safeConfig = Config.safe(config);
        return safeConfig.adapterCoreServiceId().equals(String.valueOf(record.getOrDefault("serviceId", "")))
                && safeConfig.namespace().equals(String.valueOf(record.getOrDefault("resolvedModuleId", "")))
                && safeConfig.nativeLoaderBackendClass().equals(String.valueOf(record.getOrDefault("backendClass", "")))
                && safeConfig.nativeLoaderRuntimeLane().equals(String.valueOf(record.getOrDefault("runtimeLane", "")))
                && Boolean.TRUE.equals(record.get("runtimeHostRegistered"))
                && Boolean.TRUE.equals(record.get("adapterCoreCallEnteredNativeLoaderHost"))
                && Boolean.TRUE.equals(record.get("adapterCoreCallEnteredNativeLoaderBackend"))
                && !Boolean.TRUE.equals(record.get("compatibilityFallbackUsed"))
                && String.valueOf(record.getOrDefault("runtimeHostClass", "")).contains("NativeLoader")
                && !String.valueOf(record.getOrDefault("resolvedServiceClass", "")).isBlank();
    }

    public static boolean mutationRecordUsesNativeMinecraftDelegate(Map<String, Object> record, Config config) {
        Config safeConfig = Config.safe(config);
        String liveClass = String.valueOf(record.getOrDefault("liveMinecraftDelegateClass", ""));
        String liveId = String.valueOf(record.getOrDefault("liveMinecraftDelegateId", ""));
        String compatibilityBackendClass = String.valueOf(record.getOrDefault("compatibilityBackendClass", ""));
        String compatibilityDelegate = String.valueOf(record.getOrDefault("compatibilityDelegate", ""));
        String after = String.valueOf(record.getOrDefault("after", ""));
        boolean afterNamesNativeMinecraftDelegate = after.contains("liveMinecraftDelegateClass=" + safeConfig.nativeMinecraftRuntimeHostClass())
                && after.contains("liveMinecraftDelegateId=" + safeConfig.nativeMinecraftRuntimeHostId());
        String blockedCompatibilityDelegateClass = safeConfig.compatibilityDelegateClass();
        String blockedCompatibilityDelegateId = safeConfig.compatibilityDelegateId();
        boolean afterNamesCompatibilityDelegate = (!blockedCompatibilityDelegateClass.isBlank()
                && (after.contains("liveMinecraftDelegateClass=" + blockedCompatibilityDelegateClass)
                || after.contains("compatibilityBackendClass=" + blockedCompatibilityDelegateClass)))
                || (!blockedCompatibilityDelegateId.isBlank()
                && (after.contains("liveMinecraftDelegateId=" + blockedCompatibilityDelegateId)
                || after.contains("compatibilityDelegate=" + blockedCompatibilityDelegateId)))
                || after.contains("compatibilityFallbackUsed=true");
        if (liveClass.isBlank() && afterNamesNativeMinecraftDelegate) {
            liveClass = safeConfig.nativeMinecraftRuntimeHostClass();
        }
        if (liveId.isBlank() && afterNamesNativeMinecraftDelegate) {
            liveId = safeConfig.nativeMinecraftRuntimeHostId();
        }
        return safeConfig.nativeMinecraftRuntimeHostClass().equals(liveClass)
                && safeConfig.nativeMinecraftRuntimeHostId().equals(liveId)
                && compatibilityBackendClass.isBlank()
                && compatibilityDelegate.isBlank()
                && !afterNamesCompatibilityDelegate
                && !Boolean.TRUE.equals(record.get("compatibilityFallbackUsed"));
    }

    private static Map<String, Object> liveMutationRecord(
            int sequence,
            String surface,
            String action,
            String target,
            boolean mutated,
            String before,
            String after,
            String nativeInterface,
            Map<String, Object> hostEvidence,
            Config config
    ) {
        Map<String, Object> record = new LinkedHashMap<>();
        Map<String, Object> evidence = hostEvidence == null ? Map.of() : hostEvidence;
        record.put("sequence", sequence);
        record.put("surface", surface);
        record.put("action", action);
        record.put("target", target == null ? "" : target);
        record.put("status", mutated ? "MUTATED" : "FAILED");
        record.put("before", before == null ? "" : before);
        record.put("after", after == null ? "" : after);
        record.put("nativeInterface", nativeInterface);
        record.put("serviceId", config.adapterCoreServiceId());
        String runtimeHostClass = String.valueOf(evidence.getOrDefault("runtimeHostClass", ""));
        String runtimeHostId = String.valueOf(evidence.getOrDefault("runtimeHostId", ""));
        String runtimeLane = String.valueOf(evidence.getOrDefault("runtimeHostLane", ""));
        String liveMinecraftDelegateId = String.valueOf(evidence.getOrDefault("liveMinecraftDelegateId", ""));
        String liveMinecraftDelegateClass = String.valueOf(evidence.getOrDefault("liveMinecraftDelegateClass", ""));
        String compatibilityBackendClass = String.valueOf(evidence.getOrDefault("compatibilityBackendClass", ""));
        boolean runtimeHostRegistered = Boolean.TRUE.equals(evidence.get("runtimeHostRegistered"));
        String adapterCoreBackendClass = String.valueOf(evidence.getOrDefault("adapterCoreBackendClass", ""));
        boolean adapterCoreCallEnteredNativeLoaderHost =
                Boolean.TRUE.equals(evidence.get("adapterCoreCallEnteredNativeLoaderHost"));
        boolean adapterCoreCallEnteredNativeLoaderBackend =
                Boolean.TRUE.equals(evidence.get("adapterCoreCallEnteredNativeLoaderBackend"));
        boolean compatibilityFallbackUsed = Boolean.TRUE.equals(evidence.get("compatibilityFallbackUsed"));
        boolean nativeLoaderHost = config.nativeLoaderRuntimeLane().equals(runtimeLane)
                && !runtimeHostClass.isBlank()
                && runtimeHostClass.contains("NativeLoader")
                && runtimeHostRegistered
                && adapterCoreCallEnteredNativeLoaderHost
                && adapterCoreCallEnteredNativeLoaderBackend
                && !compatibilityFallbackUsed
                && config.nativeLoaderBackendClass().equals(adapterCoreBackendClass);
        record.put("resolvedModuleId", nativeLoaderHost ? config.namespace() : "");
        record.put("resolvedServiceClass", nativeLoaderHost ? runtimeHostClass : "");
        record.put("backendClass", nativeLoaderHost ? adapterCoreBackendClass : "");
        record.put("runtimeHostId", runtimeHostId);
        record.put("runtimeHostClass", runtimeHostClass);
        record.put("runtimeLane", runtimeLane);
        record.put("runtimeHostRegistered", runtimeHostRegistered);
        record.put("adapterCoreCallEnteredNativeLoaderHost", adapterCoreCallEnteredNativeLoaderHost);
        record.put("adapterCoreCallEnteredNativeLoaderBackend", adapterCoreCallEnteredNativeLoaderBackend);
        record.put("adapterCoreBackendClass", adapterCoreBackendClass);
        record.put("nativeLoaderBackendAttached", Boolean.TRUE.equals(evidence.get("nativeLoaderBackendAttached")));
        record.put("nativeLoaderBackendRecordStatus", String.valueOf(evidence.getOrDefault("nativeLoaderBackendRecordStatus", "")));
        record.put("nativeLoaderBackendRecord", object(evidence.get("nativeLoaderBackendRecord")));
        record.put("nativeLoaderRuntimeHostClass", String.valueOf(evidence.getOrDefault("nativeLoaderRuntimeHostClass", "")));
        record.put("compatibilityFallbackUsed", compatibilityFallbackUsed);
        record.put("compatibilityDelegate", String.valueOf(evidence.getOrDefault("compatibilityDelegate", "")));
        record.put("compatibilityBackendClass", compatibilityBackendClass);
        record.put("liveMinecraftDelegateId", liveMinecraftDelegateId);
        record.put("liveMinecraftDelegateClass", liveMinecraftDelegateClass);
        return Map.copyOf(record);
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return Map.copyOf(object);
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> object = new LinkedHashMap<>();
                map.forEach((key, child) -> object.put(String.valueOf(key), child));
                list.add(Map.copyOf(object));
            }
        }
        return List.copyOf(list);
    }

    public record Config(
            String adapterCoreServiceId,
            String namespace,
            String nativeLoaderBackendClass,
            String nativeLoaderRuntimeLane,
            String nativeMinecraftRuntimeHostClass,
            String nativeMinecraftRuntimeHostId,
            String compatibilityDelegateClass,
            String compatibilityDelegateId,
            String hudLedgerTarget
    ) {
        public Config {
            adapterCoreServiceId = text(adapterCoreServiceId);
            namespace = text(namespace);
            nativeLoaderBackendClass = text(nativeLoaderBackendClass);
            nativeLoaderRuntimeLane = text(nativeLoaderRuntimeLane);
            nativeMinecraftRuntimeHostClass = text(nativeMinecraftRuntimeHostClass);
            nativeMinecraftRuntimeHostId = text(nativeMinecraftRuntimeHostId);
            compatibilityDelegateClass = text(compatibilityDelegateClass);
            compatibilityDelegateId = text(compatibilityDelegateId);
            hudLedgerTarget = text(hudLedgerTarget);
        }

        public static Config safe(Config config) {
            return config == null
                    ? new Config("", "", "", "", "", "", "", "", "")
                    : config;
        }

        private static String text(String value) {
            return value == null ? "" : value;
        }
    }
}
