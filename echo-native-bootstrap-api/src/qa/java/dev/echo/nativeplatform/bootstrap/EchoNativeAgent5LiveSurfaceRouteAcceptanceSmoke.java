package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveSurfaceRouteAcceptanceSmoke {
    private static final String SCREEN_CLASS = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";
    private static final List<Route> ROUTES = List.of(
            new Route("M", "TERMINAL"),
            new Route("G", "INDEX"),
            new Route("R", "INDEX"),
            new Route("U", "INDEX"),
            new Route("B", "INDEX"),
            new Route("LEFT_ALT", "LENS"),
            new Route("J", "HOLOMAP"),
            new Route("K", "HOLOMAP"),
            new Route("RIGHT_BRACKET", "HOLOMAP"),
            new Route("LEFT_BRACKET", "HOLOMAP"),
            new Route("BACKSLASH", "HOLOMAP"),
            new Route("N", "SIGNALOS"),
            new Route("X", "ASHFALL_DRONE"),
            new Route("C", "ASHFALL_DRONE"),
            new Route("Y", "ASHFALL_DRONE"),
            new Route("Z", "ASHFALL_DRONE")
    );

    private EchoNativeAgent5LiveSurfaceRouteAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        List<Map<String, Object>> acceptedRoutes = ROUTES.stream()
                .map(route -> route(route.key(), route.surface()))
                .toList();
        Map<String, Object> rejectedNoHotkey = EchoNativeAgent5LiveSurfaceRouteAcceptance.assess(
                Map.of("handled", false, "key", "M", "surface", "TERMINAL", "hudOverlay", false),
                liveSurface("TERMINAL", true),
                physicalInput(hotkey("M"), liveSurface("TERMINAL", true)),
                render(liveSurface("TERMINAL", true), "TERMINAL", true)
        );
        Map<String, Object> rejectedNoSurface = EchoNativeAgent5LiveSurfaceRouteAcceptance.assess(
                hotkey("M"),
                liveSurface("TERMINAL", false),
                physicalInput(hotkey("M"), liveSurface("TERMINAL", false)),
                render(liveSurface("TERMINAL", false), "TERMINAL", true)
        );
        Map<String, Object> rejectedNoRender = EchoNativeAgent5LiveSurfaceRouteAcceptance.assess(
                hotkey("M"),
                liveSurface("TERMINAL", true),
                physicalInput(hotkey("M"), liveSurface("TERMINAL", true)),
                render(liveSurface("TERMINAL", true), "TERMINAL", false)
        );
        Map<String, Object> rejectedWikiNoPhysicalKey = route("W", "WIKI");
        boolean passed = acceptedRoutes.stream().allMatch(route -> Boolean.TRUE.equals(route.get("accepted")))
                && routeKeys(acceptedRoutes).equals(ROUTES.stream().map(Route::key).toList())
                && routeSurfaces(acceptedRoutes).equals(ROUTES.stream().map(Route::surface).toList())
                && Boolean.FALSE.equals(rejectedNoHotkey.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoSurface.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRender.get("accepted"))
                && Boolean.FALSE.equals(rejectedWikiNoPhysicalKey.get("accepted"))
                && "".equals(rejectedWikiNoPhysicalKey.get("surface"))
                && Boolean.FALSE.equals(rejectedWikiNoPhysicalKey.get("physicalHotkeyHandled"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveSurfaceRouteAcceptanceSmokeClass",
                EchoNativeAgent5LiveSurfaceRouteAcceptanceSmoke.class.getSimpleName());
        smoke.put("acceptedRoutes", acceptedRoutes);
        smoke.put("routeSurfaces", routeSurfaces(acceptedRoutes));
        smoke.put("rejectedNoHotkey", rejectedNoHotkey);
        smoke.put("rejectedNoSurface", rejectedNoSurface);
        smoke.put("rejectedNoRender", rejectedNoRender);
        smoke.put("rejectedWikiNoPhysicalKey", rejectedWikiNoPhysicalKey);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> route(String key, String surface) {
        Map<String, Object> hotkey = hotkey(key);
        Map<String, Object> liveSurface = liveSurface(surface, true);
        Map<String, Object> input = physicalInput(hotkey, liveSurface);
        Map<String, Object> render = render(liveSurface, surface, true);
        return EchoNativeAgent5LiveSurfaceRouteAcceptance.assess(hotkey, liveSurface, input, render);
    }

    private static Map<String, Object> hotkey(String key) {
        Map<String, Boolean> current = new LinkedHashMap<>(EchoNativeAgent5PhysicalHotkeyPoller.emptyState());
        current.put(key, true);
        return EchoNativeAgent5PhysicalHotkeyPoller.poll(
                EchoNativeAgent5PhysicalHotkeyPoller.emptyState(),
                current
        );
    }

    private static Map<String, Object> liveSurface(String surface, boolean accepted) {
        return EchoNativeAgent5LiveSurfaceAcceptance.assess(
                accepted,
                SCREEN_CLASS,
                SCREEN_CLASS,
                surface,
                surface
        );
    }

    private static Map<String, Object> physicalInput(
            Map<String, Object> hotkey,
            Map<String, Object> liveSurface
    ) {
        return EchoNativeAgent5PhysicalInputAcceptance.assess(hotkey, liveSurface);
    }

    private static Map<String, Object> render(
            Map<String, Object> liveSurface,
            String surface,
            boolean opened
    ) {
        return EchoNativeAgent5LiveSurfaceRenderAcceptance.assess(
                liveSurface,
                EchoNativeAgent5UiHostSmokeSnapshot.capture(
                        surface,
                        opened,
                        SCREEN_CLASS,
                        "echoashfallprotocol",
                        92,
                        20,
                        1,
                        1
                )
        );
    }

    private static List<String> routeSurfaces(List<Map<String, Object>> routes) {
        return routes.stream()
                .map(route -> String.valueOf(route.get("surface")))
                .toList();
    }

    private static List<String> routeKeys(List<Map<String, Object>> routes) {
        return routes.stream()
                .map(route -> String.valueOf(route.get("key")))
                .toList();
    }

    private record Route(String key, String surface) {
    }
}
