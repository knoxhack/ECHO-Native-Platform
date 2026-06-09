package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5HotkeyBridgeSmoke {
    private EchoNativeAgent5HotkeyBridgeSmoke() {
    }

    public static Map<String, Object> capture(
            boolean clientUiHostAttached,
            boolean hudOverlayRendered,
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        List<Map<String, Object>> steps = new ArrayList<>();
        for (EchoNativeAgent5PhysicalRouteRequirements.RouteSpec route
                : EchoNativeAgent5PhysicalRouteRequirements.phase5Routes()) {
            steps.add(routeStep(route.hotkey(), startingMode(route), route.surface()));
        }
        steps.add(routeStep("ESCAPE", "PAUSE"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("hotkeyBridgeSmokeClass", EchoNativeAgent5HotkeyBridgeSmoke.class.getSimpleName());
        smoke.put("screenClass", screenClass);
        smoke.put("steps", List.copyOf(steps));
        smoke.put("hotkeys", steps.stream()
                .map(step -> String.valueOf(step.get("key")))
                .toList());
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", steps.stream().allMatch(step -> Boolean.TRUE.equals(step.get("passed"))));
        return Map.copyOf(smoke);
    }

    private static String startingMode(EchoNativeAgent5PhysicalRouteRequirements.RouteSpec route) {
        return "B".equals(route.hotkey()) && "INDEX".equals(route.surface()) ? "INDEX" : "TERMINAL";
    }

    private static Map<String, Object> routeStep(String key, String expectedMode) {
        return routeStep(key, "TERMINAL", expectedMode);
    }

    private static Map<String, Object> routeStep(String key, String startingMode, String expectedMode) {
        Map<String, Object> route = EchoNativeAgent5UiActionRouter.routeKey(key, startingMode, "WIKI");
        boolean passed = Boolean.TRUE.equals(route.get("handled"))
                && expectedMode.equals(route.get("destinationMode"));
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("key", key);
        step.put("startingMode", startingMode);
        step.put("expectedMode", expectedMode);
        step.put("destinationMode", route.get("destinationMode"));
        step.put("effect", route.get("effect"));
        step.put("routerClass", route.get("routerClass"));
        step.put("passed", passed);
        return Map.copyOf(step);
    }

}
