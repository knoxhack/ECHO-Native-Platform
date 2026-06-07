package dev.echo.nativeplatform.contracts;

import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeSurfaceModuleEntrypoint extends EchoNativeModuleEntrypoint {
    Map<String, Object> describeNativeSurfaces(Map<String, String> context);

    @Override
    default void discover(EchoNativeModuleLoadContext context) {
        context.attribute("nativeEntrypointBridge", "direct_native_surface_module_entrypoint");
        context.attribute("nativeEntrypointDelegateClass", getClass().getName());
        context.attribute("nativeModuleEntrypoint", true);
        context.attribute("nativeActivationEntrypoint", false);
        EchoNativeActivationSurfaceRegistrar.recordLifecycleCallback(context, "onModuleDiscovered");
    }

    @Override
    default void registerServices(EchoNativeModuleLoadContext context) {
        Map<String, Object> activation = activation(context);
        EchoNativeActivationSurfaceRegistrar.registerServices(
                context,
                this,
                activation,
                "native_surface_module_entrypoint",
                "direct_native_surface_module_entrypoint"
        );
    }

    @Override
    default void registerContent(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.registerContent(context, activation(context));
    }

    @Override
    default void commonSetup(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.recordLifecycleCallback(context, "onCommonSetup");
    }

    @Override
    default void clientSetup(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.recordLifecycleCallback(context, "onClientSetup");
    }

    @Override
    default void serverSetup(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.recordLifecycleCallback(context, "onWorldReady");
        EchoNativeActivationSurfaceRegistrar.recordLifecycleCallback(context, "onPlayerReady");
    }

    @Override
    default void ready(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.ready(context);
    }

    @Override
    default void shutdown(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.recordLifecycleCallback(context, "onRuntimeShutdown");
    }

    private Map<String, Object> activation(EchoNativeModuleLoadContext context) {
        return EchoNativeActivationSurfaceRegistrar.activation(
                context,
                () -> describeNativeSurfaces(EchoNativeActivationSurfaceRegistrar.bridgeContext(context))
        );
    }
}
