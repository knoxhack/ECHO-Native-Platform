package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.util.List;
import java.util.Map;

final class EchoNativePlayableModuleGate {
    private static final List<String> REQUIRED_LIVE_MUTATION_SURFACES = List.of(
            "inventory",
            "world_blocks",
            "save_data",
            "hud"
    );

    private EchoNativePlayableModuleGate() {
    }

    static boolean nativeProductModulesReady(
            Map<String, Object> activationMarker,
            int descriptorCount,
            int activeModuleCount
    ) {
        return !activationMarker.isEmpty()
                && descriptorCount > 0
                && activeModuleCount >= descriptorCount
                && Boolean.TRUE.equals(activationMarker.get("adapterCoreRuntimeBridgeActive"))
                && productLoopReady(activationMarker)
                && Boolean.TRUE.equals(activationMarker.get("nativeLiveGameplayHandlersAttached"))
                && Boolean.TRUE.equals(activationMarker.get("nativeWorldLiveHostHooksVerified"))
                && liveRuntimeProofAccepted(activationMarker);
    }

    static Map<String, Object> nativeLoaderLiveProof(Map<String, Object> activationMarker) {
        return EchoNativeJson.asObject(activationMarker.get("nativeLoaderLiveProof"));
    }

    static boolean liveRuntimeProofAccepted(Map<String, Object> activationMarker) {
        Map<String, Object> proof = nativeLoaderLiveProof(activationMarker);
        return "MUTATED".equals(String.valueOf(proof.getOrDefault("status", "")))
                && Boolean.TRUE.equals(proof.get("complete"))
                && Boolean.TRUE.equals(proof.get("gameplayReadyClaimAllowed"))
                && Boolean.TRUE.equals(proof.get("liveClientGameplayReadyClaimAllowed"))
                && Boolean.TRUE.equals(proof.get("nativeMutationLedgerRecorded"))
                && Boolean.TRUE.equals(proof.get("requiredMutationSurfacesMutated"))
                && Boolean.TRUE.equals(proof.get("livePlayerOrWorldMutation"))
                && Boolean.TRUE.equals(proof.get("liveSaveDataWrite"))
                && Boolean.TRUE.equals(proof.get("liveHudNotificationEmitted"))
                && containsAllRequiredMutationSurfaces(proof.get("mutationLedgerMutatedSurfaces"));
    }

    private static boolean productLoopReady(Map<String, Object> activationMarker) {
        return Boolean.TRUE.equals(activationMarker.get("nativeProductLoopReady"))
                || Boolean.TRUE.equals(activationMarker.get("nativeFirstPlayableLoopReady"));
    }

    private static boolean containsAllRequiredMutationSurfaces(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return false;
        }
        for (String requiredSurface : REQUIRED_LIVE_MUTATION_SURFACES) {
            boolean found = false;
            for (Object raw : iterable) {
                if (requiredSurface.equals(String.valueOf(raw))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}
