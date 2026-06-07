package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5HeadlessUiBridgeReadinessAcceptanceSmoke {
    private static final String SCREEN_CLASS = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";

    private EchoNativeAgent5HeadlessUiBridgeReadinessAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> accepted = EchoNativeAgent5HeadlessUiBridgeReadinessAcceptance.assess(
                fixture(false, true, true, SCREEN_CLASS, true, true),
                SCREEN_CLASS
        );
        Map<String, Object> rejectedLiveAttached = EchoNativeAgent5HeadlessUiBridgeReadinessAcceptance.assess(
                fixture(true, true, true, SCREEN_CLASS, true, true),
                SCREEN_CLASS
        );
        Map<String, Object> rejectedNoTerminal = EchoNativeAgent5HeadlessUiBridgeReadinessAcceptance.assess(
                fixture(false, false, true, SCREEN_CLASS, true, true),
                SCREEN_CLASS
        );
        Map<String, Object> rejectedNoHotkeys = EchoNativeAgent5HeadlessUiBridgeReadinessAcceptance.assess(
                fixture(false, true, false, SCREEN_CLASS, true, true),
                SCREEN_CLASS
        );
        Map<String, Object> rejectedScreenMismatch = EchoNativeAgent5HeadlessUiBridgeReadinessAcceptance.assess(
                fixture(false, true, true, "dev.echo.nativeplatform.generated.OtherScreen", true, true),
                SCREEN_CLASS
        );
        Map<String, Object> rejectedLiveHostOverclaim = EchoNativeAgent5HeadlessUiBridgeReadinessAcceptance.assess(
                fixture(false, true, true, SCREEN_CLASS, false, true),
                SCREEN_CLASS
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "headless_ui_bridge_readiness:accepted:EchoNativeDashboardScreen".equals(accepted.get("effect"))
                && Boolean.TRUE.equals(accepted.get("serviceCodeExecuted"))
                && Boolean.FALSE.equals(rejectedLiveAttached.get("accepted"))
                && Boolean.FALSE.equals(rejectedLiveAttached.get("serviceCodeExecuted"))
                && Boolean.FALSE.equals(rejectedNoTerminal.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoTerminal.get("serviceCodeExecuted"))
                && Boolean.FALSE.equals(rejectedNoHotkeys.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHotkeys.get("serviceCodeExecuted"))
                && Boolean.FALSE.equals(rejectedScreenMismatch.get("accepted"))
                && Boolean.FALSE.equals(rejectedScreenMismatch.get("serviceCodeExecuted"))
                && Boolean.FALSE.equals(rejectedLiveHostOverclaim.get("accepted"))
                && Boolean.FALSE.equals(rejectedLiveHostOverclaim.get("serviceCodeExecuted"));
        return Map.of(
                "headlessUiBridgeReadinessAcceptanceSmokeClass",
                EchoNativeAgent5HeadlessUiBridgeReadinessAcceptanceSmoke.class.getSimpleName(),
                "accepted", accepted,
                "rejectedLiveAttached", rejectedLiveAttached,
                "rejectedNoTerminal", rejectedNoTerminal,
                "rejectedNoHotkeys", rejectedNoHotkeys,
                "rejectedScreenMismatch", rejectedScreenMismatch,
                "rejectedLiveHostOverclaim", rejectedLiveHostOverclaim,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true,
                "passed", passed
        );
    }

    private static Map<String, Object> fixture(
            boolean clientUiHostAttached,
            boolean terminalEvidence,
            boolean hotkeys,
            String screenClass,
            boolean honestLiveHostRejection,
            boolean smokeEvidence
    ) {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("installed", true);
        bridge.put("fallbackHostAttached", true);
        bridge.put("headlessUiHostAttached", true);
        bridge.put("clientUiHostAttached", clientUiHostAttached);
        bridge.put("clientThreadAccepted", clientUiHostAttached);
        bridge.put("screenClass", screenClass);
        bridge.put("hotkeys", hotkeys ? List.of(
                "M:Terminal",
                "G:Index Catalog",
                "R:Index Recipe",
                "U:Index Uses",
                "B:Index Bookmark",
                "Left Alt:Lens Deep Scan",
                "J:HoloMap",
                "K:HoloMap Minimap",
                "]:HoloMap Zoom In",
                "[:HoloMap Zoom Out",
                "\\:HoloMap Corner",
                "N:SignalOS Terminal",
                "X:Ashfall Drone Recall",
                "C:Ashfall Drone Scan",
                "Y:Ashfall Drone Scout",
                "Z:Ashfall Drone Status",
                "B:Ashfall Drone Assist"
        ) : List.of("M:Terminal"));
        bridge.put("screenIds", List.of(
                "echo:main_menu",
                "echoterminal:terminal",
                "echoindex:index",
                "echolens:lens",
                "echohudcore:hud",
                "echonotificationcore:queue",
                "echoscreencore:mission_log",
                "echoscreencore:settings",
                "echoscreencore:pause_flow",
                "echoscreencore:death_recovery",
                "echoholomap:holomap",
                "echowiki:wiki",
                "signalos:terminal",
                "echoashfallprotocol:drone"
        ));
        bridge.put("agent5DataSources", Map.of("terminal", Map.of("command", "status")));
        for (String key : List.of(
                "terminalFallbackReady",
                "indexFallbackReady",
                "lensFallbackReady",
                "hudFallbackReady",
                "notificationQueueReady",
                "missionLogFallbackReady",
                "settingsFallbackReady",
                "pauseFlowFallbackReady",
                "deathRecoveryFallbackReady",
                "holomapFallbackReady",
                "wikiFallbackReady",
                "customMainMenuReady",
                "screenFocusRoutingReady",
                "textInputRoutingReady",
                "mouseRoutingReady",
                "notificationQueueDispatched",
                "missionLogTracksActiveMission",
                "settingsProfileApplied",
                "pauseFlowResumesPreviousScreen",
                "deathRecoveryActionExecuted",
                "noScreenCrash"
        )) {
            bridge.put(key, terminalEvidence || !key.equals("terminalFallbackReady"));
        }
        for (String key : List.of(
                "lastTerminalEndToEndAcceptance",
                "lastIndexEndToEndAcceptance",
                "lastLensEndToEndAcceptance",
                "lastHoloMapEndToEndAcceptance",
                "lastWikiEndToEndAcceptance",
                "lastMissionLogEndToEndAcceptance",
                "lastSettingsEndToEndAcceptance",
                "lastPauseEndToEndAcceptance",
                "lastRecoveryEndToEndAcceptance",
                "lastNotificationEndToEndAcceptance",
                "lastMainMenuEndToEndAcceptance",
                "lastLiveCoreToolsAcceptance",
                "lastLiveMissionObjectiveAcceptance",
                "lastLiveSystemFlowAcceptance",
                "lastLiveHoloMapWikiNavigationAcceptance",
                "lastLiveNotificationQueueAcceptance",
                "lastUiHostInteractionStateAcceptance"
        )) {
            bridge.put(key, Map.of("accepted", terminalEvidence));
        }
        for (String key : List.of(
                "hotkeyBridgeSmoke",
                "hostEventTranscriptSmoke",
                "physicalHotkeyPollingSmoke",
                "screenLifecycleSmoke",
                "liveSurfaceAcceptanceSmoke",
                "physicalInputAcceptanceSmoke",
                "liveSurfaceRenderAcceptanceSmoke",
                "uiHostEndToEndAcceptanceSmoke"
        )) {
            bridge.put(key, Map.of("passed", smokeEvidence));
        }
        bridge.put("lastLiveClientHostEvidenceAcceptance", Map.of(
                "accepted", !honestLiveHostRejection,
                "serviceCodeExecuted", !honestLiveHostRejection,
                "headlessOnly", honestLiveHostRejection
        ));
        return bridge;
    }
}
