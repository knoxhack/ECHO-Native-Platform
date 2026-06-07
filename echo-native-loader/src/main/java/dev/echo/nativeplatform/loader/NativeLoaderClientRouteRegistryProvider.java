package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientSurfaceLifecycle;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientSurfaceLifecycleEvent;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.List;
import java.util.Map;

public final class NativeLoaderClientRouteRegistryProvider implements EchoNativeClientRouteRegistry {
    private static final NativeLoaderClientUiHost SDK_UI_HOST = new NativeLoaderClientUiHost();

    public static Map<String, Object> sdkRouteHostEvidence() {
        return SDK_UI_HOST.routeHostEvidence();
    }

    @Override
    public EchoNativeLoadStatus registerRoute(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Object> config,
            Map<String, Object> evidence,
            boolean trustedMutation
    ) {
        return NativeLoaderClientRouteTable.registerRoute(
                moduleId,
                surfaceId,
                surfaceType,
                config,
                evidence,
                trustedMutation
        );
    }

    @Override
    public EchoNativeLoadStatus registerActions(
            String surfaceType,
            Map<String, Map<String, Object>> actions
    ) {
        return NativeLoaderClientRouteTable.registerActions(surfaceType, actions);
    }

    @Override
    public EchoNativeLoadStatus registerActions(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Map<String, Object>> actions
    ) {
        return NativeLoaderClientRouteTable.registerActions(moduleId, surfaceId, surfaceType, actions);
    }

    @Override
    public EchoNativeLoadStatus registerInputBinding(
            String surfaceType,
            String actionId,
            Map<String, Object> binding
    ) {
        return NativeLoaderClientRouteTable.registerInputBinding(surfaceType, actionId, binding);
    }

    @Override
    public Map<String, Map<String, List<Map<String, Object>>>> inputBindings() {
        return NativeLoaderClientRouteTable.inputBindings();
    }

    @Override
    public boolean dispatchInputBinding(
            String keyMapping,
            int keyCode,
            String inputType
    ) {
        return dispatchInputBindingStatus(keyMapping, keyCode, inputType) == EchoNativeLoadStatus.MUTATED;
    }

    @Override
    public boolean dispatchInputBinding(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        return dispatchInputBindingStatus(keyMapping, keyCode, inputType, metadata) == EchoNativeLoadStatus.MUTATED;
    }

    @Override
    public EchoNativeLoadStatus dispatchInputBindingStatus(
            String keyMapping,
            int keyCode,
            String inputType
    ) {
        return SDK_UI_HOST.dispatchInputBindingStatus(keyMapping, keyCode, inputType);
    }

    @Override
    public EchoNativeLoadStatus dispatchInputBindingStatus(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        return SDK_UI_HOST.dispatchInputBindingStatus(keyMapping, keyCode, inputType, metadata);
    }

    @Override
    public EchoNativeLoadStatus keyInput(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        return SDK_UI_HOST.keyInput(keyMapping, keyCode, inputType, metadata);
    }

    @Override
    public EchoNativeLoadStatus overlayInput(String surfaceType, String actionId, Map<String, Object> metadata) {
        return SDK_UI_HOST.overlayInput(surfaceType, actionId, metadata);
    }

    @Override
    public EchoNativeLoadStatus mouseInput(String surfaceType, String actionId, Map<String, Object> metadata) {
        return SDK_UI_HOST.mouseInput(surfaceType, actionId, metadata);
    }

    @Override
    public EchoNativeLoadStatus focusOverlay(String surfaceType, boolean focused, Map<String, Object> metadata) {
        return SDK_UI_HOST.focusOverlay(surfaceType, focused, metadata);
    }

    @Override
    public EchoNativeLoadStatus tick(String phase, Map<String, Object> metadata) {
        return SDK_UI_HOST.tick(phase, metadata);
    }

    @Override
    public EchoNativeLoadStatus tickRoute(
            String surfaceType,
            String actionId,
            String phase,
            Map<String, Object> metadata
    ) {
        return SDK_UI_HOST.tickRoute(surfaceType, actionId, phase, metadata);
    }

    @Override
    public EchoNativeLoadStatus renderGuiLayer(String surfaceType, String actionId, Map<String, Object> metadata) {
        return SDK_UI_HOST.renderGuiLayer(surfaceType, actionId, metadata);
    }

    @Override
    public EchoNativeLoadStatus renderHudLayer(String surfaceType, String actionId, Map<String, Object> metadata) {
        return SDK_UI_HOST.renderHudLayer(surfaceType, actionId, metadata);
    }

    @Override
    public EchoNativeLoadStatus registerActionHandler(
            String surfaceType,
            NativeClientRouteActionHandler handler
    ) {
        return registerActionHandler(surfaceType, "", handler);
    }

    @Override
    public EchoNativeLoadStatus registerActionHandler(
            String surfaceType,
            String handlerId,
            NativeClientRouteActionHandler handler
    ) {
        if (handler == null) {
            return EchoNativeLoadStatus.FAILED;
        }
        return NativeLoaderClientRouteTable.registerActionHandler(
                surfaceType,
                handlerId,
                context -> handler.dispatch(new NativeClientRouteActionContext(
                        context.surfaceType(),
                        context.actionId(),
                        context.route(),
                        context.action(),
                        context.metadata()
                ))
        );
    }

    @Override
    public EchoNativeLoadStatus registerLifecycle(
            String surfaceType,
            NativeClientSurfaceLifecycle lifecycle
    ) {
        return NativeLoaderClientRouteTable.registerLifecycle(surfaceType, lifecycle);
    }

    @Override
    public NativeClientSurfaceLifecycle lifecycle(String surfaceType) {
        return NativeLoaderClientRouteTable.lifecycle(surfaceType);
    }

    @Override
    public Map<String, NativeClientSurfaceLifecycle> lifecycles() {
        return NativeLoaderClientRouteTable.lifecycles();
    }

    @Override
    public EchoNativeLoadStatus publishLifecycleEvent(NativeClientSurfaceLifecycleEvent event) {
        return SDK_UI_HOST.screenLifecycleEvent(
                event.surfaceType(),
                event.phase(),
                event.actionId(),
                event.metadata()
        );
    }

    @Override
    public Map<String, List<NativeClientSurfaceLifecycleEvent>> lifecycleEvents() {
        return NativeLoaderClientRouteTable.lifecycleEvents();
    }

    @Override
    public EchoNativeLoadStatus mountSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        return SDK_UI_HOST.mountSurface(surfaceType, actionId, metadata);
    }

    @Override
    public EchoNativeLoadStatus openSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        return SDK_UI_HOST.openSurface(surfaceType, actionId, metadata);
    }

    @Override
    public EchoNativeLoadStatus closeSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        return SDK_UI_HOST.closeSurface(surfaceType, actionId, metadata);
    }

    @Override
    public EchoNativeLoadStatus unmountSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        return SDK_UI_HOST.unmountSurface(surfaceType, actionId, metadata);
    }

    @Override
    public EchoNativeLoadStatus screenLifecycle(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        return SDK_UI_HOST.screenLifecycleEvent(surfaceType, phase, actionId, metadata);
    }

    @Override
    public EchoNativeLoadStatus publishLifecycleEvent(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        return SDK_UI_HOST.screenLifecycleEvent(surfaceType, phase, actionId, metadata);
    }

    @Override
    public Map<String, Map<String, Object>> mountedSurfaceRoutes() {
        return NativeLoaderClientRouteTable.mountedSurfaceRoutes();
    }

    @Override
    public Map<String, Map<String, Object>> visibleSurfaceRoutes() {
        return NativeLoaderClientRouteTable.visibleSurfaceRoutes();
    }

    @Override
    public boolean dispatch(String surfaceType, String actionId) {
        return dispatchStatus(surfaceType, actionId) == EchoNativeLoadStatus.MUTATED;
    }

    @Override
    public boolean dispatch(String surfaceType, String actionId, Map<String, Object> metadata) {
        return dispatchStatus(surfaceType, actionId, metadata) == EchoNativeLoadStatus.MUTATED;
    }

    @Override
    public EchoNativeLoadStatus dispatchStatus(String surfaceType, String actionId) {
        return SDK_UI_HOST.dispatchRouteStatus(surfaceType, actionId, Map.of());
    }

    @Override
    public EchoNativeLoadStatus dispatchStatus(String surfaceType, String actionId, Map<String, Object> metadata) {
        return SDK_UI_HOST.dispatchRouteStatus(surfaceType, actionId, metadata);
    }

    @Override
    public boolean hasRoute(String surfaceType) {
        return NativeLoaderClientRouteTable.hasRoute(surfaceType);
    }

    @Override
    public boolean hasTrustedRoute(String surfaceType) {
        return NativeLoaderClientRouteTable.hasTrustedRoute(surfaceType);
    }
}
