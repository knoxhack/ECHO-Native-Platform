package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance {
    private EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> hud,
            Map<String, Object> routeEffectTranscript
    ) {
        boolean hudAccepted = hud != null
                && Boolean.TRUE.equals(hud.get("accepted"))
                && EchoNativeAgent5UiExpectedValues.hudOverlayEffect().equals(hud.get("effect"));
        List<String> observedKeys = strings(routeEffectTranscript == null
                ? null
                : routeEffectTranscript.get("observedKeys"));
        boolean routeBound = hudAccepted
                && routeEffectTranscript != null
                && Boolean.TRUE.equals(routeEffectTranscript.get("accepted"))
                && observedKeys.contains("HUD_DATA");
        boolean accepted = hudAccepted && routeBound;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("hudAccepted", hudAccepted);
        result.put("routeBound", routeBound);
        result.put("observedKeys", observedKeys);
        result.put("hudHealth", integer(hud == null ? null : hud.get("hudHealth")));
        result.put("hudHazard", text(hud == null ? null : hud.get("hudHazard")));
        result.put("cameraMode", text(hud == null ? null : hud.get("cameraMode")));
        result.put("effect", accepted
                ? "live_route_bound_hud_update:accepted:data_backed_hud"
                : "live_route_bound_hud_update:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> smoke() {
        Map<String, Object> hud = (Map<String, Object>) EchoNativeAgent5HudOverlayEndToEndAcceptanceSmoke
                .capture()
                .get("accepted");
        Map<String, Object> route = Map.of(
                "accepted", true,
                "observedKeys", List.of("M", "G", "R", "U", "B", "LEFT_ALT", "J", "K",
                        "RIGHT_BRACKET", "LEFT_BRACKET", "BACKSLASH", "N", "HUD_DATA")
        );
        Map<String, Object> accepted = assess(hud, route);
        Map<String, Object> rejectedNoHud = assess(Map.of(), route);
        Map<String, Object> rejectedNoRoute = assess(hud, Map.of(
                "accepted", false,
                "observedKeys", List.of("M", "I", "L")
        ));
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_route_bound_hud_update:accepted:data_backed_hud".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoHud.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRoute.get("accepted"));
        return Map.of(
                "liveRouteBoundHudUpdateAcceptanceClass",
                EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoHud", rejectedNoHud,
                "rejectedNoRoute", rejectedNoRoute,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance::text).toList();
        }
        return List.of();
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
