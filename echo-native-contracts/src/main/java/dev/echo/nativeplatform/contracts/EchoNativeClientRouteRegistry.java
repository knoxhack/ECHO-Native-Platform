package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

/**
 * Public Native Loader client route SDK.
 *
 * <p>Addon modules use this contract to declare trusted native client surfaces
 * and action dispatchers without depending on loader internals.</p>
 */
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeClientRouteRegistry {
    EchoNativeClientRouteRegistry NOOP = new EchoNativeClientRouteRegistry() {
    };

    default EchoNativeLoadStatus registerRoute(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Object> config,
            Map<String, Object> evidence,
            boolean trustedMutation
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus registerActions(
            String surfaceType,
            Map<String, Map<String, Object>> actions
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus registerActions(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Map<String, Object>> actions
    ) {
        return registerActions(surfaceType, actions);
    }

    default EchoNativeLoadStatus registerInputBinding(
            String surfaceType,
            String actionId,
            Map<String, Object> binding
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default Map<String, Map<String, List<Map<String, Object>>>> inputBindings() {
        return Map.of();
    }

    default boolean dispatchInputBinding(
            String keyMapping,
            int keyCode,
            String inputType
    ) {
        return dispatchInputBindingStatus(keyMapping, keyCode, inputType) == EchoNativeLoadStatus.MUTATED;
    }

    default boolean dispatchInputBinding(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        return dispatchInputBindingStatus(keyMapping, keyCode, inputType, metadata) == EchoNativeLoadStatus.MUTATED;
    }

    default EchoNativeLoadStatus dispatchInputBindingStatus(
            String keyMapping,
            int keyCode,
            String inputType
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus dispatchInputBindingStatus(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        return dispatchInputBindingStatus(keyMapping, keyCode, inputType);
    }

    default EchoNativeLoadStatus keyInput(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        return dispatchInputBindingStatus(keyMapping, keyCode, inputType, metadata);
    }

    default EchoNativeLoadStatus overlayInput(String surfaceType, String actionId, Map<String, Object> metadata) {
        return dispatchStatus(surfaceType, actionId, metadata);
    }

    default EchoNativeLoadStatus mouseInput(String surfaceType, String actionId, Map<String, Object> metadata) {
        return dispatchStatus(surfaceType, actionId, metadata);
    }

    default EchoNativeLoadStatus focusOverlay(String surfaceType, boolean focused, Map<String, Object> metadata) {
        return dispatchStatus(surfaceType, "native_loader.overlay_focus", metadata);
    }

    default EchoNativeLoadStatus tick(String phase, Map<String, Object> metadata) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus tickRoute(
            String surfaceType,
            String actionId,
            String phase,
            Map<String, Object> metadata
    ) {
        return dispatchStatus(surfaceType, actionId, metadata);
    }

    default EchoNativeLoadStatus renderGuiLayer(String surfaceType, String actionId, Map<String, Object> metadata) {
        return dispatchStatus(surfaceType, actionId, metadata);
    }

    default EchoNativeLoadStatus renderHudLayer(String surfaceType, String actionId, Map<String, Object> metadata) {
        return dispatchStatus(surfaceType, actionId, metadata);
    }

    default EchoNativeLoadStatus registerActionHandler(
            String surfaceType,
            NativeClientRouteActionHandler handler
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default EchoNativeLoadStatus registerActionHandler(
            String surfaceType,
            String handlerId,
            NativeClientRouteActionHandler handler
    ) {
        return registerActionHandler(surfaceType, handler);
    }

    default EchoNativeLoadStatus registerLifecycle(
            String surfaceType,
            NativeClientSurfaceLifecycle lifecycle
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default NativeClientSurfaceLifecycle lifecycle(String surfaceType) {
        return NativeClientSurfaceLifecycle.empty(surfaceType);
    }

    default Map<String, NativeClientSurfaceLifecycle> lifecycles() {
        return Map.of();
    }

    default EchoNativeLoadStatus publishLifecycleEvent(NativeClientSurfaceLifecycleEvent event) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default Map<String, List<NativeClientSurfaceLifecycleEvent>> lifecycleEvents() {
        return Map.of();
    }

    default EchoNativeLoadStatus mountSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        return screenLifecycle(surfaceType, "mount", actionId, metadata);
    }

    default EchoNativeLoadStatus openSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        return screenLifecycle(surfaceType, "open", actionId, metadata);
    }

    default EchoNativeLoadStatus closeSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        return screenLifecycle(surfaceType, "close", actionId, metadata);
    }

    default EchoNativeLoadStatus unmountSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        return screenLifecycle(surfaceType, "unmount", actionId, metadata);
    }

    default EchoNativeLoadStatus screenLifecycle(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        return publishLifecycleEvent(surfaceType, phase, actionId, metadata);
    }

    default EchoNativeLoadStatus publishLifecycleEvent(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        return publishLifecycleEvent(new NativeClientSurfaceLifecycleEvent(
                surfaceType,
                phase,
                actionId,
                metadata
        ));
    }

    default Map<String, Map<String, Object>> mountedSurfaceRoutes() {
        return Map.of();
    }

    default Map<String, Map<String, Object>> visibleSurfaceRoutes() {
        return Map.of();
    }

    default boolean dispatch(String surfaceType, String actionId) {
        return dispatchStatus(surfaceType, actionId) == EchoNativeLoadStatus.MUTATED;
    }

    default boolean dispatch(String surfaceType, String actionId, Map<String, Object> metadata) {
        return dispatchStatus(surfaceType, actionId, metadata) == EchoNativeLoadStatus.MUTATED;
    }

    default EchoNativeLoadStatus dispatchStatus(String surfaceType, String actionId) {
        return dispatchStatus(surfaceType, actionId, Map.of());
    }

    default EchoNativeLoadStatus dispatchStatus(String surfaceType, String actionId, Map<String, Object> metadata) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    default boolean hasRoute(String surfaceType) {
        return false;
    }

    default boolean hasTrustedRoute(String surfaceType) {
        return false;
    }

    @FunctionalInterface
    interface NativeClientRouteActionHandler {
        boolean dispatch(NativeClientRouteActionContext context);
    }

    record NativeClientRouteActionContext(
            String surfaceType,
            String actionId,
            Map<String, Object> route,
            Map<String, Object> action,
            Map<String, Object> metadata
    ) {
        public NativeClientRouteActionContext {
            surfaceType = surfaceType == null ? "" : surfaceType.trim();
            actionId = actionId == null ? "" : actionId.trim();
            route = route == null ? Map.of() : Map.copyOf(route);
            action = action == null ? Map.of() : Map.copyOf(action);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    record NativeClientSurfaceLifecycle(
            String surfaceType,
            boolean renderLifecycle,
            boolean screenLifecycle,
            boolean inputLifecycle,
            boolean visibleByDefault,
            boolean mountedByDefault,
            List<String> renderPhases,
            List<String> screenPhases,
            List<String> inputPhases,
            Map<String, Object> metadata
    ) {
        public NativeClientSurfaceLifecycle {
            surfaceType = surfaceType == null ? "" : surfaceType.trim();
            renderPhases = renderPhases == null ? List.of() : List.copyOf(renderPhases);
            screenPhases = screenPhases == null ? List.of() : List.copyOf(screenPhases);
            inputPhases = inputPhases == null ? List.of() : List.copyOf(inputPhases);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }

        public static NativeClientSurfaceLifecycle empty(String surfaceType) {
            return new NativeClientSurfaceLifecycle(
                    surfaceType,
                    false,
                    false,
                    false,
                    false,
                    false,
                    List.of(),
                    List.of(),
                    List.of(),
                    Map.of()
            );
        }

        public Map<String, Object> toEvidence() {
            return Map.of(
                    "surfaceType", surfaceType,
                    "renderLifecycle", renderLifecycle,
                    "screenLifecycle", screenLifecycle,
                    "inputLifecycle", inputLifecycle,
                    "visibleByDefault", visibleByDefault,
                    "mountedByDefault", mountedByDefault,
                    "renderPhases", renderPhases,
                    "screenPhases", screenPhases,
                    "inputPhases", inputPhases,
                    "metadata", metadata
            );
        }
    }

    record NativeClientSurfaceLifecycleEvent(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        public NativeClientSurfaceLifecycleEvent {
            surfaceType = surfaceType == null ? "" : surfaceType.trim();
            phase = phase == null ? "" : phase.trim();
            actionId = actionId == null ? "" : actionId.trim();
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }
}
