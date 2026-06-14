package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.loader.NativeLoaderActivationReports;
import dev.echo.nativeplatform.loader.NativeLoaderClasspathSupport;
import dev.echo.nativeplatform.loader.NativeLoaderDeclarationPromotionService;
import dev.echo.nativeplatform.loader.NativeLoaderRuntimeBridgeEnricher;
import dev.echo.nativeplatform.loader.NativeLoaderJsonSupport;
import dev.echo.nativeplatform.loader.NativeLoaderProductGameplayFlow;
import dev.echo.nativeplatform.loader.NativeLoaderBridgeFlow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

final class EchoNativeBootstrapActivationFlow {
    private final String bootstrapMainClass;
    private final EchoNativeBootstrapProductProfile profile;
    private final String nativeGameDirProperty;
    private final String nativeModuleClasspathProperty;
    private final String nativeServiceRegistryPathProperty;
    private final String productGameplayBridgeKey;
    private final String playableRuntimeKey;
    private final String agent7DirectEvidencePathProperty;
    private final String agent7SnapshotForceProperty;
    private final String agent7SnapshotMaxPollsProperty;
    private final String agent7SnapshotPollMillisProperty;
    private final Context context;

    EchoNativeBootstrapActivationFlow(
            String bootstrapMainClass,
            EchoNativeBootstrapProductProfile profile,
            String nativeGameDirProperty,
            String nativeModuleClasspathProperty,
            String nativeServiceRegistryPathProperty,
            String productGameplayBridgeKey,
            String playableRuntimeKey,
            String agent7DirectEvidencePathProperty,
            String agent7SnapshotForceProperty,
            String agent7SnapshotMaxPollsProperty,
            String agent7SnapshotPollMillisProperty,
            Context context
    ) {
        this.bootstrapMainClass = bootstrapMainClass == null ? "" : bootstrapMainClass;
        this.profile = profile;
        this.nativeGameDirProperty = nativeGameDirProperty == null ? "" : nativeGameDirProperty;
        this.nativeModuleClasspathProperty = nativeModuleClasspathProperty == null ? "" : nativeModuleClasspathProperty;
        this.nativeServiceRegistryPathProperty = nativeServiceRegistryPathProperty == null ? "" : nativeServiceRegistryPathProperty;
        this.productGameplayBridgeKey = productGameplayBridgeKey == null ? "" : productGameplayBridgeKey;
        this.playableRuntimeKey = playableRuntimeKey == null ? "" : playableRuntimeKey;
        this.agent7DirectEvidencePathProperty = agent7DirectEvidencePathProperty == null ? "" : agent7DirectEvidencePathProperty;
        this.agent7SnapshotForceProperty = agent7SnapshotForceProperty == null ? "" : agent7SnapshotForceProperty;
        this.agent7SnapshotMaxPollsProperty = agent7SnapshotMaxPollsProperty == null ? "" : agent7SnapshotMaxPollsProperty;
        this.agent7SnapshotPollMillisProperty = agent7SnapshotPollMillisProperty == null ? "" : agent7SnapshotPollMillisProperty;
        this.context = context;
    }

    List<String> activationArgsFromProperties() {
        String gameDir = System.getProperty(nativeGameDirProperty, "");
        if (gameDir.isBlank()) {
            return List.of();
        }
        return List.of("--gameDir", gameDir);
    }

    Path writeActivationMarker(
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints,
            Map<String, Object> runtimeBridge
    ) throws IOException {
        return EchoNativeActivationMarkerWriter.write(
                activationMarkerWriterContext(),
                markerPath,
                packId,
                realMainClass,
                modules,
                nativeEntrypoints,
                runtimeBridge
        );
    }

    private EchoNativeActivationMarkerWriter.Context activationMarkerWriterContext() {
        return new EchoNativeActivationMarkerWriter.Context(
                productGameplayBridgeKey,
                playableRuntimeKey,
                agent7DirectEvidencePathProperty,
                agent7SnapshotForceProperty,
                agent7SnapshotMaxPollsProperty,
                agent7SnapshotPollMillisProperty,
                this::activateNativeModules,
                this::enrichRuntimeBridge,
                context.playableRuntimeFlow()::liveClientProbePath,
                context.playableRuntimeFlow()::liveClientProbe,
                context.productGameplayFlow()::applyLiveHookEvidence,
                context.playableRuntimeFlow()::applyLiveClientProbe,
                context.playableRuntimeFlow()::writeLiveClientProbe,
                this::writeActivationMarkerSnapshot,
                context.runtimeClass()::apply,
                context.nativeModuleClassLoader()
        );
    }

    private void writeActivationMarkerSnapshot(
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints,
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations
    ) throws IOException {
        EchoNativeActivationMarkerSnapshot.write(
                new EchoNativeActivationMarkerSnapshot.Context(
                        bootstrapMainClass,
                        productGameplayBridgeKey,
                        agent7DirectEvidencePathProperty,
                        context.nativeLoaderClientLabel(),
                        context.nativeModuleClassLoader(),
                        context.playableRuntimeFlow()::nativeLoaderLiveProof,
                        context.playableRuntimeFlow()::writeNativeLoaderLiveProof,
                        context.adapterCoreProbe()
                ),
                markerPath,
                packId,
                realMainClass,
                modules,
                nativeEntrypoints,
                runtimeBridge,
                nativeActivations
        );
    }

    private Map<String, Object> enrichRuntimeBridge(
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations,
            String packId,
            List<String> modules
    ) {
        Map<String, Object> enriched = NativeLoaderRuntimeBridgeEnricher.enrich(
                runtimeBridge,
                nativeActivations,
                context.bridgeFlow().runtimeBridgeAggregatorConfig(),
                productGameplayBridgeKey,
                context.productGameplayFlow()::attachHandlers
        );
        return NativeLoaderDeclarationPromotionService.promoteDeclarations(
                enriched,
                nativeActivations,
                context.bridgeFlow()::applyRegistryBridge,
                packId,
                modules
        );
    }

    private Map<String, Map<String, Object>> activateNativeModules(
            String packId,
            Map<String, String> nativeEntrypoints,
            Path markerPath
    ) {
        EchoNativeBootstrapActivationRunner.ActivationRun run = EchoNativeBootstrapActivationRunner.run(
                new EchoNativeBootstrapActivationRunner.Config(
                        packId,
                        nativeEntrypoints,
                        markerPath,
                        EchoNativeBootstrapActivationEnvironment.fixtureRoot(
                                markerPath,
                                System.getProperty(nativeGameDirProperty, "")
                        ),
                        NativeLoaderClasspathSupport.nativeModuleClasspathEntries(nativeModuleClasspathProperty),
                        context.nativeModuleClassLoader().get(),
                        profile.nativeLoaderAdapterCoreServiceId(),
                        (runnerPackId, result, lifecycleEventHost) ->
                                NativeLoaderActivationReports.activationReport(
                                        runnerPackId,
                                        result,
                                        lifecycleEventHost,
                                        profile.requiredNativeLifecycleCallbacks()
                                ),
                        NativeLoaderActivationReports::failureReport,
                        NativeLoaderActivationReports::unloadedEntrypointReport
                )
        );
        EchoNativeBootstrapActivationEnvironment.writeServiceRegistry(
                markerPath,
                run.serviceRegistry(),
                nativeServiceRegistryPathProperty,
                NativeLoaderJsonSupport::writeAtomically
        );
        return run.activations();
    }

    record Context(
            Function<String, String> runtimeClass,
            Supplier<String> nativeLoaderClientLabel,
            Supplier<ClassLoader> nativeModuleClassLoader,
            NativeLoaderBridgeFlow bridgeFlow,
            NativeLoaderProductGameplayFlow productGameplayFlow,
            EchoNativeBootstrapPlayableRuntimeFlow playableRuntimeFlow,
            Supplier<Map<String, Object>> adapterCoreProbe
    ) {
    }
}
