package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance {
    private EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance() {
    }

    public static Map<String, Object> assess(
            boolean scheduled,
            boolean executed,
            List<Map<String, Object>> routes,
            boolean noScreenCrash
    ) {
        List<Route> requiredRoutes = requiredRoutes();
        List<String> requiredSurfaces = requiredSurfaces(requiredRoutes);
        List<String> requiredHotkeys = requiredHotkeys(requiredRoutes);
        List<Map<String, Object>> sequence = routes == null ? List.of() : routes;
        List<String> surfaces = sequence.stream()
                .map(route -> String.valueOf(route.get("surface")))
                .toList();
        List<String> routeTypes = sequence.stream()
                .map(route -> String.valueOf(route.get("routeType")))
                .toList();
        List<String> hotkeys = sequence.stream()
                .map(route -> String.valueOf(route.get("hotkey")))
                .toList();
        List<String> physicalHotkeyEffects = sequence.stream()
                .map(route -> String.valueOf(route.get("physicalHotkeyEffect")))
                .toList();
        List<String> physicalHotkeySurfaces = sequence.stream()
                .map(route -> String.valueOf(route.get("physicalHotkeySurface")))
                .toList();
        boolean physicalPollerExecuted = sequence.stream()
                .allMatch(route -> Boolean.TRUE.equals(route.get("physicalPollerExecuted")));
        boolean accepted = scheduled
                && executed
                && noScreenCrash
                && surfaces.equals(requiredSurfaces)
                && hotkeys.equals(requiredHotkeys)
                && physicalPollerExecuted
                && sequence.stream().allMatch(EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance::routeAccepted);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("scheduled", scheduled);
        result.put("executed", executed);
        result.put("routeCount", sequence.size());
        result.put("surfaces", surfaces);
        result.put("requiredSurfaces", requiredSurfaces);
        result.put("routeTypes", routeTypes);
        result.put("hotkeys", hotkeys);
        result.put("requiredHotkeys", requiredHotkeys);
        result.put("physicalPollerExecuted", physicalPollerExecuted);
        result.put("physicalHotkeySurfaces", physicalHotkeySurfaces);
        result.put("physicalHotkeyEffects", physicalHotkeyEffects);
        result.put("noScreenCrash", noScreenCrash);
        result.put("effect", accepted
                ? "live_client_phase5_route_sequence:accepted:" + sequence.size()
                : "live_client_phase5_route_sequence:rejected:" + sequence.size());
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        List<Route> requiredRoutes = requiredRoutes();
        List<Map<String, Object>> routes = requiredRoutes.stream()
                .map(EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance::route)
                .toList();
        Map<String, Object> accepted = assess(true, true, routes, true);
        List<Map<String, Object>> wrongOrderRoutes = new ArrayList<>(routes);
        wrongOrderRoutes.set(0, routes.get(1));
        wrongOrderRoutes.set(1, routes.get(0));
        Map<String, Object> rejectedWrongOrder = assess(true, true, wrongOrderRoutes, true);
        List<Map<String, Object>> wrongTypeRoutes = new ArrayList<>(routes);
        Route routeTypeProbe = firstRouteWithType(requiredRoutes, "action");
        if (routeTypeProbe == null) {
            routeTypeProbe = requiredRoutes.get(0);
        }
        wrongTypeRoutes.set(requiredRoutes.indexOf(routeTypeProbe),
                route(new Route(routeTypeProbe.hotkey(), routeTypeProbe.surface(),
                        oppositeRouteType(routeTypeProbe.routeType()), routeTypeProbe.action())));
        Map<String, Object> rejectedWrongRouteType = assess(true, true, wrongTypeRoutes, true);
        List<Map<String, Object>> wrongHotkeyRoutes = new ArrayList<>(routes);
        Route expectedHotkeyRoute = requiredRoutes.size() > 6 ? requiredRoutes.get(6) : requiredRoutes.get(0);
        Route alternateHotkeyRoute = firstRouteWithDifferentHotkey(requiredRoutes, expectedHotkeyRoute.hotkey());
        wrongHotkeyRoutes.set(requiredRoutes.indexOf(expectedHotkeyRoute), fixtureRoute(
                "surface", expectedHotkeyRoute.surface(),
                "hotkey", alternateHotkeyRoute.hotkey(),
                "routeType", expectedHotkeyRoute.routeType(),
                "physicalHotkeyAccepted", false,
                "physicalPollerExecuted", true,
                "physicalHotkeySurface", alternateHotkeyRoute.surface(),
                "physicalHotkeyEffect", observedEffect(alternateHotkeyRoute),
                "routeEffectAccepted", true,
                "screenOpened", true,
                "dataBackedAction", true,
                "renderAccepted", true
        ));
        Map<String, Object> rejectedWrongHotkey = assess(true, true, wrongHotkeyRoutes, true);
        List<Map<String, Object>> noPhysicalHotkeyRoutes = new ArrayList<>(routes);
        noPhysicalHotkeyRoutes.set(5, fixtureRoute(
                "surface", "LENS",
                "hotkey", "LEFT_ALT",
                "routeType", "screen",
                "physicalHotkeyAccepted", false,
                "physicalPollerExecuted", false,
                "physicalHotkeySurface", "",
                "physicalHotkeyEffect", "physical_hotkey:none",
                "routeEffectAccepted", true,
                "screenOpened", true,
                "dataBackedAction", true,
                "renderAccepted", true
        ));
        Map<String, Object> rejectedNoPhysicalHotkey = assess(true, true, noPhysicalHotkeyRoutes, true);
        List<Map<String, Object>> noLensOverlayRoutes = new ArrayList<>(routes);
        Route lensRoute = firstRouteWithSurface(requiredRoutes, "LENS");
        int lensRouteIndex = lensRoute == null ? 5 : requiredRoutes.indexOf(lensRoute);
        noLensOverlayRoutes.set(lensRouteIndex, fixtureRouteWithMutationEvidence(
                "surface", "LENS",
                "hotkey", lensRoute == null ? "LEFT_ALT" : lensRoute.hotkey(),
                "routeType", "screen",
                "physicalHotkeyAccepted", false,
                "physicalPollerExecuted", true,
                "physicalHotkeySurface", "LENS",
                "physicalHotkeyEffect", "physical_hotkey_observed:"
                        + (lensRoute == null ? "LEFT_ALT" : lensRoute.hotkey()) + "->LENS:lens.deep_scan",
                "routeEffectAccepted", true,
                "runtimeHostMutated", true,
                "adapterCoreMutation", true,
                "saveTouched", true,
                "feedbackEmitted", true,
                "missionUpdated", true,
                "screenOpened", false,
                "overlayRendered", false,
                "dataBackedAction", true,
                "renderAccepted", true
        ));
        Map<String, Object> rejectedNoLensOverlay = assess(true, true, noLensOverlayRoutes, true);
        List<Map<String, Object>> noHoloMapOverlayRoutes = new ArrayList<>(routes);
        Route holoOverlayRoute = requiredRoutes.stream()
                .filter(route -> "HOLOMAP".equals(route.surface()) && !"J".equals(route.hotkey()))
                .findFirst()
                .orElse(null);
        if (holoOverlayRoute != null) {
            int holoOverlayIndex = requiredRoutes.indexOf(holoOverlayRoute);
            noHoloMapOverlayRoutes.set(holoOverlayIndex, fixtureRouteWithMutationEvidence(
                    "surface", "HOLOMAP",
                    "hotkey", holoOverlayRoute.hotkey(),
                    "routeType", "screen",
                    "physicalHotkeyAccepted", false,
                    "physicalPollerExecuted", true,
                    "physicalHotkeySurface", "HOLOMAP",
                    "physicalHotkeyEffect", "physical_hotkey_observed:"
                            + holoOverlayRoute.hotkey() + "->HOLOMAP:" + holoOverlayRoute.action(),
                    "routeEffectAccepted", true,
                    "runtimeHostMutated", true,
                    "adapterCoreMutation", true,
                    "saveTouched", true,
                    "feedbackEmitted", true,
                    "missionUpdated", true,
                    "screenOpened", false,
                    "mapStateChanged", true,
                    "clientOverlayStateChanged", false,
                    "renderAccepted", true
            ));
        }
        Map<String, Object> rejectedNoHoloMapOverlay = assess(true, true, noHoloMapOverlayRoutes, true);
        Map<String, Object> rejectedCrash = assess(true, true, routes, false);
        List<Map<String, Object>> missingHostMutationRoutes = new ArrayList<>(routes);
        missingHostMutationRoutes.set(0, fixtureRouteWithMutationEvidence(
                "surface", "TERMINAL",
                "hotkey", "M",
                "routeType", "screen",
                "physicalHotkeyAccepted", false,
                "physicalPollerExecuted", true,
                "physicalHotkeySurface", "TERMINAL",
                "physicalHotkeyEffect", "physical_hotkey_observed:M->TERMINAL:terminal.open",
                "routeEffectAccepted", true,
                "runtimeHostMutated", false,
                "adapterCoreMutation", true,
                "saveTouched", true,
                "feedbackEmitted", true,
                "missionUpdated", true,
                "screenOpened", true,
                "dataBackedAction", true,
                "renderAccepted", true
        ));
        Map<String, Object> rejectedMissingHostMutation = assess(true, true, missingHostMutationRoutes, true);
        List<Map<String, Object>> missingSaveRoutes = new ArrayList<>(routes);
        missingSaveRoutes.set(1, fixtureRouteWithMutationEvidence(
                "surface", "INDEX",
                "hotkey", "G",
                "routeType", "screen",
                "physicalHotkeyAccepted", false,
                "physicalPollerExecuted", true,
                "physicalHotkeySurface", "INDEX",
                "physicalHotkeyEffect", "physical_hotkey_observed:G->INDEX:index.catalog",
                "routeEffectAccepted", true,
                "runtimeHostMutated", true,
                "adapterCoreMutation", true,
                "saveTouched", false,
                "feedbackEmitted", true,
                "missionUpdated", true,
                "screenOpened", true,
                "dataBackedAction", true,
                "renderAccepted", true
        ));
        Map<String, Object> rejectedMissingSave = assess(true, true, missingSaveRoutes, true);
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveClientPhase5RouteSequenceAcceptanceSmokeClass",
                EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedWrongOrder", rejectedWrongOrder);
        smoke.put("rejectedWrongRouteType", rejectedWrongRouteType);
        smoke.put("rejectedWrongHotkey", rejectedWrongHotkey);
        smoke.put("rejectedNoPhysicalHotkey", rejectedNoPhysicalHotkey);
        smoke.put("rejectedNoLensOverlay", rejectedNoLensOverlay);
        smoke.put("rejectedNoHoloMapOverlay", rejectedNoHoloMapOverlay);
        smoke.put("rejectedCrash", rejectedCrash);
        smoke.put("rejectedMissingHostMutation", rejectedMissingHostMutation);
        smoke.put("rejectedMissingSave", rejectedMissingSave);
        smoke.put("passed", Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.FALSE.equals(rejectedWrongOrder.get("accepted"))
                && Boolean.FALSE.equals(rejectedWrongRouteType.get("accepted"))
                && Boolean.FALSE.equals(rejectedWrongHotkey.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalHotkey.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoLensOverlay.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHoloMapOverlay.get("accepted"))
                && Boolean.FALSE.equals(rejectedCrash.get("accepted"))
                && Boolean.FALSE.equals(rejectedMissingHostMutation.get("accepted"))
                && Boolean.FALSE.equals(rejectedMissingSave.get("accepted")));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> fixtureRoute(Object... entries) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            row.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return Map.copyOf(row);
    }

    private static Map<String, Object> fixtureRouteWithMutationEvidence(Object... entries) {
        Map<String, Object> row = new LinkedHashMap<>(fixtureRoute(entries));
        row.putIfAbsent("routeEffectAccepted", true);
        row.putIfAbsent("runtimeHostMutated", true);
        row.putIfAbsent("adapterCoreMutation", true);
        row.putIfAbsent("saveTouched", true);
        row.putIfAbsent("feedbackEmitted", true);
        row.putIfAbsent("missionUpdated", true);
        return Map.copyOf(row);
    }

    private static Map<String, Object> route(Route expected) {
        Map<String, Object> physicalHotkey = physicalHotkey(expected.hotkey(), expected.surface());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("surface", expected.surface());
        row.put("hotkey", expected.hotkey());
        row.put("routeType", expected.routeType());
        row.put("physicalHotkeyAccepted", Boolean.TRUE.equals(physicalHotkey.get("handled")));
        row.put("physicalPollerExecuted", Boolean.TRUE.equals(physicalHotkey.get("physicalPollerExecuted")));
        row.put("physicalHotkeySurface", String.valueOf(physicalHotkey.get("surface")));
        row.put("physicalHotkeyEffect", String.valueOf(physicalHotkey.get("effect")));
        row.put("routeEffectAccepted", true);
        row.put("runtimeHostMutated", true);
        row.put("adapterCoreMutation", true);
        row.put("saveTouched", true);
        row.put("feedbackEmitted", true);
        row.put("missionUpdated", true);
        row.put("renderAccepted", !"action".equals(expected.routeType()));
        if ("action".equals(expected.routeType())
                && EchoNativeAgent5PhysicalRouteRequirements.productActionSurface(expected.surface())) {
            row.put("serverboundPacketSent", true);
            row.put("entityCommandExecuted", true);
        }
        switch (expected.surface()) {
            case "TERMINAL", "INDEX" -> {
                row.put("screenOpened", true);
                row.put("dataBackedAction", true);
            }
            case "LENS" -> {
                row.put("screenOpened", false);
                row.put("overlayRendered", true);
                row.put("dataBackedAction", true);
            }
            case "HOLOMAP" -> {
                if ("J".equals(expected.hotkey())) {
                    row.put("screenOpened", true);
                    row.put("dataBackedAction", true);
                } else {
                    row.put("mapStateChanged", true);
                    row.put("clientOverlayStateChanged", true);
                }
            }
            case "SIGNALOS" -> {
                row.put("screenOpened", true);
                row.put("serverboundPacketSent", true);
            }
            default -> {
            }
        }
        return Map.copyOf(row);
    }

    private static boolean routeAccepted(Map<String, Object> route) {
        String surface = String.valueOf(route.get("surface"));
        String routeType = String.valueOf(route.get("routeType"));
        String hotkey = String.valueOf(route.get("hotkey"));
        String physicalHotkeySurface = String.valueOf(route.get("physicalHotkeySurface"));
        String physicalHotkeyEffect = String.valueOf(route.get("physicalHotkeyEffect"));
        Route expected = routeFor(hotkey, surface);
        return expected != null
                && expected.routeType().equals(routeType)
                && Boolean.FALSE.equals(route.get("physicalHotkeyAccepted"))
                && Boolean.TRUE.equals(route.get("physicalPollerExecuted"))
                && surface.equals(physicalHotkeySurface)
                && physicalHotkeyEffect.startsWith("physical_hotkey_observed:" + hotkey + "->" + surface + ":")
                && expected.action().equals(actionFromEffect(physicalHotkeyEffect))
                && Boolean.TRUE.equals(route.get("routeEffectAccepted"))
                && runtimeMutationEvidence(route)
                && (!"LENS".equals(surface) || Boolean.TRUE.equals(route.get("overlayRendered")))
                && (!holoMapOverlayRoute(hotkey, surface) || Boolean.TRUE.equals(route.get("clientOverlayStateChanged")))
                && concreteRouteEvidence(route)
                && ("action".equals(routeType) || Boolean.TRUE.equals(route.get("renderAccepted")));
    }

    private static boolean runtimeMutationEvidence(Map<String, Object> route) {
        return Boolean.TRUE.equals(route.get("adapterCoreMutation"))
                && Boolean.TRUE.equals(route.get("runtimeHostMutated"))
                && Boolean.TRUE.equals(route.get("saveTouched"))
                && Boolean.TRUE.equals(route.get("feedbackEmitted"))
                && Boolean.TRUE.equals(route.get("missionUpdated"));
    }

    private static boolean concreteRouteEvidence(Map<String, Object> route) {
        return Boolean.TRUE.equals(route.get("screenOpened"))
                || Boolean.TRUE.equals(route.get("dataBackedAction"))
                || Boolean.TRUE.equals(route.get("overlayRendered"))
                || Boolean.TRUE.equals(route.get("mapStateChanged"))
                || Boolean.TRUE.equals(route.get("clientOverlayStateChanged"))
                || Boolean.TRUE.equals(route.get("hudStateChanged"))
                || Boolean.TRUE.equals(route.get("serverboundPacketSent"))
                || Boolean.TRUE.equals(route.get("entityCommandExecuted"))
                || Boolean.TRUE.equals(route.get("worldStateMutated"));
    }

    private static Map<String, Object> physicalHotkey(String key, String expectedSurface) {
        Map<String, Boolean> current = new LinkedHashMap<>(EchoNativeAgent5PhysicalHotkeyPoller.emptyState());
        current.put(key, true);
        Map<String, Object> event = EchoNativeAgent5PhysicalHotkeyPoller.poll(
                EchoNativeAgent5PhysicalHotkeyPoller.emptyState(),
                Map.copyOf(current)
        );
        EchoNativeAgent5PhysicalRouteRequirements.RouteSpec contextual =
                EchoNativeAgent5PhysicalRouteRequirements.contextualRouteForKey(key);
        if (contextual != null && expectedSurface.equals(contextual.surface())) {
            Map<String, Object> alternate = new LinkedHashMap<>(event);
            alternate.put("surface", contextual.surface());
            alternate.put("action", contextual.action());
            alternate.put("effect", "physical_hotkey_observed:" + key + "->"
                    + contextual.surface() + ":" + contextual.action());
            alternate.put("contextual", true);
            return Map.copyOf(alternate);
        }
        return event;
    }

    private static boolean holoMapOverlayRoute(String hotkey, String surface) {
        return "HOLOMAP".equals(surface) && !"J".equals(hotkey);
    }

    private static Route routeFor(String hotkey, String surface) {
        return requiredRoutes().stream()
                .filter(route -> route.hotkey().equals(hotkey) && route.surface().equals(surface))
                .findFirst()
                .orElse(null);
    }

    private static List<Route> requiredRoutes() {
        return EchoNativeAgent5PhysicalRouteRequirements.phase5Routes().stream()
                .map(route -> new Route(route.hotkey(), route.surface(), route.routeType(), route.action()))
                .toList();
    }

    private static List<String> requiredSurfaces(List<Route> routes) {
        return routes.stream()
                .map(Route::surface)
                .toList();
    }

    private static List<String> requiredHotkeys(List<Route> routes) {
        return routes.stream()
                .map(Route::hotkey)
                .toList();
    }

    private static Route firstRouteWithType(List<Route> routes, String routeType) {
        return routes.stream()
                .filter(route -> route.routeType().equals(routeType))
                .findFirst()
                .orElse(null);
    }

    private static Route firstRouteWithDifferentHotkey(List<Route> routes, String hotkey) {
        return routes.stream()
                .filter(route -> !route.hotkey().equals(hotkey))
                .findFirst()
                .orElse(routes.get(0));
    }

    private static Route firstRouteWithSurface(List<Route> routes, String surface) {
        return routes.stream()
                .filter(route -> route.surface().equals(surface))
                .findFirst()
                .orElse(null);
    }

    private static String oppositeRouteType(String routeType) {
        return "action".equals(routeType) ? "screen" : "action";
    }

    private static String observedEffect(Route route) {
        return "physical_hotkey_observed:" + route.hotkey() + "->" + route.surface() + ":" + route.action();
    }

    private static String actionFromEffect(String effect) {
        int index = effect.lastIndexOf(':');
        return index < 0 || index == effect.length() - 1 ? "" : effect.substring(index + 1);
    }

    private record Route(String hotkey, String surface, String routeType, String action) {
    }
}
