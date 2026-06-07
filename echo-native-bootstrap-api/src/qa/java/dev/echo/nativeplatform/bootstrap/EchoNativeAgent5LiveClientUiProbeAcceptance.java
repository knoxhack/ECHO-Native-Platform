package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveClientUiProbeAcceptance {
    private static final List<String> REQUIRED_SURFACES = List.of(
            "TERMINAL",
            "INDEX",
            "LENS",
            "MISSION_LOG",
            "SETTINGS",
            "PAUSE",
            "RECOVERY",
            "HOLOMAP",
            "WIKI",
            "MAIN_MENU",
            "HUD"
    );

    private EchoNativeAgent5LiveClientUiProbeAcceptance() {
    }

    public static Map<String, Object> assess(boolean scheduled, boolean executed, List<Map<String, Object>> routes) {
        List<Map<String, Object>> probeRoutes = routes == null ? List.of() : routes;
        List<String> surfaces = probeRoutes.stream()
                .map(route -> String.valueOf(route.get("surface")))
                .toList();
        boolean accepted = scheduled
                && executed
                && surfaces.equals(REQUIRED_SURFACES)
                && probeRoutes.stream().allMatch(EchoNativeAgent5LiveClientUiProbeAcceptance::routeVisible);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("scheduled", scheduled);
        result.put("executed", executed);
        result.put("routeCount", probeRoutes.size());
        result.put("surfaces", surfaces);
        result.put("effect", accepted
                ? "live_client_ui_probe:accepted:" + probeRoutes.size()
                : "live_client_ui_probe:rejected:" + probeRoutes.size());
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        List<Map<String, Object>> routes = REQUIRED_SURFACES.stream()
                .map(surface -> Map.<String, Object>of(
                        "surface", surface,
                        "liveSurfaceAccepted", true,
                        "liveSurfaceRendered", true,
                        "screenOpened", !"HUD".equals(surface) && !"LENS".equals(surface),
                        "overlayRendered", "LENS".equals(surface),
                        "hudFrameRendered", "HUD".equals(surface)
                ))
                .toList();
        Map<String, Object> accepted = assess(true, true, routes);
        Map<String, Object> rejectedNotExecuted = assess(true, false, routes);
        Map<String, Object> rejectedMissingSurface = assess(true, true, routes.subList(0, routes.size() - 1));
        List<Map<String, Object>> routesWithoutLensOverlay = routes.stream()
                .map(route -> {
                    if (!"LENS".equals(route.get("surface"))) {
                        return route;
                    }
                    Map<String, Object> rejected = new LinkedHashMap<>(route);
                    rejected.put("overlayRendered", false);
                    return Map.copyOf(rejected);
                })
                .toList();
        List<Map<String, Object>> routesWithoutHudFrame = routes.stream()
                .map(route -> {
                    if (!"HUD".equals(route.get("surface"))) {
                        return route;
                    }
                    Map<String, Object> rejected = new LinkedHashMap<>(route);
                    rejected.put("hudFrameRendered", false);
                    return Map.copyOf(rejected);
                })
                .toList();
        Map<String, Object> rejectedNoLensOverlay = assess(true, true, routesWithoutLensOverlay);
        Map<String, Object> rejectedNoHudFrame = assess(true, true, routesWithoutHudFrame);
        return Map.of(
                "liveClientUiProbeAcceptanceSmokeClass",
                EchoNativeAgent5LiveClientUiProbeAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNotExecuted", rejectedNotExecuted,
                "rejectedMissingSurface", rejectedMissingSurface,
                "rejectedNoLensOverlay", rejectedNoLensOverlay,
                "rejectedNoHudFrame", rejectedNoHudFrame,
                "passed", Boolean.TRUE.equals(accepted.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNotExecuted.get("accepted"))
                        && Boolean.FALSE.equals(rejectedMissingSurface.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoLensOverlay.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoHudFrame.get("accepted")),
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static boolean routeVisible(Map<String, Object> route) {
        boolean surfaceAccepted = Boolean.TRUE.equals(route.get("liveSurfaceAccepted"));
        boolean surfaceRendered = Boolean.TRUE.equals(route.get("liveSurfaceRendered"));
        if (!surfaceAccepted || !surfaceRendered) {
            return false;
        }
        String surface = String.valueOf(route.get("surface"));
        if ("HUD".equals(surface)) {
            return Boolean.TRUE.equals(route.get("hudFrameRendered"));
        }
        if ("LENS".equals(surface)) {
            return Boolean.TRUE.equals(route.get("overlayRendered"));
        }
        return Boolean.TRUE.equals(route.get("screenOpened"));
    }
}
