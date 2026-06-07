package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeAgent5PhysicalRouteRequirements {
    private static final List<RouteSpec> ROUTES = List.of(
            new RouteSpec("M", "TERMINAL", "screen", "terminal.open"),
            new RouteSpec("G", "INDEX", "screen", "index.open"),
            new RouteSpec("R", "INDEX", "action", "index.recipe"),
            new RouteSpec("U", "INDEX", "action", "index.usage"),
            new RouteSpec("B", "INDEX", "action", "index.bookmark"),
            new RouteSpec("LEFT_ALT", "LENS", "overlay", "lens.scan"),
            new RouteSpec("J", "HOLOMAP", "screen", "holomap.open"),
            new RouteSpec("K", "HOLOMAP", "overlay", "holomap.minimap"),
            new RouteSpec("RIGHT_BRACKET", "HOLOMAP", "action", "holomap.zoom_in"),
            new RouteSpec("LEFT_BRACKET", "HOLOMAP", "action", "holomap.zoom_out"),
            new RouteSpec("BACKSLASH", "HOLOMAP", "action", "holomap.corner"),
            new RouteSpec("N", "SIGNALOS", "screen", "signalos.open")
    );

    private EchoNativeAgent5PhysicalRouteRequirements() {
    }

    static List<RouteSpec> phase5Routes() {
        return ROUTES;
    }

    static List<RouteSpec> physicalCoverageRoutes() {
        return ROUTES;
    }

    static List<String> physicalCoverageKeys() {
        return ROUTES.stream().map(RouteSpec::hotkey).toList();
    }

    static Map<String, String> physicalCoverageSurfacesByKey() {
        Map<String, String> surfaces = new LinkedHashMap<>();
        for (RouteSpec route : ROUTES) {
            surfaces.put(route.hotkey(), route.surface());
        }
        return Map.copyOf(surfaces);
    }

    static RouteSpec contextualRouteForKey(String key) {
        return ROUTES.stream().filter(route -> route.hotkey().equals(key)).findFirst().orElse(null);
    }

    static boolean productActionSurface(String surface) {
        return "INDEX".equals(surface) || "HOLOMAP".equals(surface) || "LENS".equals(surface) || "SIGNALOS".equals(surface);
    }

    record RouteSpec(String hotkey, String surface, String routeType, String action) {
        boolean contextual() {
            return false;
        }
    }
}
