package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderJsonSupport;
import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class EchoNativeAgent5UiBridgeContractVerifier {
    private static final String NATIVE_RUNTIME_HOST_ID_PROPERTY = "echo.native.runtime.host.id";
    private static final String MISSING_SELECTED_RUNTIME_HOST_ID = "echo-contract-missing-selected-runtime-host";
    private static final String LIMITED_SELECTED_RUNTIME_HOST_ID = "echo-contract-limited-selected-runtime-host";
    private static final String MUTATING_SELECTED_RUNTIME_HOST_ID = "echo-contract-mutating-selected-runtime-host";
    private static final String QUEUED_SELECTED_RUNTIME_HOST_ID = "echo-contract-queued-selected-runtime-host";
    private static final String NOOP_SELECTED_RUNTIME_HOST_ID = "echo-contract-noop-selected-runtime-host";

    private EchoNativeAgent5UiBridgeContractVerifier() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, Object> contract = EchoNativeLiveUiBridge.contractSnapshot();
        require("echo-native-loader".equals(contract.get("runtimeId")), "native runtime id must match");
        require(Boolean.TRUE.equals(contract.get("adapterCoreBridge")), "contract must be AdapterCore-backed");
        require(Boolean.FALSE.equals(contract.get("standaloneDuplicateGameplaySystem")),
                "contract must not be a standalone duplicate");
        requireListContains(contract, "screenIds", "echo:main_menu");
        requireListContains(contract, "screenIds", "echoterminal:terminal");
        requireListContains(contract, "screenIds", "echoindex:index");
        requireListContains(contract, "screenIds", "echolens:lens");
        requireListContains(contract, "screenIds", "echohudcore:hud");
        requireListContains(contract, "screenIds", "echonotificationcore:queue");
        requireListContains(contract, "screenIds", "echoscreencore:mission_log");
        requireListContains(contract, "screenIds", "echoscreencore:settings");
        requireListContains(contract, "screenIds", "echoscreencore:pause_flow");
        requireListContains(contract, "screenIds", "echoscreencore:death_recovery");
        requireListContains(contract, "screenIds", "echoholomap:holomap");
        requireListContains(contract, "screenIds", "echowiki:wiki");
        requireListContains(contract, "screenIds", "signalos:terminal");
        requireListContains(contract, "screenIds", "echoashfallprotocol:drone");
        requireListContains(contract, "features", "ui_reference_audit_smoke_executes");
        requireListContains(contract, "features", "ui_runtime_equivalence_audit_smoke_executes");
        requireListContains(contract, "features", "screencore_primitive_execution_smoke_executes");
        requireListContains(contract, "features", "phase5_ui_parity_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_client_attachment_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_client_host_evidence_acceptance_smoke_executes");
        requireListContains(contract, "features", "headless_ui_bridge_readiness_acceptance_smoke_executes");
        requireListContains(contract, "features", "adaptercore_runtime_bridge_guard_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_surface_route_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_text_input_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_hud_overlay_route_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_main_menu_override_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_notification_queue_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_holomap_wiki_navigation_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_system_flow_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_core_tools_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_mission_objective_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_input_focus_routing_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_screen_stack_stability_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_visual_frame_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_module_surface_catalog_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_physical_event_transcript_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_physical_route_effect_transcript_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_route_bound_text_command_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_route_bound_lens_scan_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_route_bound_hud_update_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_route_bound_holomap_wiki_acceptance_smoke_executes");
        requireListContains(contract, "features", "terminal_command_executes");
        requireListContains(contract, "features", "terminal_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "index_opens_and_searches");
        requireListContains(contract, "features", "index_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "lens_scans_target");
        requireListContains(contract, "features", "lens_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "notification_queue_dispatches");
        requireListContains(contract, "features", "notification_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "mission_log_opens_and_tracks_active_mission");
        requireListContains(contract, "features", "mission_log_update_smoke_executes");
        requireListContains(contract, "features", "mission_log_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "settings_opens_and_applies_profile");
        requireListContains(contract, "features", "settings_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "pause_flow_opens_and_resumes_previous_screen");
        requireListContains(contract, "features", "pause_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "death_recovery_screen_opens_and_recovers");
        requireListContains(contract, "features", "recovery_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "screencore_contract_primitives_execute");
        requireListContains(contract, "features", "ui_data_sources_drive_all_agent5_surfaces");
        requireListContains(contract, "features", "screen_focus_and_input_routing_execute");
        requireListContains(contract, "features", "focus_manager_smoke_executes");
        requireListContains(contract, "features", "text_editing_smoke_executes");
        requireListContains(contract, "features", "mouse_activation_smoke_executes");
        requireListContains(contract, "features", "list_navigation_smoke_executes");
        requireListContains(contract, "features", "notification_dismiss_smoke_executes");
        requireListContains(contract, "features", "settings_adjustment_smoke_executes");
        requireListContains(contract, "features", "pause_option_activation_smoke_executes");
        requireListContains(contract, "features", "adapter_ui_handlers_execute");
        requireListContains(contract, "features", "holomap_wiki_handlers_execute");
        requireListContains(contract, "features", "native_surface_render_models_execute");
        requireListContains(contract, "features", "surface_renderer_classes_execute");
        requireListContains(contract, "features", "input_action_router_classes_execute");
        requireListContains(contract, "features", "screen_host_models_execute");
        requireListContains(contract, "features", "screen_stack_execution_smoke_executes");
        requireListContains(contract, "features", "screen_lifecycle_smoke_executes");
        requireListContains(contract, "features", "screen_lifecycle_actions_execute");
        requireListContains(contract, "features", "module_surface_renderers_execute");
        requireListContains(contract, "features", "all_module_surface_renderers_execute");
        requireListContains(contract, "features", "theme_application_smoke_executes");
        requireListContains(contract, "features", "ui_host_smoke_snapshots_execute");
        requireListContains(contract, "features", "ui_host_interaction_smoke_executes");
        requireListContains(contract, "features", "ui_host_full_surface_interactions_execute");
        requireListContains(contract, "features", "main_menu_override_smoke_executes");
        requireListContains(contract, "features", "main_menu_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "hud_overlay_smoke_executes");
        requireListContains(contract, "features", "hud_overlay_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "hotkey_bridge_smoke_executes");
        requireListContains(contract, "features", "notification_queue_smoke_executes");
        requireListContains(contract, "features", "main_menu_option_activation_smoke_executes");
        requireListContains(contract, "features", "initial_focus_smoke_executes");
        requireListContains(contract, "features", "hud_update_smoke_executes");
        requireListContains(contract, "features", "camera_cinematic_smoke_executes");
        requireListContains(contract, "features", "rendercore_layout_smoke_executes");
        requireListContains(contract, "features", "host_event_transcript_smoke_executes");
        requireListContains(contract, "features", "physical_hotkey_polling_smoke_executes");
        requireListContains(contract, "features", "live_surface_acceptance_smoke_executes");
        requireListContains(contract, "features", "physical_input_acceptance_smoke_executes");
        requireListContains(contract, "features", "live_surface_render_acceptance_smoke_executes");
        requireListContains(contract, "features", "ui_host_interaction_state_acceptance_smoke_executes");
        requireListContains(contract, "features", "ui_host_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "holomap_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "wiki_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "signalos_end_to_end_acceptance_smoke_executes");
        requireListContains(contract, "features", "ashfall_drone_hotkey_route_executes");
        Map<String, Object> dataSources = object(contract.get("agent5DataSources"));
        Map<String, Object> terminalData = object(dataSources.get("terminal"));
        Map<String, Object> indexData = object(dataSources.get("index"));
        Map<String, Object> lensData = object(dataSources.get("lens"));
        Map<String, Object> hudData = object(dataSources.get("hud"));
        Map<String, Object> missionLogData = object(dataSources.get("missionLog"));
        Map<String, Object> settingsData = object(dataSources.get("settings"));
        Map<String, Object> pauseFlowData = object(dataSources.get("pauseFlow"));
        Map<String, Object> deathRecoveryData = object(dataSources.get("deathRecovery"));
        Map<String, Object> holomapData = object(dataSources.get("holomap"));
        Map<String, Object> wikiData = object(dataSources.get("wiki"));
        Map<String, Object> signalosData = object(dataSources.get("signalos"));
        Map<String, Object> ashfallDroneData = object(dataSources.get("ashfallDrone"));
        Map<String, Object> cameraData = object(dataSources.get("camera"));
        Map<String, Object> cinematicData = object(dataSources.get("cinematic"));
        require(!dataSources.isEmpty(), "contract must expose Agent 5 UI data sources");
        require("signalos:terminal".equals(signalosData.get("screenId")),
                "SignalOS data source must expose its native terminal screen id");
        require("echoashfallprotocol:drone".equals(ashfallDroneData.get("screenId")),
                "Ashfall drone data source must expose its native drone surface id");
        require(list(ashfallDroneData, "commands").equals(List.of("recall", "scan", "scout", "status", "toggle_assist")),
                "Ashfall drone data source must expose the real client command packet actions");
        require(list(ashfallDroneData, "keys").equals(List.of("X", "C", "Y", "Z", "B")),
                "Ashfall drone data source must expose the real client key mapping order");
        require("status".equals(contract.get("terminalCommand")), "terminal command must match reference");
        require("status".equals(terminalData.get("command")), "terminal data source command must match reference");
        require(EchoNativeAgent5UiExpectedValues.terminalOutput().equals(terminalData.get("readyLine")),
                "terminal data source output must come from terminal page data");
        require("ashfall".equals(contract.get("indexQuery")), "index query must match reference");
        require("ashfall".equals(indexData.get("query")), "index data source query must match reference");
        require(EchoNativeAgent5UiExpectedValues.indexOutput().equals(indexData.get("result")),
                "index data source result must come from index entry data");
        require(EchoNativeAgent5UiExpectedValues.lensTarget().equals(contract.get("lensTarget")),
                "lens target must match source-backed scan profile");
        require(EchoNativeAgent5UiExpectedValues.lensTarget().equals(lensData.get("target")),
                "lens data source target must match source-backed scan profile");
        require(EchoNativeAgent5UiExpectedValues.lensOutput().equals(lensData.get("result")),
                "lens data source result must come from scan profile data");
        Map<String, Object> expectedHud = EchoNativeAgent5UiExpectedValues.hud();
        require(number(expectedHud.get("health")).equals(number(hudData.get("health"))),
                "HUD health data source must match registry data");
        require(expectedHud.get("hazard").equals(hudData.get("hazard")),
                "HUD hazard data source must match registry data");
        require("echoashfallprotocol:secure_crash_outpost".equals(hudData.get("missionId")),
                "HUD mission id must be sourced from the Ashfall MissionCore starter mission");
        require("TRACKED".equals(hudData.get("missionStatus")),
                "HUD mission status must follow the MissionCore mission log state");
        require(EchoNativeAgent5UiExpectedValues.missionObjective().equals(hudData.get("mission")),
                "HUD mission line must come from the MissionCore mission objective");
        require("echoashfallprotocol:secure_crash_outpost".equals(contract.get("activeMissionId")),
                "active mission id must match reference");
        require("echoashfallprotocol:secure_crash_outpost".equals(missionLogData.get("missionId")),
                "mission log data source mission id must match reference");
        require(number(missionLogData.get("recordCount")) > 0,
                "mission log data source must load real Ashfall MissionCore records");
        require(EchoNativeAgent5UiExpectedValues.missionObjective().equals(contract.get("activeMissionObjective")),
                "active mission objective must come from mission data");
        require("TRACKED".equals(contract.get("activeMissionStatus")), "active mission status must be tracked");
        require("ashfall-accessible".equals(contract.get("settingsProfile")), "settings profile must match reference");
        require("ashfall-accessible".equals(settingsData.get("profile")),
                "settings data source profile must match reference");
        require("ashfall-agent5".equals(contract.get("settingsTheme")), "settings theme must match reference");
        require("keyboard_mouse".equals(contract.get("settingsInputMode")), "settings input mode must match reference");
        require(Boolean.TRUE.equals(contract.get("settingsSubtitles")), "settings subtitles must be enabled");
        require("echowiki:wiki".equals(contract.get("pauseResumeTarget")),
                "pause flow resume target must match reference");
        require("echowiki:wiki".equals(pauseFlowData.get("resumeTarget")),
                "pause flow data source resume target must match reference");
        require(list(contract, "pauseOptions").contains("Resume"), "pause flow must expose resume");
        require("recover".equals(contract.get("recoveryAction")), "recovery action must match reference");
        require("recover".equals(deathRecoveryData.get("action")), "recovery data source action must match reference");
        require(deathRecoveryData.get("recoveryPoint").equals(contract.get("recoveryPoint")),
                "recovery point must come from recovery data");
        require("RECOVERED".equals(contract.get("recoveryStatus")), "recovery status must match reference");
        require(Integer.valueOf(35).equals(number(contract.get("recoveryHealth"))),
                "recovery health must match reference");
        require("echoashfallprotocol:first_month_field_intel".equals(holomapData.get("layer")),
                "holomap data source layer must match reference");
        require(EchoNativeAgent5UiExpectedValues.wiki().get("page").equals(wikiData.get("page")),
                "wiki data source page must match wiki article data");
        require("over_shoulder".equals(cameraData.get("mode")), "camera data source mode must match reference");
        require(Integer.valueOf(72).equals(number(cameraData.get("fov"))),
                "camera data source FOV must match reference");
        require(terminalData.get("title").equals(cinematicData.get("cue")),
                "cinematic data source cue must come from terminal page data");
        require(Boolean.TRUE.equals(cinematicData.get("letterbox")),
                "cinematic data source must enable the letterbox cue");
        requireNativeUiReferenceAuditSmokeExecutes();
        requireNativeUiRuntimeEquivalenceAuditSmokeExecutes();
        requireNativeScreenCorePrimitiveExecutionSmokeExecutes();
        requireNativePhase5UiParityAcceptanceSmokeExecutes();
        requireNativeLiveClientAttachmentAcceptanceSmokeExecutes();
        requireNativeLiveClientHostEvidenceAcceptanceSmokeExecutes();
        requireNativeHeadlessUiBridgeReadinessAcceptanceSmokeExecutes();
        requireNativeAdapterCoreRuntimeBridgeGuardAcceptanceSmokeExecutes();
        requireNativeLiveClientUiProbeAcceptanceSmokeExecutes();
        requireNativeLiveClientInteractionProbeAcceptanceSmokeExecutes();
        requireNativeLiveClientPhase5RouteSequenceAcceptanceSmokeExecutes();
        requireNativeLivePhase5AcceptanceSmokeExecutes();
        requireNativeLiveSurfaceRouteAcceptanceSmokeExecutes();
        requireNativeLiveTextInputAcceptanceSmokeExecutes();
        requireNativeLiveHudOverlayRouteAcceptanceSmokeExecutes();
        requireNativeLiveMainMenuOverrideAcceptanceSmokeExecutes();
        requireNativeLiveNotificationQueueAcceptanceSmokeExecutes();
        requireNativeLiveHoloMapWikiNavigationAcceptanceSmokeExecutes();
        requireNativeLiveSystemFlowAcceptanceSmokeExecutes();
        requireNativeLiveCoreToolsAcceptanceSmokeExecutes();
        requireNativeLiveMissionObjectiveAcceptanceSmokeExecutes();
        requireNativeLiveInputFocusRoutingAcceptanceSmokeExecutes();
        requireNativeLiveScreenStackStabilityAcceptanceSmokeExecutes();
        requireNativeLiveVisualFrameAcceptanceSmokeExecutes();
        requireNativeLiveModuleSurfaceCatalogAcceptanceSmokeExecutes();
        requireNativeLiveRenderCallbackAcceptanceSmokeExecutes();
        requireNativeLiveScreenOwnershipAcceptanceSmokeExecutes();
        requireNativeLivePhysicalPollLoopAcceptanceSmokeExecutes();
        requireNativeLivePhysicalEventTranscriptAcceptanceSmokeExecutes();
        requireNativeLivePhysicalRouteEffectTranscriptAcceptanceSmokeExecutes();
        requireNativeLiveRouteBoundTextCommandAcceptanceSmokeExecutes();
        requireNativeLiveRouteBoundLensScanAcceptanceSmokeExecutes();
        requireNativeLiveRouteBoundHudUpdateAcceptanceSmokeExecutes();
        requireNativeLiveRouteBoundHoloMapWikiAcceptanceSmokeExecutes();
        requireNativeTerminalEndToEndAcceptanceSmokeExecutes();
        requireNativeIndexEndToEndAcceptanceSmokeExecutes();
        requireNativeLensEndToEndAcceptanceSmokeExecutes();
        requireNativeUiRuntimeActionsAreHostDerived();
        requireNativeEchoModuleReportsDoNotCountAsPlayableActions();
        requireNativeAshfallPowerNodeRequiresCompletedAdapterCoreChain();
        requireNativeAshfallGridRequiresCompletedAdapterCoreChain();
        requireNativeAshfallGeneratorAndProcessorRequireCompletedTick();
        requireNativeAshfallWaterHazardAndResearchRequireCompletedAdapterCoreChains();
        requireNativeAshfallConsumablesRequireInventoryRemoval();
        requireMissingSelectedRuntimeHostRejectsUiActions();
        requireSelectedRuntimeHostCapabilitiesRejectUnsupportedUiActions();
        requireSelectedRuntimeHostMutatesSupportedUiActions();
        requireSelectedRuntimeHostQueuedResultsRejectUiActions();
        requireSelectedRuntimeHostNoopResultsRejectUiActions();
        requireNativeSurfaceRenderModelsExecute();
        requireNativeInputActionRouterClassesExecute();
        requireNativeFocusManagerSmokeExecutes();
        requireNativeInitialFocusSmokeExecutes();
        requireNativeTextEditingSmokeExecutes();
        requireNativeMouseActivationSmokeExecutes();
        requireNativeListNavigationSmokeExecutes();
        requireNativeNotificationDismissSmokeExecutes();
        requireNativeNotificationEndToEndAcceptanceSmokeExecutes();
        requireNativeSettingsAdjustmentSmokeExecutes();
        requireNativeSettingsEndToEndAcceptanceSmokeExecutes();
        requireNativePauseOptionActivationSmokeExecutes();
        requireNativePauseEndToEndAcceptanceSmokeExecutes();
        requireNativeRecoveryEndToEndAcceptanceSmokeExecutes();
        requireNativeMissionLogUpdateSmokeExecutes();
        requireNativeMissionLogEndToEndAcceptanceSmokeExecutes();
        requireNativeScreenHostModelsExecute();
        requireNativeScreenStackExecutionSmokeExecutes();
        requireNativeScreenLifecycleSmokeExecutes();
        requireNativeModuleSurfaceRenderersExecute();
        requireNativeThemeApplicationSmokeExecutes();
        requireNativeUiHostSmokeSnapshotsExecute();
        requireNativeUiHostInteractionSmokeExecutes();
        requireNativeMainMenuOverrideSmokeExecutes();
        requireNativeMainMenuEndToEndAcceptanceSmokeExecutes();
        requireNativeWorldSetupCreateAcceptanceSmokeExecutes();
        requireNativeHudOverlaySmokeExecutes();
        requireNativeHudOverlayEndToEndAcceptanceSmokeExecutes();
        requireNativeHotkeyBridgeSmokeExecutes();
        requireNativeNotificationQueueSmokeExecutes();
        requireNativeHudUpdateSmokeExecutes();
        requireNativeCameraCinematicSmokeExecutes();
        requireNativeRenderCoreLayoutSmokeExecutes();
        requireNativeHostEventTranscriptSmokeExecutes();
        requireNativePhysicalHotkeyPollingSmokeExecutes();
        requireNativeLiveSurfaceAcceptanceSmokeExecutes();
        requireNativePhysicalInputAcceptanceSmokeExecutes();
        requireNativeLiveSurfaceRenderAcceptanceSmokeExecutes();
        requireNativeUiHostInteractionStateAcceptanceSmokeExecutes();
        requireNativeUiHostEndToEndAcceptanceSmokeExecutes();
        requireNativeHoloMapEndToEndAcceptanceSmokeExecutes();
        requireNativeWikiEndToEndAcceptanceSmokeExecutes();
        requireNativeMainMenuOptionActivationSmokeExecutes();
        requireNativeUiHandlersExecute(terminalData, indexData, lensData, hudData, deathRecoveryData,
                holomapData, wikiData);
        for (String primitive : List.of(
                "EchoScreen",
                "EchoScreenStack",
                "EchoScreenRoute",
                "EchoHudLayer",
                "EchoInputAction",
                "EchoTheme",
                "EchoWidget",
                "EchoTextInput",
                "EchoButton",
                "EchoListView",
                "EchoTerminalBuffer",
                "EchoNotification"
        )) {
            requireListContains(contract, "screenCorePrimitives", primitive);
        }
        require("top_left_safe_area".equals(contract.get("notificationAnchor")),
                "notification anchor must match HUD safe-area reference");
        require(list(contract, "notificationMessages").size() == 2,
                "notification queue must define two reference messages");
        require("guarded_title_screen_replacement".equals(contract.get("mainMenuOverrideStrategy")),
                "main menu override strategy must be guarded title-screen replacement");
        requireGeneratedScreenCompiles(contract);
        requireGeneratedScreenCoversContract(contract);
        writeJsonReportIfRequested(contract);
        System.out.println("agent5 native ui bridge contract PASS screens="
                + list(contract, "screenIds").size()
                + " features="
                + list(contract, "features").size()
                + " generatedScreen=compiled+executed");
    }

    private static void writeJsonReportIfRequested(Map<String, Object> contract) throws IOException {
        String configured = System.getProperty("echo.native.agent5.uiBridgeContractReport");
        if (configured == null || configured.isBlank()) {
            return;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.native.agent5.ui_bridge_contract_smoke.v1");
        report.put("generatedAt", "1970-01-01T00:00:00Z");
        report.put("status", "PASS");
        report.put("runtime", "echo_native");
        report.put("moduleIds", List.of(
                "echoterminal",
                "echoindex",
                "echolens",
                "echohudcore",
                "echoscreencore",
                "echoholomap",
                "echowiki",
                "echoashfallprotocol",
                "echonotificationcore",
                "echothemecore"
        ));
        report.put("featureBuckets", List.of(
                "gui",
                "hud",
                "screen",
                "inventory_overlay",
                "terminal",
                "index",
                "holomap",
                "lens",
                "audio"
        ));
        report.put("trustedMutations", List.of(
                "generated ScreenCore screen compiled and executed",
                "live route handlers executed for Terminal, Index, Lens, HUD, HoloMap, Wiki, and death recovery",
                "input focus, hotkey, notification, and main-menu override acceptance smokes executed"
        ));
        report.put("visibleRoutes", list(contract, "screenIds"));
        report.put("saveEvidence", List.of("UI theme/profile and screen-stack state routes are contract verified"));
        report.put("networkEvidence", List.of("client route ownership and handler dispatch contract verified"));
        report.put("blockers", List.of());
        NativeLoaderJsonSupport.writeAtomically(Path.of(configured), report);
    }

    private static void requireListContains(Map<String, Object> contract, String key, String value) {
        require(list(contract, key).contains(value), key + " missing " + value);
    }

    private static void requireNativeUiRuntimeActionsAreHostDerived() {
        String bootstrapSource = readBootstrapSource();
        String orchestratorSource = readSourceFile("EchoNativeBootstrapOrchestrator.java");
        String uiClientFlowSource = readSourceFile("EchoNativeBootstrapUiClientFlow.java");
        String runtimeHostFlowSource = readSourceFile("NativeLoaderRuntimeHostFlow.java");
        String runtimeHostSupportSource = readSourceFile("NativeLoaderRuntimeHostSupport.java");
        String runtimeActionSource = readSourceFile("NativeLoaderClientUiRuntimeActions.java");
        String playableActionSource = readSourceFile("NativeLoaderClientUiPlayableActions.java");
        String adapterCoreRuntimeMutationsSource = readSourceFile("NativeLoaderAdapterCoreRuntimeMutations.java");
        String scannerRuntimeSource = readSourceFile("NativeLoaderAdapterCoreScannerRuntimeActions.java");
        String source = String.join("\n",
                bootstrapSource,
                orchestratorSource,
                uiClientFlowSource,
                runtimeHostFlowSource,
                runtimeHostSupportSource,
                runtimeActionSource,
                playableActionSource,
                adapterCoreRuntimeMutationsSource,
                scannerRuntimeSource);
        String liveBridgeSource = readSourceFile("EchoNativeLiveUiBridge.java");
        String generatedUiSource = readSourceFile("NativeLoaderGeneratedUiSources.java");
        String liveUiSource = liveBridgeSource + "\n" + generatedUiSource;
        String routerSource = readSourceFile("EchoNativeAgent5UiActionRouter.java")
                + "\n" + readSourceFile("NativeLoaderUiActionRouter.java");
        String rendererSource = readSourceFile("EchoNativeAgent5ModuleSurfaceRenderers.java")
                + "\n" + readSourceFile("NativeLoaderModuleSurfaceRenderers.java");
        String routeEffectSource = readSourceFile("EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance.java");
        require(!source.contains("NATIVE_CLIENT_RUNTIME_ACTIONS"),
                "native UI supported actions must not come from a fixed runtime action list");
        require(runtimeActionSource.contains("context.runtimeSurfaceSupported().test(actionRuntimeHost, \"events\")"),
                "native UI scanner action support must be derived from the active host events surface");
        require(runtimeActionSource.contains("actions.add(context.scannerUsedAction())")
                        && runtimeActionSource.contains("actions.add(context.useScannerAction())"),
                "native UI scanner actions must only be advertised inside the events surface gate");
        require(runtimeActionSource.contains("context.runtimeSurfaceSupported().test(grantRuntimeHost, \"playerInventory\")"),
                "native UI grant action support must be derived from the active grant host player inventory surface");
        require(runtimeActionSource.contains("actions.add(context.grantItemAction())"),
                "native UI grant action must only be advertised inside the player inventory surface gate");
        require(runtimeActionSource.contains("context.runtimeSurfaceSupported().test(runtimeHost, \"events\")")
                        && runtimeActionSource.contains("addEventAction(context, actions, actionRuntimeHost, context.terminalCommandAction())"),
                "native UI terminal command support must be derived from the active host events surface");
        require(runtimeActionSource.contains("context.runtimeSurfaceSupported().test(runtimeHost, \"events\")")
                        && runtimeActionSource.contains("addEventAction(context, actions, actionRuntimeHost, context.indexSearchAction())"),
                "native UI index search support must be derived from the active host events surface");
        require(runtimeActionSource.contains("addEventAction(context, actions, actionRuntimeHost, context.hudRefreshAction())")
                        && runtimeActionSource.contains("addEventAction(context, actions, actionRuntimeHost, context.missionLogUpdateAction())"),
                "native UI HUD and mission actions must be derived from active host event support");
        require(source.contains("\"portableScannerUsed\""),
                "native UI scanner action must call the same portable scanner runtime entrypoint as the Minecraft item");
        require(!source.contains(".getMethod(\"scannerUsed\", serverPlayerClass, scanHitClass, String.class, boolean.class)"),
                "native UI scanner action must not bypass portable scanner use with the low-level scanner event helper");
        require(playableActionSource.contains("scannerUsePayload(context, \"native_ui_scanner\", false)")
                        && playableActionSource.contains("payload.put(\"deepScan\", deepScan)")
                        && scannerRuntimeSource.contains("portableScannerUsed"),
                "native UI scanner action must preserve scanner payload context and portable scanner runtime entrypoint");
        require(source.contains("public static boolean grantNativeItemFromUi(String itemId, int count)"),
                "native UI grant action must expose a bootstrap entry point");
        require(uiClientFlowSource.contains("return NativeLoaderClientUiPlayableActions.mutationAccepted(useScannerEvidence())")
                        && uiClientFlowSource.contains("return NativeLoaderClientUiPlayableActions.mutationAccepted(grantItemEvidence(itemId, count))")
                        && playableActionSource.contains("public static boolean mutationAccepted(Map<String, Object> evidence)")
                        && source.contains("&& Boolean.TRUE.equals(result.get(\"saveTouched\"))")
                        && source.contains("&& Boolean.TRUE.equals(result.get(\"missionUpdated\"))")
                        && source.contains("&& Boolean.TRUE.equals(result.get(\"feedbackEmitted\"))"),
                "native UI public action success must require mutation, save, mission, and feedback evidence");
        require(source.contains("public static Map<String, Object> grantNativeItemFromUiEvidence(String itemId, int count)")
                        && playableActionSource.contains("context.grantItemResult().grant("),
                "native UI grant action must expose AdapterCore mutation evidence from the active runtime host");
        require(source.contains("public static Map<String, Object> useNativeScannerFromUiEvidence()")
                        && playableActionSource.contains("context.publishEvent().publish(")
                        && playableActionSource.contains("scannerUsePayload(context, \"native_ui_scanner\", false)"),
                "native UI scanner action must expose AdapterCore mutation evidence from the canonical scanner event");
        require(source.contains("NATIVE_RUNTIME_HOST_ID_PROPERTY")
                        && source.contains("ECHO_NATIVE_RUNTIME_HOST_ID")
                        && source.contains("NativeLoaderRuntimeHostSupport")
                        && source.contains("selectedRegisteredRuntimeHost("),
                "native UI grant action must be able to resolve a selected registered runtime host");
        require(source.contains("selectedRegistryValue(runtimeHostId, \"capabilities\")")
                        && source.contains(".getMethod(\"supportsAction\", String.class)"),
                "native UI supported actions must honor selected runtime host declared capabilities");
        require(source.contains("context.resolveRuntimeItemId().apply(selectedRuntimeHost, itemId)")
                        && source.contains(".getMethod(\"supportsCanonicalContent\", String.class)"),
                "selected runtime host grants must resolve item ids from AdapterCore canonical content capabilities");
        require(source.contains("!context.runtimeActionSupported().test(selectedRuntimeHost, context.scannerUsedAction())")
                        && source.contains("!context.runtimeActionSupported().test(selectedRuntimeHost, context.grantItemAction())"),
                "native UI scanner and grant entrypoints must reject unsupported selected-host actions");
        require(source.contains("context.putSelectedRuntimeHostEvidence().accept(evidence, null)")
                        && source.contains("context.putSelectedRuntimeHostEvidence().accept(evidence, selectedRuntimeHost)")
                        && source.contains("evidence.put(\"selectedRuntimeHostConfigured\", configured)")
                        && source.contains("evidence.put(\"selectedRuntimeHostId\", configured ? selectedRuntimeHostId() : \"\")")
                        && source.contains("evidence.put(\"selectedRuntimeHostResolved\", configured && runtimeHost != null)")
                        && source.contains("failureKind\", \"missing_selected_runtime_host\""),
                "native UI scanner and grant evidence must expose missing selected-host rejections");
        require(source.contains("context.selectedRuntimeHostConfigured().get()")
                        && source.contains("context.publishEvent().publish(")
                        && source.contains("context.scannerUsedAction()")
                        && source.contains("context.grantItemResult().grant(selectedRuntimeHost, resolvedItemId, Math.max(1, count))"),
                "native UI scanner and grant actions must prefer the selected runtime host before falling back to Minecraft");
        require(source.contains("context.putNativeRuntimeHostEvidence().accept(evidence, runtimeHost)")
                        && source.contains("evidence.put(\"runtimeHostClass\", runtimeHost.getClass().getName())")
                        && source.contains("evidence.put(\"runtimePlayerId\", runtimePlayerId(runtimeHost))"),
                "native UI grant evidence must identify the runtime host that received the mutation call");
        require(source.contains("Object grantRuntimeHost = context.grantRuntimeHost().apply(runtimeHost);")
                        && source.contains("context.runtimeSurfaceSupported().test(grantRuntimeHost, \"playerInventory\")"),
                "native UI grant support must be advertised from the selected grant runtime host");
        require(source.contains("native_client.grant_item.")
                        && source.contains("EchoNativeRuntimeHost.PlayerInventory")
                        && source.contains("context.grantItemResult().grant("),
                "native UI grant action must call the active runtime host player inventory grant mutation");
        require(source.contains("private static Object runtimePlayerRef(Context context, Object runtimeHost, Class<?> playerRefClass)")
                        && source.contains("optionalMethodValue(runtimeHost, \"playerRef\")")
                        && source.contains(".getConstructor(String.class).newInstance(runtimePlayerId(runtimeHost))"),
                "native UI grant action must build AdapterCore player refs for selected non-NeoForge hosts");
        require(source.contains("private static Object runtimeMutationContext(")
                        && source.contains(".getConstructor(String.class, String.class, String.class, String.class, long.class, Map.class)")
                        && source.contains("metadata.put(\"hostRuntime\""),
                "native UI grant action must build public AdapterCore mutation contexts for selected non-NeoForge hosts");
        require(liveUiSource.contains("EchoNativeBootstrapMain.grantNativeItemFromUiEvidence("),
                "native live UI recovery action must mutate inventory through the bootstrap grant evidence path");
        require(liveUiSource.contains("recovery item grant unavailable for active runtime host"),
                "native live UI recovery action must surface failed inventory grants");
        require(bootstrapSource.contains("public static Map<String, Object> executeNativeTerminalCommandFromUi(")
                        && runtimeActionSource.contains("context.commandExecutionEvent()")
                        && runtimeActionSource.contains("return publishEvent(")
                        && source.contains("EchoNativeRuntimeHost.Events"),
                "native UI terminal command must publish the canonical command event through AdapterCore events");
        require(source.contains("Map<String, Object> details = object(snapshot.get(\"details\"))")
                        && source.contains("evidence.put(\"hostSaveTouched\", hostSaveTouched)")
                        && source.contains("boolean saveTouched = hostSaveTouched")
                        && !source.contains("evidence.put(\"saveTouched\", mutated ||")
                        && source.contains("evidence.put(\"missionUpdated\", missionUpdated)")
                        && source.contains("details.containsKey(\"mission\")"),
                "native UI mutation evidence must surface explicit AdapterCore host-save and mission details");
        require(runtimeActionSource.contains("!context.runtimeActionSupported().test(runtimeHost, context.terminalCommandAction())"),
                "native UI terminal command entrypoint must reject unsupported active-host actions");
        require(liveUiSource.contains("EchoNativeBootstrapMain.executeNativeTerminalCommandFromUi(")
                        && liveUiSource.contains("this.terminalCommandExecuted = runtimeMutationAccepted(terminalMutation)")
                        && liveUiSource.contains("terminal command unavailable for active runtime host"),
                "native live UI terminal command must have accepted runtime mutation evidence before marking execution");
        require(liveUiSource.contains("this.terminalOutput = runtimeFeedback(terminalMutation, this.terminalOutput)")
                        && liveUiSource.contains("String.valueOf(state.get(\"terminalOutput\")).contains(\"save=true\")")
                        && liveUiSource.contains("String.valueOf(state.get(\"terminalOutput\")).contains(\"mission=true\")"),
                "native live UI terminal feedback must be rendered from the runtime mutation result");
        require(bootstrapSource.contains("public static Map<String, Object> executeNativeIndexSearchFromUi(")
                        && runtimeActionSource.contains("context.terminalOpenedEvent()")
                        && runtimeActionSource.contains("return publishEvent(")
                        && source.contains("EchoNativeRuntimeHost.Events"),
                "native UI index search must publish the canonical terminal-opened AdapterCore event");
        require(runtimeActionSource.contains("!context.runtimeActionSupported().test(runtimeHost, context.indexSearchAction())"),
                "native UI index search entrypoint must reject unsupported active-host actions");
        require(liveUiSource.contains("EchoNativeBootstrapMain.executeNativeIndexSearchFromUi(")
                        && liveUiSource.contains("this.indexSearchExecuted = runtimeMutationAccepted(indexMutation)")
                        && liveUiSource.contains("index search unavailable for active runtime host"),
                "native live UI index search must have accepted runtime mutation evidence before marking execution");
        require(liveUiSource.contains("this.indexOutput = runtimeFeedback(indexMutation, this.indexOutput)")
                        && liveUiSource.contains("String.valueOf(state.get(\"indexOutput\")).contains(\"save=true\")")
                        && liveUiSource.contains("String.valueOf(state.get(\"indexOutput\")).contains(\"mission=true\")"),
                "native live UI index feedback must be rendered from the runtime mutation result");
        require(bootstrapSource.contains("public static Map<String, Object> executeNativeHudRefreshFromUi(")
                        && runtimeActionSource.contains("context.clientTickEvent()")
                        && runtimeActionSource.contains("context.hudRefreshAction()"),
                "native UI HUD refresh must publish a canonical runtime tick event");
        require(bootstrapSource.contains("public static Map<String, Object> executeNativeMissionLogUpdateFromUi(")
                        && runtimeActionSource.contains("context.missionObjectiveCompletedEvent()")
                        && runtimeActionSource.contains("context.missionLogUpdateAction()"),
                "native UI mission log update must publish a canonical mission event");
        require(runtimeActionSource.contains("!context.runtimeActionSupported().test(runtimeHost, runtimeActionId)"),
                "native UI event entrypoints must reject unsupported active-host actions");
        require(liveUiSource.contains("EchoNativeBootstrapMain.executeNativeHudRefreshFromUi(")
                        && liveUiSource.contains("if (!runtimeMutationAccepted(hudMutation))")
                        && liveUiSource.contains("HUD refresh unavailable for active runtime host"),
                "native live UI HUD refresh must have accepted runtime mutation evidence before updating UI state");
        require(liveUiSource.contains("EchoNativeBootstrapMain.executeNativeMissionLogUpdateFromUi(")
                        && liveUiSource.contains("if (!runtimeMutationAccepted(missionMutation))")
                        && liveUiSource.contains("mission update unavailable for active runtime host"),
                "native live UI mission log update must have accepted runtime mutation evidence before updating UI state");
        require(liveUiSource.contains("this.hudUpdateOutput = runtimeFeedback(hudMutation")
                        && liveUiSource.contains("this.missionUpdateLine = runtimeFeedback(missionMutation")
                        && liveUiSource.contains("private static String runtimeFeedback(")
                        && liveUiSource.contains("boolean missionUpdated = Boolean.TRUE.equals(mutation.get(\"missionUpdated\"))")
                        && liveUiSource.contains("\"; mission=\" + missionUpdated"),
                "native HUD and mission visible feedback must include AdapterCore mutation result evidence");
        require(runtimeActionSource.contains("context.surfaceOpenAction()")
                        && runtimeActionSource.contains("context.indexBookmarkAction()")
                        && runtimeActionSource.contains("context.holoMapStateAction()")
                        && runtimeActionSource.contains("context.signalOsTerminalAction()")
                        && runtimeActionSource.contains("context.productCommandAction()")
                        && bootstrapSource.contains("public static Map<String, Object> executeNativeUiRuntimeEventFromUi(")
                        && source.contains("EchoNativeRuntimeHost.Events")
                        && runtimeActionSource.contains("executeRuntimeEvent(Context context, String runtimeActionId"),
                "native hotkey-only actions must expose AdapterCore event mutation action ids");
        require(runtimeActionSource.contains("context.runtimeSurfaceSupported().test(runtimeHost, \"events\")")
                        && runtimeActionSource.contains("addEventAction(context, actions, actionRuntimeHost, context.surfaceOpenAction())")
                        && runtimeActionSource.contains("addEventAction(context, actions, actionRuntimeHost, context.indexBookmarkAction())")
                        && runtimeActionSource.contains("addEventAction(context, actions, actionRuntimeHost, context.holoMapStateAction())")
                        && runtimeActionSource.contains("addEventAction(context, actions, actionRuntimeHost, context.signalOsTerminalAction())")
                        && runtimeActionSource.contains("addEventAction(context, actions, actionRuntimeHost, context.productCommandAction())"),
                "native hotkey-only actions must be advertised only from active hosts with events support");
        require(runtimeActionSource.contains("!context.runtimeActionSupported().test(runtimeHost, runtimeActionId)")
                        && source.contains("failureKind\", \"unsupported_runtime_action\""),
                "native hotkey-only event mutations must honor selected-host action support");
        require(liveBridgeSource.contains("nativeUiHotkeySupportsAny(route, \"native.ui.surface_open\")")
                        && liveBridgeSource.contains("nativeUiHotkeySupportsAny(route, \"native.ui.index_bookmark\")")
                        && liveBridgeSource.contains("nativeUiHotkeySupportsAny(route, \"native.ui.holomap_state\")")
                        && liveBridgeSource.contains("nativeUiHotkeySupportsAny(route, \"native.ui.signalos_terminal\")")
                        && liveBridgeSource.contains("EchoNativeBootstrapMain.executeNativeUiRuntimeEventFromUi(")
                        && liveBridgeSource.contains("route.put(\"routeType\", mutated ? \"adaptercore_event\"")
                        && !liveBridgeSource.contains("adaptercore_save_data"),
                "native live hotkey routes must hide unsupported active-host actions");
        int signalOsCloseMutationIndex = liveBridgeSource.indexOf(
                "Map<String, Object> mutation = writeNativeSignalOsTerminalAction(key, surface, \"close\", false)");
        int signalOsCloseIndex = liveBridgeSource.indexOf(
                "boolean closed = mutated && closeCurrentScreen(minecraftClass, minecraft)");
        int signalOsOpenMutationIndex = liveBridgeSource.indexOf(
                "Map<String, Object> mutation = writeNativeSignalOsTerminalAction(key, surface, \"open\", false)");
        int signalOsPacketIndex = liveBridgeSource.indexOf(
                "boolean sent = mutated && sendSignalOsOpenTerminalCommand()");
        int dataSurfaceMutationIndex = liveBridgeSource.indexOf(
                "? EchoNativeBootstrapMain.executeNativeSurfaceOpenFromUi(destination, effect)");
        int dataSurfaceGateIndex = dataSurfaceMutationIndex < 0
                ? -1
                : liveBridgeSource.indexOf(
                "if (mutated && openRealDeclaredModuleSurface(destination, minecraftClass, minecraft, route))",
                dataSurfaceMutationIndex);
        require(signalOsCloseMutationIndex >= 0
                        && signalOsCloseIndex > signalOsCloseMutationIndex
                        && signalOsOpenMutationIndex >= 0
                        && signalOsPacketIndex > signalOsOpenMutationIndex
                        && dataSurfaceMutationIndex >= 0
                        && dataSurfaceGateIndex > dataSurfaceMutationIndex
                        && liveBridgeSource.contains("boolean mutated = applyNativeUiMutationEvidence(route, mutation)")
                        && liveBridgeSource.contains("if (!mutated)"),
                "native packet and screen effects must be gated by AdapterCore runtime mutation evidence");
        require(liveUiSource.contains("EchoNativeBootstrapMain.useNativeScannerFromUiEvidence()")
                        && liveUiSource.contains("this.lensScanExecuted = runtimeMutationAccepted(scannerMutation)")
                        && liveUiSource.contains("this.recoveryActionExecuted = runtimeMutationAccepted(recoveryMutation)")
                        && liveBridgeSource.contains("route.put(\"eventName\", \"player.scanner_used\")"),
                "native lens and recovery UI routes must use canonical events and accepted runtime mutation evidence");
        require(liveBridgeSource.contains("applyNativeMutationEvidence(route, mutation)")
                        && liveBridgeSource.contains("boolean fullGameplayEvidence = mutated && saveTouched && feedbackEmitted && missionUpdated")
                        && liveBridgeSource.contains("boolean accepted = uiEventMutation ? mutated : fullGameplayEvidence")
                        && liveBridgeSource.contains("route.put(\"runtimeMutationAccepted\", accepted)")
                        && liveBridgeSource.contains("route.put(\"runtimeMutationFullGameplayEvidence\", fullGameplayEvidence)")
                        && liveBridgeSource.contains("return accepted")
                        && !liveBridgeSource.contains("Boolean.TRUE.equals(scannerMutation.get(\"mutated\"))")
                        && liveBridgeSource.contains("route.get(\"runtimeHostMutated\")")
                        && liveBridgeSource.contains("&& saveTouched")
                        && liveBridgeSource.contains("&& feedbackEmitted")
                        && liveBridgeSource.contains("&& missionUpdated"),
                "native live hotkey routes must not be accepted without AdapterCore runtime mutation, save, feedback, and mission evidence");
        require(routeEffectSource.contains("boolean runtimeMutation = Boolean.TRUE.equals(map.get(\"runtimeHostMutated\"))")
                        && routeEffectSource.contains("boolean durableFeedback = Boolean.TRUE.equals(map.get(\"saveTouched\"))")
                        && routeEffectSource.contains("&& Boolean.TRUE.equals(map.get(\"feedbackEmitted\"))")
                        && routeEffectSource.contains("boolean missionUpdated = Boolean.TRUE.equals(map.get(\"missionUpdated\"))")
                        && routeEffectSource.contains("&& durableFeedback")
                        && routeEffectSource.contains("&& missionUpdated")
                        && !routeEffectSource.contains("|| Boolean.TRUE.equals(map.get(\"adapterCoreMutation\"))")
                        && routeEffectSource.contains("rejectedAdapterCoreOnly")
                        && routeEffectSource.contains("rejectedNoSave")
                        && routeEffectSource.contains("rejectedNoFeedback")
                        && routeEffectSource.contains("rejectedNoMission"),
                "native physical route transcript acceptance must reject local-only, save-missing, feedback-missing, and mission-missing route effects");
        require(routerSource.contains("action.put(\"runtimeActionId\", \"native.ui.terminal_command\")")
                        && routerSource.contains("action.put(\"runtimeEventName\", \"command_execution\")"),
                "native terminal action router must provide the canonical runtime command payload");
        require(routerSource.contains("action.put(\"runtimeActionId\", \"native.ui.index_search\")")
                        && routerSource.contains("action.put(\"runtimeEventName\", \"player.terminal_opened\")"),
                "native index action router must provide the canonical runtime event payload");
        require(rendererSource.contains("terminal command unavailable for active runtime host"),
                "native terminal surface must hide command input unsupported by the active host");
        require(rendererSource.contains("Index search unavailable for active runtime host"),
                "native index surface must hide search actions unsupported by the active host");
        require(rendererSource.contains("HUD refresh unavailable for active runtime host")
                        && rendererSource.contains("mission update unavailable for active runtime host"),
                "native HUD and mission surfaces must hide unsupported runtime actions");
        require(routerSource.contains("action.put(\"runtimeActionId\", \"player.inventory.grant\")")
                        && routerSource.contains("EchoNativeBootstrapMain::nativeRecoveryItemId")
                        && routerSource.contains("String recoveryItemId = context.recoveryItemId().get()")
                        && routerSource.contains("action.put(\"grantItemId\", recoveryItemId)")
                        && routerSource.contains("action.put(\"grantItemCount\", 1)"),
                "native recovery action router must provide the canonical inventory grant payload");
        require(liveUiSource.contains("this.lensOutput = runtimeFeedback(scannerMutation, this.lensOutput)")
                        && liveUiSource.contains("this.recoveryOutput = runtimeFeedback(recoveryMutation, this.recoveryOutput)")
                        && liveUiSource.contains("String.valueOf(state.get(\"lensOutput\")).contains(\"save=true\")")
                        && liveUiSource.contains("String.valueOf(state.get(\"recoveryOutput\")).contains(\"save=true\")"),
                "native scanner and recovery visible feedback must include AdapterCore mutation result evidence");
        require(rendererSource.contains("recovery grant unavailable for active runtime host"),
                "native recovery surface must hide grant actions unsupported by the active host");
    }

    private static void requireNativeEchoModuleReportsDoNotCountAsPlayableActions() {
        String source = readBootstrapSource() + "\n"
                + readSourceFile("EchoNativeBootstrapOrchestrator.java") + "\n"
                + readSourceFile("EchoNativeBootstrapProductActionFlow.java");
        require(source.contains("return PRODUCT_ACTION_FLOW.moduleRuntimeMutationAccepted(report);")
                        && source.contains("boolean moduleRuntimeMutationAccepted(Map<String, Object> report)")
                        && source.contains("Boolean.TRUE.equals(result.get(\"adapterCoreActionExecuted\"))")
                        && source.contains("Boolean.TRUE.equals(result.get(\"runtimeHostMutated\"))")
                        && source.contains("Boolean.TRUE.equals(result.get(\"stateMutation\"))")
                        && source.contains("Boolean.TRUE.equals(result.get(\"saveTouched\"))")
                        && source.contains("Boolean.TRUE.equals(result.get(\"missionUpdated\"))")
                        && source.contains("Boolean.TRUE.equals(result.get(\"feedbackEmitted\"))")
                        && !source.contains("return Boolean.TRUE.equals(report.get(\"stateMutation\"))")
                        && !source.contains("|| Boolean.TRUE.equals(report.get(\"screenOpened\"))"),
                "native item/block module actions must not count activation reports or screen opens as playable success");
        Map<String, Object> activationOnly = new LinkedHashMap<>();
        activationOnly.put("nativeActivationBound", true);
        activationOnly.put("runtimeBound", true);
        activationOnly.put("successfulCallCount", 6);
        activationOnly.put("screenOpened", true);
        require(!EchoNativeBootstrapMain.nativeEchoModuleRuntimeMutationAccepted(activationOnly),
                "activation reports, runtime binding, call counts, and screen opens must not be playable actions");
        Map<String, Object> localMutationOnly = new LinkedHashMap<>(activationOnly);
        localMutationOnly.put("stateMutation", true);
        localMutationOnly.put("saveTouched", true);
        localMutationOnly.put("missionUpdated", true);
        localMutationOnly.put("feedbackEmitted", true);
        require(!EchoNativeBootstrapMain.nativeEchoModuleRuntimeMutationAccepted(localMutationOnly),
                "native module action success must require an AdapterCore action and runtime host mutation");
        Map<String, Object> adapterCoreWithoutHost = new LinkedHashMap<>(localMutationOnly);
        adapterCoreWithoutHost.put("adapterCoreActionExecuted", true);
        require(!EchoNativeBootstrapMain.nativeEchoModuleRuntimeMutationAccepted(adapterCoreWithoutHost),
                "native module action success must require the active runtime host to mutate");
        Map<String, Object> accepted = new LinkedHashMap<>(adapterCoreWithoutHost);
        accepted.put("runtimeHostMutated", true);
        require(EchoNativeBootstrapMain.nativeEchoModuleRuntimeMutationAccepted(accepted),
                "native module action success must accept the full AdapterCore host mutation/save/mission/feedback chain");
    }

    private static void requireNativeAshfallPowerNodeRequiresCompletedAdapterCoreChain() {
        String source = readSourceFile("NativeLoaderProductBlockActionExecutor.java").replace("\r\n", "\n");
        int start = source.indexOf("private static boolean powerNodeAction(");
        int end = start < 0 ? -1 : source.indexOf("private static boolean bunkAction(", start);
        require(start >= 0 && end > start,
                "native Ashfall power node action must be present for block-use caller-path verification");
        String body = source.substring(start, end).replace("\r\n", "\n");
        require(body.contains("boolean used = ops.machineUseBlock(level, player, pos, machineId)")
                        && body.contains("if (!used) {\n            return false;")
                        && body.contains("if (fuel.isBlank()) {\n            return false;")
                        && body.contains("if (!ops.machineReceiveEnergy(level, player, pos, machineId, ops.energyItemCharge(fuel))) {\n            return false;")
                        && body.contains("if (!ops.removeItem(level, player, fuel, 1)) {\n            return false;")
                        && body.contains("boolean ticked = ops.machineTick(level, player, pos, machineId)")
                        && body.contains("boolean powered = ops.powerNodeState(level, player, pos, true, 1, \"native_client_block_use\")")
                        && body.contains("return ticked && powered;")
                        && !body.contains("\n        ops.machineUseBlock(level, player, pos, machineId);\n")
                        && !body.contains("return true;"),
                "native Ashfall power node block action must require use, fuel transfer, item removal, tick, and state mutation");
    }

    private static void requireNativeAshfallGridRequiresCompletedAdapterCoreChain() {
        String source = readSourceFile("NativeLoaderProductBlockActionExecutor.java").replace("\r\n", "\n");
        int start = source.indexOf("private static boolean gridAction(");
        int end = start < 0 ? -1 : source.indexOf("private static boolean processorAction(", start);
        require(start >= 0 && end > start,
                "native Ashfall grid action must be present for block-use caller-path verification");
        String body = source.substring(start, end).replace("\r\n", "\n");
        require(body.contains("boolean used = ops.machineUseBlock(level, player, pos, machineId)")
                        && body.contains("if (!used) {\n            return false;")
                        && body.contains("if (!ops.machineReceiveEnergy(level, player, pos, machineId, amount)) {\n                return false;")
                        && body.contains("if (!ops.removeItem(level, player, fuel, 1)) {\n                return false;")
                        && body.contains("return ops.machineTick(level, player, pos, machineId);")
                        && body.contains("if (!ops.machineExtractEnergy(level, player, pos, machineId, rules.gridExtractEnergy())) {\n            return false;")
                        && body.contains("if (!ops.giveItem(player, ops.configuredId(rules.gridExtractOutputItemId()), 1)) {\n            return false;")
                        && !body.contains("\n        ops.machineUseBlock(level, player, pos, machineId);\n")
                        && !body.contains("return true;"),
                "native Ashfall grid block action must require use, energy transfer/extraction, inventory update, and tick mutation");
    }

    private static void requireNativeAshfallGeneratorAndProcessorRequireCompletedTick() {
        String source = readSourceFile("NativeLoaderProductBlockActionExecutor.java").replace("\r\n", "\n");
        int generatorStart = source.indexOf("private static boolean generatorAction(");
        int generatorEnd = generatorStart < 0 ? -1 : source.indexOf("private static boolean gridAction(", generatorStart);
        require(generatorStart >= 0 && generatorEnd > generatorStart,
                "native Ashfall generator action must be present for block-use caller-path verification");
        String generatorBody = source.substring(generatorStart, generatorEnd).replace("\r\n", "\n");
        require(generatorBody.contains("boolean fuelAvailable = false;")
                        && generatorBody.contains("fuelAvailable = true;")
                        && generatorBody.contains("if (fuelAvailable && insertedFuel.isBlank()) {\n            return false;")
                        && generatorBody.contains("if (!insertedFuel.isBlank() && !ops.removeItem(level, player, insertedFuel, 1)) {\n            return false;")
                        && generatorBody.contains("return ops.machineTick(level, player, pos, machineId);")
                        && !generatorBody.contains("\n        ops.machineTick(level, player, pos, machineId);\n")
                        && !generatorBody.contains("return true;"),
                "native Ashfall generator block action must require fuel insertion truth and completed machine tick");

        int processorStart = source.indexOf("private static boolean processorAction(");
        int processorEnd = processorStart < 0 ? -1 : source.indexOf("private static boolean tickRepeated(", processorStart);
        require(processorStart >= 0 && processorEnd > processorStart,
                "native Ashfall processor action must be present for block-use caller-path verification");
        String processorBody = source.substring(processorStart, processorEnd).replace("\r\n", "\n");
        require(processorBody.contains("if (!ops.machineUseBlock(level, player, pos, machineId)) {\n            return false;")
                        && processorBody.contains("return ops.machineTick(level, player, pos, machineId);")
                        && !processorBody.contains("\n        ops.machineTick(level, player, pos, machineId);\n")
                        && !processorBody.contains("return true;"),
                "native Ashfall processor block action must require completed machine tick after use-block mutation");
    }

    private static void requireNativeAshfallWaterHazardAndResearchRequireCompletedAdapterCoreChains() {
        String source = readSourceFile("NativeLoaderProductBlockActionExecutor.java").replace("\r\n", "\n");
        require(source.contains("private static boolean tickRepeated(")
                        && source.contains("if (!ops.machineTick(level, player, pos, machineId)) {\n                return false;"),
                "native Ashfall multi-tick machine actions must fail when any AdapterCore tick is not accepted");

        int waterStart = source.indexOf("private static boolean waterMachineAction(");
        int waterEnd = waterStart < 0 ? -1 : source.indexOf("private static boolean hazardMachineAction(", waterStart);
        require(waterStart >= 0 && waterEnd > waterStart,
                "native Ashfall water machine action must be present for caller-path verification");
        String waterBody = source.substring(waterStart, waterEnd).replace("\r\n", "\n");
        require(waterBody.contains("if (!ops.machineUseBlock(level, player, pos, machineId)) {\n                return false;")
                        && waterBody.contains("if (!ops.machineReceiveEnergy(level, player, pos, machineId, ops.energyItemCharge(charge))) {\n                return false;")
                        && waterBody.contains("if (dirtyWaterAvailable || !filter.isBlank())")
                        && waterBody.contains("if (!dirtyWaterAvailable || filter.isBlank()) {\n                return false;")
                        && waterBody.contains("if (!insertedDirtyWater || !insertedFilter) {\n                return false;")
                        && waterBody.contains("if (!tickRepeated(level, player, pos, machineId, rules.waterFilterTicks(), ops)) {\n                return false;")
                        && waterBody.contains("if (!ops.machineExtractItem(level, player, pos, machineId, cleanWaterId, 1)) {\n                return false;")
                        && waterBody.contains("if (!ops.giveItem(player, cleanWaterId, 1)) {\n                return false;")
                        && waterBody.contains("return ops.waterFiltered(level, player, \"native_client_water_purifier\");")
                        && !waterBody.contains("return true;"),
                "native Ashfall water machines must require use, optional energy removal, recipe insert/remove, ticks, extract, grant, and water-filtered mutation");

        int hazardStart = source.indexOf("private static boolean hazardMachineAction(");
        int hazardEnd = hazardStart < 0 ? -1 : source.indexOf("private static boolean researchLabAction(", hazardStart);
        require(hazardStart >= 0 && hazardEnd > hazardStart,
                "native Ashfall hazard machine action must be present for caller-path verification");
        String hazardBody = source.substring(hazardStart, hazardEnd).replace("\r\n", "\n");
        require(hazardBody.contains("if (filter.isBlank() && input.isBlank()) {\n                return ops.machineTick(level, player, pos, machineId);")
                        && hazardBody.contains("if (filter.isBlank() || input.isBlank()) {\n                return false;")
                        && hazardBody.contains("if (!insertedInput || !insertedFilter) {\n                return false;")
                        && hazardBody.contains("if (!ops.machineReceiveEnergy(level, player, pos, machineId, rules.radiationCleanserEnergy())) {\n                return false;")
                        && hazardBody.contains("if (!tickRepeated(level, player, pos, machineId, rules.radiationCleanserTicks(), ops)) {\n                return false;")
                        && hazardBody.contains("if (!ops.machineExtractItem(level, player, pos, machineId, output, 1)) {\n                return false;")
                        && hazardBody.contains("return ops.giveItem(player, output, 1);")
                        && hazardBody.contains("return tickRepeated(level, player, pos, machineId, rules.medicalMachineTicks(), ops);")
                        && hazardBody.contains("return tickRepeated(level, player, pos, machineId, rules.hazardMachineTicks(), ops);")
                        && !hazardBody.contains("nativeAdapterCoreRadiationCleanserUsed")
                        && !hazardBody.contains("nativeAdapterCoreMedBayUsed")
                        && !hazardBody.contains("nativeAdapterCoreAtmosphericScrubberUsed")
                        && !hazardBody.contains("return true;"),
                "native Ashfall hazard machines must only report success through canonical runtime-host tick/extract paths");

        int researchStart = source.indexOf("private static boolean researchLabAction(");
        int researchEnd = researchStart < 0 ? -1 : source.indexOf("private static boolean generatorAction(", researchStart);
        require(researchStart >= 0 && researchEnd > researchStart,
                "native Ashfall research lab action must be present for caller-path verification");
        String researchBody = source.substring(researchStart, researchEnd).replace("\r\n", "\n");
        require(researchBody.contains("if (!ops.machineUseBlock(level, player, pos, machineId)) {\n            return false;")
                        && researchBody.contains("return ops.researchLabAnalyze(level, player, \"native_client_block_use\");")
                        && researchBody.contains("return ops.machineTick(level, player, pos, machineId);")
                        && !researchBody.contains("nativeAdapterCoreLabObjective")
                        && !researchBody.contains("return true;"),
                "native Ashfall research lab block action must require analyze or a canonical runtime-host machine tick");
    }

    private static void requireNativeAshfallConsumablesRequireInventoryRemoval() {
        String source = readSourceFile("NativeLoaderProductItemActionExecutor.java");
        require(source.contains("RemoveItem removeConsumableItem")
                        && source.contains("boolean removeConsumableItem(Object level, Object player, String itemId, int count)")
                        && source.contains("return removeConsumableItem.run(level, player, itemId, count);"),
                "native Ashfall consumable item actions must share a creative-aware AdapterCore inventory removal gate");
        int start = source.indexOf("static boolean execute(");
        int end = start < 0 ? -1 : source.indexOf("record Operations(", start);
        require(start >= 0 && end > start,
                "native server item action must be present for consumable caller-path verification");
        String body = source.substring(start, end).replace("\r\n", "\n");
        require(countOccurrences(body, "return ops.removeConsumableItem(level, player, itemId, 1);") >= 3
                        && body.contains("if (!ops.removeConsumableItem(level, player, itemId, 1)) {\n            return false;")
                        && body.contains("return ops.isCreativePlayer(player) || ops.giveItem(player, \"minecraft:glass_bottle\", 1);")
                        && body.contains("if (!ops.healPlayer(player, amount)) {\n            return false;")
                        && body.contains("return ops.isCreativePlayer(player) || ops.damageOrShrinkItemStack(stack, player, handOrStack, 1);")
                        && body.contains("|| ops.damageOrShrinkItemStack(stack, player, handOrStack, deepScan ? 3 : 1);")
                        && body.contains("&& !ops.giveItem(player, ops.productId(\"schematic_fragment\"), 1)) {\n            return false;")
                        && body.contains("return ops.openModuleSurface(\"echoindex\", \"index\");")
                        && !body.contains("ops.removeConsumableItem(level, player, itemId, 1);\n        return true;")
                        && !body.contains("ops.openModuleSurface(\"echoindex\", \"index\");\n        return true;"),
                "native Ashfall consumable item actions must not ignore AdapterCore inventory removal before success");
    }

    private static void requireMissingSelectedRuntimeHostRejectsUiActions() {
        String previous = System.getProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
        try {
            System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, MISSING_SELECTED_RUNTIME_HOST_ID);
            Map<String, Object> scannerEvidence = EchoNativeBootstrapMain.useNativeScannerFromUiEvidence();
            requireMissingSelectedRuntimeHostEvidence(scannerEvidence, "scanner");
            Map<String, Object> grantEvidence = EchoNativeBootstrapMain.grantNativeItemFromUiEvidence(
                    "echoashfallprotocol:portable_signal_scanner",
                    1
            );
            requireMissingSelectedRuntimeHostEvidence(grantEvidence, "grant");
            List<String> supportedActions = EchoNativeBootstrapMain.nativeUiSupportedActionIds();
            require(!supportedActions.contains("player.scanner_used")
                            && !supportedActions.contains("native.ui.use_scanner")
                            && !supportedActions.contains("player.inventory.grant"),
                    "missing selected runtime host must not advertise Minecraft fallback UI actions");
        } finally {
            if (previous == null) {
                System.clearProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
            } else {
                System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, previous);
            }
        }
    }

    private static void requireSelectedRuntimeHostMutatesSupportedUiActions() {
        String previous = System.getProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
        boolean registered = false;
        SelectedRuntimeHostMutationProbe probe = new SelectedRuntimeHostMutationProbe();
        try {
            registerMutatingSelectedRuntimeHost(probe);
            registered = true;
            System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, MUTATING_SELECTED_RUNTIME_HOST_ID);
            List<String> supportedActions = EchoNativeBootstrapMain.nativeUiSupportedActionIds();
            require(supportedActions.contains("player.scanner_used")
                            && supportedActions.contains("native.ui.use_scanner")
                            && supportedActions.contains("player.inventory.grant"),
                    "mutating selected runtime host must advertise supported scanner and grant actions");
            require(supportedActions.contains("native.ui.terminal_command")
                            && supportedActions.contains("native.ui.index_search")
                            && supportedActions.contains("native.ui.hud_refresh")
                            && supportedActions.contains("native.ui.mission_log_update")
                            && supportedActions.contains("native.ui.surface_open")
                            && supportedActions.contains("native.ui.index_bookmark")
                            && supportedActions.contains("native.ui.holomap_state")
                            && supportedActions.contains("native.ui.signalos_terminal")
                            && supportedActions.contains("native.ui.ashfall_drone_command"),
                    "mutating selected runtime host must advertise supported UI event actions: " + supportedActions);
            Map<String, Object> scannerEvidence = EchoNativeBootstrapMain.useNativeScannerFromUiEvidence();
            requireAcceptedSelectedMutationEvidence(scannerEvidence, "scanner");
            require(probe.scannerEvents == 1,
                    "selected runtime host scanner evidence path must publish exactly one AdapterCore event");
            require("player.scanner_used".equals(probe.scannerEventId),
                    "selected runtime host scanner must publish the canonical player.scanner_used event");
            require("echoashfallprotocol:portable_signal_scanner".equals(probe.scannerCanonicalId),
                    "selected runtime host scanner must preserve the canonical scanner item id: "
                            + probe.scannerCanonicalId);
            require(EchoNativeBootstrapMain.useNativeScannerFromUi(),
                    "selected runtime host scanner public action must accept mutation/save/mission/feedback evidence");
            require(probe.scannerEvents == 2,
                    "selected runtime host scanner public action must call the selected events host");
            Map<String, Object> grantEvidence = EchoNativeBootstrapMain.grantNativeItemFromUiEvidence(
                    "echoashfallprotocol:portable_signal_scanner",
                    1
            );
            requireAcceptedSelectedMutationEvidence(grantEvidence, "grant");
            require(probe.grants == 1,
                    "selected runtime host grant evidence path must call inventory grant exactly once");
            require("echoashfallprotocol:portable_signal_scanner".equals(probe.grantItemId),
                    "selected runtime host grant must preserve the canonical scanner item id");
            require(probe.grantCount == 1,
                    "selected runtime host grant must preserve the requested item count");
            require(EchoNativeBootstrapMain.grantNativeItemFromUi("echoashfallprotocol:portable_signal_scanner", 1),
                    "selected runtime host grant public action must accept mutation/save/mission/feedback evidence");
            require(probe.grants == 2,
                    "selected runtime host grant public action must call the selected inventory host");
            Map<String, Object> terminalEvidence = EchoNativeBootstrapMain.executeNativeTerminalCommandFromUi(
                    "status",
                    "Ashfall Terminal"
            );
            requireAcceptedSelectedMutationEvidence(terminalEvidence, "terminal");
            require(probe.hasEvent("command_execution", "echoterminal:terminal"),
                    "selected runtime host terminal action must publish the canonical command_execution event");
            Map<String, Object> indexEvidence = EchoNativeBootstrapMain.executeNativeIndexSearchFromUi(
                    "ashfall",
                    "Ashfall Index"
            );
            requireAcceptedSelectedMutationEvidence(indexEvidence, "index");
            require(probe.hasEvent("player.terminal_opened", "echoindex:index"),
                    "selected runtime host index action must publish the canonical terminal-opened event");
            Map<String, Object> hudEvidence = EchoNativeBootstrapMain.executeNativeHudRefreshFromUi(
                    19,
                    "clear",
                    "Secure the crash outpost",
                    "boot"
            );
            requireAcceptedSelectedMutationEvidence(hudEvidence, "hud");
            require(probe.hasEvent("client_tick", "echoashfallprotocol:runtime_hud_notification"),
                    "selected runtime host HUD action must publish the canonical client_tick event");
            Map<String, Object> missionEvidence = EchoNativeBootstrapMain.executeNativeMissionLogUpdateFromUi(
                    "echoashfallprotocol:secure_crash_outpost",
                    "Secure Crash Outpost",
                    "Stabilize the camp",
                    1.0D,
                    "COMPLETE",
                    "Outpost secured"
            );
            requireAcceptedSelectedMutationEvidence(missionEvidence, "mission");
            require(probe.hasEvent("mission.objective_completed", "echoashfallprotocol:secure_crash_outpost"),
                    "selected runtime host mission action must publish the canonical mission event");
            Map<String, Object> surfaceEvidence = EchoNativeBootstrapMain.executeNativeSurfaceOpenFromUi(
                    "TERMINAL",
                    "native_data_surface.open:TERMINAL"
            );
            requireAcceptedSelectedMutationEvidence(surfaceEvidence, "surface");
            require(probe.hasEvent("native.ui.surface_open", "echoterminal:terminal"),
                    "selected runtime host surface-open action must publish the runtime surface event");
            Map<String, Object> indexBookmarkPayload = new LinkedHashMap<>();
            indexBookmarkPayload.put("screenId", "echoindex:index");
            indexBookmarkPayload.put("canonicalId", "echoindex:index/ashfall");
            indexBookmarkPayload.put("target", "echoindex:index/ashfall");
            Map<String, Object> indexBookmarkEvidence = EchoNativeBootstrapMain.executeNativeUiRuntimeEventFromUi(
                    "native.ui.index_bookmark",
                    indexBookmarkPayload
            );
            requireAcceptedSelectedMutationEvidence(indexBookmarkEvidence, "index bookmark");
            require(probe.hasEvent("native.ui.index_bookmark", "echoindex:index/ashfall"),
                    "selected runtime host index bookmark action must publish the runtime bookmark event");
            Map<String, Object> holomapPayload = new LinkedHashMap<>();
            holomapPayload.put("screenId", "echoholomap:map");
            holomapPayload.put("canonicalId", "echoholomap:ashfall_map");
            holomapPayload.put("target", "echoholomap:ashfall_map");
            Map<String, Object> holomapEvidence = EchoNativeBootstrapMain.executeNativeUiRuntimeEventFromUi(
                    "native.ui.holomap_state",
                    holomapPayload
            );
            requireAcceptedSelectedMutationEvidence(holomapEvidence, "holomap");
            require(probe.hasEvent("native.ui.holomap_state", "echoholomap:ashfall_map"),
                    "selected runtime host holomap state action must publish the runtime holomap event");
            Map<String, Object> signalOsPayload = new LinkedHashMap<>();
            signalOsPayload.put("screenId", "echosignalos:terminal");
            signalOsPayload.put("canonicalId", "echosignalos:terminal");
            signalOsPayload.put("target", "echosignalos:terminal");
            Map<String, Object> signalOsEvidence = EchoNativeBootstrapMain.executeNativeUiRuntimeEventFromUi(
                    "native.ui.signalos_terminal",
                    signalOsPayload
            );
            requireAcceptedSelectedMutationEvidence(signalOsEvidence, "signalos");
            require(probe.hasEvent("native.ui.signalos_terminal", "echosignalos:terminal"),
                    "selected runtime host SignalOS action must publish the runtime SignalOS event");
            Map<String, Object> dronePayload = new LinkedHashMap<>();
            dronePayload.put("screenId", "echoashfallprotocol:drone");
            dronePayload.put("canonicalId", "echoashfallprotocol:companion_drone");
            dronePayload.put("target", "echoashfallprotocol:companion_drone");
            Map<String, Object> droneEvidence = EchoNativeBootstrapMain.executeNativeUiRuntimeEventFromUi(
                    "native.ui.ashfall_drone_command",
                    dronePayload
            );
            requireAcceptedSelectedMutationEvidence(droneEvidence, "ashfall drone");
            require(probe.hasEvent("native.ui.ashfall_drone_command", "echoashfallprotocol:companion_drone"),
                    "selected runtime host Ashfall drone action must publish the runtime drone event");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("selected runtime host positive mutation smoke could not use AdapterCore: "
                    + exception.getMessage(), exception);
        } finally {
            if (registered) {
                unregisterSelectedRuntimeHost(MUTATING_SELECTED_RUNTIME_HOST_ID);
            }
            if (previous == null) {
                System.clearProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
            } else {
                System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, previous);
            }
        }
    }

    private static void requireSelectedRuntimeHostQueuedResultsRejectUiActions() {
        String previous = System.getProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
        boolean registered = false;
        SelectedRuntimeHostNonMutatingProbe probe = new SelectedRuntimeHostNonMutatingProbe("QUEUED");
        try {
            registerNonMutatingSelectedRuntimeHost(QUEUED_SELECTED_RUNTIME_HOST_ID, probe);
            registered = true;
            System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, QUEUED_SELECTED_RUNTIME_HOST_ID);
            List<String> supportedActions = EchoNativeBootstrapMain.nativeUiSupportedActionIds();
            require(supportedActions.contains("player.scanner_used")
                            && supportedActions.contains("native.ui.use_scanner")
                            && supportedActions.contains("player.inventory.grant")
                            && supportedActions.contains("native.ui.terminal_command"),
                    "queued selected runtime host must advertise actions before queued results are rejected");
            Map<String, Object> scannerEvidence = EchoNativeBootstrapMain.useNativeScannerFromUiEvidence();
            requireRejectedNonMutatingSelectedMutationEvidence(
                    scannerEvidence,
                    "scanner",
                    QUEUED_SELECTED_RUNTIME_HOST_ID,
                    "QUEUED"
            );
            require(probe.scannerEvents == 1,
                    "queued selected runtime host scanner evidence path must still call the selected events host");
            require(!EchoNativeBootstrapMain.useNativeScannerFromUi(),
                    "queued selected runtime host scanner public action must not report playable mutation success");
            require(probe.scannerEvents == 2,
                    "queued selected runtime host scanner public action must call the selected events host");
            Map<String, Object> grantEvidence = EchoNativeBootstrapMain.grantNativeItemFromUiEvidence(
                    "echoashfallprotocol:portable_signal_scanner",
                    1
            );
            requireRejectedNonMutatingSelectedMutationEvidence(
                    grantEvidence,
                    "grant",
                    QUEUED_SELECTED_RUNTIME_HOST_ID,
                    "QUEUED"
            );
            require(probe.grants == 1,
                    "queued selected runtime host grant evidence path must still call the selected inventory host");
            require(!EchoNativeBootstrapMain.grantNativeItemFromUi("echoashfallprotocol:portable_signal_scanner", 1),
                    "queued selected runtime host grant public action must not report playable mutation success");
            require(probe.grants == 2,
                    "queued selected runtime host grant public action must call the selected inventory host");
            Map<String, Object> terminalEvidence = EchoNativeBootstrapMain.executeNativeTerminalCommandFromUi(
                    "status",
                    "Queued Terminal"
            );
            requireRejectedNonMutatingSelectedMutationEvidence(
                    terminalEvidence,
                    "terminal",
                    QUEUED_SELECTED_RUNTIME_HOST_ID,
                    "QUEUED"
            );
            require(probe.hasEvent("command_execution", "echoterminal:terminal"),
                    "queued selected runtime host terminal action must still publish through the selected events host");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("selected runtime host queued-result rejection smoke could not use AdapterCore: "
                    + exception.getMessage(), exception);
        } finally {
            if (registered) {
                unregisterSelectedRuntimeHost(QUEUED_SELECTED_RUNTIME_HOST_ID);
            }
            if (previous == null) {
                System.clearProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
            } else {
                System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, previous);
            }
        }
    }

    private static void requireSelectedRuntimeHostNoopResultsRejectUiActions() {
        String previous = System.getProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
        boolean registered = false;
        SelectedRuntimeHostNonMutatingProbe probe = new SelectedRuntimeHostNonMutatingProbe("NOOP");
        try {
            registerNonMutatingSelectedRuntimeHost(NOOP_SELECTED_RUNTIME_HOST_ID, probe);
            registered = true;
            System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, NOOP_SELECTED_RUNTIME_HOST_ID);
            List<String> supportedActions = EchoNativeBootstrapMain.nativeUiSupportedActionIds();
            require(supportedActions.contains("player.scanner_used")
                            && supportedActions.contains("native.ui.use_scanner")
                            && supportedActions.contains("player.inventory.grant")
                            && supportedActions.contains("native.ui.terminal_command"),
                    "NOOP selected runtime host must advertise actions before NOOP results are rejected");
            Map<String, Object> scannerEvidence = EchoNativeBootstrapMain.useNativeScannerFromUiEvidence();
            requireRejectedNonMutatingSelectedMutationEvidence(
                    scannerEvidence,
                    "scanner",
                    NOOP_SELECTED_RUNTIME_HOST_ID,
                    "NOOP"
            );
            require(probe.scannerEvents == 1,
                    "NOOP selected runtime host scanner evidence path must still call the selected events host");
            require(!EchoNativeBootstrapMain.useNativeScannerFromUi(),
                    "NOOP selected runtime host scanner public action must not report playable mutation success");
            require(probe.scannerEvents == 2,
                    "NOOP selected runtime host scanner public action must call the selected events host");
            Map<String, Object> grantEvidence = EchoNativeBootstrapMain.grantNativeItemFromUiEvidence(
                    "echoashfallprotocol:portable_signal_scanner",
                    1
            );
            requireRejectedNonMutatingSelectedMutationEvidence(
                    grantEvidence,
                    "grant",
                    NOOP_SELECTED_RUNTIME_HOST_ID,
                    "NOOP"
            );
            require(probe.grants == 1,
                    "NOOP selected runtime host grant evidence path must still call the selected inventory host");
            require(!EchoNativeBootstrapMain.grantNativeItemFromUi("echoashfallprotocol:portable_signal_scanner", 1),
                    "NOOP selected runtime host grant public action must not report playable mutation success");
            require(probe.grants == 2,
                    "NOOP selected runtime host grant public action must call the selected inventory host");
            Map<String, Object> terminalEvidence = EchoNativeBootstrapMain.executeNativeTerminalCommandFromUi(
                    "status",
                    "Noop Terminal"
            );
            requireRejectedNonMutatingSelectedMutationEvidence(
                    terminalEvidence,
                    "terminal",
                    NOOP_SELECTED_RUNTIME_HOST_ID,
                    "NOOP"
            );
            require(probe.hasEvent("command_execution", "echoterminal:terminal"),
                    "NOOP selected runtime host terminal action must still publish through the selected events host");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("selected runtime host NOOP-result rejection smoke could not use AdapterCore: "
                    + exception.getMessage(), exception);
        } finally {
            if (registered) {
                unregisterSelectedRuntimeHost(NOOP_SELECTED_RUNTIME_HOST_ID);
            }
            if (previous == null) {
                System.clearProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
            } else {
                System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, previous);
            }
        }
    }

    private static void requireSelectedRuntimeHostCapabilitiesRejectUnsupportedUiActions() {
        String previous = System.getProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
        boolean registered = false;
        try {
            registerLimitedSelectedRuntimeHost();
            registered = true;
            System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, LIMITED_SELECTED_RUNTIME_HOST_ID);
            List<String> supportedActions = EchoNativeBootstrapMain.nativeUiSupportedActionIds();
            require(supportedActions.contains("native.ui.terminal_command"),
                    "limited selected runtime host must still advertise explicitly supported actions");
            require(!supportedActions.contains("player.scanner_used")
                            && !supportedActions.contains("native.ui.use_scanner")
                            && !supportedActions.contains("player.inventory.grant"),
                    "limited selected runtime host must not advertise unsupported scanner or grant actions");
            Map<String, Object> scannerEvidence = EchoNativeBootstrapMain.useNativeScannerFromUiEvidence();
            requireUnsupportedSelectedRuntimeHostEvidence(scannerEvidence, "scanner");
            Map<String, Object> grantEvidence = EchoNativeBootstrapMain.grantNativeItemFromUiEvidence(
                    "echoashfallprotocol:portable_signal_scanner",
                    1
            );
            requireUnsupportedSelectedRuntimeHostEvidence(grantEvidence, "grant");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("selected runtime host capability rejection smoke could not use AdapterCore: "
                    + exception.getMessage(), exception);
        } finally {
            if (registered) {
                unregisterSelectedRuntimeHost(LIMITED_SELECTED_RUNTIME_HOST_ID);
            }
            if (previous == null) {
                System.clearProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
            } else {
                System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, previous);
            }
        }
    }

    private static void requireMissingSelectedRuntimeHostEvidence(Map<String, Object> evidence, String actionName) {
        require(Boolean.TRUE.equals(evidence.get("selectedRuntimeHostConfigured")),
                actionName + " evidence must report selected host configuration");
        require(MISSING_SELECTED_RUNTIME_HOST_ID.equals(evidence.get("selectedRuntimeHostId")),
                actionName + " evidence must report the required selected host id");
        require(Boolean.FALSE.equals(evidence.get("selectedRuntimeHostResolved")),
                actionName + " evidence must report unresolved selected host");
        require("missing_selected_runtime_host".equals(evidence.get("failureKind")),
                actionName + " evidence must reject a missing selected runtime host");
        require(Boolean.FALSE.equals(evidence.get("mutated")),
                actionName + " evidence must not mutate through fallback when the selected host is missing");
    }

    private static void requireUnsupportedSelectedRuntimeHostEvidence(Map<String, Object> evidence, String actionName) {
        require(Boolean.TRUE.equals(evidence.get("selectedRuntimeHostConfigured")),
                actionName + " evidence must report selected host configuration");
        require(LIMITED_SELECTED_RUNTIME_HOST_ID.equals(evidence.get("selectedRuntimeHostId")),
                actionName + " evidence must report the selected limited-capability host id");
        require(Boolean.TRUE.equals(evidence.get("selectedRuntimeHostResolved")),
                actionName + " evidence must report that the selected host resolved");
        require("unsupported_runtime_action".equals(evidence.get("failureKind")),
                actionName + " evidence must reject unsupported selected-host capabilities");
        require(Boolean.FALSE.equals(evidence.get("mutated")),
                actionName + " evidence must not mutate through fallback when the selected host rejects the action");
    }

    private static void requireAcceptedSelectedMutationEvidence(Map<String, Object> evidence, String actionName) {
        require(Boolean.TRUE.equals(evidence.get("selectedRuntimeHostConfigured")),
                actionName + " evidence must report selected host configuration");
        require(MUTATING_SELECTED_RUNTIME_HOST_ID.equals(evidence.get("selectedRuntimeHostId")),
                actionName + " evidence must report the selected mutating host id");
        require(Boolean.TRUE.equals(evidence.get("selectedRuntimeHostResolved")),
                actionName + " evidence must report that the selected host resolved");
        require(Boolean.TRUE.equals(evidence.get("mutated")),
                actionName + " evidence must report AdapterCore mutation");
        require(Boolean.TRUE.equals(evidence.get("saveTouched")),
                actionName + " evidence must report save mutation");
        require(Boolean.TRUE.equals(evidence.get("missionUpdated")),
                actionName + " evidence must report mission mutation");
        require(Boolean.TRUE.equals(evidence.get("feedbackEmitted")),
                actionName + " evidence must report visible feedback");
        require("MUTATED".equals(evidence.get("status")),
                actionName + " evidence must preserve the AdapterCore MUTATED status");
    }

    private static void requireRejectedNonMutatingSelectedMutationEvidence(
            Map<String, Object> evidence,
            String actionName,
            String runtimeHostId,
            String resultStatus
    ) {
        require(Boolean.TRUE.equals(evidence.get("selectedRuntimeHostConfigured")),
                actionName + " evidence must report selected host configuration");
        require(runtimeHostId.equals(evidence.get("selectedRuntimeHostId")),
                actionName + " evidence must report the selected non-mutating host id");
        require(Boolean.TRUE.equals(evidence.get("selectedRuntimeHostResolved")),
                actionName + " evidence must report that the non-mutating selected host resolved");
        require(Boolean.FALSE.equals(evidence.get("mutated")),
                actionName + " evidence must reject non-mutating AdapterCore results");
        require(Boolean.TRUE.equals(evidence.get("saveTouched")),
                actionName + " evidence must preserve non-mutating save evidence without accepting it");
        require(Boolean.TRUE.equals(evidence.get("missionUpdated")),
                actionName + " evidence must preserve non-mutating mission evidence without accepting it");
        require(Boolean.TRUE.equals(evidence.get("feedbackEmitted")),
                actionName + " evidence must preserve non-mutating feedback evidence without accepting it");
        require(resultStatus.equals(evidence.get("status")),
                actionName + " evidence must preserve the AdapterCore " + resultStatus + " status");
        Map<String, Object> snapshot = object(evidence.get("resultSnapshot"));
        require(Boolean.TRUE.equals(snapshot.get("rawMutationFlag")),
                actionName + " non-mutating evidence must expose and reject raw mutation overclaims");
    }

    private static Map<String, Object> withMutatingSelectedRuntimeHost(
            java.util.function.Supplier<Map<String, Object>> capture
    ) {
        String previous = System.getProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
        boolean registered = false;
        SelectedRuntimeHostMutationProbe probe = new SelectedRuntimeHostMutationProbe();
        try {
            registerMutatingSelectedRuntimeHost(probe);
            registered = true;
            System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, MUTATING_SELECTED_RUNTIME_HOST_ID);
            return capture.get();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("mutating selected runtime host fixture could not use AdapterCore: "
                    + exception.getMessage(), exception);
        } finally {
            if (registered) {
                unregisterSelectedRuntimeHost(MUTATING_SELECTED_RUNTIME_HOST_ID);
            }
            if (previous == null) {
                System.clearProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY);
            } else {
                System.setProperty(NATIVE_RUNTIME_HOST_ID_PROPERTY, previous);
            }
        }
    }

    private static void registerMutatingSelectedRuntimeHost(SelectedRuntimeHostMutationProbe probe)
            throws ReflectiveOperationException {
        Class<?> runtimeHostInterface = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost");
        Class<?> eventsInterface = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost$Events");
        Class<?> inventoryInterface = Class.forName(
                "com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost$PlayerInventory");
        Class<?> nativeResultClass = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost$NativeResult");
        Class<?> registryClass = Class.forName("com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry");
        Class<?> capabilitiesClass = Class.forName("com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities");
        ClassLoader loader = runtimeHostInterface.getClassLoader();
        Object events = Proxy.newProxyInstance(loader, new Class<?>[]{eventsInterface}, (proxy, method, args) -> switch (method.getName()) {
            case "publish" -> probe.publish(args == null ? new Object[0] : args, nativeResultClass);
            case "toString" -> "MutatingSelectedRuntimeHost.Events";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> defaultValue(method.getReturnType());
        });
        Object inventory = Proxy.newProxyInstance(loader, new Class<?>[]{inventoryInterface}, (proxy, method, args) -> switch (method.getName()) {
            case "grant" -> probe.grant(args == null ? new Object[0] : args, nativeResultClass);
            case "toString" -> "MutatingSelectedRuntimeHost.PlayerInventory";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> defaultValue(method.getReturnType());
        });
        InvocationHandler hostHandler = (proxy, method, args) -> switch (method.getName()) {
            case "events" -> events;
            case "playerInventory" -> inventory;
            case "toString" -> "MutatingSelectedRuntimeHost[" + MUTATING_SELECTED_RUNTIME_HOST_ID + "]";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> defaultValue(method.getReturnType());
        };
        Object host = Proxy.newProxyInstance(loader, new Class<?>[]{runtimeHostInterface}, hostHandler);
        Object capabilities = capabilitiesClass
                .getConstructor(String.class, Set.class, Set.class, Set.class,
                        boolean.class, boolean.class, boolean.class)
                .newInstance(
                        MUTATING_SELECTED_RUNTIME_HOST_ID,
                        Set.of(
                                "EchoNativeRuntimeHost.Events",
                                "EchoNativeRuntimeHost.PlayerInventory"
                        ),
                        Set.of(
                                "player.scanner_used",
                                "native.ui.use_scanner",
                                "player.inventory.grant",
                                "native.ui.terminal_command",
                                "native.ui.index_search",
                                "native.ui.hud_refresh",
                                "native.ui.mission_log_update",
                                "native.ui.surface_open",
                                "native.ui.index_bookmark",
                                "native.ui.holomap_state",
                                "native.ui.signalos_terminal",
                                "native.ui.ashfall_drone_command"
                        ),
                        Set.of("echoashfallprotocol:portable_signal_scanner"),
                        true,
                        true,
                        true
                );
        Object registry = registryClass.getMethod("global").invoke(null);
        registryClass
                .getMethod("register", String.class, runtimeHostInterface, capabilitiesClass)
                .invoke(registry, MUTATING_SELECTED_RUNTIME_HOST_ID, host, capabilities);
    }

    private static void registerNonMutatingSelectedRuntimeHost(
            String runtimeHostId,
            SelectedRuntimeHostNonMutatingProbe probe
    )
            throws ReflectiveOperationException {
        Class<?> runtimeHostInterface = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost");
        Class<?> eventsInterface = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost$Events");
        Class<?> inventoryInterface = Class.forName(
                "com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost$PlayerInventory");
        Class<?> nativeResultClass = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost$NativeResult");
        Class<?> registryClass = Class.forName("com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry");
        Class<?> capabilitiesClass = Class.forName("com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities");
        ClassLoader loader = runtimeHostInterface.getClassLoader();
        Object events = Proxy.newProxyInstance(loader, new Class<?>[]{eventsInterface}, (proxy, method, args) -> switch (method.getName()) {
            case "publish" -> probe.publish(args == null ? new Object[0] : args, nativeResultClass);
            case "toString" -> "NonMutatingSelectedRuntimeHost.Events";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> defaultValue(method.getReturnType());
        });
        Object inventory = Proxy.newProxyInstance(loader, new Class<?>[]{inventoryInterface}, (proxy, method, args) -> switch (method.getName()) {
            case "grant" -> probe.grant(args == null ? new Object[0] : args, nativeResultClass);
            case "toString" -> "NonMutatingSelectedRuntimeHost.PlayerInventory";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> defaultValue(method.getReturnType());
        });
        InvocationHandler hostHandler = (proxy, method, args) -> switch (method.getName()) {
            case "events" -> events;
            case "playerInventory" -> inventory;
            case "toString" -> "NonMutatingSelectedRuntimeHost[" + runtimeHostId + "]";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> defaultValue(method.getReturnType());
        };
        Object host = Proxy.newProxyInstance(loader, new Class<?>[]{runtimeHostInterface}, hostHandler);
        Object capabilities = capabilitiesClass
                .getConstructor(String.class, Set.class, Set.class, Set.class,
                        boolean.class, boolean.class, boolean.class)
                .newInstance(
                        runtimeHostId,
                        Set.of(
                                "EchoNativeRuntimeHost.Events",
                                "EchoNativeRuntimeHost.PlayerInventory"
                        ),
                        Set.of(
                                "player.scanner_used",
                                "native.ui.use_scanner",
                                "player.inventory.grant",
                                "native.ui.terminal_command"
                        ),
                        Set.of("echoashfallprotocol:portable_signal_scanner"),
                        true,
                        true,
                        true
                );
        Object registry = registryClass.getMethod("global").invoke(null);
        registryClass
                .getMethod("register", String.class, runtimeHostInterface, capabilitiesClass)
                .invoke(registry, runtimeHostId, host, capabilities);
    }

    private static void registerLimitedSelectedRuntimeHost() throws ReflectiveOperationException {
        Class<?> runtimeHostInterface = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost");
        Class<?> eventsInterface = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost$Events");
        Class<?> inventoryInterface = Class.forName(
                "com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost$PlayerInventory");
        Class<?> registryClass = Class.forName("com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry");
        Class<?> capabilitiesClass = Class.forName("com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities");
        ClassLoader loader = runtimeHostInterface.getClassLoader();
        Object events = passiveProxy(loader, eventsInterface, "LimitedSelectedRuntimeHost.Events");
        Object inventory = passiveProxy(loader, inventoryInterface, "LimitedSelectedRuntimeHost.PlayerInventory");
        InvocationHandler hostHandler = (proxy, method, args) -> switch (method.getName()) {
            case "events" -> events;
            case "playerInventory" -> inventory;
            case "toString" -> "LimitedSelectedRuntimeHost[" + LIMITED_SELECTED_RUNTIME_HOST_ID + "]";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> defaultValue(method.getReturnType());
        };
        Object host = Proxy.newProxyInstance(loader, new Class<?>[]{runtimeHostInterface}, hostHandler);
        Object capabilities = capabilitiesClass
                .getConstructor(String.class, Set.class, Set.class, Set.class,
                        boolean.class, boolean.class, boolean.class)
                .newInstance(
                        LIMITED_SELECTED_RUNTIME_HOST_ID,
                        Set.of(
                                "EchoNativeRuntimeHost.Events",
                                "EchoNativeRuntimeHost.PlayerInventory"
                        ),
                        Set.of("native.ui.terminal_command"),
                        Set.of(),
                        true,
                        true,
                        true
                );
        Object registry = registryClass.getMethod("global").invoke(null);
        registryClass
                .getMethod("register", String.class, runtimeHostInterface, capabilitiesClass)
                .invoke(registry, LIMITED_SELECTED_RUNTIME_HOST_ID, host, capabilities);
    }

    private static Object passiveProxy(ClassLoader loader, Class<?> contract, String label) {
        return Proxy.newProxyInstance(loader, new Class<?>[]{contract}, (proxy, method, args) -> switch (method.getName()) {
            case "toString" -> label;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> defaultValue(method.getReturnType());
        });
    }

    private static void unregisterSelectedRuntimeHost(String runtimeHostId) {
        try {
            Class<?> registryClass = Class.forName("com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry");
            Object registry = registryClass.getMethod("global").invoke(null);
            registryClass.getMethod("unregister", String.class).invoke(registry, runtimeHostId);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Cleanup is best-effort; each verifier run uses a stable id and overwrites registrations.
        }
    }

    private static Object nativeResult(
            Class<?> nativeResultClass,
            String message,
            String eventName,
            String canonicalId
    ) throws ReflectiveOperationException {
        return nativeResultWithStatus(nativeResultClass, "MUTATED", true, message, eventName, canonicalId);
    }

    private static Object nativeResultWithStatus(
            Class<?> nativeResultClass,
            String status,
            boolean rawMutationFlag,
            String message,
            String eventName,
            String canonicalId
    ) throws ReflectiveOperationException {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("hostSaveTouched", true);
        snapshot.put("saveTouched", true);
        snapshot.put("missionAdvanced", true);
        snapshot.put("missionTouched", true);
        snapshot.put("feedbackEmitted", true);
        snapshot.put("hudOrEventEmitted", true);
        snapshot.put("eventName", eventName == null ? "" : eventName);
        snapshot.put("canonicalId", canonicalId == null ? "" : canonicalId);
        snapshot.put("details", Map.of(
                "mission", "selected_runtime_host_probe",
                "saveTouched", true,
                "feedbackEmitted", true
        ));
        return nativeResultClass
                .getConstructor(boolean.class, String.class, String.class, Map.class)
                .newInstance(rawMutationFlag, status, message, Map.copyOf(snapshot));
    }

    private static Object methodValue(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        Method method = target.getClass().getMethod(methodName);
        method.trySetAccessible();
        return method.invoke(target);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int countOccurrences(String text, String needle) {
        if (text == null || needle == null || needle.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == null || returnType == Void.TYPE || !returnType.isPrimitive()) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return 0;
    }

    private static final class SelectedRuntimeHostMutationProbe {
        private int scannerEvents;
        private String scannerEventId = "";
        private String scannerCanonicalId = "";
        private final List<String> eventIds = new ArrayList<>();
        private final List<String> eventCanonicalIds = new ArrayList<>();
        private int grants;
        private String grantItemId = "";
        private int grantCount;

        private Object publish(Object[] args, Class<?> nativeResultClass) throws ReflectiveOperationException {
            Object event = args.length > 0 ? args[0] : null;
            String eventId = text(methodValue(event, "eventId"));
            Map<String, Object> payload = object(methodValue(event, "payload"));
            String canonicalId = text(payload.get("canonicalId"));
            eventIds.add(eventId);
            eventCanonicalIds.add(canonicalId);
            if ("player.scanner_used".equals(eventId)) {
                scannerEvents++;
                scannerEventId = eventId;
                scannerCanonicalId = canonicalId;
            }
            return nativeResult(
                    nativeResultClass,
                    "Selected runtime host scanner event applied.",
                    eventId,
                    canonicalId
            );
        }

        private Object grant(Object[] args, Class<?> nativeResultClass) throws ReflectiveOperationException {
            Object stack = args.length > 1 ? args[1] : null;
            grants++;
            grantItemId = text(methodValue(stack, "itemId"));
            grantCount = number(methodValue(stack, "count"));
            return nativeResult(
                    nativeResultClass,
                    "Selected runtime host inventory grant applied.",
                    "player.inventory.grant",
                    grantItemId
            );
        }

        private boolean hasEvent(String eventId, String canonicalId) {
            for (int index = 0; index < eventIds.size(); index++) {
                if (eventId.equals(eventIds.get(index)) && canonicalId.equals(eventCanonicalIds.get(index))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class SelectedRuntimeHostNonMutatingProbe {
        private final String resultStatus;
        private int scannerEvents;
        private final List<String> eventIds = new ArrayList<>();
        private final List<String> eventCanonicalIds = new ArrayList<>();
        private int grants;

        private SelectedRuntimeHostNonMutatingProbe(String resultStatus) {
            this.resultStatus = resultStatus == null || resultStatus.isBlank() ? "NOOP" : resultStatus;
        }

        private Object publish(Object[] args, Class<?> nativeResultClass) throws ReflectiveOperationException {
            Object event = args.length > 0 ? args[0] : null;
            String eventId = text(methodValue(event, "eventId"));
            Map<String, Object> payload = object(methodValue(event, "payload"));
            String canonicalId = text(payload.get("canonicalId"));
            eventIds.add(eventId);
            eventCanonicalIds.add(canonicalId);
            if ("player.scanner_used".equals(eventId)) {
                scannerEvents++;
            }
            return nativeResultWithStatus(
                    nativeResultClass,
                    resultStatus,
                    true,
                    "Selected runtime host returned " + resultStatus + " for the event.",
                    eventId,
                    canonicalId
            );
        }

        private Object grant(Object[] args, Class<?> nativeResultClass) throws ReflectiveOperationException {
            Object stack = args.length > 1 ? args[1] : null;
            grants++;
            String itemId = text(methodValue(stack, "itemId"));
            return nativeResultWithStatus(
                    nativeResultClass,
                    resultStatus,
                    true,
                    "Selected runtime host returned " + resultStatus + " for the inventory grant.",
                    "player.inventory.grant",
                    itemId
            );
        }

        private boolean hasEvent(String eventId, String canonicalId) {
            for (int index = 0; index < eventIds.size(); index++) {
                if (eventId.equals(eventIds.get(index)) && canonicalId.equals(eventCanonicalIds.get(index))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String readBootstrapSource() {
        return readSourceFile("EchoNativeBootstrapMain.java");
    }

    private static String readSourceFile(String fileName) {
        for (Path candidate : List.of(
                Path.of("echo-native-bootstrap-api", "src", "main", "java", "dev", "echo",
                        "nativeplatform", "bootstrap", fileName),
                Path.of("echo-native-bootstrap-api", "src", "qa", "java", "dev", "echo",
                        "nativeplatform", "bootstrap", fileName),
                Path.of("echo-native-loader", "src", "main", "java", "dev", "echo",
                        "nativeplatform", "loader", fileName),
                Path.of("echo-native-loader", "src", "qa", "java", "dev", "echo",
                        "nativeplatform", "loader", fileName),
                Path.of("echo-native-platform", "echo-native-bootstrap-api", "src", "main", "java", "dev",
                        "echo", "nativeplatform", "bootstrap", fileName),
                Path.of("echo-native-platform", "echo-native-bootstrap-api", "src", "qa", "java", "dev",
                        "echo", "nativeplatform", "bootstrap", fileName),
                Path.of("echo-native-platform", "echo-native-loader", "src", "main", "java", "dev",
                        "echo", "nativeplatform", "loader", fileName),
                Path.of("echo-native-platform", "echo-native-loader", "src", "qa", "java", "dev",
                        "echo", "nativeplatform", "loader", fileName))) {
            if (Files.isRegularFile(candidate)) {
                try {
                    return Files.readString(candidate, StandardCharsets.UTF_8);
                } catch (IOException exception) {
                    throw new AssertionError("native UI verifier could not read " + fileName + ": "
                            + exception.getMessage());
                }
            }
        }
        throw new AssertionError("native UI verifier could not locate " + fileName);
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Map<String, Object> contract, String key) {
        Object value = contract.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> checklist(Map<String, Object> contract, String key) {
        Object value = contract.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .filter(entry -> Boolean.TRUE.equals(entry.get("passed")))
                    .map(entry -> String.valueOf(entry.get("id")))
                    .toList();
        }
        return List.of();
    }

    private static Integer number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> model) {
        Object value = model.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static void requireNativeSurfaceRenderModelsExecute() {
        Map<String, Object> terminal = EchoNativeAgent5UiHandlerRegistry.renderSurface("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "mouseRouted", true,
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "terminalOutput", EchoNativeAgent5UiExpectedValues.terminalOutput(),
                "terminalCommandExecuted", true
        ));
        require(Boolean.TRUE.equals(terminal.get("serviceCodeExecuted")),
                "terminal surface render model must execute service code");
        require(lines(terminal).contains(EchoNativeAgent5UiExpectedValues.terminalCommand()
                        + " -> " + EchoNativeAgent5UiExpectedValues.terminalOutput()),
                "terminal surface render model must include executed command output");
        require("EchoNativeAgent5SurfaceRenderer".equals(terminal.get("rendererClass")),
                "terminal surface render model must be produced by the renderer class");

        Map<String, Object> directTerminal = EchoNativeAgent5SurfaceRenderer.render("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "mouseRouted", true,
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "terminalOutput", EchoNativeAgent5UiExpectedValues.terminalOutput(),
                "terminalCommandExecuted", true
        ), EchoNativeAgent5UiHandlerRegistry.dataSources());
        require(lines(directTerminal).equals(lines(terminal)),
                "surface renderer class must match registry renderSurface output");

        Map<String, Object> holomap = EchoNativeAgent5UiHandlerRegistry.renderSurface("HOLOMAP", Map.of());
        require(lines(holomap).stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.holomapMarker())),
                "HoloMap surface render model must include marker output");

        Map<String, Object> wiki = EchoNativeAgent5UiHandlerRegistry.renderSurface("WIKI", Map.of());
        require(lines(wiki).stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.wikiLink())),
                "Wiki surface render model must include link output");

        Map<String, Object> mainMenu = EchoNativeAgent5UiHandlerRegistry.renderSurface("MAIN_MENU", Map.of());
        require(lines(mainMenu).stream().anyMatch(line -> line.contains("Continue")),
                "main menu surface render model must include menu options");
    }

    private static void requireNativeTerminalEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5TerminalEndToEndAcceptanceSmoke::capture);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native terminal end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native terminal end-to-end acceptance smoke must pass accepted and rejected cases");
        require("EchoNativeAgent5TerminalEndToEndAcceptanceSmoke".equals(
                        smoke.get("terminalEndToEndAcceptanceSmokeClass")),
                "native terminal end-to-end acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native terminal end-to-end acceptance must accept hotkey, focus, typed command, and render");
        require(("terminal_end_to_end:M->TERMINAL:"
                        + EchoNativeAgent5UiExpectedValues.terminalCommand()).equals(accepted.get("effect")),
                "native terminal end-to-end acceptance must report the command chain");
        require(Boolean.TRUE.equals(accepted.get("physicalInputAccepted")),
                "native terminal end-to-end acceptance must include physical input acceptance");
        require(Boolean.TRUE.equals(accepted.get("renderAccepted")),
                "native terminal end-to-end acceptance must include rendered surface acceptance");
        require(Boolean.TRUE.equals(accepted.get("commandExecuted")),
                "native terminal end-to-end acceptance must execute the terminal command");
        require(Boolean.TRUE.equals(accepted.get("terminalRendered")),
                "native terminal end-to-end acceptance must render terminal output");
        require(Boolean.TRUE.equals(accepted.get("runtimeMutationAccepted"))
                        && "native.ui.terminal_command".equals(accepted.get("runtimeActionId"))
                        && "command_execution".equals(accepted.get("eventName")),
                "native terminal end-to-end acceptance must require canonical runtime mutation evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native terminal end-to-end acceptance must reject missing physical input");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native terminal end-to-end acceptance must reject missing render");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoCommand")).get("accepted")),
                "native terminal end-to-end acceptance must reject missing command execution");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoTranscript")).get("accepted")),
                "native terminal end-to-end acceptance must reject missing host transcript");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoMutation")).get("accepted")),
                "native terminal end-to-end acceptance must reject transcript-only command execution");
    }

    private static void requireNativeIndexEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5IndexEndToEndAcceptanceSmoke::capture);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native Index end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native Index end-to-end acceptance smoke must pass accepted and rejected cases");
        require("EchoNativeAgent5IndexEndToEndAcceptanceSmoke".equals(
                        smoke.get("indexEndToEndAcceptanceSmokeClass")),
                "native Index end-to-end acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native Index end-to-end acceptance must accept hotkey, focus, query, and render");
        require(("index_end_to_end:G->INDEX:"
                        + EchoNativeAgent5UiExpectedValues.indexQuery()).equals(accepted.get("effect")),
                "native Index end-to-end acceptance must report the query chain");
        require(Boolean.TRUE.equals(accepted.get("physicalInputAccepted")),
                "native Index end-to-end acceptance must include physical input acceptance");
        require(Boolean.TRUE.equals(accepted.get("renderAccepted")),
                "native Index end-to-end acceptance must include rendered surface acceptance");
        require(Boolean.TRUE.equals(accepted.get("searchExecuted")),
                "native Index end-to-end acceptance must execute the index search");
        require(Boolean.TRUE.equals(accepted.get("indexRendered")),
                "native Index end-to-end acceptance must render index output");
        require(Boolean.TRUE.equals(accepted.get("runtimeMutationAccepted"))
                        && "native.ui.index_search".equals(accepted.get("runtimeActionId"))
                        && "player.terminal_opened".equals(accepted.get("eventName")),
                "native Index end-to-end acceptance must require canonical runtime mutation evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native Index end-to-end acceptance must reject missing physical input");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native Index end-to-end acceptance must reject missing render");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoSearch")).get("accepted")),
                "native Index end-to-end acceptance must reject missing search execution");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoTranscript")).get("accepted")),
                "native Index end-to-end acceptance must reject missing host transcript");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoMutation")).get("accepted")),
                "native Index end-to-end acceptance must reject transcript-only search execution");
    }

    private static void requireNativeLensEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5LensEndToEndAcceptanceSmoke::capture);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native Lens end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native Lens end-to-end acceptance smoke must pass accepted and rejected cases");
        require("EchoNativeAgent5LensEndToEndAcceptanceSmoke".equals(
                        smoke.get("lensEndToEndAcceptanceSmokeClass")),
                "native Lens end-to-end acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native Lens end-to-end acceptance must accept hotkey, focus, scan, and render");
        require(("lens_end_to_end:LEFT_ALT->LENS:"
                        + EchoNativeAgent5UiExpectedValues.lensTarget()).equals(accepted.get("effect")),
                "native Lens end-to-end acceptance must report the scan chain");
        require(Boolean.TRUE.equals(accepted.get("physicalInputAccepted")),
                "native Lens end-to-end acceptance must include physical input acceptance");
        require(Boolean.TRUE.equals(accepted.get("renderAccepted")),
                "native Lens end-to-end acceptance must include rendered surface acceptance");
        require(Boolean.TRUE.equals(accepted.get("scanExecuted")),
                "native Lens end-to-end acceptance must execute the lens scan");
        require(Boolean.TRUE.equals(accepted.get("lensRendered")),
                "native Lens end-to-end acceptance must render lens output");
        require(Boolean.TRUE.equals(accepted.get("runtimeMutationAccepted"))
                        && "player.scanner_used".equals(accepted.get("runtimeActionId"))
                        && "player.scanner_used".equals(accepted.get("eventName")),
                "native Lens end-to-end acceptance must require canonical runtime mutation evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native Lens end-to-end acceptance must reject missing physical input");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native Lens end-to-end acceptance must reject missing render");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoScan")).get("accepted")),
                "native Lens end-to-end acceptance must reject missing scan execution");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoTranscript")).get("accepted")),
                "native Lens end-to-end acceptance must reject missing host transcript");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoMutation")).get("accepted")),
                "native Lens end-to-end acceptance must reject transcript-only scan execution");
    }

    private static void requireNativeInputActionRouterClassesExecute() {
        require("terminal:input".equals(EchoNativeAgent5UiActionRouter.focusPath("TERMINAL", "WIKI")),
                "native action router must resolve terminal focus path");
        String terminalCommand = EchoNativeAgent5UiExpectedValues.terminalCommand();
        Map<String, Object> typed = EchoNativeAgent5UiActionRouter.routeCharacter(
                "TERMINAL",
                "terminal:input",
                terminalCommand.substring(0, Math.max(0, terminalCommand.length() - 1)),
                "",
                terminalCommand.charAt(terminalCommand.length() - 1)
        );
        require(Boolean.TRUE.equals(typed.get("handled")), "native action router must handle terminal typing");
        require(EchoNativeAgent5UiExpectedValues.terminalCommand().equals(typed.get("value")),
                "native action router must update terminal buffer");
        require("NativeLoaderUiActionRouter".equals(typed.get("routerClass")),
                "native action router must identify its executable class");

        Map<String, Object> initialFocus = EchoNativeAgent5UiActionRouter.routeInitialFocus("TERMINAL", "WIKI");
        require(Boolean.TRUE.equals(initialFocus.get("handled")),
                "native action router must route initial focus");
        require("terminal:input".equals(initialFocus.get("focusedControl")),
                "native action router initial focus must target terminal input");
        require("focus:initial:terminal".equals(initialFocus.get("effect")),
                "native action router initial focus must record effect");

        Map<String, Object> terminal = EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand()
        ));
        require(Boolean.TRUE.equals(terminal.get("handled")), "native action router must execute terminal action");
        require("terminalCommandExecuted".equals(terminal.get("executedKey")),
                "native action router must return terminal executed flag");
        require(EchoNativeAgent5UiExpectedValues.terminalOutput().equals(terminal.get("output")),
                "native action router terminal output must match reference");

        Map<String, Object> gatedTerminal = EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("native.ui.terminal_command")
        ));
        require(Boolean.TRUE.equals(gatedTerminal.get("handled")),
                "native action router must execute terminal command when host supports save data");
        require("native.ui.terminal_command".equals(gatedTerminal.get("runtimeActionId")),
                "native action router terminal command must name the runtime terminal action");
        require("command_execution".equals(gatedTerminal.get("runtimeEventName")),
                "native action router terminal command must use the canonical command event");

        Map<String, Object> unsupportedTerminal = EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("player.inventory.grant")
        ));
        require(Boolean.FALSE.equals(unsupportedTerminal.get("handled")),
                "native action router must reject terminal command when active host does not support save data");
        require(String.valueOf(unsupportedTerminal.get("reason")).contains("native.ui.terminal_command"),
                "native action router terminal rejection must name the missing runtime terminal action");
        Map<String, Object> unsupportedTerminalSurface = EchoNativeAgent5UiHandlerRegistry.renderSurface("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "initialFocusRouted", true,
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("player.inventory.grant")
        ));
        require(lines(unsupportedTerminalSurface).stream().noneMatch(line -> line.contains("terminal:input ready")),
                "native Terminal surface must not show command action as ready when host rejects it");
        require(lines(unsupportedTerminalSurface).stream().anyMatch(line -> line.contains("terminal command unavailable")),
                "native Terminal surface must show host-gated command unavailability");
        Map<String, Object> substringOnlyTerminal = EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", "prefix.native.ui.terminal_command.suffix"
        ));
        require(Boolean.FALSE.equals(substringOnlyTerminal.get("handled")),
                "native action router must reject stringified supported actions that only contain the terminal action as a substring");
        Map<String, Object> substringOnlyTerminalSurface = EchoNativeAgent5UiHandlerRegistry.renderSurface("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "initialFocusRouted", true,
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", "prefix.native.ui.terminal_command.suffix"
        ));
        require(lines(substringOnlyTerminalSurface).stream().noneMatch(line -> line.contains("terminal:input ready")),
                "native Terminal surface must not show command action from substring-only host support");
        Map<String, Object> delimitedTerminal = EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", "player.inventory.grant, native.ui.terminal_command"
        ));
        require(Boolean.TRUE.equals(delimitedTerminal.get("handled")),
                "native action router must accept delimited string host support with an exact terminal action token");

        Map<String, Object> index = EchoNativeAgent5UiActionRouter.activate("INDEX", Map.of(
                "focusedControl", "index:search",
                "indexBuffer", EchoNativeAgent5UiExpectedValues.indexQuery()
        ));
        require(Boolean.TRUE.equals(index.get("handled")), "native action router must execute index search");
        require("indexSearchExecuted".equals(index.get("executedKey")),
                "native action router must return index executed flag");

        Map<String, Object> gatedIndex = EchoNativeAgent5UiActionRouter.activate("INDEX", Map.of(
                "focusedControl", "index:search",
                "indexBuffer", EchoNativeAgent5UiExpectedValues.indexQuery(),
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("native.ui.index_search")
        ));
        require(Boolean.TRUE.equals(gatedIndex.get("handled")),
                "native action router must execute index search when host supports events");
        require("native.ui.index_search".equals(gatedIndex.get("runtimeActionId")),
                "native action router index search must name the runtime index action");
        require("player.terminal_opened".equals(gatedIndex.get("runtimeEventName")),
                "native action router index search must use the canonical terminal-opened event");

        Map<String, Object> unsupportedIndex = EchoNativeAgent5UiActionRouter.activate("INDEX", Map.of(
                "focusedControl", "index:search",
                "indexBuffer", EchoNativeAgent5UiExpectedValues.indexQuery(),
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("native.ui.terminal_command")
        ));
        require(Boolean.FALSE.equals(unsupportedIndex.get("handled")),
                "native action router must reject index search when active host does not support events");
        require(String.valueOf(unsupportedIndex.get("reason")).contains("native.ui.index_search"),
                "native action router index rejection must name the missing runtime index action");
        Map<String, Object> unsupportedIndexSurface = EchoNativeAgent5UiHandlerRegistry.renderSurface("INDEX", Map.of(
                "focusedControl", "index:search",
                "initialFocusRouted", true,
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("native.ui.terminal_command")
        ));
        require(lines(unsupportedIndexSurface).stream().noneMatch(line -> line.contains("index:search ready")),
                "native Index surface must not show search action as ready when host rejects it");
        require(lines(unsupportedIndexSurface).stream().anyMatch(line -> line.contains("Index search unavailable")),
                "native Index surface must show host-gated search unavailability");
        Map<String, Object> substringOnlyIndex = EchoNativeAgent5UiActionRouter.activate("INDEX", Map.of(
                "focusedControl", "index:search",
                "indexBuffer", EchoNativeAgent5UiExpectedValues.indexQuery(),
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", "native.ui.index_search.extra"
        ));
        require(Boolean.FALSE.equals(substringOnlyIndex.get("handled")),
                "native action router must reject stringified supported actions that only contain the index action as a substring");
        Map<String, Object> substringOnlyIndexSurface = EchoNativeAgent5UiHandlerRegistry.renderSurface("INDEX", Map.of(
                "focusedControl", "index:search",
                "initialFocusRouted", true,
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", "native.ui.index_search.extra"
        ));
        require(lines(substringOnlyIndexSurface).stream().noneMatch(line -> line.contains("index:search ready")),
                "native Index surface must not show search action from substring-only host support");

        Map<String, Object> lens = EchoNativeAgent5UiActionRouter.activate("LENS", Map.of(
                "focusedControl", "lens:scan"
        ));
        require(Boolean.TRUE.equals(lens.get("handled")), "native action router must execute lens scan");
        require("lensScanExecuted".equals(lens.get("executedKey")),
                "native action router must return lens executed flag");

        Map<String, Object> unsupportedLens = EchoNativeAgent5UiActionRouter.activate("LENS", Map.of(
                "focusedControl", "lens:scan",
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("player.inventory.grant")
        ));
        require(Boolean.FALSE.equals(unsupportedLens.get("handled")),
                "native action router must reject scanner action when active host does not support it");
        require(String.valueOf(unsupportedLens.get("reason")).contains("unsupported-host-action"),
                "native action router scanner rejection must name unsupported host action");
        Map<String, Object> unsupportedLensSurface = EchoNativeAgent5UiHandlerRegistry.renderSurface("LENS", Map.of(
                "focusedControl", "lens:scan",
                "initialFocusRouted", true,
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("player.inventory.grant")
        ));
        require(lines(unsupportedLensSurface).stream().noneMatch(line -> line.contains("lens:scan ready")),
                "native Lens surface must not show scanner action as ready when host rejects it");
        require(lines(unsupportedLensSurface).stream().anyMatch(line -> line.contains("scanner unavailable")),
                "native Lens surface must show host-gated scanner unavailability");
        Map<String, Object> substringOnlyLens = EchoNativeAgent5UiActionRouter.activate("LENS", Map.of(
                "focusedControl", "lens:scan",
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", "xplayer.scanner_usedx native.ui.use_scanner.preview"
        ));
        require(Boolean.FALSE.equals(substringOnlyLens.get("handled")),
                "native action router must reject stringified supported actions that only contain scanner actions as substrings");
        Map<String, Object> substringOnlyLensSurface = EchoNativeAgent5UiHandlerRegistry.renderSurface("LENS", Map.of(
                "focusedControl", "lens:scan",
                "initialFocusRouted", true,
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", "xplayer.scanner_usedx native.ui.use_scanner.preview"
        ));
        require(lines(substringOnlyLensSurface).stream().noneMatch(line -> line.contains("lens:scan ready")),
                "native Lens surface must not show scanner action from substring-only host support");

        Map<String, Object> recovery = EchoNativeAgent5UiActionRouter.activate("RECOVERY", Map.of(
                "focusedControl", "recovery:recover",
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("player.inventory.grant")
        ));
        require(Boolean.TRUE.equals(recovery.get("handled")),
                "native action router must execute recovery grant action when host supports inventory grants");
        require("recoveryActionExecuted".equals(recovery.get("executedKey")),
                "native action router recovery grant must return recovery executed flag");
        require("player.inventory.grant".equals(recovery.get("runtimeActionId")),
                "native action router recovery grant must name the runtime inventory grant action");
        require("echoashfallprotocol:portable_signal_scanner".equals(recovery.get("grantItemId")),
                "native action router recovery grant must use the canonical portable scanner item id");
        require(Integer.valueOf(1).equals(recovery.get("grantItemCount")),
                "native action router recovery grant must use a concrete item count");

        Map<String, Object> unsupportedRecovery = EchoNativeAgent5UiActionRouter.activate("RECOVERY", Map.of(
                "focusedControl", "recovery:recover",
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("player.scanner_used", "native.ui.use_scanner")
        ));
        require(Boolean.FALSE.equals(unsupportedRecovery.get("handled")),
                "native action router must reject recovery grant when active host does not support inventory grants");
        require(String.valueOf(unsupportedRecovery.get("reason")).contains("player.inventory.grant"),
                "native action router recovery rejection must name the missing inventory grant action");
        Map<String, Object> unsupportedRecoverySurface = EchoNativeAgent5UiHandlerRegistry.renderSurface("RECOVERY", Map.of(
                "focusedControl", "recovery:recover",
                "initialFocusRouted", true,
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", List.of("player.scanner_used", "native.ui.use_scanner")
        ));
        require(lines(unsupportedRecoverySurface).stream().noneMatch(line -> line.contains("recovery:recover ready")),
                "native Recovery surface must not show grant action as ready when host rejects it");
        require(lines(unsupportedRecoverySurface).stream().anyMatch(line -> line.contains("recovery grant unavailable")),
                "native Recovery surface must show host-gated inventory grant unavailability");
        Map<String, Object> substringOnlyRecovery = EchoNativeAgent5UiActionRouter.activate("RECOVERY", Map.of(
                "focusedControl", "recovery:recover",
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", "player.inventory.grant.preview"
        ));
        require(Boolean.FALSE.equals(substringOnlyRecovery.get("handled")),
                "native action router must reject stringified supported actions that only contain inventory grants as substrings");
        Map<String, Object> substringOnlyRecoverySurface = EchoNativeAgent5UiHandlerRegistry.renderSurface("RECOVERY", Map.of(
                "focusedControl", "recovery:recover",
                "initialFocusRouted", true,
                "runtimeHostActionGateActive", true,
                "runtimeSupportedActions", "player.inventory.grant.preview"
        ));
        require(lines(substringOnlyRecoverySurface).stream().noneMatch(line -> line.contains("recovery:recover ready")),
                "native Recovery surface must not show grant action from substring-only host support");

        Map<String, Object> escape = EchoNativeAgent5UiActionRouter.routeKey("ESCAPE", "TERMINAL", "WIKI");
        require(Boolean.TRUE.equals(escape.get("handled")), "native action router must handle escape route");
        require("PAUSE".equals(escape.get("destinationMode")), "native action router escape must open pause");
        require("TERMINAL".equals(escape.get("destinationPreviousMode")),
                "native action router escape must preserve previous mode");

        Map<String, Object> unmappedHudHotkey = EchoNativeAgent5UiActionRouter.routeKey("H", "TERMINAL", "WIKI");
        require(Boolean.FALSE.equals(unmappedHudHotkey.get("handled")),
                "native action router must not invent a HUD hotkey");

        Map<String, Object> hudUpdate = EchoNativeAgent5UiActionRouter.routeHudUpdate(Map.of(
                "hudHealth", EchoNativeAgent5UiExpectedValues.hud().get("health")));
        require(Boolean.TRUE.equals(hudUpdate.get("handled")),
                "native action router must execute HUD update");
        require(Integer.valueOf(EchoNativeAgent5UiExpectedValues.hudUpdatedHealth()).equals(hudUpdate.get("hudHealth")),
                "native action router HUD update must mutate health");
        require("hud:update:health_hazard_mission".equals(hudUpdate.get("effect")),
                "native action router HUD update must record effect");

        Map<String, Object> cameraCinematic = EchoNativeAgent5UiActionRouter.routeCameraCinematicFrame(Map.of("cinematicFrame", 0));
        require(Boolean.TRUE.equals(cameraCinematic.get("handled")),
                "native action router must execute camera/cinematic frame");
        require("over_shoulder".equals(cameraCinematic.get("cameraMode")),
                "native action router camera/cinematic frame must route camera mode");
        require(Integer.valueOf(1).equals(cameraCinematic.get("cinematicFrame")),
                "native action router camera/cinematic frame must advance frame");
        require(("camera_cinematic:frame:" + EchoNativeAgent5UiExpectedValues.terminal().get("title"))
                        .equals(cameraCinematic.get("effect")),
                "native action router camera/cinematic frame must record effect");

        Map<String, Object> mission = EchoNativeAgent5UiActionRouter.routeMissionLogUpdate(Map.of(
                "missionProgress", 0.25D,
                "missionStatus", "TRACKED"
        ));
        require(Boolean.TRUE.equals(mission.get("handled")),
                "native action router must execute mission log update");
        require("UPDATED".equals(mission.get("missionStatus")),
                "native action router mission update must set updated status");
        require(Double.valueOf(0.5D).equals(mission.get("missionProgress")),
                "native action router mission update must advance progress");
        require("mission:update:echoashfallprotocol:secure_crash_outpost".equals(mission.get("effect")),
                "native action router mission update must record update effect");

        Map<String, Object> mainMenu = EchoNativeAgent5UiActionRouter.routeMainMenuOption("Settings");
        require(Boolean.TRUE.equals(mainMenu.get("handled")),
                "native action router must execute main-menu option activation");
        require("SETTINGS".equals(mainMenu.get("destinationMode")),
                "native action router main-menu Settings option must route to settings");
        require("main_menu:settings".equals(mainMenu.get("effect")),
                "native action router main-menu Settings option must record effect");
    }

    private static void requireNativeScreenHostModelsExecute() {
        Map<String, Object> hostModel = EchoNativeAgent5ScreenHostModel.render("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "mouseRouted", true,
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "terminalOutput", EchoNativeAgent5UiExpectedValues.terminalOutput(),
                "terminalCommandExecuted", true
        ), "ashfall", 12, 3, 2, 1);
        require(Boolean.TRUE.equals(hostModel.get("serviceCodeExecuted")),
                "native screen host model must execute service code");
        require("EchoNativeAgent5ScreenHostModel".equals(hostModel.get("hostModelClass")),
                "native screen host model must identify its executable class");
        require("ECHO NATIVE // TERMINAL".equals(hostModel.get("screenTitle")),
                "native screen host model must render the title line");
        require(list(hostModel, "headerLines").stream().anyMatch(line -> line.contains(
                        "Health " + EchoNativeAgent5UiExpectedValues.hud().get("health"))),
                "native screen host model must include HUD health");
        require(list(hostModel, "headerLines").stream()
                        .anyMatch(line -> line.contains(String.join(" / ",
                                EchoNativeAgent5UiExpectedValues.notificationMessages()))),
                "native screen host model must include notification queue");
        require(list(hostModel, "surfaceLines").contains(EchoNativeAgent5UiExpectedValues.terminalCommand()
                        + " -> " + EchoNativeAgent5UiExpectedValues.terminalOutput()),
                "native screen host model must include surface body lines");
        require(String.valueOf(hostModel.get("footerLine")).contains("M Terminal")
                        && String.valueOf(hostModel.get("footerLine")).contains("J Map")
                        && String.valueOf(hostModel.get("footerLine")).contains("N SignalOS")
                        && String.valueOf(hostModel.get("footerLine")).contains("X/C/Y/Z Drone"),
                "native screen host model must include the Agent 5 hotkey footer");
    }

    private static void requireNativeFocusManagerSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5FocusManagerSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native focus manager smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native focus manager smoke must pass focus/input gating");
        require("EchoNativeAgent5FocusManagerSmoke".equals(smoke.get("focusManagerSmokeClass")),
                "native focus manager smoke must identify its executable class");
        require(list(smoke, "focusOrder").equals(List.of(
                "terminal:input",
                "index:search",
                "lens:scan",
                "recovery:recover"
        )), "native focus manager smoke must preserve focus order");
        require(list(smoke, "ignoredReasons").containsAll(List.of("character:unfocused", "character:control")),
                "native focus manager smoke must reject unfocused and control-character typing");
        require(EchoNativeAgent5UiExpectedValues.terminalCommand().equals(smoke.get("terminalBuffer")),
                "native focus manager smoke must build terminal buffer through typed routing");
        require(EchoNativeAgent5UiExpectedValues.indexQuery().equals(smoke.get("indexBuffer")),
                "native focus manager smoke must build index buffer through typed routing");
        require(list(smoke, "activationKeys").containsAll(List.of(
                "terminalCommandExecuted",
                "indexSearchExecuted",
                "lensScanExecuted",
                "recoveryActionExecuted"
        )), "native focus manager smoke must activate focused actions");
        require(list(smoke, "renderedFocusLines").stream().anyMatch(line -> line.contains("terminal:input ready")),
                "native focus manager smoke must render terminal focus ready");
        require(list(smoke, "renderedFocusLines").stream().anyMatch(line -> line.contains("index:search ready")),
                "native focus manager smoke must render index focus ready");
        require(list(smoke, "renderedFocusLines").stream().anyMatch(line -> line.contains("lens:scan ready")),
                "native focus manager smoke must render lens focus ready");
        require(list(smoke, "renderedFocusLines").stream().anyMatch(line -> line.contains("recovery:recover ready")),
                "native focus manager smoke must render recovery focus ready");
    }

    private static void requireNativeInitialFocusSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5InitialFocusSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native initial focus smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native initial focus smoke must pass keyboard-first behavior");
        require("EchoNativeAgent5InitialFocusSmoke".equals(smoke.get("initialFocusSmokeClass")),
                "native initial focus smoke must identify its executable class");
        require(list(smoke, "focusPaths").equals(List.of(
                "terminal:input",
                "index:search",
                "lens:scan",
                "recovery:recover"
        )), "native initial focus smoke must focus actionable controls");
        require(list(smoke, "effects").equals(List.of(
                "focus:initial:terminal",
                "focus:initial:index",
                "focus:initial:lens",
                "focus:initial:recovery"
        )), "native initial focus smoke must record initial focus effects");
        require("status".equals(smoke.get("terminalBuffer")),
                "native initial focus smoke must allow terminal typing without mouse");
        require("ashfall".equals(smoke.get("indexBuffer")),
                "native initial focus smoke must allow index typing without mouse");
        require(list(smoke, "executedKeys").containsAll(List.of("lensScanExecuted", "recoveryActionExecuted")),
                "native initial focus smoke must activate lens and recovery without mouse");
        require(list(smoke, "renderedLines").stream().anyMatch(line -> line.contains("terminal:input ready")),
                "native initial focus smoke must render terminal initial focus ready");
        require(list(smoke, "renderedLines").stream().anyMatch(line -> line.contains("lens:scan ready")),
                "native initial focus smoke must render lens initial focus ready");
    }

    private static void requireNativeTextEditingSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5TextEditingSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native text editing smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native text editing smoke must pass edit behavior");
        require("EchoNativeAgent5TextEditingSmoke".equals(smoke.get("textEditingSmokeClass")),
                "native text editing smoke must identify its executable class");
        require(EchoNativeAgent5UiExpectedValues.terminalCommand().equals(smoke.get("terminalBuffer")),
                "native text editing smoke must correct terminal buffer with Backspace");
        require(EchoNativeAgent5UiExpectedValues.indexQuery().equals(smoke.get("indexBuffer")),
                "native text editing smoke must correct index buffer with Backspace");
        require("".equals(smoke.get("emptyBackspaceValue")),
                "native text editing smoke must keep empty Backspace safe");
        require(list(smoke, "editEffects").containsAll(List.of(
                "terminal-character",
                "terminal-backspace",
                "index-character",
                "index-backspace"
        )), "native text editing smoke must record character and Backspace effects");
        require(list(smoke, "activationKeys").containsAll(List.of("terminalCommandExecuted", "indexSearchExecuted")),
                "native text editing smoke must activate corrected text inputs");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.terminalCommand()
                                + " -> " + EchoNativeAgent5UiExpectedValues.terminalOutput())),
                "native text editing smoke must render corrected terminal output");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.indexQuery()
                                + " -> " + EchoNativeAgent5UiExpectedValues.indexSearchOutput())),
                "native text editing smoke must render corrected index output");
    }

    private static void requireNativeMouseActivationSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5MouseActivationSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native mouse activation smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native mouse activation smoke must pass click activation behavior");
        require("EchoNativeAgent5MouseActivationSmoke".equals(smoke.get("mouseActivationSmokeClass")),
                "native mouse activation smoke must identify its executable class");
        require(list(smoke, "focusPaths").containsAll(List.of(
                "terminal:input",
                "index:search",
                "lens:scan",
                "recovery:recover"
        )), "native mouse activation smoke must focus all actionable controls");
        require(list(smoke, "clickEffects").containsAll(List.of(
                "mouse:focus:terminal",
                "mouse:activate:terminal",
                "mouse:activate:index",
                "mouse:activate:lens",
                "mouse:activate:recovery"
        )), "native mouse activation smoke must record focus and activation effects");
        require(list(smoke, "executedKeys").containsAll(List.of(
                "terminalCommandExecuted",
                "indexSearchExecuted",
                "lensScanExecuted",
                "recoveryActionExecuted"
        )), "native mouse activation smoke must execute all clicked actions");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.terminalCommand()
                                + " -> " + EchoNativeAgent5UiExpectedValues.terminalOutput())),
                "native mouse activation smoke must render terminal click output");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.indexQuery()
                                + " -> " + EchoNativeAgent5UiExpectedValues.indexSearchOutput())),
                "native mouse activation smoke must render index click output");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.lensOutput())),
                "native mouse activation smoke must render lens click output");
        require(list(smoke, "renderedLines").stream().anyMatch(line -> line.contains("Status: RECOVERED")),
                "native mouse activation smoke must render recovery click output");
    }

    private static void requireNativeListNavigationSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5ListNavigationSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native list navigation smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native list navigation smoke must pass Up/Down behavior");
        require("EchoNativeAgent5ListNavigationSmoke".equals(smoke.get("listNavigationSmokeClass")),
                "native list navigation smoke must identify its executable class");
        require(list(smoke, "selectedOptions").equals(List.of(
                "New Run",
                "Settings",
                "Theme",
                "Input Mode",
                "Quit to Main Menu"
        )), "native list navigation smoke must select expected rows");
        require(list(smoke, "effects").containsAll(List.of(
                "list:main_menu:down",
                "list:settings:down",
                "list:pause:up"
        )), "native list navigation smoke must record list navigation effects");
        require(list(smoke, "renderedLines").stream().anyMatch(line -> line.contains("Selected: Settings")),
                "native list navigation smoke must render selected main-menu option");
        require(list(smoke, "renderedLines").stream().anyMatch(line -> line.contains("Selected: Input Mode")),
                "native list navigation smoke must render selected settings option");
        require(list(smoke, "renderedLines").stream().anyMatch(line -> line.contains("Selected: Quit to Main Menu")),
                "native list navigation smoke must render selected pause option");
    }

    private static void requireNativeNotificationDismissSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5NotificationDismissSmoke.capture("ashfall", 12, 3, 2, 1);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native notification dismiss smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native notification dismiss smoke must pass queue lifecycle behavior");
        require("EchoNativeAgent5NotificationDismissSmoke".equals(smoke.get("notificationDismissSmokeClass")),
                "native notification dismiss smoke must identify its executable class");
        require(String.valueOf(smoke.get("dismissedId")).startsWith("echoterminal:"),
                "native notification dismiss smoke must dismiss the oldest notification");
        require(!EchoNativeAgent5UiExpectedValues.notificationMessages().isEmpty()
                        && EchoNativeAgent5UiExpectedValues.notificationMessages().get(0)
                                .equals(smoke.get("dismissedMessage")),
                "native notification dismiss smoke must report dismissed message");
        require("notification:dismiss-oldest".equals(smoke.get("effect")),
                "native notification dismiss smoke must record dismiss effect");
        require(list(smoke, "remainingMessages").equals(
                        EchoNativeAgent5UiExpectedValues.notificationMessages().size() <= 1
                                ? List.of()
                                : EchoNativeAgent5UiExpectedValues.notificationMessages().subList(
                                        1,
                                        EchoNativeAgent5UiExpectedValues.notificationMessages().size())),
                "native notification dismiss smoke must keep the remaining notification");
        require(list(smoke, "afterHeaderLines").stream()
                        .anyMatch(line -> line.contains("Notifications: "
                                + String.join(" / ",
                                        EchoNativeAgent5UiExpectedValues.notificationMessages().size() <= 1
                                                ? List.of()
                                                : EchoNativeAgent5UiExpectedValues.notificationMessages().subList(
                                                        1,
                                                        EchoNativeAgent5UiExpectedValues.notificationMessages().size())))),
                "native notification dismiss smoke must update host notification header");
    }

    private static void requireNativeNotificationEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5NotificationEndToEndAcceptanceSmoke.capture("ashfall", 12, 3, 2, 1);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native notification end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native notification end-to-end acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5NotificationEndToEndAcceptanceSmoke".equals(
                        smoke.get("notificationEndToEndAcceptanceSmokeClass")),
                "native notification end-to-end acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native notification end-to-end acceptance smoke must accept complete notification chain");
        require("notification_end_to_end:queue->hud:drop-oldest".equals(accepted.get("effect")),
                "native notification end-to-end acceptance smoke must record accepted notification effect");
        require(Boolean.TRUE.equals(accepted.get("queueAccepted"))
                        && Boolean.TRUE.equals(accepted.get("hudAccepted"))
                        && Boolean.TRUE.equals(accepted.get("dismissAccepted")),
                "native notification end-to-end acceptance smoke must accept queue, HUD, and dismiss stages");
        require(String.valueOf(accepted.get("dismissedId")).startsWith("echoterminal:"),
                "native notification end-to-end acceptance smoke must dismiss the oldest notification");
        require(list(accepted, "remainingMessages").equals(
                        EchoNativeAgent5UiExpectedValues.notificationMessages().size() <= 1
                                ? List.of()
                                : EchoNativeAgent5UiExpectedValues.notificationMessages().subList(
                                        1,
                                        EchoNativeAgent5UiExpectedValues.notificationMessages().size())),
                "native notification end-to-end acceptance smoke must keep the remaining notification");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoQueue")).get("accepted")),
                "native notification end-to-end acceptance smoke must reject missing queue proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoDismiss")).get("accepted")),
                "native notification end-to-end acceptance smoke must reject missing dismiss proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoHud")).get("accepted")),
                "native notification end-to-end acceptance smoke must reject missing HUD proof");
    }

    private static void requireNativeUiReferenceAuditSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5UiReferenceAuditSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native UI reference audit smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native UI reference audit smoke must pass behavior coverage checks");
        require("EchoNativeAgent5UiReferenceAuditSmoke".equals(smoke.get("uiReferenceAuditSmokeClass")),
                "native UI reference audit smoke must identify its executable class");
        require(Integer.valueOf(14).equals(number(smoke.get("behaviorCount"))),
                "native UI reference audit smoke must cover all source-backed UI behaviors");
        require(list(smoke, "behaviors").equals(List.of(
                        "custom_main_menu",
                        "terminal",
                        "index",
                        "lens_scanner",
                        "hud",
                        "mission_log",
                        "notifications",
                        "holomap",
                        "wiki",
                        "settings",
                        "pause_flow",
                        "death_recovery_screen",
                        "signalos_terminal",
                        "ashfall_drone"
                )),
                "native UI reference audit smoke must preserve source-backed behavior order");
        require(list(smoke, "missingScreens").isEmpty()
                        && list(smoke, "missingDataSources").isEmpty()
                        && list(smoke, "missingAcceptanceFeatures").isEmpty(),
                "native UI reference audit smoke must map every behavior to screen, data source, and acceptance feature");
    }

    private static void requireNativeUiRuntimeEquivalenceAuditSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5UiRuntimeEquivalenceAuditSmoke::capture);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native UI runtime equivalence audit smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native UI runtime equivalence audit smoke must pass equivalence checks: " + smoke);
        require("EchoNativeAgent5UiRuntimeEquivalenceAuditSmoke".equals(
                        smoke.get("uiRuntimeEquivalenceAuditSmokeClass")),
                "native UI runtime equivalence audit smoke must identify its executable class");
        require(Boolean.TRUE.equals(smoke.get("screenIdsMatch"))
                        && Boolean.TRUE.equals(smoke.get("terminalMatches"))
                        && Boolean.TRUE.equals(smoke.get("indexMatches"))
                        && Boolean.TRUE.equals(smoke.get("lensMatches"))
                        && Boolean.TRUE.equals(smoke.get("hudMatches"))
                        && Boolean.TRUE.equals(smoke.get("missionMatches"))
                        && Boolean.TRUE.equals(smoke.get("notificationsMatch")),
                "native UI runtime equivalence audit smoke must match all Phase 4 runtime values");
        require(EchoNativeAgent5UiExpectedValues.terminalOutput().equals(smoke.get("terminalOutput")),
                "native UI runtime equivalence audit smoke must preserve terminal output");
        require("UPDATED".equals(smoke.get("missionUpdateStatus"))
                        && Double.valueOf(0.5D).equals(smoke.get("missionUpdateProgress")),
                "native UI runtime equivalence audit smoke must preserve mission update output");
    }

    private static void requireNativeScreenCorePrimitiveExecutionSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5ScreenCorePrimitiveExecutionSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native ScreenCore primitive execution smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native ScreenCore primitive execution smoke must pass behavior checks");
        require("EchoNativeAgent5ScreenCorePrimitiveExecutionSmoke".equals(
                        smoke.get("screenCorePrimitiveExecutionSmokeClass")),
                "native ScreenCore primitive execution smoke must identify its executable class");
        require(list(smoke, "executedPrimitives").equals(List.of(
                        "EchoScreen",
                        "EchoScreenStack",
                        "EchoScreenRoute",
                        "EchoHudLayer",
                        "EchoInputAction",
                        "EchoTheme",
                        "EchoWidget",
                        "EchoTextInput",
                        "EchoButton",
                        "EchoListView",
                        "EchoTerminalBuffer",
                        "EchoNotification"
                )),
                "native ScreenCore primitive execution smoke must execute every required primitive");
        require("echoterminal:terminal".equals(smoke.get("stackCurrent")),
                "native ScreenCore primitive execution smoke must exercise screen stack current state");
        require("terminal:input".equals(smoke.get("routeFocusPath")),
                "native ScreenCore primitive execution smoke must exercise screen route focus");
        require(EchoNativeAgent5UiExpectedValues.terminalCommand().equals(smoke.get("terminalInputValue")),
                "native ScreenCore primitive execution smoke must exercise text input");
        require("Settings".equals(smoke.get("selectedRow")),
                "native ScreenCore primitive execution smoke must exercise list selection");
        require(!EchoNativeAgent5UiExpectedValues.notificationMessages().isEmpty()
                        && EchoNativeAgent5UiExpectedValues.notificationMessages().get(0)
                                .equals(smoke.get("notificationMessage")),
                "native ScreenCore primitive execution smoke must exercise notification payload");
    }

    private static void requireNativePhase5UiParityAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5Phase5UiParityAcceptanceSmoke::capture);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native Phase 5 UI parity acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native Phase 5 UI parity acceptance smoke must pass the full checklist: " + smoke);
        require("EchoNativeAgent5Phase5UiParityAcceptanceSmoke".equals(
                        smoke.get("phase5UiParityAcceptanceSmokeClass")),
                "native Phase 5 UI parity acceptance smoke must identify its executable class");
        require(checklist(smoke, "checklist").equals(List.of(
                        "terminal_opens",
                        "terminal_command_executes",
                        "index_opens_and_searches",
                        "lens_scans_target",
                        "hud_updates_health_hazard_mission",
                        "holomap_opens",
                        "wiki_page_opens",
                        "custom_main_menu_appears",
                        "no_screen_crash"
                )),
                "native Phase 5 UI parity acceptance smoke must cover every Phase 5 done item");
        require(("terminal_end_to_end:M->TERMINAL:"
                        + EchoNativeAgent5UiExpectedValues.terminalCommand()).equals(smoke.get("terminalEffect")),
                "native Phase 5 UI parity acceptance smoke must prove terminal execution");
        require(("index_end_to_end:G->INDEX:"
                        + EchoNativeAgent5UiExpectedValues.indexQuery()).equals(smoke.get("indexEffect")),
                "native Phase 5 UI parity acceptance smoke must prove index search");
        require(("lens_end_to_end:LEFT_ALT->LENS:"
                        + EchoNativeAgent5UiExpectedValues.lensTarget()).equals(smoke.get("lensEffect")),
                "native Phase 5 UI parity acceptance smoke must prove lens scan");
        require(EchoNativeAgent5UiExpectedValues.hudOverlayEffect().equals(smoke.get("hudEffect")),
                "native Phase 5 UI parity acceptance smoke must prove HUD update");
        require(("holomap_end_to_end:J->HOLOMAP:"
                        + EchoNativeAgent5UiExpectedValues.holomapMarker()).equals(smoke.get("holomapEffect")),
                "native Phase 5 UI parity acceptance smoke must prove HoloMap open");
        require("wiki_end_to_end:MODULE_ROUTE->WIKI:ashfall".equals(smoke.get("wikiEffect")),
                "native Phase 5 UI parity acceptance smoke must prove Wiki page open");
        require("main_menu_end_to_end:accepted:4".equals(smoke.get("mainMenuEffect")),
                "native Phase 5 UI parity acceptance smoke must prove custom main menu");
        require("ui_host_end_to_end:M->TERMINAL:10".equals(smoke.get("uiHostEffect")),
                "native Phase 5 UI parity acceptance smoke must prove no screen crash");
    }

    private static void requireNativeLiveClientAttachmentAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveClientAttachmentAcceptanceSmoke.capture();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live-client attachment acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live-client attachment acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveClientAttachmentAcceptanceSmoke".equals(
                        smoke.get("liveClientAttachmentAcceptanceSmokeClass")),
                "native live-client attachment acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native live-client attachment acceptance smoke must accept the ready client path");
        require("live_client_attachment:accepted:EchoNativeDashboardScreen".equals(accepted.get("effect")),
                "native live-client attachment acceptance smoke must preserve the accepted effect");
        require(Boolean.TRUE.equals(accepted.get("minecraftClientReady"))
                        && Boolean.TRUE.equals(accepted.get("dashboardScreenCompiled"))
                        && Boolean.TRUE.equals(accepted.get("clientThreadAccepted"))
                        && Boolean.TRUE.equals(accepted.get("physicalHotkeyPollingReady"))
                        && Boolean.TRUE.equals(accepted.get("screenClassMatches")),
                "native live-client attachment acceptance smoke must verify every prerequisite");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoClient")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoScreen")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoClientThread")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoWindow")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedScreenMismatch")).get("accepted")),
                "native live-client attachment acceptance smoke must reject incomplete attachment states");
    }

    private static void requireNativeLiveClientHostEvidenceAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5LiveClientHostEvidenceAcceptanceSmoke::capture);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        Map<String, Object> rejectedHeadlessOnly = object(smoke.get("rejectedHeadlessOnly"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live-client host evidence acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live-client host evidence acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveClientHostEvidenceAcceptanceSmoke".equals(
                        smoke.get("liveClientHostEvidenceAcceptanceSmokeClass")),
                "native live-client host evidence acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native live-client host evidence acceptance smoke must accept a real attached host");
        require("live_client_host_evidence:accepted:EchoNativeDashboardScreen".equals(accepted.get("effect")),
                "native live-client host evidence acceptance smoke must preserve the accepted effect");
        boolean acceptedRequiresLiveHostEvidence = Boolean.TRUE.equals(accepted.get("clientUiHostAttached"))
                        && Boolean.TRUE.equals(accepted.get("clientThreadAccepted"))
                        && Boolean.TRUE.equals(accepted.get("liveWindowHandlePresent"))
                        && Boolean.TRUE.equals(accepted.get("physicalHotkeyPollingReady"))
                        && Boolean.TRUE.equals(accepted.get("surfaceRouteSmokePassed"))
                        && Boolean.TRUE.equals(accepted.get("textInputSmokePassed"))
                        && Boolean.TRUE.equals(accepted.get("hudRouteSmokePassed"))
                        && Boolean.TRUE.equals(accepted.get("moduleCatalogSmokePassed"))
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
                        && Boolean.TRUE.equals(accepted.get("actualLivePhase5Accepted"));
        require(acceptedRequiresLiveHostEvidence,
                "native live-client host evidence acceptance smoke must require live client input and rendered surface evidence");
        require(Boolean.FALSE.equals(rejectedHeadlessOnly.get("accepted"))
                        && Boolean.TRUE.equals(rejectedHeadlessOnly.get("headlessOnly"))
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
                        && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveWindowFocusAccepted"))
                        && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveRenderCallbackAccepted"))
                        && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveScreenOwnershipAccepted"))
                        && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLivePhysicalPollLoopAccepted"))
                        && Boolean.FALSE.equals(rejectedHeadlessOnly.get(
                        "actualLivePhysicalEventTranscriptAccepted"))
                        && Boolean.FALSE.equals(rejectedHeadlessOnly.get(
                        "actualLivePhysicalRouteEffectTranscriptAccepted"))
                        && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLivePhysicalInputCoverageAccepted"))
                        && Boolean.FALSE.equals(rejectedHeadlessOnly.get("actualLiveSurfaceAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalInput")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRenderedSurface")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoScreenClass")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLiveInteraction")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLiveInteraction"))
                        .get("actualSurfaceRouteAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLiveSurfaceAcceptance")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLiveSurfaceAcceptance"))
                        .get("actualLiveSurfaceAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoWindowFocus")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoWindowFocus"))
                        .get("actualLiveWindowFocusAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRenderCallback")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRenderCallback"))
                        .get("actualLiveRenderCallbackAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoScreenOwnership")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoScreenOwnership"))
                        .get("actualLiveScreenOwnershipAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalPollLoop")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalPollLoop"))
                        .get("actualLivePhysicalPollLoopAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalEventTranscript")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalEventTranscript"))
                        .get("actualLivePhysicalEventTranscriptAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalRouteEffectTranscript"))
                        .get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalRouteEffectTranscript"))
                        .get("actualLivePhysicalRouteEffectTranscriptAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalInputCoverage")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalInputCoverage"))
                        .get("actualLivePhysicalInputCoverageAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHudOverlayInteraction")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHudOverlayInteraction"))
                        .get("actualHudOverlayRouteAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoTextInputInteraction")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoTextInputInteraction"))
                        .get("actualTextInputAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoTextInputAcceptance")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoTextInputAcceptance"))
                        .get("actualLiveTextInputAcceptanceAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoTextInputCoverage")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoTextInputCoverage"))
                        .get("actualLiveTextInputCoverageAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRouteBoundTextCommand")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRouteBoundTextCommand"))
                        .get("actualLiveRouteBoundTextCommandAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRouteBoundLensScan")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRouteBoundLensScan"))
                        .get("actualLiveRouteBoundLensScanAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRouteBoundHudUpdate")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRouteBoundHudUpdate"))
                        .get("actualLiveRouteBoundHudUpdateAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRouteBoundHoloMapWiki")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRouteBoundHoloMapWiki"))
                        .get("actualLiveRouteBoundHoloMapWikiAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoUiProbe")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoUiProbe"))
                        .get("actualLiveClientUiProbeAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoGeneratedInteractionProbe")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoGeneratedInteractionProbe"))
                        .get("actualLiveClientInteractionProbeAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoNotificationQueue")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoNotificationQueue"))
                        .get("actualLiveNotificationQueueAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMissionObjective")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMissionObjective"))
                        .get("actualLiveMissionObjectiveAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoCoreTools")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoCoreTools"))
                        .get("actualLiveCoreToolsAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoSystemFlow")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoSystemFlow"))
                        .get("actualLiveSystemFlowAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoInputFocusRouting")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoInputFocusRouting"))
                        .get("actualLiveInputFocusRoutingAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoScreenStackStability")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoScreenStackStability"))
                        .get("actualLiveScreenStackStabilityAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoVisualFrame")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoVisualFrame"))
                        .get("actualLiveVisualFrameAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoModuleSurfaceCatalog")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoModuleSurfaceCatalog"))
                        .get("actualLiveModuleSurfaceCatalogAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoTerminalEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoTerminalEndToEnd"))
                        .get("actualLiveTerminalEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoIndexEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoIndexEndToEnd"))
                        .get("actualLiveIndexEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLensEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLensEndToEnd"))
                        .get("actualLiveLensEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHudOverlayEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHudOverlayEndToEnd"))
                        .get("actualLiveHudOverlayEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHoloMapEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHoloMapEndToEnd"))
                        .get("actualLiveHoloMapEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoWikiEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoWikiEndToEnd"))
                        .get("actualLiveWikiEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMainMenuEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMainMenuEndToEnd"))
                        .get("actualLiveMainMenuEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMissionLogEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMissionLogEndToEnd"))
                        .get("actualLiveMissionLogEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoSettingsEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoSettingsEndToEnd"))
                        .get("actualLiveSettingsEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPauseEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPauseEndToEnd"))
                        .get("actualLivePauseEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRecoveryEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRecoveryEndToEnd"))
                        .get("actualLiveRecoveryEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoNotificationEndToEnd")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoNotificationEndToEnd"))
                        .get("actualLiveNotificationEndToEndAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLivePhase5")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLivePhase5"))
                        .get("actualLivePhase5Accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMainMenuOverride")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMainMenuOverride"))
                        .get("actualLiveMainMenuOverrideAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHoloMapWikiNavigation")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHoloMapWikiNavigation"))
                        .get("actualLiveHoloMapWikiNavigationAccepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhase5RouteSequence")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhase5RouteSequence"))
                        .get("actualLiveClientPhase5RouteSequenceAccepted")),
                "native live-client host evidence acceptance smoke must reject headless-only or incomplete evidence");
    }

    private static void requireNativeHeadlessUiBridgeReadinessAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5HeadlessUiBridgeReadinessAcceptanceSmoke.capture();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        Map<String, Object> rejectedLiveAttached = object(smoke.get("rejectedLiveAttached"));
        Map<String, Object> rejectedNoTerminal = object(smoke.get("rejectedNoTerminal"));
        Map<String, Object> rejectedNoHotkeys = object(smoke.get("rejectedNoHotkeys"));
        Map<String, Object> rejectedScreenMismatch = object(smoke.get("rejectedScreenMismatch"));
        Map<String, Object> rejectedLiveHostOverclaim = object(smoke.get("rejectedLiveHostOverclaim"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native headless UI bridge readiness smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native headless UI bridge readiness smoke must pass behavior checks");
        require("EchoNativeAgent5HeadlessUiBridgeReadinessAcceptanceSmoke".equals(
                        smoke.get("headlessUiBridgeReadinessAcceptanceSmokeClass")),
                "native headless UI bridge readiness smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native headless UI bridge readiness smoke must accept executed headless UI host evidence");
        require("headless_ui_bridge_readiness:accepted:EchoNativeDashboardScreen".equals(accepted.get("effect")),
                "native headless UI bridge readiness smoke must preserve the accepted effect");
        require(Boolean.TRUE.equals(accepted.get("fallbackHostAttached"))
                        && Boolean.TRUE.equals(accepted.get("headlessUiHostAttached"))
                        && Boolean.FALSE.equals(accepted.get("clientUiHostAttached"))
                        && Boolean.FALSE.equals(accepted.get("clientThreadAccepted"))
                        && Boolean.TRUE.equals(accepted.get("readyFlagsPresent"))
                        && Boolean.TRUE.equals(accepted.get("hotkeysReady"))
                        && Boolean.TRUE.equals(accepted.get("screenIdsReady"))
                        && Boolean.TRUE.equals(accepted.get("acceptedEvidenceReady"))
                        && Boolean.TRUE.equals(accepted.get("smokeEvidenceReady"))
                        && Boolean.TRUE.equals(accepted.get("liveHostRejectedHonesty"))
                        && Boolean.FALSE.equals(accepted.get("minecraftRuntimeAccessed")),
                "native headless UI bridge readiness must prove executed fallback host evidence without live Minecraft access");
        require(Boolean.FALSE.equals(rejectedLiveAttached.get("accepted"))
                        && Boolean.FALSE.equals(rejectedLiveAttached.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedNoTerminal.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoTerminal.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedNoHotkeys.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoHotkeys.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedScreenMismatch.get("accepted"))
                        && Boolean.FALSE.equals(rejectedScreenMismatch.get("serviceCodeExecuted"))
                        && Boolean.FALSE.equals(rejectedLiveHostOverclaim.get("accepted"))
                        && Boolean.FALSE.equals(rejectedLiveHostOverclaim.get("serviceCodeExecuted")),
                "native headless UI bridge readiness smoke must reject incomplete or overclaimed evidence without service execution");
    }

    private static void requireNativeAdapterCoreRuntimeBridgeGuardAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5AdapterCoreRuntimeBridgeGuardAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        Map<String, Object> rejectedNoRuntimeBridge = object(smoke.get("rejectedNoRuntimeBridge"));
        Map<String, Object> rejectedNoHostEvidence = object(smoke.get("rejectedNoHostEvidence"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native AdapterCore runtime bridge guard smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native AdapterCore runtime bridge guard smoke must pass behavior checks");
        require("EchoNativeAgent5AdapterCoreRuntimeBridgeGuardAcceptance".equals(
                        smoke.get("adapterCoreRuntimeBridgeGuardAcceptanceSmokeClass")),
                "native AdapterCore runtime bridge guard smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && Boolean.TRUE.equals(accepted.get("adapterCoreRuntimeBridgeActive"))
                        && Boolean.TRUE.equals(accepted.get("liveClientHostEvidenceAccepted"))
                        && "adaptercore_runtime_bridge_guard:accepted:agent5_ui".equals(accepted.get("effect")),
                "native AdapterCore runtime bridge guard must accept active runtime bridge plus host evidence");
        require(Boolean.FALSE.equals(rejectedNoRuntimeBridge.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoRuntimeBridge.get("adapterCoreRuntimeBridgeActive"))
                        && Boolean.TRUE.equals(rejectedNoRuntimeBridge.get("liveClientHostEvidenceAccepted"))
                        && "adaptercore_runtime_bridge_inactive".equals(rejectedNoRuntimeBridge.get("rejection")),
                "native AdapterCore runtime bridge guard must reject inactive runtime bridge");
        require(Boolean.FALSE.equals(rejectedNoHostEvidence.get("accepted"))
                        && Boolean.TRUE.equals(rejectedNoHostEvidence.get("adapterCoreRuntimeBridgeActive"))
                        && Boolean.FALSE.equals(rejectedNoHostEvidence.get("liveClientHostEvidenceAccepted"))
                        && "live_client_host_evidence_not_accepted".equals(rejectedNoHostEvidence.get("rejection")),
                "native AdapterCore runtime bridge guard must reject missing live host evidence");
    }

    private static void requireNativeLiveClientUiProbeAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveClientUiProbeAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        Map<String, Object> rejectedNoLensOverlay = object(smoke.get("rejectedNoLensOverlay"));
        Map<String, Object> rejectedNoHudFrame = object(smoke.get("rejectedNoHudFrame"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live-client UI probe acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live-client UI probe acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveClientUiProbeAcceptance".equals(
                        smoke.get("liveClientUiProbeAcceptanceSmokeClass")),
                "native live-client UI probe acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native live-client UI probe acceptance smoke must accept every live client UI surface");
        require("live_client_ui_probe:accepted:11".equals(accepted.get("effect")),
                "native live-client UI probe acceptance smoke must preserve the accepted effect");
        require(Boolean.TRUE.equals(accepted.get("scheduled"))
                        && Boolean.TRUE.equals(accepted.get("executed"))
                        && Integer.valueOf(11).equals(number(accepted.get("routeCount")))
                        && list(accepted, "surfaces").equals(List.of(
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
                )),
                "native live-client UI probe acceptance smoke must require all screen and HUD surfaces");
        require(Boolean.FALSE.equals(rejectedNoLensOverlay.get("accepted"))
                        && Boolean.FALSE.equals(rejectedNoHudFrame.get("accepted")),
                "native live-client UI probe acceptance smoke must reject routes without visible Lens overlay or HUD frame evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNotExecuted")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedMissingSurface")).get("accepted")),
                "native live-client UI probe acceptance smoke must reject missing execution or missing surfaces");
    }

    private static void requireNativeLiveClientInteractionProbeAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveClientInteractionProbeAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live-client interaction probe acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live-client interaction probe acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveClientInteractionProbeAcceptance".equals(
                        smoke.get("liveClientInteractionProbeAcceptanceSmokeClass")),
                "native live-client interaction probe acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native live-client interaction probe acceptance smoke must accept every generated-screen interaction");
        require("live_client_interaction_probe:accepted:11".equals(accepted.get("effect")),
                "native live-client interaction probe acceptance smoke must preserve the accepted effect");
        require(Boolean.TRUE.equals(accepted.get("scheduled"))
                        && Boolean.TRUE.equals(accepted.get("executed"))
                        && Integer.valueOf(11).equals(number(accepted.get("routeCount")))
                        && list(accepted, "surfaces").equals(List.of(
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
                ))
                        && list(accepted, "interactions").equals(List.of(
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
                )),
                "native live-client interaction probe acceptance smoke must require all generated-screen actions");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNotExecuted")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedWrongInteraction")).get("accepted")),
                "native live-client interaction probe acceptance smoke must reject missing execution or wrong interactions");
    }

    private static void requireNativeLiveClientPhase5RouteSequenceAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        String routeSequenceSource = readSourceFile("EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance.java");
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live-client Phase 5 route-sequence acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live-client Phase 5 route-sequence acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance".equals(
                        smoke.get("liveClientPhase5RouteSequenceAcceptanceSmokeClass")),
                "native live-client Phase 5 route-sequence acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native live-client Phase 5 route-sequence acceptance smoke must accept the ordered Phase 5 route sequence");
        require(("live_client_phase5_route_sequence:accepted:" + accepted.get("routeCount"))
                        .equals(accepted.get("effect")),
                "native live-client Phase 5 route-sequence acceptance smoke must preserve the accepted effect: " + accepted);
        require(Boolean.TRUE.equals(accepted.get("scheduled"))
                        && Boolean.TRUE.equals(accepted.get("executed"))
                        && number(accepted.get("routeCount")) == list(accepted, "requiredHotkeys").size()
                        && list(accepted, "surfaces").equals(list(accepted, "requiredSurfaces"))
                        && list(accepted, "routeTypes").equals(list(accepted, "requiredRouteTypes"))
                        && list(accepted, "hotkeys").equals(list(accepted, "requiredHotkeys"))
                        && Boolean.TRUE.equals(accepted.get("physicalPollerExecuted"))
                        && list(accepted, "physicalHotkeySurfaces").containsAll(List.of(
                        "TERMINAL", "INDEX", "LENS", "HOLOMAP", "SIGNALOS"))
                        && list(accepted, "physicalHotkeyEffects").containsAll(List.of(
                        "physical_hotkey_observed:M->TERMINAL:terminal.open",
                        "physical_hotkey_observed:G->INDEX:index.open",
                        "physical_hotkey_observed:LEFT_ALT->LENS:lens.scan",
                        "physical_hotkey_observed:J->HOLOMAP:holomap.open",
                        "physical_hotkey_observed:N->SIGNALOS:signalos.open"))
                        && Boolean.TRUE.equals(accepted.get("noScreenCrash")),
                "native live-client Phase 5 route-sequence acceptance smoke must require ordered declared routes and concrete route evidence");
        require(routeSequenceSource.contains("private static boolean runtimeMutationEvidence(Map<String, Object> route)")
                        && routeSequenceSource.contains("Boolean.TRUE.equals(route.get(\"adapterCoreMutation\"))")
                        && routeSequenceSource.contains("Boolean.TRUE.equals(route.get(\"runtimeHostMutated\"))")
                        && routeSequenceSource.contains("Boolean.TRUE.equals(route.get(\"saveTouched\"))")
                        && routeSequenceSource.contains("Boolean.TRUE.equals(route.get(\"feedbackEmitted\"))")
                        && routeSequenceSource.contains("Boolean.TRUE.equals(route.get(\"missionUpdated\"))")
                        && routeSequenceSource.contains("&& runtimeMutationEvidence(route)")
                        && routeSequenceSource.contains("row.put(\"runtimeHostMutated\", true);")
                        && routeSequenceSource.contains("row.put(\"saveTouched\", true);")
                        && routeSequenceSource.contains("row.put(\"feedbackEmitted\", true);")
                        && routeSequenceSource.contains("row.put(\"missionUpdated\", true);"),
                "native live-client Phase 5 route-sequence acceptance must require AdapterCore runtime host mutation, save, feedback, and mission evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedWrongOrder")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedWrongRouteType")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedWrongHotkey")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhysicalHotkey")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedCrash")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedMissingHostMutation")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedMissingSave")).get("accepted")),
                "native live-client Phase 5 route-sequence acceptance smoke must reject wrong order, wrong route type, wrong hotkey, missing physical hotkey, crash, missing host mutation, and missing save evidence");
    }

    private static void requireNativeLivePhase5AcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LivePhase5Acceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live Phase 5 acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live Phase 5 acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LivePhase5Acceptance".equals(smoke.get("livePhase5AcceptanceSmokeClass")),
                "native live Phase 5 acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native live Phase 5 acceptance smoke must accept all live UI done requirements");
        require("live_phase5:accepted:9".equals(accepted.get("effect")),
                "native live Phase 5 acceptance smoke must preserve accepted effect");
        require(checklist(accepted, "checklist").equals(List.of(
                        "terminal_opens",
                        "terminal_command_executes",
                        "index_opens_and_searches",
                        "lens_scans_target",
                        "hud_updates_health_hazard_mission",
                        "holomap_opens",
                        "wiki_page_opens",
                        "custom_main_menu_appears",
                        "no_screen_crash"
                ))
                        && Boolean.TRUE.equals(accepted.get("uiProbeAccepted"))
                        && Boolean.TRUE.equals(accepted.get("interactionProbeAccepted"))
                        && Boolean.TRUE.equals(accepted.get("terminalTextAccepted"))
                        && Boolean.TRUE.equals(accepted.get("indexTextAccepted"))
                        && Boolean.TRUE.equals(accepted.get("hudOverlayAccepted"))
                        && Boolean.TRUE.equals(accepted.get("mainMenuOverrideAccepted"))
                        && Boolean.TRUE.equals(accepted.get("holomapWikiNavigationAccepted"))
                        && Boolean.TRUE.equals(accepted.get("phase5RouteSequenceAccepted"))
                        && Boolean.TRUE.equals(accepted.get("noScreenCrash")),
                "native live Phase 5 acceptance smoke must require concrete UI probe, interaction, text, HUD, and no-crash evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoUiProbe")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoInteractionProbe")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHudOverlay")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMainMenuOverride")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHoloMapWikiNavigation")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPhase5RouteSequence")).get("accepted")),
                "native live Phase 5 acceptance smoke must reject incomplete live proof");
    }

    private static void requireNativeLiveSurfaceRouteAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveSurfaceRouteAcceptanceSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live surface route acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live surface route acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveSurfaceRouteAcceptanceSmoke".equals(
                        smoke.get("liveSurfaceRouteAcceptanceSmokeClass")),
                "native live surface route acceptance smoke must identify its executable class");
        require(list(smoke, "routeSurfaces").equals(EchoNativeAgent5PhysicalRouteRequirements.phase5Routes()
                        .stream()
                        .map(EchoNativeAgent5PhysicalRouteRequirements.RouteSpec::surface)
                        .toList()),
                "native live surface route acceptance smoke must cover every non-HUD hotkey surface");
        for (Object route : rawList(smoke.get("acceptedRoutes"))) {
            Map<String, Object> acceptedRoute = object(route);
            require(Boolean.TRUE.equals(acceptedRoute.get("accepted")),
                    "native live surface route acceptance smoke must accept every route");
            require(String.valueOf(acceptedRoute.get("effect")).startsWith("live_surface_route:accepted:"),
                    "native live surface route acceptance smoke must record accepted route effects");
            require(Boolean.TRUE.equals(acceptedRoute.get("physicalHotkeyHandled"))
                            && Boolean.TRUE.equals(acceptedRoute.get("liveSurfaceAccepted"))
                            && Boolean.TRUE.equals(acceptedRoute.get("physicalInputAccepted"))
                            && Boolean.TRUE.equals(acceptedRoute.get("liveSurfaceRendered")),
                    "native live surface route acceptance smoke must verify the full hotkey->surface->render chain");
        }
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoHotkey")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoSurface")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native live surface route acceptance smoke must reject incomplete route states");
    }

    private static void requireNativeLiveTextInputAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveTextInputAcceptanceSmoke.capture();
        Map<String, Object> terminal = object(smoke.get("terminal"));
        Map<String, Object> index = object(smoke.get("index"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live text input acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live text input acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveTextInputAcceptanceSmoke".equals(
                        smoke.get("liveTextInputAcceptanceSmokeClass")),
                "native live text input acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(terminal.get("accepted"))
                        && ("live_text_input:accepted:TERMINAL:"
                                + EchoNativeAgent5UiExpectedValues.terminalCommand()).equals(terminal.get("effect"))
                        && EchoNativeAgent5UiExpectedValues.terminalCommand().equals(terminal.get("finalBuffer"))
                        && EchoNativeAgent5UiExpectedValues.terminalOutput().equals(terminal.get("output")),
                "native live text input acceptance smoke must accept Terminal typing, edit, submit, and render");
        require(Boolean.TRUE.equals(index.get("accepted"))
                        && ("live_text_input:accepted:INDEX:"
                                + EchoNativeAgent5UiExpectedValues.indexQuery()).equals(index.get("effect"))
                        && EchoNativeAgent5UiExpectedValues.indexQuery().equals(index.get("finalBuffer"))
                        && EchoNativeAgent5UiExpectedValues.indexSearchOutput().equals(index.get("output")),
                "native live text input acceptance smoke must accept Index typing, edit, submit, and render");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedUnfocused")).get("accepted")),
                "native live text input acceptance smoke must reject unfocused typing");
    }

    private static void requireNativeLiveHudOverlayRouteAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5LiveHudOverlayRouteAcceptanceSmoke::capture);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live HUD overlay route acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live HUD overlay route acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveHudOverlayRouteAcceptanceSmoke".equals(
                        smoke.get("liveHudOverlayRouteAcceptanceSmokeClass")),
                "native live HUD overlay route acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && ("live_hud_overlay_route:accepted:data_backed_hud:"
                        + EchoNativeAgent5UiExpectedValues.hudUpdatedHealth()).equals(accepted.get("effect"))
                        && "HUD".equals(accepted.get("destinationMode"))
                        && Boolean.TRUE.equals(accepted.get("overlayRendered"))
                        && Integer.valueOf(EchoNativeAgent5UiExpectedValues.hudUpdatedHealth())
                                .equals(number(accepted.get("hudHealth"))),
                "native live HUD overlay route acceptance smoke must accept the data-backed HUD update into overlay render");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRoute")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoOverlay")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoEndToEnd")).get("accepted")),
                "native live HUD overlay route acceptance smoke must reject incomplete HUD route states");
    }

    private static void requireNativeLiveMainMenuOverrideAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveMainMenuOverrideAcceptanceSmoke.capture();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live main-menu override acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live main-menu override acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveMainMenuOverrideAcceptanceSmoke".equals(
                        smoke.get("liveMainMenuOverrideAcceptanceSmokeClass")),
                "native live main-menu override acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_main_menu_override:accepted:MAIN_MENU:4".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("titleScreenDetected"))
                        && Boolean.TRUE.equals(accepted.get("overrideAttached"))
                        && Boolean.TRUE.equals(accepted.get("liveSurfaceAccepted"))
                        && "MAIN_MENU".equals(accepted.get("surface"))
                        && Integer.valueOf(4).equals(number(accepted.get("optionCount")))
                        && "SETTINGS".equals(accepted.get("settingsDestination"))
                        && Boolean.TRUE.equals(accepted.get("quitRequested")),
                "native live main-menu override acceptance smoke must accept title override, live MAIN_MENU surface, and option behavior");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoTitle")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoSurface")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoOptions")).get("accepted")),
                "native live main-menu override acceptance smoke must reject incomplete main-menu states");
    }

    private static void requireNativeLiveNotificationQueueAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveNotificationQueueAcceptanceSmoke.capture();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live notification queue acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live notification queue acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveNotificationQueueAcceptanceSmoke".equals(
                        smoke.get("liveNotificationQueueAcceptanceSmokeClass")),
                "native live notification queue acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_notification_queue:accepted:2->1:top_left_safe_area".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("queueDispatched"))
                        && "top_left_safe_area".equals(accepted.get("notificationAnchor"))
                        && Boolean.TRUE.equals(accepted.get("queueAccepted"))
                        && Boolean.TRUE.equals(accepted.get("hudAccepted"))
                        && Boolean.TRUE.equals(accepted.get("dismissAccepted"))
                        && Integer.valueOf(2).equals(number(accepted.get("sourceCount")))
                        && Integer.valueOf(2).equals(number(accepted.get("dispatchedCount")))
                        && String.valueOf(accepted.get("dismissedId")).startsWith("echoterminal:")
                        && list(accepted, "remainingMessages").equals(
                                EchoNativeAgent5UiExpectedValues.notificationMessages().size() <= 1
                                        ? List.of()
                                        : EchoNativeAgent5UiExpectedValues.notificationMessages().subList(
                                                1,
                                                EchoNativeAgent5UiExpectedValues.notificationMessages().size())),
                "native live notification queue acceptance smoke must accept dispatch, HUD anchor, and dismiss behavior");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoDispatch")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedWrongAnchor")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoEndToEnd")).get("accepted")),
                "native live notification queue acceptance smoke must reject incomplete notification states");
    }

    private static void requireNativeLiveHoloMapWikiNavigationAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5LiveHoloMapWikiNavigationAcceptanceSmoke::capture);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live HoloMap/Wiki navigation acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live HoloMap/Wiki navigation acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveHoloMapWikiNavigationAcceptanceSmoke".equals(
                        smoke.get("liveHoloMapWikiNavigationAcceptanceSmokeClass")),
                "native live HoloMap/Wiki navigation acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_holomap_wiki_navigation:accepted:J/MODULE_ROUTE".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("holomapAccepted"))
                        && Boolean.TRUE.equals(accepted.get("wikiAccepted"))
                        && "HOLOMAP".equals(accepted.get("holomapSurface"))
                        && "WIKI".equals(accepted.get("wikiSurface"))
                        && EchoNativeAgent5UiExpectedValues.holomap().get("layer").equals(accepted.get("layer"))
                        && EchoNativeAgent5UiExpectedValues.holomapMarker().equals(accepted.get("marker"))
                        && EchoNativeAgent5UiExpectedValues.wiki().get("guide").equals(accepted.get("guide"))
                        && EchoNativeAgent5UiExpectedValues.wiki().get("page").equals(accepted.get("page"))
                        && Boolean.TRUE.equals(accepted.get("holomapRuntimeMutationAccepted"))
                        && Boolean.TRUE.equals(accepted.get("wikiRuntimeMutationAccepted")),
                "native live HoloMap/Wiki navigation acceptance smoke must accept J map and module-route Wiki outputs with surface-open mutation proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoHoloMap")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoWiki")).get("accepted")),
                "native live HoloMap/Wiki navigation acceptance smoke must reject incomplete navigation states");
    }

    private static void requireNativeLiveSystemFlowAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5LiveSystemFlowAcceptanceSmoke::capture);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live system flow acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live system flow acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveSystemFlowAcceptanceSmoke".equals(
                        smoke.get("liveSystemFlowAcceptanceSmokeClass")),
                "native live system flow acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_system_flow:accepted:SETTINGS_ACTION/screen_escape/RECOVERY_ACTION".equals(
                                accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("settingsAccepted"))
                        && Boolean.TRUE.equals(accepted.get("pauseAccepted"))
                        && Boolean.TRUE.equals(accepted.get("recoveryAccepted"))
                        && "ashfall-accessible".equals(accepted.get("settingsProfile"))
                        && Double.valueOf(1.25D).equals(accepted.get("settingsHudScale"))
                        && Boolean.FALSE.equals(accepted.get("settingsSubtitles"))
                        && "LENS".equals(accepted.get("pauseResumeDestination"))
                        && "recovery:recover".equals(accepted.get("recoveryFocusPath"))
                        && Boolean.TRUE.equals(accepted.get("recoveryRuntimeMutationAccepted")),
                "native live system flow acceptance smoke must accept settings, pause, and recovery live chains with runtime mutation proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoSettings")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPause")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRecovery")).get("accepted")),
                "native live system flow acceptance smoke must reject incomplete system flow states");
    }

    private static void requireNativeLiveCoreToolsAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5LiveCoreToolsAcceptanceSmoke::capture);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live core tools acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live core tools acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveCoreToolsAcceptanceSmoke".equals(
                        smoke.get("liveCoreToolsAcceptanceSmokeClass")),
                "native live core tools acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_core_tools:accepted:M/G/LEFT_ALT".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("terminalAccepted"))
                        && Boolean.TRUE.equals(accepted.get("indexAccepted"))
                        && Boolean.TRUE.equals(accepted.get("lensAccepted"))
                        && EchoNativeAgent5UiExpectedValues.terminalCommand().equals(accepted.get("terminalCommand"))
                        && EchoNativeAgent5UiExpectedValues.indexQuery().equals(accepted.get("indexQuery"))
                        && EchoNativeAgent5UiExpectedValues.lensTarget().equals(accepted.get("lensTarget")),
                "native live core tools acceptance smoke must accept Terminal, Index, and Lens live chains");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoTerminal")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoIndex")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLens")).get("accepted")),
                "native live core tools acceptance smoke must reject incomplete tool chains");
    }

    private static void requireNativeLiveMissionObjectiveAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5LiveMissionObjectiveAcceptanceSmoke::capture);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live mission objective acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live mission objective acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveMissionObjectiveAcceptanceSmoke".equals(
                        smoke.get("liveMissionObjectiveAcceptanceSmokeClass")),
                "native live mission objective acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_mission_objective:accepted:MISSION_ACTION/HUD:secure_crash_outpost:UPDATED"
                                .equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("missionAccepted"))
                        && Boolean.TRUE.equals(accepted.get("hudAccepted"))
                        && "echoashfallprotocol:secure_crash_outpost".equals(accepted.get("missionId"))
                        && "UPDATED".equals(accepted.get("missionStatus"))
                        && Double.valueOf(0.5D).equals(accepted.get("missionProgress"))
                        && Integer.valueOf(EchoNativeAgent5UiExpectedValues.hudUpdatedHealth())
                                .equals(accepted.get("hudHealth"))
                        && EchoNativeAgent5UiExpectedValues.hud().get("hazard").equals(accepted.get("hudHazard"))
                        && EchoNativeAgent5UiExpectedValues.hud().get("mission").equals(accepted.get("hudMission")),
                "native live mission objective acceptance smoke must accept Mission Log update and HUD mission output");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoMission")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHud")).get("accepted")),
                "native live mission objective acceptance smoke must reject missing Mission Log or HUD state");
    }

    private static void requireNativeLiveInputFocusRoutingAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveInputFocusRoutingAcceptanceSmoke.capture();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live input focus routing acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live input focus routing acceptance smoke must pass behavior checks: " + smoke);
        require("EchoNativeAgent5LiveInputFocusRoutingAcceptanceSmoke".equals(
                        smoke.get("liveInputFocusRoutingAcceptanceSmokeClass")),
                "native live input focus routing acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_input_focus_routing:accepted:focus/text/mouse/list".equals(
                        accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("focusAccepted"))
                        && Boolean.TRUE.equals(accepted.get("editingAccepted"))
                        && Boolean.TRUE.equals(accepted.get("mouseAccepted"))
                        && Boolean.TRUE.equals(accepted.get("listAccepted"))
                        && EchoNativeAgent5UiExpectedValues.terminalCommand().equals(accepted.get("terminalBuffer"))
                        && EchoNativeAgent5UiExpectedValues.indexQuery().equals(accepted.get("indexBuffer")),
                "native live input focus routing acceptance smoke must accept focus, typing, mouse, and list routing");
        require(list(accepted, "selectedOptions").equals(List.of(
                        "New Run",
                        "Settings",
                        "Theme",
                        "Input Mode",
                        "Quit to Main Menu"
                )),
                "native live input focus routing acceptance smoke must preserve selected list outputs");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoFocus")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoEditing")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMouse")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoList")).get("accepted")),
                "native live input focus routing acceptance smoke must reject incomplete routing states");
    }

    private static void requireNativeLiveScreenStackStabilityAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveScreenStackStabilityAcceptanceSmoke.capture();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live screen stack stability acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live screen stack stability acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveScreenStackStabilityAcceptanceSmoke".equals(
                        smoke.get("liveScreenStackStabilityAcceptanceSmokeClass")),
                "native live screen stack stability acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_screen_stack_stability:accepted:10-surfaces:no-crash".equals(
                        accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("stackAccepted"))
                        && Boolean.TRUE.equals(accepted.get("lifecycleAccepted"))
                        && Boolean.TRUE.equals(accepted.get("interactionAccepted"))
                        && "MAIN_MENU".equals(accepted.get("finalCurrentMode"))
                        && Integer.valueOf(1).equals(accepted.get("finalStackSize"))
                        && "LENS".equals(accepted.get("resumeMode"))
                        && Integer.valueOf(10).equals(accepted.get("interactionStepCount")),
                "native live screen stack stability acceptance smoke must accept stack lifecycle and 10-surface interactions");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoStack")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLifecycle")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoInteraction")).get("accepted")),
                "native live screen stack stability acceptance smoke must reject incomplete stack/lifecycle/interaction states");
    }

    private static void requireNativeLiveVisualFrameAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveVisualFrameAcceptanceSmoke.capture();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live visual frame acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live visual frame acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveVisualFrameAcceptanceSmoke".equals(
                        smoke.get("liveVisualFrameAcceptanceSmokeClass")),
                "native live visual frame acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_visual_frame:accepted:theme/render/camera/hud".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("themeAccepted"))
                        && Boolean.TRUE.equals(accepted.get("layoutAccepted"))
                        && Boolean.TRUE.equals(accepted.get("cameraAccepted"))
                        && Boolean.TRUE.equals(accepted.get("hudAccepted"))
                        && "echo_native:loader_blue_console".equals(accepted.get("themeId"))
                        && Integer.valueOf(620).equals(accepted.get("desktopPanelW"))
                        && Integer.valueOf(300).equals(accepted.get("compactPanelW"))
                        && "over_shoulder".equals(accepted.get("cameraMode"))
                        && EchoNativeAgent5UiExpectedValues.terminal().get("title").equals(accepted.get("cinematicCue"))
                        && "echohudcore:hud".equals(accepted.get("overlayLayerId")),
                "native live visual frame acceptance smoke must accept theme, render layout, camera, cinematic, and HUD frame");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoTheme")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoLayout")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoCamera")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHud")).get("accepted")),
                "native live visual frame acceptance smoke must reject incomplete visual-frame states");
    }

    private static void requireNativeLiveModuleSurfaceCatalogAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveModuleSurfaceCatalogAcceptanceSmoke.capture();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live module surface catalog acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live module surface catalog acceptance smoke must pass behavior checks: " + smoke);
        require("EchoNativeAgent5LiveModuleSurfaceCatalogAcceptanceSmoke".equals(
                        smoke.get("liveModuleSurfaceCatalogAcceptanceSmokeClass")),
                "native live module surface catalog acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_module_surface_catalog:accepted:11-surfaces".equals(accepted.get("effect"))
                        && Integer.valueOf(11).equals(accepted.get("surfaceCount"))
                        && Boolean.TRUE.equals(accepted.get("terminalAccepted"))
                        && Boolean.TRUE.equals(accepted.get("indexAccepted"))
                        && Boolean.TRUE.equals(accepted.get("lensAccepted"))
                        && Boolean.TRUE.equals(accepted.get("holomapAccepted"))
                        && Boolean.TRUE.equals(accepted.get("wikiAccepted"))
                        && Boolean.TRUE.equals(accepted.get("missionAccepted"))
                        && Boolean.TRUE.equals(accepted.get("settingsAccepted"))
                        && Boolean.TRUE.equals(accepted.get("pauseAccepted"))
                        && Boolean.TRUE.equals(accepted.get("recoveryAccepted"))
                        && Boolean.TRUE.equals(accepted.get("mainMenuAccepted"))
                        && Boolean.TRUE.equals(accepted.get("hudAccepted")),
                "native live module surface catalog acceptance smoke must accept all module-owned surface renderers");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedMissingHud")).get("accepted")),
                "native live module surface catalog acceptance smoke must reject missing HUD surface");
    }

    private static void requireNativeLiveRenderCallbackAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveRenderCallbackAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live render callback acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live render callback acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveRenderCallbackAcceptance".equals(
                        smoke.get("liveRenderCallbackAcceptanceClass")),
                "native live render callback acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_render_callback:accepted:TERMINAL".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("callbackExecuted"))
                        && Integer.valueOf(1).equals(accepted.get("callbackCount"))
                        && Integer.valueOf(5).equals(accepted.get("lineCount"))
                        && Integer.valueOf(1280).equals(accepted.get("width"))
                        && Integer.valueOf(720).equals(accepted.get("height")),
                "native live render callback acceptance smoke must require an executed generated-screen render callback");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoSurface")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoCallback")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedWrongMode")).get("accepted")),
                "native live render callback acceptance smoke must reject missing surface, missing callback, and mismatched mode");
    }

    private static void requireNativeLiveScreenOwnershipAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveScreenOwnershipAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live screen ownership acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live screen ownership acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveScreenOwnershipAcceptance".equals(
                        smoke.get("liveScreenOwnershipAcceptanceClass")),
                "native live screen ownership acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_screen_ownership:accepted:TERMINAL".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("surfaceAccepted"))
                        && Boolean.TRUE.equals(accepted.get("currentScreenIsGeneratedInstance"))
                        && "TERMINAL".equals(accepted.get("currentMode"))
                        && "TERMINAL".equals(accepted.get("expectedMode")),
                "native live screen ownership acceptance smoke must require the exact generated screen instance");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoSurface")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedWrongInstance")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedWrongMode")).get("accepted")),
                "native live screen ownership acceptance smoke must reject missing surface, wrong instance, and mismatched mode");
    }

    private static void requireNativeLivePhysicalPollLoopAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LivePhysicalPollLoopAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live physical poll-loop acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live physical poll-loop acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LivePhysicalPollLoopAcceptance".equals(
                        smoke.get("livePhysicalPollLoopAcceptanceClass")),
                "native live physical poll-loop acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_physical_poll_loop:accepted:3".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("windowHandlePresent"))
                        && Boolean.TRUE.equals(accepted.get("focusChecked"))
                        && Integer.valueOf(3).equals(accepted.get("pollIterations"))
                        && Integer.valueOf(33).equals(accepted.get("keySamples"))
                        && Integer.valueOf(11).equals(accepted.get("hotkeyCount")),
                "native live physical poll-loop acceptance smoke must require repeated GLFW key sampling");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoWindow")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoFocusCheck")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedTooFewIterations")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedTooFewSamples")).get("accepted")),
                "native live physical poll-loop acceptance smoke must reject incomplete sampling");
    }

    private static void requireNativeLivePhysicalEventTranscriptAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LivePhysicalEventTranscriptAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        List<String> expectedKeys = EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageKeys();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live physical event transcript acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live physical event transcript acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LivePhysicalEventTranscriptAcceptance".equals(
                        smoke.get("livePhysicalEventTranscriptAcceptanceClass")),
                "native live physical event transcript acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && ("live_physical_event_transcript:accepted:" + expectedKeys.size())
                                .equals(accepted.get("effect"))
                        && Integer.valueOf(expectedKeys.size()).equals(accepted.get("eventCount"))
                        && Boolean.TRUE.equals(accepted.get("sequenceOrdered"))
                        && Boolean.TRUE.equals(accepted.get("pollMetricsPresent"))
                        && list(accepted, "observedKeys").equals(expectedKeys),
                "native live physical event transcript acceptance smoke must require ordered sampled input events: "
                        + accepted);
        require(Boolean.FALSE.equals(object(smoke.get("rejectedMissingSequence")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedUnordered")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoPollMetrics")).get("accepted")),
                "native live physical event transcript acceptance smoke must reject unordered or unsampled events");
    }

    private static void requireNativeLivePhysicalRouteEffectTranscriptAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        List<String> expectedKeys = EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageKeys();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live physical route-effect transcript acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live physical route-effect transcript acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance".equals(
                        smoke.get("livePhysicalRouteEffectTranscriptAcceptanceClass")),
                "native live physical route-effect transcript acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && ("live_physical_route_effect_transcript:accepted:" + expectedKeys.size())
                                .equals(accepted.get("effect"))
                        && Integer.valueOf(expectedKeys.size()).equals(accepted.get("eventCount"))
                        && list(accepted, "observedKeys").equals(expectedKeys),
                "native live physical route-effect transcript acceptance smoke must require sampled routed UI effects: "
                        + accepted);
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoSurfaceEffect")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoHudEffect")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoSampleMetrics")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedAdapterCoreOnly")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoSave")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoFeedback")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoMission")).get("accepted")),
                "native live physical route-effect transcript acceptance smoke must reject missing UI route, host mutation, save, feedback, and mission evidence");
    }

    private static void requireNativeLiveRouteBoundTextCommandAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveRouteBoundTextCommandAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live route-bound text command acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live route-bound text command acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveRouteBoundTextCommandAcceptance".equals(
                        smoke.get("liveRouteBoundTextCommandAcceptanceClass")),
                "native live route-bound text command acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_route_bound_text_command:accepted:terminal+index".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("terminalAccepted"))
                        && Boolean.TRUE.equals(accepted.get("indexAccepted"))
                        && Boolean.TRUE.equals(accepted.get("routeBound"))
                        && list(accepted, "observedKeys").containsAll(List.of("M", "G")),
                "native live route-bound text command acceptance smoke must bind Terminal/Index commands to routed screens");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoTerminal")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoIndex")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRoute")).get("accepted")),
                "native live route-bound text command acceptance smoke must reject missing command or route evidence");
    }

    private static void requireNativeLiveRouteBoundLensScanAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveRouteBoundLensScanAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live route-bound lens scan acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live route-bound lens scan acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveRouteBoundLensScanAcceptance".equals(
                        smoke.get("liveRouteBoundLensScanAcceptanceClass")),
                "native live route-bound lens scan acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_route_bound_lens_scan:accepted:LEFT_ALT->LENS".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("lensAccepted"))
                        && Boolean.TRUE.equals(accepted.get("routeBound"))
                        && list(accepted, "observedKeys").contains("LEFT_ALT")
                        && EchoNativeAgent5UiExpectedValues.lensTarget().equals(accepted.get("target")),
                "native live route-bound lens scan acceptance smoke must bind Lens scan to routed Left Alt evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoLens")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRoute")).get("accepted")),
                "native live route-bound lens scan acceptance smoke must reject missing Lens or route evidence");
    }

    private static void requireNativeLiveRouteBoundHudUpdateAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance::smoke);
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live route-bound HUD update acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live route-bound HUD update acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance".equals(
                        smoke.get("liveRouteBoundHudUpdateAcceptanceClass")),
                "native live route-bound HUD update acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_route_bound_hud_update:accepted:data_backed_hud".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("hudAccepted"))
                        && Boolean.TRUE.equals(accepted.get("routeBound"))
                        && list(accepted, "observedKeys").contains("HUD_DATA")
                        && Integer.valueOf(EchoNativeAgent5UiExpectedValues.hudUpdatedHealth())
                                .equals(number(accepted.get("hudHealth")))
                        && EchoNativeAgent5UiExpectedValues.hud().get("hazard").equals(accepted.get("hudHazard"))
                        && "over_shoulder".equals(accepted.get("cameraMode")),
                "native live route-bound HUD update acceptance smoke must bind HUD update to data-backed module evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoHud")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRoute")).get("accepted")),
                "native live route-bound HUD update acceptance smoke must reject missing HUD or route evidence");
    }

    private static void requireNativeLiveRouteBoundHoloMapWikiAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance.smoke();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live route-bound HoloMap/Wiki acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live route-bound HoloMap/Wiki acceptance smoke must pass behavior checks");
        require("EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance".equals(
                        smoke.get("liveRouteBoundHoloMapWikiAcceptanceClass")),
                "native live route-bound HoloMap/Wiki acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted"))
                        && "live_route_bound_holomap_wiki:accepted:J/MODULE_ROUTE".equals(accepted.get("effect"))
                        && Boolean.TRUE.equals(accepted.get("holomapAccepted"))
                        && Boolean.TRUE.equals(accepted.get("wikiAccepted"))
                        && Boolean.TRUE.equals(accepted.get("routeBound"))
                        && list(accepted, "observedKeys").contains("J")
                        && EchoNativeAgent5UiExpectedValues.holomap().get("layer").equals(accepted.get("layer"))
                        && EchoNativeAgent5UiExpectedValues.holomapMarker().equals(accepted.get("marker"))
                        && EchoNativeAgent5UiExpectedValues.wiki().get("guide").equals(accepted.get("guide"))
                        && EchoNativeAgent5UiExpectedValues.wiki().get("page").equals(accepted.get("page")),
                "native live route-bound HoloMap/Wiki acceptance smoke must bind J map and module-route Wiki navigation to route evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoHoloMap")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoWiki")).get("accepted"))
                        && Boolean.FALSE.equals(object(smoke.get("rejectedNoRoute")).get("accepted")),
                "native live route-bound HoloMap/Wiki acceptance smoke must reject missing navigation or route evidence");
    }

    private static void requireNativeSettingsAdjustmentSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5SettingsAdjustmentSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native settings adjustment smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native settings adjustment smoke must pass settings mutation behavior");
        require("EchoNativeAgent5SettingsAdjustmentSmoke".equals(smoke.get("settingsAdjustmentSmokeClass")),
                "native settings adjustment smoke must identify its executable class");
        require(list(smoke, "selectedOptions").equals(List.of("HUD Scale", "Subtitles")),
                "native settings adjustment smoke must select the adjusted settings rows");
        require(list(smoke, "effects").equals(List.of("settings:hud_scale", "settings:subtitles")),
                "native settings adjustment smoke must record settings effects");
        require(Double.valueOf(1.25D).equals(smoke.get("settingsHudScale")),
                "native settings adjustment smoke must apply HUD scale");
        require(Boolean.FALSE.equals(smoke.get("settingsSubtitles")),
                "native settings adjustment smoke must toggle subtitles off");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains("HUD scale: 1.25    Subtitles: disabled")),
                "native settings adjustment smoke must render adjusted settings");
    }

    private static void requireNativeSettingsEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5SettingsEndToEndAcceptanceSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native settings end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native settings end-to-end acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5SettingsEndToEndAcceptanceSmoke".equals(
                        smoke.get("settingsEndToEndAcceptanceSmokeClass")),
                "native settings end-to-end acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native settings end-to-end acceptance smoke must accept complete settings chain");
        require("settings_end_to_end:SETTINGS_ACTION->SETTINGS:ashfall-accessible:subtitles_off".equals(accepted.get("effect")),
                "native settings end-to-end acceptance smoke must record accepted settings effect");
        require(Boolean.TRUE.equals(accepted.get("physicalInputAccepted"))
                        && Boolean.TRUE.equals(accepted.get("renderAccepted"))
                        && Boolean.TRUE.equals(accepted.get("interactionAccepted"))
                        && Boolean.TRUE.equals(accepted.get("adjustmentAccepted"))
                        && Boolean.TRUE.equals(accepted.get("settingsRendered")),
                "native settings end-to-end acceptance smoke must accept every chain stage");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native settings end-to-end acceptance smoke must reject missing physical input");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native settings end-to-end acceptance smoke must reject render failure");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInteraction")).get("accepted")),
                "native settings end-to-end acceptance smoke must reject missing interaction proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoAdjustment")).get("accepted")),
                "native settings end-to-end acceptance smoke must reject missing adjustment proof");
    }

    private static void requireNativePauseOptionActivationSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5PauseOptionActivationSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native pause option activation smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native pause option activation smoke must pass option routing behavior");
        require("EchoNativeAgent5PauseOptionActivationSmoke".equals(smoke.get("pauseOptionActivationSmokeClass")),
                "native pause option activation smoke must identify its executable class");
        require(list(smoke, "selectedOptions").equals(List.of("Resume", "Settings", "Quit to Main Menu")),
                "native pause option activation smoke must cover pause options");
        require(list(smoke, "destinations").equals(List.of("LENS", "SETTINGS", "MAIN_MENU")),
                "native pause option activation smoke must route to expected destinations");
        require(list(smoke, "effects").equals(List.of("pause:resume", "pause:settings", "pause:main_menu")),
                "native pause option activation smoke must record pause effects");
        require(list(smoke, "renderedLines").stream().anyMatch(line -> line.contains("Selected: Settings")),
                "native pause option activation smoke must render selected pause option");
    }

    private static void requireNativePauseEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5PauseEndToEndAcceptanceSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native pause end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native pause end-to-end acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5PauseEndToEndAcceptanceSmoke".equals(
                        smoke.get("pauseEndToEndAcceptanceSmokeClass")),
                "native pause end-to-end acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native pause end-to-end acceptance smoke must accept complete pause chain");
        require("pause_end_to_end:screen_escape->PAUSE:LENS".equals(accepted.get("effect")),
                "native pause end-to-end acceptance smoke must record accepted resume effect");
        require(Boolean.TRUE.equals(accepted.get("screenRouteHandled"))
                        && Boolean.TRUE.equals(accepted.get("screenInputAccepted"))
                        && Boolean.FALSE.equals(accepted.get("physicalInputAccepted"))
                        && Boolean.TRUE.equals(accepted.get("renderAccepted"))
                        && Boolean.TRUE.equals(accepted.get("interactionAccepted"))
                        && Boolean.TRUE.equals(accepted.get("optionAccepted"))
                        && Boolean.TRUE.equals(accepted.get("pauseRendered")),
                "native pause end-to-end acceptance smoke must accept every chain stage");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native pause end-to-end acceptance smoke must reject missing screen input");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native pause end-to-end acceptance smoke must reject render failure");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInteraction")).get("accepted")),
                "native pause end-to-end acceptance smoke must reject missing interaction proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoOption")).get("accepted")),
                "native pause end-to-end acceptance smoke must reject missing option proof");
    }

    private static void requireNativeRecoveryEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5RecoveryEndToEndAcceptanceSmoke::capture);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native recovery end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native recovery end-to-end acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5RecoveryEndToEndAcceptanceSmoke".equals(
                        smoke.get("recoveryEndToEndAcceptanceSmokeClass")),
                "native recovery end-to-end acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native recovery end-to-end acceptance smoke must accept complete recovery chain");
        require("recovery_end_to_end:RECOVERY_ACTION->RECOVERY:RECOVERED".equals(accepted.get("effect")),
                "native recovery end-to-end acceptance smoke must record accepted recovery effect");
        require(Boolean.TRUE.equals(accepted.get("physicalInputAccepted"))
                        && Boolean.TRUE.equals(accepted.get("renderAccepted"))
                        && Boolean.TRUE.equals(accepted.get("interactionAccepted"))
                        && Boolean.TRUE.equals(accepted.get("recoveryRendered"))
                        && Boolean.TRUE.equals(accepted.get("runtimeMutationAccepted"))
                        && "player.inventory.grant".equals(accepted.get("runtimeActionId"))
                        && "echoashfallprotocol:portable_signal_scanner".equals(accepted.get("itemId")),
                "native recovery end-to-end acceptance smoke must accept every chain stage and runtime grant mutation");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native recovery end-to-end acceptance smoke must reject missing physical input");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native recovery end-to-end acceptance smoke must reject render failure");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInteraction")).get("accepted")),
                "native recovery end-to-end acceptance smoke must reject missing interaction proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoMutation")).get("accepted")),
                "native recovery end-to-end acceptance smoke must reject recovery proof without runtime mutation");
    }

    private static void requireNativeMissionLogUpdateSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5MissionLogUpdateSmoke::capture);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native mission log update smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native mission log update smoke must pass update behavior");
        require("EchoNativeAgent5MissionLogUpdateSmoke".equals(smoke.get("missionLogUpdateSmokeClass")),
                "native mission log update smoke must identify its executable class");
        require("echoashfallprotocol:secure_crash_outpost".equals(smoke.get("missionId")),
                "native mission log update smoke must update the active mission");
        require("UPDATED".equals(smoke.get("missionStatus")),
                "native mission log update smoke must render updated status");
        require(Double.valueOf(0.5D).equals(smoke.get("missionProgress")),
                "native mission log update smoke must advance mission progress");
        require("mission:update:echoashfallprotocol:secure_crash_outpost".equals(smoke.get("effect")),
                "native mission log update smoke must record update effect");
        require(Boolean.TRUE.equals(smoke.get("runtimeMutationAccepted"))
                        && "native.ui.mission_log_update".equals(smoke.get("runtimeActionId"))
                        && "mission.objective_completed".equals(smoke.get("eventName")),
                "native mission log update smoke must require canonical runtime mutation evidence");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains("Status: UPDATED    Progress: 50%")),
                "native mission log update smoke must render updated progress");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains("Update: Drop pod signal confirmed")),
                "native mission log update smoke must render update line");
    }

    private static void requireNativeMissionLogEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5MissionLogEndToEndAcceptanceSmoke::capture);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native mission log end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native mission log end-to-end acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5MissionLogEndToEndAcceptanceSmoke".equals(
                        smoke.get("missionLogEndToEndAcceptanceSmokeClass")),
                "native mission log end-to-end acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native mission log end-to-end acceptance smoke must accept complete mission log chain");
        require("mission_log_end_to_end:MISSION_ACTION->MISSION_LOG:secure_crash_outpost:UPDATED".equals(accepted.get("effect")),
                "native mission log end-to-end acceptance smoke must record accepted update effect");
        require(Boolean.TRUE.equals(accepted.get("physicalInputAccepted"))
                        && Boolean.TRUE.equals(accepted.get("renderAccepted"))
                        && Boolean.TRUE.equals(accepted.get("interactionAccepted"))
                        && Boolean.TRUE.equals(accepted.get("updateAccepted"))
                        && Boolean.TRUE.equals(accepted.get("missionLogRendered"))
                        && Boolean.TRUE.equals(accepted.get("runtimeMutationAccepted")),
                "native mission log end-to-end acceptance smoke must accept every chain stage");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native mission log end-to-end acceptance smoke must reject missing physical input");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native mission log end-to-end acceptance smoke must reject render failure");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInteraction")).get("accepted")),
                "native mission log end-to-end acceptance smoke must reject missing interaction proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoUpdate")).get("accepted")),
                "native mission log end-to-end acceptance smoke must reject missing update proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoMutation")).get("accepted")),
                "native mission log end-to-end acceptance smoke must reject update-only proof without runtime mutation");
    }

    private static void requireNativeMainMenuOptionActivationSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5MainMenuOptionActivationSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native main-menu option activation smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native main-menu option activation smoke must pass route behavior");
        require("EchoNativeAgent5MainMenuOptionActivationSmoke".equals(smoke.get("mainMenuOptionActivationSmokeClass")),
                "native main-menu option activation smoke must identify its executable class");
        require(list(smoke, "selectedOptions").equals(List.of("Continue", "New Run", "Settings", "Quit")),
                "native main-menu option activation smoke must cover main-menu options");
        require(list(smoke, "destinations").equals(List.of("WIKI", "WORLD_SETUP", "SETTINGS", "MAIN_MENU")),
                "native main-menu option activation smoke must route to expected destinations");
        require(list(smoke, "effects").equals(List.of(
                "main_menu:continue",
                "main_menu:new_run_world_setup",
                "main_menu:settings",
                "main_menu:quit_requested"
        )), "native main-menu option activation smoke must record effects");
        require(Boolean.TRUE.equals(smoke.get("quitRequested")),
                "native main-menu option activation smoke must expose quit request");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains("Action: Settings selected: opening Settings")),
                "native main-menu option activation smoke must render activated option output");
    }

    private static void requireNativeScreenLifecycleSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5ScreenLifecycleSmoke.capture(
                "dev.echo.nativeplatform.generated.EchoAgent5UiScreen",
                "ashfall",
                12,
                3,
                2,
                1
        );
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native screen lifecycle smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native screen lifecycle smoke must pass route/focus checks");
        require("EchoNativeAgent5ScreenLifecycleSmoke".equals(smoke.get("screenLifecycleSmokeClass")),
                "native screen lifecycle smoke must identify its executable class");
        require(list(smoke, "visitedModes").containsAll(List.of(
                "MAIN_MENU",
                "TERMINAL",
                "INDEX",
                "LENS",
                "PAUSE",
                "RECOVERY"
        )), "native screen lifecycle smoke must visit the Agent 5 screens");
        require(list(smoke, "routeEffects").containsAll(List.of(
                "route:terminal",
                "route:index",
                "route:lens",
                "route:escape",
                "system:recovery",
                "system:main_menu"
        )), "native screen lifecycle smoke must execute screen routes");
        require("LENS".equals(smoke.get("pausePreviousMode")),
                "native screen lifecycle smoke must preserve pause previous mode");
        require("LENS".equals(smoke.get("resumeMode")),
                "native screen lifecycle smoke must resume the lens screen");
        require("terminal:input".equals(smoke.get("terminalFocusPath")),
                "native screen lifecycle smoke must expose terminal focus");
        require("index:search".equals(smoke.get("indexFocusPath")),
                "native screen lifecycle smoke must expose index focus");
        require("lens:scan".equals(smoke.get("lensFocusPath")),
                "native screen lifecycle smoke must expose lens focus");
        require("recovery:recover".equals(smoke.get("recoveryFocusPath")),
                "native screen lifecycle smoke must expose recovery focus");
        require(list(smoke, "actionExecutedKeys").containsAll(List.of(
                "terminalCommandExecuted",
                "indexSearchExecuted",
                "lensScanExecuted",
                "recoveryActionExecuted"
        )), "native screen lifecycle smoke must execute screen actions");
        require(list(smoke, "actionOutputs").containsAll(List.of(
                EchoNativeAgent5UiExpectedValues.terminalOutput(),
                EchoNativeAgent5UiExpectedValues.indexSearchOutput(),
                "Status: RECOVERED    Health: 35"
        )) && list(smoke, "actionOutputs").stream()
                .anyMatch(output -> output.contains(EchoNativeAgent5UiExpectedValues.lensOutput())),
                "native screen lifecycle smoke action outputs must match reference");
        require(list(smoke, "actionSurfaceLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.terminalCommand()
                                + " -> " + EchoNativeAgent5UiExpectedValues.terminalOutput())),
                "native screen lifecycle smoke must render terminal action output");
        require(list(smoke, "actionSurfaceLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.indexQuery()
                                + " -> " + EchoNativeAgent5UiExpectedValues.indexSearchOutput())),
                "native screen lifecycle smoke must render index action output");
        require(list(smoke, "actionSurfaceLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.lensOutput())),
                "native screen lifecycle smoke must render lens action output");
        require(list(smoke, "actionSurfaceLines").stream()
                        .anyMatch(line -> line.contains("Status: RECOVERED")),
                "native screen lifecycle smoke must render recovery action output");
        require(list(smoke, "screenTitles").contains("ECHO NATIVE // PAUSE"),
                "native screen lifecycle smoke must render the pause title");
    }

    private static void requireNativeScreenStackExecutionSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5ScreenStackSmoke.capture(
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "ashfall",
                12,
                3,
                2,
                1
        );
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native screen stack smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native screen stack smoke must pass stack semantics");
        require("EchoNativeAgent5ScreenStackSmoke".equals(smoke.get("screenStackSmokeClass")),
                "native screen stack smoke must identify its executable class");
        require(list(smoke, "events").containsAll(List.of(
                "push:MAIN_MENU",
                "push:TERMINAL",
                "push:INDEX",
                "push:LENS",
                "push:PAUSE",
                "pop:PAUSE",
                "replace:SETTINGS",
                "replace:LENS",
                "push:RECOVERY",
                "pop:RECOVERY",
                "empty-pop"
        )), "native screen stack smoke must execute push/pop/replace/empty-pop events");
        require(list(smoke, "currentModes").containsAll(List.of(
                "MAIN_MENU",
                "TERMINAL",
                "INDEX",
                "LENS",
                "PAUSE",
                "SETTINGS",
                "RECOVERY"
        )), "native screen stack smoke must record routed current modes");
        require(list(smoke, "routeFocusPaths").containsAll(List.of(
                "terminal:input",
                "index:search",
                "lens:scan",
                "pause:resume:LENS",
                "recovery:recover"
        )), "native screen stack smoke must record route focus paths");
        require("LENS".equals(smoke.get("resumeMode")),
                "native screen stack smoke must resume Lens after Pause pop");
        require(Boolean.TRUE.equals(smoke.get("emptyPopSafe")),
                "native screen stack smoke must guard empty pop");
        require("MAIN_MENU".equals(smoke.get("finalCurrentMode")),
                "native screen stack smoke must restore main menu after empty-pop guard");
        require(Integer.valueOf(1).equals(number(smoke.get("finalStackSize"))),
                "native screen stack smoke must end with one fallback screen");
        require(list(smoke, "screenTitles").containsAll(List.of(
                "ECHO NATIVE // MAIN_MENU",
                "ECHO NATIVE // PAUSE",
                "ECHO NATIVE // RECOVERY"
        )), "native screen stack smoke must render stack current screens");
    }

    private static void requireNativeModuleSurfaceRenderersExecute() {
        requireNestedRendererClass("EchoNativeTerminalSurfaceRenderer");
        requireNestedRendererClass("EchoNativeIndexSurfaceRenderer");
        requireNestedRendererClass("EchoNativeLensSurfaceRenderer");
        requireNestedRendererClass("EchoNativeHolomapSurfaceRenderer");
        requireNestedRendererClass("EchoNativeWikiSurfaceRenderer");
        requireNestedRendererClass("EchoNativeMissionLogSurfaceRenderer");
        requireNestedRendererClass("EchoNativeSettingsSurfaceRenderer");
        requireNestedRendererClass("EchoNativePauseSurfaceRenderer");
        requireNestedRendererClass("EchoNativeRecoverySurfaceRenderer");
        requireNestedRendererClass("EchoNativeMainMenuSurfaceRenderer");
        requireNestedRendererClass("EchoNativeHudSurfaceRenderer");

        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        Map<String, Object> terminal = EchoNativeAgent5ModuleSurfaceRenderers.renderTerminal(Map.of(
                "focusedControl", "terminal:input",
                "mouseRouted", true,
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "terminalOutput", EchoNativeAgent5UiExpectedValues.terminalOutput(),
                "terminalCommandExecuted", true
        ), dataSources);
        require("EchoNativeTerminalSurfaceRenderer".equals(terminal.get("moduleRendererClass")),
                "native terminal module renderer must execute");
        require(list(terminal, "lines").contains(EchoNativeAgent5UiExpectedValues.terminalCommand()
                        + " -> " + EchoNativeAgent5UiExpectedValues.terminalOutput()),
                "native terminal module renderer must include terminal output");

        Map<String, Object> index = EchoNativeAgent5ModuleSurfaceRenderers.renderIndex(Map.of(
                "indexBuffer", EchoNativeAgent5UiExpectedValues.indexQuery(),
                "indexOutput", EchoNativeAgent5UiExpectedValues.indexSearchOutput(),
                "indexSearchExecuted", true
        ), dataSources);
        require("EchoNativeIndexSurfaceRenderer".equals(index.get("moduleRendererClass")),
                "native index module renderer must execute");
        require(list(index, "lines").stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.indexSearchOutput())),
                "native index module renderer must include index result");

        Map<String, Object> lens = EchoNativeAgent5ModuleSurfaceRenderers.renderLens(Map.of(
                "lensOutput", EchoNativeAgent5UiExpectedValues.lensOutput(),
                "lensScanExecuted", true
        ), dataSources);
        require("EchoNativeLensSurfaceRenderer".equals(lens.get("moduleRendererClass")),
                "native lens module renderer must execute");
        require(list(lens, "lines").stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.lensOutput())),
                "native lens module renderer must include lens scan result");

        Map<String, Object> holomap = EchoNativeAgent5ModuleSurfaceRenderers.renderHolomap(Map.of(), dataSources);
        require("EchoNativeHolomapSurfaceRenderer".equals(holomap.get("moduleRendererClass")),
                "native HoloMap module renderer must execute");
        require(list(holomap, "lines").stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.holomapMarker())),
                "native HoloMap module renderer must include marker output");

        Map<String, Object> wiki = EchoNativeAgent5ModuleSurfaceRenderers.renderWiki(Map.of(), dataSources);
        require("EchoNativeWikiSurfaceRenderer".equals(wiki.get("moduleRendererClass")),
                "native Wiki module renderer must execute");
        require(list(wiki, "lines").stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.wikiLink())),
                "native Wiki module renderer must include page link output");

        Map<String, Object> surface = EchoNativeAgent5UiHandlerRegistry.renderSurface("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "mouseRouted", true,
                "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "terminalOutput", EchoNativeAgent5UiExpectedValues.terminalOutput(),
                "terminalCommandExecuted", true
        ));
        require("EchoNativeTerminalSurfaceRenderer".equals(surface.get("moduleRendererClass")),
                "shared surface renderer must delegate terminal mode to the terminal module renderer");

        Map<String, Object> missionLog = EchoNativeAgent5ModuleSurfaceRenderers.renderMissionLog(Map.of(), dataSources);
        require("EchoNativeMissionLogSurfaceRenderer".equals(missionLog.get("moduleRendererClass")),
                "native mission log module renderer must execute");
        require(list(missionLog, "lines").stream().anyMatch(line -> line.contains("Anchor Pod Outpost")),
                "native mission log module renderer must include mission title");

        Map<String, Object> settings = EchoNativeAgent5ModuleSurfaceRenderers.renderSettings(Map.of(), dataSources);
        require("EchoNativeSettingsSurfaceRenderer".equals(settings.get("moduleRendererClass")),
                "native settings module renderer must execute");
        require(list(settings, "lines").stream().anyMatch(line -> line.contains("ashfall-agent5")),
                "native settings module renderer must include theme");

        Map<String, Object> pause = EchoNativeAgent5ModuleSurfaceRenderers.renderPause(Map.of("previousMode", "WIKI"), dataSources);
        require("EchoNativePauseSurfaceRenderer".equals(pause.get("moduleRendererClass")),
                "native pause module renderer must execute");
        require("pause:resume:WIKI".equals(pause.get("focusPath")),
                "native pause module renderer must preserve resume focus");

        Map<String, Object> recovery = EchoNativeAgent5ModuleSurfaceRenderers.renderRecovery(Map.of(
                "focusedControl", "recovery:recover",
                "mouseRouted", true
        ), dataSources);
        require("EchoNativeRecoverySurfaceRenderer".equals(recovery.get("moduleRendererClass")),
                "native recovery module renderer must execute");
        String recoveryPoint = String.valueOf(object(dataSources.get("deathRecovery")).get("recoveryPoint"));
        require(list(recovery, "lines").stream().anyMatch(line -> line.contains(recoveryPoint)),
                "native recovery module renderer must include recovery point");

        Map<String, Object> mainMenu = EchoNativeAgent5ModuleSurfaceRenderers.renderMainMenu(Map.of(), dataSources);
        require("EchoNativeMainMenuSurfaceRenderer".equals(mainMenu.get("moduleRendererClass")),
                "native main menu module renderer must execute");
        require(list(mainMenu, "lines").stream().anyMatch(line -> line.contains("Continue")),
                "native main menu module renderer must include options");

        Map<String, Object> hud = EchoNativeAgent5ModuleSurfaceRenderers.renderHud(Map.of(), dataSources);
        require("EchoNativeHudSurfaceRenderer".equals(hud.get("moduleRendererClass")),
                "native HUD module renderer must execute");
        require(list(hud, "lines").stream().anyMatch(line -> line.contains(
                        "Health " + EchoNativeAgent5UiExpectedValues.hud().get("health"))),
                "native HUD module renderer must include health");

        require("EchoNativeMissionLogSurfaceRenderer".equals(
                        EchoNativeAgent5UiHandlerRegistry.renderSurface("MISSION_LOG", Map.of()).get("moduleRendererClass")),
                "shared surface renderer must delegate mission log mode to the mission module renderer");
        require("EchoNativeSettingsSurfaceRenderer".equals(
                        EchoNativeAgent5UiHandlerRegistry.renderSurface("SETTINGS", Map.of()).get("moduleRendererClass")),
                "shared surface renderer must delegate settings mode to the settings module renderer");
        require("EchoNativePauseSurfaceRenderer".equals(
                        EchoNativeAgent5UiHandlerRegistry.renderSurface("PAUSE", Map.of("previousMode", "WIKI")).get("moduleRendererClass")),
                "shared surface renderer must delegate pause mode to the pause module renderer");
        require("EchoNativeRecoverySurfaceRenderer".equals(
                        EchoNativeAgent5UiHandlerRegistry.renderSurface("RECOVERY", Map.of()).get("moduleRendererClass")),
                "shared surface renderer must delegate recovery mode to the recovery module renderer");
        require("EchoNativeMainMenuSurfaceRenderer".equals(
                        EchoNativeAgent5UiHandlerRegistry.renderSurface("MAIN_MENU", Map.of()).get("moduleRendererClass")),
                "shared surface renderer must delegate main menu mode to the main menu module renderer");
        require("EchoNativeHudSurfaceRenderer".equals(
                        EchoNativeAgent5UiHandlerRegistry.renderSurface("HUD", Map.of()).get("moduleRendererClass")),
                "shared surface renderer must delegate HUD mode to the HUD module renderer");
    }

    private static void requireNativeUiHostSmokeSnapshotsExecute() {
        Map<String, Object> terminal = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "TERMINAL",
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "ashfall",
                12,
                3,
                2,
                1
        );
        require(Boolean.TRUE.equals(terminal.get("serviceCodeExecuted")),
                "native UI host smoke snapshot must execute service code");
        require("EchoNativeAgent5UiHostSmokeSnapshot".equals(terminal.get("snapshotClass")),
                "native UI host smoke snapshot must identify its executable class");
        require("TERMINAL".equals(terminal.get("surface")), "native UI host smoke snapshot must record surface");
        require("terminal:input".equals(terminal.get("focusPath")),
                "native UI host smoke snapshot must record focus path");
        require("EchoNativeTerminalSurfaceRenderer".equals(terminal.get("moduleRendererClass")),
                "native UI host smoke snapshot must record module renderer");
        require(EchoNativeAgent5UiHostSmokeSnapshot.strings(terminal, "surfaceLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.terminalOutput())),
                "native UI host smoke snapshot must include terminal output");
        require(EchoNativeAgent5UiHostSmokeSnapshot.strings(terminal, "headerLines").stream()
                        .anyMatch(line -> line.contains(
                                "Health " + EchoNativeAgent5UiExpectedValues.hud().get("health"))),
                "native UI host smoke snapshot must include HUD output");

        Map<String, Object> holomap = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "HOLOMAP",
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "ashfall",
                12,
                3,
                2,
                1
        );
        require("EchoNativeHolomapSurfaceRenderer".equals(holomap.get("moduleRendererClass")),
                "native UI host smoke snapshot must record HoloMap module renderer");
        require(EchoNativeAgent5UiHostSmokeSnapshot.strings(holomap, "surfaceLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.holomapMarker())),
                "native UI host smoke snapshot must include HoloMap marker output");
    }

    private static void requireNativeUiHostInteractionSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5UiHostInteractionSmoke.run(
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "ashfall",
                12,
                3,
                2,
                1
        );
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native UI host interaction smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native UI host interaction smoke must pass all steps");
        require("EchoNativeAgent5UiHostInteractionSmoke".equals(smoke.get("interactionSmokeClass")),
                "native UI host interaction smoke must identify its executable class");
        List<Map<String, Object>> steps = maps(smoke.get("steps"));
        require(steps.size() == 10, "native UI host interaction smoke must include ten interaction steps");
        require(steps.stream().anyMatch(step -> "terminal_command".equals(step.get("id"))
                        && "EchoNativeTerminalSurfaceRenderer".equals(step.get("moduleRendererClass"))),
                "native UI host interaction smoke must execute terminal command through terminal renderer");
        require(steps.stream().anyMatch(step -> "index_search".equals(step.get("id"))
                        && "EchoNativeIndexSurfaceRenderer".equals(step.get("moduleRendererClass"))),
                "native UI host interaction smoke must execute index search through index renderer");
        require(steps.stream().anyMatch(step -> "lens_scan".equals(step.get("id"))
                        && "EchoNativeLensSurfaceRenderer".equals(step.get("moduleRendererClass"))),
                "native UI host interaction smoke must execute lens scan through lens renderer");
        require(steps.stream().anyMatch(step -> "mission_log_open".equals(step.get("id"))
                        && "EchoNativeMissionLogSurfaceRenderer".equals(step.get("moduleRendererClass"))),
                "native UI host interaction smoke must open mission log through mission log renderer");
        require(steps.stream().anyMatch(step -> "settings_open".equals(step.get("id"))
                        && "EchoNativeSettingsSurfaceRenderer".equals(step.get("moduleRendererClass"))),
                "native UI host interaction smoke must open settings through settings renderer");
        require(steps.stream().anyMatch(step -> "pause_resume".equals(step.get("id"))
                        && "EchoNativePauseSurfaceRenderer".equals(step.get("moduleRendererClass"))
                        && "LENS".equals(step.get("resumeDestinationMode"))),
                "native UI host interaction smoke must open and resume pause flow through pause renderer");
        require(steps.stream().anyMatch(step -> "recovery_action".equals(step.get("id"))
                        && "EchoNativeRecoverySurfaceRenderer".equals(step.get("moduleRendererClass"))),
                "native UI host interaction smoke must execute recovery through recovery renderer");
        require(steps.stream().anyMatch(step -> "holomap_open".equals(step.get("id"))
                        && "EchoNativeHolomapSurfaceRenderer".equals(step.get("moduleRendererClass"))),
                "native UI host interaction smoke must open HoloMap through HoloMap renderer");
        require(steps.stream().anyMatch(step -> "wiki_open".equals(step.get("id"))
                        && "EchoNativeWikiSurfaceRenderer".equals(step.get("moduleRendererClass"))),
                "native UI host interaction smoke must open Wiki through Wiki renderer");
        require(steps.stream().anyMatch(step -> "main_menu_open".equals(step.get("id"))
                        && "EchoNativeMainMenuSurfaceRenderer".equals(step.get("moduleRendererClass"))),
                "native UI host interaction smoke must open main menu through main menu renderer");
    }

    private static void requireNativeMainMenuOverrideSmokeExecutes() {
        Map<String, Object> attached = EchoNativeAgent5MainMenuOverrideSmoke.capture(
                true,
                true,
                "",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "ashfall",
                12,
                3,
                2,
                1
        );
        require(Boolean.TRUE.equals(attached.get("serviceCodeExecuted")),
                "native main-menu override smoke must execute service code");
        require(Boolean.TRUE.equals(attached.get("passed")),
                "native main-menu override smoke must pass when title screen attaches");
        require(Boolean.TRUE.equals(attached.get("guardSatisfied")),
                "native main-menu override smoke must record guard satisfaction");
        require("guarded_title_screen_replacement".equals(attached.get("strategy")),
                "native main-menu override smoke must record guarded strategy");
        require("EchoNativeAgent5MainMenuOverrideSmoke".equals(attached.get("mainMenuOverrideSmokeClass")),
                "native main-menu override smoke must identify its executable class");
        require(String.valueOf(attached.get("screenTitle")).contains("MAIN_MENU"),
                "native main-menu override smoke must render main menu host model");
        require(EchoNativeAgent5UiHostSmokeSnapshot.strings(object(attached.get("snapshot")), "surfaceLines").stream()
                        .anyMatch(line -> line.contains("Main Menu: ECHO Ashfall Terminal boot routes")),
                "native main-menu override smoke must include main menu surface lines");

        Map<String, Object> skipped = EchoNativeAgent5MainMenuOverrideSmoke.capture(
                false,
                false,
                "current_screen_not_title:example",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "ashfall",
                12,
                3,
                2,
                1
        );
        require(Boolean.TRUE.equals(skipped.get("passed")),
                "native main-menu override smoke must pass guarded skip when title screen is not active");
        require("current_screen_not_title:example".equals(skipped.get("skipReason")),
                "native main-menu override smoke must record skip reason");
    }

    private static void requireNativeMainMenuEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5MainMenuEndToEndAcceptanceSmoke.capture();
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native main-menu end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native main-menu end-to-end acceptance smoke must pass accepted and rejected cases");
        require("EchoNativeAgent5MainMenuEndToEndAcceptanceSmoke".equals(
                        smoke.get("mainMenuEndToEndAcceptanceSmokeClass")),
                "native main-menu end-to-end acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native main-menu end-to-end acceptance must accept attached rendered menu plus options");
        require("main_menu_end_to_end:accepted:4".equals(accepted.get("effect")),
                "native main-menu end-to-end acceptance must report the accepted option chain");
        require("SETTINGS".equals(accepted.get("settingsDestination")),
                "native main-menu end-to-end acceptance must route Settings");
        require(Boolean.TRUE.equals(accepted.get("quitRequested")),
                "native main-menu end-to-end acceptance must include Quit Request handling");
        require(list(accepted, "selectedOptions").equals(List.of("Continue", "New Run", "Settings", "Quit")),
                "native main-menu end-to-end acceptance must include all reference options");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoOverride")).get("accepted")),
                "native main-menu end-to-end acceptance must reject missing title-screen override");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoOptions")).get("accepted")),
                "native main-menu end-to-end acceptance must reject missing options");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoQuit")).get("accepted")),
                "native main-menu end-to-end acceptance must reject missing Quit Request handling");
    }

    private static void requireNativeWorldSetupCreateAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5WorldSetupCreateAcceptanceSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native world setup create acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native world setup create acceptance smoke must pass owned world preparation checks");
        require("EchoNativeAgent5WorldSetupCreateAcceptanceSmoke".equals(
                        smoke.get("worldSetupCreateAcceptanceSmokeClass")),
                "native world setup create acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(smoke.get("productWorldMarkerWritten")),
                "native world setup create acceptance smoke must write the product world marker");
        require(Boolean.TRUE.equals(smoke.get("productWorldOpenDispatchWritten")),
                "native world setup create acceptance smoke must write the product world open dispatch marker");
        require(Boolean.TRUE.equals(smoke.get("stagedDatapackReady")),
                "native world setup create acceptance smoke must stage a valid product datapack");
        require(Boolean.TRUE.equals(smoke.get("nativeLoaderOwnedWorldPolicy"))
                        && Boolean.FALSE.equals(smoke.get("vanillaWorldCreationFallbackAllowed"))
                        && NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID.equals(smoke.get("forcedWorldPreset")),
                "native world setup create acceptance smoke must preserve Native Loader-owned world policy");
        Map<String, Object> route = object(smoke.get("route"));
        require(Boolean.TRUE.equals(route.get("worldSetupPrepared"))
                        && Boolean.TRUE.equals(route.get("nativeProductWorldOpenDispatchRecorded"))
                        && "CREATE_NEW".equals(route.get("worldSetupStartupAction"))
                        && "MISSION_LOG".equals(route.get("destinationMode")),
                "native world setup create route must carry create/open preparation evidence");
    }

    private static void requireNativeHudOverlaySmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5HudOverlaySmoke.capture(
                true,
                true,
                "hud:passive",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "ashfall",
                12,
                3,
                2,
                1
        );
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native HUD overlay smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native HUD overlay smoke must pass when host is attached and overlay renders");
        require("EchoNativeAgent5HudOverlaySmoke".equals(smoke.get("hudOverlaySmokeClass")),
                "native HUD overlay smoke must identify its executable class");
        require("echohudcore:hud".equals(smoke.get("overlayLayerId")),
                "native HUD overlay smoke must record the HUD layer id");
        require("hud:passive".equals(smoke.get("trigger")),
                "native HUD overlay smoke must record the passive HUD trigger");
        require(String.valueOf(smoke.get("overlayMessage")).contains(
                        "Health " + EchoNativeAgent5UiExpectedValues.hud().get("health")),
                "native HUD overlay smoke must include health in the overlay message");
        require(String.valueOf(smoke.get("overlayMessage")).contains(
                        String.valueOf(EchoNativeAgent5UiExpectedValues.hud().get("mission"))),
                "native HUD overlay smoke must include the active mission in the overlay message");
        require(list(smoke, "overlayLines").stream().anyMatch(line -> line.contains(
                        String.valueOf(EchoNativeAgent5UiExpectedValues.hud().get("hazard")))),
                "native HUD overlay smoke must include hazard text");
        require(list(smoke, "overlayLines").stream()
                        .anyMatch(line -> line.contains(String.join(" / ",
                                EchoNativeAgent5UiExpectedValues.notificationMessages()))),
                "native HUD overlay smoke must include notification text");
        require("top_left_safe_area".equals(smoke.get("notificationAnchor")),
                "native HUD overlay smoke must record the notification anchor");
        require(list(smoke, "hostHeaderLines").stream().anyMatch(line -> line.contains(
                        "HUD: Health " + EchoNativeAgent5UiExpectedValues.hud().get("health"))),
                "native HUD overlay smoke must be derived from the native host HUD header model");
    }

    private static void requireNativeHudOverlayEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5HudOverlayEndToEndAcceptanceSmoke::capture);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native HUD overlay end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native HUD overlay end-to-end acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5HudOverlayEndToEndAcceptanceSmoke".equals(
                        smoke.get("hudOverlayEndToEndAcceptanceSmokeClass")),
                "native HUD overlay end-to-end acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native HUD overlay end-to-end acceptance smoke must accept the HUD overlay chain");
        require(EchoNativeAgent5UiExpectedValues.hudOverlayEffect().equals(accepted.get("effect")),
                "native HUD overlay end-to-end acceptance smoke must record accepted HUD chain effect");
        require(Boolean.TRUE.equals(accepted.get("overlayRendered"))
                        && Integer.valueOf(EchoNativeAgent5UiExpectedValues.hudUpdatedHealth())
                                .equals(accepted.get("hudHealth"))
                        && "over_shoulder".equals(accepted.get("cameraMode"))
                        && Boolean.TRUE.equals(accepted.get("runtimeMutationAccepted")),
                "native HUD overlay end-to-end acceptance smoke must include overlay, HUD update, and camera evidence");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoOverlay")).get("accepted")),
                "native HUD overlay end-to-end acceptance smoke must reject missing overlay rendering");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoHudUpdate")).get("accepted")),
                "native HUD overlay end-to-end acceptance smoke must reject missing HUD update");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoCamera")).get("accepted")),
                "native HUD overlay end-to-end acceptance smoke must reject missing camera/cinematic update");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoMutation")).get("accepted")),
                "native HUD overlay end-to-end acceptance smoke must reject HUD update without runtime mutation");
    }

    private static void requireNativeHotkeyBridgeSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5HotkeyBridgeSmoke.capture(
                true,
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "ashfall",
                12,
                3,
                2,
                1
        );
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native hotkey bridge smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native hotkey bridge smoke must pass all hotkey routes");
        require("EchoNativeAgent5HotkeyBridgeSmoke".equals(smoke.get("hotkeyBridgeSmokeClass")),
                "native hotkey bridge smoke must identify its executable class");
        List<Map<String, Object>> steps = maps(smoke.get("steps"));
        require(steps.size() == EchoNativeAgent5PhysicalRouteRequirements.phase5Routes().size() + 1,
                "native hotkey bridge smoke must include every source-declared UI hotkey");
        require(steps.stream().anyMatch(step -> "M".equals(step.get("key"))
                        && "TERMINAL".equals(step.get("destinationMode"))
                        && Boolean.TRUE.equals(step.get("passed"))),
                "native hotkey bridge smoke must route M to Terminal");
        require(steps.stream().anyMatch(step -> "G".equals(step.get("key"))
                        && "INDEX".equals(step.get("destinationMode"))
                        && Boolean.TRUE.equals(step.get("passed"))),
                "native hotkey bridge smoke must route G to Index");
        require(steps.stream().anyMatch(step -> "B".equals(step.get("key"))
                        && "INDEX".equals(step.get("startingMode"))
                        && "INDEX".equals(step.get("destinationMode"))
                        && Boolean.TRUE.equals(step.get("passed"))),
                "native hotkey bridge smoke must route contextual B to Index bookmark");
        require(steps.stream().anyMatch(step -> "LEFT_ALT".equals(step.get("key"))
                        && "LENS".equals(step.get("destinationMode"))
                        && Boolean.TRUE.equals(step.get("passed"))),
                "native hotkey bridge smoke must route Left Alt to Lens");
        require(steps.stream().anyMatch(step -> "J".equals(step.get("key"))
                        && "HOLOMAP".equals(step.get("destinationMode"))
                        && Boolean.TRUE.equals(step.get("passed"))),
                "native hotkey bridge smoke must route J to HoloMap");
        require(steps.stream().anyMatch(step -> "N".equals(step.get("key"))
                        && "SIGNALOS".equals(step.get("destinationMode"))
                        && Boolean.TRUE.equals(step.get("passed"))),
                "native hotkey bridge smoke must route N to SignalOS");
        require(steps.stream().anyMatch(step -> "ESCAPE".equals(step.get("key"))
                        && "PAUSE".equals(step.get("destinationMode"))
                        && Boolean.TRUE.equals(step.get("passed"))),
                "native hotkey bridge smoke must route Esc to pause flow");
    }

    private static void requireNativeNotificationQueueSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5NotificationQueueSmoke.capture("ashfall", 12, 3, 2, 1);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native notification queue smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native notification queue smoke must pass queue lifecycle checks");
        require("EchoNativeAgent5NotificationQueueSmoke".equals(smoke.get("notificationQueueSmokeClass")),
                "native notification queue smoke must identify its executable class");
        require("echonotificationcore:queue".equals(smoke.get("queueId")),
                "native notification queue smoke must record queue id");
        require(Integer.valueOf(2).equals(number(smoke.get("sourceCount"))),
                "native notification queue smoke must include two source notifications");
        require(Integer.valueOf(2).equals(number(smoke.get("dispatchedCount"))),
                "native notification queue smoke must dispatch two notifications");
        require(Boolean.TRUE.equals(smoke.get("delivered")),
                "native notification queue smoke must mark notifications delivered");
        require(Boolean.TRUE.equals(smoke.get("anchored")),
                "native notification queue smoke must anchor notifications in the HUD safe area");
        require(list(smoke, "messages").equals(EchoNativeAgent5UiExpectedValues.notificationMessages()),
                "native notification queue smoke must preserve message order");
        require(list(smoke, "severities").equals(List.of("INFO", "INFO")),
                "native notification queue smoke must preserve severity order");
        require(list(smoke, "hostHeaderLines").stream()
                        .anyMatch(line -> line.contains(String.join(" / ",
                                EchoNativeAgent5UiExpectedValues.notificationMessages()))),
                "native notification queue smoke must surface notifications through the host model");
    }

    private static void requireNativeHudUpdateSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5HudUpdateSmoke::capture);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native HUD update smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native HUD update smoke must pass update behavior");
        require("EchoNativeAgent5HudUpdateSmoke".equals(smoke.get("hudUpdateSmokeClass")),
                "native HUD update smoke must identify its executable class");
        require(Integer.valueOf(EchoNativeAgent5UiExpectedValues.hudUpdatedHealth()).equals(smoke.get("hudHealth")),
                "native HUD update smoke must mutate health");
        require(EchoNativeAgent5UiExpectedValues.hud().get("hazard").equals(smoke.get("hudHazard")),
                "native HUD update smoke must mutate hazard");
        require("hud:update:health_hazard_mission".equals(smoke.get("effect")),
                "native HUD update smoke must record HUD update effect");
        require(Boolean.TRUE.equals(smoke.get("runtimeMutationAccepted"))
                        && "native.ui.hud_refresh".equals(smoke.get("runtimeActionId"))
                        && "client_tick".equals(smoke.get("eventName")),
                "native HUD update smoke must require canonical runtime mutation evidence");
        require(list(smoke, "surfaceLines").stream()
                        .anyMatch(line -> line.contains(
                                "HUD: Health " + EchoNativeAgent5UiExpectedValues.hudUpdatedHealth())),
                "native HUD update smoke must render updated HUD health");
        require(list(smoke, "hostSurfaceLines").stream()
                        .anyMatch(line -> line.contains("HUD refreshed: health "
                                + EchoNativeAgent5UiExpectedValues.hudUpdatedHealth() + " / "
                                + EchoNativeAgent5UiExpectedValues.hud().get("hazard"))),
                "native HUD update smoke must render updated host HUD surface");
    }

    private static void requireNativeCameraCinematicSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5CameraCinematicSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native camera/cinematic smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native camera/cinematic smoke must pass frame behavior");
        require("EchoNativeAgent5CameraCinematicSmoke".equals(smoke.get("cameraCinematicSmokeClass")),
                "native camera/cinematic smoke must identify its executable class");
        require("over_shoulder".equals(smoke.get("cameraMode")),
                "native camera/cinematic smoke must route camera mode");
        require(Integer.valueOf(72).equals(number(smoke.get("cameraFov"))),
                "native camera/cinematic smoke must route camera FOV");
        require(EchoNativeAgent5UiExpectedValues.terminal().get("title").equals(smoke.get("cinematicCue")),
                "native camera/cinematic smoke must route cinematic cue");
        require(Integer.valueOf(1).equals(number(smoke.get("cinematicFrame"))),
                "native camera/cinematic smoke must advance cinematic frame");
        require(Boolean.TRUE.equals(smoke.get("cinematicLetterbox")),
                "native camera/cinematic smoke must route letterbox state");
        require(("camera_cinematic:frame:" + EchoNativeAgent5UiExpectedValues.terminal().get("title"))
                        .equals(smoke.get("effect")),
                "native camera/cinematic smoke must record frame effect");
        require(list(smoke, "surfaceLines").stream()
                        .anyMatch(line -> line.contains("Camera over_shoulder frame 1 cue "
                                + EchoNativeAgent5UiExpectedValues.terminal().get("title"))),
                "native camera/cinematic smoke must render camera frame on HUD surface");
        require(list(smoke, "hostSurfaceLines").stream()
                        .anyMatch(line -> line.contains("Letterbox: active")),
                "native camera/cinematic smoke must render letterbox state through host surface");
    }

    private static void requireNativeRenderCoreLayoutSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5RenderCoreLayoutSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native rendercore layout smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native rendercore layout smoke must pass viewport layout behavior");
        require("EchoNativeAgent5RenderCoreLayoutSmoke".equals(smoke.get("renderCoreLayoutSmokeClass")),
                "native rendercore layout smoke must identify its executable class");
        require(Integer.valueOf(620).equals(number(smoke.get("desktopPanelW"))),
                "native rendercore layout smoke must clamp desktop panel width");
        require(Integer.valueOf(300).equals(number(smoke.get("compactPanelW"))),
                "native rendercore layout smoke must preserve compact panel minimum width");
        require(number(smoke.get("compactTextMaxWidth")).intValue() >= 80,
                "native rendercore layout smoke must preserve compact text clip width");
        require(!list(smoke, "layouts").isEmpty(),
                "native rendercore layout smoke must include layout samples");
    }

    private static void requireNativeHostEventTranscriptSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(() -> EchoNativeAgent5HostEventTranscriptSmoke.capture(
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "ashfall",
                12,
                3,
                2,
                1
        ));
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native host event transcript smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native host event transcript smoke must pass keypress-to-render behavior");
        require("EchoNativeAgent5HostEventTranscriptSmoke".equals(smoke.get("hostEventTranscriptSmokeClass")),
                "native host event transcript smoke must identify its executable class");
        require(list(smoke, "events").contains("key:M->TERMINAL"),
                "native host event transcript smoke must record terminal hotkey");
        require(list(smoke, "events").contains("text:terminal:status"),
                "native host event transcript smoke must record terminal typed input");
        require(list(smoke, "events").contains("text:index:ashfall"),
                "native host event transcript smoke must record index typed input");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.terminalOutput())),
                "native host event transcript smoke must render terminal output");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.indexSearchOutput())),
                "native host event transcript smoke must render index output");
        require(list(smoke, "renderedLines").stream()
                        .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.lensOutput())),
                "native host event transcript smoke must render lens output");
        require(list(smoke, "renderedLines").stream().anyMatch(line -> line.contains("HUD refreshed: health 93")),
                "native host event transcript smoke must render updated HUD output");
        require(Boolean.TRUE.equals(smoke.get("terminalRuntimeMutationAccepted"))
                        && Boolean.TRUE.equals(smoke.get("indexRuntimeMutationAccepted"))
                        && Boolean.TRUE.equals(smoke.get("lensRuntimeMutationAccepted")),
                "native host event transcript smoke must call AdapterCore mutation paths for terminal, Index, and Lens");
        requireAcceptedSelectedMutationEvidence(object(smoke.get("terminalMutation")), "terminal transcript");
        requireAcceptedSelectedMutationEvidence(object(smoke.get("indexMutation")), "index transcript");
        requireAcceptedSelectedMutationEvidence(object(smoke.get("lensMutation")), "lens transcript");
    }

    private static void requireNativePhysicalHotkeyPollingSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5PhysicalHotkeyPollingSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native physical hotkey polling smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native physical hotkey polling smoke must pass rising-edge behavior");
        require("EchoNativeAgent5PhysicalHotkeyPollingSmoke".equals(smoke.get("physicalHotkeyPollingSmokeClass")),
                "native physical hotkey polling smoke must identify its executable class");
        List<?> events = rawList(smoke.get("events"));
        for (Map.Entry<String, String> expected
                : EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageSurfacesByKey().entrySet()) {
            String effectPrefix = "physical_hotkey_observed:" + expected.getKey() + "->" + expected.getValue() + ":";
            require(events.stream().map(EchoNativeAgent5UiBridgeContractVerifier::object)
                            .anyMatch(event -> String.valueOf(event.get("effect")).startsWith(effectPrefix)
                                    && expected.getValue().equals(event.get("surface"))),
                    "native physical hotkey polling smoke must route " + expected.getKey()
                            + " to " + expected.getValue());
        }
        require(events.stream().map(EchoNativeAgent5UiBridgeContractVerifier::object)
                        .anyMatch(event -> Boolean.FALSE.equals(event.get("observed"))
                                && String.valueOf(event.getOrDefault("effect", "")).isBlank()),
                "native physical hotkey polling smoke must suppress held-key repeats");
    }

    private static void requireNativeLiveSurfaceAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveSurfaceAcceptanceSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live surface acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live surface acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5LiveSurfaceAcceptanceSmoke".equals(smoke.get("liveSurfaceAcceptanceSmokeClass")),
                "native live surface acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(object(smoke.get("accepted")).get("accepted")),
                "native live surface acceptance smoke must accept matching screen and mode");
        require("live_surface:accepted:TERMINAL".equals(object(smoke.get("accepted")).get("effect")),
                "native live surface acceptance smoke must record accepted effect");
        require(list(smoke, "routeSurfaces").equals(List.of(
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
                )),
                "native live surface acceptance smoke must cover every Agent 5 UI surface");
        for (Object route : rawList(smoke.get("acceptedRoutes"))) {
            Map<String, Object> acceptedRoute = object(route);
            require(Boolean.TRUE.equals(acceptedRoute.get("accepted")),
                    "native live surface route must accept matching screen and mode");
            require(String.valueOf(acceptedRoute.get("effect")).startsWith("live_surface:accepted:"),
                    "native live surface route must record accepted surface effect");
        }
        require(Boolean.FALSE.equals(object(smoke.get("rejectedMode")).get("accepted")),
                "native live surface acceptance smoke must reject mismatched mode");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedSetScreen")).get("accepted")),
                "native live surface acceptance smoke must reject failed setScreen invocation");
    }

    private static void requireNativePhysicalInputAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5PhysicalInputAcceptanceSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native physical input acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native physical input acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5PhysicalInputAcceptanceSmoke".equals(smoke.get("physicalInputAcceptanceSmokeClass")),
                "native physical input acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(object(smoke.get("accepted")).get("accepted")),
                "native physical input acceptance smoke must accept a matching physical key and live surface");
        require("physical_input_acceptance:M->TERMINAL".equals(object(smoke.get("accepted")).get("effect")),
                "native physical input acceptance smoke must record the accepted input-to-surface effect");
        require(list(smoke, "routeSurfaces").equals(EchoNativeAgent5PhysicalRouteRequirements
                        .physicalCoverageRoutes()
                        .stream()
                        .map(EchoNativeAgent5PhysicalRouteRequirements.RouteSpec::surface)
                        .toList()),
                "native physical input acceptance smoke must cover every advertised physical UI surface");
        for (Object route : rawList(smoke.get("acceptedRoutes"))) {
            Map<String, Object> acceptedRoute = object(route);
            require(Boolean.TRUE.equals(acceptedRoute.get("accepted")),
                    "native physical input route must accept matching key and live surface");
            require(String.valueOf(acceptedRoute.get("effect")).startsWith("physical_input_acceptance:"),
                    "native physical input route must record its key-to-surface effect");
        }
        require(Boolean.FALSE.equals(object(smoke.get("rejectedSurfaceMismatch")).get("accepted")),
                "native physical input acceptance smoke must reject mismatched surfaces");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoHotkey")).get("accepted")),
                "native physical input acceptance smoke must reject missing physical key events");
    }

    private static void requireNativeLiveSurfaceRenderAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5LiveSurfaceRenderAcceptanceSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native live surface render acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native live surface render acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5LiveSurfaceRenderAcceptanceSmoke".equals(
                        smoke.get("liveSurfaceRenderAcceptanceSmokeClass")),
                "native live surface render acceptance smoke must identify its executable class");
        require(Boolean.TRUE.equals(object(smoke.get("accepted")).get("accepted")),
                "native live surface render acceptance smoke must accept rendered terminal content");
        require("live_surface_render:accepted:TERMINAL".equals(object(smoke.get("accepted")).get("effect")),
                "native live surface render acceptance smoke must record accepted render effect");
        require(list(smoke, "routeSurfaces").equals(List.of(
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
                )),
                "native live surface render acceptance smoke must cover every Agent 5 rendered surface");
        for (Object route : rawList(smoke.get("acceptedRoutes"))) {
            Map<String, Object> acceptedRoute = object(route);
            require(Boolean.TRUE.equals(acceptedRoute.get("accepted")),
                    "native live surface render route must accept matching rendered surface");
            require(String.valueOf(acceptedRoute.get("effect")).startsWith("live_surface_render:accepted:"),
                    "native live surface render route must record accepted render effect");
            require(Integer.parseInt(String.valueOf(acceptedRoute.get("renderedLineCount"))) > 0,
                    "native live surface render route must include rendered surface lines");
        }
        require(Boolean.FALSE.equals(object(smoke.get("rejectedUnacceptedSurface")).get("accepted")),
                "native live surface render acceptance smoke must reject unaccepted setScreen");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedRenderedSurfaceMismatch")).get("accepted")),
                "native live surface render acceptance smoke must reject mismatched rendered surface");
    }

    private static void requireNativeUiHostInteractionStateAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5UiHostInteractionStateAcceptanceSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native UI host interaction state acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native UI host interaction state acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5UiHostInteractionStateAcceptanceSmoke".equals(
                        smoke.get("uiHostInteractionStateAcceptanceSmokeClass")),
                "native UI host interaction state acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native UI host interaction state acceptance smoke must accept ten interaction steps");
        require("ui_host_interaction_state:accepted:10".equals(accepted.get("effect")),
                "native UI host interaction state acceptance smoke must record accepted interaction effect");
        require(Boolean.TRUE.equals(accepted.get("terminalAccepted"))
                        && Boolean.TRUE.equals(accepted.get("indexAccepted"))
                        && Boolean.TRUE.equals(accepted.get("lensAccepted"))
                        && Boolean.TRUE.equals(accepted.get("holomapAccepted"))
                        && Boolean.TRUE.equals(accepted.get("wikiAccepted")),
                "native UI host interaction state acceptance smoke must accept key Agent 5 surfaces");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedMissingStep")).get("accepted")),
                "native UI host interaction state acceptance smoke must reject missing steps");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedFailedStep")).get("accepted")),
                "native UI host interaction state acceptance smoke must reject failed steps");
    }

    private static void requireNativeUiHostEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5UiHostEndToEndAcceptanceSmoke.capture();
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native UI host end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native UI host end-to-end acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5UiHostEndToEndAcceptanceSmoke".equals(
                        smoke.get("uiHostEndToEndAcceptanceSmokeClass")),
                "native UI host end-to-end acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native UI host end-to-end acceptance smoke must accept complete UI chain");
        require("ui_host_end_to_end:M->TERMINAL:10".equals(accepted.get("effect")),
                "native UI host end-to-end acceptance smoke must record accepted chain effect");
        require(Boolean.TRUE.equals(accepted.get("physicalInputAccepted"))
                        && Boolean.TRUE.equals(accepted.get("liveSurfaceAccepted"))
                        && Boolean.TRUE.equals(accepted.get("renderAccepted"))
                        && Boolean.TRUE.equals(accepted.get("interactionStateAccepted")),
                "native UI host end-to-end acceptance smoke must accept every chain stage");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native UI host end-to-end acceptance smoke must reject missing physical input acceptance");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedRender")).get("accepted")),
                "native UI host end-to-end acceptance smoke must reject render failure");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedInteraction")).get("accepted")),
                "native UI host end-to-end acceptance smoke must reject interaction failure");
    }

    private static void requireNativeHoloMapEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5HoloMapEndToEndAcceptanceSmoke::capture);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native HoloMap end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native HoloMap end-to-end acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5HoloMapEndToEndAcceptanceSmoke".equals(
                        smoke.get("holoMapEndToEndAcceptanceSmokeClass")),
                "native HoloMap end-to-end acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native HoloMap end-to-end acceptance smoke must accept complete HoloMap chain");
        require(("holomap_end_to_end:J->HOLOMAP:" + EchoNativeAgent5UiExpectedValues.holomapMarker())
                        .equals(accepted.get("effect")),
                "native HoloMap end-to-end acceptance smoke must record accepted route effect");
        require(Boolean.TRUE.equals(accepted.get("physicalInputAccepted"))
                        && Boolean.TRUE.equals(accepted.get("renderAccepted"))
                        && Boolean.TRUE.equals(accepted.get("interactionAccepted"))
                        && Boolean.TRUE.equals(accepted.get("holomapRendered"))
                        && Boolean.TRUE.equals(accepted.get("runtimeMutationAccepted"))
                        && "native.ui.surface_open".equals(accepted.get("runtimeActionId"))
                        && "native.ui.surface_open".equals(accepted.get("eventName"))
                        && "echoholomap:ashfall_map".equals(accepted.get("surfaceCanonicalId")),
                "native HoloMap end-to-end acceptance smoke must accept every chain stage and runtime surface-open mutation");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native HoloMap end-to-end acceptance smoke must reject missing physical input");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native HoloMap end-to-end acceptance smoke must reject render failure");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInteraction")).get("accepted")),
                "native HoloMap end-to-end acceptance smoke must reject missing interaction proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoMutation")).get("accepted")),
                "native HoloMap end-to-end acceptance smoke must reject rendered HoloMap proof without runtime mutation");
    }

    private static void requireNativeWikiEndToEndAcceptanceSmokeExecutes() {
        Map<String, Object> smoke = withMutatingSelectedRuntimeHost(
                EchoNativeAgent5WikiEndToEndAcceptanceSmoke::capture);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native Wiki end-to-end acceptance smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native Wiki end-to-end acceptance smoke must pass accepted/rejected checks");
        require("EchoNativeAgent5WikiEndToEndAcceptanceSmoke".equals(
                        smoke.get("wikiEndToEndAcceptanceSmokeClass")),
                "native Wiki end-to-end acceptance smoke must identify its executable class");
        Map<String, Object> accepted = object(smoke.get("accepted"));
        require(Boolean.TRUE.equals(accepted.get("accepted")),
                "native Wiki end-to-end acceptance smoke must accept complete Wiki chain");
        require("wiki_end_to_end:MODULE_ROUTE->WIKI:ashfall".equals(accepted.get("effect")),
                "native Wiki end-to-end acceptance smoke must record accepted page effect");
        require(Boolean.TRUE.equals(accepted.get("physicalInputAccepted"))
                        && Boolean.TRUE.equals(accepted.get("renderAccepted"))
                        && Boolean.TRUE.equals(accepted.get("interactionAccepted"))
                        && Boolean.TRUE.equals(accepted.get("wikiRendered"))
                        && Boolean.TRUE.equals(accepted.get("runtimeMutationAccepted"))
                        && "native.ui.surface_open".equals(accepted.get("runtimeActionId"))
                        && "native.ui.surface_open".equals(accepted.get("eventName"))
                        && "echowiki:ashfall".equals(accepted.get("surfaceCanonicalId")),
                "native Wiki end-to-end acceptance smoke must accept every chain stage and runtime surface-open mutation");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInput")).get("accepted")),
                "native Wiki end-to-end acceptance smoke must reject missing physical input");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoRender")).get("accepted")),
                "native Wiki end-to-end acceptance smoke must reject render failure");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoInteraction")).get("accepted")),
                "native Wiki end-to-end acceptance smoke must reject missing interaction proof");
        require(Boolean.FALSE.equals(object(smoke.get("rejectedNoMutation")).get("accepted")),
                "native Wiki end-to-end acceptance smoke must reject rendered Wiki proof without runtime mutation");
    }

    private static void requireNativeThemeApplicationSmokeExecutes() {
        Map<String, Object> smoke = EchoNativeAgent5ThemeApplicationSmoke.capture("ashfall", 12, 3, 2, 1);
        require(Boolean.TRUE.equals(smoke.get("serviceCodeExecuted")),
                "native theme application smoke must execute service code");
        require(Boolean.TRUE.equals(smoke.get("passed")),
                "native theme application smoke must pass theme application checks");
        require("EchoNativeAgent5ThemeApplicationSmoke".equals(smoke.get("themeApplicationSmokeClass")),
                "native theme application smoke must identify its executable class");
        require("echo_native:loader_blue_console".equals(smoke.get("nativeLoaderThemeId")),
                "native theme application smoke must record the native loader theme id");
        require("ashfall-accessible".equals(smoke.get("settingsProfile")),
                "native theme application smoke must record the settings profile");
        require("EchoNativeSettingsSurfaceRenderer".equals(smoke.get("settingsSurfaceRenderer")),
                "native theme application smoke must use the settings module renderer");
        require("EchoNativeTerminalSurfaceRenderer".equals(smoke.get("terminalSurfaceRenderer")),
                "native theme application smoke must use the terminal module renderer");
        require(list(smoke, "settingsSurfaceLines").stream().anyMatch(line -> line.contains("Theme: ashfall-agent5")),
                "native theme application smoke must render the theme on the settings surface");
        require(list(smoke, "terminalSurfaceLines").stream().anyMatch(line -> line.contains(
                        EchoNativeAgent5UiExpectedValues.terminalOutput())),
                "native theme application smoke must render the theme through terminal output");
        require(object(smoke.get("tokens")).get("terminal.prompt").equals("ASHFALL>"),
                "native theme application smoke must preserve Ashfall terminal prompt token");
        Map<String, Object> resolverScenarios = object(smoke.get("resolverScenarios"));
        require(Boolean.TRUE.equals(resolverScenarios.get("invalidThemeIdFallsBack")),
                "native theme resolver must fall back to built-in theme for invalid loader_default theme ids");
        require(Boolean.TRUE.equals(resolverScenarios.get("systemPropertiesOverrideProfile")),
                "native theme resolver system properties must override profile defaults");
        require(Boolean.TRUE.equals(resolverScenarios.get("loaderDefaultIgnoresThemeCoreResource")),
                "native theme resolver loader_default mode must ignore ThemeCore-compatible resource themes");
        require(Boolean.TRUE.equals(resolverScenarios.get("modpackUsesThemeCoreResource")),
                "native theme resolver modpack mode must use ThemeCore-compatible resource theme data");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static void requireNestedRendererClass(String simpleName) {
        String className = EchoNativeAgent5ModuleSurfaceRenderers.class.getName() + "$" + simpleName;
        try {
            Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("native module renderer class missing " + className, exception);
        }
    }

    private static void requireNativeUiHandlersExecute(
            Map<String, Object> terminalData,
            Map<String, Object> indexData,
            Map<String, Object> lensData,
            Map<String, Object> hudData,
            Map<String, Object> deathRecoveryData,
            Map<String, Object> holomapData,
            Map<String, Object> wikiData
    ) {
        Map<String, Object> terminal = EchoNativeAgent5UiHandlerRegistry.executeTerminal(
                String.valueOf(terminalData.get("command")));
        require(Boolean.TRUE.equals(terminal.get("handled")), "terminal native UI handler must execute");
        require(Boolean.TRUE.equals(terminal.get("serviceCodeExecuted")),
                "terminal native UI handler must mark service code executed");
        require(String.valueOf(terminalData.get("readyLine")).equals(terminal.get("output")),
                "terminal native UI handler output must match reference");

        Map<String, Object> index = EchoNativeAgent5UiHandlerRegistry.searchIndex(String.valueOf(indexData.get("query")));
        require(Boolean.TRUE.equals(index.get("handled")), "index native UI handler must execute");
        require(EchoNativeAgent5UiExpectedValues.indexSearchOutput().equals(index.get("output")),
                "index native UI handler output must match reference");

        Map<String, Object> lens = EchoNativeAgent5UiHandlerRegistry.scanLens(String.valueOf(lensData.get("target")));
        require(Boolean.TRUE.equals(lens.get("handled")), "lens native UI handler must execute");
        require(String.valueOf(lens.get("output")).contains(String.valueOf(lensData.get("result"))),
                "lens native UI handler output must match reference");

        Map<String, Object> hud = EchoNativeAgent5UiHandlerRegistry.renderHud();
        require(Boolean.TRUE.equals(hud.get("handled")), "HUD native UI handler must execute");
        require(String.valueOf(hud.get("output")).contains(String.valueOf(hudData.get("hazard"))),
                "HUD native UI handler output must include hazard reference");

        Map<String, Object> recovery = EchoNativeAgent5UiHandlerRegistry.recover();
        require(Boolean.TRUE.equals(recovery.get("handled")), "recovery native UI handler must execute");
        require(String.valueOf(recovery.get("output")).contains(String.valueOf(deathRecoveryData.get("status"))),
                "recovery native UI handler output must match reference");

        Map<String, Object> holomap = EchoNativeAgent5UiHandlerRegistry.openHolomap(
                String.valueOf(holomapData.get("layer")),
                String.valueOf(holomapData.get("marker"))
        );
        require(Boolean.TRUE.equals(holomap.get("handled")), "HoloMap native UI handler must execute");
        require(String.valueOf(holomap.get("output")).contains(String.valueOf(holomapData.get("focus"))),
                "HoloMap native UI handler output must match reference");

        Map<String, Object> wiki = EchoNativeAgent5UiHandlerRegistry.openWiki(
                String.valueOf(wikiData.get("guide")),
                String.valueOf(wikiData.get("page"))
        );
        require(Boolean.TRUE.equals(wiki.get("handled")), "Wiki native UI handler must execute");
        require(String.valueOf(wiki.get("output")).contains(String.valueOf(wikiData.get("link"))),
                "Wiki native UI handler output must match reference");
    }

    private static void requireGeneratedScreenCompiles(Map<String, Object> contract) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "generated screen smoke requires a system Java compiler");
        try {
            Path root = Files.createTempDirectory("echo-agent5-screen-smoke-");
            Path sourceRoot = root.resolve("src");
            Path classRoot = root.resolve("classes");
            Files.createDirectories(classRoot);
            writeSource(sourceRoot, "dev/echo/nativeplatform/generated/EchoNativeDashboardScreen.java",
                    EchoNativeLiveUiBridge.screenSource());
            writeSource(sourceRoot, minecraftSourcePath("client", "Minecraft"), javaSource(minecraftPackage("client"), """
                    import %s;
                    import %s;

                    public final class Minecraft {
                        public final Font font = new Font();
                        private static final Minecraft INSTANCE = new Minecraft();
                        private Screen screen;

                        public static Minecraft getInstance() {
                            return INSTANCE;
                        }

                        public Window getWindow() {
                            return new Window();
                        }

                        public void setScreen(Screen screen) {
                            this.screen = screen;
                        }

                        public Screen screen() {
                            return screen;
                        }

                        public static final class Window {
                            public int getGuiScaledWidth() {
                                return 640;
                            }
                        }
                    }
                    """.formatted(minecraftClass("client.gui", "Font"), minecraftClass("client.gui.screens", "Screen"))));
            writeSource(sourceRoot, minecraftSourcePath("client.gui", "Font"), javaSource(minecraftPackage("client.gui"), """
                    public final class Font {
                        public int width(String value) {
                            return value == null ? 0 : value.length();
                        }

                        public String plainSubstrByWidth(String value, int width) {
                            if (value == null || width <= 0) {
                                return "";
                            }
                            return value.length() <= width ? value : value.substring(0, width);
                        }
                    }
                    """));
            writeSource(sourceRoot, minecraftSourcePath("client.gui", "GuiGraphicsExtractor"),
                    javaSource(minecraftPackage("client.gui"), """
                    import java.util.ArrayList;
                    import java.util.List;

                    public final class GuiGraphicsExtractor {
                        private final ArrayList<String> textCalls = new ArrayList<>();

                        public void fill(int left, int top, int right, int bottom, int color) {
                        }

                        public void outline(int left, int top, int width, int height, int color) {
                        }

                        public void text(Font font, String value, int x, int y, int color, boolean shadow) {
                            textCalls.add(value);
                        }

                        public List<String> textCalls() {
                            return List.copyOf(textCalls);
                        }
                    }
                    """));
            writeSource(sourceRoot, minecraftSourcePath("client.gui.screens", "Screen"),
                    javaSource(minecraftPackage("client.gui.screens"), """
                    import %s;
                    import %s;
                    import %s;
                    import %s;
                    import %s;

                    public class Screen {
                        protected int width = 640;
                        protected int height = 360;

                        public Screen(Component title) {
                        }

                        public void tick() {
                        }

                        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                        }

                        public boolean keyPressed(KeyEvent event) {
                            return false;
                        }

                        public boolean charTyped(CharacterEvent event) {
                            return false;
                        }

                        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                            return false;
                        }
                    }
                    """.formatted(
                            minecraftClass("client.gui", "GuiGraphicsExtractor"),
                            minecraftClass("client.input", "KeyEvent"),
                            minecraftClass("client.input", "CharacterEvent"),
                            minecraftClass("client.input", "MouseButtonEvent"),
                            minecraftClass("network.chat", "Component"))));
            writeSource(sourceRoot, minecraftSourcePath("client.input", "KeyEvent"),
                    javaSource(minecraftPackage("client.input"), """
                    public final class KeyEvent {
                        private final int key;

                        public KeyEvent(int key) {
                            this.key = key;
                        }

                        public KeyEvent(int key, int scancode, int modifiers) {
                            this.key = key;
                        }

                        public int key() {
                            return key;
                        }
                    }
                    """));
            writeSource(sourceRoot, minecraftSourcePath("client.input", "CharacterEvent"),
                    javaSource(minecraftPackage("client.input"), """
                    public final class CharacterEvent {
                        private final int codepoint;

                        public CharacterEvent(int codepoint) {
                            this.codepoint = codepoint;
                        }

                        public int codepoint() {
                            return codepoint;
                        }

                        public String codepointAsString() {
                            return Character.toString(codepoint);
                        }

                        public boolean isAllowedChatCharacter() {
                            return true;
                        }
                    }
                    """));
            writeSource(sourceRoot, minecraftSourcePath("client.input", "MouseButtonInfo"),
                    javaSource(minecraftPackage("client.input"), """
                    public final class MouseButtonInfo {
                        private final int button;
                        private final int modifiers;

                        public MouseButtonInfo(int button, int modifiers) {
                            this.button = button;
                            this.modifiers = modifiers;
                        }

                        public int button() {
                            return button;
                        }

                        public int modifiers() {
                            return modifiers;
                        }
                    }
                    """));
            writeSource(sourceRoot, minecraftSourcePath("client.input", "MouseButtonEvent"),
                    javaSource(minecraftPackage("client.input"), """
                    public final class MouseButtonEvent {
                        private final double x;
                        private final double y;
                        private final MouseButtonInfo buttonInfo;

                        public MouseButtonEvent(double x, double y, MouseButtonInfo buttonInfo) {
                            this.x = x;
                            this.y = y;
                            this.buttonInfo = buttonInfo;
                        }

                        public double x() {
                            return x;
                        }

                        public double y() {
                            return y;
                        }

                        public int button() {
                            return buttonInfo.button();
                        }

                        public int modifiers() {
                            return buttonInfo.modifiers();
                        }
                    }
                    """));
            writeSource(sourceRoot, minecraftSourcePath("network.chat", "Component"),
                    javaSource(minecraftPackage("network.chat"), """
                    public final class Component {
                        public static Component literal(String value) {
                            return new Component();
                        }
                    }
                    """));
            writeSource(sourceRoot, "org/lwjgl/glfw/GLFW.java", """
                    package org.lwjgl.glfw;

                    public final class GLFW {
                        public static final int GLFW_KEY_ESCAPE = 256;
                        public static final int GLFW_KEY_ENTER = 257;
                        public static final int GLFW_KEY_BACKSPACE = 259;
                        public static final int GLFW_KEY_DOWN = 264;
                        public static final int GLFW_KEY_UP = 265;
                        public static final int GLFW_KEY_G = 71;
                        public static final int GLFW_KEY_J = 74;
                        public static final int GLFW_KEY_K = 75;
                        public static final int GLFW_KEY_M = 77;
                        public static final int GLFW_KEY_N = 78;
                        public static final int GLFW_KEY_R = 82;
                        public static final int GLFW_KEY_U = 85;
                        public static final int GLFW_KEY_B = 66;
                        public static final int GLFW_KEY_LEFT_ALT = 342;
                        public static final int GLFW_KEY_RIGHT_BRACKET = 93;
                        public static final int GLFW_KEY_LEFT_BRACKET = 91;
                        public static final int GLFW_KEY_BACKSLASH = 92;
                        public static final int GLFW_KEY_X = 88;
                        public static final int GLFW_KEY_C = 67;
                        public static final int GLFW_KEY_Y = 89;
                        public static final int GLFW_KEY_Z = 90;

                        private GLFW() {
                        }
                    }
                    """);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
                List<Path> sources;
                try (var stream = Files.walk(sourceRoot)) {
                    sources = stream
                            .filter(path -> path.toString().endsWith(".java"))
                            .toList();
                }
                Boolean ok = compiler.getTask(
                        null,
                        fileManager,
                        diagnostics,
                        List.of(
                                "-classpath", System.getProperty("java.class.path"),
                                "-d", classRoot.toString()
                        ),
                        null,
                        fileManager.getJavaFileObjectsFromPaths(sources)
                ).call();
                require(Boolean.TRUE.equals(ok),
                        "generated native UI screen must compile against client stubs: " + diagnostics(diagnostics));
            }
            requireGeneratedScreenExecutes(classRoot, contract);
        } catch (IOException exception) {
            throw new IllegalStateException("generated native UI screen smoke failed: " + exception.getMessage(), exception);
        }
    }

    private static void requireGeneratedScreenExecutes(Path classRoot, Map<String, Object> contract) {
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classRoot.toUri().toURL()},
                EchoNativeAgent5UiBridgeContractVerifier.class.getClassLoader())) {
            Class<?> screenClass = Class.forName(
                    "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                    true,
                    loader
            );
            Class<?> vanillaScreenClass = Class.forName(minecraftClass("client.gui.screens", "Screen"), true, loader);
            Class<?> graphicsClass = Class.forName(minecraftClass("client.gui", "GuiGraphicsExtractor"), true, loader);
            Class<?> keyEventClass = Class.forName(minecraftClass("client.input", "KeyEvent"), true, loader);
            Class<?> characterEventClass = Class.forName(minecraftClass("client.input", "CharacterEvent"), true, loader);
            Class<?> mouseButtonInfoClass = Class.forName(minecraftClass("client.input", "MouseButtonInfo"), true, loader);
            Class<?> mouseButtonEventClass = Class.forName(minecraftClass("client.input", "MouseButtonEvent"), true, loader);
            Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW", true, loader);
            Class<?> minecraftClass = Class.forName(minecraftClass("client", "Minecraft"), true, loader);
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            Method setScreen = minecraftClass.getMethod("setScreen", vanillaScreenClass);
            Method currentScreen = minecraftClass.getMethod("screen");
            Method keyPressed = screenClass.getMethod("keyPressed", keyEventClass);
            Method charTyped = screenClass.getMethod("charTyped", characterEventClass);
            Method mouseClicked = screenClass.getMethod("mouseClicked", mouseButtonEventClass, boolean.class);
            Method render = screenClass.getMethod("extractRenderState", graphicsClass, int.class, int.class, float.class);
            Constructor<?> screenConstructor = screenClass.getConstructor(
                    String.class,
                    String.class,
                    int.class,
                    int.class,
                    int.class,
                    int.class
            );
            Map<String, Object> dataSources = object(contract.get("agent5DataSources"));
            Map<String, Object> terminalData = object(dataSources.get("terminal"));
            Map<String, Object> indexData = object(dataSources.get("index"));
            Map<String, Object> lensData = object(dataSources.get("lens"));
            Map<String, Object> hudData = object(dataSources.get("hud"));
            Map<String, Object> missionLogData = object(dataSources.get("missionLog"));
            Map<String, Object> settingsData = object(dataSources.get("settings"));
            Map<String, Object> deathRecoveryData = object(dataSources.get("deathRecovery"));
            Map<String, Object> holomapData = object(dataSources.get("holomap"));
            Map<String, Object> wikiData = object(dataSources.get("wiki"));

            Object terminal = screenConstructor.newInstance("TERMINAL", "ashfall", 12, 3, 2, 1);
            require("terminal:input".equals(field(terminal, "focusedControl")),
                    "generated terminal screen must initialize keyboard focus");
            require(Boolean.TRUE.equals(field(terminal, "initialFocusRouted")),
                    "generated terminal screen must record initial focus routing");
            Object graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(terminal, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "terminal command unavailable for active runtime host");
            requireRenderedTextContains(graphics, "Prompt: ASH>");
            requireRenderedTextContains(graphics, "Health " + hudData.get("health"));
            requireRenderedTextContains(graphics, String.valueOf(hudData.get("hazard")));
            require(Boolean.FALSE.equals(keyPressed.invoke(terminal, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated terminal screen must reject Enter without a runtime save-data host");
            require(Boolean.FALSE.equals(field(terminal, "terminalCommandExecuted")),
                    "generated terminal screen must not fake terminal command execution without a runtime host");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(terminal, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "awaiting supported runtime save data");

            Object index = screenConstructor.newInstance("INDEX", "ashfall", 12, 3, 2, 1);
            require("index:search".equals(field(index, "focusedControl")),
                    "generated index screen must initialize keyboard focus");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(index, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "Index search unavailable for active runtime host");
            requireRenderedTextContains(graphics, "Health " + hudData.get("health"));
            require(Boolean.FALSE.equals(keyPressed.invoke(index, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated index screen must reject Enter without a runtime events host");
            require(Boolean.FALSE.equals(field(index, "indexSearchExecuted")),
                    "generated index screen must not fake index search execution without a runtime host");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(index, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "awaiting supported runtime events");

            Object lens = screenConstructor.newInstance("LENS", "ashfall", 12, 3, 2, 1);
            require("lens:scan".equals(field(lens, "focusedControl")),
                    "generated lens screen must initialize keyboard focus");
            require(Boolean.FALSE.equals(keyPressed.invoke(lens, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated lens screen must not fake scanner execution without a runtime host");
            require(Boolean.TRUE.equals(mouseClicked(mouseClicked, mouseButtonEventClass, mouseButtonInfoClass, lens, 0)),
                    "generated lens screen must still allow mouse focus for unavailable scanner action");
            require("lens:scan".equals(field(lens, "focusedControl")),
                    "generated lens screen must focus scan action");
            require(Boolean.FALSE.equals(field(lens, "lensScanExecuted")),
                    "generated lens screen must not mark scanner executed until runtime host mutates");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(lens, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, String.valueOf(lensData.get("target")));
            requireRenderedTextContains(graphics, "scanner unavailable for active runtime host");

            Object mission = screenConstructor.newInstance("MISSION_LOG", "ashfall", 12, 3, 2, 1);
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(mission, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, String.valueOf(missionLogData.get("title")));
            requireRenderedTextContains(graphics, String.valueOf(missionLogData.get("objective")));
            requireRenderedTextContains(graphics, "mission update unavailable for active runtime host");
            require(Boolean.FALSE.equals(keyPressed.invoke(mission, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated mission log screen must reject Enter without a runtime events host");
            require(!"UPDATED".equals(field(mission, "missionStatus")),
                    "generated mission log screen must not fake updated mission status without a runtime host");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(mission, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "awaiting supported runtime events");

            Object settings = screenConstructor.newInstance("SETTINGS", "ashfall", 12, 3, 2, 1);
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(settings, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, String.valueOf(settingsData.get("profile")));
            requireRenderedTextContains(graphics, String.valueOf(settingsData.get("inputMode")));
            require(Boolean.TRUE.equals(keyPressed.invoke(settings, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_DOWN"))),
                    "generated settings screen must route list Down");
            require(Boolean.TRUE.equals(keyPressed.invoke(settings, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_DOWN"))),
                    "generated settings screen must route repeated list Down");
            require("Input Mode".equals(field(settings, "selectedOption")),
                    "generated settings screen must update selected option");
            require(Boolean.TRUE.equals(keyPressed.invoke(settings, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_DOWN"))),
                    "generated settings screen must route Down to HUD scale");
            require("HUD Scale".equals(field(settings, "selectedOption")),
                    "generated settings screen must select HUD scale");
            require(Boolean.TRUE.equals(keyPressed.invoke(settings, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated settings screen must apply selected HUD scale");
            require(Double.valueOf(1.25D).equals(field(settings, "settingsHudScale")),
                    "generated settings screen must store adjusted HUD scale");
            require(Boolean.TRUE.equals(keyPressed.invoke(settings, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_DOWN"))),
                    "generated settings screen must route Down to subtitles");
            require("Subtitles".equals(field(settings, "selectedOption")),
                    "generated settings screen must select subtitles");
            require(Boolean.TRUE.equals(keyPressed.invoke(settings, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated settings screen must apply subtitles toggle");
            require(Boolean.FALSE.equals(field(settings, "settingsSubtitles")),
                    "generated settings screen must store adjusted subtitles flag");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(settings, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "Selected: Subtitles");
            requireRenderedTextContains(graphics, "HUD scale: 1.25    Subtitles: disabled");

            Object holomap = screenConstructor.newInstance("HOLOMAP", "ashfall", 12, 3, 2, 1);
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(holomap, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, String.valueOf(holomapData.get("layer")));
            requireRenderedTextContains(graphics, String.valueOf(holomapData.get("marker")));
            requireRenderedTextContains(graphics, String.valueOf(holomapData.get("focus")));

            Object wiki = screenConstructor.newInstance("WIKI", "ashfall", 12, 3, 2, 1);
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(wiki, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, String.valueOf(wikiData.get("guide")));
            requireRenderedTextContains(graphics, String.valueOf(wikiData.get("page")));
            requireRenderedTextContains(graphics, String.valueOf(wikiData.get("summary")));

            Object mainMenu = screenConstructor.newInstance("MAIN_MENU", "ashfall", 12, 3, 2, 1);
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(mainMenu, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "Main Menu: ECHO Ashfall Terminal boot routes");
            require(Boolean.TRUE.equals(keyPressed.invoke(mainMenu, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_DOWN"))),
                    "generated main menu screen must route Down to New Run");
            require(Boolean.TRUE.equals(keyPressed.invoke(mainMenu, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_DOWN"))),
                    "generated main menu screen must route Down to Settings");
            require("Settings".equals(field(mainMenu, "selectedOption")),
                    "generated main menu screen must select Settings option");
            clearScreen(setScreen, minecraft);
            require(Boolean.TRUE.equals(keyPressed.invoke(mainMenu, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated main menu screen must activate Settings option");
            require("SETTINGS".equals(mode(currentScreen.invoke(minecraft))),
                    "generated main menu Settings option must route to settings screen");
            require("Settings selected: opening Settings".equals(field(mainMenu, "mainMenuOutput")),
                    "generated main menu screen must store selected option output");

            require(Boolean.TRUE.equals(keyPressed.invoke(terminal, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_N"))),
                    "generated terminal screen must route the SignalOS hotkey");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(terminal, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "Notifications: "
                    + String.join(" / ", EchoNativeAgent5UiExpectedValues.notificationMessages()));

            clearScreen(setScreen, minecraft);
            require(Boolean.TRUE.equals(keyPressed.invoke(terminal, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_M"))),
                    "generated screen must handle M terminal route");
            require(currentScreen.invoke(minecraft) == null,
                    "M route must hand off to the native terminal surface instead of opening a generated placeholder");

            Object recovery = screenConstructor.newInstance("RECOVERY", "ashfall", 12, 3, 2, 1);
            require("recovery:recover".equals(field(recovery, "focusedControl")),
                    "generated recovery screen must initialize keyboard focus");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(recovery, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, String.valueOf(deathRecoveryData.get("recoveryPoint")));
            requireRenderedTextContains(graphics, "recovery grant unavailable for active runtime host");
            require(Boolean.FALSE.equals(keyPressed.invoke(recovery, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated recovery screen must not recover from keyboard focus without inventory grant support");
            require(Boolean.TRUE.equals(mouseClicked(mouseClicked, mouseButtonEventClass, mouseButtonInfoClass, recovery, 0)),
                    "generated recovery screen must still allow mouse focus for unavailable inventory grant action");
            require("recovery:recover".equals(field(recovery, "focusedControl")),
                    "generated recovery screen must focus recover action");
            require(Boolean.FALSE.equals(field(recovery, "recoveryActionExecuted")),
                    "generated recovery screen must not mark recovery executed until runtime host mutates inventory");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(recovery, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "recovery grant unavailable for active runtime host");
            requireRenderedTextContains(graphics, "awaiting supported runtime inventory");

            Object hudScreen = screenConstructor.newInstance("HUD", "ashfall", 12, 3, 2, 1);
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(hudScreen, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "HUD: Health " + EchoNativeAgent5UiExpectedValues.hud().get("health"));
            requireRenderedTextContains(graphics, "HUD refresh unavailable for active runtime host");
            require(Boolean.FALSE.equals(keyPressed.invoke(hudScreen, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated HUD screen must reject Enter without a runtime events host");
            require(EchoNativeAgent5UiExpectedValues.hud().get("health").equals(field(hudScreen, "hudHealth")),
                    "generated HUD screen must not fake updated health without a runtime host");
            graphics = graphicsClass.getConstructor().newInstance();
            render.invoke(hudScreen, graphics, 0, 0, 0.0F);
            requireRenderedTextContains(graphics, "awaiting supported runtime events");

            clearScreen(setScreen, minecraft);
            require(Boolean.TRUE.equals(keyPressed.invoke(terminal, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ESCAPE"))),
                    "generated screen must close to gameplay on Esc");
            require(currentScreen.invoke(minecraft) == null, "Esc must close generated screen to gameplay");
            Object pause = screenConstructor.newInstance("PAUSE", "ashfall", 12, 3, 2, 1);
            require(Boolean.TRUE.equals(keyPressed.invoke(pause, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_DOWN"))),
                    "generated pause screen must route Down to Settings");
            require("Settings".equals(field(pause, "selectedOption")),
                    "generated pause screen must select Settings option");
            require(Boolean.TRUE.equals(keyPressed.invoke(pause, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"))),
                    "generated pause screen must activate Settings option");
            require("SETTINGS".equals(mode(currentScreen.invoke(minecraft))),
                    "generated pause Settings option must route to settings screen");
            clearScreen(setScreen, minecraft);
            require(Boolean.TRUE.equals(keyPressed.invoke(pause, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ESCAPE"))),
                    "generated pause screen must also close to gameplay on Esc");
            require(currentScreen.invoke(minecraft) == null, "Esc from pause must close generated screen to gameplay");
        } catch (ReflectiveOperationException | IOException exception) {
            throw new IllegalStateException("generated native UI screen execution smoke failed: "
                    + exception.getMessage(), exception);
        }
    }

    private static Object keyEvent(Class<?> keyEventClass, Class<?> glfwClass, String keyName)
            throws ReflectiveOperationException {
        int key = glfwClass.getField(keyName).getInt(null);
        try {
            return keyEventClass.getConstructor(int.class, int.class, int.class).newInstance(key, 0, 0);
        } catch (NoSuchMethodException exception) {
            return keyEventClass.getConstructor(int.class).newInstance(key);
        }
    }

    private static void type(Method charTyped, Class<?> characterEventClass, Object target, String value)
            throws ReflectiveOperationException {
        for (int index = 0; index < value.length(); index++) {
            Object event = characterEventClass.getConstructor(int.class).newInstance((int) value.charAt(index));
            require(Boolean.TRUE.equals(charTyped.invoke(target, event)),
                    "generated screen must accept typed character " + value.charAt(index));
        }
    }

    private static Object mouseClicked(
            Method mouseClicked,
            Class<?> mouseButtonEventClass,
            Class<?> mouseButtonInfoClass,
            Object target,
            int button
    ) throws ReflectiveOperationException {
        Object info = mouseButtonInfoClass.getConstructor(int.class, int.class).newInstance(button, 0);
        Object event = mouseButtonEventClass.getConstructor(double.class, double.class, mouseButtonInfoClass)
                .newInstance(160.0D, 120.0D, info);
        return mouseClicked.invoke(target, event, false);
    }

    private static void clearScreen(Method setScreen, Object minecraft) throws ReflectiveOperationException {
        setScreen.invoke(minecraft, new Object[]{null});
    }

    private static Object field(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static String mode(Object screen) throws ReflectiveOperationException {
        if (screen == null) {
            return "";
        }
        Object value = field(screen, "mode");
        return value == null ? "" : String.valueOf(value);
    }

    private static void requireRenderedTextContains(Object graphics, String token) throws ReflectiveOperationException {
        String renderedText = String.join("\n", renderedText(graphics));
        require(renderedText.contains(token), "generated screen render text missing data-source token " + token);
    }

    @SuppressWarnings("unchecked")
    private static List<String> renderedText(Object graphics) throws ReflectiveOperationException {
        Object value = graphics.getClass().getMethod("textCalls").invoke(graphics);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static void requireGeneratedScreenCoversContract(Map<String, Object> contract) {
        String source = EchoNativeLiveUiBridge.screenSource();
        require(source.contains("NativeLoaderScreenHostModel.render")
                        && source.contains("NativeLoaderScreenHostModel.configure")
                        && source.contains("EchoNativeAgent5UiHandlerRegistry.renderSurface"),
                "generated screen must delegate screen model composition to the native host model provider");
        require(source.contains("routeKeyName") && source.contains("EchoNativeAgent5UiActionRouter.routeKey"),
                "generated screen must delegate key routing to the Agent 5 action router");
        require(source.contains("GLFW_KEY_M") && source.contains("return \"M\""), "generated screen must route terminal key");
        require(source.contains("GLFW_KEY_G") && source.contains("return \"G\""), "generated screen must route index catalog key");
        require(source.contains("GLFW_KEY_R") && source.contains("return \"R\""), "generated screen must route index recipe key");
        require(source.contains("GLFW_KEY_U") && source.contains("return \"U\""), "generated screen must route index usage key");
        require(source.contains("GLFW_KEY_B") && source.contains("return \"B\""), "generated screen must route contextual bookmark/drone key");
        require(source.contains("GLFW_KEY_LEFT_ALT") && source.contains("return \"LEFT_ALT\""),
                "generated screen must route lens deep-scan key");
        require(source.contains("GLFW_KEY_J") && source.contains("return \"J\""), "generated screen must route holomap open key");
        require(source.contains("GLFW_KEY_K") && source.contains("return \"K\""), "generated screen must route holomap minimap key");
        require(source.contains("GLFW_KEY_RIGHT_BRACKET") && source.contains("return \"RIGHT_BRACKET\""),
                "generated screen must route holomap zoom-in key");
        require(source.contains("GLFW_KEY_LEFT_BRACKET") && source.contains("return \"LEFT_BRACKET\""),
                "generated screen must route holomap zoom-out key");
        require(source.contains("GLFW_KEY_BACKSLASH") && source.contains("return \"BACKSLASH\""),
                "generated screen must route holomap corner key");
        require(source.contains("GLFW_KEY_N") && source.contains("return \"N\""),
                "generated screen must route SignalOS key");
        require(source.contains("GLFW_KEY_X") && source.contains("return \"X\""), "generated screen must route drone recall key");
        require(source.contains("GLFW_KEY_C") && source.contains("return \"C\""), "generated screen must route drone scan key");
        require(source.contains("GLFW_KEY_Y") && source.contains("return \"Y\""), "generated screen must route drone scout key");
        require(source.contains("GLFW_KEY_Z") && source.contains("return \"Z\""), "generated screen must route drone status key");
        require(source.contains("GLFW_KEY_ESCAPE") && source.contains("return \"ESCAPE\""),
                "generated screen must route pause key");
        require(source.contains("GLFW_KEY_BACKSPACE") && source.contains("return \"BACKSPACE\"")
                        && source.contains("EchoNativeAgent5UiActionRouter.routeEditKey"),
                "generated screen must route text editing key");
        require(source.contains("GLFW_KEY_UP") && source.contains("return \"UP\"")
                        && source.contains("GLFW_KEY_DOWN") && source.contains("return \"DOWN\"")
                        && source.contains("EchoNativeAgent5UiActionRouter.routeListNavigation"),
                "generated screen must route list navigation keys");
        require(source.contains("EchoNativeAgent5UiActionRouter.routeMouseClick"),
                "generated screen must route mouse click activation");
        require(source.contains("EchoNativeAgent5UiActionRouter.routeSettingsAdjustment"),
                "generated screen must route settings adjustment");
        require(source.contains("EchoNativeAgent5UiActionRouter.routePauseOption"),
                "generated screen must route pause option activation");
        require(source.contains("EchoNativeAgent5UiActionRouter.routeMissionLogUpdate"),
                "generated screen must route mission log updates");
        require(source.contains("EchoNativeAgent5UiActionRouter.routeMainMenuOption"),
                "generated screen must route main-menu option activation");
        require(source.contains("EchoNativeAgent5UiActionRouter.routeWorldSetupCreate")
                        && source.contains("EchoNativeBootstrapMain.openOrCreateProductWorldFromUi"),
                "generated screen must route world setup create into the native product world open flow");
        require(source.contains("EchoNativeAgent5UiActionRouter.routeInitialFocus"),
                "generated screen must route initial keyboard focus");
        require(source.contains("EchoNativeAgent5UiActionRouter.routeHudUpdate"),
                "generated screen must route HUD updates");
        require(source.contains("EchoNativeAgent5UiActionRouter.routeCameraCinematicFrame"),
                "generated screen must route camera/cinematic frame updates");
        require(source.contains("NativeLoaderRenderCoreLayout.compute"),
                "generated screen must use rendercore layout computation");
        for (Object feature : list(contract, "features")) {
            if ("no_screen_crash".equals(feature)
                    || "custom_main_menu_appears".equals(feature)
                    || "screencore_contract_primitives_execute".equals(feature)
                    || "ui_reference_audit_smoke_executes".equals(feature)
                    || "ui_runtime_equivalence_audit_smoke_executes".equals(feature)
                    || "screencore_primitive_execution_smoke_executes".equals(feature)
                    || "phase5_ui_parity_acceptance_smoke_executes".equals(feature)
                    || "live_client_attachment_acceptance_smoke_executes".equals(feature)
                    || "live_client_host_evidence_acceptance_smoke_executes".equals(feature)
                    || "headless_ui_bridge_readiness_acceptance_smoke_executes".equals(feature)
                    || "adaptercore_runtime_bridge_guard_acceptance_smoke_executes".equals(feature)
                    || "live_client_ui_probe_acceptance_smoke_executes".equals(feature)
                    || "live_client_interaction_probe_acceptance_smoke_executes".equals(feature)
                    || "live_client_phase5_route_sequence_acceptance_smoke_executes".equals(feature)
                    || "live_phase5_acceptance_smoke_executes".equals(feature)
                    || "live_surface_route_acceptance_smoke_executes".equals(feature)
                    || "live_text_input_acceptance_smoke_executes".equals(feature)
                    || "live_hud_overlay_route_acceptance_smoke_executes".equals(feature)
                    || "live_main_menu_override_acceptance_smoke_executes".equals(feature)
                    || "live_notification_queue_acceptance_smoke_executes".equals(feature)
                    || "live_holomap_wiki_navigation_acceptance_smoke_executes".equals(feature)
                    || "live_system_flow_acceptance_smoke_executes".equals(feature)
                    || "live_core_tools_acceptance_smoke_executes".equals(feature)
                    || "live_mission_objective_acceptance_smoke_executes".equals(feature)
                    || "live_input_focus_routing_acceptance_smoke_executes".equals(feature)
                    || "live_screen_stack_stability_acceptance_smoke_executes".equals(feature)
                    || "live_visual_frame_acceptance_smoke_executes".equals(feature)
                    || "live_module_surface_catalog_acceptance_smoke_executes".equals(feature)
                    || "live_render_callback_acceptance_smoke_executes".equals(feature)
                    || "live_screen_ownership_acceptance_smoke_executes".equals(feature)
                    || "live_physical_poll_loop_acceptance_smoke_executes".equals(feature)
                    || "live_physical_event_transcript_acceptance_smoke_executes".equals(feature)
                    || "live_physical_route_effect_transcript_acceptance_smoke_executes".equals(feature)
                    || "live_route_bound_text_command_acceptance_smoke_executes".equals(feature)
                    || "live_route_bound_lens_scan_acceptance_smoke_executes".equals(feature)
                    || "live_route_bound_hud_update_acceptance_smoke_executes".equals(feature)
                    || "live_route_bound_holomap_wiki_acceptance_smoke_executes".equals(feature)
                    || "terminal_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "index_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "lens_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "mission_log_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "settings_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "pause_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "recovery_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "holomap_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "wiki_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "signalos_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "product_action_hotkey_route_executes".equals(feature)
                    || "ashfall_drone_hotkey_route_executes".equals(feature)
                    || "notification_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "hotkey_bridge_smoke_executes".equals(feature)
                    || "notification_queue_smoke_executes".equals(feature)
                    || "notification_dismiss_smoke_executes".equals(feature)
                    || "physical_hotkey_polling_smoke_executes".equals(feature)
                    || "live_surface_acceptance_smoke_executes".equals(feature)
                    || "physical_input_acceptance_smoke_executes".equals(feature)
                    || "live_surface_render_acceptance_smoke_executes".equals(feature)
                    || "ui_host_interaction_state_acceptance_smoke_executes".equals(feature)
                    || "ui_host_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "main_menu_end_to_end_acceptance_smoke_executes".equals(feature)
                    || "hud_overlay_end_to_end_acceptance_smoke_executes".equals(feature)) {
                continue;
            }
            require(source.contains(referenceToken(String.valueOf(feature))),
                    "generated screen source is missing feature token " + feature);
        }
    }

    private static String referenceToken(String feature) {
        return switch (feature) {
            case "ui_reference_audit_smoke_executes" -> "EchoNativeAgent5UiReferenceAudit";
            case "ui_runtime_equivalence_audit_smoke_executes" -> "EchoNativeAgent5UiRuntimeEquivalenceAuditSmoke";
            case "screencore_primitive_execution_smoke_executes" -> "EchoNativeAgent5ScreenCorePrimitiveExecutionSmoke";
            case "phase5_ui_parity_acceptance_smoke_executes" -> "EchoNativeAgent5Phase5UiParityAcceptanceSmoke";
            case "live_client_attachment_acceptance_smoke_executes" -> "EchoNativeAgent5LiveClientAttachmentAcceptanceSmoke";
            case "live_client_host_evidence_acceptance_smoke_executes" -> "EchoNativeAgent5LiveClientHostEvidenceAcceptanceSmoke";
            case "headless_ui_bridge_readiness_acceptance_smoke_executes" -> "EchoNativeAgent5HeadlessUiBridgeReadinessAcceptanceSmoke";
            case "adaptercore_runtime_bridge_guard_acceptance_smoke_executes" -> "EchoNativeAgent5AdapterCoreRuntimeBridgeGuardAcceptance";
            case "live_surface_route_acceptance_smoke_executes" -> "EchoNativeAgent5LiveSurfaceRouteAcceptanceSmoke";
            case "live_text_input_acceptance_smoke_executes" -> "EchoNativeAgent5LiveTextInputAcceptanceSmoke";
            case "live_hud_overlay_route_acceptance_smoke_executes" -> "EchoNativeAgent5LiveHudOverlayRouteAcceptanceSmoke";
            case "live_main_menu_override_acceptance_smoke_executes" -> "EchoNativeAgent5LiveMainMenuOverrideAcceptanceSmoke";
            case "live_notification_queue_acceptance_smoke_executes" -> "EchoNativeAgent5LiveNotificationQueueAcceptanceSmoke";
            case "live_holomap_wiki_navigation_acceptance_smoke_executes" -> "EchoNativeAgent5LiveHoloMapWikiNavigationAcceptanceSmoke";
            case "live_system_flow_acceptance_smoke_executes" -> "EchoNativeAgent5LiveSystemFlowAcceptanceSmoke";
            case "live_core_tools_acceptance_smoke_executes" -> "EchoNativeAgent5LiveCoreToolsAcceptanceSmoke";
            case "live_mission_objective_acceptance_smoke_executes" -> "EchoNativeAgent5LiveMissionObjectiveAcceptanceSmoke";
            case "live_input_focus_routing_acceptance_smoke_executes" -> "EchoNativeAgent5LiveInputFocusRoutingAcceptanceSmoke";
            case "live_screen_stack_stability_acceptance_smoke_executes" -> "EchoNativeAgent5LiveScreenStackStabilityAcceptanceSmoke";
            case "live_visual_frame_acceptance_smoke_executes" -> "EchoNativeAgent5LiveVisualFrameAcceptanceSmoke";
            case "live_module_surface_catalog_acceptance_smoke_executes" -> "EchoNativeAgent5LiveModuleSurfaceCatalogAcceptanceSmoke";
            case "live_render_callback_acceptance_smoke_executes" -> "EchoNativeAgent5LiveRenderCallbackAcceptance";
            case "live_screen_ownership_acceptance_smoke_executes" -> "EchoNativeAgent5LiveScreenOwnershipAcceptance";
            case "live_physical_poll_loop_acceptance_smoke_executes" -> "EchoNativeAgent5LivePhysicalPollLoopAcceptance";
            case "live_physical_event_transcript_acceptance_smoke_executes" -> "EchoNativeAgent5LivePhysicalEventTranscriptAcceptance";
            case "live_physical_route_effect_transcript_acceptance_smoke_executes" -> "EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance";
            case "live_route_bound_text_command_acceptance_smoke_executes" -> "EchoNativeAgent5LiveRouteBoundTextCommandAcceptance";
            case "live_route_bound_lens_scan_acceptance_smoke_executes" -> "EchoNativeAgent5LiveRouteBoundLensScanAcceptance";
            case "live_route_bound_hud_update_acceptance_smoke_executes" -> "EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance";
            case "live_route_bound_holomap_wiki_acceptance_smoke_executes" -> "EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance";
            case "terminal_opens" -> "NativeLoaderScreenHostModel";
            case "terminal_command_executes", "index_opens_and_searches", "lens_scans_target" -> "activate";
            case "terminal_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5TerminalEndToEndAcceptance";
            case "index_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5IndexEndToEndAcceptance";
            case "lens_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5LensEndToEndAcceptance";
            case "mission_log_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5MissionLogEndToEndAcceptance";
            case "settings_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5SettingsEndToEndAcceptance";
            case "pause_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5PauseEndToEndAcceptance";
            case "recovery_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5RecoveryEndToEndAcceptance";
            case "holomap_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5HoloMapEndToEndAcceptance";
            case "wiki_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5WikiEndToEndAcceptance";
            case "notification_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5NotificationEndToEndAcceptance";
            case "hud_updates_health_hazard_mission", "notification_queue_dispatches" -> "NativeLoaderScreenHostModel";
            case "death_recovery_screen_opens_and_recovers" -> "activate";
            case "ui_data_sources_drive_all_agent5_surfaces" -> "NativeLoaderScreenHostModel";
            case "screen_focus_and_input_routing_execute" -> "routeMouseClick";
            case "focus_manager_smoke_executes" -> "routeCharacter";
            case "text_editing_smoke_executes" -> "routeEditKey";
            case "mouse_activation_smoke_executes" -> "routeMouseClick";
            case "list_navigation_smoke_executes" -> "routeListNavigation";
            case "notification_dismiss_smoke_executes" -> "routeNotificationDismiss";
            case "settings_adjustment_smoke_executes" -> "routeSettingsAdjustment";
            case "pause_option_activation_smoke_executes" -> "routePauseOption";
            case "mission_log_update_smoke_executes" -> "routeMissionLogUpdate";
            case "main_menu_option_activation_smoke_executes" -> "routeMainMenuOption";
            case "initial_focus_smoke_executes" -> "routeInitialFocus";
            case "hud_update_smoke_executes" -> "routeHudUpdate";
            case "camera_cinematic_smoke_executes" -> "routeCameraCinematicFrame";
            case "rendercore_layout_smoke_executes" -> "NativeLoaderRenderCoreLayout";
            case "host_event_transcript_smoke_executes" -> "NativeLoaderScreenHostModel";
            case "physical_hotkey_polling_smoke_executes" -> "EchoNativeAgent5PhysicalHotkeyPoller";
            case "live_surface_acceptance_smoke_executes" -> "EchoNativeAgent5LiveSurfaceAcceptance";
            case "physical_input_acceptance_smoke_executes" -> "EchoNativeAgent5PhysicalInputAcceptance";
            case "live_surface_render_acceptance_smoke_executes" -> "EchoNativeAgent5LiveSurfaceRenderAcceptance";
            case "ui_host_interaction_state_acceptance_smoke_executes" -> "EchoNativeAgent5UiHostInteractionStateAcceptance";
            case "ui_host_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5UiHostEndToEndAcceptance";
            case "adapter_ui_handlers_execute" -> "EchoNativeAgent5UiActionRouter";
            case "holomap_wiki_handlers_execute" -> "NativeLoaderScreenHostModel";
            case "native_surface_render_models_execute" -> "NativeLoaderScreenHostModel";
            case "surface_renderer_classes_execute" -> "NativeLoaderScreenHostModel";
            case "input_action_router_classes_execute" -> "EchoNativeAgent5UiActionRouter";
            case "screen_host_models_execute" -> "NativeLoaderScreenHostModel";
            case "screen_stack_execution_smoke_executes" -> "NativeLoaderScreenHostModel";
            case "screen_lifecycle_smoke_executes" -> "EchoNativeAgent5UiActionRouter";
            case "screen_lifecycle_actions_execute" -> "activate";
            case "module_surface_renderers_execute" -> "NativeLoaderScreenHostModel";
            case "all_module_surface_renderers_execute" -> "NativeLoaderScreenHostModel";
            case "theme_application_smoke_executes" -> "NativeLoaderScreenHostModel";
            case "ui_host_smoke_snapshots_execute" -> "NativeLoaderScreenHostModel";
            case "ui_host_interaction_smoke_executes" -> "NativeLoaderScreenHostModel";
            case "ui_host_full_surface_interactions_execute" -> "NativeLoaderScreenHostModel";
            case "main_menu_override_smoke_executes" -> "NativeLoaderScreenHostModel";
            case "main_menu_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5MainMenuEndToEndAcceptance";
            case "hud_overlay_smoke_executes" -> "NativeLoaderScreenHostModel";
            case "hud_overlay_end_to_end_acceptance_smoke_executes" -> "EchoNativeAgent5HudOverlayEndToEndAcceptance";
            case "hotkey_bridge_smoke_executes" -> "EchoNativeAgent5UiActionRouter";
            case "notification_queue_smoke_executes" -> "NativeLoaderScreenHostModel";
            case "mission_log_opens_and_tracks_active_mission",
                    "settings_opens_and_applies_profile",
                    "pause_flow_opens_and_resumes_previous_screen",
                    "holomap_opens",
                    "wiki_page_opens" -> "NativeLoaderScreenHostModel";
            default -> feature;
        };
    }

    private static void writeSource(Path sourceRoot, String relativePath, String source) throws IOException {
        Path path = sourceRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, source, StandardCharsets.UTF_8);
    }

    private static String minecraftPackage(String suffix) {
        return "net" + ".minecraft" + (suffix.isBlank() ? "" : "." + suffix);
    }

    private static String minecraftClass(String packageSuffix, String simpleName) {
        return minecraftPackage(packageSuffix) + "." + simpleName;
    }

    private static String minecraftSourcePath(String packageSuffix, String simpleName) {
        return minecraftPackage(packageSuffix).replace('.', '/') + "/" + simpleName + ".java";
    }

    private static String javaSource(String packageName, String body) {
        return "package " + packageName + ";\n\n" + body;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static List<?> rawList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private static String diagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder builder = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append(diagnostic.getKind())
                    .append(" line ")
                    .append(diagnostic.getLineNumber())
                    .append(": ")
                    .append(diagnostic.getMessage(null));
        }
        return builder.toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
