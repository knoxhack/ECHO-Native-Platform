package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

final class EchoNativeAgent5PhysicalHotkeyPoller {
    private EchoNativeAgent5PhysicalHotkeyPoller() {
    }

    static Map<String, Boolean> emptyState() {
        Map<String, Boolean> state = new LinkedHashMap<>();
        for (String key : EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageKeys()) {
            state.put(key, false);
        }
        return Map.copyOf(state);
    }

    static Map<String, Object> poll(Map<String, Boolean> previous, Map<String, Boolean> current) {
        Map<String, Boolean> safePrevious = previous == null ? Map.of() : previous;
        Map<String, Boolean> safeCurrent = current == null ? Map.of() : current;
        for (EchoNativeAgent5PhysicalRouteRequirements.RouteSpec route : EchoNativeAgent5PhysicalRouteRequirements.phase5Routes()) {
            boolean wasPressed = Boolean.TRUE.equals(safePrevious.get(route.hotkey()));
            boolean isPressed = Boolean.TRUE.equals(safeCurrent.get(route.hotkey()));
            if (isPressed && !wasPressed) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("observed", true);
                event.put("hotkey", route.hotkey());
                event.put("surface", route.surface());
                event.put("routeType", route.routeType());
                event.put("action", route.action());
                event.put("effect", "physical_hotkey_observed:" + route.hotkey() + "->" + route.surface() + ":" + route.action());
                event.put("screenOpened", "screen".equals(route.routeType()));
                event.put("overlayRendered", "overlay".equals(route.routeType()));
                event.put("dataBackedAction", "action".equals(route.routeType()));
                event.put("mapStateChanged", route.surface().equals("HOLOMAP"));
                event.put("clientOverlayStateChanged", "overlay".equals(route.routeType()));
                event.put("hudStateChanged", false);
                event.put("serverboundPacketSent", "action".equals(route.routeType()));
                event.put("entityCommandExecuted", false);
                event.put("worldStateMutated", false);
                return Map.copyOf(event);
            }
        }
        return Map.of("observed", false, "effect", "");
    }
}
