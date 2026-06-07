package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5LiveSurfaceRouteAcceptance {
    private EchoNativeAgent5LiveSurfaceRouteAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> liveSurfaceAcceptance,
            Map<String, Object> physicalInputAcceptance,
            Map<String, Object> liveSurfaceRenderAcceptance
    ) {
        Map<String, Object> hotkey = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> surface = liveSurfaceAcceptance == null ? Map.of() : liveSurfaceAcceptance;
        Map<String, Object> input = physicalInputAcceptance == null ? Map.of() : physicalInputAcceptance;
        Map<String, Object> render = liveSurfaceRenderAcceptance == null ? Map.of() : liveSurfaceRenderAcceptance;
        String hotkeySurface = normalize(hotkey.get("surface"));
        String liveSurface = normalize(surface.get("currentMode"));
        String renderedSurface = normalize(render.get("surface"));
        boolean accepted = Boolean.TRUE.equals(hotkey.get("observed"))
                && !Boolean.TRUE.equals(hotkey.get("hudOverlay"))
                && Boolean.TRUE.equals(surface.get("accepted"))
                && Boolean.TRUE.equals(input.get("accepted"))
                && Boolean.TRUE.equals(render.get("accepted"))
                && !hotkeySurface.isBlank()
                && hotkeySurface.equals(liveSurface)
                && hotkeySurface.equals(renderedSurface);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", text(hotkey.get("key")));
        result.put("surface", hotkeySurface);
        result.put("liveSurface", liveSurface);
        result.put("renderedSurface", renderedSurface);
        result.put("physicalHotkeyHandled", Boolean.TRUE.equals(hotkey.get("observed"))
                || Boolean.TRUE.equals(hotkey.get("handled")));
        result.put("liveSurfaceAccepted", Boolean.TRUE.equals(surface.get("accepted")));
        result.put("physicalInputAccepted", Boolean.TRUE.equals(input.get("accepted")));
        result.put("liveSurfaceRendered", Boolean.TRUE.equals(render.get("accepted")));
        result.put("effect", accepted
                ? "live_surface_route:accepted:" + hotkeySurface
                : "live_surface_route:rejected:" + (hotkeySurface.isBlank() ? "none" : hotkeySurface));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    private static String normalize(Object value) {
        return text(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
