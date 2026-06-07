package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.Map;

/**
 * Typed bridge from the Native Loader client UI host into a live client/render pipeline.
 *
 * <p>The headless UI host can record module-declared surfaces for package and
 * module-readiness checks. Full client runtime parity attaches an implementation
 * of this bridge so surface registrations are dispatched into a real native
 * client instead of remaining only loader-side declarations.</p>
 */
public interface NativeLoaderLiveClientBridge {
    NativeLoaderLiveClientBridge UNATTACHED = new NativeLoaderLiveClientBridge() {
    };

    default boolean attached() {
        return false;
    }

    default String bridgeId() {
        return "native_loader:unattached_live_client_bridge";
    }

    default boolean firstClassNativeClientRouteTable() {
        return false;
    }

    default boolean nativeClientRouteProcess() {
        return false;
    }

    default boolean releaseClientRouteTrusted() {
        return false;
    }

    default boolean clientRouteMutationSupported() {
        return false;
    }

    default boolean firstClassNativeClientRenderPipeline() {
        return false;
    }

    default boolean nativeClientRenderProcess() {
        return false;
    }

    default boolean releaseClientRenderTrusted() {
        return false;
    }

    default boolean clientRenderMutationSupported() {
        return false;
    }

    default boolean nativeLoaderOwnsClientHostServices() {
        return false;
    }

    default boolean neoForgeClientEventsCompatibilityAdaptersOnly() {
        return true;
    }

    default EchoNativeLoadStatus registerSurface(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Object> config
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default Map<String, Object> surfaceRegistrationEvidence(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Object> config
    ) {
        return Map.of();
    }

    default EchoNativeLoadStatus dispatchRoute(
            String surfaceType,
            String actionId,
            Map<String, Object> metadata
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus dispatchInputBinding(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus tick(
            String phase,
            Map<String, Object> metadata
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus screenLifecycle(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus overlayFocus(
            String surfaceType,
            boolean focused,
            Map<String, Object> metadata
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus mouseInput(
            String surfaceType,
            String actionId,
            Map<String, Object> metadata
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus overlayInput(
            String surfaceType,
            String actionId,
            Map<String, Object> metadata
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus renderGuiLayer(
            String surfaceType,
            String actionId,
            Map<String, Object> metadata
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus renderHudLayer(
            String surfaceType,
            String actionId,
            Map<String, Object> metadata
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default Map<String, Object> clientHostServiceEvidence() {
        return Map.of(
                "bridgeId", bridgeId(),
                "attached", attached(),
                "hostServiceDispatchSupported", false,
                "nativeLoaderOwnsClientHostServices", nativeLoaderOwnsClientHostServices(),
                "neoForgeClientEventsCompatibilityAdaptersOnly", neoForgeClientEventsCompatibilityAdaptersOnly()
        );
    }
}
