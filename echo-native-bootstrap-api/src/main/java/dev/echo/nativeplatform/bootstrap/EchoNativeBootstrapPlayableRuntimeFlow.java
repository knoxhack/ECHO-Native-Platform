package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderLiveInteractionProbeBridge;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.loader.NativeLoaderActivationModuleSnapshot;
import dev.echo.nativeplatform.loader.NativeLoaderLiveProofSidecar;
import dev.echo.nativeplatform.loader.NativeLoaderProductPlayableRuntimeBridge;
import dev.echo.nativeplatform.loader.NativeLoaderProductPlayableRuntimeActions;
import dev.echo.nativeplatform.loader.NativeLoaderProductPlayableRuntimeConfig;
import dev.echo.nativeplatform.loader.NativeLoaderJsonSupport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class EchoNativeBootstrapPlayableRuntimeFlow {
    private final String playableRuntimeKey;
    private final NativeLoaderProductPlayableRuntimeConfig playableRuntimeConfig;
    private final EchoNativeLoaderLiveProof liveProof;
    private final Context context;

    EchoNativeBootstrapPlayableRuntimeFlow(
            EchoNativeBootstrapProductProfile profile,
            EchoNativeProductProfileCore productProfile,
            String playableRuntimeKey,
            Context context
    ) {
        List<String> requiredLiveMutationSurfaces = profile.requiredLiveMutationSurfaces();
        this.playableRuntimeKey = playableRuntimeKey == null ? "" : playableRuntimeKey;
        this.playableRuntimeConfig = new NativeLoaderProductPlayableRuntimeConfig(
                profile,
                new NativeLoaderProductPlayableRuntimeConfig.ProductIdResolver() {
                    @Override
                    public String configuredId(String id) {
                        return productProfile.configuredId(id);
                    }

                    @Override
                    public List<String> configuredIds(List<String> ids) {
                        return productProfile.configuredIds(ids);
                    }
                },
                profile.nativeLoaderAdapterCoreServiceId(),
                profile.nativeLoaderBackendClass(),
                profile.nativeLoaderRuntimeLane(),
                profile.nativeMinecraftRuntimeHostClass(),
                profile.nativeMinecraftRuntimeHostId(),
                requiredLiveMutationSurfaces
        );
        this.liveProof = new EchoNativeLoaderLiveProof(this.playableRuntimeKey, requiredLiveMutationSurfaces);
        this.context = context;
    }

    Map<String, Object> applyLiveClientProbe(Path markerPath, int attempt)
            throws ReflectiveOperationException, IOException {
        return EchoNativeLiveClientProbeRunner.apply(markerPath, attempt, liveClientProbeRunnerContext());
    }

    Map<String, Object> liveClientProbe(
            boolean executed,
            boolean hudSent,
            boolean chatSent,
            int attempt,
            String state,
            String playerClass
    ) {
        return EchoNativeLiveClientProbeRunner.probe(
                context.nativeLoaderActive().getAsBoolean(),
                context.nativeLoaderMainLabel().get(),
                context.nativeLoaderClientLabel().get(),
                executed,
                hudSent,
                chatSent,
                attempt,
                state,
                playerClass
        );
    }

    Map<String, Object> nativeLoaderLiveProof(
            String realMainClass,
            Map<String, Object> liveClientProbe,
            Map<String, Object> nativeClientUiBridge,
            Map<String, Object> productGameplayBridge,
            Map<String, Object> serviceBridge,
            Map<String, Map<String, Object>> nativeActivations
    ) {
        return liveProof.create(
                realMainClass,
                liveClientProbe,
                nativeClientUiBridge,
                productGameplayBridge,
                serviceBridge,
                nativeActivations,
                playableRuntimeConfig.evidenceConfig(),
                NativeLoaderActivationModuleSnapshot::nativeActivationLoaded
        );
    }

    void writeLiveClientProbe(Path markerPath, Map<String, Object> probe) throws IOException {
        NativeLoaderJsonSupport.writeAtomically(liveClientProbePath(markerPath), probe);
    }

    Map<String, Object> writeNativeLoaderLiveProof(Path markerPath, Map<String, Object> proof) throws IOException {
        return liveProof.writeCurrentRunProof(
                markerPath,
                proof,
                playableRuntimeConfig.evidenceConfig(),
                NativeLoaderJsonSupport::writeAtomically
        );
    }

    Path liveClientProbePath(Path markerPath) {
        return sibling(markerPath, "live-client-probe.json");
    }

    private EchoNativeLiveClientProbeRunner.Context liveClientProbeRunnerContext() {
        return new EchoNativeLiveClientProbeRunner.Context(
                playableRuntimeKey,
                context.nativeLoaderActive().getAsBoolean(),
                context.nativeLoaderMainLabel().get(),
                context.nativeLoaderClientLabel().get(),
                context.nativeLoaderWindowTitle().get(),
                context.gameDir().get(),
                context.runtimeClass(),
                playableRuntimeConfig.interactionProbeConfig(),
                playableRuntimeConfig.bridgeConfig(),
                playableRuntimeConfig.actionsConfig(),
                context.clientThreadInvoker(),
                context.fieldReader(),
                context.methodReader(),
                context.itemAction(),
                context.blockPlacement(),
                context.blockAction(),
                context.commandExecutor(),
                context.hostInventoryMutation(),
                context.itemGranter(),
                context.intMethodReader(),
                context.blockSetter(),
                context.hostWorldBlockMutation(),
                context.saveDataWriter(),
                context.hudNotificationPublisher(),
                this::writeLiveClientProbe
        );
    }

    private static Path sibling(Path markerPath, String fileName) {
        return markerPath.toAbsolutePath().normalize().getParent().resolve(fileName);
    }

    record Context(
            BooleanSupplier nativeLoaderActive,
            Supplier<String> nativeLoaderMainLabel,
            Supplier<String> nativeLoaderClientLabel,
            Supplier<String> nativeLoaderWindowTitle,
            Supplier<Path> gameDir,
            EchoNativeLiveClientProbeRunner.RuntimeClassResolver runtimeClass,
            NativeLoaderLiveInteractionProbeBridge.ClientThreadInvoker clientThreadInvoker,
            NativeLoaderLiveInteractionProbeBridge.FieldReader fieldReader,
            NativeLoaderLiveInteractionProbeBridge.MethodReader methodReader,
            NativeLoaderLiveInteractionProbeBridge.ItemAction itemAction,
            NativeLoaderLiveInteractionProbeBridge.BlockPlacement blockPlacement,
            NativeLoaderLiveInteractionProbeBridge.BlockAction blockAction,
            NativeLoaderLiveInteractionProbeBridge.CommandExecutor commandExecutor,
            NativeLoaderProductPlayableRuntimeBridge.HostInventoryMutation hostInventoryMutation,
            NativeLoaderProductPlayableRuntimeActions.ItemGranter itemGranter,
            NativeLoaderProductPlayableRuntimeActions.IntMethodReader intMethodReader,
            NativeLoaderProductPlayableRuntimeActions.BlockSetter blockSetter,
            NativeLoaderProductPlayableRuntimeBridge.HostWorldBlockMutation hostWorldBlockMutation,
            NativeLoaderProductPlayableRuntimeBridge.SaveDataWriter saveDataWriter,
            NativeLoaderProductPlayableRuntimeBridge.HudNotificationPublisher hudNotificationPublisher
    ) {
    }
}
