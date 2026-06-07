package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.List;
import java.util.Map;

/**
 * Typed bridge from the Native Loader runtime host into a live Minecraft runtime.
 *
 * <p>The headless product runtime remains useful for packaging and module-level
 * launch checks. Full live-runtime parity attaches an implementation of this
 * interface so AdapterCore mutations are dispatched through a real runtime path
 * instead of being only JSON-backed native state.</p>
 */
public interface NativeLoaderLiveRuntimeBridge {
    NativeLoaderLiveRuntimeBridge UNATTACHED = new NativeLoaderLiveRuntimeBridge() {
    };

    default boolean attached() {
        return false;
    }

    default String bridgeId() {
        return "native_loader:unattached_live_runtime_bridge";
    }

    default boolean liveRuntimeAccessed() {
        return false;
    }

    default boolean minecraftRuntimeAccessed() {
        return false;
    }

    default boolean liveRuntimeMutationSupported() {
        return false;
    }

    default Map<String, Object> runtimeEvidence() {
        return Map.of(
                "bridgeId", bridgeId(),
                "attached", attached(),
                "liveRuntimeAccessed", liveRuntimeAccessed(),
                "minecraftRuntimeAccessed", minecraftRuntimeAccessed(),
                "liveRuntimeMutationSupported", liveRuntimeMutationSupported()
        );
    }

    default Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
        return Map.of();
    }

    default void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
    }

    default EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus removeItem(String playerId, String itemId, int count) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus updatePlayerState(String playerId, String key, String value) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus placeBlock(String dimension, int x, int y, int z, String blockId) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus updateWorldState(String dimension, String key, String value) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus placeStructure(String dimension, String structureId, int x, int y, int z) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus updateBlockEntity(String dimension, int x, int y, int z, String key, String value) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus updateCapability(String target, String capability, String value) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus emitEvent(String eventType, String payload) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus sendPacketHud(String channel, String payload) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus writeSaveData(String key, String value) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus deleteSaveData(String key) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus emitHud(String channel, String message) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus updateMission(String missionId, String phase, String objectiveKey) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus emitFeedback(String source, String message) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus renderLayer(String layerId, Map<String, Object> payload) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus screenEvent(String screenId, String eventType, Map<String, Object> payload) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus keybind(String keybindId, String action, Map<String, Object> payload) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus registerCommand(
            String moduleId,
            String commandId,
            String targetSurface,
            String targetBridge,
            Map<String, Object> evidence
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus registerNetworkPacket(
            String moduleId,
            String packetId,
            String surface,
            String sourceRuntimeTarget,
            List<String> consumers,
            Map<String, Object> evidence
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus reloadConfig(
            String moduleId,
            String configId,
            String scope,
            Map<String, Object> evidence
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus reloadResources(
            String moduleId,
            String resourceId,
            String scope,
            Map<String, Object> evidence
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus saveHook(String hookId, Map<String, Object> payload) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus publishRuntimeEvent(
            String sourceModule,
            String eventId,
            Map<String, Object> payload,
            EchoNativeLoadStatus status
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus syncServerClient(String channel, String payload) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }
}
