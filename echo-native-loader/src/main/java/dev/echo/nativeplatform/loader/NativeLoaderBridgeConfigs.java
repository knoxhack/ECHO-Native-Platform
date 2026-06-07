package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

public final class NativeLoaderBridgeConfigs {
    public static final String SERVICE_ID = "echo.native.bridge_configs";
    private final EchoNativeBootstrapProductProfile profile;
    private final String nativeModuleClasspathProperty;
    private final Class<?> bootstrapClass;

    public NativeLoaderBridgeConfigs(
            EchoNativeBootstrapProductProfile profile,
            String nativeModuleClasspathProperty,
            Class<?> bootstrapClass
    ) {
        this.profile = profile;
        this.nativeModuleClasspathProperty = nativeModuleClasspathProperty;
        this.bootstrapClass = bootstrapClass;
    }

    public NativeLoaderRegistryBridge.Config registryBridgeConfig(String nativeGameDirProperty) {
        return new NativeLoaderRegistryBridge.Config(
                profile,
                nativeGameDirProperty
        );
    }

    public NativeLoaderRuntimeBridgeAggregator.Config runtimeBridgeAggregatorConfig() {
        return new NativeLoaderRuntimeBridgeAggregator.Config(profile);
    }

    public NativeLoaderEntityRegistryBridge.Config entityRegistryBridgeConfig() {
        return new NativeLoaderEntityRegistryBridge.Config(profile);
    }

    public NativeLoaderGeneratedContentBridge.Config generatedContentBridgeConfig() {
        return new NativeLoaderGeneratedContentBridge.Config(
                nativeModuleClasspathProperty,
                bootstrapClass
        );
    }
}
