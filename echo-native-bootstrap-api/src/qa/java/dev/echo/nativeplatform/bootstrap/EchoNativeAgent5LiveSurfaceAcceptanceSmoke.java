package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveSurfaceAcceptanceSmoke {
    private static final String SCREEN_CLASS = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";
    private static final List<String> SURFACES = List.of(
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

    private EchoNativeAgent5LiveSurfaceAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        List<Map<String, Object>> acceptedRoutes = SURFACES.stream()
                .map(EchoNativeAgent5LiveSurfaceAcceptanceSmoke::accepted)
                .toList();
        Map<String, Object> accepted = acceptedRoutes.get(0);
        Map<String, Object> rejectedMode = EchoNativeAgent5LiveSurfaceAcceptance.assess(
                true,
                SCREEN_CLASS,
                SCREEN_CLASS,
                "INDEX",
                "TERMINAL"
        );
        Map<String, Object> rejectedSetScreen = EchoNativeAgent5LiveSurfaceAcceptance.assess(
                false,
                SCREEN_CLASS,
                SCREEN_CLASS,
                "TERMINAL",
                "TERMINAL"
        );
        boolean passed = acceptedRoutes.stream().allMatch(route -> Boolean.TRUE.equals(route.get("accepted")))
                && acceptedRoutes.stream().allMatch(route -> Boolean.TRUE.equals(route.get("serviceCodeExecuted")))
                && routeSurfaces(acceptedRoutes).equals(SURFACES)
                && "live_surface:accepted:TERMINAL".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedMode.get("accepted"))
                && Boolean.FALSE.equals(rejectedMode.get("serviceCodeExecuted"))
                && "live_surface:rejected:TERMINAL".equals(rejectedMode.get("effect"))
                && Boolean.FALSE.equals(rejectedSetScreen.get("accepted"))
                && Boolean.FALSE.equals(rejectedSetScreen.get("serviceCodeExecuted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveSurfaceAcceptanceSmokeClass", EchoNativeAgent5LiveSurfaceAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("acceptedRoutes", acceptedRoutes);
        smoke.put("routeSurfaces", routeSurfaces(acceptedRoutes));
        smoke.put("rejectedMode", rejectedMode);
        smoke.put("rejectedSetScreen", rejectedSetScreen);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> accepted(String surface) {
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
                .map(route -> String.valueOf(route.get("currentMode")))
                .toList();
    }
}
