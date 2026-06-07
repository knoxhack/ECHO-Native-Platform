package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveClientHostEvidenceAcceptance {
    private EchoNativeAgent5LiveClientHostEvidenceAcceptance() {
    }

    public static Map<String, Object> assess(Map<String, Object> bridge) {
        boolean clientAttached = flag(bridge, "clientUiHostAttached");
        boolean headlessAttached = flag(bridge, "headlessUiHostAttached");
        boolean clientThreadAccepted = flag(bridge, "clientThreadAccepted");
        boolean windowHandlePresent = flag(bridge, "liveWindowHandlePresent");
        boolean physicalHotkeyPollingReady = flag(bridge, "physicalHotkeyPollingReady");
        boolean noScreenCrash = flag(bridge, "noScreenCrash");
        boolean surfaceRouteSmokePassed = smokePassed(bridge, "liveSurfaceRouteAcceptanceSmoke");
        boolean textInputSmokePassed = smokePassed(bridge, "liveTextInputAcceptanceSmoke");
        boolean hudRouteSmokePassed = smokePassed(bridge, "liveHudOverlayRouteAcceptanceSmoke");
        boolean moduleCatalogSmokePassed = smokePassed(bridge, "liveModuleSurfaceCatalogAcceptanceSmoke");
        boolean windowFocusSmokePassed = smokePassed(bridge, "liveWindowFocusAcceptanceSmoke");
        boolean actualLiveWindowFocusAccepted =
                clientAttached && accepted(bridge, "lastLiveWindowFocusAcceptance");
        boolean renderCallbackSmokePassed = smokePassed(bridge, "liveRenderCallbackAcceptanceSmoke");
        boolean actualLiveRenderCallbackAccepted =
                clientAttached && accepted(bridge, "lastLiveRenderCallbackAcceptance");
        boolean screenOwnershipSmokePassed = smokePassed(bridge, "liveScreenOwnershipAcceptanceSmoke");
        boolean actualLiveScreenOwnershipAccepted =
                clientAttached && accepted(bridge, "lastLiveScreenOwnershipAcceptance");
        boolean physicalPollLoopSmokePassed = smokePassed(bridge, "livePhysicalPollLoopAcceptanceSmoke");
        boolean actualLivePhysicalPollLoopAccepted =
                clientAttached && accepted(bridge, "lastLivePhysicalPollLoopAcceptance");
        boolean physicalEventTranscriptSmokePassed =
                smokePassed(bridge, "livePhysicalEventTranscriptAcceptanceSmoke");
        boolean actualLivePhysicalEventTranscriptAccepted =
                clientAttached && accepted(bridge, "lastLivePhysicalEventTranscriptAcceptance");
        boolean physicalRouteEffectTranscriptSmokePassed =
                smokePassed(bridge, "livePhysicalRouteEffectTranscriptAcceptanceSmoke");
        boolean actualLivePhysicalRouteEffectTranscriptAccepted =
                clientAttached && accepted(bridge, "lastLivePhysicalRouteEffectTranscriptAcceptance");
        boolean physicalInputCoverageSmokePassed =
                smokePassed(bridge, "livePhysicalInputCoverageAcceptanceSmoke");
        boolean actualLivePhysicalInputCoverageAccepted =
                clientAttached && accepted(bridge, "lastLivePhysicalInputCoverageAcceptance");
        boolean actualLiveSurfaceAccepted =
                clientAttached && accepted(bridge, "lastLiveSurfaceAcceptance");
        boolean actualSurfaceRouteAccepted =
                clientAttached && accepted(bridge, "lastLiveSurfaceRouteAcceptance");
        boolean actualPhysicalInputAccepted =
                clientAttached && accepted(bridge, "lastPhysicalInputAcceptance");
        boolean actualLiveSurfaceRendered =
                clientAttached && accepted(bridge, "lastLiveSurfaceRenderAcceptance");
        boolean actualUiHostEndToEndAccepted =
                clientAttached && accepted(bridge, "lastUiHostEndToEndAcceptance");
        boolean actualHudOverlayRouteAccepted =
                clientAttached && accepted(bridge, "lastLiveHudOverlayRouteAcceptance");
        boolean actualTextInputAccepted =
                clientAttached && accepted(bridge, "lastLiveTextInputInteraction");
        boolean actualLiveTextInputAcceptanceAccepted =
                clientAttached && textInputAcceptancePassed(bridge, "lastLiveTextInputAcceptance");
        boolean textInputCoverageSmokePassed =
                smokePassed(bridge, "liveTextInputCoverageAcceptanceSmoke");
        boolean actualLiveTextInputCoverageAccepted =
                clientAttached && accepted(bridge, "lastLiveTextInputCoverageAcceptance");
        boolean routeBoundTextCommandSmokePassed =
                smokePassed(bridge, "liveRouteBoundTextCommandAcceptanceSmoke");
        boolean actualLiveRouteBoundTextCommandAccepted =
                clientAttached && accepted(bridge, "lastLiveRouteBoundTextCommandAcceptance");
        boolean routeBoundLensScanSmokePassed =
                smokePassed(bridge, "liveRouteBoundLensScanAcceptanceSmoke");
        boolean actualLiveRouteBoundLensScanAccepted =
                clientAttached && accepted(bridge, "lastLiveRouteBoundLensScanAcceptance");
        boolean routeBoundHudUpdateSmokePassed =
                smokePassed(bridge, "liveRouteBoundHudUpdateAcceptanceSmoke");
        boolean actualLiveRouteBoundHudUpdateAccepted =
                clientAttached && accepted(bridge, "lastLiveRouteBoundHudUpdateAcceptance");
        boolean routeBoundHoloMapWikiSmokePassed =
                smokePassed(bridge, "liveRouteBoundHoloMapWikiAcceptanceSmoke");
        boolean actualLiveRouteBoundHoloMapWikiAccepted =
                clientAttached && accepted(bridge, "lastLiveRouteBoundHoloMapWikiAcceptance");
        boolean actualLiveClientUiProbeAccepted =
                clientAttached && accepted(bridge, "lastLiveClientUiProbeAcceptance");
        boolean actualLiveClientInteractionProbeAccepted =
                clientAttached && accepted(bridge, "lastLiveClientInteractionProbeAcceptance");
        boolean actualLiveNotificationQueueAccepted =
                clientAttached && accepted(bridge, "lastLiveNotificationQueueAcceptance");
        boolean actualLiveMissionObjectiveAccepted =
                clientAttached && accepted(bridge, "lastLiveMissionObjectiveAcceptance");
        boolean actualLiveCoreToolsAccepted =
                clientAttached && accepted(bridge, "lastLiveCoreToolsAcceptance");
        boolean actualLiveSystemFlowAccepted =
                clientAttached && accepted(bridge, "lastLiveSystemFlowAcceptance");
        boolean actualLiveInputFocusRoutingAccepted =
                clientAttached && accepted(bridge, "lastLiveInputFocusRoutingAcceptance");
        boolean actualLiveScreenStackStabilityAccepted =
                clientAttached && accepted(bridge, "lastLiveScreenStackStabilityAcceptance");
        boolean actualLiveVisualFrameAccepted =
                clientAttached && accepted(bridge, "lastLiveVisualFrameAcceptance");
        boolean actualLiveModuleSurfaceCatalogAccepted =
                clientAttached && accepted(bridge, "lastLiveModuleSurfaceCatalogAcceptance");
        boolean actualLiveTerminalEndToEndAccepted =
                clientAttached && accepted(bridge, "lastTerminalEndToEndAcceptance");
        boolean actualLiveIndexEndToEndAccepted =
                clientAttached && accepted(bridge, "lastIndexEndToEndAcceptance");
        boolean actualLiveLensEndToEndAccepted =
                clientAttached && accepted(bridge, "lastLensEndToEndAcceptance");
        boolean actualLiveHudOverlayEndToEndAccepted =
                clientAttached && accepted(bridge, "lastHudOverlayEndToEndAcceptance");
        boolean actualLiveHoloMapEndToEndAccepted =
                clientAttached && accepted(bridge, "lastHoloMapEndToEndAcceptance");
        boolean actualLiveWikiEndToEndAccepted =
                clientAttached && accepted(bridge, "lastWikiEndToEndAcceptance");
        boolean actualLiveMainMenuEndToEndAccepted =
                clientAttached && accepted(bridge, "lastMainMenuEndToEndAcceptance");
        boolean actualLiveMissionLogEndToEndAccepted =
                clientAttached && accepted(bridge, "lastMissionLogEndToEndAcceptance");
        boolean actualLiveSettingsEndToEndAccepted =
                clientAttached && accepted(bridge, "lastSettingsEndToEndAcceptance");
        boolean actualLivePauseEndToEndAccepted =
                clientAttached && accepted(bridge, "lastPauseEndToEndAcceptance");
        boolean actualLiveRecoveryEndToEndAccepted =
                clientAttached && accepted(bridge, "lastRecoveryEndToEndAcceptance");
        boolean actualLiveNotificationEndToEndAccepted =
                clientAttached && accepted(bridge, "lastNotificationEndToEndAcceptance");
        boolean actualLiveMainMenuOverrideAccepted =
                clientAttached && accepted(bridge, "lastLiveMainMenuOverrideAcceptance");
        boolean actualLiveHoloMapWikiNavigationAccepted =
                clientAttached && accepted(bridge, "lastLiveHoloMapWikiNavigationAcceptance");
        boolean actualLiveClientPhase5RouteSequenceAccepted =
                clientAttached && accepted(bridge, "lastLiveClientPhase5RouteSequenceAcceptance");
        boolean actualLivePhase5Accepted =
                clientAttached && accepted(bridge, "lastLivePhase5Acceptance");
        String lastOpenedSurface = text(bridge.get("lastOpenedSurface"));
        String screenClass = text(bridge.get("screenClass"));
        boolean screenClassPresent = !screenClass.isBlank();
        boolean accepted = clientAttached
                && clientThreadAccepted
                && windowHandlePresent
                && physicalHotkeyPollingReady
                && noScreenCrash
                && surfaceRouteSmokePassed
                && textInputSmokePassed
                && hudRouteSmokePassed
                && moduleCatalogSmokePassed
                && windowFocusSmokePassed
                && actualLiveWindowFocusAccepted
                && renderCallbackSmokePassed
                && actualLiveRenderCallbackAccepted
                && screenOwnershipSmokePassed
                && actualLiveScreenOwnershipAccepted
                && physicalPollLoopSmokePassed
                && actualLivePhysicalPollLoopAccepted
                && physicalEventTranscriptSmokePassed
                && actualLivePhysicalEventTranscriptAccepted
                && physicalRouteEffectTranscriptSmokePassed
                && actualLivePhysicalRouteEffectTranscriptAccepted
                && physicalInputCoverageSmokePassed
                && actualLivePhysicalInputCoverageAccepted
                && actualLiveSurfaceAccepted
                && actualSurfaceRouteAccepted
                && actualPhysicalInputAccepted
                && actualLiveSurfaceRendered
                && actualUiHostEndToEndAccepted
                && actualHudOverlayRouteAccepted
                && actualTextInputAccepted
                && actualLiveTextInputAcceptanceAccepted
                && textInputCoverageSmokePassed
                && actualLiveTextInputCoverageAccepted
                && routeBoundTextCommandSmokePassed
                && actualLiveRouteBoundTextCommandAccepted
                && routeBoundLensScanSmokePassed
                && actualLiveRouteBoundLensScanAccepted
                && routeBoundHudUpdateSmokePassed
                && actualLiveRouteBoundHudUpdateAccepted
                && routeBoundHoloMapWikiSmokePassed
                && actualLiveRouteBoundHoloMapWikiAccepted
                && actualLiveClientUiProbeAccepted
                && actualLiveClientInteractionProbeAccepted
                && actualLiveNotificationQueueAccepted
                && actualLiveMissionObjectiveAccepted
                && actualLiveCoreToolsAccepted
                && actualLiveSystemFlowAccepted
                && actualLiveInputFocusRoutingAccepted
                && actualLiveScreenStackStabilityAccepted
                && actualLiveVisualFrameAccepted
                && actualLiveModuleSurfaceCatalogAccepted
                && actualLiveTerminalEndToEndAccepted
                && actualLiveIndexEndToEndAccepted
                && actualLiveLensEndToEndAccepted
                && actualLiveHudOverlayEndToEndAccepted
                && actualLiveHoloMapEndToEndAccepted
                && actualLiveWikiEndToEndAccepted
                && actualLiveMainMenuEndToEndAccepted
                && actualLiveMissionLogEndToEndAccepted
                && actualLiveSettingsEndToEndAccepted
                && actualLivePauseEndToEndAccepted
                && actualLiveRecoveryEndToEndAccepted
                && actualLiveNotificationEndToEndAccepted
                && actualLiveMainMenuOverrideAccepted
                && actualLiveHoloMapWikiNavigationAccepted
                && actualLiveClientPhase5RouteSequenceAccepted
                && actualLivePhase5Accepted
                && screenClassPresent;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("clientUiHostAttached", clientAttached);
        result.put("headlessUiHostAttached", headlessAttached);
        result.put("clientThreadAccepted", clientThreadAccepted);
        result.put("liveWindowHandlePresent", windowHandlePresent);
        result.put("physicalHotkeyPollingReady", physicalHotkeyPollingReady);
        result.put("noScreenCrash", noScreenCrash);
        result.put("surfaceRouteSmokePassed", surfaceRouteSmokePassed);
        result.put("textInputSmokePassed", textInputSmokePassed);
        result.put("hudRouteSmokePassed", hudRouteSmokePassed);
        result.put("moduleCatalogSmokePassed", moduleCatalogSmokePassed);
        result.put("windowFocusSmokePassed", windowFocusSmokePassed);
        result.put("actualLiveWindowFocusAccepted", actualLiveWindowFocusAccepted);
        result.put("renderCallbackSmokePassed", renderCallbackSmokePassed);
        result.put("actualLiveRenderCallbackAccepted", actualLiveRenderCallbackAccepted);
        result.put("screenOwnershipSmokePassed", screenOwnershipSmokePassed);
        result.put("actualLiveScreenOwnershipAccepted", actualLiveScreenOwnershipAccepted);
        result.put("physicalPollLoopSmokePassed", physicalPollLoopSmokePassed);
        result.put("actualLivePhysicalPollLoopAccepted", actualLivePhysicalPollLoopAccepted);
        result.put("physicalEventTranscriptSmokePassed", physicalEventTranscriptSmokePassed);
        result.put("actualLivePhysicalEventTranscriptAccepted", actualLivePhysicalEventTranscriptAccepted);
        result.put("physicalRouteEffectTranscriptSmokePassed", physicalRouteEffectTranscriptSmokePassed);
        result.put("actualLivePhysicalRouteEffectTranscriptAccepted", actualLivePhysicalRouteEffectTranscriptAccepted);
        result.put("physicalInputCoverageSmokePassed", physicalInputCoverageSmokePassed);
        result.put("actualLivePhysicalInputCoverageAccepted", actualLivePhysicalInputCoverageAccepted);
        result.put("actualLiveSurfaceAccepted", actualLiveSurfaceAccepted);
        result.put("actualSurfaceRouteAccepted", actualSurfaceRouteAccepted);
        result.put("actualPhysicalInputAccepted", actualPhysicalInputAccepted);
        result.put("actualLiveSurfaceRendered", actualLiveSurfaceRendered);
        result.put("actualUiHostEndToEndAccepted", actualUiHostEndToEndAccepted);
        result.put("actualHudOverlayRouteAccepted", actualHudOverlayRouteAccepted);
        result.put("actualTextInputAccepted", actualTextInputAccepted);
        result.put("actualLiveTextInputAcceptanceAccepted", actualLiveTextInputAcceptanceAccepted);
        result.put("textInputCoverageSmokePassed", textInputCoverageSmokePassed);
        result.put("actualLiveTextInputCoverageAccepted", actualLiveTextInputCoverageAccepted);
        result.put("routeBoundTextCommandSmokePassed", routeBoundTextCommandSmokePassed);
        result.put("actualLiveRouteBoundTextCommandAccepted", actualLiveRouteBoundTextCommandAccepted);
        result.put("routeBoundLensScanSmokePassed", routeBoundLensScanSmokePassed);
        result.put("actualLiveRouteBoundLensScanAccepted", actualLiveRouteBoundLensScanAccepted);
        result.put("routeBoundHudUpdateSmokePassed", routeBoundHudUpdateSmokePassed);
        result.put("actualLiveRouteBoundHudUpdateAccepted", actualLiveRouteBoundHudUpdateAccepted);
        result.put("routeBoundHoloMapWikiSmokePassed", routeBoundHoloMapWikiSmokePassed);
        result.put("actualLiveRouteBoundHoloMapWikiAccepted", actualLiveRouteBoundHoloMapWikiAccepted);
        result.put("actualLiveClientUiProbeAccepted", actualLiveClientUiProbeAccepted);
        result.put("actualLiveClientInteractionProbeAccepted", actualLiveClientInteractionProbeAccepted);
        result.put("actualLiveNotificationQueueAccepted", actualLiveNotificationQueueAccepted);
        result.put("actualLiveMissionObjectiveAccepted", actualLiveMissionObjectiveAccepted);
        result.put("actualLiveCoreToolsAccepted", actualLiveCoreToolsAccepted);
        result.put("actualLiveSystemFlowAccepted", actualLiveSystemFlowAccepted);
        result.put("actualLiveInputFocusRoutingAccepted", actualLiveInputFocusRoutingAccepted);
        result.put("actualLiveScreenStackStabilityAccepted", actualLiveScreenStackStabilityAccepted);
        result.put("actualLiveVisualFrameAccepted", actualLiveVisualFrameAccepted);
        result.put("actualLiveModuleSurfaceCatalogAccepted", actualLiveModuleSurfaceCatalogAccepted);
        result.put("actualLiveTerminalEndToEndAccepted", actualLiveTerminalEndToEndAccepted);
        result.put("actualLiveIndexEndToEndAccepted", actualLiveIndexEndToEndAccepted);
        result.put("actualLiveLensEndToEndAccepted", actualLiveLensEndToEndAccepted);
        result.put("actualLiveHudOverlayEndToEndAccepted", actualLiveHudOverlayEndToEndAccepted);
        result.put("actualLiveHoloMapEndToEndAccepted", actualLiveHoloMapEndToEndAccepted);
        result.put("actualLiveWikiEndToEndAccepted", actualLiveWikiEndToEndAccepted);
        result.put("actualLiveMainMenuEndToEndAccepted", actualLiveMainMenuEndToEndAccepted);
        result.put("actualLiveMissionLogEndToEndAccepted", actualLiveMissionLogEndToEndAccepted);
        result.put("actualLiveSettingsEndToEndAccepted", actualLiveSettingsEndToEndAccepted);
        result.put("actualLivePauseEndToEndAccepted", actualLivePauseEndToEndAccepted);
        result.put("actualLiveRecoveryEndToEndAccepted", actualLiveRecoveryEndToEndAccepted);
        result.put("actualLiveNotificationEndToEndAccepted", actualLiveNotificationEndToEndAccepted);
        result.put("actualLiveMainMenuOverrideAccepted", actualLiveMainMenuOverrideAccepted);
        result.put("actualLiveHoloMapWikiNavigationAccepted", actualLiveHoloMapWikiNavigationAccepted);
        result.put("actualLiveClientPhase5RouteSequenceAccepted", actualLiveClientPhase5RouteSequenceAccepted);
        result.put("actualLivePhase5Accepted", actualLivePhase5Accepted);
        result.put("lastOpenedSurface", lastOpenedSurface);
        result.put("screenClass", screenClass);
        result.put("screenClassPresent", screenClassPresent);
        result.put("headlessOnly", headlessAttached && !clientAttached);
        result.put("effect", accepted
                ? "live_client_host_evidence:accepted:" + simpleName(screenClass)
                : "live_client_host_evidence:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", accepted);
        return Map.copyOf(result);
    }

    private static boolean smokePassed(Map<String, Object> bridge, String key) {
        Object value = bridge.get(key);
        if (value instanceof Map<?, ?> map) {
            return Boolean.TRUE.equals(map.get("passed"));
        }
        return switch (key) {
            case "liveSurfaceRouteAcceptanceSmoke" -> accepted(bridge, "lastLiveSurfaceRouteAcceptance");
            case "liveTextInputAcceptanceSmoke" -> textInputAcceptancePassed(bridge, "lastLiveTextInputAcceptance");
            case "liveHudOverlayRouteAcceptanceSmoke" -> accepted(bridge, "lastLiveHudOverlayRouteAcceptance");
            case "liveModuleSurfaceCatalogAcceptanceSmoke" -> accepted(bridge, "lastLiveModuleSurfaceCatalogAcceptance");
            case "liveWindowFocusAcceptanceSmoke" -> accepted(bridge, "lastLiveWindowFocusAcceptance");
            case "liveRenderCallbackAcceptanceSmoke" -> accepted(bridge, "lastLiveRenderCallbackAcceptance");
            case "liveScreenOwnershipAcceptanceSmoke" -> accepted(bridge, "lastLiveScreenOwnershipAcceptance");
            case "livePhysicalPollLoopAcceptanceSmoke" -> accepted(bridge, "lastLivePhysicalPollLoopAcceptance");
            case "livePhysicalEventTranscriptAcceptanceSmoke" -> accepted(bridge, "lastLivePhysicalEventTranscriptAcceptance");
            case "livePhysicalRouteEffectTranscriptAcceptanceSmoke" ->
                    accepted(bridge, "lastLivePhysicalRouteEffectTranscriptAcceptance");
            case "livePhysicalInputCoverageAcceptanceSmoke" -> accepted(bridge, "lastLivePhysicalInputCoverageAcceptance");
            case "liveTextInputCoverageAcceptanceSmoke" -> accepted(bridge, "lastLiveTextInputCoverageAcceptance");
            case "liveRouteBoundTextCommandAcceptanceSmoke" -> accepted(bridge, "lastLiveRouteBoundTextCommandAcceptance");
            case "liveRouteBoundLensScanAcceptanceSmoke" -> accepted(bridge, "lastLiveRouteBoundLensScanAcceptance");
            case "liveRouteBoundHudUpdateAcceptanceSmoke" -> accepted(bridge, "lastLiveRouteBoundHudUpdateAcceptance");
            case "liveRouteBoundHoloMapWikiAcceptanceSmoke" -> accepted(bridge, "lastLiveRouteBoundHoloMapWikiAcceptance");
            default -> false;
        };
    }

    private static boolean accepted(Map<String, Object> bridge, String key) {
        Object value = bridge.get(key);
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Map<?, ?> map) {
            return Boolean.TRUE.equals(map.get("accepted"));
        }
        return false;
    }

    private static boolean textInputAcceptancePassed(Map<String, Object> bridge, String key) {
        Object value = bridge.get(key);
        if (value instanceof Map<?, ?> map) {
            if (!Boolean.TRUE.equals(map.get("passed"))) {
                return false;
            }
            Object terminal = map.get("terminal");
            Object index = map.get("index");
            return terminal instanceof Map<?, ?> terminalMap
                    && index instanceof Map<?, ?> indexMap
                    && Boolean.TRUE.equals(terminalMap.get("accepted"))
                    && Boolean.TRUE.equals(indexMap.get("accepted"));
        }
        return false;
    }

    private static boolean flag(Map<String, Object> bridge, String key) {
        return Boolean.TRUE.equals(bridge.get(key));
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
