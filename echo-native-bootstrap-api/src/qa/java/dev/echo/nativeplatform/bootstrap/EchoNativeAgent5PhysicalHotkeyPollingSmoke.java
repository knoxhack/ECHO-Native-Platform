package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5PhysicalHotkeyPollingSmoke {
    private EchoNativeAgent5PhysicalHotkeyPollingSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Boolean> empty = EchoNativeAgent5PhysicalHotkeyPoller.emptyState();
        Map<String, Object> terminal = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("M"));
        Map<String, Object> repeat = EchoNativeAgent5PhysicalHotkeyPoller.poll(pressed("M"), pressed("M"));
        Map<String, Object> index = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("G"));
        Map<String, Object> recipe = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("R"));
        Map<String, Object> usage = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("U"));
        Map<String, Object> bookmark = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("B"));
        Map<String, Object> lens = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("LEFT_ALT"));
        Map<String, Object> holomap = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("J"));
        Map<String, Object> minimap = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("K"));
        Map<String, Object> zoomIn = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("RIGHT_BRACKET"));
        Map<String, Object> zoomOut = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("LEFT_BRACKET"));
        Map<String, Object> corner = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("BACKSLASH"));
        Map<String, Object> signalos = EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed("N"));
        List<EchoNativeAgent5PhysicalRouteRequirements.RouteSpec> productRoutes =
                EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageRoutes().stream()
                        .filter(route -> "action".equals(route.routeType()))
                        .toList();
        List<Map<String, Object>> productActionEvents = productRoutes.stream()
                .map(route -> EchoNativeAgent5PhysicalHotkeyPoller.poll(empty, pressed(route.hotkey())))
                .toList();
        boolean passed = Boolean.TRUE.equals(terminal.get("observed"))
                && "TERMINAL".equals(terminal.get("surface"))
                && "physical_hotkey_observed:M->TERMINAL:terminal.open".equals(terminal.get("effect"))
                && Boolean.FALSE.equals(repeat.get("observed"))
                && Boolean.TRUE.equals(index.get("observed"))
                && "INDEX".equals(index.get("surface"))
                && Boolean.TRUE.equals(recipe.get("observed"))
                && "INDEX".equals(recipe.get("surface"))
                && Boolean.TRUE.equals(usage.get("observed"))
                && "INDEX".equals(usage.get("surface"))
                && Boolean.TRUE.equals(bookmark.get("observed"))
                && "INDEX".equals(bookmark.get("surface"))
                && "index.bookmark".equals(bookmark.get("action"))
                && Boolean.TRUE.equals(lens.get("observed"))
                && "LENS".equals(lens.get("surface"))
                && Boolean.TRUE.equals(holomap.get("observed"))
                && "HOLOMAP".equals(holomap.get("surface"))
                && Boolean.TRUE.equals(minimap.get("observed"))
                && "HOLOMAP".equals(minimap.get("surface"))
                && Boolean.TRUE.equals(zoomIn.get("observed"))
                && "HOLOMAP".equals(zoomIn.get("surface"))
                && Boolean.TRUE.equals(zoomOut.get("observed"))
                && "HOLOMAP".equals(zoomOut.get("surface"))
                && Boolean.TRUE.equals(corner.get("observed"))
                && "HOLOMAP".equals(corner.get("surface"))
                && Boolean.TRUE.equals(signalos.get("observed"))
                && "SIGNALOS".equals(signalos.get("surface"))
                && productActionsPassed(productRoutes, productActionEvents);
        Map<String, Object> smoke = new LinkedHashMap<>();
        List<Map<String, Object>> events = new ArrayList<>(List.of(
                terminal,
                repeat,
                index,
                recipe,
                usage,
                bookmark,
                lens,
                holomap,
                minimap,
                zoomIn,
                zoomOut,
                corner,
                signalos
        ));
        events.addAll(productActionEvents);
        smoke.put("physicalHotkeyPollingSmokeClass", EchoNativeAgent5PhysicalHotkeyPollingSmoke.class.getSimpleName());
        smoke.put("events", List.copyOf(events));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static boolean productActionsPassed(
            List<EchoNativeAgent5PhysicalRouteRequirements.RouteSpec> routes,
            List<Map<String, Object>> events
    ) {
        if (routes.size() != events.size()) {
            return false;
        }
        for (int index = 0; index < routes.size(); index++) {
            EchoNativeAgent5PhysicalRouteRequirements.RouteSpec route = routes.get(index);
            Map<String, Object> event = events.get(index);
            if (!Boolean.TRUE.equals(event.get("observed"))
                    || !route.surface().equals(event.get("surface"))
                    || !route.action().equals(event.get("action"))
                    || !String.valueOf(event.get("effect")).startsWith(
                            "physical_hotkey_observed:" + route.hotkey() + "->" + route.surface() + ":")) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Boolean> pressed(String key) {
        Map<String, Boolean> state = new LinkedHashMap<>(EchoNativeAgent5PhysicalHotkeyPoller.emptyState());
        state.put(key, true);
        return Map.copyOf(state);
    }
}
