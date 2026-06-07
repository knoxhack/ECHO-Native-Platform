package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.loader.NativeLoaderEntityRegistryBridge;
import dev.echo.nativeplatform.loader.NativeLoaderWorldStartupFlow;
import dev.echo.nativeplatform.loader.NativeLoaderProductClientRouteBootstrap;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class EchoNativeBootstrapEntrypointRunner {
    private EchoNativeBootstrapEntrypointRunner() {
    }

    static void run(String[] args, Context context) throws Exception {
        EchoNativeBootstrapArguments parsed = EchoNativeBootstrapArguments.parse(args);
        if (parsed.markerPath().isBlank()) {
            throw new IllegalArgumentException("--echo-marker is required");
        }
        context.ensureVanillaBootstrap().getAsBoolean();
        NativeLoaderProductClientRouteBootstrap.ClientRouteBootstrapReport clientRouteBootstrap =
                NativeLoaderProductClientRouteBootstrap.bootstrapFirstPartyClientRoutes(
                        EchoNativeBootstrapMain.nativeClientModuleClassLoader());
        if (clientRouteBootstrap.status() != EchoNativeLoadStatus.MUTATED
                || clientRouteBootstrap.mutatedCount() != clientRouteBootstrap.attemptedCount()) {
            throw new IllegalStateException(
                    "Native Loader production client route bootstrap failed before product runtime handoff: "
                            + clientRouteBootstrap.toEvidence());
        }
        Map<String, Object> runtimeBridge = context.runtimeBridge().apply(
                parsed.packId(),
                parsed.remainingArgs(),
                parsed.modules(),
                parsed.nativeEntrypoints()
        );
        runtimeBridge = new LinkedHashMap<>(runtimeBridge);
        runtimeBridge.put("nativeProductClientRouteBootstrap", clientRouteBootstrap.toEvidence());
        Path markerPath = Path.of(parsed.markerPath());
        context.activationMarker().write(
                markerPath,
                parsed.packId(),
                parsed.realMainClass(),
                parsed.modules(),
                parsed.nativeEntrypoints(),
                runtimeBridge
        );
        if (NativeLoaderWorldStartupFlow.blocksHandoff(runtimeBridge)) {
            throw new IllegalStateException(NativeLoaderWorldStartupFlow.blockMessage(runtimeBridge));
        }
        if (!parsed.realMainClass().isBlank() && parsed.handoff()) {
            NativeLoaderEntityRegistryBridge.markDeferred(runtimeBridge, context.entityRegistryBridgeConfig().get());
            context.resourcePackMount().install(
                    parsed.packId(),
                    object(runtimeBridge.get("resourceBridge")),
                    parsed.modules(),
                    markerPath
            );
            context.activationMarker().write(
                    markerPath,
                    parsed.packId(),
                    parsed.realMainClass(),
                    parsed.modules(),
                    parsed.nativeEntrypoints(),
                    runtimeBridge
            );
            Class<?> mainClass = Class.forName(parsed.realMainClass());
            mainClass.getMethod("main", String[].class).invoke(null, (Object) parsed.remainingArgs().toArray(String[]::new));
        }
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
            BooleanSupplier ensureVanillaBootstrap,
            RuntimeBridgeApplier runtimeBridge,
            ActivationMarkerWriter activationMarker,
            Supplier<NativeLoaderEntityRegistryBridge.Config> entityRegistryBridgeConfig,
            ResourcePackMountInstaller resourcePackMount
    ) {
    }

    @FunctionalInterface
    interface RuntimeBridgeApplier {
        Map<String, Object> apply(
                String packId,
                List<String> remainingArgs,
                List<String> modules,
                Map<String, String> nativeEntrypoints
        );
    }

    @FunctionalInterface
    interface ActivationMarkerWriter {
        void write(
                Path markerPath,
                String packId,
                String realMainClass,
                List<String> modules,
                Map<String, String> nativeEntrypoints,
                Map<String, Object> runtimeBridge
        ) throws Exception;
    }

    @FunctionalInterface
    interface ResourcePackMountInstaller {
        void install(
                String packId,
                Map<String, Object> resourceBridge,
                List<String> modules,
                Path markerPath
        );
    }
}
