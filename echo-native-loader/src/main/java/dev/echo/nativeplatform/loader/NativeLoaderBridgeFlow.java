package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.util.List;
import java.util.Map;

public final class NativeLoaderBridgeFlow {
    public static final String SERVICE_ID = "echo.native.bridge_flow";

    private final String nativeGameDirProperty;
    private final NativeLoaderBridgeConfigs configs;
    private final NativeLoaderRegistryBridge.Context registryBridgeContext;

    public NativeLoaderBridgeFlow(
            EchoNativeBootstrapProductProfile profile,
            String nativeModuleClasspathProperty,
            String nativeGameDirProperty,
            Class<?> bootstrapClass,
            NativeLoaderRegistryBridge.VanillaBootstrapper vanillaBootstrapper,
            NativeLoaderRegistryBridge.RuntimeClassResolver runtimeClassResolver,
            NativeLoaderRegistryBridge.ContentIdSupplier itemIdSupplier,
            NativeLoaderRegistryBridge.ContentIdSupplier blockIdSupplier,
            NativeLoaderRegistryContentBridge.NativeItemFactory itemFactory,
            NativeLoaderRegistryContentBridge.NativeBlockFactory blockFactory
    ) {
        this.nativeGameDirProperty = nativeGameDirProperty == null ? "" : nativeGameDirProperty;
        this.configs = new NativeLoaderBridgeConfigs(
                profile,
                nativeModuleClasspathProperty,
                bootstrapClass
        );
        this.registryBridgeContext = new NativeLoaderRegistryBridge.Context(
                vanillaBootstrapper,
                runtimeClassResolver,
                itemIdSupplier,
                blockIdSupplier,
                itemFactory,
                blockFactory
        );
    }

    public Map<String, Object> applyRegistryBridge(String packId, List<String> modules) {
        return applyRegistryBridge(packId, modules, List.of(), List.of());
    }

    public Map<String, Object> applyRegistryBridge(
            String packId,
            List<String> modules,
            List<String> sdkItemIds,
            List<String> sdkBlockIds
    ) {
        return applyRegistryBridge(packId, modules, sdkItemIds, sdkBlockIds, List.of());
    }

    public Map<String, Object> applyRegistryBridge(
            String packId,
            List<String> modules,
            List<String> sdkItemIds,
            List<String> sdkBlockIds,
            List<String> sdkCreativeTabIds
    ) {
        return applyRegistryBridge(packId, modules, sdkItemIds, sdkBlockIds, sdkCreativeTabIds, List.of());
    }

    public Map<String, Object> applyRegistryBridge(
            String packId,
            List<String> modules,
            List<String> sdkItemIds,
            List<String> sdkBlockIds,
            List<String> sdkCreativeTabIds,
            List<Map<String, Object>> sdkCreativeTabDeclarations
    ) {
        return applyRegistryBridge(
                packId,
                modules,
                sdkItemIds,
                sdkBlockIds,
                sdkCreativeTabIds,
                sdkCreativeTabDeclarations,
                List.of()
        );
    }

    public Map<String, Object> applyRegistryBridge(
            String packId,
            List<String> modules,
            List<String> sdkItemIds,
            List<String> sdkBlockIds,
            List<String> sdkCreativeTabIds,
            List<Map<String, Object>> sdkCreativeTabDeclarations,
            List<Map<String, Object>> sdkRegistryDeclarations
    ) {
        return NativeLoaderRegistryBridge.apply(
                packId,
                modules,
                registryBridgeConfig(),
                registryBridgeContext,
                sdkItemIds,
                sdkBlockIds,
                sdkCreativeTabIds,
                sdkCreativeTabDeclarations,
                sdkRegistryDeclarations
        );
    }

    public NativeLoaderRuntimeBridgeAggregator.Config runtimeBridgeAggregatorConfig() {
        return configs.runtimeBridgeAggregatorConfig();
    }

    public NativeLoaderEntityRegistryBridge.Config entityRegistryBridgeConfig() {
        return configs.entityRegistryBridgeConfig();
    }

    public NativeLoaderGeneratedContentBridge.Config generatedContentBridgeConfig() {
        return configs.generatedContentBridgeConfig();
    }

    private NativeLoaderRegistryBridge.Config registryBridgeConfig() {
        return configs.registryBridgeConfig(nativeGameDirProperty);
    }
}
