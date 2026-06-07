package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5HeadlessUiBridgeReadinessAcceptance {
    private static final List<String> REQUIRED_READY_FLAGS = List.of(
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
    );
    private static final List<String> REQUIRED_HOTKEYS = List.of(
            "M:",
            "G:",
            "R:",
            "U:",
            "B:",
            "Left Alt:",
            "J:",
            "K:",
            "]:",
            "[:",
            "\\:",
            "N:",
            "X:",
            "C:",
            "Y:",
            "Z:"
    );
    private static final List<String> REQUIRED_SCREEN_IDS = List.of(
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
    );
    private static final List<String> REQUIRED_ACCEPTED_KEYS = List.of(
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
            "lastLiveCoreToolsAcceptance",
            "lastLiveMissionObjectiveAcceptance",
            "lastLiveSystemFlowAcceptance",
            "lastLiveHoloMapWikiNavigationAcceptance",
            "lastLiveNotificationQueueAcceptance",
            "lastUiHostInteractionStateAcceptance"
    );
    private static final List<String> REQUIRED_PASSED_SMOKES = List.of(
            "hotkeyBridgeSmoke",
            "hostEventTranscriptSmoke",
            "physicalHotkeyPollingSmoke",
            "screenLifecycleSmoke",
            "liveSurfaceAcceptanceSmoke",
            "physicalInputAcceptanceSmoke",
            "liveSurfaceRenderAcceptanceSmoke",
            "uiHostEndToEndAcceptanceSmoke"
    );

    private EchoNativeAgent5HeadlessUiBridgeReadinessAcceptance() {
    }

    public static Map<String, Object> assess(Map<String, Object> bridge, String expectedScreenClass) {
        boolean installed = flag(bridge, "installed");
        boolean fallbackHostAttached = flag(bridge, "fallbackHostAttached");
        boolean headlessUiHostAttached = flag(bridge, "headlessUiHostAttached");
        boolean clientUiHostAttached = flag(bridge, "clientUiHostAttached");
        boolean clientThreadAccepted = flag(bridge, "clientThreadAccepted");
        String screenClass = text(bridge.get("screenClass"));
        String expectedClass = text(expectedScreenClass);
        boolean screenClassMatches = !screenClass.isBlank()
                && (screenClass.equals(expectedClass) || screenClass.endsWith("." + simpleName(expectedClass)));
        boolean readyFlagsPresent = REQUIRED_READY_FLAGS.stream().allMatch(key -> flag(bridge, key));
        boolean hotkeysReady = REQUIRED_HOTKEYS.stream().allMatch(prefix -> listContainsPrefix(bridge.get("hotkeys"), prefix));
        boolean screenIdsReady = REQUIRED_SCREEN_IDS.stream().allMatch(screenId -> listContains(bridge.get("screenIds"), screenId));
        boolean dataSourcesReady = bridge.get("agent5DataSources") instanceof Map<?, ?>;
        boolean acceptedEvidenceReady = REQUIRED_ACCEPTED_KEYS.stream().allMatch(key -> accepted(bridge, key));
        boolean smokeEvidenceReady = REQUIRED_PASSED_SMOKES.stream().allMatch(key -> smokePassed(bridge, key));
        Map<String, Object> liveHostEvidence = object(bridge.get("lastLiveClientHostEvidenceAcceptance"));
        boolean liveHostRejectedHonesty = Boolean.FALSE.equals(liveHostEvidence.get("accepted"))
                && Boolean.FALSE.equals(liveHostEvidence.get("serviceCodeExecuted"))
                && Boolean.TRUE.equals(liveHostEvidence.get("headlessOnly"));
        boolean accepted = installed
                && fallbackHostAttached
                && headlessUiHostAttached
                && !clientUiHostAttached
                && !clientThreadAccepted
                && screenClassMatches
                && readyFlagsPresent
                && hotkeysReady
                && screenIdsReady
                && dataSourcesReady
                && acceptedEvidenceReady
                && smokeEvidenceReady
                && liveHostRejectedHonesty;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("installed", installed);
        result.put("fallbackHostAttached", fallbackHostAttached);
        result.put("headlessUiHostAttached", headlessUiHostAttached);
        result.put("clientUiHostAttached", clientUiHostAttached);
        result.put("clientThreadAccepted", clientThreadAccepted);
        result.put("screenClass", screenClass);
        result.put("expectedScreenClass", expectedClass);
        result.put("screenClassMatches", screenClassMatches);
        result.put("readyFlagsPresent", readyFlagsPresent);
        result.put("hotkeysReady", hotkeysReady);
        result.put("screenIdsReady", screenIdsReady);
        result.put("dataSourcesReady", dataSourcesReady);
        result.put("acceptedEvidenceReady", acceptedEvidenceReady);
        result.put("smokeEvidenceReady", smokeEvidenceReady);
        result.put("liveHostRejectedHonesty", liveHostRejectedHonesty);
        result.put("minecraftRuntimeAccessed", false);
        result.put("effect", accepted
                ? "headless_ui_bridge_readiness:accepted:" + simpleName(expectedClass)
                : "headless_ui_bridge_readiness:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", accepted);
        return Map.copyOf(result);
    }

    private static boolean flag(Map<String, Object> values, String key) {
        return Boolean.TRUE.equals(values.get(key));
    }

    private static boolean accepted(Map<String, Object> bridge, String key) {
        Object value = bridge.get(key);
        if (value instanceof Boolean flag) {
            return flag;
        }
        return Boolean.TRUE.equals(object(value).get("accepted"));
    }

    private static boolean smokePassed(Map<String, Object> bridge, String key) {
        if (Boolean.TRUE.equals(object(bridge.get(key)).get("passed"))) {
            return true;
        }
        return switch (key) {
            case "liveSurfaceAcceptanceSmoke" -> accepted(bridge, "lastLiveSurfaceAcceptance");
            case "physicalInputAcceptanceSmoke" -> accepted(bridge, "lastPhysicalInputAcceptance");
            case "liveSurfaceRenderAcceptanceSmoke" -> accepted(bridge, "lastLiveSurfaceRenderAcceptance");
            case "uiHostEndToEndAcceptanceSmoke" -> accepted(bridge, "lastUiHostEndToEndAcceptance");
            default -> false;
        };
    }

    private static boolean listContains(Object value, String expected) {
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (expected.equals(String.valueOf(item))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean listContainsPrefix(Object value, String prefix) {
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (String.valueOf(item).startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
