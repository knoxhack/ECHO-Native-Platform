package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveClientHostEvidenceAcceptanceSmoke {
    private EchoNativeAgent5LiveClientHostEvidenceAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> accepted = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true)
        );
        Map<String, Object> rejectedHeadlessOnly = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(false, true, false, false, false, true, true, true, true, true)
        );
        Map<String, Object> rejectedNoPhysicalInput = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, false, true, true, true, true, true)
        );
        Map<String, Object> rejectedNoRenderedSurface = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, false, true, true, true)
        );
        Map<String, Object> rejectedNoScreenClass = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, false)
        );
        Map<String, Object> rejectedNoLiveInteraction = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true,
                        false, true, true, true, true, true, true, true, true, true, true, true)
        );
        Map<String, Object> rejectedNoLiveSurfaceAcceptance = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveSurfaceAcceptance")
        );
        Map<String, Object> rejectedNoWindowFocus = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveWindowFocusAcceptance")
        );
        Map<String, Object> rejectedNoRenderCallback = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveRenderCallbackAcceptance")
        );
        Map<String, Object> rejectedNoScreenOwnership = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveScreenOwnershipAcceptance")
        );
        Map<String, Object> rejectedNoPhysicalPollLoop = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLivePhysicalPollLoopAcceptance")
        );
        Map<String, Object> rejectedNoPhysicalEventTranscript = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLivePhysicalEventTranscriptAcceptance")
        );
        Map<String, Object> rejectedNoPhysicalRouteEffectTranscript =
                EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                        withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                                "lastLivePhysicalRouteEffectTranscriptAcceptance")
                );
        Map<String, Object> rejectedNoPhysicalInputCoverage = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLivePhysicalInputCoverageAcceptance")
        );
        Map<String, Object> rejectedNoHudOverlayInteraction = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true,
                        true, true, true, true, false, true, true, true, true, true, true, true)
        );
        Map<String, Object> rejectedNoTextInputInteraction = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true,
                        true, true, true, true, true, false, true, true, true, true, true, true)
        );
        Map<String, Object> rejectedNoTextInputAcceptance = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutTextInputAcceptance(bridge(true, true, true, true, true, true, true, true, true, true))
        );
        Map<String, Object> rejectedNoTextInputCoverage = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveTextInputCoverageAcceptance")
        );
        Map<String, Object> rejectedNoRouteBoundTextCommand = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveRouteBoundTextCommandAcceptance")
        );
        Map<String, Object> rejectedNoRouteBoundLensScan = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveRouteBoundLensScanAcceptance")
        );
        Map<String, Object> rejectedNoRouteBoundHudUpdate = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveRouteBoundHudUpdateAcceptance")
        );
        Map<String, Object> rejectedNoRouteBoundHoloMapWiki = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveRouteBoundHoloMapWikiAcceptance")
        );
        Map<String, Object> rejectedNoUiProbe = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true,
                        true, true, true, true, true, true, false, true, true, true, true, true)
        );
        Map<String, Object> rejectedNoGeneratedInteractionProbe = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true,
                        true, true, true, true, true, true, true, false, true, true, true, true)
        );
        Map<String, Object> rejectedNoNotificationQueue = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveNotificationQueueAcceptance")
        );
        Map<String, Object> rejectedNoMissionObjective = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveMissionObjectiveAcceptance")
        );
        Map<String, Object> rejectedNoCoreTools = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveCoreToolsAcceptance")
        );
        Map<String, Object> rejectedNoSystemFlow = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveSystemFlowAcceptance")
        );
        Map<String, Object> rejectedNoInputFocusRouting = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveInputFocusRoutingAcceptance")
        );
        Map<String, Object> rejectedNoScreenStackStability = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveScreenStackStabilityAcceptance")
        );
        Map<String, Object> rejectedNoVisualFrame = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveVisualFrameAcceptance")
        );
        Map<String, Object> rejectedNoModuleSurfaceCatalog = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLiveModuleSurfaceCatalogAcceptance")
        );
        Map<String, Object> rejectedNoTerminalEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastTerminalEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoIndexEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastIndexEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoLensEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastLensEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoHudOverlayEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastHudOverlayEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoHoloMapEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastHoloMapEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoWikiEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastWikiEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoMainMenuEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastMainMenuEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoMissionLogEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastMissionLogEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoSettingsEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastSettingsEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoPauseEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastPauseEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoRecoveryEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastRecoveryEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoNotificationEndToEnd = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                withoutAccepted(bridge(true, true, true, true, true, true, true, true, true, true),
                        "lastNotificationEndToEndAcceptance")
        );
        Map<String, Object> rejectedNoLivePhase5 = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true,
                        true, true, true, true, true, true, true, true, true, true, true, false)
        );
        Map<String, Object> rejectedNoMainMenuOverride = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true,
                        true, true, true, true, true, true, true, true, false, true, true, true)
        );
        Map<String, Object> rejectedNoHoloMapWikiNavigation = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true,
                        true, true, true, true, true, true, true, true, true, false, true, true)
        );
        Map<String, Object> rejectedNoPhase5RouteSequence = EchoNativeAgent5LiveClientHostEvidenceAcceptance.assess(
                bridge(true, true, true, true, true, true, true, true, true, true,
                        true, true, true, true, true, true, true, true, true, true, false, true)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.TRUE.equals(accepted.get("serviceCodeExecuted"))
                && "live_client_host_evidence:accepted:EchoNativeDashboardScreen".equals(accepted.get("effect"))
                && Boolean.TRUE.equals(accepted.get("windowFocusSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLiveWindowFocusAccepted"))
                && Boolean.TRUE.equals(accepted.get("renderCallbackSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLiveRenderCallbackAccepted"))
                && Boolean.TRUE.equals(accepted.get("screenOwnershipSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLiveScreenOwnershipAccepted"))
                && Boolean.TRUE.equals(accepted.get("physicalPollLoopSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLivePhysicalPollLoopAccepted"))
                && Boolean.TRUE.equals(accepted.get("physicalEventTranscriptSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLivePhysicalEventTranscriptAccepted"))
                && Boolean.TRUE.equals(accepted.get("physicalRouteEffectTranscriptSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLivePhysicalRouteEffectTranscriptAccepted"))
                && Boolean.TRUE.equals(accepted.get("physicalInputCoverageSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLivePhysicalInputCoverageAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveSurfaceAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualSurfaceRouteAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualPhysicalInputAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveSurfaceRendered"))
                && Boolean.TRUE.equals(accepted.get("actualUiHostEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualHudOverlayRouteAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualTextInputAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveTextInputAcceptanceAccepted"))
                && Boolean.TRUE.equals(accepted.get("textInputCoverageSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLiveTextInputCoverageAccepted"))
                && Boolean.TRUE.equals(accepted.get("routeBoundTextCommandSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLiveRouteBoundTextCommandAccepted"))
                && Boolean.TRUE.equals(accepted.get("routeBoundLensScanSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLiveRouteBoundLensScanAccepted"))
                && Boolean.TRUE.equals(accepted.get("routeBoundHudUpdateSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLiveRouteBoundHudUpdateAccepted"))
                && Boolean.TRUE.equals(accepted.get("routeBoundHoloMapWikiSmokePassed"))
                && Boolean.TRUE.equals(accepted.get("actualLiveRouteBoundHoloMapWikiAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveClientUiProbeAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveClientInteractionProbeAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveNotificationQueueAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveMissionObjectiveAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveCoreToolsAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveSystemFlowAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveInputFocusRoutingAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveScreenStackStabilityAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveVisualFrameAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveModuleSurfaceCatalogAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveTerminalEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveIndexEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveLensEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveHudOverlayEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveHoloMapEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveWikiEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveMainMenuEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveMissionLogEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveSettingsEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLivePauseEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveRecoveryEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveNotificationEndToEndAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveMainMenuOverrideAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveHoloMapWikiNavigationAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLiveClientPhase5RouteSequenceAccepted"))
                && Boolean.TRUE.equals(accepted.get("actualLivePhase5Accepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("accepted"))
                && Boolean.TRUE.equals(rejectedHeadlessOnly.get("headlessOnly"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveWindowFocusAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveRenderCallbackAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveScreenOwnershipAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLivePhysicalPollLoopAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLivePhysicalEventTranscriptAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLivePhysicalRouteEffectTranscriptAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLivePhysicalInputCoverageAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveSurfaceAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualSurfaceRouteAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualPhysicalInputAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveSurfaceRendered"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualUiHostEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualHudOverlayRouteAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualTextInputAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveTextInputAcceptanceAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveTextInputCoverageAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveRouteBoundTextCommandAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveRouteBoundLensScanAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveRouteBoundHudUpdateAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveRouteBoundHoloMapWikiAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveClientUiProbeAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveClientInteractionProbeAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveNotificationQueueAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveMissionObjectiveAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveCoreToolsAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveSystemFlowAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveInputFocusRoutingAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveScreenStackStabilityAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveVisualFrameAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveModuleSurfaceCatalogAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveTerminalEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveIndexEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveLensEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveHudOverlayEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveHoloMapEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveWikiEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveMainMenuEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveMissionLogEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveSettingsEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLivePauseEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveRecoveryEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveNotificationEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveMainMenuOverrideAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveHoloMapWikiNavigationAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveClientPhase5RouteSequenceAccepted"))
                && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLivePhase5Accepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalInput.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRenderedSurface.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoScreenClass.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoLiveInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoLiveInteraction.get("actualSurfaceRouteAccepted"))
                && Boolean.FALSE.equals(rejectedNoLiveSurfaceAcceptance.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoLiveSurfaceAcceptance.get("actualLiveSurfaceAccepted"))
                && Boolean.FALSE.equals(rejectedNoWindowFocus.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoWindowFocus.get("actualLiveWindowFocusAccepted"))
                && Boolean.FALSE.equals(rejectedNoRenderCallback.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRenderCallback.get("actualLiveRenderCallbackAccepted"))
                && Boolean.FALSE.equals(rejectedNoScreenOwnership.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoScreenOwnership.get("actualLiveScreenOwnershipAccepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalPollLoop.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalPollLoop.get("actualLivePhysicalPollLoopAccepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalEventTranscript.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalEventTranscript.get(
                "actualLivePhysicalEventTranscriptAccepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalRouteEffectTranscript.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalRouteEffectTranscript.get(
                "actualLivePhysicalRouteEffectTranscriptAccepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalInputCoverage.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoPhysicalInputCoverage.get("actualLivePhysicalInputCoverageAccepted"))
                && Boolean.FALSE.equals(rejectedNoHudOverlayInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHudOverlayInteraction.get("actualHudOverlayRouteAccepted"))
                && Boolean.FALSE.equals(rejectedNoTextInputInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoTextInputInteraction.get("actualTextInputAccepted"))
                && Boolean.FALSE.equals(rejectedNoTextInputAcceptance.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoTextInputAcceptance.get("actualLiveTextInputAcceptanceAccepted"))
                && Boolean.FALSE.equals(rejectedNoTextInputCoverage.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoTextInputCoverage.get("actualLiveTextInputCoverageAccepted"))
                && Boolean.FALSE.equals(rejectedNoRouteBoundTextCommand.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRouteBoundTextCommand.get(
                "actualLiveRouteBoundTextCommandAccepted"))
                && Boolean.FALSE.equals(rejectedNoRouteBoundLensScan.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRouteBoundLensScan.get(
                "actualLiveRouteBoundLensScanAccepted"))
                && Boolean.FALSE.equals(rejectedNoRouteBoundHudUpdate.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRouteBoundHudUpdate.get(
                "actualLiveRouteBoundHudUpdateAccepted"))
                && Boolean.FALSE.equals(rejectedNoRouteBoundHoloMapWiki.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRouteBoundHoloMapWiki.get(
                "actualLiveRouteBoundHoloMapWikiAccepted"))
                && Boolean.FALSE.equals(rejectedNoUiProbe.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoUiProbe.get("actualLiveClientUiProbeAccepted"))
                && Boolean.FALSE.equals(rejectedNoGeneratedInteractionProbe.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoGeneratedInteractionProbe.get("actualLiveClientInteractionProbeAccepted"))
                && Boolean.FALSE.equals(rejectedNoNotificationQueue.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoNotificationQueue.get("actualLiveNotificationQueueAccepted"))
                && Boolean.FALSE.equals(rejectedNoMissionObjective.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMissionObjective.get("actualLiveMissionObjectiveAccepted"))
                && Boolean.FALSE.equals(rejectedNoCoreTools.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoCoreTools.get("actualLiveCoreToolsAccepted"))
                && Boolean.FALSE.equals(rejectedNoSystemFlow.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoSystemFlow.get("actualLiveSystemFlowAccepted"))
                && Boolean.FALSE.equals(rejectedNoInputFocusRouting.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoInputFocusRouting.get("actualLiveInputFocusRoutingAccepted"))
                && Boolean.FALSE.equals(rejectedNoScreenStackStability.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoScreenStackStability.get("actualLiveScreenStackStabilityAccepted"))
                && Boolean.FALSE.equals(rejectedNoVisualFrame.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoVisualFrame.get("actualLiveVisualFrameAccepted"))
                && Boolean.FALSE.equals(rejectedNoModuleSurfaceCatalog.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoModuleSurfaceCatalog.get("actualLiveModuleSurfaceCatalogAccepted"))
                && Boolean.FALSE.equals(rejectedNoTerminalEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoTerminalEndToEnd.get("actualLiveTerminalEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoIndexEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoIndexEndToEnd.get("actualLiveIndexEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoLensEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoLensEndToEnd.get("actualLiveLensEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoHudOverlayEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHudOverlayEndToEnd.get("actualLiveHudOverlayEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoHoloMapEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHoloMapEndToEnd.get("actualLiveHoloMapEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoWikiEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoWikiEndToEnd.get("actualLiveWikiEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoMainMenuEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMainMenuEndToEnd.get("actualLiveMainMenuEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoMissionLogEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMissionLogEndToEnd.get("actualLiveMissionLogEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoSettingsEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoSettingsEndToEnd.get("actualLiveSettingsEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoPauseEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoPauseEndToEnd.get("actualLivePauseEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoRecoveryEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRecoveryEndToEnd.get("actualLiveRecoveryEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoNotificationEndToEnd.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoNotificationEndToEnd.get("actualLiveNotificationEndToEndAccepted"))
                && Boolean.FALSE.equals(rejectedNoLivePhase5.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoLivePhase5.get("actualLivePhase5Accepted"))
                && Boolean.FALSE.equals(rejectedNoMainMenuOverride.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMainMenuOverride.get("actualLiveMainMenuOverrideAccepted"))
                && Boolean.FALSE.equals(rejectedNoHoloMapWikiNavigation.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHoloMapWikiNavigation.get("actualLiveHoloMapWikiNavigationAccepted"))
                && Boolean.FALSE.equals(rejectedNoPhase5RouteSequence.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoPhase5RouteSequence.get("actualLiveClientPhase5RouteSequenceAccepted"))
                && rejectedServiceCodeFalse(
                rejectedHeadlessOnly,
                rejectedNoPhysicalInput,
                rejectedNoRenderedSurface,
                rejectedNoScreenClass,
                rejectedNoLiveInteraction,
                rejectedNoLiveSurfaceAcceptance,
                rejectedNoWindowFocus,
                rejectedNoRenderCallback,
                rejectedNoScreenOwnership,
                rejectedNoPhysicalPollLoop,
                rejectedNoPhysicalEventTranscript,
                rejectedNoPhysicalRouteEffectTranscript,
                rejectedNoPhysicalInputCoverage,
                rejectedNoHudOverlayInteraction,
                rejectedNoTextInputInteraction,
                rejectedNoTextInputAcceptance,
                rejectedNoTextInputCoverage,
                rejectedNoRouteBoundTextCommand,
                rejectedNoRouteBoundLensScan,
                rejectedNoRouteBoundHudUpdate,
                rejectedNoRouteBoundHoloMapWiki,
                rejectedNoUiProbe,
                rejectedNoGeneratedInteractionProbe,
                rejectedNoNotificationQueue,
                rejectedNoMissionObjective,
                rejectedNoCoreTools,
                rejectedNoSystemFlow,
                rejectedNoInputFocusRouting,
                rejectedNoScreenStackStability,
                rejectedNoVisualFrame,
                rejectedNoModuleSurfaceCatalog,
                rejectedNoTerminalEndToEnd,
                rejectedNoIndexEndToEnd,
                rejectedNoLensEndToEnd,
                rejectedNoHudOverlayEndToEnd,
                rejectedNoHoloMapEndToEnd,
                rejectedNoWikiEndToEnd,
                rejectedNoMainMenuEndToEnd,
                rejectedNoMissionLogEndToEnd,
                rejectedNoSettingsEndToEnd,
                rejectedNoPauseEndToEnd,
                rejectedNoRecoveryEndToEnd,
                rejectedNoNotificationEndToEnd,
                rejectedNoLivePhase5,
                rejectedNoMainMenuOverride,
                rejectedNoHoloMapWikiNavigation,
                rejectedNoPhase5RouteSequence
        );
        return Map.ofEntries(
                Map.entry("liveClientHostEvidenceAcceptanceSmokeClass",
                        EchoNativeAgent5LiveClientHostEvidenceAcceptanceSmoke.class.getSimpleName()),
                Map.entry("accepted", accepted),
                Map.entry("rejectedHeadlessOnly", rejectedHeadlessOnly),
                Map.entry("rejectedNoPhysicalInput", rejectedNoPhysicalInput),
                Map.entry("rejectedNoRenderedSurface", rejectedNoRenderedSurface),
                Map.entry("rejectedNoScreenClass", rejectedNoScreenClass),
                Map.entry("rejectedNoLiveInteraction", rejectedNoLiveInteraction),
                Map.entry("rejectedNoLiveSurfaceAcceptance", rejectedNoLiveSurfaceAcceptance),
                Map.entry("rejectedNoWindowFocus", rejectedNoWindowFocus),
                Map.entry("rejectedNoRenderCallback", rejectedNoRenderCallback),
                Map.entry("rejectedNoScreenOwnership", rejectedNoScreenOwnership),
                Map.entry("rejectedNoPhysicalPollLoop", rejectedNoPhysicalPollLoop),
                Map.entry("rejectedNoPhysicalEventTranscript", rejectedNoPhysicalEventTranscript),
                Map.entry("rejectedNoPhysicalRouteEffectTranscript", rejectedNoPhysicalRouteEffectTranscript),
                Map.entry("rejectedNoPhysicalInputCoverage", rejectedNoPhysicalInputCoverage),
                Map.entry("rejectedNoHudOverlayInteraction", rejectedNoHudOverlayInteraction),
                Map.entry("rejectedNoTextInputInteraction", rejectedNoTextInputInteraction),
                Map.entry("rejectedNoTextInputAcceptance", rejectedNoTextInputAcceptance),
                Map.entry("rejectedNoTextInputCoverage", rejectedNoTextInputCoverage),
                Map.entry("rejectedNoRouteBoundTextCommand", rejectedNoRouteBoundTextCommand),
                Map.entry("rejectedNoRouteBoundLensScan", rejectedNoRouteBoundLensScan),
                Map.entry("rejectedNoRouteBoundHudUpdate", rejectedNoRouteBoundHudUpdate),
                Map.entry("rejectedNoRouteBoundHoloMapWiki", rejectedNoRouteBoundHoloMapWiki),
                Map.entry("rejectedNoUiProbe", rejectedNoUiProbe),
                Map.entry("rejectedNoGeneratedInteractionProbe", rejectedNoGeneratedInteractionProbe),
                Map.entry("rejectedNoNotificationQueue", rejectedNoNotificationQueue),
                Map.entry("rejectedNoMissionObjective", rejectedNoMissionObjective),
                Map.entry("rejectedNoCoreTools", rejectedNoCoreTools),
                Map.entry("rejectedNoSystemFlow", rejectedNoSystemFlow),
                Map.entry("rejectedNoInputFocusRouting", rejectedNoInputFocusRouting),
                Map.entry("rejectedNoScreenStackStability", rejectedNoScreenStackStability),
                Map.entry("rejectedNoVisualFrame", rejectedNoVisualFrame),
                Map.entry("rejectedNoModuleSurfaceCatalog", rejectedNoModuleSurfaceCatalog),
                Map.entry("rejectedNoTerminalEndToEnd", rejectedNoTerminalEndToEnd),
                Map.entry("rejectedNoIndexEndToEnd", rejectedNoIndexEndToEnd),
                Map.entry("rejectedNoLensEndToEnd", rejectedNoLensEndToEnd),
                Map.entry("rejectedNoHudOverlayEndToEnd", rejectedNoHudOverlayEndToEnd),
                Map.entry("rejectedNoHoloMapEndToEnd", rejectedNoHoloMapEndToEnd),
                Map.entry("rejectedNoWikiEndToEnd", rejectedNoWikiEndToEnd),
                Map.entry("rejectedNoMainMenuEndToEnd", rejectedNoMainMenuEndToEnd),
                Map.entry("rejectedNoMissionLogEndToEnd", rejectedNoMissionLogEndToEnd),
                Map.entry("rejectedNoSettingsEndToEnd", rejectedNoSettingsEndToEnd),
                Map.entry("rejectedNoPauseEndToEnd", rejectedNoPauseEndToEnd),
                Map.entry("rejectedNoRecoveryEndToEnd", rejectedNoRecoveryEndToEnd),
                Map.entry("rejectedNoNotificationEndToEnd", rejectedNoNotificationEndToEnd),
                Map.entry("rejectedNoLivePhase5", rejectedNoLivePhase5),
                Map.entry("rejectedNoMainMenuOverride", rejectedNoMainMenuOverride),
                Map.entry("rejectedNoHoloMapWikiNavigation", rejectedNoHoloMapWikiNavigation),
                Map.entry("rejectedNoPhase5RouteSequence", rejectedNoPhase5RouteSequence),
                Map.entry("adapterCoreBridge", true),
                Map.entry("serviceCodeExecuted", true),
                Map.entry("passed", passed)
        );
    }

    @SafeVarargs
    private static boolean rejectedServiceCodeFalse(Map<String, Object>... results) {
        for (Map<String, Object> result : results) {
            if (!Boolean.FALSE.equals(result.get("accepted"))
                    || !Boolean.FALSE.equals(result.get("serviceCodeExecuted"))) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Object> bridge(
            boolean clientAttached,
            boolean headlessAttached,
            boolean clientThreadAccepted,
            boolean windowHandlePresent,
            boolean physicalHotkeyPollingReady,
            boolean noScreenCrash,
            boolean surfaceRouteSmokePassed,
            boolean textInputSmokePassed,
            boolean hudRouteSmokePassed,
            boolean screenClassPresent
    ) {
        return bridge(
                clientAttached,
                headlessAttached,
                clientThreadAccepted,
                windowHandlePresent,
                physicalHotkeyPollingReady,
                noScreenCrash,
                surfaceRouteSmokePassed,
                textInputSmokePassed,
                hudRouteSmokePassed,
                screenClassPresent,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true
        );
    }

    private static Map<String, Object> bridge(
            boolean clientAttached,
            boolean headlessAttached,
            boolean clientThreadAccepted,
            boolean windowHandlePresent,
            boolean physicalHotkeyPollingReady,
            boolean noScreenCrash,
            boolean surfaceRouteSmokePassed,
            boolean textInputSmokePassed,
            boolean hudRouteSmokePassed,
            boolean screenClassPresent,
            boolean liveSurfaceRouteAccepted,
            boolean physicalInputAccepted,
            boolean liveSurfaceRendered,
            boolean uiHostEndToEndAccepted,
            boolean hudOverlayRouteAccepted,
            boolean textInputInteractionAccepted,
            boolean uiProbeAccepted,
            boolean generatedInteractionProbeAccepted,
            boolean mainMenuOverrideAccepted,
            boolean holomapWikiNavigationAccepted,
            boolean phase5RouteSequenceAccepted,
            boolean livePhase5Accepted
    ) {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("clientUiHostAttached", clientAttached);
        bridge.put("headlessUiHostAttached", headlessAttached);
        bridge.put("clientThreadAccepted", clientThreadAccepted);
        bridge.put("liveWindowHandlePresent", windowHandlePresent);
        bridge.put("physicalHotkeyPollingReady", physicalHotkeyPollingReady);
        bridge.put("noScreenCrash", noScreenCrash);
        bridge.put("liveSurfaceRouteAcceptanceSmoke", smoke(surfaceRouteSmokePassed));
        bridge.put("liveTextInputAcceptanceSmoke", smoke(textInputSmokePassed));
        bridge.put("liveHudOverlayRouteAcceptanceSmoke", smoke(hudRouteSmokePassed));
        bridge.put("liveModuleSurfaceCatalogAcceptanceSmoke", smoke(true));
        bridge.put("liveWindowFocusAcceptanceSmoke", EchoNativeAgent5LiveWindowFocusAcceptance.smoke());
        bridge.put("lastLiveWindowFocusAcceptance", accepted(true));
        bridge.put("liveRenderCallbackAcceptanceSmoke", EchoNativeAgent5LiveRenderCallbackAcceptance.smoke());
        bridge.put("lastLiveRenderCallbackAcceptance", accepted(true));
        bridge.put("liveScreenOwnershipAcceptanceSmoke", EchoNativeAgent5LiveScreenOwnershipAcceptance.smoke());
        bridge.put("lastLiveScreenOwnershipAcceptance", accepted(true));
        bridge.put("livePhysicalPollLoopAcceptanceSmoke", EchoNativeAgent5LivePhysicalPollLoopAcceptance.smoke());
        bridge.put("lastLivePhysicalPollLoopAcceptance", accepted(true));
        bridge.put("livePhysicalEventTranscriptAcceptanceSmoke",
                EchoNativeAgent5LivePhysicalEventTranscriptAcceptance.smoke());
        bridge.put("lastLivePhysicalEventTranscriptAcceptance", accepted(true));
        bridge.put("livePhysicalRouteEffectTranscriptAcceptanceSmoke",
                EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance.smoke());
        bridge.put("lastLivePhysicalRouteEffectTranscriptAcceptance", accepted(true));
        bridge.put("livePhysicalInputCoverageAcceptanceSmoke",
                EchoNativeAgent5LivePhysicalInputCoverageAcceptance.smoke());
        bridge.put("lastLivePhysicalInputCoverageAcceptance", accepted(true));
        bridge.put("lastLiveSurfaceAcceptance", accepted(liveSurfaceRouteAccepted));
        bridge.put("lastLiveSurfaceRouteAcceptance", accepted(liveSurfaceRouteAccepted));
        bridge.put("lastPhysicalInputAcceptance", accepted(physicalInputAccepted));
        bridge.put("lastLiveSurfaceRenderAcceptance", accepted(liveSurfaceRendered));
        bridge.put("lastUiHostEndToEndAcceptance", accepted(uiHostEndToEndAccepted));
        bridge.put("lastLiveHudOverlayRouteAcceptance", accepted(hudOverlayRouteAccepted));
        bridge.put("lastLiveTextInputInteraction", accepted(textInputInteractionAccepted));
        bridge.put("lastLiveTextInputAcceptance", textInputAcceptance(true));
        bridge.put("liveTextInputCoverageAcceptanceSmoke", EchoNativeAgent5LiveTextInputCoverageAcceptance.smoke());
        bridge.put("lastLiveTextInputCoverageAcceptance", accepted(true));
        bridge.put("liveRouteBoundTextCommandAcceptanceSmoke",
                EchoNativeAgent5LiveRouteBoundTextCommandAcceptance.smoke());
        bridge.put("lastLiveRouteBoundTextCommandAcceptance", accepted(true));
        bridge.put("liveRouteBoundLensScanAcceptanceSmoke",
                EchoNativeAgent5LiveRouteBoundLensScanAcceptance.smoke());
        bridge.put("lastLiveRouteBoundLensScanAcceptance", accepted(true));
        bridge.put("liveRouteBoundHudUpdateAcceptanceSmoke",
                EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance.smoke());
        bridge.put("lastLiveRouteBoundHudUpdateAcceptance", accepted(true));
        bridge.put("liveRouteBoundHoloMapWikiAcceptanceSmoke",
                EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance.smoke());
        bridge.put("lastLiveRouteBoundHoloMapWikiAcceptance", accepted(true));
        bridge.put("lastLiveClientUiProbeAcceptance", accepted(uiProbeAccepted));
        bridge.put("lastLiveClientInteractionProbeAcceptance", accepted(generatedInteractionProbeAccepted));
        bridge.put("lastLiveNotificationQueueAcceptance", accepted(true));
        bridge.put("lastLiveMissionObjectiveAcceptance", accepted(true));
        bridge.put("lastLiveCoreToolsAcceptance", accepted(true));
        bridge.put("lastLiveSystemFlowAcceptance", accepted(true));
        bridge.put("lastLiveInputFocusRoutingAcceptance", accepted(true));
        bridge.put("lastLiveScreenStackStabilityAcceptance", accepted(true));
        bridge.put("lastLiveVisualFrameAcceptance", accepted(true));
        bridge.put("lastLiveModuleSurfaceCatalogAcceptance", accepted(true));
        bridge.put("lastTerminalEndToEndAcceptance", accepted(true));
        bridge.put("lastIndexEndToEndAcceptance", accepted(true));
        bridge.put("lastLensEndToEndAcceptance", accepted(true));
        bridge.put("lastHudOverlayEndToEndAcceptance", accepted(true));
        bridge.put("lastHoloMapEndToEndAcceptance", accepted(true));
        bridge.put("lastWikiEndToEndAcceptance", accepted(true));
        bridge.put("lastMainMenuEndToEndAcceptance", accepted(true));
        bridge.put("lastMissionLogEndToEndAcceptance", accepted(true));
        bridge.put("lastSettingsEndToEndAcceptance", accepted(true));
        bridge.put("lastPauseEndToEndAcceptance", accepted(true));
        bridge.put("lastRecoveryEndToEndAcceptance", accepted(true));
        bridge.put("lastNotificationEndToEndAcceptance", accepted(true));
        bridge.put("lastLiveMainMenuOverrideAcceptance", accepted(mainMenuOverrideAccepted));
        bridge.put("lastLiveHoloMapWikiNavigationAcceptance", accepted(holomapWikiNavigationAccepted));
        bridge.put("lastLiveClientPhase5RouteSequenceAcceptance", accepted(phase5RouteSequenceAccepted));
        bridge.put("lastLivePhase5Acceptance", accepted(livePhase5Accepted));
        bridge.put("lastOpenedSurface", "TERMINAL");
        bridge.put("screenClass", screenClassPresent
                ? "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen"
                : "");
        return bridge;
    }

    private static Map<String, Object> smoke(boolean passed) {
        return Map.of("passed", passed, "serviceCodeExecuted", true);
    }

    private static Map<String, Object> accepted(boolean accepted) {
        return Map.of("accepted", accepted, "serviceCodeExecuted", true);
    }

    private static Map<String, Object> textInputAcceptance(boolean accepted) {
        return Map.of(
                "passed", accepted,
                "terminal", accepted(accepted),
                "index", accepted(accepted),
                "serviceCodeExecuted", true
        );
    }

    private static Map<String, Object> withoutAccepted(Map<String, Object> source, String key) {
        Map<String, Object> bridge = new LinkedHashMap<>(source);
        bridge.put(key, accepted(false));
        return bridge;
    }

    private static Map<String, Object> withoutTextInputAcceptance(Map<String, Object> source) {
        Map<String, Object> bridge = new LinkedHashMap<>(source);
        bridge.put("lastLiveTextInputAcceptance", textInputAcceptance(false));
        return bridge;
    }
}
