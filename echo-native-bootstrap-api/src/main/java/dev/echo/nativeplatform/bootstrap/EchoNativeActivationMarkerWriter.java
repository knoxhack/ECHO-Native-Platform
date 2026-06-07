package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderProductPlayableRuntimeEvidence;

import dev.echo.nativeplatform.loader.NativeLoaderAgent7LiveHookEvidence;
import dev.echo.nativeplatform.loader.NativeLoaderAgent7LiveHookSnapshotBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveClientProbeBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveClientProbeSeed;
import dev.echo.nativeplatform.loader.NativeLoaderNoHandoffClientUiBridge;
import dev.echo.nativeplatform.loader.NativeLoaderRegistryCreativeVisibilityBridge;
import dev.echo.nativeplatform.loader.NativeLoaderJsonSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

final class EchoNativeActivationMarkerWriter {
    private EchoNativeActivationMarkerWriter() {
    }

    static Path write(
            Context context,
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints,
            Map<String, Object> runtimeBridge
    ) throws IOException {
        Files.createDirectories(markerPath.toAbsolutePath().normalize().getParent());
        NativeLoaderAgent7LiveHookEvidence.configureDirectEvidencePath(
                markerPath,
                context.agent7DirectEvidencePathProperty()
        );
        Map<String, Map<String, Object>> nativeActivations =
                context.nativeModuleActivator().activate(packId, nativeEntrypoints, markerPath);
        Map<String, Object> enrichedRuntimeBridge = new LinkedHashMap<>(
                context.runtimeBridgeEnricher().enrich(runtimeBridge, nativeActivations, packId, modules));
        Map<String, Object> initialLiveClientProbe =
                NativeLoaderLiveClientProbeSeed.initialProbe(
                        markerPath,
                        context.liveClientProbePath()::resolve,
                        NativeLoaderJsonSupport::parse,
                        context.liveClientProbeFactory()::create
                );
        initialLiveClientProbe.put("disabled", false);
        boolean preservedExistingLiveProbe =
                Boolean.TRUE.equals(initialLiveClientProbe.get("preservedExistingLiveEvidence"));
        if (realMainClass.isBlank() && !preservedExistingLiveProbe) {
            initialLiveClientProbe.put("state", "minecraft_handoff_not_requested");
            initialLiveClientProbe.put(
                    "summary",
                    "Native bootstrap ran in no-handoff mode; live Minecraft client/player evidence was not requested."
            );
            initialLiveClientProbe.put("clientAttachmentBlockedReason", "minecraft_handoff_not_requested");
        }
        enrichedRuntimeBridge.put("liveClientProbe", initialLiveClientProbe);
        if (preservedExistingLiveProbe) {
            Map<String, Object> liveProductGameplayBridge = context.productGameplayHookEvidence().apply(
                    object(enrichedRuntimeBridge.get(context.productGameplayBridgeKey())),
                    initialLiveClientProbe
            );
            liveProductGameplayBridge = NativeLoaderProductPlayableRuntimeEvidence.applyLiveEvidence(
                    liveProductGameplayBridge,
                    initialLiveClientProbe,
                    context.playableRuntimeKey()
            );
            enrichedRuntimeBridge.put(context.productGameplayBridgeKey(), liveProductGameplayBridge);
        }
        boolean preservedExistingLiveUi = false;
        if (realMainClass.isBlank()) {
            Map<String, Object> initialLiveUiBridge =
                    NativeLoaderNoHandoffClientUiBridge.initialBridge(markerPath, NativeLoaderJsonSupport::parse);
            preservedExistingLiveUi = Boolean.TRUE.equals(initialLiveUiBridge.get("preservedExistingLiveUiEvidence"));
            if (preservedExistingLiveUi) {
                enrichedRuntimeBridge.put("nativeClientUiBridge", initialLiveUiBridge);
            } else {
                NativeLoaderNoHandoffClientUiBridge.markGap(enrichedRuntimeBridge);
            }
        }
        context.markerSnapshotWriter().write(
                markerPath,
                packId,
                realMainClass,
                modules,
                nativeEntrypoints,
                enrichedRuntimeBridge,
                nativeActivations
        );
        if (!preservedExistingLiveProbe && !realMainClass.isBlank()) {
            context.liveClientProbeWriter().write(markerPath, initialLiveClientProbe);
        } else if (realMainClass.isBlank() && !preservedExistingLiveProbe) {
            context.liveClientProbeWriter().write(markerPath, initialLiveClientProbe);
        }
        if (realMainClass.isBlank() && !preservedExistingLiveUi) {
            NativeLoaderNoHandoffClientUiBridge.writeSidecar(
                    markerPath,
                    object(enrichedRuntimeBridge.get("nativeClientUiBridge")),
                    NativeLoaderJsonSupport::writeAtomically
            );
        }
        NativeLoaderRegistryCreativeVisibilityBridge.start(
                markerPath,
                packId,
                realMainClass,
                modules,
                nativeEntrypoints,
                enrichedRuntimeBridge,
                nativeActivations,
                context.runtimeClassResolver()::resolve,
                context.markerSnapshotWriter()::write
        );
        if (!realMainClass.isBlank()) {
            NativeLoaderLiveClientProbeBridge.start(
                    markerPath,
                    packId,
                    realMainClass,
                    modules,
                    nativeEntrypoints,
                    enrichedRuntimeBridge,
                    nativeActivations,
                    context.productGameplayBridgeKey(),
                    context.liveClientProbeApplier()::apply,
                    context.liveClientProbeFactory()::create,
                    (productBridge, probe) -> NativeLoaderProductPlayableRuntimeEvidence.applyLiveEvidence(
                            context.productGameplayHookEvidence().apply(productBridge, probe),
                            probe,
                            context.playableRuntimeKey()
                    ),
                    context.liveClientProbeWriter()::write,
                    context.markerSnapshotWriter()::write
            );
        }
        NativeLoaderAgent7LiveHookSnapshotBridge.start(
                markerPath,
                packId,
                realMainClass,
                modules,
                nativeEntrypoints,
                enrichedRuntimeBridge,
                nativeActivations,
                context.productGameplayBridgeKey(),
                context.agent7SnapshotForceProperty(),
                context.agent7SnapshotMaxPollsProperty(),
                context.agent7SnapshotPollMillisProperty(),
                () -> NativeLoaderAgent7LiveHookEvidence.readExactWorldHookEvidence(
                        null,
                        context.agent7DirectEvidencePathProperty(),
                        context.nativeModuleClassLoader().get(),
                        NativeLoaderJsonSupport::parse
                ),
                NativeLoaderAgent7LiveHookEvidence::worldHostHookEvidenceFromExactSnapshot,
                (productBridge, evidenceMarkerPath) -> NativeLoaderAgent7LiveHookEvidence.applyExactWorldHookEvidence(
                        productBridge,
                        evidenceMarkerPath,
                        context.agent7DirectEvidencePathProperty(),
                        context.nativeModuleClassLoader().get(),
                        NativeLoaderJsonSupport::parse
                ),
                context.markerSnapshotWriter()::write
        );
        if (!realMainClass.isBlank()) {
            EchoNativeLiveUiBridge.start(
                    markerPath,
                    packId,
                    modules,
                    enrichedRuntimeBridge,
                    () -> context.markerSnapshotWriter().write(
                            markerPath,
                            packId,
                            realMainClass,
                            modules,
                            nativeEntrypoints,
                            enrichedRuntimeBridge,
                            nativeActivations
                    )
            );
        }
        return markerPath;
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
    }

    record Context(
            String productGameplayBridgeKey,
            String playableRuntimeKey,
            String agent7DirectEvidencePathProperty,
            String agent7SnapshotForceProperty,
            String agent7SnapshotMaxPollsProperty,
            String agent7SnapshotPollMillisProperty,
            NativeModuleActivator nativeModuleActivator,
            RuntimeBridgeEnricher runtimeBridgeEnricher,
            LiveClientProbePath liveClientProbePath,
            LiveClientProbeFactory liveClientProbeFactory,
            ProductGameplayHookEvidence productGameplayHookEvidence,
            LiveClientProbeApplier liveClientProbeApplier,
            LiveClientProbeWriter liveClientProbeWriter,
            MarkerSnapshotWriter markerSnapshotWriter,
            RuntimeClassResolver runtimeClassResolver,
            Supplier<ClassLoader> nativeModuleClassLoader
    ) {
    }

    @FunctionalInterface
    interface NativeModuleActivator {
        Map<String, Map<String, Object>> activate(
                String packId,
                Map<String, String> nativeEntrypoints,
                Path markerPath
        );
    }

    @FunctionalInterface
    interface RuntimeBridgeEnricher {
        Map<String, Object> enrich(
                Map<String, Object> runtimeBridge,
                Map<String, Map<String, Object>> nativeActivations,
                String packId,
                List<String> modules
        );
    }

    @FunctionalInterface
    interface LiveClientProbePath {
        Path resolve(Path markerPath);
    }

    @FunctionalInterface
    interface LiveClientProbeFactory {
        Map<String, Object> create(
                boolean executed,
                boolean hudSent,
                boolean chatSent,
                int attempt,
                String state,
                String playerClass
        );
    }

    @FunctionalInterface
    interface ProductGameplayHookEvidence {
        Map<String, Object> apply(Map<String, Object> productBridge, Map<String, Object> liveClientProbe);
    }

    @FunctionalInterface
    interface LiveClientProbeApplier {
        Map<String, Object> apply(Path markerPath, int attempt) throws ReflectiveOperationException, IOException;
    }

    @FunctionalInterface
    interface LiveClientProbeWriter {
        void write(Path markerPath, Map<String, Object> probe) throws IOException;
    }

    @FunctionalInterface
    interface MarkerSnapshotWriter {
        void write(
                Path markerPath,
                String packId,
                String realMainClass,
                List<String> modules,
                Map<String, String> nativeEntrypoints,
                Map<String, Object> runtimeBridge,
                Map<String, Map<String, Object>> nativeActivations
        ) throws IOException;
    }

    @FunctionalInterface
    interface RuntimeClassResolver {
        String resolve(String suffix);
    }
}
