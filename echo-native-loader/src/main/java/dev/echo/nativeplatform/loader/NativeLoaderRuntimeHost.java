package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * First-class runtime host for the native product launcher path.
 *
 * <p>When a {@code savesDirectory} is configured, every mutation is persisted
 * to disk immediately. This keeps the headless native product runtime stateful
 * without requiring a live Minecraft delegate.</p>
 */
public final class NativeLoaderRuntimeHost {
    public static final String SERVICE_ID = "echo_native.runtime_host";
    private static final List<String> SUPPORTED_SURFACES = List.of(
            "runtime",
            "runtime_host",
            "minecraft_runtime_projection",
            "inventory",
            "player_state",
            "world_blocks",
            "world_state",
            "structures",
            "block_entities",
            "capabilities",
            "events",
            "packets_hud",
            "save_data",
            "hud",
            "missions",
            "feedback",
            "client_tick",
            "render_layers",
            "screen_events",
            "keybinds",
            "commands",
            "network_channels",
            "config_reloads",
            "resource_reloads",
            "save_hooks",
            "lifecycle_phases",
            "server_client_sync"
    );

    private final NativeLoaderRuntimeHostContext context;
    private final Map<String, Integer> inventory = new LinkedHashMap<>();
    private final Map<String, String> playerState = new LinkedHashMap<>();
    private final Map<String, String> worldBlocks = new LinkedHashMap<>();
    private final Map<String, String> worldState = new LinkedHashMap<>();
    private final Map<String, String> structures = new LinkedHashMap<>();
    private final Map<String, String> blockEntities = new LinkedHashMap<>();
    private final Map<String, String> capabilities = new LinkedHashMap<>();
    private final Map<String, String> events = new LinkedHashMap<>();
    private final Map<String, String> packetsHud = new LinkedHashMap<>();
    private final Map<String, String> saveData = new LinkedHashMap<>();
    private final Map<String, String> hud = new LinkedHashMap<>();
    private final Map<String, String> missions = new LinkedHashMap<>();
    private final Map<String, String> feedback = new LinkedHashMap<>();
    private final Map<String, String> clientTicks = new LinkedHashMap<>();
    private final Map<String, String> renderLayers = new LinkedHashMap<>();
    private final Map<String, String> screenEvents = new LinkedHashMap<>();
    private final Map<String, String> keybinds = new LinkedHashMap<>();
    private final Map<String, String> resourceReloads = new LinkedHashMap<>();
    private final Map<String, String> saveHooks = new LinkedHashMap<>();
    private final Map<String, String> serverClientSync = new LinkedHashMap<>();
    private final Map<String, String> liveRuntimeBridgeStatusBySurface = new LinkedHashMap<>();
    private final Map<String, Boolean> liveRuntimeBridgeProofBySurface = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> liveRuntimeBridgeProofEvidenceBySurface = new LinkedHashMap<>();
    private int liveRuntimeBridgeDispatchCount = 0;
    private int liveRuntimeBridgeMutationCount = 0;
    private int liveRuntimeBridgeFailureCount = 0;
    private int liveRuntimeBridgeUnsupportedCount = 0;
    private int nativeMirrorMutationCount = 0;
    private int fallbackMirrorMutationCount = 0;
    private final Map<String, Integer> fallbackMirrorMutationCountBySurface = new LinkedHashMap<>();
    private long liveRuntimeDispatchSequence = 0L;

    public NativeLoaderRuntimeHost(NativeLoaderRuntimeHostContext context) {
        this.context = context;
        if (context.savesDirectory() != null) {
            loadAll();
        }
    }

    public NativeLoaderRuntimeHostContext context() {
        return context;
    }

    public String runtimeHostId() {
        return context.runtimeHostId();
    }

    public boolean runtimeHostRegistered() {
        return context.runtimeHostRegistered();
    }

    public String runtimeLane() {
        return "Native Loader";
    }

    public String runtimeKind() {
        return context.liveRuntimeAttachment().runtimeKind();
    }

    public String runtimeMode() {
        return context.liveRuntimeAttachment().runtimeMode();
    }

    public boolean firstClassNativeRuntime() {
        return context.liveRuntimeAttachment().firstClassNativeRuntime();
    }

    public boolean nativeRuntimeProcess() {
        return context.liveRuntimeAttachment().nativeRuntimeProcess();
    }

    public boolean nativeStateRuntimeAvailable() {
        return runtimeHostRegistered();
    }

    public boolean delegateRequired() {
        return context.liveRuntimeAttachment().delegateRequired();
    }

    public boolean liveMinecraftAttached() {
        return context.liveRuntimeAttachment().liveMinecraftAttached();
    }

    public boolean releaseRuntimeTrusted() {
        return context.liveRuntimeAttachment().releaseRuntimeTrusted();
    }

    public boolean liveRuntimeBridgeAttached() {
        return context.liveRuntimeBridge().attached();
    }

    public String liveRuntimeBridgeId() {
        return context.liveRuntimeBridge().bridgeId();
    }

    public List<String> supportedSurfaces() {
        if (context.liveRuntimeAttachment().supportedSurfaces().isEmpty()) {
            return SUPPORTED_SURFACES;
        }
        List<String> surfaces = new ArrayList<>(SUPPORTED_SURFACES);
        for (String surface : context.liveRuntimeAttachment().supportedSurfaces()) {
            if (surface != null && !surface.isBlank() && !surfaces.contains(surface)) {
                surfaces.add(surface);
            }
        }
        return List.copyOf(surfaces);
    }

    public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
        if (playerId == null || playerId.isBlank() || itemId == null || itemId.isBlank() || count <= 0) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "inventory",
                bridge -> bridge.grantItem(playerId, itemId, count),
                () -> {
                    inventory.merge(playerId + ":" + itemId, count, Integer::sum);
                    persist("inventory");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        String key = playerId + ":" + itemId;
        inventory.merge(key, count, Integer::sum);
        persist("inventory");
        return fallbackMirrorMutation("inventory", EchoNativeLoadStatus.MUTATED);
    }

    public EchoNativeLoadStatus removeItem(String playerId, String itemId, int count) {
        if (playerId == null || playerId.isBlank() || itemId == null || itemId.isBlank() || count <= 0) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "inventory",
                bridge -> bridge.removeItem(playerId, itemId, count),
                () -> mirrorRemoveItem(playerId, itemId, count)
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        String key = playerId + ":" + itemId;
        Integer current = inventory.get(key);
        if (current == null || current <= 0) {
            return EchoNativeLoadStatus.RESOLVED;
        }
        if (count >= current) {
            inventory.remove(key);
        } else {
            inventory.put(key, current - count);
        }
        persist("inventory");
        return fallbackMirrorMutation("inventory", EchoNativeLoadStatus.MUTATED);
    }

    public EchoNativeLoadStatus updatePlayerState(String playerId, String key, String value) {
        if (playerId == null || playerId.isBlank() || key == null || key.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "player_state",
                bridge -> bridge.updatePlayerState(playerId, key, value),
                () -> {
                    playerState.put(playerId + ":" + key, value == null ? "" : value);
                    persist("playerState");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(playerState, playerId + ":" + key, value);
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("playerState");
        }
        return fallbackMirrorMutation("player_state", status);
    }

    public EchoNativeLoadStatus placeBlock(String dimension, int x, int y, int z, String blockId) {
        if (dimension == null || dimension.isBlank() || blockId == null || blockId.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        String key = dimension + ":" + x + "," + y + "," + z;
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "world_blocks",
                bridge -> bridge.placeBlock(dimension, x, y, z, blockId),
                () -> {
                    worldBlocks.put(key, blockId);
                    persist("worldBlocks");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(worldBlocks, key, blockId);
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("worldBlocks");
        }
        return fallbackMirrorMutation("world_blocks", status);
    }

    public EchoNativeLoadStatus updateWorldState(String dimension, String key, String value) {
        if (dimension == null || dimension.isBlank() || key == null || key.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "world_state",
                bridge -> bridge.updateWorldState(dimension, key, value),
                () -> {
                    worldState.put(dimension + ":" + key, value == null ? "" : value);
                    persist("worldState");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(worldState, dimension + ":" + key, value);
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("worldState");
        }
        return fallbackMirrorMutation("world_state", status);
    }

    public EchoNativeLoadStatus placeStructure(String dimension, String structureId, int x, int y, int z) {
        if (dimension == null || dimension.isBlank() || structureId == null || structureId.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "structures",
                bridge -> bridge.placeStructure(dimension, structureId, x, y, z),
                () -> {
                    structures.put(dimension + ":" + x + "," + y + "," + z, structureId);
                    persist("structures");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(structures, dimension + ":" + x + "," + y + "," + z, structureId);
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("structures");
        }
        return fallbackMirrorMutation("structures", status);
    }

    public EchoNativeLoadStatus updateBlockEntity(String dimension, int x, int y, int z, String key, String value) {
        if (dimension == null || dimension.isBlank() || key == null || key.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        String stateKey = dimension + ":" + x + "," + y + "," + z + ":" + key;
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "block_entities",
                bridge -> bridge.updateBlockEntity(dimension, x, y, z, key, value),
                () -> {
                    blockEntities.put(stateKey, value == null ? "" : value);
                    persist("blockEntities");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(blockEntities, stateKey, value);
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("blockEntities");
        }
        return fallbackMirrorMutation("block_entities", status);
    }

    public EchoNativeLoadStatus updateCapability(String target, String capability, String value) {
        if (target == null || target.isBlank() || capability == null || capability.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "capabilities",
                bridge -> bridge.updateCapability(target, capability, value),
                () -> {
                    capabilities.put(target + ":" + capability, value == null ? "" : value);
                    persist("capabilities");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(capabilities, target + ":" + capability, value);
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("capabilities");
        }
        return fallbackMirrorMutation("capabilities", status);
    }

    public EchoNativeLoadStatus emitEvent(String eventType, String payload) {
        if (eventType == null || eventType.isBlank() || payload == null || payload.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "events",
                bridge -> bridge.emitEvent(eventType, payload),
                () -> {
                    events.put(String.valueOf(events.size() + 1), eventType + "=" + payload);
                    persist("events");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        events.put(String.valueOf(events.size() + 1), eventType + "=" + payload);
        persist("events");
        return fallbackMirrorMutation("events", EchoNativeLoadStatus.MUTATED);
    }

    public EchoNativeLoadStatus sendPacketHud(String channel, String payload) {
        if (channel == null || channel.isBlank() || payload == null || payload.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "packets_hud",
                bridge -> bridge.sendPacketHud(channel, payload),
                () -> {
                    packetsHud.put(String.valueOf(packetsHud.size() + 1), channel + "=" + payload);
                    persist("packetsHud");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        packetsHud.put(String.valueOf(packetsHud.size() + 1), channel + "=" + payload);
        persist("packetsHud");
        return fallbackMirrorMutation("packets_hud", EchoNativeLoadStatus.MUTATED);
    }

    public EchoNativeLoadStatus writeSaveData(String key, String value) {
        if (key == null || key.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "save_data",
                bridge -> bridge.writeSaveData(key, value),
                () -> {
                    saveData.put(key, value == null ? "" : value);
                    persist("saveData");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(saveData, key, value);
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("saveData");
        }
        return fallbackMirrorMutation("save_data", status);
    }

    public EchoNativeLoadStatus deleteSaveData(String key) {
        if (key == null || key.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "save_data",
                bridge -> bridge.deleteSaveData(key),
                () -> {
                    saveData.remove(key);
                    persist("saveData");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        if (!saveData.containsKey(key)) {
            return EchoNativeLoadStatus.RESOLVED;
        }
        saveData.remove(key);
        persist("saveData");
        return fallbackMirrorMutation("save_data", EchoNativeLoadStatus.MUTATED);
    }

    public EchoNativeLoadStatus emitHud(String channel, String message) {
        if (channel == null || channel.isBlank() || message == null || message.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "hud",
                bridge -> bridge.emitHud(channel, message),
                () -> {
                    hud.put(channel, message);
                    persist("hud");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(hud, channel, message);
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("hud");
        }
        return fallbackMirrorMutation("hud", status);
    }

    public EchoNativeLoadStatus updateMission(String missionId, String phase, String objectiveKey) {
        if (missionId == null || missionId.isBlank() || phase == null || phase.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        String key = missionId + ":" + phase + ":" + (objectiveKey == null ? "" : objectiveKey);
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "missions",
                bridge -> bridge.updateMission(missionId, phase, objectiveKey),
                () -> {
                    missions.put(key, "active");
                    persist("missions");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(missions, key, "active");
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("missions");
        }
        return fallbackMirrorMutation("missions", status);
    }

    public EchoNativeLoadStatus emitFeedback(String source, String message) {
        if (source == null || source.isBlank() || message == null || message.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "feedback",
                bridge -> bridge.emitFeedback(source, message),
                () -> {
                    feedback.put(String.valueOf(feedback.size() + 1), source + "=" + message);
                    persist("feedback");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        feedback.put(String.valueOf(feedback.size() + 1), source + "=" + message);
        persist("feedback");
        return fallbackMirrorMutation("feedback", EchoNativeLoadStatus.MUTATED);
    }

    public EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
        if (phase == null || phase.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        Map<String, Object> safePayload = mutableEvidence(payload);
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "client_tick",
                bridge -> bridge.clientTick(phase, safePayload),
                () -> {
                    clientTicks.put(String.valueOf(clientTicks.size() + 1), phase + "=" + stringify(safePayload));
                    persist("clientTicks");
                },
                safePayload
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        clientTicks.put(String.valueOf(clientTicks.size() + 1), phase + "=" + stringify(safePayload));
        persist("clientTicks");
        return fallbackMirrorMutation("client_tick", EchoNativeLoadStatus.MUTATED);
    }

    public EchoNativeLoadStatus renderLayer(String layerId, Map<String, Object> payload) {
        if (layerId == null || layerId.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        Map<String, Object> safePayload = mutableEvidence(payload);
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "render_layers",
                bridge -> bridge.renderLayer(layerId, safePayload),
                () -> {
                    renderLayers.put(layerId, stringify(safePayload));
                    persist("renderLayers");
                },
                safePayload
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(renderLayers, layerId, stringify(safePayload));
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("renderLayers");
        }
        return fallbackMirrorMutation("render_layers", status);
    }

    public EchoNativeLoadStatus screenEvent(String screenId, String eventType, Map<String, Object> payload) {
        if (screenId == null || screenId.isBlank() || eventType == null || eventType.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        String key = screenId + ":" + eventType + ":" + (screenEvents.size() + 1);
        Map<String, Object> safePayload = mutableEvidence(payload);
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "screen_events",
                bridge -> bridge.screenEvent(screenId, eventType, safePayload),
                () -> {
                    screenEvents.put(key, stringify(safePayload));
                    persist("screenEvents");
                },
                safePayload
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        screenEvents.put(key, stringify(safePayload));
        persist("screenEvents");
        return fallbackMirrorMutation("screen_events", EchoNativeLoadStatus.MUTATED);
    }

    public EchoNativeLoadStatus keybind(String keybindId, String action, Map<String, Object> payload) {
        if (keybindId == null || keybindId.isBlank() || action == null || action.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        String key = keybindId + ":" + action;
        Map<String, Object> safePayload = mutableEvidence(payload);
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "keybinds",
                bridge -> bridge.keybind(keybindId, action, safePayload),
                () -> {
                    keybinds.put(key, stringify(safePayload));
                    persist("keybinds");
                },
                safePayload
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(keybinds, key, stringify(safePayload));
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("keybinds");
        }
        return fallbackMirrorMutation("keybinds", status);
    }

    public EchoNativeLoadStatus reloadResources(String moduleId, String resourceId, String scope, Map<String, Object> evidence) {
        if (moduleId == null || moduleId.isBlank() || resourceId == null || resourceId.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        String key = moduleId + ":" + resourceId;
        Map<String, Object> safeEvidence = mutableEvidence(evidence);
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "resource_reloads",
                bridge -> bridge.reloadResources(moduleId, resourceId, scope, safeEvidence),
                () -> {
                    resourceReloads.put(key, value(scope, "resources") + "=" + stringify(safeEvidence));
                    persist("resourceReloads");
                },
                safeEvidence
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        EchoNativeLoadStatus status = putChanged(resourceReloads, key, value(scope, "resources") + "=" + stringify(safeEvidence));
        if (status == EchoNativeLoadStatus.MUTATED) {
            persist("resourceReloads");
        }
        return fallbackMirrorMutation("resource_reloads", status);
    }

    public EchoNativeLoadStatus saveHook(String hookId, Map<String, Object> payload) {
        if (hookId == null || hookId.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        Map<String, Object> safePayload = mutableEvidence(payload);
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "save_hooks",
                bridge -> bridge.saveHook(hookId, safePayload),
                () -> {
                    saveHooks.put(String.valueOf(saveHooks.size() + 1), hookId + "=" + stringify(safePayload));
                    persist("saveHooks");
                },
                safePayload
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        saveHooks.put(String.valueOf(saveHooks.size() + 1), hookId + "=" + stringify(safePayload));
        persist("saveHooks");
        return fallbackMirrorMutation("save_hooks", EchoNativeLoadStatus.MUTATED);
    }

    public EchoNativeLoadStatus syncServerClient(String channel, String payload) {
        if (channel == null || channel.isBlank() || payload == null || payload.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        EchoNativeLoadStatus liveStatus = dispatchLive(
                "server_client_sync",
                bridge -> bridge.syncServerClient(channel, payload),
                () -> {
                    serverClientSync.put(String.valueOf(serverClientSync.size() + 1), channel + "=" + payload);
                    persist("serverClientSync");
                }
        );
        if (liveStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            return liveStatus;
        }
        serverClientSync.put(String.valueOf(serverClientSync.size() + 1), channel + "=" + payload);
        persist("serverClientSync");
        return fallbackMirrorMutation("server_client_sync", EchoNativeLoadStatus.MUTATED);
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("runtimeHost", runtimeHostReport());
        snapshot.put("inventory", Map.copyOf(inventory));
        snapshot.put("playerState", Map.copyOf(playerState));
        snapshot.put("worldBlocks", Map.copyOf(worldBlocks));
        snapshot.put("worldState", Map.copyOf(worldState));
        snapshot.put("structures", Map.copyOf(structures));
        snapshot.put("blockEntities", Map.copyOf(blockEntities));
        snapshot.put("capabilities", Map.copyOf(capabilities));
        snapshot.put("events", Map.copyOf(events));
        snapshot.put("packetsHud", Map.copyOf(packetsHud));
        snapshot.put("saveData", Map.copyOf(saveData));
        snapshot.put("hud", Map.copyOf(hud));
        snapshot.put("missions", Map.copyOf(missions));
        snapshot.put("feedback", Map.copyOf(feedback));
        snapshot.put("clientTicks", Map.copyOf(clientTicks));
        snapshot.put("renderLayers", Map.copyOf(renderLayers));
        snapshot.put("screenEvents", Map.copyOf(screenEvents));
        snapshot.put("keybinds", Map.copyOf(keybinds));
        snapshot.put("resourceReloads", Map.copyOf(resourceReloads));
        snapshot.put("saveHooks", Map.copyOf(saveHooks));
        snapshot.put("serverClientSync", Map.copyOf(serverClientSync));
        return snapshot;
    }

    public Map<String, Object> runtimeHostReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("serviceId", SERVICE_ID);
        report.put("runtimeHostId", runtimeHostId());
        report.put("runtimeHostClass", getClass().getName());
        report.put("runtimeLane", runtimeLane());
        report.put("runtimeKind", runtimeKind());
        report.put("runtimeMode", runtimeMode());
        report.put("runtimeHostRegistered", runtimeHostRegistered());
        report.put("firstClassNativeRuntime", firstClassNativeRuntime());
        report.put("nativeRuntimeProcess", nativeRuntimeProcess());
        report.put("nativeStateRuntimeAvailable", nativeStateRuntimeAvailable());
        report.put("nativeStateAuthoritative", true);
        report.put("liveRuntimeBridgeBlocksNativeState", false);
        report.put("delegateRequired", delegateRequired());
        report.put("liveMinecraftAttached", liveMinecraftAttached());
        report.put("releaseRuntimeTrusted", releaseRuntimeTrusted());
        report.put("liveRuntimeBridgeAttached", liveRuntimeBridgeAttached());
        report.put("liveRuntimeBridgeId", liveRuntimeBridgeId());
        report.put("liveRuntimeBridgeDispatchCount", liveRuntimeBridgeDispatchCount);
        report.put("liveRuntimeBridgeMutationCount", liveRuntimeBridgeMutationCount);
        report.put("liveRuntimeBridgeFailureCount", liveRuntimeBridgeFailureCount);
        report.put("liveRuntimeBridgeUnsupportedCount", liveRuntimeBridgeUnsupportedCount);
        report.put("nativeMirrorMutationCount", nativeMirrorMutationCount);
        report.put("fallbackMirrorMutationCount", fallbackMirrorMutationCount);
        report.put("fallbackMirrorMutationCountBySurface", Map.copyOf(fallbackMirrorMutationCountBySurface));
        report.put("liveRuntimeAccessed", context.liveRuntimeBridge().liveRuntimeAccessed());
        report.put("minecraftRuntimeAccessed", context.liveRuntimeBridge().minecraftRuntimeAccessed());
        report.put("liveRuntimeMutationSupported", context.liveRuntimeBridge().liveRuntimeMutationSupported());
        report.put("liveRuntimeBridgeEvidence", context.liveRuntimeBridge().runtimeEvidence());
        report.put("mirrorOnlyReleaseProof", mirrorOnlyReleaseProof());
        report.put("liveRuntimeSurfaceMutationCoverageSatisfied", liveRuntimeSurfaceMutationCoverageSatisfied());
        report.put("liveRuntimeUnbridgedMutatedSurfaces", liveRuntimeUnbridgedMutatedSurfaces());
        report.put("liveRuntimeReleaseProofSatisfied", liveRuntimeReleaseProofSatisfied());
        report.put("liveRuntimeBridgeStatusBySurface", Map.copyOf(liveRuntimeBridgeStatusBySurface));
        report.put("liveRuntimeBridgeProofBySurface", Map.copyOf(liveRuntimeBridgeProofBySurface));
        report.put("liveRuntimeBridgeProofEvidenceBySurface", copyNestedEvidence(liveRuntimeBridgeProofEvidenceBySurface));
        report.put("liveRuntimeAttachment", context.liveRuntimeAttachment().toReport());
        report.put("packId", context.packId());
        report.put("moduleId", context.moduleId());
        report.put("savesDirectoryConfigured", context.savesDirectory() != null);
        report.put("supportedSurfaces", supportedSurfaces());
        report.put("mutatedSurfaces", mutatedSurfaceNames());
        report.put("nativeStateMutationSurfaceCount", mutatedSurfaceNames().size());
        return Map.copyOf(report);
    }

    public List<String> mutatedSurfaces() {
        return mutatedSurfaceNames();
    }

    public boolean mirrorOnlyReleaseProof() {
        return fallbackMirrorMutationCount > 0 && liveRuntimeBridgeMutationCount == 0;
    }

    public boolean liveRuntimeReleaseProofSatisfied() {
        return liveRuntimeBridgeAttached()
                && context.liveRuntimeBridge().liveRuntimeAccessed()
                && context.liveRuntimeBridge().minecraftRuntimeAccessed()
                && context.liveRuntimeBridge().liveRuntimeMutationSupported()
                && liveRuntimeBridgeMutationCount > 0
                && liveRuntimeSurfaceMutationCoverageSatisfied()
                && fallbackMirrorMutationCount == 0
                && !mirrorOnlyReleaseProof();
    }

    public boolean liveRuntimeSurfaceMutationCoverageSatisfied() {
        return !releaseProofMutatedSurfaceNames().isEmpty() && liveRuntimeUnbridgedMutatedSurfaces().isEmpty();
    }

    public List<String> liveRuntimeUnbridgedMutatedSurfaces() {
        return releaseProofMutatedSurfaceNames().stream()
                .filter(surface -> !liveSurfaceProofSatisfied(liveSurfaceForNativeSurface(surface)))
                .toList();
    }

    private List<String> releaseProofMutatedSurfaceNames() {
        return mutatedSurfaceNames().stream()
                .filter(NativeLoaderRuntimeHost::releaseProofSurface)
                .toList();
    }

    private static boolean releaseProofSurface(String surface) {
        return !"feedback".equals(surface);
    }

    private List<String> mutatedSurfaceNames() {
        Map<String, Object> surfaces = new LinkedHashMap<>();
        surfaces.put("inventory", inventory);
        surfaces.put("playerState", playerState);
        surfaces.put("worldBlocks", worldBlocks);
        surfaces.put("worldState", worldState);
        surfaces.put("structures", structures);
        surfaces.put("blockEntities", blockEntities);
        surfaces.put("capabilities", capabilities);
        surfaces.put("events", events);
        surfaces.put("packetsHud", packetsHud);
        surfaces.put("saveData", saveData);
        surfaces.put("hud", hud);
        surfaces.put("missions", missions);
        surfaces.put("feedback", feedback);
        surfaces.put("clientTicks", clientTicks);
        surfaces.put("renderLayers", renderLayers);
        surfaces.put("screenEvents", screenEvents);
        surfaces.put("keybinds", keybinds);
        surfaces.put("resourceReloads", resourceReloads);
        surfaces.put("saveHooks", saveHooks);
        surfaces.put("serverClientSync", serverClientSync);
        return surfaces.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Map<?, ?> map && !map.isEmpty())
                .map(Map.Entry::getKey)
                .toList();
    }

    private static String liveSurfaceForNativeSurface(String surface) {
        if (surface == null || surface.isBlank()) {
            return "unknown";
        }
        return switch (surface) {
            case "playerState" -> "player_state";
            case "worldBlocks" -> "world_blocks";
            case "worldState" -> "world_state";
            case "blockEntities" -> "block_entities";
            case "packetsHud" -> "packets_hud";
            case "saveData" -> "save_data";
            case "clientTicks" -> "client_tick";
            case "renderLayers" -> "render_layers";
            case "screenEvents" -> "screen_events";
            case "resourceReloads" -> "resource_reloads";
            case "saveHooks" -> "save_hooks";
            case "serverClientSync" -> "server_client_sync";
            default -> surface;
        };
    }

    private void persist(String surface) {
        if (context.savesDirectory() == null) {
            return;
        }
        try {
            Path file = context.savesDirectory().resolve(surface + ".json");
            Files.createDirectories(file.getParent());
            Map<String, Object> data = switch (surface) {
                case "inventory" -> Map.copyOf(inventory);
                case "playerState" -> Map.copyOf(playerState);
                case "worldBlocks" -> Map.copyOf(worldBlocks);
                case "worldState" -> Map.copyOf(worldState);
                case "structures" -> Map.copyOf(structures);
                case "blockEntities" -> Map.copyOf(blockEntities);
                case "capabilities" -> Map.copyOf(capabilities);
                case "events" -> Map.copyOf(events);
                case "packetsHud" -> Map.copyOf(packetsHud);
                case "saveData" -> Map.copyOf(saveData);
                case "hud" -> Map.copyOf(hud);
                case "missions" -> Map.copyOf(missions);
                case "feedback" -> Map.copyOf(feedback);
                case "clientTicks" -> Map.copyOf(clientTicks);
                case "renderLayers" -> Map.copyOf(renderLayers);
                case "screenEvents" -> Map.copyOf(screenEvents);
                case "keybinds" -> Map.copyOf(keybinds);
                case "resourceReloads" -> Map.copyOf(resourceReloads);
                case "saveHooks" -> Map.copyOf(saveHooks);
                case "serverClientSync" -> Map.copyOf(serverClientSync);
                default -> Map.of();
            };
            Files.writeString(file, writeFlatJsonObject(data), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private void loadAll() {
        if (context.savesDirectory() == null) {
            return;
        }
        loadIntMap("inventory", inventory);
        loadMap("playerState", playerState);
        loadMap("worldBlocks", worldBlocks);
        loadMap("worldState", worldState);
        loadMap("structures", structures);
        loadMap("blockEntities", blockEntities);
        loadMap("capabilities", capabilities);
        loadMap("events", events);
        loadMap("packetsHud", packetsHud);
        loadMap("saveData", saveData);
        loadMap("hud", hud);
        loadMap("missions", missions);
        loadMap("feedback", feedback);
        loadMap("clientTicks", clientTicks);
        loadMap("renderLayers", renderLayers);
        loadMap("screenEvents", screenEvents);
        loadMap("keybinds", keybinds);
        loadMap("resourceReloads", resourceReloads);
        loadMap("saveHooks", saveHooks);
        loadMap("serverClientSync", serverClientSync);
    }

    @SuppressWarnings("unchecked")
    private void loadMap(String surface, Map<String, String> target) {
        Path file = context.savesDirectory().resolve(surface + ".json");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Map<String, Object> parsed = readFlatJsonObject(text);
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                target.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private void loadIntMap(String surface, Map<String, Integer> target) {
        Path file = context.savesDirectory().resolve(surface + ".json");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Map<String, Object> parsed = readFlatJsonObject(text);
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number number) {
                    target.put(entry.getKey(), number.intValue());
                } else {
                    try {
                        target.put(entry.getKey(), Integer.parseInt(String.valueOf(value)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static EchoNativeLoadStatus putChanged(Map<String, String> target, String key, String value) {
        String normalized = value == null ? "" : value;
        String previous = target.put(key, normalized);
        return normalized.equals(previous) ? EchoNativeLoadStatus.RESOLVED : EchoNativeLoadStatus.MUTATED;
    }

    private EchoNativeLoadStatus dispatchLive(
            String surface,
            Function<NativeLoaderLiveRuntimeBridge, EchoNativeLoadStatus> operation,
            Runnable mirrorMutation
    ) {
        return dispatchLive(surface, operation, mirrorMutation, null);
    }

    private EchoNativeLoadStatus dispatchLive(
            String surface,
            Function<NativeLoaderLiveRuntimeBridge, EchoNativeLoadStatus> operation,
            Runnable mirrorMutation,
            Map<String, Object> dispatchEvidence
    ) {
        NativeLoaderLiveRuntimeBridge bridge = context.liveRuntimeBridge();
        if (bridge == null || !bridge.attached()) {
            recordLiveRuntimeBridgeStatus(surface, EchoNativeLoadStatus.UNSUPPORTED);
            recordLiveRuntimeBridgeProof(surface, EchoNativeLoadStatus.UNSUPPORTED, "", dispatchEvidence, Map.of());
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        String dispatchId = nextLiveRuntimeDispatchId(surface);
        clearLiveDispatchProof(dispatchEvidence);
        stampLiveDispatchId(dispatchEvidence, dispatchId);
        bridge.beginLiveRuntimeSurfaceDispatch(surface, dispatchId);
        EchoNativeLoadStatus status;
        liveRuntimeBridgeDispatchCount++;
        try {
            status = operation.apply(bridge);
        } catch (RuntimeException exception) {
            recordLiveRuntimeBridgeStatus(surface, EchoNativeLoadStatus.FAILED);
            recordLiveRuntimeBridgeProof(surface, EchoNativeLoadStatus.FAILED, dispatchId, dispatchEvidence, Map.of());
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        if (status == null) {
            recordLiveRuntimeBridgeStatus(surface, EchoNativeLoadStatus.FAILED);
            recordLiveRuntimeBridgeProof(surface, EchoNativeLoadStatus.FAILED, dispatchId, dispatchEvidence, Map.of());
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        if (status == EchoNativeLoadStatus.MUTATED && mirrorMutation != null) {
            mirrorMutation.run();
            nativeMirrorMutationCount++;
        }
        recordLiveRuntimeBridgeStatus(surface, status);
        recordLiveRuntimeBridgeProof(surface, status, dispatchId, dispatchEvidence, bridge.liveRuntimeSurfaceEvidence(surface));
        return status == EchoNativeLoadStatus.MUTATED ? status : EchoNativeLoadStatus.UNSUPPORTED;
    }

    private EchoNativeLoadStatus fallbackMirrorMutation(String surface, EchoNativeLoadStatus status) {
        if (status == EchoNativeLoadStatus.MUTATED && releaseProofSurface(surface)) {
            fallbackMirrorMutationCount++;
            String key = surface == null || surface.isBlank() ? "unknown" : surface;
            fallbackMirrorMutationCountBySurface.merge(key, 1, Integer::sum);
        }
        return status;
    }

    private void recordLiveRuntimeBridgeStatus(String surface, EchoNativeLoadStatus status) {
        String key = surface == null || surface.isBlank() ? "unknown" : surface;
        EchoNativeLoadStatus safeStatus = status == null ? EchoNativeLoadStatus.FAILED : status;
        liveRuntimeBridgeStatusBySurface.put(key, safeStatus.name());
        if (safeStatus == EchoNativeLoadStatus.MUTATED) {
            liveRuntimeBridgeMutationCount++;
        } else if (safeStatus == EchoNativeLoadStatus.FAILED) {
            liveRuntimeBridgeFailureCount++;
        } else if (safeStatus == EchoNativeLoadStatus.UNSUPPORTED) {
            liveRuntimeBridgeUnsupportedCount++;
        }
    }

    private void recordLiveRuntimeBridgeProof(
            String surface,
            EchoNativeLoadStatus status,
            String dispatchId,
            Map<String, Object> dispatchEvidence,
            Map<String, Object> bridgeEvidence
    ) {
        String key = surface == null || surface.isBlank() ? "unknown" : surface;
        if (!directDispatchProofRequired(key)) {
            liveRuntimeBridgeProofEvidenceBySurface.remove(key);
            return;
        }
        Map<String, Object> mergedEvidence = mergeEvidence(dispatchEvidence, bridgeEvidence);
        boolean proofSatisfied = directLiveDispatchProofSatisfied(
                status,
                key,
                dispatchId,
                mergedEvidence
        );
        liveRuntimeBridgeProofBySurface.put(key, proofSatisfied);
        liveRuntimeBridgeProofEvidenceBySurface.put(key, proofSatisfied ? Map.copyOf(mergedEvidence) : Map.of());
    }

    private boolean liveSurfaceProofSatisfied(String liveSurface) {
        if (directDispatchProofRequired(liveSurface)) {
            return Boolean.TRUE.equals(liveRuntimeBridgeProofBySurface.get(liveSurface));
        }
        return "MUTATED".equals(liveRuntimeBridgeStatusBySurface.get(liveSurface));
    }

    private boolean directLiveDispatchProofSatisfied(
            EchoNativeLoadStatus status,
            String surface,
            String dispatchId,
            Map<String, Object> evidence
    ) {
        return status == EchoNativeLoadStatus.MUTATED
                && context.liveRuntimeBridge().liveRuntimeAccessed()
                && context.liveRuntimeBridge().minecraftRuntimeAccessed()
                && context.liveRuntimeBridge().liveRuntimeMutationSupported()
                && !evidence.isEmpty()
                && bool(evidence.get("liveRuntimeDispatchProofSatisfied"))
                && bool(evidence.get("liveRuntimeDispatchMinecraftAccessed"))
                && bool(evidence.get("liveRuntimeDispatchMutationSupported"))
                && bool(evidence.get("liveRuntimeDispatchLiveMutation"))
                && dispatchId != null
                && dispatchId.equals(String.valueOf(evidence.getOrDefault("liveRuntimeDispatchId", "")))
                && directSurfaceEvidenceMatches(surface, evidence)
                && directResourceReloadEvidenceSatisfied(surface, evidence)
                && directSaveDataEvidenceSatisfied(surface, evidence)
                && directPacketSurfaceEvidenceSatisfied(surface, evidence)
                && directEventSurfaceEvidenceSatisfied(surface, evidence)
                && directClientSurfaceSaveEvidenceSatisfied(surface, evidence)
                && directHudSurfaceEvidenceSatisfied(surface, evidence)
                && directInventorySurfaceEvidenceSatisfied(surface, evidence)
                && directPlayerStateSurfaceEvidenceSatisfied(surface, evidence)
                && directMissionSurfaceEvidenceSatisfied(surface, evidence)
                && directWorldBlockSurfaceEvidenceSatisfied(surface, evidence)
                && directStructureSurfaceEvidenceSatisfied(surface, evidence)
                && directBlockEntitySurfaceEvidenceSatisfied(surface, evidence)
                && directCapabilitySurfaceEvidenceSatisfied(surface, evidence);
    }

    private static boolean directSurfaceEvidenceMatches(String surface, Map<String, Object> evidence) {
        if (evidence == null || !evidence.containsKey("liveRuntimeSurface")) {
            return false;
        }
        String actual = String.valueOf(evidence.getOrDefault("liveRuntimeSurface", "")).trim();
        return !actual.isBlank() && actual.equals(surface == null ? "" : surface);
    }

    private static boolean directSaveDataEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"save_data".equals(surface) && !"save_hooks".equals(surface) && !"world_state".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeSaveDataTouched"))
                && (Boolean.TRUE.equals(evidence.get("runtimeSaveDataMutated"))
                || Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated")))
                && Boolean.TRUE.equals(evidence.get("liveSaveDataFileTouched"))
                && "world_save_file".equals(String.valueOf(evidence.get("runtimeSaveDataBackend")))
                && evidence.get("saveFile") instanceof String saveFile
                && !saveFile.isBlank();
    }

    private static boolean directResourceReloadEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"resource_reloads".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeSaveDataTouched"))
                && Boolean.TRUE.equals(evidence.get("liveSaveDataFileTouched"))
                && "world_save_file".equals(String.valueOf(evidence.get("runtimeSaveDataBackend")))
                && evidence.get("saveFile") instanceof String saveFile
                && !saveFile.isBlank();
    }

    private static boolean directPacketSurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if ("packets_hud".equals(surface)) {
            return Boolean.TRUE.equals(evidence.get("runtimePacketSent"))
                    && Boolean.TRUE.equals(evidence.get("runtimePacketMutated"));
        }
        if ("server_client_sync".equals(surface)) {
            return Boolean.TRUE.equals(evidence.get("runtimeSurfacePacketSent"))
                    && Boolean.TRUE.equals(evidence.get("runtimeSurfacePacketMutated"))
                    && Boolean.TRUE.equals(evidence.get("runtimeServerClientSyncPacketSent"))
                    && Boolean.TRUE.equals(evidence.get("runtimeServerClientSyncMutated"));
        }
        return true;
    }

    private static boolean directEventSurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"events".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeEventTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeEventPublished"))
                && Boolean.TRUE.equals(evidence.get("runtimeEventMutated"));
    }

    private static boolean directClientSurfaceSaveEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!List.of("client_tick", "render_layers", "screen_events", "keybinds").contains(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeSaveDataTouched"))
                && Boolean.TRUE.equals(evidence.get("liveSaveDataFileTouched"))
                && "world_save_file".equals(String.valueOf(evidence.get("runtimeSaveDataBackend")))
                && evidence.get("saveFile") instanceof String saveFile
                && !saveFile.isBlank();
    }

    private static boolean directHudSurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"hud".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeHudNotificationPublished"))
                && Boolean.TRUE.equals(evidence.get("runtimeHudNotificationMutated"));
    }

    private static boolean directInventorySurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"inventory".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeInventoryTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeInventoryMutated"));
    }

    private static boolean directPlayerStateSurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"player_state".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimePlayerStateTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimePlayerStateMutated"));
    }

    private static boolean directMissionSurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"missions".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimePlayerStateTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimePlayerStateMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeMissionStateTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeMissionStateMutated"));
    }

    private static boolean directWorldBlockSurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"world_blocks".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeWorldBlockTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeWorldBlockMutated"));
    }

    private static boolean directStructureSurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"structures".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeStructurePlaced"))
                && Boolean.TRUE.equals(evidence.get("runtimeStructureMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeSaveDataTouched"))
                && (Boolean.TRUE.equals(evidence.get("runtimeSaveDataMutated"))
                || Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated")))
                && Boolean.TRUE.equals(evidence.get("liveSaveDataFileTouched"))
                && "world_save_file".equals(String.valueOf(evidence.get("runtimeSaveDataBackend")))
                && evidence.get("saveFile") instanceof String saveFile
                && !saveFile.isBlank();
    }

    private static boolean directBlockEntitySurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"block_entities".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeBlockEntityTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeBlockEntityMutated"));
    }

    private static boolean directCapabilitySurfaceEvidenceSatisfied(String surface, Map<String, Object> evidence) {
        if (!"capabilities".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeCapabilityTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeCapabilityMutated"));
    }

    private static boolean directDispatchProofRequired(String liveSurface) {
        return switch (liveSurface == null ? "" : liveSurface) {
            case "inventory",
                 "player_state",
                 "world_blocks",
                 "world_state",
                 "structures",
                 "block_entities",
                 "capabilities",
                 "events",
                 "packets_hud",
                 "save_data",
                 "hud",
                 "missions",
                 "feedback",
                 "client_tick",
                 "render_layers",
                 "screen_events",
                 "keybinds",
                 "resource_reloads",
                 "save_hooks",
                 "server_client_sync" -> true;
            default -> false;
        };
    }

    private static Map<String, Object> mergeEvidence(
            Map<String, Object> dispatchEvidence,
            Map<String, Object> bridgeEvidence
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        if (dispatchEvidence != null) {
            evidence.putAll(dispatchEvidence);
        }
        if (bridgeEvidence != null) {
            evidence.putAll(bridgeEvidence);
        }
        return evidence;
    }

    private static Map<String, Map<String, Object>> copyNestedEvidence(Map<String, Map<String, Object>> evidenceBySurface) {
        Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
        evidenceBySurface.forEach((surface, evidence) ->
                copy.put(surface, evidence == null ? Map.of() : Map.copyOf(evidence)));
        return Map.copyOf(copy);
    }

    private static void clearLiveDispatchProof(Map<String, Object> evidence) {
        if (evidence == null) {
            return;
        }
        evidence.remove("liveRuntimeDispatchProofSatisfied");
        evidence.remove("liveRuntimeDispatchMinecraftAccessed");
        evidence.remove("liveRuntimeDispatchMutationSupported");
        evidence.remove("liveRuntimeDispatchLiveMutation");
        evidence.remove("liveRuntimeDispatchId");
        evidence.remove("liveRuntimeSurface");
        evidence.remove("liveMinecraftMutation");
        evidence.remove("minecraftRuntimeAccessed");
        evidence.remove("runtimeSurfaceSaveTouched");
        evidence.remove("runtimeSurfaceSaveMutated");
        evidence.remove("runtimeSaveDataTouched");
        evidence.remove("runtimeSaveDataMutated");
        evidence.remove("liveSaveDataFileTouched");
        evidence.remove("runtimeSaveDataBackend");
        evidence.remove("saveFile");
        evidence.remove("runtimeSurfacePacketSent");
        evidence.remove("runtimeSurfacePacketMutated");
        evidence.remove("runtimePacketSent");
        evidence.remove("runtimePacketMutated");
        evidence.remove("runtimePacketChannel");
        evidence.remove("runtimeServerClientSyncPacketSent");
        evidence.remove("runtimeServerClientSyncMutated");
        evidence.remove("runtimeServerClientSyncChannel");
        evidence.remove("runtimeServerClientSyncEventPublished");
        evidence.remove("runtimeSurfaceEventPublished");
        evidence.remove("runtimeSurfaceEventMutated");
        evidence.remove("runtimeEventTouched");
        evidence.remove("runtimeEventPublished");
        evidence.remove("runtimeEventMutated");
        evidence.remove("runtimeEventId");
        evidence.remove("runtimeHudNotificationPublished");
        evidence.remove("runtimeHudNotificationMutated");
        evidence.remove("runtimeInventoryTouched");
        evidence.remove("runtimeInventoryMutated");
        evidence.remove("runtimePlayerStateTouched");
        evidence.remove("runtimePlayerStateMutated");
        evidence.remove("runtimeMissionStateTouched");
        evidence.remove("runtimeMissionStateMutated");
        evidence.remove("runtimeWorldBlockTouched");
        evidence.remove("runtimeWorldBlockMutated");
        evidence.remove("runtimeStructurePlaced");
        evidence.remove("runtimeStructureMutated");
        evidence.remove("runtimeBlockEntityTouched");
        evidence.remove("runtimeBlockEntityMutated");
        evidence.remove("runtimeCapabilityTouched");
        evidence.remove("runtimeCapabilityMutated");
    }

    private void stampLiveDispatchId(Map<String, Object> evidence, String dispatchId) {
        if (evidence != null && dispatchId != null && !dispatchId.isBlank()) {
            evidence.put("liveRuntimeDispatchId", dispatchId);
        }
    }

    private String nextLiveRuntimeDispatchId(String surface) {
        liveRuntimeDispatchSequence++;
        String safeSurface = surface == null || surface.isBlank() ? "unknown" : surface;
        return context.moduleId() + ":" + safeSurface + ":" + liveRuntimeDispatchSequence;
    }

    private static Map<String, Object> mutableEvidence(Map<String, Object> evidence) {
        return new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private void mirrorRemoveItem(String playerId, String itemId, int count) {
        String key = playerId + ":" + itemId;
        Integer current = inventory.get(key);
        if (current == null || count >= current) {
            inventory.remove(key);
        } else {
            inventory.put(key, current - count);
        }
        persist("inventory");
    }

    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String stringify(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }
        return writeFlatJsonObject(data).replace("\r", "").replace("\n", "").trim();
    }

    private static String writeFlatJsonObject(Map<String, ?> data) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        List<String> keys = new ArrayList<>(data.keySet());
        keys.sort(String::compareTo);
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value = data.get(key);
            builder.append("  \"").append(escapeJson(key)).append("\": ");
            if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
            } else if (value == null) {
                builder.append("null");
            } else {
                builder.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
            if (i + 1 < keys.size()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("}\n");
        return builder.toString();
    }

    private static Map<String, Object> readFlatJsonObject(String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (text == null) {
            return result;
        }
        JsonCursor cursor = new JsonCursor(text);
        cursor.skipWhitespace();
        if (!cursor.consume('{')) {
            return result;
        }
        while (!cursor.done()) {
            cursor.skipWhitespace();
            if (cursor.consume('}')) {
                return result;
            }
            String key = cursor.readString();
            if (key == null) {
                return result;
            }
            cursor.skipWhitespace();
            if (!cursor.consume(':')) {
                return result;
            }
            cursor.skipWhitespace();
            Object value = cursor.readValue();
            result.put(key, value);
            cursor.skipWhitespace();
            if (cursor.consume(',')) {
                continue;
            }
            cursor.consume('}');
            return result;
        }
        return result;
    }

    private static String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static final class JsonCursor {
        private final String text;
        private int index;

        private JsonCursor(String text) {
            this.text = text;
        }

        private boolean done() {
            return index >= text.length();
        }

        private void skipWhitespace() {
            while (!done() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            if (!done() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private String readString() {
            if (!consume('"')) {
                return null;
            }
            StringBuilder builder = new StringBuilder();
            while (!done()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (done()) {
                        return null;
                    }
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> {
                            if (index + 4 > text.length()) {
                                return null;
                            }
                            String hex = text.substring(index, index + 4);
                            try {
                                builder.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException ignored) {
                                return null;
                            }
                            index += 4;
                        }
                        default -> builder.append(escaped);
                    }
                } else {
                    builder.append(ch);
                }
            }
            return null;
        }

        private Object readValue() {
            if (done()) {
                return "";
            }
            if (text.charAt(index) == '"') {
                String value = readString();
                return value == null ? "" : value;
            }
            int start = index;
            while (!done()) {
                char ch = text.charAt(index);
                if (ch == ',' || ch == '}' || Character.isWhitespace(ch)) {
                    break;
                }
                index++;
            }
            String raw = text.substring(start, index);
            if ("null".equals(raw)) {
                return "";
            }
            if ("true".equals(raw)) {
                return Boolean.TRUE;
            }
            if ("false".equals(raw)) {
                return Boolean.FALSE;
            }
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException ignored) {
                return raw;
            }
        }
    }
}
