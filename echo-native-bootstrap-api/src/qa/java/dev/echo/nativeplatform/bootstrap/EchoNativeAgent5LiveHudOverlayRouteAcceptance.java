package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveHudOverlayRouteAcceptance {
    private EchoNativeAgent5LiveHudOverlayRouteAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> hudRoute,
            Map<String, Object> hudOverlaySmoke,
            Map<String, Object> hudEndToEndAcceptance
    ) {
        Map<String, Object> hotkey = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> route = hudRoute == null ? Map.of() : hudRoute;
        Map<String, Object> overlay = hudOverlaySmoke == null ? Map.of() : hudOverlaySmoke;
        Map<String, Object> endToEnd = hudEndToEndAcceptance == null ? Map.of() : hudEndToEndAcceptance;
        String routeEffect = text(route.get("effect"));
        boolean accepted = Boolean.TRUE.equals(route.get("handled"))
                && Boolean.TRUE.equals(route.get("dataBackedAction"))
                && Boolean.TRUE.equals(overlay.get("overlayRendered"))
                && Boolean.TRUE.equals(endToEnd.get("accepted"))
                && routeEffect.startsWith("echohudcore:");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", text(hotkey.get("key")));
        result.put("surface", text(hotkey.get("surface")));
        result.put("destinationMode", "HUD");
        result.put("routeEffect", routeEffect);
        result.put("overlayRendered", Boolean.TRUE.equals(overlay.get("overlayRendered")));
        result.put("hudHealth", endToEnd.getOrDefault("hudHealth", 0));
        result.put("hudHazard", text(endToEnd.get("hudHazard")));
        result.put("cameraMode", text(endToEnd.get("cameraMode")));
        result.put("cinematicCue", text(endToEnd.get("cinematicCue")));
        result.put("effect", accepted
                ? "live_hud_overlay_route:accepted:data_backed_hud:"
                + EchoNativeAgent5UiExpectedValues.hudUpdatedHealth()
                : "live_hud_overlay_route:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", accepted);
        return Map.copyOf(result);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
