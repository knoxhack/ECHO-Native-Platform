package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderPhysicalHotkeyPoller {
    public static final String SERVICE_ID = "echo.native.physical_hotkey_poller";

    private NativeLoaderPhysicalHotkeyPoller() {
    }

    public static Map<String, Boolean> emptyState() {
        Map<String, Boolean> state = new LinkedHashMap<>();
        for (String key : NativeLoaderPhysicalRouteRequirements.allPhysicalKeys()) {
            state.put(key, false);
        }
        return Map.copyOf(state);
    }

    public static Map<String, Object> poll(Map<String, Boolean> previous, Map<String, Boolean> current) {
        Map<String, Boolean> prev = previous == null ? emptyState() : previous;
        Map<String, Boolean> now = current == null ? emptyState() : current;
        for (String key : NativeLoaderPhysicalRouteRequirements.allPhysicalKeys()) {
            if (Boolean.TRUE.equals(now.get(key)) && !Boolean.TRUE.equals(prev.get(key))) {
                String surface = surfaceFor(key);
                String action = actionFor(key);
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("nativePhysicalHotkeyPollerServiceId", SERVICE_ID);
                event.put("observed", true);
                event.put("handled", false);
                event.put("physicalPoller", true);
                event.put("physicalPollerExecuted", true);
                event.put("serviceCodeExecuted", false);
                event.put("key", key);
                event.put("surface", surface);
                event.put("action", action);
                event.put("declaredSource", declaredSourceFor(key));
                event.put("routePending", true);
                event.put("hudOverlay", false);
                NativeLoaderPhysicalRouteRequirements.RouteSpec contextual =
                        NativeLoaderPhysicalRouteRequirements.contextualRouteForKey(key);
                if (contextual != null) {
                    event.put("contextual", true);
                    event.put("alternateSurface", contextual.surface());
                    event.put("alternateAction", contextual.action());
                    event.put("sourceConflict", action + "/" + contextual.action());
                }
                event.put("effect", "physical_hotkey_observed:" + key + "->" + surface + ":" + action);
                return Map.copyOf(event);
            }
        }
        return Map.of(
                "nativePhysicalHotkeyPollerServiceId", SERVICE_ID,
                "observed", false,
                "handled", false,
                "physicalPoller", true,
                "physicalPollerExecuted", true,
                "serviceCodeExecuted", false,
                "effect", "physical_hotkey:none"
        );
    }

    private static String surfaceFor(String key) {
        NativeLoaderPhysicalRouteRequirements.RouteSpec route =
                NativeLoaderPhysicalRouteRequirements.primaryRouteForKey(key);
        return route == null ? "" : route.surface();
    }

    private static String actionFor(String key) {
        NativeLoaderPhysicalRouteRequirements.RouteSpec route =
                NativeLoaderPhysicalRouteRequirements.primaryRouteForKey(key);
        return route == null ? "" : route.action();
    }

    private static String declaredSourceFor(String key) {
        NativeLoaderPhysicalRouteRequirements.RouteSpec route =
                NativeLoaderPhysicalRouteRequirements.primaryRouteForKey(key);
        if (route == null) {
            return "unmapped";
        }
        return "action".equals(route.routeType()) ? "product_profile" : "addon_default";
    }
}
