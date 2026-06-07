package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class NativeLoaderProductGameplayFlow {
    public static final String SERVICE_ID = "echo.native.product_gameplay_flow";
    private final EchoNativeBootstrapProductProfile profile;
    private final List<String> requiredWorldLiveHooks;
    private final Supplier<ClassLoader> moduleClassLoader;
    private final NativeLoaderProductGameplayBridge gameplayBridge;
    private final NativeLoaderProductGameplayContentDiscovery contentDiscovery;

    public NativeLoaderProductGameplayFlow(
            EchoNativeBootstrapProductProfile profile,
            String nativeModuleClasspathProperty,
            Supplier<ClassLoader> moduleClassLoader
    ) {
        this.profile = profile;
        this.requiredWorldLiveHooks = profile.requiredAgent7WorldLiveHooks();
        this.moduleClassLoader = moduleClassLoader;
        this.gameplayBridge = new NativeLoaderProductGameplayBridge(
                profile,
                profile.requiredGameplayHandlerEvents()
        );
        this.contentDiscovery = new NativeLoaderProductGameplayContentDiscovery(
                profile,
                nativeModuleClasspathProperty
        );
    }

    public Map<String, Object> apply(String packId) {
        return gameplayBridge.apply(
                packId,
                contentDiscovery::discover,
                () -> worldHostHookEvidence(Set.of(), false),
                NativeLoaderProductGameplayFlow::failureMessage
        );
    }

    public Map<String, Object> attachHandlers(
            Map<String, Object> eventBridge,
            Map<String, Object> productGameplayBridge
    ) {
        return gameplayBridge.attachHandlers(eventBridge, productGameplayBridge);
    }

    public Map<String, Object> applyLiveHookEvidence(
            Map<String, Object> existing,
            Map<String, Object> liveClientProbe
    ) {
        return gameplayBridge.applyLiveHookEvidence(
                existing,
                liveClientProbe,
                profile.nativeUiEventClientTick(),
                profile.nativeUiEventCommandExecution(),
                this::worldHostHookEvidence,
                this::recordNativeRuntimeHookEvidence
        );
    }

    private Map<String, Object> recordNativeRuntimeHookEvidence(Map<String, Object> liveClientProbe) {
        return NativeLoaderAgent7LiveHookEvidence.recordNativeRuntimeHookEvidence(
                liveClientProbe,
                requiredWorldLiveHooks,
                moduleClassLoader.get()
        );
    }

    private Map<String, Object> worldHostHookEvidence(
            Set<String> candidateSignals,
            boolean minecraftRuntimeAccessed
    ) {
        return NativeLoaderAgent7LiveHookEvidence.worldHostHookEvidence(
                candidateSignals,
                minecraftRuntimeAccessed,
                requiredWorldLiveHooks
        );
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
