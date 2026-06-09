package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5HudOverlayEndToEndAcceptance {
    private EchoNativeAgent5HudOverlayEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> hudOverlaySmoke,
            Map<String, Object> hudUpdateSmoke,
            Map<String, Object> cameraCinematicSmoke
    ) {
        Map<String, Object> hotkey = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> overlay = hudOverlaySmoke == null ? Map.of() : hudOverlaySmoke;
        Map<String, Object> update = hudUpdateSmoke == null ? Map.of() : hudUpdateSmoke;
        Map<String, Object> camera = cameraCinematicSmoke == null ? Map.of() : cameraCinematicSmoke;
        boolean runtimeMutationAccepted = Boolean.TRUE.equals(update.get("runtimeMutationAccepted"))
                && "native.ui.hud_refresh".equals(update.get("runtimeActionId"))
                && "client_tick".equals(update.get("eventName"));
        boolean hudDataObserved = Boolean.TRUE.equals(overlay.get("passed"))
                && Boolean.TRUE.equals(overlay.get("overlayRendered"))
                && Boolean.TRUE.equals(update.get("passed"))
                && Integer.valueOf(EchoNativeAgent5UiExpectedValues.hudUpdatedHealth()).equals(update.get("hudHealth"))
                && "hud:update:health_hazard_mission".equals(update.get("effect"))
                && runtimeMutationAccepted;
        boolean accepted = hudDataObserved
                && Boolean.TRUE.equals(camera.get("passed"))
                && "over_shoulder".equals(camera.get("cameraMode"))
                && ("camera_cinematic:frame:" + EchoNativeAgent5UiExpectedValues.terminal().get("title"))
                .equals(camera.get("effect"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", string(hotkey.get("key")));
        result.put("surface", string(hotkey.get("surface")));
        result.put("hudDataObserved", hudDataObserved);
        result.put("overlayRendered", Boolean.TRUE.equals(overlay.get("overlayRendered")));
        result.put("hudHealth", update.getOrDefault("hudHealth", 0));
        result.put("hudHazard", string(update.get("hudHazard")));
        result.put("cameraMode", string(camera.get("cameraMode")));
        result.put("cinematicCue", string(camera.get("cinematicCue")));
        result.put("runtimeMutationAccepted", runtimeMutationAccepted);
        result.put("runtimeActionId", string(update.get("runtimeActionId")));
        result.put("eventName", string(update.get("eventName")));
        result.put("effect", accepted ? EchoNativeAgent5UiExpectedValues.hudOverlayEffect()
                : "hud_overlay_end_to_end:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", accepted);
        return Map.copyOf(result);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
