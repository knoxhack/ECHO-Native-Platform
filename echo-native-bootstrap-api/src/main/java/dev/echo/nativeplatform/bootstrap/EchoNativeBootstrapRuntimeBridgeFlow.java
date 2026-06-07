package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.loader.NativeLoaderEnvironmentFlow;
import dev.echo.nativeplatform.loader.NativeLoaderResourcePackFlow;
import dev.echo.nativeplatform.loader.NativeLoaderRuntimeBridgeSeed;
import dev.echo.nativeplatform.loader.NativeLoaderWorldStartupFlow;
import dev.echo.nativeplatform.loader.NativeLoaderProductGameplayFlow;
import dev.echo.nativeplatform.loader.NativeLoaderBridgeFlow;

import java.util.List;
import java.util.Map;

final class EchoNativeBootstrapRuntimeBridgeFlow {
    private final NativeLoaderRuntimeBridgeSeed runtimeBridgeSeed;
    private final NativeLoaderEnvironmentFlow loaderEnvironmentFlow;
    private final NativeLoaderResourcePackFlow resourcePackFlow;
    private final NativeLoaderBridgeFlow bridgeFlow;
    private final NativeLoaderProductGameplayFlow productGameplayFlow;
    private final EchoNativeBootstrapPlayableRuntimeFlow playableRuntimeFlow;
    private final NativeLoaderWorldStartupFlow worldStartupFlow;

    EchoNativeBootstrapRuntimeBridgeFlow(
            EchoNativeBootstrapProductProfile profile,
            String productGameplayBridgeKey,
            NativeLoaderEnvironmentFlow loaderEnvironmentFlow,
            NativeLoaderResourcePackFlow resourcePackFlow,
            NativeLoaderBridgeFlow bridgeFlow,
            NativeLoaderProductGameplayFlow productGameplayFlow,
            EchoNativeBootstrapPlayableRuntimeFlow playableRuntimeFlow
    ) {
        this.runtimeBridgeSeed = new NativeLoaderRuntimeBridgeSeed(profile, productGameplayBridgeKey);
        this.loaderEnvironmentFlow = loaderEnvironmentFlow;
        this.resourcePackFlow = resourcePackFlow;
        this.bridgeFlow = bridgeFlow;
        this.productGameplayFlow = productGameplayFlow;
        this.playableRuntimeFlow = playableRuntimeFlow;
        this.worldStartupFlow = new NativeLoaderWorldStartupFlow("echo.native.gameDir");
    }

    Map<String, Object> apply(
            String packId,
            List<String> remainingArgs,
            List<String> modules,
            Map<String, String> nativeEntrypoints
    ) {
        Map<String, Object> resourceBridge = resourcePackFlow.apply(packId, remainingArgs, modules);
        Map<String, Object> worldStartupBridge = worldStartupFlow.apply(packId, remainingArgs);
        Map<String, Object> registryBridge = bridgeFlow.applyRegistryBridge(packId, modules);
        Map<String, Object> productGameplayBridge = productGameplayFlow.apply(packId);
        return runtimeBridgeSeed.create(
                packId,
                modules,
                nativeEntrypoints,
                resourceBridge,
                worldStartupBridge,
                registryBridge,
                productGameplayBridge,
                loaderEnvironmentFlow.nativeLoaderActive(),
                loaderEnvironmentFlow.nativeLoaderMainLabel(),
                loaderEnvironmentFlow.nativeLoaderClientLabel(),
                playableRuntimeFlow::nativeLoaderLiveProof
        );
    }
}
