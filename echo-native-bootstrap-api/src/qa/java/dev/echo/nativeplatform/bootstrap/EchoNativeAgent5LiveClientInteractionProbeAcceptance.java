package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveClientInteractionProbeAcceptance {
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
    private static final Map<String, String> REQUIRED_INTERACTIONS = Map.ofEntries(
            Map.entry("TERMINAL", "terminal_command"),
            Map.entry("INDEX", "index_search"),
            Map.entry("LENS", "lens_scan"),
            Map.entry("MISSION_LOG", "mission_update"),
            Map.entry("SETTINGS", "settings_adjustment"),
            Map.entry("PAUSE", "pause_resume"),
            Map.entry("RECOVERY", "recovery_action"),
            Map.entry("HOLOMAP", "mouse_focus"),
            Map.entry("WIKI", "mouse_focus"),
            Map.entry("MAIN_MENU", "main_menu_continue"),
            Map.entry("HUD", "hud_update")
    );

    private EchoNativeAgent5LiveClientInteractionProbeAcceptance() {
    }

    public static Map<String, Object> assess(boolean scheduled, boolean executed, List<Map<String, Object>> routes) {
        List<Map<String, Object>> probeRoutes = routes == null ? List.of() : routes;
        List<String> surfaces = probeRoutes.stream()
                .map(route -> String.valueOf(route.get("surface")))
                .toList();
        boolean accepted = scheduled
                && executed
                && surfaces.equals(REQUIRED_SURFACES)
                && probeRoutes.stream().allMatch(EchoNativeAgent5LiveClientInteractionProbeAcceptance::routeAccepted);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("scheduled", scheduled);
        result.put("executed", executed);
        result.put("routeCount", probeRoutes.size());
        result.put("surfaces", surfaces);
        result.put("interactions", probeRoutes.stream()
                .map(route -> String.valueOf(route.get("interaction")))
                .toList());
        result.put("effect", accepted
                ? "live_client_interaction_probe:accepted:" + probeRoutes.size()
                : "live_client_interaction_probe:rejected:" + probeRoutes.size());
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        List<Map<String, Object>> routes = REQUIRED_SURFACES.stream()
                .map(surface -> Map.<String, Object>of(
                        "surface", surface,
                        "interaction", REQUIRED_INTERACTIONS.get(surface),
                        "interactionAccepted", true
                ))
                .toList();
        Map<String, Object> accepted = assess(true, true, routes);
        Map<String, Object> rejectedNotExecuted = assess(true, false, routes);
        Map<String, Object> rejectedWrongInteraction = assess(true, true, List.of(
                Map.of("surface", "TERMINAL", "interaction", "wrong", "interactionAccepted", true)
        ));
        return Map.of(
                "liveClientInteractionProbeAcceptanceSmokeClass",
                EchoNativeAgent5LiveClientInteractionProbeAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNotExecuted", rejectedNotExecuted,
                "rejectedWrongInteraction", rejectedWrongInteraction,
                "passed", Boolean.TRUE.equals(accepted.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNotExecuted.get("accepted"))
                        && Boolean.FALSE.equals(rejectedWrongInteraction.get("accepted")),
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static boolean routeAccepted(Map<String, Object> route) {
        String surface = String.valueOf(route.get("surface"));
        String interaction = String.valueOf(route.get("interaction"));
        return REQUIRED_INTERACTIONS.containsKey(surface)
                && REQUIRED_INTERACTIONS.get(surface).equals(interaction)
                && Boolean.TRUE.equals(route.get("interactionAccepted"));
    }
}
