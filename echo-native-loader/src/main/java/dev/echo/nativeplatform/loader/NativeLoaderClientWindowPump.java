package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderClientWindowPump {
    public static final String SERVICE_ID = "echo.native.client_window_pump";
    public static final String SOURCE = "native_loader_window_pump";

    private final NativeLoaderClientUiHost uiHost;

    public NativeLoaderClientWindowPump(NativeLoaderClientUiHost uiHost) {
        if (uiHost == null) {
            throw new IllegalArgumentException("uiHost is required");
        }
        this.uiHost = uiHost;
    }

    public NativeLoaderClientUiHost uiHost() {
        return uiHost;
    }

    public static Map<String, Object> liveClientProbeMarkerFields(
            Map<String, Object> liveClientProbe,
            String fallbackLabel
    ) {
        Map<String, Object> probe = liveClientProbe == null ? Map.of() : liveClientProbe;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeClientWindowPumpMarkerServiceId", SERVICE_ID);
        fields.put("nativeBootstrapLiveClientProbeExecuted", Boolean.TRUE.equals(probe.get("executed")));
        fields.put("nativeBootstrapHudProbeSent", Boolean.TRUE.equals(probe.get("hudProbeSent")));
        fields.put("nativeBootstrapChatProbeSent", Boolean.TRUE.equals(probe.get("chatProbeSent")));
        fields.put("nativeLoaderTextLabelApplied", Boolean.TRUE.equals(probe.get("nativeLoaderTextLabelApplied")));
        fields.put("windowTitleApplied", Boolean.TRUE.equals(probe.get("windowTitleApplied")));
        fields.put("hudLabelSent", Boolean.TRUE.equals(probe.get("hudLabelSent")));
        fields.put("chatLabelSent", Boolean.TRUE.equals(probe.get("chatLabelSent")));
        fields.put("labelText", String.valueOf(probe.getOrDefault("labelText", fallbackLabel == null ? "" : fallbackLabel)));
        return Map.copyOf(fields);
    }

    public EchoNativeLoadStatus mountScreen(
            String surfaceType,
            String actionId,
            String screenClass,
            int screenWidth,
            int screenHeight,
            Map<String, Object> metadata
    ) {
        return uiHost.mountSurface(surfaceType, actionId, screenMetadata(
                metadata,
                "mount",
                screenClass,
                screenWidth,
                screenHeight
        ));
    }

    public EchoNativeLoadStatus openScreen(
            String surfaceType,
            String actionId,
            String screenClass,
            int screenWidth,
            int screenHeight,
            String focusedSurface,
            Map<String, Object> metadata
    ) {
        Map<String, Object> enriched = screenMetadata(metadata, "open", screenClass, screenWidth, screenHeight);
        if (focusedSurface != null && !focusedSurface.isBlank()) {
            enriched.put("focusedSurface", focusedSurface);
        }
        return uiHost.openSurface(surfaceType, actionId, enriched);
    }

    public EchoNativeLoadStatus closeScreen(
            String surfaceType,
            String actionId,
            String screenClass,
            String closeReason,
            Map<String, Object> metadata
    ) {
        Map<String, Object> enriched = screenMetadata(metadata, "close", screenClass, -1, -1);
        if (closeReason != null && !closeReason.isBlank()) {
            enriched.put("closeReason", closeReason);
        }
        return uiHost.closeSurface(surfaceType, actionId, enriched);
    }

    public EchoNativeLoadStatus unmountScreen(
            String surfaceType,
            String actionId,
            String screenClass,
            String closeReason,
            Map<String, Object> metadata
    ) {
        Map<String, Object> enriched = screenMetadata(metadata, "unmount", screenClass, -1, -1);
        if (closeReason != null && !closeReason.isBlank()) {
            enriched.put("closeReason", closeReason);
        }
        return uiHost.unmountSurface(surfaceType, actionId, enriched);
    }

    public EchoNativeLoadStatus keyInput(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        Map<String, Object> enriched = metadata(metadata);
        enriched.put("inputSource", SOURCE);
        enriched.put("keyMapping", keyMapping == null ? "" : keyMapping);
        enriched.put("keyCode", keyCode);
        enriched.put("inputType", inputType == null ? "" : inputType);
        return uiHost.keyInput(keyMapping, keyCode, inputType, enriched);
    }

    public EchoNativeLoadStatus mouseInput(
            String surfaceType,
            String actionId,
            double mouseX,
            double mouseY,
            int button,
            String phase,
            Map<String, Object> metadata
    ) {
        Map<String, Object> enriched = metadata(metadata);
        enriched.put("mouseSource", SOURCE);
        enriched.put("mouseX", mouseX);
        enriched.put("mouseY", mouseY);
        enriched.put("button", button);
        if (phase != null && !phase.isBlank()) {
            enriched.put("phase", phase);
        }
        return uiHost.mouseInput(surfaceType, actionId, enriched);
    }

    public EchoNativeLoadStatus focusOverlay(
            String surfaceType,
            boolean focused,
            String focusedSurface,
            String previousSurface,
            Map<String, Object> metadata
    ) {
        Map<String, Object> enriched = metadata(metadata);
        enriched.put("focusSource", SOURCE);
        if (focusedSurface != null && !focusedSurface.isBlank()) {
            enriched.put("focusedSurface", focusedSurface);
        }
        if (previousSurface != null && !previousSurface.isBlank()) {
            enriched.put("previousSurface", previousSurface);
        }
        return uiHost.focusOverlay(surfaceType, focused, enriched);
    }

    public EchoNativeLoadStatus renderGuiLayer(
            String surfaceType,
            String actionId,
            int screenWidth,
            int screenHeight,
            double mouseX,
            double mouseY,
            float partialTick,
            Map<String, Object> metadata
    ) {
        return uiHost.renderGuiLayer(surfaceType, actionId, frameMetadata(
                metadata,
                "gui_layer",
                screenWidth,
                screenHeight,
                mouseX,
                mouseY,
                partialTick
        ));
    }

    public EchoNativeLoadStatus renderHudLayer(
            String surfaceType,
            String actionId,
            int screenWidth,
            int screenHeight,
            float partialTick,
            Map<String, Object> metadata
    ) {
        return uiHost.renderHudLayer(surfaceType, actionId, frameMetadata(
                metadata,
                "hud_layer",
                screenWidth,
                screenHeight,
                -1,
                -1,
                partialTick
        ));
    }

    public Map<String, Object> builtInProductRendererFrame(
            String surfaceType,
            String actionId,
            int screenWidth,
            int screenHeight,
            double mouseX,
            double mouseY,
            float partialTick,
            Map<String, Object> metadata
    ) {
        String safeSurfaceType = surfaceType == null ? "" : surfaceType.trim();
        String safeActionId = actionId == null ? "" : actionId.trim();
        Map<String, Object> frameMetadata = frameMetadata(
                metadata,
                "builtin_product_renderer",
                screenWidth,
                screenHeight,
                mouseX,
                mouseY,
                partialTick
        );
        frameMetadata.put("builtinProductRendererSource", SOURCE);
        Map<String, Object> surfaceState = NativeLoaderClientUiHost.builtInProductSurfaceState()
                .getOrDefault(safeSurfaceType, Map.of());
        EchoNativeLoadStatus status = Boolean.TRUE.equals(surfaceState.get("routeDrivenRendererState"))
                && surfaceState.get("renderModel") instanceof Map<?, ?>
                ? EchoNativeLoadStatus.MUTATED
                : EchoNativeLoadStatus.REGISTERED;
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("serviceId", SERVICE_ID);
        frame.put("source", SOURCE);
        frame.put("surfaceType", safeSurfaceType);
        frame.put("actionId", safeActionId);
        frame.put("status", status.name());
        frame.put("nativeWindowPumpRendererFrame", true);
        frame.put("routeDrivenRendererState", Boolean.TRUE.equals(surfaceState.get("routeDrivenRendererState")));
        frame.put("nativeProductUiReady", Boolean.TRUE.equals(surfaceState.get("nativeProductUiReady")));
        frame.put("renderModel", surfaceState.getOrDefault("renderModel", Map.of()));
        frame.put("surfaceState", surfaceState);
        frame.put("frameMetadata", Map.copyOf(frameMetadata));
        return Map.copyOf(frame);
    }

    public EchoNativeLoadStatus tick(String phase, long frameIndex, float partialTick, Map<String, Object> metadata) {
        Map<String, Object> enriched = metadata(metadata);
        enriched.put("tickSource", SOURCE);
        enriched.put("frameIndex", frameIndex >= Integer.MIN_VALUE && frameIndex <= Integer.MAX_VALUE
                ? (int) frameIndex
                : frameIndex);
        enriched.put("partialTick", partialTick);
        return uiHost.tick(phase, enriched);
    }

    public EchoNativeLoadStatus dispatchRoute(String surfaceType, String actionId, Map<String, Object> metadata) {
        Map<String, Object> enriched = metadata(metadata);
        enriched.put("routeDispatchSource", SOURCE);
        return uiHost.dispatchRouteStatus(surfaceType, actionId, enriched);
    }

    private static Map<String, Object> screenMetadata(
            Map<String, Object> metadata,
            String phase,
            String screenClass,
            int screenWidth,
            int screenHeight
    ) {
        Map<String, Object> enriched = metadata(metadata);
        enriched.put("screenSource", SOURCE);
        enriched.put("screenPhase", phase == null ? "" : phase);
        if (screenClass != null && !screenClass.isBlank()) {
            enriched.put("screenClass", screenClass);
        }
        if (screenWidth >= 0) {
            enriched.put("screenWidth", screenWidth);
        }
        if (screenHeight >= 0) {
            enriched.put("screenHeight", screenHeight);
        }
        return enriched;
    }

    private static Map<String, Object> frameMetadata(
            Map<String, Object> metadata,
            String service,
            int screenWidth,
            int screenHeight,
            double mouseX,
            double mouseY,
            float partialTick
    ) {
        Map<String, Object> enriched = metadata(metadata);
        enriched.put("frameSource", SOURCE);
        enriched.put("service", service == null ? "" : service);
        enriched.put("screenWidth", screenWidth);
        enriched.put("screenHeight", screenHeight);
        if (mouseX >= 0) {
            enriched.put("mouseX", mouseX);
        }
        if (mouseY >= 0) {
            enriched.put("mouseY", mouseY);
        }
        enriched.put("partialTick", partialTick);
        return enriched;
    }

    private static Map<String, Object> metadata(Map<String, Object> metadata) {
        Map<String, Object> enriched = new LinkedHashMap<>();
        if (metadata != null) {
            enriched.putAll(metadata);
        }
        enriched.putIfAbsent("source", "native_loader_client_ui_host");
        enriched.put("windowPumpSource", SOURCE);
        enriched.put("nativeClientRouteProcess", true);
        enriched.put("neoForgeEventOwnershipRequired", false);
        return enriched;
    }
}
