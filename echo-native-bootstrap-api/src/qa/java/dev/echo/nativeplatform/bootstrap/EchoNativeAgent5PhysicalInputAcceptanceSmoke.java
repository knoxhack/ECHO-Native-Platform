package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5PhysicalInputAcceptanceSmoke {
    private static final String SCREEN_CLASS = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";
    private static final List<Route> ROUTES = EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageRoutes()
            .stream()
            .map(route -> new Route(route.hotkey(), route.surface()))
            .toList();

    private EchoNativeAgent5PhysicalInputAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        List<Map<String, Object>> acceptedRoutes = ROUTES.stream()
                .map(route -> accepted(route.key(), route.surface()))
                .toList();
        Map<String, Object> accepted = acceptedRoutes.get(0);
        Map<String, Object> terminalHotkey = hotkey("M");
        Map<String, Object> terminalAcceptance = liveSurface("TERMINAL");
        Map<String, Object> rejectedSurfaceMismatch = EchoNativeAgent5PhysicalInputAcceptance.assess(
                terminalHotkey,
                liveSurface("INDEX")
        );
        Map<String, Object> rejectedNoHotkey = EchoNativeAgent5PhysicalInputAcceptance.assess(
                EchoNativeAgent5PhysicalHotkeyPoller.poll(
                        EchoNativeAgent5PhysicalHotkeyPoller.emptyState(),
                        EchoNativeAgent5PhysicalHotkeyPoller.emptyState()
                ),
                terminalAcceptance
        );
        boolean passed = acceptedRoutes.stream().allMatch(route -> Boolean.TRUE.equals(route.get("accepted")))
                && acceptedRoutes.stream().allMatch(route -> Boolean.TRUE.equals(route.get("serviceCodeExecuted")))
                && routeKeys(acceptedRoutes).equals(ROUTES.stream().map(Route::key).toList())
                && routeSurfaces(acceptedRoutes).equals(ROUTES.stream().map(Route::surface).toList())
                && "physical_input_acceptance:M->TERMINAL".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedSurfaceMismatch.get("accepted"))
                && Boolean.FALSE.equals(rejectedSurfaceMismatch.get("serviceCodeExecuted"))
                && "physical_input_acceptance:rejected:TERMINAL".equals(rejectedSurfaceMismatch.get("effect"))
                && Boolean.FALSE.equals(rejectedNoHotkey.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHotkey.get("serviceCodeExecuted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("physicalInputAcceptanceSmokeClass", EchoNativeAgent5PhysicalInputAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("acceptedRoutes", acceptedRoutes);
        smoke.put("routeSurfaces", routeSurfaces(acceptedRoutes));
        smoke.put("rejectedSurfaceMismatch", rejectedSurfaceMismatch);
        smoke.put("rejectedNoHotkey", rejectedNoHotkey);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> accepted(String key, String surface) {
        return EchoNativeAgent5PhysicalInputAcceptance.assess(hotkey(key), liveSurface(surface));
    }

    private static Map<String, Object> hotkey(String key) {
        return EchoNativeAgent5PhysicalHotkeyPoller.poll(
                EchoNativeAgent5PhysicalHotkeyPoller.emptyState(),
                pressed(key)
        );
    }

    private static Map<String, Object> liveSurface(String surface) {
        return EchoNativeAgent5LiveSurfaceAcceptance.assess(
                true,
                SCREEN_CLASS,
                SCREEN_CLASS,
                surface,
                surface
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

    private static Map<String, Boolean> pressed(String key) {
        Map<String, Boolean> state = new LinkedHashMap<>(EchoNativeAgent5PhysicalHotkeyPoller.emptyState());
        state.put(key, true);
        return Map.copyOf(state);
    }

    private record Route(String key, String surface) {
    }
}
