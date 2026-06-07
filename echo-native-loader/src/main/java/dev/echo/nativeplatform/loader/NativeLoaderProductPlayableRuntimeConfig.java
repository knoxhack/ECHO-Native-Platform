package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.util.List;

public final class NativeLoaderProductPlayableRuntimeConfig {
    private static final String PLAYABLE_RUNTIME_ACTIONS_PROPERTY = "echo.native.playableRuntimeActions";
    private static final String LIVE_INTERACTION_ACTIONS_PROPERTY = "echo.native.liveInteractionProbeActions";

    private final EchoNativeBootstrapProductProfile profile;
    private final ProductIdResolver productIds;
    private final String adapterCoreServiceId;
    private final String nativeLoaderBackendClass;
    private final String nativeLoaderRuntimeLane;
    private final String nativeMinecraftRuntimeHostClass;
    private final String nativeMinecraftRuntimeHostId;
    private final List<String> requiredLiveMutationSurfaces;

    public NativeLoaderProductPlayableRuntimeConfig(
            EchoNativeBootstrapProductProfile profile,
            ProductIdResolver productIds,
            String adapterCoreServiceId,
            String nativeLoaderBackendClass,
            String nativeLoaderRuntimeLane,
            String nativeMinecraftRuntimeHostClass,
            String nativeMinecraftRuntimeHostId,
            List<String> requiredLiveMutationSurfaces
    ) {
        this.profile = profile;
        this.productIds = productIds == null ? ProductIdResolver.identity() : productIds;
        this.adapterCoreServiceId = adapterCoreServiceId == null ? "" : adapterCoreServiceId;
        this.nativeLoaderBackendClass = nativeLoaderBackendClass == null ? "" : nativeLoaderBackendClass;
        this.nativeLoaderRuntimeLane = nativeLoaderRuntimeLane == null ? "" : nativeLoaderRuntimeLane;
        this.nativeMinecraftRuntimeHostClass = nativeMinecraftRuntimeHostClass == null ? "" : nativeMinecraftRuntimeHostClass;
        this.nativeMinecraftRuntimeHostId = nativeMinecraftRuntimeHostId == null ? "" : nativeMinecraftRuntimeHostId;
        this.requiredLiveMutationSurfaces = requiredLiveMutationSurfaces == null
                ? List.of()
                : List.copyOf(requiredLiveMutationSurfaces);
    }

    public NativeLoaderProductPlayableRuntimeEvidence.Config evidenceConfig() {
        return new NativeLoaderProductPlayableRuntimeEvidence.Config(
                adapterCoreServiceId,
                profile.namespace(),
                nativeLoaderBackendClass,
                nativeLoaderRuntimeLane,
                nativeMinecraftRuntimeHostClass,
                nativeMinecraftRuntimeHostId,
                profile.nativeCompatibilityDelegateClass(),
                profile.nativeCompatibilityDelegateId(),
                profile.nativePlayableHudLedgerTarget()
        );
    }

    public NativeLoaderProductPlayableRuntimeBridge.Config bridgeConfig() {
        return new NativeLoaderProductPlayableRuntimeBridge.Config(
                profile.namespace(),
                profile.nativeGameplayDisplayName(),
                profile.nativeGameplayPackId(),
                Boolean.getBoolean(PLAYABLE_RUNTIME_ACTIONS_PROPERTY),
                productIds.configuredIds(profile.nativePlayableStarterToolItemIds()),
                productIds.configuredId(profile.nativePlayableProofMarkerBlockId()),
                productIds.configuredId(profile.nativePlayableStarterRegionTerrainBlockId()),
                productIds.configuredId(profile.nativePlayableStarterRegionSurfaceBlockId()),
                productIds.configuredId(profile.nativePlayableStarterRegionCoreBlockId()),
                productIds.configuredIds(profile.nativePlayableStarterRegionFeatureBlockIds()),
                requiredLiveMutationSurfaces,
                evidenceConfig()
        );
    }

    public NativeLoaderLiveInteractionProbeBridge.Config interactionProbeConfig() {
        return new NativeLoaderLiveInteractionProbeBridge.Config(
                Boolean.getBoolean(LIVE_INTERACTION_ACTIONS_PROPERTY),
                productIds.configuredId(profile.nativeInteractionProbeItemId()),
                productIds.configuredId(profile.nativeInteractionProbePlacementBlockId()),
                productIds.configuredId(profile.nativeInteractionProbePlacementFallbackBlockId()),
                productIds.configuredId(profile.nativeInteractionProbeBlockUseId()),
                productIds.configuredId(profile.nativeInteractionProbeEntityItemId()),
                profile.nativeInteractionProbeCommand()
        );
    }

    public NativeLoaderProductPlayableRuntimeActions.Config actionsConfig() {
        return new NativeLoaderProductPlayableRuntimeActions.Config(
                productIds.configuredIds(profile.nativePlayableStarterToolItemIds()),
                profile.nativePlayableStarterCommands(),
                productIds.configuredId(profile.nativePlayableStarterRegionTerrainBlockId()),
                productIds.configuredId(profile.nativePlayableStarterRegionSurfaceBlockId()),
                productIds.configuredId(profile.nativePlayableStarterRegionCoreBlockId()),
                productIds.configuredIds(profile.nativePlayableStarterRegionFeatureBlockIds())
        );
    }

    public interface ProductIdResolver {
        String configuredId(String id);

        List<String> configuredIds(List<String> ids);

        static ProductIdResolver identity() {
            return new ProductIdResolver() {
                @Override
                public String configuredId(String id) {
                    return id == null ? "" : id;
                }

                @Override
                public List<String> configuredIds(List<String> ids) {
                    return ids == null ? List.of() : List.copyOf(ids);
                }
            };
        }
    }
}
