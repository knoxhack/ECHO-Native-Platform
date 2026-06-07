package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LivePhase5Acceptance {
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
    private static final List<String> REQUIRED_INTERACTIONS = List.of(
            "terminal_command",
            "index_search",
            "lens_scan",
            "mission_update",
            "settings_adjustment",
            "pause_resume",
            "recovery_action",
            "mouse_focus",
            "mouse_focus",
            "main_menu_continue",
            "hud_update"
    );

    private EchoNativeAgent5LivePhase5Acceptance() {
    }

    public static Map<String, Object> assess(Map<String, Object> bridge) {
        Map<String, Object> uiProbe = object(bridge.get("lastLiveClientUiProbeAcceptance"));
        Map<String, Object> interactionProbe = object(bridge.get("lastLiveClientInteractionProbeAcceptance"));
        boolean terminalTextAccepted = accepted(bridge, "lastLiveTerminalTextInputInteraction");
        boolean indexTextAccepted = accepted(bridge, "lastLiveIndexTextInputInteraction");
        boolean hudOverlayAccepted = accepted(bridge, "lastLiveHudOverlayRouteAcceptance");
        boolean mainMenuOverrideAccepted = accepted(bridge, "lastLiveMainMenuOverrideAcceptance");
        boolean holomapWikiNavigationAccepted = accepted(bridge, "lastLiveHoloMapWikiNavigationAcceptance");
        boolean phase5RouteSequenceAccepted = accepted(bridge, "lastLiveClientPhase5RouteSequenceAcceptance");
        boolean noScreenCrash = Boolean.TRUE.equals(bridge.get("noScreenCrash"));
        boolean terminalOpens = accepted(uiProbe) && strings(uiProbe, "surfaces").contains("TERMINAL");
        boolean indexOpens = accepted(uiProbe) && strings(uiProbe, "surfaces").contains("INDEX");
        boolean lensOpens = accepted(uiProbe) && strings(uiProbe, "surfaces").contains("LENS");
        boolean hudOpens = accepted(uiProbe) && strings(uiProbe, "surfaces").contains("HUD");
        boolean holomapOpens = accepted(uiProbe)
                && strings(uiProbe, "surfaces").contains("HOLOMAP")
                && holomapWikiNavigationAccepted;
        boolean wikiOpens = accepted(uiProbe)
                && strings(uiProbe, "surfaces").contains("WIKI")
                && holomapWikiNavigationAccepted;
        boolean customMainMenuAppears = accepted(uiProbe)
                && strings(uiProbe, "surfaces").contains("MAIN_MENU")
                && mainMenuOverrideAccepted;
        List<String> interactions = strings(interactionProbe, "interactions");
        boolean terminalCommandExecutes = accepted(interactionProbe)
                && terminalTextAccepted
                && interactions.contains("terminal_command");
        boolean indexSearches = accepted(interactionProbe)
                && indexTextAccepted
                && interactions.contains("index_search");
        boolean lensScans = accepted(interactionProbe) && interactions.contains("lens_scan");
        boolean hudUpdates = accepted(interactionProbe)
                && hudOverlayAccepted
                && interactions.contains("hud_update");
        boolean routeSetsMatch = strings(uiProbe, "surfaces").equals(REQUIRED_SURFACES)
                && interactions.equals(REQUIRED_INTERACTIONS);

        List<Map<String, Object>> checklist = List.of(
                checklistItem("terminal_opens", terminalOpens),
                checklistItem("terminal_command_executes", terminalCommandExecutes),
                checklistItem("index_opens_and_searches", indexOpens && indexSearches),
                checklistItem("lens_scans_target", lensOpens && lensScans),
                checklistItem("hud_updates_health_hazard_mission", hudOpens && hudUpdates),
                checklistItem("holomap_opens", holomapOpens),
                checklistItem("wiki_page_opens", wikiOpens),
                checklistItem("custom_main_menu_appears", customMainMenuAppears),
                checklistItem("no_screen_crash", noScreenCrash)
        );
        boolean accepted = routeSetsMatch
                && phase5RouteSequenceAccepted
                && checklist.stream().allMatch(item -> Boolean.TRUE.equals(item.get("passed")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("checklist", checklist);
        result.put("uiProbeAccepted", accepted(uiProbe));
        result.put("interactionProbeAccepted", accepted(interactionProbe));
        result.put("terminalTextAccepted", terminalTextAccepted);
        result.put("indexTextAccepted", indexTextAccepted);
        result.put("hudOverlayAccepted", hudOverlayAccepted);
        result.put("mainMenuOverrideAccepted", mainMenuOverrideAccepted);
        result.put("holomapWikiNavigationAccepted", holomapWikiNavigationAccepted);
        result.put("phase5RouteSequenceAccepted", phase5RouteSequenceAccepted);
        result.put("noScreenCrash", noScreenCrash);
        result.put("surfaces", strings(uiProbe, "surfaces"));
        result.put("interactions", interactions);
        result.put("effect", accepted ? "live_phase5:accepted:9" : "live_phase5:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", accepted);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> accepted = assess(bridge(true, true, true, true, true, true, true, true, true));
        Map<String, Object> rejectedNoUiProbe = assess(bridge(false, true, true, true, true, true, true, true, true));
        Map<String, Object> rejectedNoInteractionProbe = assess(bridge(true, false, true, true, true, true, true, true, true));
        Map<String, Object> rejectedNoHudOverlay = assess(bridge(true, true, true, true, false, true, true, true, true));
        Map<String, Object> rejectedNoMainMenuOverride = assess(bridge(true, true, true, true, true, false, true, true, true));
        Map<String, Object> rejectedNoHoloMapWikiNavigation = assess(bridge(true, true, true, true, true, true, false, true, true));
        Map<String, Object> rejectedNoPhase5RouteSequence = assess(bridge(true, true, true, true, true, true, true, false, true));
        return Map.ofEntries(
                Map.entry("livePhase5AcceptanceSmokeClass", EchoNativeAgent5LivePhase5Acceptance.class.getSimpleName()),
                Map.entry("accepted", accepted),
                Map.entry("rejectedNoUiProbe", rejectedNoUiProbe),
                Map.entry("rejectedNoInteractionProbe", rejectedNoInteractionProbe),
                Map.entry("rejectedNoHudOverlay", rejectedNoHudOverlay),
                Map.entry("rejectedNoMainMenuOverride", rejectedNoMainMenuOverride),
                Map.entry("rejectedNoHoloMapWikiNavigation", rejectedNoHoloMapWikiNavigation),
                Map.entry("rejectedNoPhase5RouteSequence", rejectedNoPhase5RouteSequence),
                Map.entry("passed", Boolean.TRUE.equals(accepted.get("accepted"))
                        && Boolean.TRUE.equals(accepted.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedNoUiProbe.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoUiProbe.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedNoInteractionProbe.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoInteractionProbe.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedNoHudOverlay.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoHudOverlay.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedNoMainMenuOverride.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoMainMenuOverride.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedNoHoloMapWikiNavigation.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoHoloMapWikiNavigation.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedNoPhase5RouteSequence.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoPhase5RouteSequence.get("serviceCodeExecuted"))),
                Map.entry("adapterCoreBridge", true),
                Map.entry("serviceCodeExecuted", true)
        );
    }

    private static Map<String, Object> bridge(
            boolean uiProbeAccepted,
            boolean interactionProbeAccepted,
            boolean terminalTextAccepted,
            boolean indexTextAccepted,
            boolean hudOverlayAccepted,
            boolean mainMenuOverrideAccepted,
            boolean holomapWikiNavigationAccepted,
            boolean phase5RouteSequenceAccepted,
            boolean noScreenCrash
    ) {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("lastLiveClientUiProbeAcceptance", Map.of(
                "accepted", uiProbeAccepted,
                "surfaces", REQUIRED_SURFACES
        ));
        bridge.put("lastLiveClientInteractionProbeAcceptance", Map.of(
                "accepted", interactionProbeAccepted,
                "interactions", REQUIRED_INTERACTIONS
        ));
        bridge.put("lastLiveTerminalTextInputInteraction", Map.of("accepted", terminalTextAccepted));
        bridge.put("lastLiveIndexTextInputInteraction", Map.of("accepted", indexTextAccepted));
        bridge.put("lastLiveHudOverlayRouteAcceptance", Map.of("accepted", hudOverlayAccepted));
        bridge.put("lastLiveMainMenuOverrideAcceptance", Map.of("accepted", mainMenuOverrideAccepted));
        bridge.put("lastLiveHoloMapWikiNavigationAcceptance", Map.of("accepted", holomapWikiNavigationAccepted));
        bridge.put("lastLiveClientPhase5RouteSequenceAcceptance", Map.of("accepted", phase5RouteSequenceAccepted));
        bridge.put("noScreenCrash", noScreenCrash);
        return bridge;
    }

    private static Map<String, Object> checklistItem(String id, boolean passed) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("passed", passed);
        return Map.copyOf(item);
    }

    private static boolean accepted(Map<String, Object> value) {
        return Boolean.TRUE.equals(value.get("accepted"));
    }

    private static boolean accepted(Map<String, Object> bridge, String key) {
        Object value = bridge.get(key);
        if (value instanceof Map<?, ?> map) {
            return Boolean.TRUE.equals(map.get("accepted"));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
