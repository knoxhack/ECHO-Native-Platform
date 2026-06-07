package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderPhysicalRouteRequirements {
    public static final String SERVICE_ID = "echo.native.physical_route_requirements";
    private static final List<RouteSpec> BASE_SCREEN_ROUTES = List.of(
            new RouteSpec("M", "TERMINAL", "screen", "terminal.open", false),
            new RouteSpec("G", "INDEX", "screen", "index.catalog", false),
            new RouteSpec("R", "INDEX", "screen", "index.recipe", false),
            new RouteSpec("U", "INDEX", "screen", "index.usage", false),
            new RouteSpec("B", "INDEX", "screen", "index.bookmark", false),
            new RouteSpec("LEFT_ALT", "LENS", "screen", "lens.deep_scan", false),
            new RouteSpec("RIGHT_ALT", "LENS", "screen", "lens.deep_scan", false),
            new RouteSpec("J", "HOLOMAP", "screen", "holomap.open", false),
            new RouteSpec("K", "HOLOMAP", "screen", "holomap.toggle_minimap", false),
            new RouteSpec("RIGHT_BRACKET", "HOLOMAP", "screen", "holomap.zoom_in", false),
            new RouteSpec("LEFT_BRACKET", "HOLOMAP", "screen", "holomap.zoom_out", false),
            new RouteSpec("BACKSLASH", "HOLOMAP", "screen", "holomap.cycle_corner", false),
            new RouteSpec("N", "SIGNALOS", "screen", "signalos.terminal", false)
    );
    private static volatile ProductRouteProvider productRouteProvider = List::of;

    private NativeLoaderPhysicalRouteRequirements() {
    }

    public static void configure(ProductRouteProvider provider) {
        productRouteProvider = provider == null ? List::of : provider;
    }

    public static List<RouteSpec> phase5Routes() {
        List<RouteSpec> routes = new ArrayList<>(BASE_SCREEN_ROUTES.stream()
                .filter(route -> !"RIGHT_ALT".equals(route.hotkey()))
                .toList());
        routes.addAll(productActionRoutes(true));
        return List.copyOf(routes);
    }

    public static List<RouteSpec> physicalCoverageRoutes() {
        List<RouteSpec> routes = new ArrayList<>(BASE_SCREEN_ROUTES);
        routes.addAll(productActionRoutes(false));
        return List.copyOf(routes);
    }

    public static Map<String, String> physicalCoverageSurfacesByKey() {
        Map<String, String> surfaces = new LinkedHashMap<>();
        for (RouteSpec route : physicalCoverageRoutes()) {
            surfaces.putIfAbsent(route.hotkey(), route.surface());
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(surfaces));
    }

    public static List<String> physicalCoverageKeys() {
        return List.copyOf(physicalCoverageSurfacesByKey().keySet());
    }

    public static List<String> allPhysicalKeys() {
        Map<String, String> keys = new LinkedHashMap<>();
        for (RouteSpec route : BASE_SCREEN_ROUTES) {
            keys.put(route.hotkey(), route.surface());
        }
        for (RouteSpec route : productActionRoutes(true)) {
            keys.putIfAbsent(route.hotkey(), route.surface());
        }
        return List.copyOf(keys.keySet());
    }

    public static RouteSpec routeFor(String hotkey, String surface) {
        return phase5Routes().stream()
                .filter(route -> route.hotkey().equals(hotkey) && route.surface().equals(surface))
                .findFirst()
                .orElse(null);
    }

    public static RouteSpec primaryRouteForKey(String hotkey) {
        return physicalCoverageRoutes().stream()
                .filter(route -> route.hotkey().equals(hotkey))
                .findFirst()
                .orElse(null);
    }

    public static RouteSpec contextualRouteForKey(String hotkey) {
        return productActionRoutes(true).stream()
                .filter(route -> route.hotkey().equals(hotkey) && route.contextual())
                .findFirst()
                .orElse(null);
    }

    public static boolean productActionSurface(String surface) {
        return productActionRoutes(true).stream()
                .anyMatch(route -> route.surface().equals(surface));
    }

    private static List<RouteSpec> productActionRoutes(boolean includeContextual) {
        List<RouteSpec> routes = new ArrayList<>();
        for (ProductActionRoute route : productActionRoutes()) {
            if (route == null || blank(route.key()) || blank(route.surface()) || blank(route.action())) {
                continue;
            }
            if (!includeContextual && route.contextual()) {
                continue;
            }
            routes.add(new RouteSpec(route.key(), route.surface(), "action", route.action(), route.contextual()));
        }
        if (routes.stream().noneMatch(route -> "ASHFALL_DRONE".equals(route.surface()))) {
            routes.add(new RouteSpec("X", "ASHFALL_DRONE", "action", "ashfall.drone_recall", false));
            routes.add(new RouteSpec("C", "ASHFALL_DRONE", "action", "ashfall.drone_scan", false));
            routes.add(new RouteSpec("Y", "ASHFALL_DRONE", "action", "ashfall.drone_scout", false));
            routes.add(new RouteSpec("Z", "ASHFALL_DRONE", "action", "ashfall.drone_status", false));
            if (includeContextual) {
                routes.add(new RouteSpec("B", "ASHFALL_DRONE", "action", "ashfall.drone_assist", true));
            }
        }
        return List.copyOf(routes);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static List<ProductActionRoute> productActionRoutes() {
        try {
            List<ProductActionRoute> routes = productRouteProvider.routes();
            return routes == null ? List.of() : routes;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    public record RouteSpec(String hotkey, String surface, String routeType, String action, boolean contextual) {
    }

    public record ProductActionRoute(String key, String surface, String action, boolean contextual) {
    }

    @FunctionalInterface
    public interface ProductRouteProvider {
        List<ProductActionRoute> routes();
    }
}
