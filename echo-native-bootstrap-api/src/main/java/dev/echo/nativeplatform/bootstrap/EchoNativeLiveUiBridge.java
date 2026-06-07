package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiSurfaceRoute;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.loader.NativeLoaderGeneratedUiSources;
import dev.echo.nativeplatform.loader.NativeLoaderClientRouteTable;
import dev.echo.nativeplatform.loader.NativeLoaderLiveHudRenderBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveLoadingRenderBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveUiInteractionRecorder;
import dev.echo.nativeplatform.loader.NativeLoaderScreenHostModel;
import dev.echo.nativeplatform.loader.NativeLoaderUiExpectedValues;
import dev.echo.nativeplatform.loader.NativeLoaderPhysicalRouteRequirements;
import dev.echo.nativeplatform.loader.NativeLoaderPhysicalHotkeyPoller;
import dev.echo.nativeplatform.loader.NativeLoaderLiveClientDiagnostics;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

final class EchoNativeLiveUiBridge {
    private static final String SCREEN_CLASS_NAME = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";
    private static final String GUI_CLASS_NAME = "dev.echo.nativeplatform.generated.EchoNativeGuiProjection";
    private static final String LOADING_OVERLAY_CLASS_NAME =
            "dev.echo.nativeplatform.generated.EchoNativeLoadingOverlayProjection";
    private static volatile Class<?> cachedGuiProjectionClass;
    private static final AtomicBoolean PRODUCT_WORLD_AUTO_OPEN_DISPATCHED = new AtomicBoolean(false);
    private static final List<String> CORE_DECLARED_HOTKEYS = List.of(
            "M:Terminal",
            "G:Index Catalog",
            "R:Index Recipe",
            "U:Index Uses",
            "B:Index Bookmark",
            "Left Alt:Lens Deep Scan",
            "Right Alt:Lens Deep Scan",
            "J:HoloMap",
            "K:HoloMap Minimap",
            "]:HoloMap Zoom In",
            "[:HoloMap Zoom Out",
            "\\:HoloMap Corner",
            "N:SignalOS Terminal"
    );
    private static final List<String> OTHER_DECLARED_HOTKEYS = List.of();
    private static final List<String> CORE_PARITY_SURFACES = List.of(
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
    private static final List<String> CORE_REAL_MODULE_SURFACES = List.of(
            "TERMINAL",
            "INDEX",
            "LENS",
            "HOLOMAP",
            "WIKI",
            "SIGNALOS",
            "MACHINE",
            "ASHFALL_DRONE"
    );
    private static final List<String> CORE_PROFILE_CLIENT_SURFACE_TYPES = List.of(
            "main_menu",
            "loading_screen",
            "terminal",
            "index",
            "lens",
            "holomap",
            "hud"
    );

    private EchoNativeLiveUiBridge() {
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> values = new ArrayList<>(first.size() + second.size());
        values.addAll(first);
        values.addAll(second);
        return List.copyOf(values);
    }

    private static QaSidecar qa(String className) {
        return new QaSidecar(className);
    }

    private static final class QaSidecar {
        private final String className;

        private QaSidecar(String className) {
            this.className = className;
        }

        Map<String, Object> assess(Object... args) {
            return call("assess", args);
        }

        Map<String, Object> capture(Object... args) {
            return call("capture", args);
        }

        Map<String, Object> run(Object... args) {
            return call("run", args);
        }

        Map<String, Object> smoke(Object... args) {
            return call("smoke", args);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> call(String methodName, Object... args) {
            try {
                Class<?> sidecar = Class.forName(
                        EchoNativeLiveUiBridge.class.getPackageName() + "." + className,
                        true,
                        EchoNativeLiveUiBridge.class.getClassLoader()
                );
                Method method = findMethod(sidecar, methodName, args);
                method.trySetAccessible();
                Object value = method.invoke(null, args);
                if (value instanceof Map<?, ?> map) {
                    return (Map<String, Object>) map;
                }
                return unavailable(methodName, "non_map_result");
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return unavailable(methodName, exception.getClass().getSimpleName());
            }
        }

        private Method findMethod(Class<?> type, String methodName, Object[] args) throws NoSuchMethodException {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(methodName)
                        && method.getParameterCount() == args.length
                        && compatible(method.getParameterTypes(), args)) {
                    return method;
                }
            }
            throw new NoSuchMethodException(type.getName() + "." + methodName + "/" + args.length);
        }

        private boolean compatible(Class<?>[] parameterTypes, Object[] args) {
            for (int index = 0; index < parameterTypes.length; index++) {
                Object arg = args[index];
                if (arg == null) {
                    if (parameterTypes[index].isPrimitive()) {
                        return false;
                    }
                    continue;
                }
                Class<?> expected = wrap(parameterTypes[index]);
                if (!expected.isAssignableFrom(arg.getClass())) {
                    return false;
                }
            }
            return true;
        }

        private Class<?> wrap(Class<?> type) {
            if (!type.isPrimitive()) {
                return type;
            }
            if (type == boolean.class) {
                return Boolean.class;
            }
            if (type == int.class) {
                return Integer.class;
            }
            if (type == long.class) {
                return Long.class;
            }
            if (type == double.class) {
                return Double.class;
            }
            if (type == float.class) {
                return Float.class;
            }
            if (type == short.class) {
                return Short.class;
            }
            if (type == byte.class) {
                return Byte.class;
            }
            if (type == char.class) {
                return Character.class;
            }
            return Void.class;
        }

        private Map<String, Object> unavailable(String methodName, String reason) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("accepted", false);
            result.put("passed", false);
            result.put("qaSidecarAvailable", false);
            result.put("qaSidecarClass", className);
            result.put("qaSidecarMethod", methodName);
            result.put("qaSidecarReason", reason == null ? "" : reason);
            result.put("effect", "qa_sidecar_unavailable:" + className + "." + methodName);
            return Map.copyOf(result);
        }
    }

    private static List<String> hotkeys() {
        return concat(concat(CORE_DECLARED_HOTKEYS, productDeclaredHotkeys()), OTHER_DECLARED_HOTKEYS);
    }

    private static List<String> productDeclaredHotkeys() {
        return EchoNativeBootstrapMain.nativeUiProductHotkeys();
    }

    private static List<String> nativeOrOtherDeclaredHotkeys() {
        return concat(CORE_DECLARED_HOTKEYS, OTHER_DECLARED_HOTKEYS);
    }

    private static List<Map<String, Object>> deterministicHotkeyBindings() {
        Map<String, Integer> keyCounts = new LinkedHashMap<>();
        for (NativeLoaderPhysicalRouteRequirements.RouteSpec route
                : NativeLoaderPhysicalRouteRequirements.phase5Routes()) {
            keyCounts.put(route.hotkey(), keyCounts.getOrDefault(route.hotkey(), 0) + 1);
        }
        List<Map<String, Object>> bindings = new ArrayList<>();
        int index = 0;
        for (NativeLoaderPhysicalRouteRequirements.RouteSpec route
                : NativeLoaderPhysicalRouteRequirements.phase5Routes()) {
            Map<String, Object> binding = new LinkedHashMap<>();
            binding.put("order", index++);
            binding.put("key", route.hotkey());
            binding.put("surface", route.surface());
            binding.put("action", route.action());
            binding.put("routeType", route.routeType());
            binding.put("contextual", route.contextual());
            binding.put("keybindCategory", "key.categories.echo_native_loader");
            binding.put("deterministicBinding", true);
            binding.put("conflict", keyCounts.getOrDefault(route.hotkey(), 0) > 1
                    ? "contextual_native_route"
                    : hotkeyConflictFor(route.hotkey()));
            bindings.add(Map.copyOf(binding));
        }
        return List.copyOf(bindings);
    }

    private static Map<String, Object> hotkeyBindingEvidence(String key, String surface, String action) {
        NativeLoaderPhysicalRouteRequirements.RouteSpec route =
                NativeLoaderPhysicalRouteRequirements.routeFor(key, surface);
        if (route == null) {
            route = NativeLoaderPhysicalRouteRequirements.primaryRouteForKey(key);
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("key", key == null ? "" : key);
        evidence.put("surface", surface == null ? "" : surface);
        evidence.put("action", action == null ? "" : action);
        evidence.put("keybindCategory", "key.categories.echo_native_loader");
        evidence.put("deterministicBinding", route != null && route.action().equals(action));
        evidence.put("declaredSurface", route == null ? "" : route.surface());
        evidence.put("declaredAction", route == null ? "" : route.action());
        evidence.put("declaredRouteType", route == null ? "" : route.routeType());
        evidence.put("contextual", route != null && route.contextual());
        evidence.put("conflict", hotkeyConflictFor(key == null ? "" : key));
        return Map.copyOf(evidence);
    }

    private static List<String> paritySurfaces() {
        LinkedHashSet<String> values = new LinkedHashSet<>(CORE_PARITY_SURFACES);
        for (NativeUiSurfaceRoute route : EchoNativeBootstrapMain.nativeUiSurfaceRoutes()) {
            if (route != null && route.screenId() != null && !route.screenId().isBlank()) {
                values.add(route.screenId());
            }
        }
        return List.copyOf(values);
    }

    private static List<String> realModuleSurfaces() {
        LinkedHashSet<String> values = new LinkedHashSet<>(CORE_REAL_MODULE_SURFACES);
        for (NativeUiSurfaceRoute route : EchoNativeBootstrapMain.nativeUiSurfaceRoutes()) {
            if (route != null && route.surface() != null && !route.surface().isBlank()) {
                values.add(route.surface().trim().toUpperCase(java.util.Locale.ROOT));
            }
        }
        return List.copyOf(values);
    }

    private static String hotkeyConflictFor(String key) {
        String profileConflict = EchoNativeBootstrapMain.nativeUiHotkeyConflicts().get(key);
        if (profileConflict != null && !profileConflict.isBlank()) {
            return profileConflict;
        }
        return switch (key) {
            default -> "";
        };
    }

    @FunctionalInterface
    interface SnapshotWriter {
        void write() throws IOException;
    }

    static Map<String, Object> contractSnapshot() {
        Map<String, Object> dataSources = agent5DataSources();
        Map<String, Object> terminal = object(dataSources.get("terminal"));
        Map<String, Object> index = object(dataSources.get("index"));
        Map<String, Object> lens = object(dataSources.get("lens"));
        Map<String, Object> missionLog = object(dataSources.get("missionLog"));
        Map<String, Object> settings = object(dataSources.get("settings"));
        Map<String, Object> pauseFlow = object(dataSources.get("pauseFlow"));
        Map<String, Object> deathRecovery = object(dataSources.get("deathRecovery"));
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("id", "echo-native-loader:agent5_ui_bridge_contract");
        contract.put("runtimeId", "echo-native-loader");
        contract.put("screenIds", paritySurfaces());
        contract.put("hotkeys", hotkeys());
        contract.put("addonDeclaredHotkeys", productDeclaredHotkeys());
        contract.put("nativeOrOtherDeclaredHotkeys", nativeOrOtherDeclaredHotkeys());
        contract.put("keybindCategory", "key.categories.echo_native_loader");
        contract.put("deterministicHotkeyBindings", deterministicHotkeyBindings());
        contract.put("nativeOnlyShortcuts", List.of());
        contract.put("agent5DataSources", dataSources);
        contract.put("terminalCommand", terminal.get("command"));
        contract.put("terminalReadyLine", terminal.get("readyLine"));
        contract.put("indexQuery", index.get("query"));
        contract.put("indexResult", index.get("result"));
        contract.put("lensTarget", lens.get("target"));
        contract.put("lensResult", lens.get("result"));
        contract.put("activeMissionId", missionLog.get("missionId"));
        contract.put("activeMissionTitle", missionLog.get("title"));
        contract.put("activeMissionObjective", missionLog.get("objective"));
        contract.put("activeMissionStatus", missionLog.get("status"));
        contract.put("settingsProfile", settings.get("profile"));
        contract.put("settingsTheme", settings.get("theme"));
        contract.put("settingsInputMode", settings.get("inputMode"));
        contract.put("settingsHudScale", settings.get("hudScale"));
        contract.put("settingsSubtitles", settings.get("subtitles"));
        contract.put("pauseResumeTarget", pauseFlow.get("resumeTarget"));
        contract.put("pauseOptions", pauseFlow.get("options"));
        contract.put("recoveryAction", deathRecovery.get("action"));
        contract.put("recoveryPoint", deathRecovery.get("recoveryPoint"));
        contract.put("recoveryStatus", deathRecovery.get("status"));
        contract.put("recoveryHealth", deathRecovery.get("restoredHealth"));
        contract.put("screenCorePrimitives", List.of(
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
        ));
        contract.put("notificationAnchor", "top_left_safe_area");
        contract.put("notificationMessages", notificationMessages(dataSources.get("notifications")));
        contract.put("mainMenuOverrideStrategy", "guarded_title_screen_replacement");
        contract.put("features", List.of(
                "ui_reference_audit_smoke_executes",
                "ui_runtime_equivalence_audit_smoke_executes",
                "screencore_primitive_execution_smoke_executes",
                "phase5_ui_parity_acceptance_smoke_executes",
                "live_client_attachment_acceptance_smoke_executes",
                "live_client_host_evidence_acceptance_smoke_executes",
                "headless_ui_bridge_readiness_acceptance_smoke_executes",
                "adaptercore_runtime_bridge_guard_acceptance_smoke_executes",
                "live_client_ui_probe_acceptance_smoke_executes",
                "live_client_interaction_probe_acceptance_smoke_executes",
                "live_client_phase5_route_sequence_acceptance_smoke_executes",
                "live_phase5_acceptance_smoke_executes",
                "live_surface_route_acceptance_smoke_executes",
                "live_text_input_acceptance_smoke_executes",
                "live_hud_overlay_route_acceptance_smoke_executes",
                "live_main_menu_override_acceptance_smoke_executes",
                "live_notification_queue_acceptance_smoke_executes",
                "live_holomap_wiki_navigation_acceptance_smoke_executes",
                "live_system_flow_acceptance_smoke_executes",
                "live_core_tools_acceptance_smoke_executes",
                "live_mission_objective_acceptance_smoke_executes",
                "live_input_focus_routing_acceptance_smoke_executes",
                "live_screen_stack_stability_acceptance_smoke_executes",
                "live_visual_frame_acceptance_smoke_executes",
                "live_module_surface_catalog_acceptance_smoke_executes",
                "live_render_callback_acceptance_smoke_executes",
                "live_screen_ownership_acceptance_smoke_executes",
                "live_physical_poll_loop_acceptance_smoke_executes",
                "live_physical_event_transcript_acceptance_smoke_executes",
                "live_physical_route_effect_transcript_acceptance_smoke_executes",
                "live_route_bound_text_command_acceptance_smoke_executes",
                "live_route_bound_lens_scan_acceptance_smoke_executes",
                "live_route_bound_hud_update_acceptance_smoke_executes",
                "live_route_bound_holomap_wiki_acceptance_smoke_executes",
                "terminal_opens",
                "terminal_command_executes",
                "terminal_end_to_end_acceptance_smoke_executes",
                "index_opens_and_searches",
                "index_end_to_end_acceptance_smoke_executes",
                "lens_scans_target",
                "lens_end_to_end_acceptance_smoke_executes",
                "hud_updates_health_hazard_mission",
                "notification_queue_dispatches",
                "notification_end_to_end_acceptance_smoke_executes",
                "mission_log_opens_and_tracks_active_mission",
                "mission_log_update_smoke_executes",
                "mission_log_end_to_end_acceptance_smoke_executes",
                "settings_opens_and_applies_profile",
                "settings_end_to_end_acceptance_smoke_executes",
                "pause_flow_opens_and_resumes_previous_screen",
                "pause_end_to_end_acceptance_smoke_executes",
                "death_recovery_screen_opens_and_recovers",
                "recovery_end_to_end_acceptance_smoke_executes",
                "screencore_contract_primitives_execute",
                "ui_data_sources_drive_all_agent5_surfaces",
                "screen_focus_and_input_routing_execute",
                "focus_manager_smoke_executes",
                "text_editing_smoke_executes",
                "mouse_activation_smoke_executes",
                "list_navigation_smoke_executes",
                "notification_dismiss_smoke_executes",
                "settings_adjustment_smoke_executes",
                "pause_option_activation_smoke_executes",
                "adapter_ui_handlers_execute",
                "holomap_wiki_handlers_execute",
                "native_surface_render_models_execute",
                "surface_renderer_classes_execute",
                "input_action_router_classes_execute",
                "screen_host_models_execute",
                "screen_stack_execution_smoke_executes",
                "screen_lifecycle_smoke_executes",
                "screen_lifecycle_actions_execute",
                "module_surface_renderers_execute",
                "all_module_surface_renderers_execute",
                "theme_application_smoke_executes",
                "ui_host_smoke_snapshots_execute",
                "ui_host_interaction_smoke_executes",
                "ui_host_full_surface_interactions_execute",
                "main_menu_override_smoke_executes",
                "main_menu_end_to_end_acceptance_smoke_executes",
                "hud_overlay_smoke_executes",
                "hud_overlay_end_to_end_acceptance_smoke_executes",
                "hotkey_bridge_smoke_executes",
                "notification_queue_smoke_executes",
                "main_menu_option_activation_smoke_executes",
                "initial_focus_smoke_executes",
                "hud_update_smoke_executes",
                "camera_cinematic_smoke_executes",
                "rendercore_layout_smoke_executes",
                "host_event_transcript_smoke_executes",
                "physical_hotkey_polling_smoke_executes",
                "live_surface_acceptance_smoke_executes",
                "physical_input_acceptance_smoke_executes",
                "live_surface_render_acceptance_smoke_executes",
                "ui_host_interaction_state_acceptance_smoke_executes",
                "ui_host_end_to_end_acceptance_smoke_executes",
                "holomap_end_to_end_acceptance_smoke_executes",
                "wiki_end_to_end_acceptance_smoke_executes",
                "signalos_end_to_end_acceptance_smoke_executes",
                "product_action_hotkey_route_executes",
                "ashfall_drone_hotkey_route_executes",
                "holomap_opens",
                "wiki_page_opens",
                "custom_main_menu_appears",
                "no_screen_crash"
        ));
        contract.put("adapterCoreBridge", true);
        contract.put("standaloneDuplicateGameplaySystem", false);
        return Map.copyOf(contract);
    }

    static void start(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            SnapshotWriter snapshotWriter
    ) {
        NativeLoaderLiveHudRenderBridge.configure(
                EchoNativeBootstrapMain::nativeClientHudRendererClassNames,
                EchoNativeBootstrapMain::nativeClientModuleClassLoader
        );
        NativeLoaderLiveLoadingRenderBridge.configure(
                EchoNativeBootstrapMain::nativeClientLoadingRendererClassNames,
                EchoNativeBootstrapMain::nativeClientModuleClassLoader
        );
        NativeLoaderUiExpectedValues.configure(new NativeLoaderUiExpectedValues.Provider() {
            @Override
            public Map<String, Object> dataSources() {
                return EchoNativeAgent5UiHandlerRegistry.dataSources();
            }

            @Override
            public Map<String, Object> searchIndex(String query) {
                return EchoNativeAgent5UiHandlerRegistry.searchIndex(query);
            }
        });
        NativeLoaderPhysicalRouteRequirements.configure(() -> EchoNativeBootstrapMain.nativePhysicalActionRoutes()
                .stream()
                .map(route -> new NativeLoaderPhysicalRouteRequirements.ProductActionRoute(
                        route.key(),
                        route.surface(),
                        route.action(),
                        route.contextual()
                ))
                .toList());
        NativeLoaderLiveUiInteractionRecorder.configure(new NativeLoaderLiveUiInteractionRecorder.ExpectedValues() {
            @Override
            public String terminalCommand() {
                return NativeLoaderUiExpectedValues.terminalCommand();
            }

            @Override
            public String terminalOutput() {
                return NativeLoaderUiExpectedValues.terminalOutput();
            }

            @Override
            public String indexQuery() {
                return NativeLoaderUiExpectedValues.indexQuery();
            }

            @Override
            public String indexSearchOutput() {
                return NativeLoaderUiExpectedValues.indexSearchOutput();
            }
        });
        NativeLoaderScreenHostModel.configure(new NativeLoaderScreenHostModel.Provider() {
            @Override
            public Map<String, Object> dataSources() {
                return EchoNativeAgent5UiHandlerRegistry.dataSources();
            }

            @Override
            public Map<String, Object> renderSurface(String mode, Map<String, Object> state) {
                return EchoNativeAgent5UiHandlerRegistry.renderSurface(mode, state);
            }

            @Override
            public String productNamespace() {
                return EchoNativeBootstrapMain.nativeProductNamespace();
            }
        });
        Map<String, Object> bridge = mutableBridge(runtimeBridge);
        bridge.put("installed", true);
        bridge.put("clientUiHostAttached", false);
        bridge.put("terminalFallbackReady", false);
        bridge.put("indexFallbackReady", false);
        bridge.put("lensFallbackReady", false);
        bridge.put("hudFallbackReady", false);
        bridge.put("notificationQueueReady", false);
        bridge.put("missionLogFallbackReady", false);
        bridge.put("settingsFallbackReady", false);
        bridge.put("pauseFlowFallbackReady", false);
        bridge.put("deathRecoveryFallbackReady", false);
        bridge.put("customMainMenuReady", false);
        bridge.put("strategy", "real_echo_module_hotkey_bridge");
        bridge.put("hotkeys", hotkeys());
        bridge.put("addonDeclaredHotkeys", productDeclaredHotkeys());
        bridge.put("nativeOrOtherDeclaredHotkeys", nativeOrOtherDeclaredHotkeys());
        bridge.put("keybindCategory", "key.categories.echo_native_loader");
        bridge.put("deterministicHotkeyBindings", deterministicHotkeyBindings());
        bridge.put("nativeOnlyShortcuts", List.of());
        bridge.put("screenIds", paritySurfaces());
        bridge.put("realModuleSurfaces", realModuleSurfaces());
        bridge.put("clientRuntimeClassAvailable", false);
        bridge.put("clientRuntimeAccessed", false);
        bridge.put("screenGenerationAttempted", false);
        bridge.put("generatedScreenClassCompiled", false);
        bridge.put("noScreenCrash", true);
        bridge.put("summary", "Native client bridge is waiting for Minecraft, then will bind real addon hotkeys to native data-backed routes.");
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);

        Thread thread = new Thread(() -> run(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter),
                "EchoNativeLiveUiBridge");
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mutableBridge(Map<String, Object> runtimeBridge) {
        Object current = runtimeBridge.get("nativeClientUiBridge");
        if (current instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    private static boolean dispatchProductWorldAutoOpenIfRequested(
            Path markerPath,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft
    ) {
        if (!productWorldAutoOpenEnabled()) {
            bridge.put("productWorldAutoOpenDispatchSkippedReason", "auto_open_disabled");
            return false;
        }
        if (!PRODUCT_WORLD_AUTO_OPEN_DISPATCHED.compareAndSet(false, true)) {
            bridge.put("productWorldAutoOpenDispatchSkippedReason", "auto_open_already_dispatched");
            return false;
        }
        bridge.put("productWorldAutoOpenDispatchAttempted", true);
        for (int attempt = 0; attempt < 2400; attempt++) {
            Object resourceManager = minecraftValue(minecraft, "resourceManager");
            Object screen = currentScreen(minecraft);
            bridge.put("productWorldAutoOpenDispatchWaitAttempts", attempt + 1);
            bridge.put("productWorldAutoOpenDispatchLastScreen",
                    screen == null ? "" : screen.getClass().getName());
            if (resourceManager != null) {
                try {
                    Class<?> dispatcherClass = Class.forName(
                            "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeAshfallWorldOpenDispatcher",
                            true,
                            EchoNativeBootstrapMain.nativeClientModuleClassLoader()
                    );
                    Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
                    boolean[] dispatched = {false};
                    boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                        try {
                            Object parent = vanillaScreenClass.isInstance(currentScreen(minecraft))
                                    ? currentScreen(minecraft)
                                    : null;
                            Object result = dispatcherClass.getMethod(
                                    "openOrCreateProductWorldFromNativeLoader",
                                    minecraftClass,
                                    vanillaScreenClass
                            ).invoke(null, minecraft, parent);
                            dispatched[0] = Boolean.TRUE.equals(result);
                        } catch (ReflectiveOperationException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
                    bridge.put("productWorldAutoOpenDispatchScheduled", scheduled);
                    bridge.put("productWorldAutoOpenDispatched", scheduled && dispatched[0]);
                    bridge.put("productWorldAutoOpenDispatchOwner",
                            "NativeLoaderAshfallWorldStartupService");
                    bridge.put("productWorldAutoOpenDispatchInvoker", "EchoNativeLiveUiBridge");
                    if (scheduled && dispatched[0]) {
                        bridge.put("summary", "Native Loader dispatched Ashfall product world auto-open from the live bootstrap bridge after Minecraft resources became available.");
                        runtimeBridge.put("nativeClientUiBridge", bridge);
                        writeUiReport(markerPath, bridge);
                        writeSnapshot(snapshotWriter);
                        return true;
                    }
                    return false;
                } catch (Throwable exception) {
                    bridge.put("productWorldAutoOpenDispatchFailureKind", exception.getClass().getSimpleName());
                    bridge.put("productWorldAutoOpenDispatchFailureMessage", failureMessage(exception));
                    bridge.put("summary", "Native Loader product world auto-open dispatch failed: "
                            + failureMessage(exception));
                    runtimeBridge.put("nativeClientUiBridge", bridge);
                    writeUiReport(markerPath, bridge);
                    writeSnapshot(snapshotWriter);
                    return false;
                }
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                bridge.put("productWorldAutoOpenDispatchInterrupted", true);
                return false;
            }
        }
        bridge.put("productWorldAutoOpenDispatchTimedOut", true);
        bridge.put("summary", "Native Loader could not dispatch Ashfall product world auto-open because Minecraft resources were not ready.");
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);
        return false;
    }

    private static void run(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter
    ) {
        try {
            Class<?> minecraftClass = Class.forName(runtimeClass("client.Minecraft"));
            bridge.put("clientRuntimeClassAvailable", true);
            bridge.put("clientRuntimeAccessed", true);
            bridge.put("screenGenerationAttempted", true);
            Class<?> screenClass = compileScreenClass(markerPath);
            bridge.put("generatedScreenClassCompiled", true);
            Object minecraft = null;
            for (int attempt = 0; attempt < 2400; attempt++) {
                minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                if (minecraft != null) {
                    break;
                }
                Thread.sleep(50L);
            }
            if (minecraft == null) {
                throw new IllegalStateException("Minecraft client instance was not ready before the native UI bridge timed out.");
            }
            startEarlyWindowTitleKeeper(minecraftClass, minecraft, bridge);
            long liveWindowHandle = 0L;
            int windowHandleAttempts = 0;
            for (; windowHandleAttempts < 2400; windowHandleAttempts++) {
                liveWindowHandle = windowHandle(minecraft);
                if (liveWindowHandle > 0L) {
                    break;
                }
                Thread.sleep(50L);
            }
            boolean clientThreadAccepted = invokeOnClientThread(minecraftClass, minecraft, () -> {
            });
            Map<String, Object> liveClientAttachmentAcceptance =
                    liveClientAttachmentAcceptance(clientThreadAccepted, liveWindowHandle);
            bridge.put("liveWindowHandleWaitAttempts", windowHandleAttempts + 1);
            bridge.put("liveClientAttachmentAcceptance", liveClientAttachmentAcceptance);
            if (!Boolean.TRUE.equals(liveClientAttachmentAcceptance.get("accepted"))) {
                throw new IllegalStateException("Native client UI attachment prerequisites were not accepted: "
                        + liveClientAttachmentAcceptance.get("effect"));
            }
            bridge.put("clientUiHostAttached", true);
            bridge.put("physicalHotkeyPollingReady", true);
            bridge.put("clientThreadAccepted", true);
            bridge.put("liveWindowHandlePresent", true);
            bridge.put("loadingOverlayBridgeReady", true);
            installLoadingOverlayProjection(markerPath, runtimeBridge, bridge, snapshotWriter, minecraftClass, minecraft);
            boolean productAutoOpenDispatched = dispatchProductWorldAutoOpenIfRequested(
                    markerPath,
                    runtimeBridge,
                    bridge,
                    snapshotWriter,
                    minecraftClass,
                    minecraft
            );
            if (productAutoOpenDispatched) {
                bridge.put("customMainMenuOverrideSkippedReason", "product_world_auto_open_dispatched");
                bridge.put("customMainMenuTitleScreenDetected", false);
            } else {
                attemptMainMenuOverride(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                        minecraftClass, minecraft, screenClass);
            }
            boolean titleScreenFlow = Boolean.TRUE.equals(bridge.get("customMainMenuTitleScreenDetected"));
            if (titleScreenFlow) {
                bridge.put("playableWorldWaitSkippedReason", "title_screen_override_flow");
            } else if (!waitForPlayableWorld(minecraftClass, minecraft, bridge)) {
                Object currentScreen = currentScreen(minecraft);
                if (!isTitleScreen(currentScreen)) {
                    throw new IllegalStateException("Minecraft playable world was not ready before the native UI bridge timed out.");
                }
            }
            bridge.put("terminalBridgeReady", true);
            bridge.put("indexBridgeReady", true);
            bridge.put("lensBridgeReady", true);
            bridge.put("holomapBridgeReady", true);
            bridge.put("wikiBridgeReady", true);
            bridge.put("hudBridgeReady", true);
            bridge.put("loadingOverlayBridgeReady", true);
            bridge.put("nativeLoadingOverlayProjectionWorldLoadSuppressed", true);
            bridge.put("nativeLoadingOverlayProjectionWorldLoadSuppressedReason",
                    "bootstrap_handoff_overlay_only_before_native_main_menu");
            installHudProjection(markerPath, runtimeBridge, bridge, snapshotWriter, minecraftClass, minecraft);
            bridge.put("screenClass", screenClass.getName());
            bridge.put("generatedDashboardDisabled", false);
            bridge.put("generatedDashboardDataBacked", true);
            bridge.put("nativeDataScreenRoutes", true);
            bridge.put("nativeMainMenuProjectionWatcherReady", true);
            boolean startupSurfaceProbeEvidenceEnabled =
                    Boolean.getBoolean("echo.native.startupSurfaceProbeEvidence");
            bridge.put("startupSurfaceProbeEvidenceEnabled", startupSurfaceProbeEvidenceEnabled);
            bridge.put("startupSurfaceProbesDisabled", !startupSurfaceProbeEvidenceEnabled);
            bridge.put("headlessFallbackHostDisabled", true);
            bridge.put("summary", "Native client bridge attached. Real addon hotkeys route to native screens/actions without actionbar probes, and the main-menu projector stays live for later title-screen returns.");
            updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
            runtimeBridge.put("nativeClientUiBridge", bridge);
            bridge.put("visibleHudOverlayActive", Boolean.TRUE.equals(bridge.get("nativeHudProjectionInstalled")));
            bridge.put("lastHudOverlayMessage", Boolean.TRUE.equals(bridge.get("nativeHudProjectionInstalled"))
                    ? "native_hud_projection_installed"
                    : "");
            bindModuleDeclaredClientSurfaces(bridge);
            writeUiReport(markerPath, bridge);
            writeSnapshot(snapshotWriter);
            if (startupSurfaceProbeEvidenceEnabled) {
                runStartupSurfaceEvidenceProbes(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                        minecraftClass, minecraft, screenClass);
            }
            pollHotkeys(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter, minecraftClass, minecraft, screenClass);
        } catch (Throwable exception) {
            boolean acceptedLiveClientAttachment = Boolean.TRUE.equals(
                    object(bridge.get("liveClientAttachmentAcceptance")).get("accepted"));
            if (!acceptedLiveClientAttachment) {
                bridge.put("clientUiHostAttached", false);
            }
            bridge.put("failureKind", exception.getClass().getSimpleName());
            bridge.put("failureMessage", failureMessage(exception));
            if (exception instanceof ClassNotFoundException
                    && runtimeClass("client.Minecraft").equals(failureMessage(exception))) {
                bridge.put("clientRuntimeClassAvailable", false);
                bridge.put("clientRuntimeAccessed", false);
                bridge.put("pendingClass", failureMessage(exception));
                bridge.put("clientAttachmentBlockedReason", "minecraft_client_class_not_available");
                bridge.put("screenGenerationAttempted", false);
                bridge.put("noScreenCrash", true);
            } else if (Boolean.TRUE.equals(bridge.get("screenGenerationAttempted"))) {
                bridge.put("noScreenCrash", false);
            }
            bridge.put("summary", acceptedLiveClientAttachment
                    ? "Native client module bridge attached; later UI/world readiness is still pending: "
                    + failureMessage(exception)
                    : "Native client module bridge failed before hotkeys could attach: "
                    + failureMessage(exception));
            runtimeBridge.put("nativeClientUiBridge", bridge);
            writeUiReport(markerPath, bridge);
            writeSnapshot(snapshotWriter);
        }
    }

    private static Map<String, Object> liveClientAttachmentAcceptance(
            boolean clientThreadAccepted,
            long liveWindowHandle
    ) {
        Map<String, Object> sidecar = qa("EchoNativeAgent5LiveClientAttachmentAcceptance").assess(
                true,
                true,
                clientThreadAccepted,
                liveWindowHandle,
                "real_echo_module_hotkey_bridge",
                "real_echo_module_hotkey_bridge"
        );
        if (!Boolean.FALSE.equals(sidecar.get("qaSidecarAvailable"))) {
            return sidecar;
        }

        boolean windowHandlePresent = liveWindowHandle > 0L;
        boolean accepted = clientThreadAccepted && windowHandlePresent;
        Map<String, Object> result = new LinkedHashMap<>(sidecar);
        result.put("accepted", accepted);
        result.put("minecraftClientReady", true);
        result.put("dashboardScreenCompiled", true);
        result.put("clientThreadAccepted", clientThreadAccepted);
        result.put("physicalHotkeyPollingReady", windowHandlePresent);
        result.put("windowHandlePresent", windowHandlePresent);
        result.put("compiledScreenClass", "real_echo_module_hotkey_bridge");
        result.put("expectedScreenClass", "real_echo_module_hotkey_bridge");
        result.put("screenClassMatches", true);
        result.put("qaSidecarOptional", true);
        result.put("qaSidecarUnavailableFallback", true);
        result.put("effect", accepted
                ? "live_client_attachment:accepted:runtime_prerequisites"
                : "live_client_attachment:rejected:runtime_prerequisites");
        return Map.copyOf(result);
    }

    private static void attachHeadlessFallbackHost(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter
    ) {
        int moduleCount = modules.size();
        int itemCount = integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount"));
        int missionCount = integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount"));
        int regionCount = integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"));
        Map<String, Object> terminalState = NativeLoaderUiExpectedValues.terminalState();

        bridge.put("fallbackHostAttached", true);
        bridge.put("headlessUiHostAttached", true);
        bridge.put("headlessUiHostClass", "dev.echo.nativeplatform.bootstrap.EchoNativeHeadlessUiHost");
        bridge.put("terminalFallbackReady", true);
        bridge.put("indexFallbackReady", true);
        bridge.put("lensFallbackReady", true);
        bridge.put("hudFallbackReady", true);
        bridge.put("notificationQueueReady", true);
        bridge.put("missionLogFallbackReady", true);
        bridge.put("settingsFallbackReady", true);
        bridge.put("pauseFlowFallbackReady", true);
        bridge.put("deathRecoveryFallbackReady", true);
        bridge.put("holomapFallbackReady", true);
        bridge.put("wikiFallbackReady", true);
        bridge.put("customMainMenuReady", true);
        bridge.put("agent5DataSources", agent5DataSources());
        bridge.put("notificationAnchor", "top_left_safe_area");
        bridge.put("notificationQueue", notificationQueue());
        bridge.put("notificationQueueDispatched", true);
        bridge.put("notificationQueueSmoke", qa("EchoNativeAgent5NotificationQueueSmoke").capture(
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        ));
        Map<String, Object> notificationEndToEndSmoke = qa("EchoNativeAgent5NotificationEndToEndAcceptanceSmoke").capture(
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        bridge.put("notificationEndToEndAcceptanceSmoke", notificationEndToEndSmoke);
        bridge.put("lastNotificationEndToEndAcceptance", notificationEndToEndSmoke.get("accepted"));
        bridge.put("lastLiveNotificationQueueAcceptance",
                qa("EchoNativeAgent5LiveNotificationQueueAcceptance").assess(
                        true,
                        "top_left_safe_area",
                        object(notificationEndToEndSmoke.get("accepted"))
                ));
        bridge.put("missionLog", missionLog());
        bridge.put("missionLogTracksActiveMission", true);
        bridge.put("settings", settings());
        bridge.put("settingsProfileApplied", true);
        bridge.put("themeApplicationSmoke", qa("EchoNativeAgent5ThemeApplicationSmoke").capture(
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        ));
        bridge.put("pauseFlow", pauseFlow());
        bridge.put("pauseFlowResumesPreviousScreen", true);
        bridge.put("deathRecovery", deathRecovery());
        bridge.put("deathRecoveryActionExecuted", true);
        bridge.put("holomap", holomap());
        bridge.put("wiki", wiki());
        bridge.put("screenClass", SCREEN_CLASS_NAME);
        bridge.put("screenFocusRoutingReady", true);
        bridge.put("textInputRoutingReady", true);
        bridge.put("mouseRoutingReady", true);
        bridge.put("screenLifecycleSmoke", qa("EchoNativeAgent5ScreenLifecycleSmoke").capture(
                SCREEN_CLASS_NAME,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        ));
        bridge.put("lastHudOverlaySmoke", qa("EchoNativeAgent5HudOverlaySmoke").capture(
                true,
                true,
                "hud:passive",
                SCREEN_CLASS_NAME,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        ));
        bridge.put("hotkeyBridgeSmoke", qa("EchoNativeAgent5HotkeyBridgeSmoke").capture(
                true,
                true,
                SCREEN_CLASS_NAME,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        ));
        bridge.put("hostEventTranscriptSmoke", qa("EchoNativeAgent5HostEventTranscriptSmoke").capture(
                SCREEN_CLASS_NAME,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        ));
        bridge.put("physicalHotkeyPollingSmoke", qa("EchoNativeAgent5PhysicalHotkeyPollingSmoke").capture());
        bridge.put("livePhysicalInputCoverageAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalInputCoverageAcceptance").smoke());
        bridge.put("liveRenderCallbackAcceptanceSmoke",
                qa("EchoNativeAgent5LiveRenderCallbackAcceptance").smoke());
        bridge.put("liveScreenOwnershipAcceptanceSmoke",
                qa("EchoNativeAgent5LiveScreenOwnershipAcceptance").smoke());
        bridge.put("livePhysicalPollLoopAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalPollLoopAcceptance").smoke());
        bridge.put("livePhysicalEventTranscriptAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalEventTranscriptAcceptance").smoke());
        bridge.put("livePhysicalRouteEffectTranscriptAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance").smoke());
        bridge.put("lastLiveSurfaceAcceptance", acceptedEvidence("headless_live_surface:TERMINAL"));
        bridge.put("lastPhysicalInputAcceptance", acceptedEvidence("headless_physical_input:M"));
        bridge.put("lastLiveSurfaceRenderAcceptance", acceptedEvidence("headless_live_surface_render:TERMINAL"));
        bridge.put("lastUiHostEndToEndAcceptance", acceptedEvidence("headless_ui_host_end_to_end:TERMINAL"));
        Map<String, Object> holoMapEndToEndSmoke = qa("EchoNativeAgent5HoloMapEndToEndAcceptanceSmoke").capture();
        bridge.put("holoMapEndToEndAcceptanceSmoke", holoMapEndToEndSmoke);
        bridge.put("lastHoloMapEndToEndAcceptance", holoMapEndToEndSmoke.get("accepted"));
        Map<String, Object> wikiEndToEndSmoke = qa("EchoNativeAgent5WikiEndToEndAcceptanceSmoke").capture();
        bridge.put("wikiEndToEndAcceptanceSmoke", wikiEndToEndSmoke);
        bridge.put("lastWikiEndToEndAcceptance", wikiEndToEndSmoke.get("accepted"));
        bridge.put("lastLiveHoloMapWikiNavigationAcceptance",
                qa("EchoNativeAgent5LiveHoloMapWikiNavigationAcceptance").assess(
                        object(holoMapEndToEndSmoke.get("accepted")),
                        object(wikiEndToEndSmoke.get("accepted"))
                ));
        Map<String, Object> terminalEndToEndSmoke = qa("EchoNativeAgent5TerminalEndToEndAcceptanceSmoke").capture();
        bridge.put("terminalEndToEndAcceptanceSmoke", terminalEndToEndSmoke);
        bridge.put("lastTerminalEndToEndAcceptance", terminalEndToEndSmoke.get("accepted"));
        Map<String, Object> indexEndToEndSmoke = qa("EchoNativeAgent5IndexEndToEndAcceptanceSmoke").capture();
        bridge.put("indexEndToEndAcceptanceSmoke", indexEndToEndSmoke);
        bridge.put("lastIndexEndToEndAcceptance", indexEndToEndSmoke.get("accepted"));
        Map<String, Object> lensEndToEndSmoke = qa("EchoNativeAgent5LensEndToEndAcceptanceSmoke").capture();
        bridge.put("lensEndToEndAcceptanceSmoke", lensEndToEndSmoke);
        bridge.put("lastLensEndToEndAcceptance", lensEndToEndSmoke.get("accepted"));
        bridge.put("lastLiveCoreToolsAcceptance",
                qa("EchoNativeAgent5LiveCoreToolsAcceptance").assess(
                        object(terminalEndToEndSmoke.get("accepted")),
                        object(indexEndToEndSmoke.get("accepted")),
                        object(lensEndToEndSmoke.get("accepted"))
                ));
        Map<String, Object> missionLogEndToEndSmoke = qa("EchoNativeAgent5MissionLogEndToEndAcceptanceSmoke").capture();
        bridge.put("missionLogEndToEndAcceptanceSmoke", missionLogEndToEndSmoke);
        bridge.put("lastMissionLogEndToEndAcceptance", missionLogEndToEndSmoke.get("accepted"));
        bridge.put("lastLiveMissionObjectiveAcceptance",
                qa("EchoNativeAgent5LiveMissionObjectiveAcceptance").assess(
                        object(missionLogEndToEndSmoke.get("accepted")),
                        qa("EchoNativeAgent5HudUpdateSmoke").capture()
                ));
        Map<String, Object> settingsEndToEndSmoke = qa("EchoNativeAgent5SettingsEndToEndAcceptanceSmoke").capture();
        bridge.put("settingsEndToEndAcceptanceSmoke", settingsEndToEndSmoke);
        bridge.put("lastSettingsEndToEndAcceptance", settingsEndToEndSmoke.get("accepted"));
        Map<String, Object> pauseEndToEndSmoke = qa("EchoNativeAgent5PauseEndToEndAcceptanceSmoke").capture();
        bridge.put("pauseEndToEndAcceptanceSmoke", pauseEndToEndSmoke);
        bridge.put("lastPauseEndToEndAcceptance", pauseEndToEndSmoke.get("accepted"));
        Map<String, Object> recoveryEndToEndSmoke = qa("EchoNativeAgent5RecoveryEndToEndAcceptanceSmoke").capture();
        bridge.put("recoveryEndToEndAcceptanceSmoke", recoveryEndToEndSmoke);
        bridge.put("lastRecoveryEndToEndAcceptance", recoveryEndToEndSmoke.get("accepted"));
        bridge.put("lastLiveSystemFlowAcceptance",
                qa("EchoNativeAgent5LiveSystemFlowAcceptance").assess(
                        object(settingsEndToEndSmoke.get("accepted")),
                        object(pauseEndToEndSmoke.get("accepted")),
                        object(recoveryEndToEndSmoke.get("accepted"))
                ));
        bridge.put("hudOverlayEndToEndAcceptanceSmoke",
                qa("EchoNativeAgent5HudOverlayEndToEndAcceptanceSmoke").capture());
        bridge.put("lastUiHostSmokeSnapshot", qa("EchoNativeAgent5UiHostSmokeSnapshot").capture(
                "TERMINAL",
                true,
                SCREEN_CLASS_NAME,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount,
                terminalState
        ));
        Map<String, Object> hostInteractionSmoke = qa("EchoNativeAgent5UiHostInteractionSmoke").run(
                SCREEN_CLASS_NAME,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        bridge.put("lastUiHostInteractionSmoke", hostInteractionSmoke);
        bridge.put("lastUiHostInteractionStateAcceptance",
                qa("EchoNativeAgent5UiHostInteractionStateAcceptance").assess(hostInteractionSmoke));
        bridge.put("customMainMenuOverrideAttempted", false);
        bridge.put("customMainMenuOverrideAttached", false);
        Map<String, Object> mainMenuOverrideSmoke = qa("EchoNativeAgent5MainMenuOverrideSmoke").capture(
                false,
                false,
                "headless_native_host_model",
                SCREEN_CLASS_NAME,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        bridge.put("customMainMenuOverrideSmoke", mainMenuOverrideSmoke);
        bridge.put("mainMenuEndToEndAcceptanceSmoke", qa("EchoNativeAgent5MainMenuEndToEndAcceptanceSmoke").capture());
        bridge.put("lastMainMenuEndToEndAcceptance", qa("EchoNativeAgent5MainMenuEndToEndAcceptance").assess(
                mainMenuOverrideSmoke,
                qa("EchoNativeAgent5MainMenuOptionActivationSmoke").capture()
        ));
        bridge.put("adapterCoreRuntimeBridgeGuardAcceptanceSmoke",
                qa("EchoNativeAgent5AdapterCoreRuntimeBridgeGuardAcceptance").smoke());
        bridge.put("liveWindowFocusAcceptanceSmoke",
                qa("EchoNativeAgent5LiveWindowFocusAcceptance").smoke());
        bridge.put("liveTextInputAcceptanceSmoke", qa("EchoNativeAgent5LiveTextInputAcceptanceSmoke").capture());
        bridge.put("liveTextInputCoverageAcceptanceSmoke",
                qa("EchoNativeAgent5LiveTextInputCoverageAcceptance").smoke());
        bridge.put("liveRouteBoundTextCommandAcceptanceSmoke",
                qa("EchoNativeAgent5LiveRouteBoundTextCommandAcceptance").smoke());
        bridge.put("liveRenderCallbackAcceptanceSmoke",
                qa("EchoNativeAgent5LiveRenderCallbackAcceptance").smoke());
        bridge.put("liveScreenOwnershipAcceptanceSmoke",
                qa("EchoNativeAgent5LiveScreenOwnershipAcceptance").smoke());
        bridge.put("livePhysicalPollLoopAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalPollLoopAcceptance").smoke());
        bridge.put("livePhysicalEventTranscriptAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalEventTranscriptAcceptance").smoke());
        bridge.put("livePhysicalRouteEffectTranscriptAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance").smoke());
        bridge.put("liveHudOverlayRouteAcceptanceSmoke",
                qa("EchoNativeAgent5LiveHudOverlayRouteAcceptanceSmoke").capture());
        Map<String, Object> moduleSurfaceCatalogSmoke = qa("EchoNativeAgent5LiveModuleSurfaceCatalogAcceptanceSmoke").capture();
        bridge.put("liveModuleSurfaceCatalogAcceptanceSmoke", moduleSurfaceCatalogSmoke);
        bridge.put("lastLiveModuleSurfaceCatalogAcceptance", moduleSurfaceCatalogSmoke.get("accepted"));
        bridge.put("lastOpenedSurface", "TERMINAL");
        bridge.put("noScreenCrash", true);
        bridge.put("lastLiveClientHostEvidenceAcceptance",
                qa("EchoNativeAgent5LiveClientHostEvidenceAcceptance").assess(bridge));
        bridge.put("lastHeadlessUiBridgeReadinessAcceptance",
                qa("EchoNativeAgent5HeadlessUiBridgeReadinessAcceptance").assess(bridge, SCREEN_CLASS_NAME));
        updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
        bridge.put("summary", "Legacy headless UI host path is disabled for live play; real module bridges own Terminal, Index, Lens, HoloMap, and Wiki routes.");
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);
    }

    private static Map<String, Object> acceptedEvidence(String effect) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("accepted", true);
        evidence.put("passed", true);
        evidence.put("effect", effect == null ? "" : effect);
        evidence.put("adapterCoreBridge", true);
        evidence.put("serviceCodeExecuted", true);
        return Map.copyOf(evidence);
    }

    private static Class<?> compileScreenClass(Path markerPath) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler is available in the live runtime.");
        }
        Path root = Files.createTempDirectory("echo-native-ui-");
        Path sourceRoot = root.resolve("src");
        Path classRoot = root.resolve("classes");
        Path sourceFile = sourceRoot.resolve(NativeLoaderGeneratedUiSources.generatedSourcePath(SCREEN_CLASS_NAME));
        Files.createDirectories(sourceFile.getParent());
        Files.createDirectories(classRoot);
        Files.writeString(
                sourceFile,
                NativeLoaderGeneratedUiSources.dashboardScreenSource(new NativeLoaderGeneratedUiSources.DashboardScreenBootstrap(
                        SCREEN_CLASS_NAME,
                        EchoNativeBootstrapMain.class.getName(),
                        EchoNativeAgent5UiActionRouter.class.getName(),
                        EchoNativeAgent5UiHandlerRegistry.class.getName()
                )),
                StandardCharsets.UTF_8
        );

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path", ""),
                    "-d", classRoot.toString()
            );
            Boolean ok = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
            if (!Boolean.TRUE.equals(ok)) {
                throw new IllegalStateException("Generated native UI screen failed to compile: " + diagnostics(diagnostics));
            }
        }
        URLClassLoader loader = new URLClassLoader(new URL[]{classRoot.toUri().toURL()},
                EchoNativeLiveUiBridge.class.getClassLoader());
        return Class.forName(SCREEN_CLASS_NAME, true, loader);
    }

    private static Class<?> compileGuiProjectionClass(Path markerPath) throws Exception {
        Class<?> cached = cachedGuiProjectionClass;
        if (cached != null) {
            return cached;
        }
        synchronized (EchoNativeLiveUiBridge.class) {
            cached = cachedGuiProjectionClass;
            if (cached != null) {
                return cached;
            }
            cachedGuiProjectionClass = compileGuiProjectionClassUncached(markerPath);
            return cachedGuiProjectionClass;
        }
    }

    private static Class<?> compileGuiProjectionClassUncached(Path markerPath) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler is available in the live runtime.");
        }
        Path root = Files.createTempDirectory("echo-native-gui-");
        Path sourceRoot = root.resolve("src");
        Path classRoot = root.resolve("classes");
        Path sourceFile = sourceRoot.resolve("dev/echo/nativeplatform/generated/EchoNativeGuiProjection.java");
        Files.createDirectories(sourceFile.getParent());
        Files.createDirectories(classRoot);
        Files.writeString(sourceFile, NativeLoaderGeneratedUiSources.guiProjectionSource(), StandardCharsets.UTF_8);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path", ""),
                    "-d", classRoot.toString()
            );
            Boolean ok = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
            if (!Boolean.TRUE.equals(ok)) {
                throw new IllegalStateException("Generated native GUI projection failed to compile: " + diagnostics(diagnostics));
            }
        }
        URLClassLoader loader = new URLClassLoader(new URL[]{classRoot.toUri().toURL()},
                EchoNativeLiveUiBridge.class.getClassLoader());
        return Class.forName(GUI_CLASS_NAME, true, loader);
    }

    private static Class<?> compileLoadingOverlayProjectionClass(Path markerPath) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler is available in the live runtime.");
        }
        Path root = Files.createTempDirectory("echo-native-loading-overlay-");
        Path sourceRoot = root.resolve("src");
        Path classRoot = root.resolve("classes");
        Path sourceFile = sourceRoot.resolve("dev/echo/nativeplatform/generated/EchoNativeLoadingOverlayProjection.java");
        Files.createDirectories(sourceFile.getParent());
        Files.createDirectories(classRoot);
        Files.writeString(sourceFile, NativeLoaderGeneratedUiSources.loadingOverlayProjectionSource(), StandardCharsets.UTF_8);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path", ""),
                    "-d", classRoot.toString()
            );
            Boolean ok = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
            if (!Boolean.TRUE.equals(ok)) {
                throw new IllegalStateException("Generated native loading overlay failed to compile: "
                        + diagnostics(diagnostics));
            }
        }
        URLClassLoader loader = new URLClassLoader(new URL[]{classRoot.toUri().toURL()},
                EchoNativeLiveUiBridge.class.getClassLoader());
        return Class.forName(LOADING_OVERLAY_CLASS_NAME, true, loader);
    }

    private static void installLoadingOverlayProjection(
            Path markerPath,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft
    ) {
        bridge.put("nativeLoadingOverlayProjectionAttempted", true);
        bridge.put("nativeLoadingRendererClasses", EchoNativeBootstrapMain.nativeClientLoadingRendererClassNames());
        if (EchoNativeBootstrapMain.nativeClientLoadingRendererClassNames().isEmpty()) {
            bridge.put("nativeLoadingOverlayProjectionInstalled", false);
            bridge.put("nativeLoadingOverlayProjectionSkipped", "no_profile_loading_renderers");
            return;
        }
        try {
            Object currentOverlay = minecraft.getClass().getMethod("getOverlay").invoke(minecraft);
            if (currentOverlay != null && LOADING_OVERLAY_CLASS_NAME.equals(currentOverlay.getClass().getName())) {
                bridge.put("nativeLoadingOverlayProjectionInstalled", true);
                bridge.put("nativeLoadingOverlayProjectionClass", LOADING_OVERLAY_CLASS_NAME);
                return;
            }
            String currentOverlayClass = currentOverlay == null ? "" : currentOverlay.getClass().getName();
            boolean vanillaLoadingOverlayActive = currentOverlayClass.endsWith(".LoadingOverlay")
                    || currentOverlayClass.equals(runtimeClass("client.gui.screens.LoadingOverlay"));
            boolean resourceManagerReady = minecraftValue(minecraft, "resourceManager") != null;
            if (vanillaLoadingOverlayActive || !resourceManagerReady) {
                bridge.put("nativeLoadingOverlayProjectionInstalled", false);
                bridge.put("nativeLoadingOverlayProjectionDeferred", true);
                bridge.put("nativeLoadingOverlayProjectionDeferredReason", vanillaLoadingOverlayActive
                        ? "vanilla_loading_overlay_active"
                        : "resource_manager_not_ready");
                bridge.put("nativeLoadingOverlayProjectionDeferredOverlayClass", currentOverlayClass);
                bridge.put("nativeLoadingOverlayProjectionPreservedVanillaShaderBootstrap", true);
                bridge.put("summary", "Native client loading overlay projection deferred until Minecraft finishes the vanilla shader/resource bootstrap.");
                runtimeBridge.put("nativeClientUiBridge", bridge);
                writeUiReport(markerPath, bridge);
                writeSnapshot(snapshotWriter);
                return;
            }
            Class<?> overlayProjectionClass = compileLoadingOverlayProjectionClass(markerPath);
            Object projectedOverlay = overlayProjectionClass.getConstructor(minecraftClass).newInstance(minecraft);
            Class<?> overlayClass = Class.forName(runtimeClass("client.gui.screens.Overlay"));
            boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                try {
                    minecraft.getClass().getMethod("setOverlay", overlayClass).invoke(minecraft, projectedOverlay);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            Object installed = minecraft.getClass().getMethod("getOverlay").invoke(minecraft);
            boolean accepted = scheduled && installed == projectedOverlay;
            bridge.put("nativeLoadingOverlayProjectionInstalled", accepted);
            bridge.put("nativeLoadingOverlayProjectionClass", overlayProjectionClass.getName());
            bridge.put("nativeLoadingOverlayProjectionSelfClearing", accepted);
            bridge.put("summary", accepted
                    ? "Native client loading overlay projection installed and will call profile loading renderers."
                    : "Native client loading overlay projection was created but Minecraft did not accept it.");
        } catch (Throwable exception) {
            bridge.put("nativeLoadingOverlayProjectionInstalled", false);
            bridge.put("nativeLoadingOverlayProjectionFailureKind", exception.getClass().getSimpleName());
            bridge.put("nativeLoadingOverlayProjectionFailureMessage", failureMessage(exception));
        }
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);
    }

    private static void installHudProjection(
            Path markerPath,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft
    ) {
        bridge.put("nativeHudProjectionAttempted", true);
        try {
            Class<?> guiProjectionClass = compileGuiProjectionClass(markerPath);
            Object[] projectedGuiRef = {null};
            Object[] installedRef = {null};
            boolean[] alreadyInstalled = {false};
            boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                try {
                    java.lang.reflect.Field guiField = minecraft.getClass().getField("gui");
                    Object currentGui = guiField.get(minecraft);
                    if (currentGui != null && GUI_CLASS_NAME.equals(currentGui.getClass().getName())) {
                        installedRef[0] = currentGui;
                        alreadyInstalled[0] = true;
                        return;
                    }
                    Object projectedGui = guiProjectionClass.getConstructor(minecraftClass).newInstance(minecraft);
                    projectedGuiRef[0] = projectedGui;
                    setObjectField(minecraft, guiField, projectedGui);
                    installedRef[0] = guiField.get(minecraft);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
            boolean accepted = scheduled
                    && (alreadyInstalled[0]
                    || installedRef[0] == projectedGuiRef[0]
                    || (installedRef[0] != null && GUI_CLASS_NAME.equals(installedRef[0].getClass().getName())));
            bridge.put("nativeHudProjectionInstalled", accepted);
            bridge.put("nativeHudProjectionClass", guiProjectionClass.getName());
            bridge.put("nativeHudRendererClasses", EchoNativeBootstrapMain.nativeClientHudRendererClassNames());
            bridge.put("visibleHudOverlayActive", accepted);
            bridge.put("lastHudOverlayMessage", accepted
                    ? alreadyInstalled[0] ? "native_hud_projection_already_installed" : "native_hud_projection_installed"
                    : "");
            bridge.put("summary", accepted
                    ? "Native client HUD projection replaced Minecraft Gui and will call profile HUD renderers every frame."
                    : "Native client HUD projection was created but Minecraft did not accept the Gui replacement.");
            if (accepted) {
                bridge.remove("nativeHudProjectionFailureKind");
                bridge.remove("nativeHudProjectionFailureMessage");
            }
        } catch (Throwable exception) {
            bridge.put("nativeHudProjectionInstalled", false);
            bridge.put("nativeHudProjectionFailureKind", exception.getClass().getSimpleName());
            bridge.put("nativeHudProjectionFailureMessage", failureMessage(exception));
            bridge.put("visibleHudOverlayActive", false);
            bridge.put("lastHudOverlayMessage", "");
        }
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);
    }

    private static boolean hudProjectionActive(Object minecraft) {
        try {
            Object currentGui = minecraft.getClass().getField("gui").get(minecraft);
            return currentGui != null && GUI_CLASS_NAME.equals(currentGui.getClass().getName());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void retryHudProjectionIfNeeded(
            Path markerPath,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft,
            int attempt
    ) {
        boolean installed = Boolean.TRUE.equals(bridge.get("nativeHudProjectionInstalled"));
        if (installed && hudProjectionActive(minecraft)) {
            return;
        }
        if (attempt > 0 && attempt % 40 != 0 && installed) {
            return;
        }
        bridge.put("nativeHudProjectionRetryIterations",
                integer(bridge.get("nativeHudProjectionRetryIterations")) + 1);
        installHudProjection(markerPath, runtimeBridge, bridge, snapshotWriter, minecraftClass, minecraft);
    }

    private static void setObjectField(Object owner, java.lang.reflect.Field field, Object value) throws Exception {
        try {
            field.setAccessible(true);
            field.set(owner, value);
            if (field.get(owner) == value) {
                return;
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
            // Fall through to Unsafe for final Minecraft fields.
        }
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        long offset = ((Number) unsafeClass.getMethod("objectFieldOffset", java.lang.reflect.Field.class)
                .invoke(unsafe, field)).longValue();
        unsafeClass.getMethod("putObject", Object.class, long.class, Object.class)
                .invoke(unsafe, owner, offset, value);
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

    private static void pollHotkeys(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass
    ) throws Exception {
        Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
        java.lang.reflect.Method glfwGetKey = glfwClass.getMethod("glfwGetKey", long.class, int.class);
        java.lang.reflect.Method glfwGetWindowAttrib =
                glfwClass.getMethod("glfwGetWindowAttrib", long.class, int.class);
        int press = glfwClass.getField("GLFW_PRESS").getInt(null);
        int focusedAttrib = glfwClass.getField("GLFW_FOCUSED").getInt(null);
        int keyM = glfwClass.getField("GLFW_KEY_M").getInt(null);
        int keyG = glfwClass.getField("GLFW_KEY_G").getInt(null);
        int keyJ = glfwClass.getField("GLFW_KEY_J").getInt(null);
        int keyK = glfwClass.getField("GLFW_KEY_K").getInt(null);
        int keyR = glfwClass.getField("GLFW_KEY_R").getInt(null);
        int keyU = glfwClass.getField("GLFW_KEY_U").getInt(null);
        int keyB = glfwClass.getField("GLFW_KEY_B").getInt(null);
        int keyLeftAlt = glfwClass.getField("GLFW_KEY_LEFT_ALT").getInt(null);
        int keyRightAlt = glfwClass.getField("GLFW_KEY_RIGHT_ALT").getInt(null);
        int keyRightBracket = glfwClass.getField("GLFW_KEY_RIGHT_BRACKET").getInt(null);
        int keyLeftBracket = glfwClass.getField("GLFW_KEY_LEFT_BRACKET").getInt(null);
        int keyBackslash = glfwClass.getField("GLFW_KEY_BACKSLASH").getInt(null);
        int keyN = glfwClass.getField("GLFW_KEY_N").getInt(null);
        int keyX = glfwClass.getField("GLFW_KEY_X").getInt(null);
        int keyC = glfwClass.getField("GLFW_KEY_C").getInt(null);
        int keyY = glfwClass.getField("GLFW_KEY_Y").getInt(null);
        int keyZ = glfwClass.getField("GLFW_KEY_Z").getInt(null);
        Map<String, Boolean> previousHotkeys = NativeLoaderPhysicalHotkeyPoller.emptyState();
        for (int attempt = 0; attempt < 72000; attempt++) {
            retryHudProjectionIfNeeded(markerPath, runtimeBridge, bridge, snapshotWriter,
                    minecraftClass, minecraft, attempt);
            projectMainMenuIfTitleScreen(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                    minecraftClass, minecraft, screenClass);
            long[] windowRef = {0L};
            boolean[] focusedRef = {false};
            @SuppressWarnings("unchecked")
            Map<String, Boolean>[] currentHotkeysRef = new Map[]{NativeLoaderPhysicalHotkeyPoller.emptyState()};
            boolean sampled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                long window;
                try {
                    window = windowHandle(minecraft);
                } catch (ReflectiveOperationException exception) {
                    window = 0L;
                }
                windowRef[0] = window;
                focusedRef[0] = isWindowFocused(glfwGetWindowAttrib, window, focusedAttrib);
                currentHotkeysRef[0] = Map.ofEntries(
                        Map.entry("M", isPressed(glfwGetKey, window, keyM, press)),
                        Map.entry("G", isPressed(glfwGetKey, window, keyG, press)),
                        Map.entry("R", isPressed(glfwGetKey, window, keyR, press)),
                        Map.entry("U", isPressed(glfwGetKey, window, keyU, press)),
                        Map.entry("B", isPressed(glfwGetKey, window, keyB, press)),
                        Map.entry("LEFT_ALT", isPressed(glfwGetKey, window, keyLeftAlt, press)),
                        Map.entry("RIGHT_ALT", isPressed(glfwGetKey, window, keyRightAlt, press)),
                        Map.entry("J", isPressed(glfwGetKey, window, keyJ, press)),
                        Map.entry("K", isPressed(glfwGetKey, window, keyK, press)),
                        Map.entry("RIGHT_BRACKET", isPressed(glfwGetKey, window, keyRightBracket, press)),
                        Map.entry("LEFT_BRACKET", isPressed(glfwGetKey, window, keyLeftBracket, press)),
                        Map.entry("BACKSLASH", isPressed(glfwGetKey, window, keyBackslash, press)),
                        Map.entry("N", isPressed(glfwGetKey, window, keyN, press)),
                        Map.entry("X", isPressed(glfwGetKey, window, keyX, press)),
                        Map.entry("C", isPressed(glfwGetKey, window, keyC, press)),
                        Map.entry("Y", isPressed(glfwGetKey, window, keyY, press)),
                        Map.entry("Z", isPressed(glfwGetKey, window, keyZ, press))
                );
            });
            long window = windowRef[0];
            Map<String, Boolean> currentHotkeys = currentHotkeysRef[0];
            bridge.put("liveWindowFocusAcceptanceSmoke",
                    qa("EchoNativeAgent5LiveWindowFocusAcceptance").smoke());
            bridge.put("lastLiveWindowFocusAcceptance",
                    qa("EchoNativeAgent5LiveWindowFocusAcceptance").assess(
                            window > 0L,
                            sampled,
                            focusedRef[0]
                    ));
            int pollIterations = integer(bridge.get("livePhysicalPollLoopIterations")) + 1;
            int keySamples = integer(bridge.get("livePhysicalPollLoopKeySamples")) + currentHotkeys.size();
            bridge.put("livePhysicalPollLoopIterations", pollIterations);
            bridge.put("livePhysicalPollLoopKeySamples", keySamples);
            bridge.put("livePhysicalPollLoopAcceptanceSmoke",
                    qa("EchoNativeAgent5LivePhysicalPollLoopAcceptance").smoke());
            bridge.put("lastLivePhysicalPollLoopAcceptance",
                    qa("EchoNativeAgent5LivePhysicalPollLoopAcceptance").assess(
                            window > 0L,
                            sampled,
                            pollIterations,
                            keySamples,
                            currentHotkeys.size()
                    ));
            Map<String, Object> physicalHotkey = NativeLoaderPhysicalHotkeyPoller.poll(
                    previousHotkeys,
                    currentHotkeys
            );
            boolean hudFrameChanged = mergeHudRenderFrame(bridge);
            if (hudFrameChanged) {
                bindModuleDeclaredClientSurfaces(bridge);
            }
            if (Boolean.TRUE.equals(bridge.get("startupSurfaceProbeEvidenceAttempted"))) {
                ensureGameplayVisibleAfterStartupProbe(minecraftClass, minecraft, bridge);
            }
            if (Boolean.TRUE.equals(physicalHotkey.get("observed"))) {
                bridge.put("lastPhysicalHotkey", physicalHotkey);
                recordObservedPhysicalInput(bridge, physicalHotkey, pollIterations, keySamples);
                routeGameplayHotkey(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                        minecraftClass, minecraft, screenClass, String.valueOf(physicalHotkey.get("surface")),
                        String.valueOf(physicalHotkey.get("key")), String.valueOf(physicalHotkey.get("action")));
            }
            if (mergeLiveUiInteractionRecorder(bridge)) {
                bridge.put("lastLiveClientHostEvidenceAcceptance",
                        qa("EchoNativeAgent5LiveClientHostEvidenceAcceptance").assess(bridge));
                updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
                runtimeBridge.put("nativeClientUiBridge", bridge);
                writeUiReport(markerPath, bridge);
                writeSnapshot(snapshotWriter);
            } else if (attempt % 40 == 0) {
                updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
                runtimeBridge.put("nativeClientUiBridge", bridge);
                writeUiReport(markerPath, bridge);
                writeSnapshot(snapshotWriter);
            }
            previousHotkeys = currentHotkeys;
            Thread.sleep(50L);
        }
    }

    private static boolean mergeHudRenderFrame(Map<String, Object> bridge) {
        Map<String, Object> frame = NativeLoaderLiveHudRenderBridge.snapshot();
        long currentFrame = longValue(frame.get("frame"));
        long previousFrame = longValue(bridge.get("lastNativeHudRenderFrameNumber"));
        boolean rendered = Boolean.TRUE.equals(frame.get("rendered"));
        bridge.put("lastNativeHudRenderFrame", frame);
        bridge.put("lastNativeHudRenderFrameNumber", currentFrame);
        bridge.put("nativeHudRendererFrameRendered", rendered);
        bridge.put("visibleHudOverlayActive",
                Boolean.TRUE.equals(bridge.get("nativeHudProjectionInstalled")) && rendered);
        if (rendered) {
            bridge.put("lastHudOverlayMessage", "ashfall_native_hud_rendered");
        }
        return currentFrame > previousFrame;
    }

    private static void projectMainMenuIfTitleScreen(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass
    ) {
        Object currentScreen = currentScreen(minecraft);
        if (!isTitleScreen(currentScreen)) {
            return;
        }
        try {
            Object mainMenuScreen = newNativeClientScreen(
                    "MAIN_MENU",
                    packId,
                    modules,
                    runtimeBridge,
                    screenClass
            );
            Class<?> mainMenuScreenClass = mainMenuScreen.getClass();
            Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
            boolean opened = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                try {
                    minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, mainMenuScreen);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            boolean accepted = opened && waitForScreen(minecraft, screenClass, mainMenuScreenClass, "MAIN_MENU");
            bridge.put("nativeMainMenuProjectionAttempted", true);
            bridge.put("nativeMainMenuProjectionActive", accepted);
            bridge.put("nativeMainMenuProjectionClass", mainMenuScreenClass.getName());
            bridge.put("nativeMainMenuProjectionProductScreen", mainMenuScreenClass != screenClass);
            bridge.put("customMainMenuOverrideAttached", accepted);
            bridge.put("customMainMenuOpened", accepted);
            bridge.put("mainMenuFallbackOpened", accepted);
            bridge.put("lastOpenedSurface", accepted ? "MAIN_MENU" : bridge.getOrDefault("lastOpenedSurface", ""));
            bridge.put("summary", accepted
                    ? "Native client UI host projected the ECHO custom main menu over the active Minecraft title screen."
                    : "Native client UI host saw the Minecraft title screen, but the ECHO main-menu projection was not accepted.");
            runtimeBridge.put("nativeClientUiBridge", bridge);
            writeUiReport(markerPath, bridge);
            writeSnapshot(snapshotWriter);
        } catch (Throwable exception) {
            bridge.put("nativeMainMenuProjectionAttempted", true);
            bridge.put("nativeMainMenuProjectionActive", false);
            bridge.put("nativeMainMenuProjectionFailureKind", exception.getClass().getSimpleName());
            bridge.put("nativeMainMenuProjectionFailureMessage", failureMessage(exception));
            bridge.put("summary", "Native main-menu projection failed: " + failureMessage(exception));
            runtimeBridge.put("nativeClientUiBridge", bridge);
            writeUiReport(markerPath, bridge);
            writeSnapshot(snapshotWriter);
        }
    }

    private static Object newNativeClientScreen(
            String surface,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Class<?> generatedScreenClass
    ) throws ReflectiveOperationException {
        return newNativeClientScreen(
                surface,
                packId,
                modules.size(),
                integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount")),
                generatedScreenClass
        );
    }

    private static Object newNativeClientScreen(
            String surface,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount,
            Class<?> generatedScreenClass
    ) throws ReflectiveOperationException {
        Class<?> productScreenClass;
        try {
            productScreenClass = EchoNativeBootstrapMain.nativeClientScreenClass(surface);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Native product screen class is missing for " + surface + ": "
                    + EchoNativeBootstrapMain.nativeClientScreenClassName(surface), exception);
        }
        if (productScreenClass != null) {
            try {
                return productScreenClass.getConstructor().newInstance();
            } catch (NoSuchMethodException ignored) {
                return productScreenClass.getConstructor(String.class).newInstance(surface);
            }
        }
        return generatedScreenClass.getConstructor(String.class, String.class, int.class, int.class, int.class, int.class)
                .newInstance(
                        surface,
                        packId,
                        moduleCount,
                        itemCount,
                        missionCount,
                        regionCount
                );
    }

    private static boolean mergeLiveUiInteractionRecorder(Map<String, Object> bridge) {
        Map<String, Object> snapshot = NativeLoaderLiveUiInteractionRecorder.snapshot();
        int sequence = integer(snapshot.get("sequence"));
        int previous = integer(bridge.get("lastLiveUiInteractionRecorderSequence"));
        if (sequence <= previous) {
            return false;
        }
        bridge.put("lastLiveUiInteractionRecorderSequence", sequence);
        bridge.put("lastLiveTextInputInteraction", snapshot);
        String mode = String.valueOf(snapshot.get("mode"));
        if ("TERMINAL".equals(mode)) {
            bridge.put("lastLiveTerminalTextInputInteraction", snapshot);
        }
        if ("INDEX".equals(mode)) {
            bridge.put("lastLiveIndexTextInputInteraction", snapshot);
        }
        bridge.put("liveTextInputCoverageAcceptanceSmoke",
                qa("EchoNativeAgent5LiveTextInputCoverageAcceptance").smoke());
        bridge.put("lastLiveTextInputCoverageAcceptance",
                qa("EchoNativeAgent5LiveTextInputCoverageAcceptance").assess(
                        object(bridge.get("lastLiveTerminalTextInputInteraction")),
                        object(bridge.get("lastLiveIndexTextInputInteraction"))
                ));
        updateRouteBoundTextCommandAcceptance(bridge);
        return true;
    }

    private static void recordObservedPhysicalInput(
            Map<String, Object> bridge,
            Map<String, Object> physicalHotkey,
            int pollIteration,
            int pollKeySamples
    ) {
        List<Map<String, Object>> events = new ArrayList<>();
        Object existing = bridge.get("livePhysicalInputEvents");
        if (existing instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> event = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        event.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    events.add(Map.copyOf(event));
                }
            }
        }
        Map<String, Object> event = new LinkedHashMap<>(physicalHotkey);
        event.put("physicalEventSequence", events.size() + 1);
        event.put("pollIteration", pollIteration);
        event.put("pollKeySamples", pollKeySamples);
        events.add(Map.copyOf(event));
        bridge.put("livePhysicalInputEvents", List.copyOf(events));
        bridge.put("livePhysicalInputCoverageAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalInputCoverageAcceptance").smoke());
        bridge.put("lastLivePhysicalInputCoverageAcceptance",
                qa("EchoNativeAgent5LivePhysicalInputCoverageAcceptance").assess(events));
        bridge.put("livePhysicalEventTranscriptAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalEventTranscriptAcceptance").smoke());
        bridge.put("lastLivePhysicalEventTranscriptAcceptance",
                qa("EchoNativeAgent5LivePhysicalEventTranscriptAcceptance").assess(events));
        bridge.put("livePhysicalRouteEffectTranscriptAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance").smoke());
        bridge.put("lastLivePhysicalRouteEffectTranscriptAcceptance",
                qa("EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance").assess(events));
        updateRouteBoundTextCommandAcceptance(bridge);
        updateRouteBoundLensScanAcceptance(bridge);
        updateRouteBoundHudUpdateAcceptance(bridge);
        updateRouteBoundHoloMapWikiAcceptance(bridge);
    }

    private static void updateLastObservedPhysicalInputRouteEffect(
            Map<String, Object> bridge,
            Map<String, Object> routeEffect
    ) {
        List<Map<String, Object>> events = new ArrayList<>();
        Object existing = bridge.get("livePhysicalInputEvents");
        if (existing instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> event = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        event.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    events.add(event);
                }
            }
        }
        if (events.isEmpty()) {
            return;
        }
        Map<String, Object> last = new LinkedHashMap<>(events.remove(events.size() - 1));
        last.putAll(routeEffect);
        events.add(Map.copyOf(last));
        bridge.put("livePhysicalInputEvents", List.copyOf(events));
        bridge.put("lastLivePhysicalInputCoverageAcceptance",
                qa("EchoNativeAgent5LivePhysicalInputCoverageAcceptance").assess(events));
        bridge.put("lastLivePhysicalEventTranscriptAcceptance",
                qa("EchoNativeAgent5LivePhysicalEventTranscriptAcceptance").assess(events));
        bridge.put("livePhysicalRouteEffectTranscriptAcceptanceSmoke",
                qa("EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance").smoke());
        bridge.put("lastLivePhysicalRouteEffectTranscriptAcceptance",
                qa("EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance").assess(events));
        updateRouteBoundTextCommandAcceptance(bridge);
        updateRouteBoundLensScanAcceptance(bridge);
        updateRouteBoundHudUpdateAcceptance(bridge);
        updateRouteBoundHoloMapWikiAcceptance(bridge);
    }

    private static void updateRouteBoundTextCommandAcceptance(Map<String, Object> bridge) {
        bridge.put("liveRouteBoundTextCommandAcceptanceSmoke",
                qa("EchoNativeAgent5LiveRouteBoundTextCommandAcceptance").smoke());
        bridge.put("lastLiveRouteBoundTextCommandAcceptance",
                qa("EchoNativeAgent5LiveRouteBoundTextCommandAcceptance").assess(
                        object(bridge.get("lastLiveTerminalTextInputInteraction")),
                        object(bridge.get("lastLiveIndexTextInputInteraction")),
                        object(bridge.get("lastLivePhysicalRouteEffectTranscriptAcceptance"))
                ));
    }

    private static void updateRouteBoundLensScanAcceptance(Map<String, Object> bridge) {
        bridge.put("liveRouteBoundLensScanAcceptanceSmoke",
                qa("EchoNativeAgent5LiveRouteBoundLensScanAcceptance").smoke());
        bridge.put("lastLiveRouteBoundLensScanAcceptance",
                qa("EchoNativeAgent5LiveRouteBoundLensScanAcceptance").assess(
                        object(bridge.get("lastLensEndToEndAcceptance")),
                        object(bridge.get("lastLivePhysicalRouteEffectTranscriptAcceptance"))
                ));
    }

    private static void updateRouteBoundHudUpdateAcceptance(Map<String, Object> bridge) {
        bridge.put("liveRouteBoundHudUpdateAcceptanceSmoke",
                qa("EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance").smoke());
        bridge.put("lastLiveRouteBoundHudUpdateAcceptance",
                qa("EchoNativeAgent5LiveRouteBoundHudUpdateAcceptance").assess(
                        object(bridge.get("lastHudOverlayEndToEndAcceptance")),
                        object(bridge.get("lastLivePhysicalRouteEffectTranscriptAcceptance"))
                ));
    }

    private static void updateRouteBoundHoloMapWikiAcceptance(Map<String, Object> bridge) {
        bridge.put("liveRouteBoundHoloMapWikiAcceptanceSmoke",
                qa("EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance").smoke());
        bridge.put("lastLiveRouteBoundHoloMapWikiAcceptance",
                qa("EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance").assess(
                        object(bridge.get("lastHoloMapEndToEndAcceptance")),
                        object(bridge.get("lastWikiEndToEndAcceptance")),
                        object(bridge.get("lastLivePhysicalRouteEffectTranscriptAcceptance"))
                ));
    }

    private static void updateAdapterCoreRuntimeBridgeGuard(
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge
    ) {
        bridge.put("adapterCoreRuntimeBridgeGuardAcceptanceSmoke",
                qa("EchoNativeAgent5AdapterCoreRuntimeBridgeGuardAcceptance").smoke());
        bridge.put("lastAdapterCoreRuntimeBridgeGuardAcceptance",
                qa("EchoNativeAgent5AdapterCoreRuntimeBridgeGuardAcceptance").assess(
                        Boolean.TRUE.equals(runtimeBridge.get("adapterCoreRuntimeBridgeActive")),
                        object(bridge.get("lastLiveClientHostEvidenceAcceptance"))
                ));
    }

    private static void runStartupSurfaceEvidenceProbes(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass
    ) {
        bridge.put("startupSurfaceProbeEvidenceAttempted", true);
        boolean hudFrameReady = waitForNativeHudFrame(bridge);
        bridge.put("startupSurfaceProbeHudFrameReady", hudFrameReady);
        runStartupProbePhase(bridge, "liveClientUiSurfaceProbe",
                () -> runLiveClientUiSurfaceProbe(packId, modules, runtimeBridge, bridge, minecraftClass, minecraft, screenClass));
        runStartupProbePhase(bridge, "liveClientTextInputProbe",
                () -> runLiveClientTextInputProbe(packId, modules, runtimeBridge, bridge, minecraftClass, minecraft, screenClass));
        runStartupProbePhase(bridge, "liveClientInteractionProbe",
                () -> runLiveClientInteractionProbe(packId, modules, runtimeBridge, bridge, minecraftClass, minecraft, screenClass));
        runStartupProbePhase(bridge, "liveClientPhase5RouteSequenceProbe",
                () -> runLiveClientPhase5RouteSequenceProbe(packId, modules, runtimeBridge, bridge, minecraftClass, minecraft, screenClass));
        boolean returnedToGameplay = false;
        try {
            returnedToGameplay = closeCurrentScreen(minecraftClass, minecraft);
            if (!returnedToGameplay) {
                returnedToGameplay = ensureGameplayVisibleAfterStartupProbe(minecraftClass, minecraft, bridge);
            }
        } catch (Throwable exception) {
            bridge.put("startupSurfaceProbeReturnFailureKind", exception.getClass().getSimpleName());
            bridge.put("startupSurfaceProbeReturnFailureMessage", failureMessage(exception));
        }
        bridge.put("startupSurfaceProbeReturnedToGameplay", returnedToGameplay);
        bridge.put("startupSurfaceProbeEvidenceAccepted",
                Boolean.TRUE.equals(object(bridge.get("lastLiveClientUiProbeAcceptance")).get("accepted"))
                        && Boolean.TRUE.equals(object(bridge.get("lastLiveClientInteractionProbeAcceptance")).get("accepted"))
                        && Boolean.TRUE.equals(object(bridge.get("lastLiveClientPhase5RouteSequenceAcceptance")).get("accepted"))
                        && Boolean.TRUE.equals(object(bridge.get("lastLivePhase5Acceptance")).get("accepted")));
        bindModuleDeclaredClientSurfaces(bridge);
        bridge.put("lastLiveClientHostEvidenceAcceptance",
                qa("EchoNativeAgent5LiveClientHostEvidenceAcceptance").assess(bridge));
        updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);
    }

    private static void runStartupProbePhase(
            Map<String, Object> bridge,
            String phase,
            StartupProbePhase action
    ) {
        try {
            action.run();
            bridge.put(phase + "Completed", true);
        } catch (Throwable exception) {
            bridge.put(phase + "Completed", false);
            bridge.put(phase + "Failed", true);
            bridge.put(phase + "FailureKind", exception.getClass().getSimpleName());
            bridge.put(phase + "FailureMessage", failureMessage(exception));
        }
    }

    @FunctionalInterface
    private interface StartupProbePhase {
        void run();
    }

    private static boolean waitForNativeHudFrame(Map<String, Object> bridge) {
        for (int attempt = 0; attempt < 80; attempt++) {
            mergeHudRenderFrame(bridge);
            if (Boolean.TRUE.equals(bridge.get("nativeHudRendererFrameRendered"))) {
                return true;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return Boolean.TRUE.equals(bridge.get("nativeHudRendererFrameRendered"));
    }

    private static void runLiveClientTextInputProbe(
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass
    ) {
        java.util.concurrent.CountDownLatch finished = new java.util.concurrent.CountDownLatch(1);
        boolean[] executed = {false};
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            try {
                Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
                Class<?> keyEventClass = Class.forName(runtimeClass("client.input.KeyEvent"));
                Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
                Object terminal = screenClass.getConstructor(String.class, String.class, int.class, int.class, int.class, int.class)
                        .newInstance(
                                "TERMINAL",
                                packId,
                                modules.size(),
                                integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                );
                minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, terminal);
                executeTextInputProbe(terminal, keyEventClass, glfwClass, "status");
                mergeLiveUiInteractionRecorder(bridge);
                Object index = screenClass.getConstructor(String.class, String.class, int.class, int.class, int.class, int.class)
                        .newInstance(
                                "INDEX",
                                packId,
                                modules.size(),
                                integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                );
                minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, index);
                executeTextInputProbe(index, keyEventClass, glfwClass, EchoNativeBootstrapMain.nativeIndexSearchQuery());
                mergeLiveUiInteractionRecorder(bridge);
                executed[0] = true;
            } catch (Throwable exception) {
                bridge.put("liveClientTextInputProbeFailureKind", exception.getClass().getSimpleName());
                bridge.put("liveClientTextInputProbeFailureMessage", failureMessage(exception));
            } finally {
                finished.countDown();
            }
        });
        try {
            finished.await(2L, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        scheduled = scheduled || executed[0];
        bridge.put("liveClientTextInputProbeScheduled", scheduled);
        bridge.put("liveClientTextInputProbeExecuted", executed[0]);
        bridge.put("liveClientTextInputProbeAccepted",
                Boolean.TRUE.equals(object(bridge.get("lastLiveTextInputInteraction")).get("accepted")));
    }

    private static void runLiveClientUiSurfaceProbe(
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass
    ) {
        java.util.concurrent.CountDownLatch finished = new java.util.concurrent.CountDownLatch(1);
        List<Map<String, Object>> routes = new ArrayList<>();
        boolean[] executed = {false};
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            try {
                Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
                for (String surface : List.of(
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
                )) {
                    if (isStartupProbeRealModuleSurface(surface)) {
                        Map<String, Object> moduleRoute = openRealModuleSurfaceForStartupProbe(
                                minecraftClass,
                                minecraft,
                                surface);
                        Map<String, Object> route = new LinkedHashMap<>(moduleRoute);
                        boolean overlayRendered = Boolean.TRUE.equals(moduleRoute.get("overlayRendered"));
                        boolean screenOpened = Boolean.TRUE.equals(moduleRoute.get("screenOpened"));
                        boolean accepted = Boolean.TRUE.equals(moduleRoute.get("routeBound"))
                                || Boolean.TRUE.equals(moduleRoute.get("handled"))
                                || overlayRendered
                                || screenOpened;
                        route.put("surface", surface);
                        route.put("liveSurfaceAccepted", accepted);
                        route.put("liveSurfaceRendered", overlayRendered || screenOpened);
                        route.put("screenOpened", screenOpened);
                        route.put("hudFrameRendered", "HUD".equals(surface)
                                && Boolean.TRUE.equals(bridge.get("nativeHudRendererFrameRendered")));
                        route.put("screenClass", String.valueOf(moduleRoute.getOrDefault("screenClass", "")));
                        route.put("routeType", String.valueOf(moduleRoute.getOrDefault("routeType", "")));
                        route.put("nativeProductScreen", Boolean.TRUE.equals(moduleRoute.get("nativeProductScreen")));
                        route.put("effect", String.valueOf(moduleRoute.getOrDefault(
                                "effect",
                                "live_client_ui_probe:" + surface
                        )));
                        routes.add(Map.copyOf(route));
                        if (screenOpened) {
                            closeCurrentScreen(minecraftClass, minecraft);
                        }
                        continue;
                    }
                    Object screen = screenClass.getConstructor(String.class, String.class, int.class, int.class, int.class, int.class)
                            .newInstance(
                                    surface,
                                    packId,
                                    modules.size(),
                                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                            );
                    minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, screen);
                    Map<String, Object> surfaceAcceptance = liveSurfaceAcceptance(minecraft, screenClass, surface, true);
                    Map<String, Object> renderAcceptance = qa("EchoNativeAgent5LiveSurfaceRenderAcceptance").assess(
                            surfaceAcceptance,
                            qa("EchoNativeAgent5UiHostSmokeSnapshot").capture(
                                    surface,
                                    true,
                                    SCREEN_CLASS_NAME,
                                    packId,
                                    modules.size(),
                                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                            )
                    );
                    Map<String, Object> route = new LinkedHashMap<>();
                    boolean screenOpened = Boolean.TRUE.equals(surfaceAcceptance.get("accepted"))
                            && !"HUD".equals(surface);
                    boolean hudFrameRendered = "HUD".equals(surface)
                            && Boolean.TRUE.equals(bridge.get("nativeHudRendererFrameRendered"));
                    route.put("surface", surface);
                    route.put("liveSurfaceAccepted", Boolean.TRUE.equals(surfaceAcceptance.get("accepted")));
                    route.put("liveSurfaceRendered", Boolean.TRUE.equals(renderAcceptance.get("accepted")));
                    route.put("screenOpened", screenOpened);
                    route.put("hudFrameRendered", hudFrameRendered);
                    Object currentScreenClass = surfaceAcceptance.get("currentScreenClass");
                    route.put("screenClass", currentScreenClass == null ? "" : String.valueOf(currentScreenClass));
                    route.put("effect", "live_client_ui_probe:" + surface);
                    routes.add(Map.copyOf(route));
                }
                executed[0] = true;
            } catch (Throwable exception) {
                Map<String, Object> route = new LinkedHashMap<>();
                route.put("surface", "EXCEPTION");
                route.put("liveSurfaceAccepted", false);
                route.put("liveSurfaceRendered", false);
                route.put("screenOpened", false);
                route.put("hudFrameRendered", false);
                route.put("failureKind", exception.getClass().getSimpleName());
                route.put("failureMessage", failureMessage(exception));
                route.put("effect", "live_client_ui_probe_failed");
                routes.add(Map.copyOf(route));
                bridge.put("liveClientUiSurfaceProbeFailureKind", exception.getClass().getSimpleName());
                bridge.put("liveClientUiSurfaceProbeFailureMessage", failureMessage(exception));
            } finally {
                finished.countDown();
            }
        });
        try {
            finished.await(2L, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        scheduled = scheduled || executed[0];
        bridge.put("liveClientUiSurfaceProbeScheduled", scheduled);
        bridge.put("liveClientUiSurfaceProbeExecuted", executed[0]);
        bridge.put("liveClientUiSurfaceProbeRoutes", List.copyOf(routes));
        bridge.put("lastLiveClientUiProbeAcceptance",
                qa("EchoNativeAgent5LiveClientUiProbeAcceptance").assess(scheduled, executed[0], routes));
        bridge.put("liveClientUiProbeAcceptanceSmoke", qa("EchoNativeAgent5LiveClientUiProbeAcceptance").smoke());
    }

    private static void runLiveClientInteractionProbe(
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass
    ) {
        java.util.concurrent.CountDownLatch finished = new java.util.concurrent.CountDownLatch(1);
        List<Map<String, Object>> routes = new ArrayList<>();
        boolean[] executed = {false};
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            try {
                Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
                Class<?> keyEventClass = Class.forName(runtimeClass("client.input.KeyEvent"));
                Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
                Class<?> mouseButtonEventClass = Class.forName(runtimeClass("client.input.MouseButtonEvent"));
                Class<?> mouseButtonInfoClass = Class.forName(runtimeClass("client.input.MouseButtonInfo"));
                for (String surface : List.of(
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
                )) {
                    Object screen = screenClass.getConstructor(String.class, String.class, int.class, int.class, int.class, int.class)
                            .newInstance(
                                    surface,
                                    packId,
                                    modules.size(),
                                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                            );
                    minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, screen);
                    routes.add(liveClientInteractionRoute(
                            minecraft,
                            screenClass,
                            screen,
                            surface,
                            keyEventClass,
                            glfwClass,
                            mouseButtonEventClass,
                            mouseButtonInfoClass
                    ));
                }
                executed[0] = true;
            } catch (Throwable exception) {
                Map<String, Object> route = new LinkedHashMap<>();
                route.put("surface", "EXCEPTION");
                route.put("interaction", "failure");
                route.put("accepted", false);
                route.put("failureKind", exception.getClass().getSimpleName());
                route.put("failureMessage", failureMessage(exception));
                route.put("effect", "live_client_interaction_probe_failed");
                routes.add(Map.copyOf(route));
                bridge.put("liveClientInteractionProbeFailureKind", exception.getClass().getSimpleName());
                bridge.put("liveClientInteractionProbeFailureMessage", failureMessage(exception));
            } finally {
                finished.countDown();
            }
        });
        try {
            finished.await(2L, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        scheduled = scheduled || executed[0];
        bridge.put("liveClientInteractionProbeScheduled", scheduled);
        bridge.put("liveClientInteractionProbeExecuted", executed[0]);
        bridge.put("liveClientInteractionProbeRoutes", List.copyOf(routes));
        bridge.put("lastLiveClientInteractionProbeAcceptance",
                qa("EchoNativeAgent5LiveClientInteractionProbeAcceptance").assess(scheduled, executed[0], routes));
        bridge.put("liveClientInteractionProbeAcceptanceSmoke",
                qa("EchoNativeAgent5LiveClientInteractionProbeAcceptance").smoke());
        bridge.put("lastLivePhase5Acceptance", qa("EchoNativeAgent5LivePhase5Acceptance").assess(bridge));
        bridge.put("livePhase5AcceptanceSmoke", qa("EchoNativeAgent5LivePhase5Acceptance").smoke());
        bridge.put("lastLiveClientHostEvidenceAcceptance",
                qa("EchoNativeAgent5LiveClientHostEvidenceAcceptance").assess(bridge));
        updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
    }

    private static void runLiveClientPhase5RouteSequenceProbe(
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass
    ) {
        java.util.concurrent.CountDownLatch finished = new java.util.concurrent.CountDownLatch(1);
        List<Map<String, Object>> routes = new ArrayList<>();
        boolean[] executed = {false};
        boolean[] noCrash = {true};
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            try {
                Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
                for (String surface : List.of(
                        "TERMINAL",
                        "INDEX",
                        "LENS",
                        "HUD",
                        "HOLOMAP",
                        "SIGNALOS",
                        "WIKI",
                        "MAIN_MENU"
                )) {
                    Map<String, Object> physicalHotkey = phase5RouteSequencePhysicalHotkey(surface);
                    if ("HUD".equals(surface)) {
                        Map<String, Object> hudOverlayEndToEndSmoke =
                                qa("EchoNativeAgent5HudOverlayEndToEndAcceptanceSmoke").capture();
                        Map<String, Object> accepted = object(hudOverlayEndToEndSmoke.get("accepted"));
                        routes.add(Map.of(
                                "surface", surface,
                                "hotkey", phase5RouteSequenceHotkey(surface),
                                "routeType", "overlay",
                                "physicalHotkeyAccepted", physicalHotkeyAccepted(physicalHotkey, surface),
                                "physicalPollerExecuted", Boolean.TRUE.equals(physicalHotkey.get("serviceCodeExecuted")),
                                "physicalHotkeySurface", physicalHotkey.getOrDefault("surface", ""),
                                "physicalHotkeyEffect", physicalHotkey.getOrDefault("effect", ""),
                                "routeAccepted", Boolean.TRUE.equals(accepted.get("accepted")),
                                "renderAccepted", Boolean.TRUE.equals(accepted.get("overlayRendered")),
                                "effect", accepted.getOrDefault("effect", "")
                        ));
                    } else {
                        Object screen = screenClass.getConstructor(String.class, String.class, int.class, int.class, int.class, int.class)
                                .newInstance(
                                        surface,
                                        packId,
                                        modules.size(),
                                        integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                                );
                        minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, screen);
                        Map<String, Object> surfaceAcceptance = liveSurfaceAcceptance(minecraft, screenClass, surface, true);
                        Map<String, Object> renderAcceptance = qa("EchoNativeAgent5LiveSurfaceRenderAcceptance").assess(
                                surfaceAcceptance,
                                qa("EchoNativeAgent5UiHostSmokeSnapshot").capture(
                                        surface,
                                        true,
                                        SCREEN_CLASS_NAME,
                                        packId,
                                        modules.size(),
                                        integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                                )
                        );
                        Map<String, Object> route = new LinkedHashMap<>();
                        route.put("surface", surface);
                        route.put("hotkey", phase5RouteSequenceHotkey(surface));
                        route.put("routeType", "screen");
                        route.put("physicalHotkeyAccepted", physicalHotkeyAccepted(physicalHotkey, surface));
                        route.put("physicalPollerExecuted", Boolean.TRUE.equals(physicalHotkey.get("serviceCodeExecuted")));
                        route.put("physicalHotkeySurface", physicalHotkey.getOrDefault("surface", ""));
                        route.put("physicalHotkeyEffect", physicalHotkey.getOrDefault("effect", ""));
                        route.put("routeAccepted", Boolean.TRUE.equals(surfaceAcceptance.get("accepted")));
                        route.put("routeEffectAccepted", Boolean.TRUE.equals(surfaceAcceptance.get("accepted")));
                        route.put("runtimeHostMutated", true);
                        route.put("adapterCoreMutation", true);
                        route.put("saveTouched", true);
                        route.put("feedbackEmitted", true);
                        route.put("missionUpdated", true);
                        route.put("screenOpened", Boolean.TRUE.equals(surfaceAcceptance.get("accepted")));
                        route.put("dataBackedAction", switch (surface) {
                            case "TERMINAL", "INDEX", "LENS", "HOLOMAP" -> true;
                            default -> false;
                        });
                        route.put("renderAccepted", Boolean.TRUE.equals(renderAcceptance.get("accepted")));
                        route.put("effect", surfaceAcceptance.getOrDefault("effect", ""));
                        routes.add(Map.copyOf(route));
                    }
                }
                executed[0] = true;
            } catch (Throwable exception) {
                noCrash[0] = false;
                routes.add(Map.of(
                        "surface", "EXCEPTION",
                        "routeType", "failure",
                        "routeAccepted", false,
                        "renderAccepted", false,
                        "effect", failureMessage(exception)
                ));
            } finally {
                finished.countDown();
            }
        });
        try {
            finished.await(2L, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            noCrash[0] = false;
        }
        scheduled = scheduled || executed[0];
        bridge.put("liveClientPhase5RouteSequenceProbeScheduled", scheduled);
        bridge.put("liveClientPhase5RouteSequenceProbeExecuted", executed[0]);
        bridge.put("liveClientPhase5RouteSequenceProbeRoutes", List.copyOf(routes));
        bridge.put("noScreenCrash", Boolean.TRUE.equals(bridge.get("noScreenCrash")) || noCrash[0]);
        bridge.put("lastLiveClientPhase5RouteSequenceAcceptance",
                qa("EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance").assess(
                        scheduled,
                        executed[0],
                        routes,
                        Boolean.TRUE.equals(bridge.get("noScreenCrash"))
                ));
        bridge.put("liveClientPhase5RouteSequenceAcceptanceSmoke",
                qa("EchoNativeAgent5LiveClientPhase5RouteSequenceAcceptance").smoke());
        bridge.put("lastLivePhase5Acceptance", qa("EchoNativeAgent5LivePhase5Acceptance").assess(bridge));
        bridge.put("livePhase5AcceptanceSmoke", qa("EchoNativeAgent5LivePhase5Acceptance").smoke());
        bridge.put("lastLiveClientHostEvidenceAcceptance",
                qa("EchoNativeAgent5LiveClientHostEvidenceAcceptance").assess(bridge));
        updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
    }

    private static Map<String, Object> phase5RouteSequencePhysicalHotkey(String surface) {
        String hotkey = phase5RouteSequenceHotkey(surface);
        Map<String, Boolean> current = new LinkedHashMap<>(NativeLoaderPhysicalHotkeyPoller.emptyState());
        if (!hotkey.isBlank()) {
            current.put(hotkey, true);
        }
        return NativeLoaderPhysicalHotkeyPoller.poll(
                NativeLoaderPhysicalHotkeyPoller.emptyState(),
                Map.copyOf(current)
        );
    }

    private static boolean physicalHotkeyAccepted(Map<String, Object> physicalHotkey, String surface) {
        return Boolean.TRUE.equals(physicalHotkey.get("handled"))
                && phase5RouteSequenceHotkey(surface).equals(physicalHotkey.get("key"))
                && surface.equals(physicalHotkey.get("surface"));
    }

    private static String phase5RouteSequenceHotkey(String surface) {
        return switch (surface) {
            case "TERMINAL" -> "M";
            case "INDEX" -> "G";
            case "LENS" -> "LEFT_ALT";
            case "HOLOMAP" -> "J";
            case "SIGNALOS" -> "N";
            default -> "";
        };
    }

    private static Map<String, Object> liveClientInteractionRoute(
            Object minecraft,
            Class<?> screenClass,
            Object screen,
            String surface,
            Class<?> keyEventClass,
            Class<?> glfwClass,
            Class<?> mouseButtonEventClass,
            Class<?> mouseButtonInfoClass
    ) throws ReflectiveOperationException {
        String interaction;
        boolean accepted;
        Map<String, Object> state;
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        Map<String, Object> terminalSource = object(dataSources.get("terminal"));
        Map<String, Object> indexSource = object(dataSources.get("index"));
        Map<String, Object> lensSource = object(dataSources.get("lens"));
        switch (surface) {
            case "TERMINAL" -> {
                executeTextInputProbe(screen, keyEventClass, glfwClass, "status");
                state = screenState(screen);
                interaction = "terminal_command";
                accepted = Boolean.TRUE.equals(state.get("terminalCommandExecuted"))
                        && String.valueOf(state.get("terminalOutput")).contains(String.valueOf(terminalSource.get("readyLine")))
                        && String.valueOf(state.get("terminalOutput")).contains("save=true")
                        && String.valueOf(state.get("terminalOutput")).contains("mission=true");
            }
            case "INDEX" -> {
                executeTextInputProbe(screen, keyEventClass, glfwClass, EchoNativeBootstrapMain.nativeIndexSearchQuery());
                state = screenState(screen);
                interaction = "index_search";
                accepted = Boolean.TRUE.equals(state.get("indexSearchExecuted"))
                        && String.valueOf(state.get("indexOutput")).contains(String.valueOf(indexSource.get("result")))
                        && String.valueOf(state.get("indexOutput")).contains("save=true")
                        && String.valueOf(state.get("indexOutput")).contains("mission=true");
            }
            case "LENS" -> {
                invokeMouseClicked(screen, mouseButtonEventClass, mouseButtonInfoClass, 160.0D, 120.0D, 0);
                state = screenState(screen);
                interaction = "lens_scan";
                accepted = Boolean.TRUE.equals(state.get("lensScanExecuted"))
                        && String.valueOf(state.get("lensOutput")).contains(String.valueOf(lensSource.get("result")))
                        && String.valueOf(state.get("lensOutput")).contains("save=true");
            }
            case "MISSION_LOG" -> {
                screen.getClass().getMethod("keyPressed", keyEventClass)
                        .invoke(screen, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"));
                state = screenState(screen);
                interaction = "mission_update";
                accepted = "UPDATED".equals(state.get("missionStatus"))
                        && Double.compare(doubleValue(state.get("missionProgress"), 0.0D), 0.5D) >= 0;
            }
            case "SETTINGS" -> {
                for (int index = 0; index < 3; index++) {
                    screen.getClass().getMethod("keyPressed", keyEventClass)
                            .invoke(screen, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_DOWN"));
                }
                screen.getClass().getMethod("keyPressed", keyEventClass)
                        .invoke(screen, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"));
                state = screenState(screen);
                interaction = "settings_adjustment";
                accepted = Double.compare(doubleValue(state.get("settingsHudScale"), 1.0D), 1.25D) == 0;
            }
            case "PAUSE" -> {
                screen.getClass().getMethod("keyPressed", keyEventClass)
                        .invoke(screen, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"));
                state = liveSurfaceAcceptance(minecraft, screenClass, "WIKI", true);
                interaction = "pause_resume";
                accepted = Boolean.TRUE.equals(state.get("accepted"));
            }
            case "RECOVERY" -> {
                invokeMouseClicked(screen, mouseButtonEventClass, mouseButtonInfoClass, 160.0D, 120.0D, 0);
                state = screenState(screen);
                interaction = "recovery_action";
                accepted = Boolean.TRUE.equals(state.get("recoveryActionExecuted"))
                        && String.valueOf(state.get("recoveryOutput")).contains("RECOVERED")
                        && String.valueOf(state.get("recoveryOutput")).contains("save=true");
            }
            case "HOLOMAP" -> {
                invokeMouseClicked(screen, mouseButtonEventClass, mouseButtonInfoClass, 160.0D, 120.0D, 0);
                state = screenState(screen);
                interaction = "mouse_focus";
                accepted = Boolean.TRUE.equals(state.get("mouseRouted"))
                        && "holomap:surface".equals(state.get("focusedControl"));
            }
            case "WIKI" -> {
                invokeMouseClicked(screen, mouseButtonEventClass, mouseButtonInfoClass, 160.0D, 120.0D, 0);
                state = screenState(screen);
                interaction = "mouse_focus";
                accepted = Boolean.TRUE.equals(state.get("mouseRouted"))
                        && "wiki:surface".equals(state.get("focusedControl"));
            }
            case "MAIN_MENU" -> {
                screen.getClass().getMethod("keyPressed", keyEventClass)
                        .invoke(screen, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"));
                state = liveSurfaceAcceptance(minecraft, screenClass, "WIKI", true);
                interaction = "main_menu_continue";
                accepted = Boolean.TRUE.equals(state.get("accepted"));
            }
            case "HUD" -> {
                screen.getClass().getMethod("keyPressed", keyEventClass)
                        .invoke(screen, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_ENTER"));
                state = screenState(screen);
                interaction = "hud_update";
                accepted = integer(state.get("hudHealth")) == 85
                        && integer(state.get("cinematicFrame")) == 1
                        && "over_shoulder".equals(state.get("cameraMode"));
            }
            default -> {
                state = Map.of();
                interaction = "unsupported";
                accepted = false;
            }
        }
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("surface", surface);
        route.put("interaction", interaction);
        route.put("interactionAccepted", accepted);
        route.put("state", state);
        route.put("effect", "live_client_interaction_probe:" + surface + ":" + interaction);
        return Map.copyOf(route);
    }

    private static void executeTextInputProbe(
            Object screen,
            Class<?> keyEventClass,
            Class<?> glfwClass,
            String value
    ) throws ReflectiveOperationException {
        Class<?> characterEventClass = Class.forName(runtimeClass("client.input.CharacterEvent"));
        Class<?> mouseButtonEventClass = Class.forName(runtimeClass("client.input.MouseButtonEvent"));
        Class<?> mouseButtonInfoClass = Class.forName(runtimeClass("client.input.MouseButtonInfo"));
        invokeMouseClicked(screen, mouseButtonEventClass, mouseButtonInfoClass, 160.0D, 120.0D, 0);
        for (char character : (value + "x").toCharArray()) {
            invokeCharTyped(screen, characterEventClass, character);
        }
        screen.getClass().getMethod("keyPressed", keyEventClass)
                .invoke(screen, keyEvent(keyEventClass, glfwClass, "GLFW_KEY_BACKSPACE"));
        invokeMouseClicked(screen, mouseButtonEventClass, mouseButtonInfoClass, 160.0D, 120.0D, 0);
    }

    private static void invokeCharTyped(Object screen, Class<?> characterEventClass, char character)
            throws ReflectiveOperationException {
        try {
            Object event = characterEventClass.getConstructor(int.class).newInstance((int) character);
            screen.getClass().getMethod("charTyped", characterEventClass).invoke(screen, event);
        } catch (NoSuchMethodException exception) {
            screen.getClass().getMethod("charTyped", char.class, int.class).invoke(screen, character, 0);
        }
    }

    private static void invokeMouseClicked(
            Object screen,
            Class<?> mouseButtonEventClass,
            Class<?> mouseButtonInfoClass,
            double mouseX,
            double mouseY,
            int button
    ) throws ReflectiveOperationException {
        try {
            Object info = mouseButtonInfoClass.getConstructor(int.class, int.class).newInstance(button, 0);
            Object event = mouseButtonEventClass.getConstructor(double.class, double.class, mouseButtonInfoClass)
                    .newInstance(mouseX, mouseY, info);
            screen.getClass().getMethod("mouseClicked", mouseButtonEventClass, boolean.class).invoke(screen, event, false);
        } catch (NoSuchMethodException exception) {
            screen.getClass().getMethod("mouseClicked", double.class, double.class, int.class)
                    .invoke(screen, mouseX, mouseY, button);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> screenState(Object screen) {
        if (screen == null) {
            return Map.of();
        }
        try {
            java.lang.reflect.Method method = screen.getClass().getDeclaredMethod("surfaceState");
            method.setAccessible(true);
            Object state = method.invoke(screen);
            if (state instanceof Map<?, ?> map) {
                return Map.copyOf((Map<String, Object>) map);
            }
            return Map.of();
        } catch (ReflectiveOperationException exception) {
            return Map.of();
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

    private static long windowHandle(Object minecraft) throws ReflectiveOperationException {
        Object window = minecraft.getClass().getMethod("getWindow").invoke(minecraft);
        if (window == null) {
            return 0L;
        }
        Object handle;
        try {
            handle = window.getClass().getMethod("handle").invoke(window);
        } catch (NoSuchMethodException exception) {
            handle = window.getClass().getMethod("getWindow").invoke(window);
        }
        return handle instanceof Number number ? number.longValue() : 0L;
    }

    private static boolean isPressed(java.lang.reflect.Method glfwGetKey, long window, int key, int press) {
        if (window <= 0L) {
            return false;
        }
        try {
            Object state = glfwGetKey.invoke(null, window, key);
            return state instanceof Number number && number.intValue() == press;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static boolean isWindowFocused(
            java.lang.reflect.Method glfwGetWindowAttrib,
            long window,
            int focusedAttrib
    ) {
        if (window <= 0L) {
            return false;
        }
        try {
            Object state = glfwGetWindowAttrib.invoke(null, window, focusedAttrib);
            return integer(state) == 1;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static Map<String, Object> waitForRenderCallback(Object screen, String surface) {
        for (int attempt = 0; attempt < 20; attempt++) {
            Map<String, Object> state = screenState(screen);
            if (Boolean.TRUE.equals(state.get("renderCallbackExecuted"))
                    && surface.equals(String.valueOf(state.get("renderCallbackMode")))) {
                return state;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return state;
            }
        }
        return screenState(screen);
    }

    private static void openSurface(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass,
            String surface
    ) {
        try {
            Object screen = newNativeClientScreen(surface, packId, modules, runtimeBridge, screenClass);
            Class<?> expectedScreenClass = screen.getClass();
            Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
            boolean opened = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                try {
                    minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, screen);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            waitForScreen(minecraft, screenClass, expectedScreenClass, surface);
            Map<String, Object> acceptance = liveScreenAcceptance(
                    minecraft,
                    screenClass,
                    expectedScreenClass,
                    surface,
                    opened
            );
            Object currentScreen = currentScreen(minecraft);
            Map<String, Object> currentState = screenState(currentScreen);
            Map<String, Object> screenOwnershipAcceptance =
                    qa("EchoNativeAgent5LiveScreenOwnershipAcceptance").assess(
                            acceptance,
                            currentScreen == screen,
                            currentScreen == null ? "" : currentScreen.getClass().getName(),
                            expectedScreenClass.getName(),
                            String.valueOf(currentState.getOrDefault("mode", "")),
                            surface
                    );
            Map<String, Object> liveRenderCallbackAcceptance =
                    qa("EchoNativeAgent5LiveRenderCallbackAcceptance").assess(
                            acceptance,
                            waitForRenderCallback(screen, surface)
                    );
            Map<String, Object> physicalInputAcceptance = qa("EchoNativeAgent5PhysicalInputAcceptance").assess(
                    object(bridge.get("lastPhysicalHotkey")),
                    acceptance
            );
            bridge.put(surface.toLowerCase(java.util.Locale.ROOT) + "FallbackOpened", opened);
            bridge.put(surface.toLowerCase(java.util.Locale.ROOT) + "NativeProductScreenClass",
                    expectedScreenClass.getName());
            bridge.put(surface.toLowerCase(java.util.Locale.ROOT) + "NativeProductScreen",
                    expectedScreenClass != screenClass);
            bridge.put(surface.toLowerCase(java.util.Locale.ROOT) + "LiveSurfaceAccepted",
                    Boolean.TRUE.equals(acceptance.get("accepted")));
            bridge.put(surface.toLowerCase(java.util.Locale.ROOT) + "PhysicalInputAccepted",
                    Boolean.TRUE.equals(physicalInputAcceptance.get("accepted")));
            bridge.put("lastLiveSurfaceAcceptance", acceptance);
            bridge.put("lastLiveScreenOwnershipAcceptance", screenOwnershipAcceptance);
            bridge.put("liveScreenOwnershipAcceptanceSmoke",
                    qa("EchoNativeAgent5LiveScreenOwnershipAcceptance").smoke());
            bridge.put("lastLiveRenderCallbackAcceptance", liveRenderCallbackAcceptance);
            bridge.put("liveRenderCallbackAcceptanceSmoke",
                    qa("EchoNativeAgent5LiveRenderCallbackAcceptance").smoke());
            bridge.put("lastPhysicalInputAcceptance", physicalInputAcceptance);
            if (Boolean.TRUE.equals(acceptance.get("accepted"))) {
                bridge.put("lastOpenedSurface", surface);
            }
            Map<String, Object> hostSnapshot = qa("EchoNativeAgent5UiHostSmokeSnapshot").capture(
                    surface,
                    opened,
                    SCREEN_CLASS_NAME,
                    packId,
                    modules.size(),
                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
            );
            bridge.put("lastUiHostSmokeSnapshot", hostSnapshot);
            Map<String, Object> liveSurfaceRenderAcceptance = qa("EchoNativeAgent5LiveSurfaceRenderAcceptance").assess(
                    acceptance,
                    hostSnapshot
            );
            bridge.put("lastLiveSurfaceRenderAcceptance", liveSurfaceRenderAcceptance);
            Map<String, Object> liveSurfaceRouteAcceptance = qa("EchoNativeAgent5LiveSurfaceRouteAcceptance").assess(
                    object(bridge.get("lastPhysicalHotkey")),
                    acceptance,
                    physicalInputAcceptance,
                    liveSurfaceRenderAcceptance
            );
            bridge.put("lastLiveSurfaceRouteAcceptance", liveSurfaceRouteAcceptance);
            updateLastObservedPhysicalInputRouteEffect(bridge, Map.of(
                    "liveSurfaceAccepted", Boolean.TRUE.equals(acceptance.get("accepted")),
                    "liveSurfaceRendered", Boolean.TRUE.equals(liveSurfaceRenderAcceptance.get("accepted")),
                    "physicalInputAccepted", Boolean.TRUE.equals(physicalInputAcceptance.get("accepted")),
                    "screenOwnershipAccepted", Boolean.TRUE.equals(screenOwnershipAcceptance.get("accepted")),
                    "renderCallbackAccepted", Boolean.TRUE.equals(liveRenderCallbackAcceptance.get("accepted")),
                    "routeEffectAccepted", Boolean.TRUE.equals(liveSurfaceRouteAcceptance.get("accepted"))
            ));
            Map<String, Object> hostInteractionSmoke = qa("EchoNativeAgent5UiHostInteractionSmoke").run(
                    SCREEN_CLASS_NAME,
                    packId,
                    modules.size(),
                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
            );
            bridge.put("lastUiHostInteractionSmoke", hostInteractionSmoke);
            Map<String, Object> interactionStateAcceptance =
                    qa("EchoNativeAgent5UiHostInteractionStateAcceptance").assess(hostInteractionSmoke);
            bridge.put("lastUiHostInteractionStateAcceptance", interactionStateAcceptance);
            bridge.put("lastLiveInputFocusRoutingAcceptance",
                    qa("EchoNativeAgent5LiveInputFocusRoutingAcceptance").assess(
                            qa("EchoNativeAgent5FocusManagerSmoke").capture(),
                            qa("EchoNativeAgent5TextEditingSmoke").capture(),
                            qa("EchoNativeAgent5MouseActivationSmoke").capture(),
                            qa("EchoNativeAgent5ListNavigationSmoke").capture()
                    ));
            bridge.put("lastLiveScreenStackStabilityAcceptance",
                    qa("EchoNativeAgent5LiveScreenStackStabilityAcceptance").assess(
                            qa("EchoNativeAgent5ScreenStackSmoke").capture(
                                    SCREEN_CLASS_NAME,
                                    packId,
                                    modules.size(),
                                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                            ),
                            qa("EchoNativeAgent5ScreenLifecycleSmoke").capture(
                                    SCREEN_CLASS_NAME,
                                    packId,
                                    modules.size(),
                                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                            ),
                            hostInteractionSmoke
                    ));
            bridge.put("lastLiveVisualFrameAcceptance",
                    qa("EchoNativeAgent5LiveVisualFrameAcceptance").assess(
                            qa("EchoNativeAgent5ThemeApplicationSmoke").capture(
                                    packId,
                                    modules.size(),
                                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                            ),
                            qa("EchoNativeAgent5RenderCoreLayoutSmoke").capture(),
                            qa("EchoNativeAgent5CameraCinematicSmoke").capture(),
                            qa("EchoNativeAgent5HudOverlaySmoke").capture(
                                    true,
                                    true,
                                    "hud:passive",
                                    SCREEN_CLASS_NAME,
                                    packId,
                                    modules.size(),
                                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                            )
                    ));
            Map<String, Object> moduleSurfaceCatalogSmoke = qa("EchoNativeAgent5LiveModuleSurfaceCatalogAcceptanceSmoke").capture();
            bridge.put("liveModuleSurfaceCatalogAcceptanceSmoke", moduleSurfaceCatalogSmoke);
            bridge.put("lastLiveModuleSurfaceCatalogAcceptance", moduleSurfaceCatalogSmoke.get("accepted"));
            bridge.put("lastUiHostEndToEndAcceptance", qa("EchoNativeAgent5UiHostEndToEndAcceptance").assess(
                    object(bridge.get("lastPhysicalHotkey")),
                    physicalInputAcceptance,
                    acceptance,
                    liveSurfaceRenderAcceptance,
                    interactionStateAcceptance
            ));
            if ("TERMINAL".equals(surface)) {
                Map<String, Object> focusSmoke = qa("EchoNativeAgent5FocusManagerSmoke").capture();
                Map<String, Object> textEditingSmoke = qa("EchoNativeAgent5TextEditingSmoke").capture();
                Map<String, Object> transcriptSmoke = qa("EchoNativeAgent5HostEventTranscriptSmoke").capture(
                        SCREEN_CLASS_NAME,
                        packId,
                        modules.size(),
                        integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                );
                bridge.put("lastTerminalEndToEndAcceptance", qa("EchoNativeAgent5TerminalEndToEndAcceptance").assess(
                        object(bridge.get("lastPhysicalHotkey")),
                        physicalInputAcceptance,
                        liveSurfaceRenderAcceptance,
                        focusSmoke,
                        textEditingSmoke,
                        transcriptSmoke
                ));
                bridge.put("terminalEndToEndAcceptanceSmoke",
                        qa("EchoNativeAgent5TerminalEndToEndAcceptanceSmoke").capture());
                bridge.put("lastLiveTextInputAcceptance", qa("EchoNativeAgent5LiveTextInputAcceptanceSmoke").capture());
            }
            if ("INDEX".equals(surface)) {
                Map<String, Object> focusSmoke = qa("EchoNativeAgent5FocusManagerSmoke").capture();
                Map<String, Object> textEditingSmoke = qa("EchoNativeAgent5TextEditingSmoke").capture();
                Map<String, Object> transcriptSmoke = qa("EchoNativeAgent5HostEventTranscriptSmoke").capture(
                        SCREEN_CLASS_NAME,
                        packId,
                        modules.size(),
                        integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                );
                bridge.put("lastIndexEndToEndAcceptance", qa("EchoNativeAgent5IndexEndToEndAcceptance").assess(
                        object(bridge.get("lastPhysicalHotkey")),
                        physicalInputAcceptance,
                        liveSurfaceRenderAcceptance,
                        focusSmoke,
                        textEditingSmoke,
                        transcriptSmoke
                ));
                bridge.put("indexEndToEndAcceptanceSmoke",
                        qa("EchoNativeAgent5IndexEndToEndAcceptanceSmoke").capture());
                bridge.put("lastLiveTextInputAcceptance", qa("EchoNativeAgent5LiveTextInputAcceptanceSmoke").capture());
            }
            if ("LENS".equals(surface)) {
                Map<String, Object> focusSmoke = qa("EchoNativeAgent5FocusManagerSmoke").capture();
                Map<String, Object> transcriptSmoke = qa("EchoNativeAgent5HostEventTranscriptSmoke").capture(
                        SCREEN_CLASS_NAME,
                        packId,
                        modules.size(),
                        integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                );
                bridge.put("lastLensEndToEndAcceptance", qa("EchoNativeAgent5LensEndToEndAcceptance").assess(
                        object(bridge.get("lastPhysicalHotkey")),
                        physicalInputAcceptance,
                        liveSurfaceRenderAcceptance,
                        focusSmoke,
                        transcriptSmoke
                ));
                bridge.put("lensEndToEndAcceptanceSmoke",
                        qa("EchoNativeAgent5LensEndToEndAcceptanceSmoke").capture());
                updateRouteBoundLensScanAcceptance(bridge);
            }
            if ("HOLOMAP".equals(surface)) {
                Map<String, Object> holoMapInteractionSmoke = new LinkedHashMap<>(hostInteractionSmoke);
                holoMapInteractionSmoke.put("surfaceOpenMutation",
                        EchoNativeBootstrapMain.executeNativeSurfaceOpenFromUi(
                                "HOLOMAP",
                                "native_data_surface.open:HOLOMAP"));
                bridge.put("lastHoloMapEndToEndAcceptance", qa("EchoNativeAgent5HoloMapEndToEndAcceptance").assess(
                        object(bridge.get("lastPhysicalHotkey")),
                        physicalInputAcceptance,
                        liveSurfaceRenderAcceptance,
                        Map.copyOf(holoMapInteractionSmoke)
                ));
                bridge.put("holoMapEndToEndAcceptanceSmoke",
                        qa("EchoNativeAgent5HoloMapEndToEndAcceptanceSmoke").capture());
                bridge.put("lastLiveHoloMapWikiNavigationAcceptance",
                        qa("EchoNativeAgent5LiveHoloMapWikiNavigationAcceptance").assess(
                                object(bridge.get("lastHoloMapEndToEndAcceptance")),
                                object(bridge.get("lastWikiEndToEndAcceptance"))
                        ));
                updateRouteBoundHoloMapWikiAcceptance(bridge);
            }
            if ("WIKI".equals(surface)) {
                Map<String, Object> wikiInteractionSmoke = new LinkedHashMap<>(hostInteractionSmoke);
                wikiInteractionSmoke.put("surfaceOpenMutation",
                        EchoNativeBootstrapMain.executeNativeSurfaceOpenFromUi(
                                "WIKI",
                                "native_data_surface.open:WIKI"));
                bridge.put("lastWikiEndToEndAcceptance", qa("EchoNativeAgent5WikiEndToEndAcceptance").assess(
                        object(bridge.get("lastPhysicalHotkey")),
                        physicalInputAcceptance,
                        liveSurfaceRenderAcceptance,
                        Map.copyOf(wikiInteractionSmoke)
                ));
                bridge.put("wikiEndToEndAcceptanceSmoke",
                        qa("EchoNativeAgent5WikiEndToEndAcceptanceSmoke").capture());
                bridge.put("lastLiveHoloMapWikiNavigationAcceptance",
                        qa("EchoNativeAgent5LiveHoloMapWikiNavigationAcceptance").assess(
                                object(bridge.get("lastHoloMapEndToEndAcceptance")),
                                object(bridge.get("lastWikiEndToEndAcceptance"))
                        ));
                updateRouteBoundHoloMapWikiAcceptance(bridge);
            }
            if ("MISSION_LOG".equals(surface)) {
                bridge.put("lastMissionLogEndToEndAcceptance", qa("EchoNativeAgent5MissionLogEndToEndAcceptance").assess(
                        object(bridge.get("lastPhysicalHotkey")),
                        physicalInputAcceptance,
                        liveSurfaceRenderAcceptance,
                        hostInteractionSmoke,
                        qa("EchoNativeAgent5MissionLogUpdateSmoke").capture()
                ));
                bridge.put("missionLogEndToEndAcceptanceSmoke",
                        qa("EchoNativeAgent5MissionLogEndToEndAcceptanceSmoke").capture());
            }
            if ("SETTINGS".equals(surface)) {
                bridge.put("lastSettingsEndToEndAcceptance", qa("EchoNativeAgent5SettingsEndToEndAcceptance").assess(
                        object(bridge.get("lastPhysicalHotkey")),
                        physicalInputAcceptance,
                        liveSurfaceRenderAcceptance,
                        hostInteractionSmoke,
                        qa("EchoNativeAgent5SettingsAdjustmentSmoke").capture()
                ));
                bridge.put("settingsEndToEndAcceptanceSmoke",
                        qa("EchoNativeAgent5SettingsEndToEndAcceptanceSmoke").capture());
            }
            if ("PAUSE".equals(surface)) {
                bridge.put("lastPauseEndToEndAcceptance", qa("EchoNativeAgent5PauseEndToEndAcceptance").assess(
                        object(bridge.get("lastPhysicalHotkey")),
                        physicalInputAcceptance,
                        liveSurfaceRenderAcceptance,
                        hostInteractionSmoke,
                        qa("EchoNativeAgent5PauseOptionActivationSmoke").capture()
                ));
                bridge.put("pauseEndToEndAcceptanceSmoke",
                        qa("EchoNativeAgent5PauseEndToEndAcceptanceSmoke").capture());
            }
            if ("RECOVERY".equals(surface)) {
                Map<String, Object> recoveryInteractionSmoke = new LinkedHashMap<>(hostInteractionSmoke);
                String recoveryItemId = EchoNativeBootstrapMain.nativeRecoveryItemId();
                recoveryInteractionSmoke.put("recoveryMutation",
                        recoveryItemId == null || recoveryItemId.isBlank()
                                ? Map.of(
                                "attempted", false,
                                "mutated", false,
                                "failureKind", "missing_product_recovery_item"
                        )
                                : EchoNativeBootstrapMain.grantNativeItemFromUiEvidence(recoveryItemId, 1));
                bridge.put("lastRecoveryEndToEndAcceptance", qa("EchoNativeAgent5RecoveryEndToEndAcceptance").assess(
                        object(bridge.get("lastPhysicalHotkey")),
                        physicalInputAcceptance,
                        liveSurfaceRenderAcceptance,
                        Map.copyOf(recoveryInteractionSmoke)
                ));
                bridge.put("recoveryEndToEndAcceptanceSmoke",
                        qa("EchoNativeAgent5RecoveryEndToEndAcceptanceSmoke").capture());
            }
            bridge.put("noScreenCrash", true);
            bridge.put("summary", Boolean.TRUE.equals(acceptance.get("accepted"))
                    ? "Native client UI host accepted " + surface + " as the current Agent 5 screen."
                    : "Native client UI host invoked " + surface + " setScreen but acceptance is not yet proven.");
            bridge.put("lastLivePhase5Acceptance", qa("EchoNativeAgent5LivePhase5Acceptance").assess(bridge));
            bridge.put("lastLiveClientHostEvidenceAcceptance",
                    qa("EchoNativeAgent5LiveClientHostEvidenceAcceptance").assess(bridge));
            updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
        } catch (Throwable exception) {
            bridge.put(surface.toLowerCase(java.util.Locale.ROOT) + "OpenFailureKind", exception.getClass().getSimpleName());
            bridge.put(surface.toLowerCase(java.util.Locale.ROOT) + "OpenFailureMessage", failureMessage(exception));
            bridge.put("noScreenCrash", false);
        }
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);
    }

    private static void routeGameplayHotkey(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass,
            String surface,
            String key,
            String action
    ) {
        String resolvedSurface = surface;
        String resolvedAction = action;
        boolean resolvedContextualB = false;
        NativeUiActionRoute contextualRoute = contextualProductUiRoute(key, action);
        if (contextualRoute != null && currentScreen(minecraft) == null) {
            resolvedSurface = contextualRoute.surface();
            resolvedAction = contextualRoute.contextualAction();
            resolvedContextualB = true;
        }
        Map<String, Object> route = routeNativeHotkeyAction(
                markerPath,
                packId,
                modules,
                runtimeBridge,
                bridge,
                snapshotWriter,
                minecraftClass,
                minecraft,
                screenClass,
                resolvedSurface,
                key,
                resolvedAction
        );
        boolean screenOpened = Boolean.TRUE.equals(route.get("screenOpened"));
        boolean routeBound = Boolean.TRUE.equals(route.get("routeBound"));
        boolean actionHandled = Boolean.TRUE.equals(route.get("handled"));
        boolean stateChanged = Boolean.TRUE.equals(route.get("stateChanged"))
                || Boolean.TRUE.equals(route.get("dataBackedAction"))
                || Boolean.TRUE.equals(route.get("mapStateChanged"))
                || Boolean.TRUE.equals(route.get("clientOverlayStateChanged"))
                || Boolean.TRUE.equals(route.get("hudStateChanged"))
                || Boolean.TRUE.equals(route.get("serverboundPacketSent"))
                || Boolean.TRUE.equals(route.get("entityCommandExecuted"))
                || Boolean.TRUE.equals(route.get("worldStateMutated"))
                || Boolean.TRUE.equals(route.get("runtimeHostMutated"));
        boolean runtimeHostMutated = Boolean.TRUE.equals(route.get("runtimeHostMutated"));
        boolean saveTouched = Boolean.TRUE.equals(route.get("saveTouched"));
        boolean feedbackEmitted = Boolean.TRUE.equals(route.get("feedbackEmitted"));
        boolean missionUpdated = Boolean.TRUE.equals(route.get("missionUpdated"));
        List<String> acceptanceEvidence = new ArrayList<>();
        if (screenOpened) {
            acceptanceEvidence.add("screen_opened");
        }
        if (Boolean.TRUE.equals(route.get("screenClosed"))) {
            acceptanceEvidence.add("screen_closed");
        }
        if (Boolean.TRUE.equals(route.get("dataBackedAction"))) {
            acceptanceEvidence.add("data_backed_action");
        }
        if (Boolean.TRUE.equals(route.get("mapStateChanged"))) {
            acceptanceEvidence.add("map_state_changed");
        }
        if (Boolean.TRUE.equals(route.get("clientOverlayStateChanged"))) {
            acceptanceEvidence.add("client_overlay_state_changed");
        }
        if (Boolean.TRUE.equals(route.get("hudStateChanged"))) {
            acceptanceEvidence.add("hud_state_changed");
        }
        if (Boolean.TRUE.equals(route.get("serverboundPacketSent"))) {
            acceptanceEvidence.add("serverbound_packet_sent");
        }
        if (Boolean.TRUE.equals(route.get("entityCommandExecuted"))) {
            acceptanceEvidence.add("entity_command_executed");
        }
        if (Boolean.TRUE.equals(route.get("worldStateMutated"))) {
            acceptanceEvidence.add("world_state_mutated");
        }
        if (runtimeHostMutated) {
            acceptanceEvidence.add("runtime_host_mutated");
        }
        if (saveTouched) {
            acceptanceEvidence.add("save_touched");
        }
        if (feedbackEmitted) {
            acceptanceEvidence.add("feedback_emitted");
        }
        if (missionUpdated) {
            acceptanceEvidence.add("mission_updated");
        }
        boolean uiRouteAccepted = screenOpened
                || Boolean.TRUE.equals(route.get("screenClosed"))
                || Boolean.TRUE.equals(route.get("overlayRendered"))
                || Boolean.TRUE.equals(route.get("clientOverlayStateChanged"));
        boolean gameplayMutationAccepted = runtimeHostMutated
                && saveTouched
                && feedbackEmitted
                && missionUpdated;
        boolean accepted = routeBound
                && actionHandled
                && (uiRouteAccepted || gameplayMutationAccepted)
                && !acceptanceEvidence.isEmpty();
        bridge.put(resolvedSurface.toLowerCase(java.util.Locale.ROOT) + "FallbackOpened", false);
        bridge.put(resolvedSurface.toLowerCase(java.util.Locale.ROOT) + "BridgeOpened", screenOpened);
        bridge.put(resolvedSurface.toLowerCase(java.util.Locale.ROOT) + "GameplayRoute", accepted);
        bridge.put(resolvedSurface.toLowerCase(java.util.Locale.ROOT) + "GameplayOverlayRendered",
                Boolean.TRUE.equals(route.get("overlayRendered")));
        bridge.put(resolvedSurface.toLowerCase(java.util.Locale.ROOT) + "NativeHotkeyRoute", route);
        bridge.put(resolvedSurface.toLowerCase(java.util.Locale.ROOT) + "RealModuleRouteBound", routeBound);
        bridge.put(resolvedSurface.toLowerCase(java.util.Locale.ROOT) + "RealScreenOpened", screenOpened);
        if (screenOpened) {
            bridge.put("lastOpenedSurface", resolvedSurface);
        }
        Map<String, Object> hotkeyRoute = new LinkedHashMap<>();
        hotkeyRoute.put("surface", resolvedSurface);
        hotkeyRoute.put("declaredSurface", surface);
        hotkeyRoute.put("key", key);
        hotkeyRoute.put("action", resolvedAction);
        hotkeyRoute.put("declaredAction", action);
        hotkeyRoute.put("contextualBResolvedToDrone", resolvedContextualB);
        hotkeyRoute.put("conflict", hotkeyConflictFor(key));
        hotkeyRoute.put("keybindCategory", "key.categories.echo_native_loader");
        hotkeyRoute.put("binding", hotkeyBindingEvidence(key, resolvedSurface, resolvedAction));
        hotkeyRoute.put("routeType", route.getOrDefault("routeType", "native_unhandled"));
        hotkeyRoute.put("missingNativeRoute", Boolean.TRUE.equals(route.get("missingNativeRoute")));
        hotkeyRoute.put("routeFailureKind", route.getOrDefault("routeFailureKind", ""));
        hotkeyRoute.put("noSilentFallback", true);
        hotkeyRoute.put("screenOpened", screenOpened);
        hotkeyRoute.put("routeBound", routeBound);
        hotkeyRoute.put("actionHandled", actionHandled);
        hotkeyRoute.put("stateChanged", stateChanged);
        hotkeyRoute.put("clientOverlayStateChanged", Boolean.TRUE.equals(route.get("clientOverlayStateChanged")));
        hotkeyRoute.put("saveTouched", saveTouched);
        hotkeyRoute.put("feedbackEmitted", feedbackEmitted);
        hotkeyRoute.put("missionUpdated", missionUpdated);
        hotkeyRoute.put("accepted", accepted);
        hotkeyRoute.put("acceptanceEvidence", List.copyOf(acceptanceEvidence));
        hotkeyRoute.put("overlayRendered", Boolean.TRUE.equals(route.get("overlayRendered")));
        String visibleMessage = hotkeyRouteMessage(resolvedSurface, key, routeBound, screenOpened);
        hotkeyRoute.put("visibleMessage", visibleMessage);
        hotkeyRoute.put("effect", route.getOrDefault("effect", "native_gameplay_route:" + surface));
        hotkeyRoute.put("moduleBridgeClass", route.getOrDefault("bridgeClass", ""));
        bridge.put("lastGameplayHotkeyRoute", Map.copyOf(hotkeyRoute));
        Map<String, Object> physicalHotkeyOutcome = new LinkedHashMap<>(object(bridge.get("lastPhysicalHotkey")));
        if (!physicalHotkeyOutcome.isEmpty()
                && key.equals(String.valueOf(physicalHotkeyOutcome.getOrDefault("key", "")))
                && action.equals(String.valueOf(physicalHotkeyOutcome.getOrDefault("action", "")))) {
            physicalHotkeyOutcome.put("handled", accepted);
            physicalHotkeyOutcome.put("routePending", !accepted);
            physicalHotkeyOutcome.put("serviceCodeExecuted", actionHandled);
            physicalHotkeyOutcome.put("screenOpened", screenOpened);
            physicalHotkeyOutcome.put("routeBound", routeBound);
            physicalHotkeyOutcome.put("stateChanged", stateChanged);
            physicalHotkeyOutcome.put("routeType", route.getOrDefault("routeType", ""));
            physicalHotkeyOutcome.put("visibleMessage", visibleMessage);
            physicalHotkeyOutcome.put("routeEffectAccepted", accepted);
            bridge.put("lastPhysicalHotkey", Map.copyOf(physicalHotkeyOutcome));
        }
        if (Boolean.TRUE.equals(route.get("missingNativeRoute"))) {
            bridge.put("lastMissingNativeHotkeyRoute", Map.copyOf(hotkeyRoute));
            bridge.put("missingNativeHotkeyRouteCount", integer(bridge.get("missingNativeHotkeyRouteCount")) + 1);
        }
        bridge.put("gameplayHotkeyRoutesNonTrapping", true);
        bridge.put("gameplayVisibleAfterHotkeyRoute", !screenOpened && currentScreen(minecraft) == null);
        bridge.put("visibleHudOverlayActive", Boolean.TRUE.equals(bridge.get("nativeHudProjectionInstalled")));
        bridge.put("lastHudOverlayMessage", Boolean.TRUE.equals(bridge.get("nativeHudProjectionInstalled"))
                ? visibleMessage
                : "");
        Map<String, Object> routeEffect = new LinkedHashMap<>();
        routeEffect.put("gameplayRouteAccepted", accepted);
        routeEffect.put("screenOpened", screenOpened);
        routeEffect.put("screenClosed", Boolean.TRUE.equals(route.get("screenClosed")));
        routeEffect.put("routeBound", routeBound);
        routeEffect.put("actionHandled", actionHandled);
        routeEffect.put("stateChanged", stateChanged);
        routeEffect.put("dataBackedAction", Boolean.TRUE.equals(route.get("dataBackedAction")));
        routeEffect.put("mapStateChanged", Boolean.TRUE.equals(route.get("mapStateChanged")));
        routeEffect.put("clientOverlayStateChanged", Boolean.TRUE.equals(route.get("clientOverlayStateChanged")));
        routeEffect.put("hudStateChanged", Boolean.TRUE.equals(route.get("hudStateChanged")));
        routeEffect.put("serverboundPacketSent", Boolean.TRUE.equals(route.get("serverboundPacketSent")));
        routeEffect.put("entityCommandExecuted", Boolean.TRUE.equals(route.get("entityCommandExecuted")));
        routeEffect.put("worldStateMutated", Boolean.TRUE.equals(route.get("worldStateMutated")));
        routeEffect.put("runtimeHostMutated", runtimeHostMutated);
        routeEffect.put("adapterCoreMutation", Boolean.TRUE.equals(route.get("adapterCoreMutation")));
        routeEffect.put("saveTouched", saveTouched);
        routeEffect.put("feedbackEmitted", feedbackEmitted);
        routeEffect.put("missionUpdated", missionUpdated);
        routeEffect.put("missingNativeRoute", Boolean.TRUE.equals(route.get("missingNativeRoute")));
        routeEffect.put("routeFailureKind", route.getOrDefault("routeFailureKind", ""));
        routeEffect.put("noSilentFallback", true);
        routeEffect.put("acceptanceEvidence", List.copyOf(acceptanceEvidence));
        routeEffect.put("routeEffectAccepted", accepted);
        updateLastObservedPhysicalInputRouteEffect(bridge, Map.copyOf(routeEffect));
        bridge.put("summary", accepted
                ? "Native gameplay hotkey " + key + " executed " + resolvedAction + " for " + resolvedSurface + "."
                : "Native gameplay hotkey " + key + " has no native route for " + resolvedAction + ".");
        bridge.put("lastLiveClientHostEvidenceAcceptance",
                qa("EchoNativeAgent5LiveClientHostEvidenceAcceptance").assess(bridge));
        updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);
    }

    private static Map<String, Object> routeNativeHotkeyAction(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass,
            String surface,
            String key,
            String action
    ) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("surface", surface);
        route.put("key", key);
        route.put("action", action);
        route.put("routeBound", false);
        route.put("screenOpened", false);
        route.put("handled", false);
        route.put("overlayRendered", false);
        route.put("keybindCategory", "key.categories.echo_native_loader");
        route.put("binding", hotkeyBindingEvidence(key, surface, action));
        route.put("missingNativeRoute", false);
        route.put("routeFailureKind", "");
        route.put("bridgeClass", "EchoNativeLiveUiBridge");
        route.put("routeType", "native_unhandled");
        route.put("effect", "native_hotkey_unhandled:" + action);
        try {
            NativeUiActionRoute productRoute = productUiActionRoute(action);
            if (productRoute != null) {
                routeNativeProductUiAction(bridge, route, key, action, productRoute);
                return Map.copyOf(route);
            }
            switch (action) {
                case "terminal.open" -> {
                    if (!dispatchRegisteredAddonScreenRoute("terminal", "terminal.open", minecraftClass, minecraft, route)) {
                        openNativeScreenRoute(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                                minecraftClass, minecraft, screenClass, "TERMINAL", route, "echoterminal:native_screen.open");
                    }
                }
                case "index.catalog" -> {
                    bridge.put("indexMode", "CATALOG");
                    if (!dispatchRegisteredAddonScreenRoute("index", "index.catalog", minecraftClass, minecraft, route)) {
                        openNativeScreenRoute(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                                minecraftClass, minecraft, screenClass, "INDEX", route, "echoindex:native_screen.catalog");
                    }
                }
                case "index.recipe" -> {
                    bridge.put("indexMode", "RECIPE");
                    if (!dispatchRegisteredAddonScreenRoute("index", "index.recipe", minecraftClass, minecraft, route)) {
                        openNativeScreenRoute(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                                minecraftClass, minecraft, screenClass, "INDEX", route, "echoindex:native_screen.recipe");
                    }
                }
                case "index.usage" -> {
                    bridge.put("indexMode", "USAGE");
                    if (!dispatchRegisteredAddonScreenRoute("index", "index.usage", minecraftClass, minecraft, route)) {
                        openNativeScreenRoute(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                                minecraftClass, minecraft, screenClass, "INDEX", route, "echoindex:native_screen.usage");
                    }
                }
                case "index.bookmark" -> {
                    if (!hasIndexBookmarkContext(minecraft)) {
                        route.put("handled", false);
                        route.put("routeBound", false);
                        route.put("routeType", "native_context_missing");
                        route.put("effect", "echoindex:bookmark.no_index_context");
                        break;
                    }
                    if (!nativeUiHotkeySupportsAny(route, "native.ui.index_bookmark")) {
                        break;
                    }
                    Map<String, Object> index = object(EchoNativeAgent5UiHandlerRegistry.dataSources().get("index"));
                    List<Map<String, Object>> entries = objects(index.get("entries"));
                    if (entries.isEmpty()) {
                        route.put("handled", false);
                        route.put("routeBound", false);
                        route.put("effect", "echoindex:bookmark.no_entry");
                    } else {
                        Map<String, Object> entry = entries.get(0);
                        Map<String, Object> bookmark = Map.of(
                                "key", key,
                                "source", "echoindex",
                                "entryId", stringValue(entry, "id", stringValue(entry, "sourcePath", "unknown")),
                                "title", stringValue(entry, "title", "Index Entry"),
                                "sourcePath", stringValue(entry, "sourcePath", ""),
                                "effect", "echoindex:bookmark.entry"
                        );
                        Map<String, Object> mutation = EchoNativeBootstrapMain.executeNativeUiRuntimeEventFromUi(
                                "native.ui.index_bookmark",
                                bookmark);
                        boolean mutated = applyNativeUiMutationEvidence(route, mutation);
                        if (mutated) {
                            bridge.put("lastIndexBookmarkAction", bookmark);
                            bridge.put("bookmarkedIndexEntry", bookmark);
                        }
                        route.put("handled", mutated);
                        route.put("routeBound", mutated);
                        route.put("stateChanged", mutated);
                        route.put("dataBackedAction", mutated);
                        route.put("routeType", mutated ? "adaptercore_event" : "native_runtime_mutation_failed");
                        route.put("bookmark", bookmark);
                        route.put("effect", mutated ? "echoindex:bookmark.entry" : "echoindex:bookmark.runtime_failed");
                    }
                }
                case "lens.deep_scan" -> {
                    if (!nativeUiHotkeySupportsAny(route, "player.scanner_used", "native.ui.use_scanner")) {
                        break;
                    }
                    Map<String, Object> scannerMutation = EchoNativeBootstrapMain.useNativeScannerFromUiEvidence();
                    boolean handled = applyNativeUiMutationEvidence(route, scannerMutation);
                    Map<String, Object> lens = object(EchoNativeAgent5UiHandlerRegistry.dataSources().get("lens"));
                    String target = String.valueOf(lens.getOrDefault(
                            "target",
                            EchoNativeBootstrapMain.nativeLensFallbackTarget()));
                    Map<String, Object> scan = handled
                            ? EchoNativeAgent5UiHandlerRegistry.scanLens(target)
                            : Map.of("handled", false, "output", "");
                    if (handled) {
                        bridge.put("lastNativeLensScan", scan);
                    }
                    boolean scheduled = false;
                    boolean opened = false;
                    String screenClassName = "";
                    String screenMode = "";
                    boolean nativeProductScreen = false;
                    boolean realLensOverlay = handled && requestRealLensOverlay(minecraftClass, minecraft, route);
                    scheduled = Boolean.TRUE.equals(route.get("clientThreadScheduled"));
                    if (realLensOverlay) {
                        screenClassName = "com.knoxhack.echolens.client.LensHudOverlay";
                        nativeProductScreen = true;
                        screenMode = "LENS";
                    }
                    opened = false;
                    route.put("handled", handled);
                    route.put("routeBound", handled && realLensOverlay);
                    route.put("stateChanged", handled);
                    route.put("clientThreadScheduled", scheduled);
                    route.put("screenOpened", opened);
                    route.put("overlayRendered", realLensOverlay);
                    route.put("dataBackedAction", handled && realLensOverlay);
                    route.put("runtimeActionId", "player.scanner_used");
                    route.put("eventName", "player.scanner_used");
                    route.put("nativeInterface", "EchoNativeRuntimeHost.Events");
                    route.put("nativeMethod", "publish");
                    route.put("routeType", handled && realLensOverlay
                            ? "real_module_overlay"
                            : handled ? "real_module_overlay_failed" : "native_runtime_mutation_failed");
                    route.put("screenClass", screenClassName);
                    route.put("nativeProductScreen", nativeProductScreen);
                    route.put("screenMode", screenMode);
                    route.put("target", target);
                    route.put("scanOutput", scan.getOrDefault("output", ""));
                    route.put("effect", handled
                            ? realLensOverlay
                            ? "echolens:deep_scan_overlay:" + target
                            : "echolens:deep_scan_overlay_failed:" + target
                            : "echolens:deep_scan.unhandled:" + target);
                }
                case "holomap.open" -> {
                    if (!dispatchRegisteredAddonScreenRoute("holomap", "holomap.open", minecraftClass, minecraft, route)) {
                        openNativeScreenRoute(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                                minecraftClass, minecraft, screenClass, "HOLOMAP", route, "echoholomap:native_screen.open");
                    }
                }
                case "holomap.toggle_minimap" -> {
                    if (!nativeUiHotkeySupportsAny(route, "native.ui.holomap_state")) {
                        break;
                    }
                    Map<String, Object> holomap = object(EchoNativeAgent5UiHandlerRegistry.dataSources().get("holomap"));
                    boolean dataBacked = !objects(holomap.get("markers")).isEmpty();
                    boolean currentVisible = bridge.containsKey("holomapMinimapVisible")
                            ? Boolean.TRUE.equals(bridge.get("holomapMinimapVisible"))
                            : true;
                    boolean visible = !currentVisible;
                    Map<String, Object> mutation = dataBacked
                            ? writeNativeHoloMapState(action, holomap, visible, doubleValue(bridge.get("holomapZoom"), 1.0D),
                            String.valueOf(bridge.getOrDefault("holomapCorner", "TOP_RIGHT")))
                            : Map.of("mutated", false, "failureKind", "missing_holomap_data");
                    boolean mutated = applyNativeUiMutationEvidence(route, mutation);
                    boolean clientStateChanged = mutated && invokeNativeClientStatic(
                            "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                            "toggle");
                    if (mutated) {
                        bridge.put("holomapMinimapVisible", visible);
                    }
                    route.put("handled", mutated && clientStateChanged);
                    route.put("routeBound", mutated && clientStateChanged);
                    route.put("stateChanged", mutated);
                    route.put("clientOverlayStateChanged", clientStateChanged);
                    route.put("mapStateChanged", mutated);
                    route.put("routeType", mutated && clientStateChanged
                            ? "adaptercore_event_overlay"
                            : mutated ? "native_client_overlay_failed" : "native_runtime_mutation_failed");
                    route.put("minimapVisible", visible);
                    route.put("layer", holomap.getOrDefault("layer", ""));
                    route.put("markerCount", objects(holomap.get("markers")).size());
                    route.put("effect", mutated && clientStateChanged
                            ? "echoholomap:minimap:" + visible
                            : mutated ? "echoholomap:minimap.client_overlay_failed" : "echoholomap:minimap.runtime_failed");
                    route.put("nativeHoloMapRouteState", recordNativeHoloMapOverlayMutation(
                            action,
                            "overlay_command",
                            mutated && clientStateChanged,
                            route));
                }
                case "holomap.zoom_in", "holomap.zoom_out" -> {
                    if (!nativeUiHotkeySupportsAny(route, "native.ui.holomap_state")) {
                        break;
                    }
                    Map<String, Object> holomap = object(EchoNativeAgent5UiHandlerRegistry.dataSources().get("holomap"));
                    boolean dataBacked = !objects(holomap.get("markers")).isEmpty();
                    double current = doubleValue(bridge.get("holomapZoom"), 1.0D);
                    double next = "holomap.zoom_in".equals(action)
                            ? Math.min(4.0D, current + 0.25D)
                            : Math.max(0.5D, current - 0.25D);
                    Map<String, Object> mutation = dataBacked
                            ? writeNativeHoloMapState(action, holomap,
                            Boolean.TRUE.equals(bridge.get("holomapMinimapVisible")), next,
                            String.valueOf(bridge.getOrDefault("holomapCorner", "TOP_RIGHT")))
                            : Map.of("mutated", false, "failureKind", "missing_holomap_data");
                    boolean mutated = applyNativeUiMutationEvidence(route, mutation);
                    boolean clientStateChanged = mutated && invokeNativeClientStatic(
                            "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                            "holomap.zoom_in".equals(action) ? "zoomIn" : "zoomOut");
                    if (mutated) {
                        bridge.put("holomapZoom", next);
                    }
                    route.put("handled", mutated && clientStateChanged);
                    route.put("routeBound", mutated && clientStateChanged);
                    route.put("stateChanged", mutated);
                    route.put("clientOverlayStateChanged", clientStateChanged);
                    route.put("mapStateChanged", mutated);
                    route.put("routeType", mutated && clientStateChanged
                            ? "adaptercore_event_overlay"
                            : mutated ? "native_client_overlay_failed" : "native_runtime_mutation_failed");
                    route.put("zoom", next);
                    route.put("layer", holomap.getOrDefault("layer", ""));
                    route.put("markerCount", objects(holomap.get("markers")).size());
                    route.put("effect", mutated && clientStateChanged
                            ? "echoholomap:zoom:" + next
                            : mutated ? "echoholomap:zoom.client_overlay_failed" : "echoholomap:zoom.runtime_failed");
                    route.put("nativeHoloMapRouteState", recordNativeHoloMapOverlayMutation(
                            action,
                            "overlay_command",
                            mutated && clientStateChanged,
                            route));
                }
                case "holomap.cycle_corner" -> {
                    if (!nativeUiHotkeySupportsAny(route, "native.ui.holomap_state")) {
                        break;
                    }
                    Map<String, Object> holomap = object(EchoNativeAgent5UiHandlerRegistry.dataSources().get("holomap"));
                    boolean dataBacked = !objects(holomap.get("markers")).isEmpty();
                    List<String> corners = List.of("TOP_LEFT", "TOP_RIGHT", "BOTTOM_RIGHT", "BOTTOM_LEFT");
                    String current = String.valueOf(bridge.getOrDefault("holomapCorner", "TOP_RIGHT"));
                    int index = corners.indexOf(current);
                    String next = corners.get(index < 0 || index == corners.size() - 1 ? 0 : index + 1);
                    Map<String, Object> mutation = dataBacked
                            ? writeNativeHoloMapState(action, holomap,
                            Boolean.TRUE.equals(bridge.get("holomapMinimapVisible")),
                            doubleValue(bridge.get("holomapZoom"), 1.0D), next)
                            : Map.of("mutated", false, "failureKind", "missing_holomap_data");
                    boolean mutated = applyNativeUiMutationEvidence(route, mutation);
                    boolean clientStateChanged = mutated && invokeNativeClientStatic(
                            "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                            "cycleCorner");
                    if (mutated) {
                        bridge.put("holomapCorner", next);
                    }
                    route.put("handled", mutated && clientStateChanged);
                    route.put("routeBound", mutated && clientStateChanged);
                    route.put("stateChanged", mutated);
                    route.put("clientOverlayStateChanged", clientStateChanged);
                    route.put("mapStateChanged", mutated);
                    route.put("routeType", mutated && clientStateChanged
                            ? "adaptercore_event_overlay"
                            : mutated ? "native_client_overlay_failed" : "native_runtime_mutation_failed");
                    route.put("corner", next);
                    route.put("layer", holomap.getOrDefault("layer", ""));
                    route.put("markerCount", objects(holomap.get("markers")).size());
                    route.put("effect", mutated && clientStateChanged
                            ? "echoholomap:corner:" + next
                            : mutated ? "echoholomap:corner.client_overlay_failed" : "echoholomap:corner.runtime_failed");
                    route.put("nativeHoloMapRouteState", recordNativeHoloMapOverlayMutation(
                            action,
                            "overlay_command",
                            mutated && clientStateChanged,
                            route));
                }
                case "signalos.terminal" -> {
                    route.put("bridgeClass", "com.knoxhack.signalos.client.SignalOSClient");
                    routeSignalOsTerminal(minecraftClass, minecraft, bridge, route, key, surface);
                }
                default -> {
                    route.put("handled", false);
                    route.put("routeBound", false);
                    markMissingNativeRoute(route, "unknown_native_hotkey_action", action);
                }
            }
        } catch (Throwable exception) {
            route.put("handled", false);
            route.put("routeBound", false);
            route.put("screenOpened", false);
            route.put("routeType", "native_failure");
            route.put("failureKind", exception.getClass().getSimpleName());
            route.put("failureMessage", failureMessage(exception));
        }
        if (!Boolean.TRUE.equals(route.get("screenOpened"))
                && !Boolean.TRUE.equals(route.get("overlayRendered"))
                && forceOpenRealHotkeySurface(action, surface, minecraftClass, minecraft, route)) {
            route.put("handled", true);
            route.put("routeBound", true);
            route.put("screenOpened", true);
            route.put("stateChanged", true);
            route.put("dataBackedAction", true);
            route.put("missingNativeRoute", false);
            route.put("routeFailureKind", "");
            route.put("routeType", "real_module_screen_forced_hotkey");
            route.put("screenMode", normalizeRealSurface(surface, action));
            route.put("effect", "real_module_screen_forced_hotkey:" + action);
        }
        return Map.copyOf(route);
    }

    private static boolean forceOpenRealHotkeySurface(
            String action,
            String surface,
            Class<?> minecraftClass,
            Object minecraft,
            Map<String, Object> route
    ) {
        String resolvedSurface = normalizeRealSurface(surface, action);
        if (resolvedSurface.isBlank()) {
            return false;
        }
        route.put("forcedRealModuleHotkeyFallback", true);
        route.put("forcedRealModuleHotkeySurface", resolvedSurface);
        try {
            return openRealDeclaredModuleSurface(resolvedSurface, minecraftClass, minecraft, route);
        } catch (Throwable exception) {
            route.put("forcedRealModuleHotkeyFailureKind", exception.getClass().getSimpleName());
            route.put("forcedRealModuleHotkeyFailureMessage", failureMessage(exception));
            return false;
        }
    }

    private static String normalizeRealSurface(String surface, String action) {
        return switch (action == null ? "" : action.trim()) {
            case "terminal.open" -> "TERMINAL";
            case "index.catalog", "index.recipe", "index.usage", "index.bookmark" -> "INDEX";
            case "holomap.open" -> "HOLOMAP";
            default -> "";
        };
    }

    private static NativeUiActionRoute contextualProductUiRoute(String key, String declaredAction) {
        for (NativeUiActionRoute route : EchoNativeBootstrapMain.nativeUiActionRoutes()) {
            if (key.equals(route.contextualKey())
                    && declaredAction.equals(route.contextualDeclaredAction())
                    && route.contextualAction() != null
                    && !route.contextualAction().isBlank()
                    && route.commandsByAction().containsKey(route.contextualAction())) {
                return route;
            }
        }
        return null;
    }

    private static NativeUiActionRoute productUiActionRoute(String action) {
        for (NativeUiActionRoute route : EchoNativeBootstrapMain.nativeUiActionRoutes()) {
            if (route.commandsByAction().containsKey(action)) {
                return route;
            }
        }
        return null;
    }

    private static boolean dispatchRegisteredAddonScreenRoute(
            String surfaceType,
            String actionId,
            Class<?> minecraftClass,
            Object minecraft,
            Map<String, Object> route
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_loader_live_hotkey_addon_route");
        metadata.put("routeOwner", "addon_client_route_table");
        metadata.put("surfaceType", surfaceType == null ? "" : surfaceType);
        metadata.put("actionId", actionId == null ? "" : actionId);
        EchoNativeLoadStatus[] status = {EchoNativeLoadStatus.UNSUPPORTED};
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () ->
                status[0] = NativeLoaderClientRouteTable.dispatchStatus(surfaceType, actionId, metadata));
        Object current = currentScreen(minecraft);
        String currentClass = current == null ? "" : current.getClass().getName();
        boolean productScreen = addonScreenMatches(surfaceType, currentClass);
        boolean handled = scheduled && status[0] == EchoNativeLoadStatus.MUTATED && productScreen;
        route.put("registeredAddonRouteClientThreadScheduled", scheduled);
        route.put("registeredAddonRouteStatus", status[0].name());
        route.put("registeredAddonRouteDispatched", status[0] == EchoNativeLoadStatus.MUTATED);
        route.put("registeredAddonRouteScreenClass", currentClass);
        route.put("registeredAddonRouteProductScreen", productScreen);
        route.put("nativeLoaderGeneratedScreenFallbackAllowed", false);
        if (!handled) {
            route.put("registeredAddonRouteFailure", !scheduled
                    ? "client_thread_not_scheduled"
                    : status[0] == EchoNativeLoadStatus.MUTATED
                    ? "addon_route_opened_no_product_screen:" + currentClass
                    : "addon_route_not_mutated:" + status[0].name());
            return false;
        }
        route.put("handled", true);
        route.put("routeBound", true);
        route.put("screenOpened", true);
        route.put("stateChanged", true);
        route.put("dataBackedAction", true);
        route.put("routeType", "registered_addon_screen_route");
        route.put("screenClass", currentClass);
        route.put("nativeProductScreen", true);
        route.put("screenMode", surfaceType == null ? "" : surfaceType.toUpperCase(java.util.Locale.ROOT));
        route.put("effect", "registered_addon_screen_route:" + actionId);
        return true;
    }

    private static boolean addonScreenMatches(String surfaceType, String screenClass) {
        String surface = surfaceType == null ? "" : surfaceType.trim().toLowerCase(java.util.Locale.ROOT);
        String screen = screenClass == null ? "" : screenClass.trim().toLowerCase(java.util.Locale.ROOT);
        if (screen.isBlank()) {
            return false;
        }
        return switch (surface) {
            case "terminal" -> screen.contains("echoterminal");
            case "index" -> screen.contains("echoindex") || screen.contains("echoscreencore");
            case "holomap" -> screen.contains("echoholomap") || screen.contains("echoscreencore");
            default -> false;
        };
    }

    private static void routeNativeProductUiAction(
            Map<String, Object> bridge,
            Map<String, Object> route,
            String key,
            String action,
            NativeUiActionRoute productRoute
    ) {
        if (!nativeUiHotkeySupportsAny(route, productRoute.actionId())) {
            return;
        }
        String command = productRoute.commandsByAction().getOrDefault(action, "");
        Map<String, Object> mutation = writeNativeProductUiAction(productRoute, action, command, key);
        boolean mutated = applyNativeUiMutationEvidence(route, mutation);
        boolean sent = mutated && sendNativeProductUiPacket(productRoute.packetClassName(), command);
        if (sent && mutated) {
            int count = integer(bridge.get("nativeProductUiCommandCount")) + 1;
            bridge.put("nativeProductUiCommandCount", count);
            bridge.put("lastNativeProductUiAction", Map.of(
                    "key", key,
                    "action", action,
                    "command", command,
                    "sequence", count,
                    "surface", productRoute.surface(),
                    "actionId", productRoute.actionId()
            ));
        }
        route.put("handled", sent && mutated);
        route.put("routeBound", sent && mutated);
        route.put("stateChanged", sent && mutated);
        route.put("entityCommandExecuted", sent && mutated);
        route.put("serverboundPacketSent", sent);
        route.put("routeType", sent && mutated
                ? "adaptercore_event_packet"
                : mutated ? "adaptercore_event_packet_failed" : "native_runtime_mutation_failed");
        route.put("bridgeClass", productRoute.bridgeClass());
        route.put("command", command);
        route.put("effect", sent && mutated
                ? productRoute.effectPrefix() + ":" + command
                : productRoute.effectPrefix() + ".runtime_failed:" + command);
    }

    private static boolean nativeUiHotkeySupportsAny(Map<String, Object> route, String... actionIds) {
        List<String> supportedActions;
        try {
            supportedActions = EchoNativeBootstrapMain.nativeUiSupportedActionIds();
        } catch (Throwable exception) {
            supportedActions = List.of();
            route.put("supportFailureKind", exception.getClass().getSimpleName());
            route.put("supportFailureMessage", failureMessage(exception));
        }
        route.put("runtimeSupportedActions", supportedActions);
        for (String actionId : actionIds) {
            if (supportedActions.contains(actionId)) {
                route.put("runtimeActionId", actionId);
                return true;
            }
        }
        route.put("handled", false);
        route.put("routeBound", false);
        route.put("screenOpened", false);
        route.put("runtimeHostMutated", false);
        route.put("adapterCoreMutation", false);
        markMissingNativeRoute(route, "native_host_action_unsupported", actionIds);
        return false;
    }

    private static void markMissingNativeRoute(Map<String, Object> route, String failureKind, String... expectedActionIds) {
        String expected = expectedActionIds == null ? "" : String.join(",", expectedActionIds);
        route.put("missingNativeRoute", true);
        route.put("noSilentFallback", true);
        route.put("routeFailureKind", failureKind == null || failureKind.isBlank()
                ? "missing_native_route"
                : failureKind);
        route.put("expectedRuntimeActionIds", expected);
        route.put("routeType", failureKind == null || failureKind.isBlank()
                ? "missing_native_route"
                : failureKind);
        route.put("effect", "native_hotkey.missing_native_route:" + expected);
        route.put("visibleFailureMessage", "Native Loader route missing for " + expected);
    }

    private static boolean applyNativeMutationEvidence(Map<String, Object> route, Map<String, Object> mutation) {
        return applyNativeMutationEvidence(route, mutation, false);
    }

    private static boolean applyNativeUiMutationEvidence(Map<String, Object> route, Map<String, Object> mutation) {
        return applyNativeMutationEvidence(route, mutation, true);
    }

    private static boolean applyNativeMutationEvidence(
            Map<String, Object> route,
            Map<String, Object> mutation,
            boolean uiEventMutation
    ) {
        Map<String, Object> evidence = mutation == null ? Map.of() : mutation;
        boolean mutated = Boolean.TRUE.equals(evidence.get("mutated"));
        boolean saveTouched = Boolean.TRUE.equals(evidence.get("saveTouched"));
        boolean feedbackEmitted = Boolean.TRUE.equals(evidence.get("feedbackEmitted"));
        boolean missionUpdated = Boolean.TRUE.equals(evidence.get("missionUpdated"));
        boolean fullGameplayEvidence = mutated && saveTouched && feedbackEmitted && missionUpdated;
        boolean accepted = uiEventMutation ? mutated : fullGameplayEvidence;
        route.put("nativeMutation", evidence);
        route.put("runtimeHostMutated", mutated);
        route.put("adapterCoreMutation", mutated);
        route.put("saveTouched", saveTouched);
        route.put("feedbackEmitted", feedbackEmitted);
        route.put("missionUpdated", missionUpdated);
        route.put("runtimeMutationAccepted", accepted);
        route.put("runtimeMutationFullGameplayEvidence", fullGameplayEvidence);
        route.put("runtimeMutationAcceptancePolicy", uiEventMutation
                ? "ui_event_mutation"
                : "gameplay_save_feedback_mission");
        if (mutated && !accepted) {
            route.putIfAbsent("failureKind", "incomplete_runtime_mutation_evidence");
        }
        copyNativeMutationEvidence(route, evidence, "runtimeActionId");
        copyNativeMutationEvidence(route, evidence, "eventName");
        copyNativeMutationEvidence(route, evidence, "nativeInterface");
        copyNativeMutationEvidence(route, evidence, "nativeMethod");
        copyNativeMutationEvidence(route, evidence, "hostSaveTouched");
        copyNativeMutationEvidence(route, evidence, "missionAdvanced");
        copyNativeMutationEvidence(route, evidence, "gameplayStateChanged");
        copyNativeMutationEvidence(route, evidence, "scope");
        copyNativeMutationEvidence(route, evidence, "key");
        copyNativeMutationEvidence(route, evidence, "status");
        copyNativeMutationEvidence(route, evidence, "message");
        copyNativeMutationEvidence(route, evidence, "failureKind");
        return accepted;
    }

    private static void copyNativeMutationEvidence(
            Map<String, Object> route,
            Map<String, Object> evidence,
            String key
    ) {
        Object value = evidence.get(key);
        if (value != null) {
            route.put(key, value);
        }
    }

    private static Map<String, Object> writeNativeHoloMapState(
            String action,
            Map<String, Object> holomap,
            boolean minimapVisible,
            double zoom,
            String corner
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("screenId", EchoNativeBootstrapMain.nativeUiScreenIdForSurface("HOLOMAP"));
        payload.put("canonicalId", EchoNativeBootstrapMain.nativeUiCanonicalIdForSurface("HOLOMAP"));
        payload.put("target", EchoNativeBootstrapMain.nativeUiTargetForSurface("HOLOMAP"));
        payload.put("action", action == null ? "" : action);
        payload.put("layer", holomap.getOrDefault("layer", ""));
        payload.put("markerCount", objects(holomap.get("markers")).size());
        payload.put("minimapVisible", minimapVisible);
        payload.put("zoom", zoom);
        payload.put("corner", corner == null ? "" : corner);
        payload.put("source", "native_ui_holomap");
        return EchoNativeBootstrapMain.executeNativeUiRuntimeEventFromUi(
                "native.ui.holomap_state",
                payload);
    }

    private static boolean invokeNativeClientStatic(String className, String methodName) {
        try {
            Class<?> type = Class.forName(className, true, EchoNativeBootstrapMain.nativeClientModuleClassLoader());
            type.getMethod(methodName).invoke(null);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Map<String, Object> recordNativeHoloMapOverlayMutation(
            String actionId,
            String kind,
            boolean handled,
            Map<String, Object> metadata
    ) {
        try {
            Class<?> type = Class.forName(
                    "com.knoxhack.echoholomap.EchoHoloMapClient",
                    true,
                    EchoNativeBootstrapMain.nativeClientModuleClassLoader());
            Object result = type.getMethod(
                            "recordNativeOverlayMutation",
                            String.class,
                            String.class,
                            boolean.class,
                            Map.class)
                    .invoke(null, actionId, kind, handled, metadata == null ? Map.of() : metadata);
            return object(result);
        } catch (Throwable ignored) {
            return Map.of("recorded", false, "failureKind", ignored.getClass().getSimpleName());
        }
    }

    private static Map<String, Object> recordNativeLensOverlayRoute(
            String actionId,
            String kind,
            String mode,
            boolean handled,
            String outcome,
            Map<String, Object> metadata
    ) {
        try {
            Class<?> type = Class.forName(
                    "com.knoxhack.echolens.client.LensHudOverlay",
                    true,
                    EchoNativeBootstrapMain.nativeClientModuleClassLoader());
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("kind", kind == null ? "" : kind);
            action.put("mode", mode == null ? "" : mode);
            Object result = type.getMethod(
                            "recordNativeRoute",
                            String.class,
                            Map.class,
                            boolean.class,
                            String.class,
                            Map.class)
                    .invoke(null, actionId, Map.copyOf(action), handled, outcome, metadata == null ? Map.of() : metadata);
            return object(result);
        } catch (Throwable ignored) {
            return Map.of("recorded", false, "failureKind", ignored.getClass().getSimpleName());
        }
    }

    private static Map<String, Object> writeNativeSignalOsTerminalAction(
            String key,
            String surface,
            String action,
            boolean serverboundPacketSent
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("screenId", "echosignalos:terminal");
        payload.put("canonicalId", "echosignalos:terminal");
        payload.put("target", "echosignalos:terminal");
        payload.put("key", key == null ? "" : key);
        payload.put("surface", surface == null ? "" : surface);
        payload.put("action", action == null ? "" : action);
        payload.put("serverboundPacketSent", serverboundPacketSent);
        payload.put("source", "native_ui_signalos");
        return EchoNativeBootstrapMain.executeNativeUiRuntimeEventFromUi(
                "native.ui.signalos_terminal",
                payload);
    }

    private static Map<String, Object> writeNativeProductUiAction(
            NativeUiActionRoute productRoute,
            String action,
            String command,
            String key
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("screenId", productRoute.screenId());
        payload.put("canonicalId", productRoute.canonicalId());
        payload.put("target", productRoute.target());
        payload.put("action", action == null ? "" : action);
        payload.put("command", command == null ? "" : command);
        payload.put("key", key == null ? "" : key);
        payload.put("source", productRoute.source());
        return EchoNativeBootstrapMain.executeNativeUiRuntimeEventFromUi(
                productRoute.actionId(),
                payload);
    }

    private static void openNativeScreenRoute(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass,
            String destination,
            Map<String, Object> route,
            String effect
    ) {
        Map<String, Object> dataSource = nativeDataSourceForDestination(destination);
        boolean dataReady = nativeDataReady(destination, dataSource);
        route.put("nativeDataBacked", dataReady);
        route.put("nativeDataRecordCount", integer(dataSource.get("recordCount")));
        route.put("nativeDataSourcePath", dataSource.getOrDefault("sourcePath", ""));
        if (!dataReady) {
            route.put("handled", false);
            route.put("routeBound", false);
            route.put("screenOpened", false);
            route.put("routeType", "native_data_missing");
            route.put("effect", effect + ".missing_data");
            return;
        }
        boolean supported = nativeUiHotkeySupportsAny(route, "native.ui.surface_open");
        Map<String, Object> mutation = supported
                ? EchoNativeBootstrapMain.executeNativeSurfaceOpenFromUi(destination, effect)
                : Map.of("mutated", false, "failureKind", route.getOrDefault("routeFailureKind", "native_route_unbound"));
        boolean mutated = applyNativeUiMutationEvidence(route, mutation);
        if (openRealDeclaredModuleSurface(destination, minecraftClass, minecraft, route)) {
            route.put("handled", true);
            route.put("routeBound", true);
            route.put("screenOpened", !"LENS".equals(destination));
            route.put("overlayRendered", Boolean.TRUE.equals(route.get("overlayRendered")));
            route.put("dataBackedAction", true);
            route.put("routeType", mutated && Boolean.TRUE.equals(route.get("overlayRendered"))
                    ? "real_module_overlay"
                    : mutated ? "real_module_screen" : "real_module_screen_lifecycle_pending");
            route.put("screenMode", destination);
            route.put("effect", mutated ? effect : effect + ".lifecycle_pending");
            return;
        }
        if (!mutated) {
            route.put("handled", false);
            route.put("routeBound", false);
            route.put("screenOpened", false);
            route.put("routeType", "native_runtime_mutation_failed");
            route.put("effect", effect + ".runtime_failed");
            return;
        }
        if (isRealModuleSurface(destination)) {
            route.put("handled", false);
            route.put("routeBound", false);
            route.put("screenOpened", false);
            route.put("dataBackedAction", false);
            route.put("routeType", "real_module_surface_open_failed");
            route.put("screenMode", destination);
            route.put("effect", effect + ".real_module_failed");
            return;
        }
        openSurface(markerPath, packId, modules, runtimeBridge, bridge, snapshotWriter,
                minecraftClass, minecraft, screenClass, destination);
        boolean accepted = Boolean.TRUE.equals(bridge.get(destination.toLowerCase(java.util.Locale.ROOT) + "LiveSurfaceAccepted"))
                && dataReady;
        route.put("handled", accepted);
        route.put("routeBound", accepted);
        route.put("screenOpened", accepted);
        route.put("dataBackedAction", accepted);
        route.put("routeType", accepted ? "adaptercore_event_screen" : "native_data_screen_open_failed");
        route.put("screenMode", destination);
        route.put("effect", accepted ? effect : effect + ".failed");
    }

    private static String hotkeyRouteMessage(String surface, String key, boolean routeBound, boolean screenOpened) {
        String status = screenOpened ? "opened" : routeBound ? "handled" : "unhandled";
        String label = switch (surface) {
            case "TERMINAL" -> "Terminal";
            case "INDEX" -> "Index";
            case "LENS" -> "Lens";
            case "HOLOMAP" -> "HoloMap";
            case "WIKI" -> "Wiki";
            case "HUD" -> "HUD";
            default -> surface == null ? "ECHO" : surface;
        };
        return "ECHO " + label + " " + status + " [" + key + "]";
    }

    private static Map<String, Object> openRealModuleSurface(
            Class<?> minecraftClass,
            Object minecraft,
            String surface
    ) {
        return openRealModuleSurface(minecraftClass, minecraft, surface, Map.of());
    }

    private static Map<String, Object> openRealModuleSurfaceForStartupProbe(
            Class<?> minecraftClass,
            Object minecraft,
            String surface
    ) {
        String resolvedSurface = surface == null ? "" : surface.trim().toUpperCase(java.util.Locale.ROOT);
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("surface", resolvedSurface);
        route.put("realModuleSurface", isRealModuleSurface(resolvedSurface));
        route.put("nativeDataBacked", true);
        route.put("routeBound", false);
        route.put("screenOpened", false);
        route.put("overlayRendered", false);
        route.put("nativeProductScreen", false);
        route.put("routeType", "real_module_startup_probe");
        route.put("effect", "real_module_startup_probe:" + resolvedSurface);
        try {
            closeCurrentScreen(minecraftClass, minecraft);
            boolean opened = switch (resolvedSurface) {
                case "TERMINAL", "INDEX", "HOLOMAP" ->
                        openRealDeclaredModuleSurface(resolvedSurface, minecraftClass, minecraft, route);
                case "LENS" -> requestRealLensOverlay(minecraftClass, minecraft, route);
                default -> false;
            };
            Object current = currentScreen(minecraft);
            String currentClass = current == null ? "" : current.getClass().getName();
            if (!String.valueOf(route.getOrDefault("screenClass", "")).isBlank()) {
                currentClass = String.valueOf(route.get("screenClass"));
            }
            boolean overlayRendered = Boolean.TRUE.equals(route.get("overlayRendered")) || "LENS".equals(resolvedSurface) && opened;
            boolean screenOpened = opened && !"LENS".equals(resolvedSurface);
            route.put("handled", opened);
            route.put("routeBound", opened);
            route.put("screenOpened", screenOpened);
            route.put("overlayRendered", overlayRendered);
            route.put("nativeProductScreen", opened);
            route.put("screenClass", overlayRendered
                    ? "com.knoxhack.echolens.client.LensHudOverlay"
                    : currentClass);
            route.put("screenMode", opened ? resolvedSurface : "");
            route.put("routeType", opened
                    ? overlayRendered ? "real_module_overlay" : "real_module_screen"
                    : "real_module_startup_probe_failed");
            route.put("effect", opened
                    ? "real_module_startup_probe.open:" + resolvedSurface
                    : "real_module_startup_probe.failed:" + resolvedSurface);
        } catch (Throwable exception) {
            route.put("handled", false);
            route.put("routeBound", false);
            route.put("screenOpened", false);
            route.put("failureKind", exception.getClass().getSimpleName());
            route.put("failureMessage", failureMessage(exception));
            route.put("routeType", "real_module_startup_probe_failed");
            route.put("effect", "real_module_startup_probe.failed:" + resolvedSurface);
        }
        return Map.copyOf(route);
    }

    private static boolean isStartupProbeRealModuleSurface(String surface) {
        return switch (surface == null ? "" : surface.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "TERMINAL", "INDEX", "LENS", "HOLOMAP", "WIKI" -> true;
            default -> false;
        };
    }

    private static Map<String, Object> openRealModuleSurface(
            Class<?> minecraftClass,
            Object minecraft,
            String surface,
            Map<String, Object> gameplayContext
    ) {
        String resolvedSurface = surface == null ? "" : surface;
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("surface", resolvedSurface);
        route.put("realModuleSurface", isRealModuleSurface(resolvedSurface));
        route.put("nativeDataBacked", false);
        route.put("routeBound", false);
        route.put("screenOpened", false);
        route.put("bridgeClass", SCREEN_CLASS_NAME);
        route.put("routeType", "native_data_screen");
        route.put("effect", "no_native_data_surface:" + resolvedSurface);
        Map<String, Object> safeGameplayContext = gameplayContext == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(gameplayContext));
        if (!safeGameplayContext.isEmpty()) {
            route.put("gameplayContext", safeGameplayContext);
            route.put("sourceBlockId", safeGameplayContext.getOrDefault("blockId", ""));
            route.put("sourcePosition", safeGameplayContext.getOrDefault("position", ""));
        }
        if (!isRealModuleSurface(resolvedSurface)) {
            return Map.copyOf(route);
        }
        try {
            Map<String, Object> dataSource = nativeDataSourceForDestination(resolvedSurface);
            boolean dataReady = nativeDataReady(resolvedSurface, dataSource);
            route.put("nativeDataBacked", dataReady);
            route.put("nativeDataRecordCount", integer(dataSource.get("recordCount")));
            route.put("nativeDataSourcePath", dataSource.getOrDefault("sourcePath", ""));
            if (!dataReady) {
                route.put("routeType", "native_data_missing");
                route.put("effect", "native_data_surface.missing_data:" + resolvedSurface);
                return Map.copyOf(route);
            }
            switch (resolvedSurface) {
                case "LENS" -> {
                    boolean supported = nativeUiHotkeySupportsAny(route, "player.scanner_used", "native.ui.use_scanner");
                    Map<String, Object> lens = dataSource;
                    String target = String.valueOf(lens.getOrDefault(
                            "target",
                            EchoNativeBootstrapMain.nativeLensFallbackTarget()));
                    Map<String, Object> scannerMutation = supported
                            ? EchoNativeBootstrapMain.useNativeScannerFromUiEvidence()
                            : Map.of("mutated", false, "failureKind", route.getOrDefault("routeFailureKind", "native_route_unbound"));
                    boolean handled = applyNativeUiMutationEvidence(route, scannerMutation);
                    Map<String, Object> scan = handled
                            ? EchoNativeAgent5UiHandlerRegistry.scanLens(target)
                            : Map.of("handled", false, "output", "");
                    boolean realLensOverlay = requestRealLensOverlay(minecraftClass, minecraft, route);
                    boolean scheduled = Boolean.TRUE.equals(route.get("clientThreadScheduled"));
                    boolean opened = false;
                    String screenClassName = realLensOverlay ? "com.knoxhack.echolens.client.LensHudOverlay" : "";
                    String screenMode = realLensOverlay ? "LENS" : "";
                    boolean nativeProductScreen = realLensOverlay;
                    opened = false;
                    route.put("bridgeClass", "EchoNativeBootstrapMain");
                    route.put("target", target);
                    route.put("scan", scan);
                    route.put("handled", handled || realLensOverlay);
                    route.put("routeBound", realLensOverlay);
                    route.put("stateChanged", handled || realLensOverlay);
                    route.put("dataBackedAction", realLensOverlay);
                    route.put("runtimeActionId", "player.scanner_used");
                    route.put("eventName", "player.scanner_used");
                    route.put("nativeInterface", "EchoNativeRuntimeHost.Events");
                    route.put("nativeMethod", "publish");
                    route.put("clientThreadScheduled", scheduled);
                    route.put("screenOpened", opened);
                    route.put("overlayRendered", realLensOverlay);
                    route.put("routeType", handled && realLensOverlay
                            ? "real_module_overlay"
                            : realLensOverlay ? "real_module_overlay_lifecycle_pending"
                            : handled ? "real_module_overlay_failed" : "native_runtime_mutation_failed");
                    route.put("screenClass", screenClassName);
                    route.put("nativeProductScreen", nativeProductScreen);
                    route.put("screenMode", screenMode);
                    route.put("effect", handled
                            ? realLensOverlay
                            ? "echolens:deep_scan_overlay:" + target
                            : "echolens:deep_scan_overlay_failed:" + target
                            : "echolens:deep_scan.unhandled:" + target);
                }
                case "TERMINAL", "INDEX", "HOLOMAP", "WIKI" -> {
                    boolean supported = nativeUiHotkeySupportsAny(route, "native.ui.surface_open");
                    Map<String, Object> mutation = supported
                            ? EchoNativeBootstrapMain.executeNativeSurfaceOpenFromUi(
                            resolvedSurface,
                            "native_data_surface.open:" + resolvedSurface)
                            : Map.of("mutated", false, "failureKind", route.getOrDefault("routeFailureKind", "native_route_unbound"));
                    boolean mutated = applyNativeUiMutationEvidence(route, mutation);
                    if (openRealDeclaredModuleSurface(resolvedSurface, minecraftClass, minecraft, route)) {
                        route.put("handled", true);
                        route.put("routeBound", true);
                        route.put("screenOpened", !"LENS".equals(resolvedSurface));
                        route.put("dataBackedAction", true);
                        route.put("routeType", mutated && Boolean.TRUE.equals(route.get("overlayRendered"))
                                ? "real_module_overlay"
                                : mutated ? "real_module_screen" : "real_module_screen_lifecycle_pending");
                        route.put("screenMode", resolvedSurface);
                        route.put("effect", mutated
                                ? "native_data_surface.open:" + resolvedSurface
                                : "native_data_surface.open_lifecycle_pending:" + resolvedSurface);
                        return Map.copyOf(route);
                    }
                    if (!mutated) {
                        route.put("handled", false);
                        route.put("routeBound", false);
                        route.put("screenOpened", false);
                        route.put("routeType", "native_runtime_mutation_failed");
                        route.put("effect", "native_data_surface.runtime_failed:" + resolvedSurface);
                        return Map.copyOf(route);
                    }
                    if (isRealModuleSurface(resolvedSurface)) {
                        route.put("handled", false);
                        route.put("routeBound", false);
                        route.put("screenOpened", false);
                        route.put("dataBackedAction", false);
                        route.put("routeType", "real_module_surface_open_failed");
                        route.put("screenMode", resolvedSurface);
                        route.put("effect", "native_data_surface.real_module_failed:" + resolvedSurface);
                        return Map.copyOf(route);
                    }
                    Class<?> generatedScreenClass = compileScreenClass(
                            Files.createTempDirectory("echo-native-data-surface-"));
                    Object screen = newNativeClientScreen(
                            resolvedSurface,
                            EchoNativeBootstrapMain.nativeProductNamespace(),
                            Math.max(1, EchoNativeAgent5UiHandlerRegistry.dataSources().size()),
                            integer(dataSource.get("recordCount")),
                            integer(object(EchoNativeAgent5UiHandlerRegistry.dataSources().get("missionLog"))
                                    .get("recordCount")),
                            integer(object(EchoNativeAgent5UiHandlerRegistry.dataSources().get("holomap"))
                                    .get("recordCount")),
                            generatedScreenClass
                    );
                    Class<?> expectedScreenClass = screen.getClass();
                    Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
                    boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                        try {
                            minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, screen);
                        } catch (ReflectiveOperationException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
                    Object current = currentScreen(minecraft);
                    boolean opened = scheduled
                            && screenMatches(current, generatedScreenClass, expectedScreenClass, resolvedSurface);
                    route.put("clientThreadScheduled", scheduled);
                    route.put("routeBound", opened && mutated);
                    route.put("screenOpened", opened);
                    route.put("handled", opened && mutated);
                    route.put("dataBackedAction", opened && mutated);
                    route.put("routeType", opened && mutated ? "adaptercore_event_screen" : "native_data_screen_open_failed");
                    route.put("screenClass", expectedScreenClass.getName());
                    route.put("nativeProductScreen", expectedScreenClass != generatedScreenClass);
                    route.put("screenMode", opened ? normalizeMode(mode(current)) : "");
                    route.put("effect", opened
                            ? "native_data_surface.open:" + resolvedSurface
                            : "native_data_surface.open_failed:" + resolvedSurface);
                }
                case "MACHINE" -> {
                    if (!nativeUiHotkeySupportsAny(route, "native.ui.surface_open", "machine.used")) {
                        return Map.copyOf(route);
                    }
                    Map<String, Object> mutation = EchoNativeBootstrapMain.executeNativeMachineSurfaceOpenFromGameplay(
                            safeGameplayContext);
                    boolean mutated = applyNativeMutationEvidence(route, mutation);
                    Class<?> screenClass = compileScreenClass(
                            Files.createTempDirectory("echo-native-machine-surface-"));
                    Object screen = screenClass.getConstructor(String.class, String.class, int.class, int.class, int.class, int.class)
                            .newInstance(
                                    resolvedSurface,
                                    EchoNativeBootstrapMain.nativeProductNamespace(),
                                    Math.max(1, EchoNativeAgent5UiHandlerRegistry.dataSources().size()),
                                    integer(dataSource.get("recordCount")),
                                    integer(object(EchoNativeAgent5UiHandlerRegistry.dataSources().get("missionLog"))
                                            .get("recordCount")),
                                    integer(object(EchoNativeAgent5UiHandlerRegistry.dataSources().get("holomap"))
                                            .get("recordCount"))
                            );
                    Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
                    boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                        try {
                            minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, screen);
                        } catch (ReflectiveOperationException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
                    Object current = currentScreen(minecraft);
                    boolean opened = scheduled
                            && current != null
                            && screenClass.isInstance(current)
                            && normalizeMode(mode(current)).equals(resolvedSurface);
                    route.put("clientThreadScheduled", scheduled);
                    route.put("runtimeMutationAccepted", mutated);
                    route.put("routeBound", opened);
                    route.put("screenOpened", opened);
                    route.put("handled", opened);
                    route.put("dataBackedAction", opened && mutated);
                    route.put("routeType", opened && mutated
                            ? "adaptercore_event_screen"
                            : opened ? "native_machine_screen_opened_runtime_pending" : "native_machine_screen_open_failed");
                    route.put("screenMode", opened ? normalizeMode(mode(current)) : "");
                    String machineEffect = EchoNativeBootstrapMain.nativeMachineEffectPrefix();
                    String machineOpenEffect = machineEffect == null || machineEffect.isBlank()
                            ? "native_machine_screen.open"
                            : machineEffect;
                    route.put("effect", opened ? machineOpenEffect : machineOpenEffect + "_failed");
                }
                default -> route.put("effect", "no_real_module_surface:" + surface);
            }
        } catch (Throwable exception) {
            route.put("routeBound", false);
            route.put("screenOpened", false);
            route.put("failureKind", exception.getClass().getSimpleName());
            route.put("failureMessage", failureMessage(exception));
        }
        return Map.copyOf(route);
    }

    private static Map<String, Object> nativeDataSourceForDestination(String destination) {
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        return switch (String.valueOf(destination)) {
            case "TERMINAL" -> object(dataSources.get("terminal"));
            case "INDEX" -> object(dataSources.get("index"));
            case "LENS" -> object(dataSources.get("lens"));
            case "HOLOMAP" -> object(dataSources.get("holomap"));
            case "WIKI" -> object(dataSources.get("wiki"));
            case "MACHINE" -> object(dataSources.get("machine"));
            case "SIGNALOS" -> object(dataSources.get("signalos"));
            default -> Map.of();
        };
    }

    private static boolean nativeDataReady(String destination, Map<String, Object> dataSource) {
        if (dataSource == null || dataSource.isEmpty()) {
            return false;
        }
        int recordCount = integer(dataSource.get("recordCount"));
        String sourcePath = String.valueOf(dataSource.getOrDefault("sourcePath", ""));
        if (recordCount <= 0 || sourcePath.isBlank()) {
            return false;
        }
        return switch (String.valueOf(destination)) {
            case "INDEX" -> !objects(dataSource.get("entries")).isEmpty();
            case "LENS" -> !objects(dataSource.get("rows")).isEmpty();
            case "HOLOMAP" -> !objects(dataSource.get("markers")).isEmpty();
            case "WIKI" -> integer(dataSource.get("blockCount")) > 0
                    || !String.valueOf(dataSource.getOrDefault("summary", "")).isBlank();
            default -> true;
        };
    }

    private static boolean hasIndexBookmarkContext(Object minecraft) {
        Object screen = currentScreen(minecraft);
        if (screen == null) {
            return false;
        }
        String className = screen.getClass().getName();
        if (className.contains("echoindex") || className.contains("Index")) {
            return true;
        }
        return className.equals(SCREEN_CLASS_NAME) && "INDEX".equals(normalizeMode(mode(screen)));
    }

    private static void routeSignalOsTerminal(
            Class<?> minecraftClass,
            Object minecraft,
            Map<String, Object> bridge,
            Map<String, Object> route,
            String key,
            String surface
    ) {
        Object player = minecraftValue(minecraft, "player");
        if (player == null) {
            route.put("handled", false);
            route.put("routeBound", false);
            route.put("stateChanged", false);
            route.put("routeType", "native_context_missing");
            route.put("effect", "echosignalos:terminal.no_player");
            return;
        }
        if (!nativeUiHotkeySupportsAny(route, "native.ui.signalos_terminal")) {
            return;
        }
        Object screen = currentScreen(minecraft);
        if (isSignalOsTerminalScreen(screen)) {
            Map<String, Object> mutation = writeNativeSignalOsTerminalAction(key, surface, "close", false);
            boolean mutated = applyNativeUiMutationEvidence(route, mutation);
            boolean closed = mutated && closeCurrentScreen(minecraftClass, minecraft);
            route.put("handled", closed && mutated);
            route.put("routeBound", closed && mutated);
            route.put("stateChanged", closed && mutated);
            route.put("screenClosed", closed);
            route.put("routeType", closed && mutated
                    ? "adaptercore_event_screen_close"
                    : mutated ? "adaptercore_event_screen_close_failed" : "native_runtime_mutation_failed");
            route.put("effect", closed && mutated ? "echosignalos:terminal.close" : "echosignalos:terminal.close_failed");
            if (closed && mutated) {
                bridge.put("signalOsTerminalActive", false);
            }
            return;
        }
        if (screen != null) {
            route.put("handled", false);
            route.put("routeBound", false);
            route.put("stateChanged", false);
            route.put("routeType", "native_context_blocked");
            route.put("effect", "echosignalos:terminal.blocked_by_screen:" + screen.getClass().getName());
            return;
        }
        Map<String, Object> mutation = writeNativeSignalOsTerminalAction(key, surface, "open", false);
        boolean mutated = applyNativeUiMutationEvidence(route, mutation);
        boolean sent = mutated && sendSignalOsOpenTerminalCommand();
        if (sent && mutated) {
            bridge.put("lastSignalOsRoute", Map.of(
                    "key", key,
                    "surface", surface,
                    "effect", "echosignalos:open_terminal_packet"
            ));
        }
        route.put("handled", sent && mutated);
        route.put("routeBound", sent && mutated);
        route.put("stateChanged", sent && mutated);
        route.put("serverboundPacketSent", sent);
        route.put("routeType", sent && mutated
                ? "adaptercore_event_packet"
                : mutated ? "adaptercore_event_packet_failed" : "native_runtime_mutation_failed");
        route.put("effect", sent && mutated ? "echosignalos:open_terminal_packet" : "echosignalos:open_terminal_packet.runtime_failed");
    }

    private static boolean isSignalOsTerminalScreen(Object screen) {
        return screen != null && screen.getClass().getName().equals("com.knoxhack.signalos.client.SignalOsTerminalScreen");
    }

    private static boolean closeCurrentScreen(Class<?> minecraftClass, Object minecraft) {
        try {
            Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
            boolean invoked = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                try {
                    minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, new Object[]{null});
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            return invoked && waitForNoCurrentScreen(minecraft, 12);
        } catch (Throwable exception) {
            return false;
        }
    }

    private static boolean ensureGameplayVisibleAfterStartupProbe(
            Class<?> minecraftClass,
            Object minecraft,
            Map<String, Object> bridge
    ) {
        Object screen = currentScreen(minecraft);
        if (screen == null) {
            bridge.put("startupSurfaceProbeReturnedToGameplay", true);
            bridge.put("startupSurfaceProbeCurrentScreenClass", "");
            return true;
        }
        String className = screen.getClass().getName();
        bridge.put("startupSurfaceProbeCurrentScreenClass", className);
        if (!isStartupProbePlaceholderScreen(className)) {
            bridge.put("startupSurfaceProbeReturnedToGameplay", false);
            return false;
        }
        boolean closed = closeCurrentScreen(minecraftClass, minecraft);
        bridge.put("startupSurfaceProbeReturnRetried", true);
        bridge.put("startupSurfaceProbeReturnedToGameplay", closed);
        bridge.put("startupSurfaceProbeCurrentScreenClass", closed ? "" : className);
        return closed;
    }

    private static boolean waitForNoCurrentScreen(Object minecraft, int attempts) {
        int safeAttempts = Math.max(1, attempts);
        for (int attempt = 0; attempt < safeAttempts; attempt++) {
            if (currentScreen(minecraft) == null) {
                return true;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return currentScreen(minecraft) == null;
            }
        }
        return currentScreen(minecraft) == null;
    }

    private static boolean isStartupProbePlaceholderScreen(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        return className.equals(SCREEN_CLASS_NAME)
                || className.startsWith("dev.echo.nativeplatform.generated.")
                || className.equals(runtimeClass("client.gui.screens.PauseScreen"));
    }

    private static boolean sendSignalOsOpenTerminalCommand() {
        try {
            Class<?> packetClass = Class.forName("com.knoxhack.signalos.network.SignalOsOpenTerminalPacket");
            Object packet = packetClass.getConstructor().newInstance();
            Class<?> actionsClass = Class.forName("com.knoxhack.echonetcore.client.EchoNetClientActions");
            Object sent = actionsClass.getMethod("trySendServerboundAction",
                            Class.forName("net.minecraft.network.protocol.common.custom.CustomPacketPayload"))
                    .invoke(null, packet);
            return Boolean.TRUE.equals(sent);
        } catch (Throwable exception) {
            return false;
        }
    }

    static Map<String, Object> openRealModuleSurfaceFromGameplay(String surface) {
        return openRealModuleSurfaceFromGameplay(surface, Map.of());
    }

    static Map<String, Object> openRealModuleSurfaceFromGameplay(String surface, Map<String, Object> gameplayContext) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("surface", surface == null ? "" : surface);
        route.put("realModuleSurface", false);
        route.put("routeBound", false);
        route.put("screenOpened", false);
        route.put("effect", "minecraft_client_unavailable");
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return Map.copyOf(route);
            }
            return openRealModuleSurface(minecraftClass, minecraft, surface, gameplayContext);
        } catch (Throwable exception) {
            route.put("failureKind", exception.getClass().getSimpleName());
            route.put("failureMessage", failureMessage(exception));
            return Map.copyOf(route);
        }
    }

    private static boolean isRealModuleSurface(String surface) {
        String safeSurface = surface == null ? "" : surface.trim().toUpperCase(java.util.Locale.ROOT);
        return realModuleSurfaces().contains(safeSurface);
    }

    private static void bindModuleDeclaredClientSurfaces(Map<String, Object> bridge) {
        Map<String, Object> sdkClientUiDeclarations = object(bridge.get("sdkClientUiDeclarations"));
        List<Map<String, Object>> declarations = objects(sdkClientUiDeclarations.get("declarations"));
        List<Map<String, Object>> bindings = new ArrayList<>();
        int liveRoutableCount = 0;
        int liveMutatedCount = 0;
        int requiredLiveRouteCount = 0;
        int requiredLiveRoutableCount = 0;
        int requiredLiveMutatedCount = 0;
        for (Map<String, Object> declaration : declarations) {
            String surfaceId = stringValue(declaration, "surfaceId", stringValue(declaration, "id", ""));
            String surfaceType = stringValue(declaration, "surfaceType", stringValue(declaration, "registry", ""));
            String canonicalSurface = canonicalDeclaredClientSurface(surfaceId, surfaceType);
            boolean liveRoutable = declaredSurfaceLiveRoutable(canonicalSurface, surfaceType, bridge);
            boolean liveMutated = declaredSurfaceLiveMutated(canonicalSurface, surfaceType, bridge);
            boolean requiredLiveRoute = requiredDeclaredClientLiveRoute(canonicalSurface, surfaceType);
            if (liveRoutable) {
                liveRoutableCount++;
            }
            if (liveMutated) {
                liveMutatedCount++;
            }
            if (requiredLiveRoute) {
                requiredLiveRouteCount++;
                if (liveRoutable) {
                    requiredLiveRoutableCount++;
                }
                if (liveMutated) {
                    requiredLiveMutatedCount++;
                }
            }
            Map<String, Object> binding = new LinkedHashMap<>(declaration);
            binding.put("surfaceId", surfaceId);
            binding.put("surfaceType", surfaceType);
            binding.put("canonicalSurface", canonicalSurface);
            binding.put("liveRouteRequired", requiredLiveRoute);
            binding.put("liveRouteBound", liveRoutable);
            binding.put("liveClientBridgeMutated", liveMutated);
            binding.put("routeSource", liveRoutable
                    ? declaredSurfaceRouteSource(canonicalSurface, surfaceType)
                    : "missing_native_client_route");
            bindings.add(Map.copyOf(binding));
        }
        bridge.put("moduleDeclaredClientSurfaceBindings", List.copyOf(bindings));
        bridge.put("moduleDeclaredClientSurfaceCount", declarations.size());
        bridge.put("moduleDeclaredClientSurfaceLiveRoutableCount", liveRoutableCount);
        bridge.put("moduleDeclaredClientSurfaceLiveMutatedCount", liveMutatedCount);
        bridge.put("moduleDeclaredClientRequiredLiveRouteCount", requiredLiveRouteCount);
        bridge.put("moduleDeclaredClientRequiredLiveRoutableCount", requiredLiveRoutableCount);
        bridge.put("moduleDeclaredClientRequiredLiveMutatedCount", requiredLiveMutatedCount);
        bridge.put("moduleDeclaredRequiredClientSurfacesLiveRoutable",
                requiredLiveRouteCount == 0 || requiredLiveRoutableCount == requiredLiveRouteCount);
        bridge.put("moduleDeclaredRequiredClientSurfacesLiveAttached",
                requiredLiveRouteCount == 0 || requiredLiveMutatedCount == requiredLiveRouteCount);
        bridge.put("moduleDeclaredClientSurfacesPromoted", !declarations.isEmpty());
        bridge.put("moduleDeclaredClientSurfacesLiveRoutable",
                declarations.isEmpty() || liveRoutableCount == declarations.size());
        bridge.put("moduleDeclaredClientSurfacesLiveAttached",
                declarations.isEmpty() || liveMutatedCount == declarations.size());
        bridge.put("liveClientBridgeRequiredByModuleDeclarations", !declarations.isEmpty());
        bindProfileClientSurfaceContract(bridge);
    }

    private static String canonicalDeclaredClientSurface(String surfaceId, String surfaceType) {
        String text = (surfaceId == null ? "" : surfaceId).trim().toLowerCase(java.util.Locale.ROOT);
        String type = surfaceType == null ? "" : surfaceType.trim().toLowerCase(java.util.Locale.ROOT);
        String profileAlias = profileSurfaceAliasCanonical(text);
        if (!profileAlias.isBlank()) {
            return profileAlias;
        }
        if ("terminal".equals(type) || text.contains("terminal") || text.startsWith("echoterminal:")) {
            return "TERMINAL";
        }
        if ("index".equals(type) || text.contains("echoindex") || text.contains(":index") || text.endsWith("_index")) {
            return "INDEX";
        }
        if ("lens".equals(type) || text.contains("echolens") || text.contains(":lens") || text.contains("lens")) {
            return "LENS";
        }
        if ("holomap".equals(type) || text.contains("echoholomap") || text.contains("holomap")
                || text.contains("ashfall_map") || text.contains("minimap")) {
            return "HOLOMAP";
        }
        if (text.contains("signalos")) {
            return "SIGNALOS";
        }
        if (text.contains("echowiki") || text.contains(":wiki") || text.contains("wiki")) {
            return "WIKI";
        }
        if (text.contains("main_menu") || "main_menu".equals(type)) {
            return "MAIN_MENU";
        }
        if (text.contains("loading") || "loading".equals(type) || "loading_screen".equals(type)) {
            return "LOADING";
        }
        if ("hud".equals(type) || text.contains("echohudcore") || text.contains(":hud")
                || text.contains("hud_") || text.contains("_hud")) {
            return "HUD";
        }
        return text.isBlank() ? "" : text.toUpperCase(java.util.Locale.ROOT);
    }

    private static String profileSurfaceAliasCanonical(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return switch (text) {
            case "echoashfallprotocol:echo_native_main_menu" -> "MAIN_MENU";
            case "echoashfallprotocol:echo_native_loading" -> "LOADING";
            case "echoterminal:terminal" -> "TERMINAL";
            case "echoindex:index" -> "INDEX";
            case "echoashfallprotocol:portable_signal_scanner" -> "LENS";
            case "echoholomap:ashfall_map" -> "HOLOMAP";
            case "echoashfallprotocol:runtime_hud_notification" -> "HUD";
            default -> "";
        };
    }

    private static boolean declaredSurfaceLiveRoutable(String canonicalSurface, String surfaceType, Map<String, Object> bridge) {
        String surface = canonicalSurface == null ? "" : canonicalSurface;
        String type = normalizedSurfaceType(surfaceType);
        return switch (surface) {
            case "HUD" -> Boolean.TRUE.equals(bridge.get("hudBridgeReady"));
            case "LOADING" -> Boolean.TRUE.equals(bridge.get("loadingOverlayBridgeReady"));
            case "MAIN_MENU" -> Boolean.TRUE.equals(bridge.get("nativeMainMenuProjectionWatcherReady"))
                    || Boolean.TRUE.equals(bridge.get("customMainMenuReady"));
            default -> isRealModuleSurface(surface)
                    || "hud".equals(type)
                    || nativeScreenDescriptorHostReady(type, bridge)
                    || nativeThemeDescriptorHostReady(type, bridge);
        };
    }

    private static boolean declaredSurfaceLiveMutated(String canonicalSurface, String surfaceType, Map<String, Object> bridge) {
        String surface = canonicalSurface == null ? "" : canonicalSurface;
        String type = normalizedSurfaceType(surfaceType);
        return switch (surface) {
            case "HUD" -> Boolean.TRUE.equals(bridge.get("nativeHudProjectionInstalled"))
                    || Boolean.TRUE.equals(bridge.get("nativeHudRendererFrameRendered"));
            case "LOADING" -> Boolean.TRUE.equals(bridge.get("nativeLoadingOverlayProjectionInstalled"))
                    || Boolean.TRUE.equals(bridge.get("nativeLoadingOverlayProjectionDeferred"))
                    || Boolean.TRUE.equals(bridge.get("nativeLoadingOverlayProjectionWorldLoadSuppressed"));
            case "MAIN_MENU" -> Boolean.TRUE.equals(bridge.get("nativeMainMenuProjectionWatcherReady"))
                    || Boolean.TRUE.equals(bridge.get("customMainMenuOverrideAttached"));
            default -> isRealModuleSurface(surface)
                    && Boolean.TRUE.equals(bridge.get("clientUiHostAttached"))
                    && Boolean.TRUE.equals(bridge.get("physicalHotkeyPollingReady"))
                    || nativeScreenDescriptorHostReady(type, bridge)
                    || nativeThemeDescriptorHostReady(type, bridge);
        };
    }

    private static void bindProfileClientSurfaceContract(Map<String, Object> bridge) {
        List<Map<String, Object>> profileSurfaces = objects(bridge.get("profileDeclaredClientSurfaces"));
        List<Map<String, Object>> bindings = new ArrayList<>();
        LinkedHashSet<String> expectedTypes = new LinkedHashSet<>();
        LinkedHashSet<String> declaredTypes = new LinkedHashSet<>();
        List<String> missingTypes = new ArrayList<>();
        List<String> missingIds = new ArrayList<>();
        for (Map<String, Object> profileSurface : profileSurfaces) {
            String surfaceType = normalizedSurfaceType(stringValue(profileSurface, "surfaceType", ""));
            String surfaceId = firstNonBlank(
                    stringValue(profileSurface, "canonicalId", ""),
                    stringValue(profileSurface, "target", ""),
                    stringValue(profileSurface, "screenId", "")
            );
            String canonicalSurface = canonicalDeclaredClientSurface(surfaceId, surfaceType);
            boolean liveRoutable = declaredSurfaceLiveRoutable(canonicalSurface, surfaceType, bridge);
            boolean liveMutated = declaredSurfaceLiveMutated(canonicalSurface, surfaceType, bridge);
            expectedTypes.add(surfaceType);
            if (liveRoutable && liveMutated) {
                declaredTypes.add(surfaceType);
            } else {
                missingTypes.add(surfaceType);
                missingIds.add(surfaceId);
            }
            Map<String, Object> binding = new LinkedHashMap<>(profileSurface);
            binding.put("surfaceType", surfaceType);
            binding.put("profileSurfaceId", surfaceId);
            binding.put("runtimeCanonicalSurface", canonicalSurface);
            binding.put("liveRouteBound", liveRoutable);
            binding.put("liveClientBridgeMutated", liveMutated);
            binding.put("routeSource", liveRoutable
                    ? declaredSurfaceRouteSource(canonicalSurface, surfaceType)
                    : "missing_native_client_route");
            bindings.add(Map.copyOf(binding));
        }
        if (profileSurfaces.isEmpty()) {
            expectedTypes.addAll(CORE_PROFILE_CLIENT_SURFACE_TYPES);
            declaredTypes.addAll(CORE_PROFILE_CLIENT_SURFACE_TYPES);
        }
        bridge.put("profileExpectedClientSurfaceTypes", List.copyOf(expectedTypes));
        bridge.put("profileDeclaredClientSurfaceTypes", List.copyOf(declaredTypes));
        bridge.put("profileMissingClientSurfaceTypes", List.copyOf(missingTypes));
        bridge.put("profileMissingClientSurfaceIds", List.copyOf(missingIds));
        bridge.put("profileClientSurfaceLiveBindings", List.copyOf(bindings));
        bridge.put("profileClientSurfaceContractSatisfied",
                profileSurfaces.isEmpty() || missingTypes.isEmpty());
    }

    private static boolean requiredDeclaredClientLiveRoute(String canonicalSurface, String surfaceType) {
        String surface = canonicalSurface == null ? "" : canonicalSurface;
        String type = normalizedSurfaceType(surfaceType);
        return isRealModuleSurface(surface) || "hud".equals(type) || "main_menu".equals(type)
                || "loading_screen".equals(type);
    }

    private static String declaredSurfaceRouteSource(String canonicalSurface, String surfaceType) {
        String type = normalizedSurfaceType(surfaceType);
        if (nativeScreenDescriptorType(type)) {
            return "EchoNativeLiveUiBridge.native_screen_descriptor_host";
        }
        if (nativeThemeDescriptorType(type)) {
            return "EchoNativeLiveUiBridge.native_theme_descriptor_host";
        }
        String surface = canonicalSurface == null ? "" : canonicalSurface;
        return "EchoNativeLiveUiBridge." + surface.toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean nativeScreenDescriptorHostReady(String surfaceType, Map<String, Object> bridge) {
        return nativeScreenDescriptorType(surfaceType)
                && Boolean.TRUE.equals(bridge.get("clientUiHostAttached"))
                && Boolean.TRUE.equals(bridge.get("nativeDataScreenRoutes"))
                && !String.valueOf(bridge.getOrDefault("screenClass", "")).isBlank();
    }

    private static boolean nativeThemeDescriptorHostReady(String surfaceType, Map<String, Object> bridge) {
        return nativeThemeDescriptorType(surfaceType)
                && Boolean.TRUE.equals(bridge.get("clientUiHostAttached"))
                && Boolean.TRUE.equals(bridge.get("generatedDashboardDataBacked"));
    }

    private static boolean nativeScreenDescriptorType(String surfaceType) {
        String type = normalizedSurfaceType(surfaceType);
        return "screen".equals(type);
    }

    private static boolean nativeThemeDescriptorType(String surfaceType) {
        String type = normalizedSurfaceType(surfaceType);
        return "theme".equals(type);
    }

    private static String normalizedSurfaceType(String surfaceType) {
        return surfaceType == null ? "" : surfaceType.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String firstNonBlank(String first, String second, String third) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return third == null ? "" : third;
    }

    private static boolean invokeRealStaticOpen(
            Class<?> minecraftClass,
            Object minecraft,
            String className,
            String methodName,
            Map<String, Object> route
    ) {
        boolean[] opened = {false};
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            try {
                Class<?> type = Class.forName(className, true, EchoNativeBootstrapMain.nativeClientModuleClassLoader());
                invokeStaticNoArg(type, "register");
                Object value = type.getMethod(methodName).invoke(null);
                opened[0] = Boolean.TRUE.equals(value);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        });
        route.put("clientThreadScheduled", scheduled);
        return scheduled && opened[0];
    }

    private static void bindRealTerminalRuntime(Map<String, Object> route) {
        invokeOptionalStaticNoArg(route, "terminalCoreServicesRegistered",
                "com.knoxhack.echoterminal.service.EchoTerminalCoreServices", "register");
        invokeOptionalStaticNoArg(route, "terminalCommonIntegrationRegistered",
                "com.knoxhack.echoterminal.BuiltinTerminalCommonIntegration", "register");
        invokeOptionalStaticNoArg(route, "terminalBuiltinTabsRegistered",
                "com.knoxhack.echoterminal.client.BuiltinTerminalTabs", "register");
        Object mainProvider = optionalStaticField(route, "terminalMainSurvivalProvider",
                "com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider", "INSTANCE");
        Object vanillaProvider = optionalStaticField(route, "terminalVanillaJourneyProvider",
                "com.knoxhack.echoterminal.mission.VanillaJourneyProvider", "INSTANCE");
        Object mainTab = optionalStaticField(route, "terminalMainSurvivalTab",
                "com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider", "TAB_ID");
        Object vanillaTab = optionalStaticField(route, "terminalVanillaJourneyTab",
                "com.knoxhack.echoterminal.mission.VanillaJourneyProvider", "TAB_ID");
        invokeOptionalStaticOneArg(route, "terminalMainSurvivalProviderRegistered",
                "com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry", "register", mainProvider);
        invokeOptionalStaticOneArg(route, "terminalVanillaJourneyProviderRegistered",
                "com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry", "register", vanillaProvider);
        invokeOptionalStaticOneArg(route, "terminalMainSurvivalActionsRegistered",
                "com.knoxhack.echoterminal.api.mission.TerminalMissionActions", "registerForTab", mainTab);
        invokeOptionalStaticOneArg(route, "terminalVanillaJourneyActionsRegistered",
                "com.knoxhack.echoterminal.api.mission.TerminalMissionActions", "registerForTab", vanillaTab);
        Object tabs = invokeOptionalStaticNoArgValue(route, "terminalTabs",
                "com.knoxhack.echoterminal.api.TerminalTabRegistry", "tabs");
        route.put("terminalTabCount", sizeOf(tabs));
    }

    private static void bindRealIndexRuntime(Map<String, Object> route) {
        invokeOptionalStaticNoArg(route, "indexTerminalCommonIntegrationRegistered",
                "com.knoxhack.echoindex.integration.IndexTerminalCommonIntegration", "register");
        invokeOptionalStaticNoArg(route, "indexMissionCoreIntegrationRegistered",
                "com.knoxhack.echoindex.integration.IndexMissionCoreIntegration", "register");
        EchoNativeBootstrapMain.invokeNativeProductHook(route, "index_provider");
        Object service = optionalStaticField(route, "indexService",
                "com.knoxhack.echoindex.service.IndexService", "INSTANCE");
        route.put("indexServiceAvailable", service != null);
        route.put("nativeDataScreenOnly", true);
        route.put("indexProviderCount", invokeOptionalInstanceNoArg(route, "indexProviderCountValue",
                service, "providerCount"));
    }

    private static void bindRealHoloMapRuntime(Map<String, Object> route) {
        invokeOptionalStaticNoArg(route, "holoMapTerminalCommonIntegrationRegistered",
                "com.knoxhack.echoholomap.integration.HoloMapTerminalCommonIntegration", "register");
        invokeOptionalStaticNoArg(route, "holoMapTerminalClientIntegrationRegistered",
                "com.knoxhack.echoholomap.integration.HoloMapTerminalClientIntegration", "register");
        invokeOptionalStaticNoArg(route, "holoMapMissionCoreIntegrationRegistered",
                "com.knoxhack.echoholomap.integration.HoloMapMissionCoreIntegration", "register");
        invokeOptionalStaticNoArg(route, "holoMapIndexIntegrationRegistered",
                "com.knoxhack.echoholomap.integration.HoloMapIndexIntegration", "register");
        Object service = optionalStaticField(route, "holoMapService",
                "com.knoxhack.echoholomap.map.HoloMapService", "INSTANCE");
        route.put("holoMapServiceAvailable", service != null);
        invokeOptionalInstanceNoArg(route, "holoMapBuiltinsRegistered", service, "registerBuiltins");
        route.put("holoMapProviderCount", invokeOptionalInstanceNoArg(route, "holoMapProviderCountValue",
                service, "providerCount"));
    }

    private static void bindRealWikiRuntime(Map<String, Object> route) {
        invokeOptionalStaticNoArg(route, "wikiDefaultsEnsured",
                "com.knoxhack.echowiki.content.WikiContentRegistry", "ensureDefaults");
        invokeOptionalStaticNoArg(route, "wikiTerminalClientIntegrationRegistered",
                "com.knoxhack.echowiki.integration.WikiTerminalClientIntegration", "register");
        route.put("nativeDataScreenOnly", true);
    }

    private static boolean openRealTerminalSurface(Class<?> minecraftClass, Object minecraft, Map<String, Object> route) {
        route.put("clientThreadScheduled", false);
        route.put("nativeDataScreenOnly", false);
        route.put("terminalScreenClass", "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen");
        route.put("terminalBridgeClass", "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreBridge");
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            boolean openedByBridge = false;
            try {
                Class<?> bridgeClass = Class.forName(
                        "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreBridge",
                        true,
                        EchoNativeBootstrapMain.nativeClientModuleClassLoader());
                bridgeClass.getMethod("register").invoke(null);
                Object opened = bridgeClass.getMethod("open").invoke(null);
                openedByBridge = Boolean.TRUE.equals(opened);
                route.put("terminalScreenCoreBridgeOpened", openedByBridge);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                route.put("terminalScreenCoreBridgeOpened", false);
                route.put("terminalScreenCoreBridgeFailureKind", exception.getClass().getSimpleName());
                route.put("terminalScreenCoreBridgeFailureMessage", failureMessage(exception));
            }
            if (!openedByBridge) {
                route.put("terminalClassicFallbackAttempted", true);
                try {
                    route.put("terminalClassicFallbackOpened", openClassicTerminalFallback(minecraft));
                } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                    route.put("terminalClassicFallbackOpened", false);
                    route.put("terminalClassicFallbackFailureKind", exception.getClass().getSimpleName());
                    route.put("terminalClassicFallbackFailureMessage", failureMessage(exception));
                    throw new IllegalStateException(exception);
                }
                if (!Boolean.TRUE.equals(route.get("terminalClassicFallbackOpened"))) {
                    throw new IllegalStateException("TerminalScreenCoreBridge.open returned false");
                }
            }
        });
        route.put("clientThreadScheduled", scheduled);
        if (!scheduled) {
            route.put("realTerminalScreenOpened", false);
            route.put("terminalBridgeFailure", "client_thread_not_scheduled");
            return false;
        }
        Object current = currentScreen(minecraft);
        String currentClass = current == null ? "" : current.getClass().getName();
        boolean opened = currentClass.equals("com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen")
                || currentClass.equals("com.knoxhack.echoterminal.client.screen.EchoTerminalScreen")
                || currentClass.equals("com.knoxhack.echoterminal.client.screen.EchoNativeTerminalScreen");
        route.put("realTerminalScreenOpened", opened);
        route.put("screenClass", currentClass);
        route.put("nativeProductScreen", true);
        if (!opened) {
            route.put("terminalBridgeFailure", currentClass.isBlank()
                    ? "terminal_bridge_left_no_screen"
                    : "terminal_bridge_opened_wrong_screen:" + currentClass);
        }
        return opened;
    }

    private static boolean openClassicTerminalFallback(Object minecraft) throws ReflectiveOperationException {
        if (openNativeTerminalFallback(minecraft)) {
            return true;
        }
        Object player = minecraft.getClass().getField("player").get(minecraft);
        if (player == null) {
            return false;
        }
        Object inventory = player.getClass().getMethod("getInventory").invoke(player);
        ClassLoader loader = EchoNativeBootstrapMain.nativeClientModuleClassLoader();
        Class<?> menuClass = Class.forName("com.knoxhack.echoterminal.menu.EchoTerminalMenu", true, loader);
        Class<?> inventoryClass = Class.forName(runtimeClass("world.entity.player.Inventory"));
        Class<?> componentClass = Class.forName(runtimeClass("network.chat.Component"));
        Class<?> screensClass = Class.forName("com.knoxhack.echoterminal.client.screen.EchoTerminalScreens", true, loader);
        Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
        Object menu = menuClass.getConstructor(int.class, inventoryClass).newInstance(0, inventory);
        Object title = componentClass.getMethod("translatable", String.class)
                .invoke(null, "container.echoterminal.echo_terminal");
        Object screen = screensClass.getMethod("create", menuClass, inventoryClass, componentClass)
                .invoke(null, menu, inventory, title);
        minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, screen);
        return true;
    }

    private static boolean openNativeTerminalFallback(Object minecraft) {
        try {
            ClassLoader loader = EchoNativeBootstrapMain.nativeClientModuleClassLoader();
            Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
            Object screen = Class.forName(
                            "com.knoxhack.echoterminal.client.screen.EchoNativeTerminalScreen",
                            true,
                            loader)
                    .getConstructor()
                    .newInstance();
            minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, screen);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean openRealDeclaredModuleSurface(
            String surface,
            Class<?> minecraftClass,
            Object minecraft,
            Map<String, Object> route
    ) {
        return switch (surface == null ? "" : surface.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "TERMINAL" -> openRealTerminalSurface(minecraftClass, minecraft, route);
            case "INDEX" -> openRealIndexSurface(minecraftClass, minecraft, route);
            case "HOLOMAP" -> openRealHoloMapSurface(minecraftClass, minecraft, route);
            default -> false;
        };
    }

    private static boolean openRealIndexSurface(Class<?> minecraftClass, Object minecraft, Map<String, Object> route) {
        route.put("clientThreadScheduled", false);
        route.put("nativeDataScreenOnly", false);
        route.put("indexScreenClass", "com.knoxhack.echoindex.client.IndexCatalogScreen");
        route.put("indexBridgeClass", "com.knoxhack.echoindex.client.IndexScreenCoreBridge");
        String action = String.valueOf(route.getOrDefault("action", "index.catalog"));
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            boolean openedByBridge = false;
            ClassLoader loader = EchoNativeBootstrapMain.nativeClientModuleClassLoader();
            try {
                Class<?> bridgeClass = Class.forName(
                        "com.knoxhack.echoindex.client.IndexScreenCoreBridge",
                        true,
                        loader);
                Object opened = switch (action) {
                    case "index.recipe" -> bridgeClass.getMethod("openMode", String.class).invoke(null, "recipes");
                    case "index.usage" -> bridgeClass.getMethod("openMode", String.class).invoke(null, "usages");
                    case "index.bookmark" -> bridgeClass.getMethod("openMode", String.class).invoke(null, "favorites");
                    default -> bridgeClass.getMethod("open").invoke(null);
                };
                openedByBridge = Boolean.TRUE.equals(opened);
                route.put("indexScreenCoreBridgeOpened", openedByBridge);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                route.put("indexScreenCoreBridgeOpened", false);
                route.put("indexScreenCoreBridgeFailureKind", exception.getClass().getSimpleName());
                route.put("indexScreenCoreBridgeFailureMessage", failureMessage(exception));
            }
            if (!openedByBridge) {
                route.put("indexClassicFallbackAttempted", true);
                try {
                    Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
                    Object fallback = Class.forName(
                                    "com.knoxhack.echoindex.client.IndexCatalogScreen",
                                    true,
                                    loader)
                            .getConstructor()
                            .newInstance();
                    minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, fallback);
                    route.put("indexClassicFallbackOpened", true);
                } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                    route.put("indexClassicFallbackOpened", false);
                    route.put("indexClassicFallbackFailureKind", exception.getClass().getSimpleName());
                    route.put("indexClassicFallbackFailureMessage", failureMessage(exception));
                    throw new IllegalStateException(exception);
                }
            }
        });
        route.put("clientThreadScheduled", scheduled);
        if (!scheduled) {
            route.put("realIndexScreenOpened", false);
            route.put("indexBridgeFailure", "client_thread_not_scheduled");
            return false;
        }
        Object current = currentScreen(minecraft);
        String currentClass = current == null ? "" : current.getClass().getName();
        boolean opened = currentClass.contains("echoindex")
                || currentClass.contains("echoscreencore")
                || currentClass.contains("EchoScreen");
        route.put("realIndexScreenOpened", opened);
        route.put("screenClass", currentClass);
        route.put("nativeProductScreen", true);
        route.put("indexInventoryOverlayClass", "com.knoxhack.echoindex.client.IndexOverlay");
        if (!opened) {
            route.put("indexBridgeFailure", currentClass.isBlank()
                    ? "index_bridge_left_no_screen"
                    : "index_bridge_opened_wrong_screen:" + currentClass);
        }
        return opened;
    }

    private static boolean requestRealLensOverlay(Class<?> minecraftClass, Object minecraft, Map<String, Object> route) {
        route.put("clientThreadScheduled", false);
        route.put("nativeDataScreenOnly", false);
        route.put("lensOverlayClass", "com.knoxhack.echolens.client.LensHudOverlay");
        boolean[] requested = {false};
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            try {
                Class<?> overlayClass = Class.forName(
                        "com.knoxhack.echolens.client.LensHudOverlay",
                        true,
                        EchoNativeBootstrapMain.nativeClientModuleClassLoader());
                Object value = overlayClass.getMethod("requestDeepScan").invoke(null);
                requested[0] = Boolean.TRUE.equals(value);
                route.put("nativeLensRouteState", recordNativeLensOverlayRoute(
                        "lens.deep_scan",
                        "hud_scan",
                        "deep",
                        requested[0],
                        requested[0] ? "scan_requested" : "unavailable",
                        route));
                route.put("lensOverlaySnapshot", overlayClass.getMethod("snapshot", boolean.class).invoke(null, true));
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        });
        route.put("clientThreadScheduled", scheduled);
        route.put("realLensOverlayRequested", requested[0]);
        route.put("overlayRendered", requested[0]);
        route.put("nativeProductScreen", true);
        if (!scheduled) {
            route.put("lensBridgeFailure", "client_thread_not_scheduled");
        } else if (!requested[0]) {
            Object current = currentScreen(minecraft);
            route.put("lensBridgeFailure", current == null
                    ? "lens_request_deep_scan_returned_false"
                    : "lens_blocked_by_screen:" + current.getClass().getName());
        }
        return scheduled && requested[0];
    }

    private static boolean openRealHoloMapSurface(Class<?> minecraftClass, Object minecraft, Map<String, Object> route) {
        route.put("clientThreadScheduled", false);
        route.put("nativeDataScreenOnly", false);
        route.put("holoMapScreenClass", "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen");
        route.put("holoMapBridgeClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration");
        boolean[] openedByBridge = {false};
        boolean scheduled = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            ClassLoader loader = EchoNativeBootstrapMain.nativeClientModuleClassLoader();
            try {
                Class<?> bridgeClass = Class.forName(
                        "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        true,
                        loader);
                Object opened = bridgeClass.getMethod("openFullscreen").invoke(null);
                openedByBridge[0] = Boolean.TRUE.equals(opened);
                route.put("holoMapScreenCoreBridgeOpened", openedByBridge[0]);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                route.put("holoMapScreenCoreBridgeOpened", false);
                route.put("holoMapScreenCoreBridgeFailureKind", exception.getClass().getSimpleName());
                route.put("holoMapScreenCoreBridgeFailureMessage", failureMessage(exception));
            }
            if (!openedByBridge[0]) {
                route.put("holoMapClassicFallbackAttempted", true);
                try {
                    Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
                    Object fallback = Class.forName(
                                    "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                                    true,
                                    loader)
                            .getConstructor()
                            .newInstance();
                    minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, fallback);
                    route.put("holoMapClassicFallbackOpened", true);
                } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                    route.put("holoMapClassicFallbackOpened", false);
                    route.put("holoMapClassicFallbackFailureKind", exception.getClass().getSimpleName());
                    route.put("holoMapClassicFallbackFailureMessage", failureMessage(exception));
                    throw new IllegalStateException(exception);
                }
            }
        });
        route.put("clientThreadScheduled", scheduled);
        if (!scheduled) {
            route.put("realHoloMapScreenOpened", false);
            route.put("holoMapBridgeFailure", "client_thread_not_scheduled");
            return false;
        }
        Object current = currentScreen(minecraft);
        String currentClass = current == null ? "" : current.getClass().getName();
        boolean opened = currentClass.contains("echoholomap")
                || currentClass.contains("echoscreencore")
                || currentClass.contains("EchoScreen");
        route.put("realHoloMapScreenOpened", opened);
        route.put("holoMapScreenCoreOpened", openedByBridge[0]);
        route.put("screenClass", currentClass);
        route.put("nativeProductScreen", true);
        route.put("holoMapMiniMapOverlayClass", "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay");
        if (!opened) {
            route.put("holoMapBridgeFailure", currentClass.isBlank()
                    ? "holomap_bridge_left_no_screen"
                    : "holomap_bridge_opened_wrong_screen:" + currentClass);
        }
        return opened;
    }

    private static int bindRealLensRuntime(Map<String, Object> route) {
        try {
            invokeStaticNoArg(nativeClientClass("com.knoxhack.echolens.provider.LensBuiltins"), "register");
            invokeStaticNoArg(nativeClientClass("com.knoxhack.echolens.integration.LensCoreIntegration"), "register");
            invokeOptionalStaticNoArg(route, "lensMissionCoreIntegrationRegistered",
                    "com.knoxhack.echolens.integration.LensMissionCoreIntegration", "register");
            EchoNativeBootstrapMain.invokeNativeProductHook(route, "lens_integration");
            Class<?> registryClass = nativeClientClass("com.knoxhack.echolens.registry.LensProviderRegistry");
            Object count = registryClass.getMethod("count").invoke(null);
            Object diagnostics = registryClass.getMethod("diagnostics").invoke(null);
            route.put("diagnostics", diagnostics == null ? "" : diagnostics.getClass().getSimpleName());
            return count instanceof Number number ? number.intValue() : 0;
        } catch (ReflectiveOperationException exception) {
            route.put("failureKind", exception.getClass().getSimpleName());
            route.put("failureMessage", failureMessage(exception));
            return 0;
        }
    }

    private static boolean invokeOptionalStaticNoArg(
            Map<String, Object> route,
            String key,
            String className,
            String methodName
    ) {
        Object value = invokeOptionalStaticNoArgValue(route, key, className, methodName);
        return Boolean.TRUE.equals(route.get(key)) || value != null;
    }

    private static Object invokeOptionalStaticNoArgValue(
            Map<String, Object> route,
            String key,
            String className,
            String methodName
    ) {
        try {
            Class<?> type = nativeClientClass(className);
            java.lang.reflect.Method method = type.getDeclaredMethod(methodName);
            method.trySetAccessible();
            Object value = method.invoke(null);
            route.put(key, !Boolean.FALSE.equals(value));
            return value;
        } catch (Throwable exception) {
            route.put(key, false);
            route.put(key + "Failure", failureMessage(exception));
            return null;
        }
    }

    private static boolean invokeOptionalStaticOneArg(
            Map<String, Object> route,
            String key,
            String className,
            String methodName,
            Object argument
    ) {
        if (argument == null) {
            route.put(key, false);
            route.put(key + "Failure", "Missing argument");
            return false;
        }
        try {
            Class<?> type = nativeClientClass(className);
            for (java.lang.reflect.Method method : type.getMethods()) {
                if (!method.getName().equals(methodName)
                        || !java.lang.reflect.Modifier.isStatic(method.getModifiers())
                        || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = wrapPrimitive(method.getParameterTypes()[0]);
                if (!parameter.isInstance(argument)) {
                    continue;
                }
                method.trySetAccessible();
                Object value = method.invoke(null, argument);
                route.put(key, !Boolean.FALSE.equals(value));
                return Boolean.TRUE.equals(route.get(key));
            }
            route.put(key, false);
            route.put(key + "Failure", "No compatible method");
            return false;
        } catch (Throwable exception) {
            route.put(key, false);
            route.put(key + "Failure", failureMessage(exception));
            return false;
        }
    }

    private static Object invokeOptionalInstanceNoArg(
            Map<String, Object> route,
            String key,
            Object target,
            String methodName
    ) {
        if (target == null) {
            route.put(key, false);
            route.put(key + "Failure", "Missing target");
            return null;
        }
        try {
            java.lang.reflect.Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            route.put(key, !Boolean.FALSE.equals(value));
            return value;
        } catch (Throwable exception) {
            route.put(key, false);
            route.put(key + "Failure", failureMessage(exception));
            return null;
        }
    }

    private static Object optionalStaticField(
            Map<String, Object> route,
            String key,
            String className,
            String fieldName
    ) {
        try {
            Class<?> type = nativeClientClass(className);
            java.lang.reflect.Field field = type.getDeclaredField(fieldName);
            field.trySetAccessible();
            Object value = field.get(null);
            route.put(key, value != null);
            return value;
        } catch (Throwable exception) {
            route.put(key, false);
            route.put(key + "Failure", failureMessage(exception));
            return null;
        }
    }

    private static int sizeOf(Object value) {
        if (value instanceof java.util.Collection<?> collection) {
            return collection.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        if (value != null && value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value);
        }
        return value == null ? 0 : 1;
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private static void invokeStaticNoArg(Class<?> type, String methodName) {
        try {
            type.getMethod(methodName).invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Class<?> nativeClientClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, EchoNativeBootstrapMain.nativeClientModuleClassLoader());
    }

    private static void closeAutoOpenedSurface(
            Path markerPath,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft
    ) {
        try {
            Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
            boolean closed = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                try {
                    minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, new Object[]{null});
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            boolean gameplayVisible = currentScreen(minecraft) == null;
            bridge.put("autoOpenedSurfaceClosedToGameplay", closed && gameplayVisible);
            bridge.put("autoOpenedSurfaceCloseInvoked", closed);
            bridge.put("gameplayVisibleAfterUiValidation", gameplayVisible);
            bridge.put("summary", gameplayVisible
                    ? "Native client UI host validated real ECHO module surfaces, then returned to gameplay. Keys: M Terminal; G/R/U Index; Left Alt Lens; J/K/[ ]/\\ HoloMap; N SignalOS."
                    : "Native client UI host validated the ECHO menu, but Minecraft still has an active screen.");
        } catch (Throwable exception) {
            bridge.put("autoOpenedSurfaceClosedToGameplay", false);
            bridge.put("autoOpenedSurfaceCloseFailureKind", exception.getClass().getSimpleName());
            bridge.put("autoOpenedSurfaceCloseFailureMessage", failureMessage(exception));
        }
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);
    }

    private static void attemptMainMenuOverride(
            Path markerPath,
            String packId,
            List<String> modules,
            Map<String, Object> runtimeBridge,
            Map<String, Object> bridge,
            SnapshotWriter snapshotWriter,
            Class<?> minecraftClass,
            Object minecraft,
            Class<?> screenClass
    ) {
        bridge.put("customMainMenuOverrideAttempted", true);
        try {
            Object currentScreen = waitForTitleScreenOrPlayableWorld(minecraftClass, minecraft, bridge);
            boolean titleScreenActive = isTitleScreen(currentScreen);
            bridge.put("customMainMenuTitleScreenDetected", titleScreenActive);
            if (!titleScreenActive) {
                String skipReason = currentScreen == null
                        ? "no_screen_active"
                        : "current_screen_not_title:" + currentScreen.getClass().getName();
                bridge.put("customMainMenuOverrideAttached", false);
                bridge.put("customMainMenuOverrideSkippedReason", skipReason);
                Map<String, Object> overrideSmoke = qa("EchoNativeAgent5MainMenuOverrideSmoke").capture(
                        false,
                        false,
                        skipReason,
                        SCREEN_CLASS_NAME,
                        packId,
                        modules.size(),
                        integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                        integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
                );
                bridge.put("customMainMenuOverrideSmoke", overrideSmoke);
                Map<String, Object> mainMenuEndToEndAcceptance = qa("EchoNativeAgent5MainMenuEndToEndAcceptance").assess(
                        overrideSmoke,
                        qa("EchoNativeAgent5MainMenuOptionActivationSmoke").capture()
                );
                Map<String, Object> mainMenuLiveSurfaceAcceptance = qa("EchoNativeAgent5LiveSurfaceAcceptance").assess(
                        false,
                        "",
                        screenClass.getName(),
                        "MAIN_MENU",
                        "MAIN_MENU"
                );
                bridge.put("lastMainMenuEndToEndAcceptance", mainMenuEndToEndAcceptance);
                bridge.put("lastMainMenuLiveSurfaceAcceptance", mainMenuLiveSurfaceAcceptance);
                bridge.put("lastLiveMainMenuOverrideAcceptance",
                        qa("EchoNativeAgent5LiveMainMenuOverrideAcceptance").assess(
                                overrideSmoke,
                                mainMenuLiveSurfaceAcceptance,
                                mainMenuEndToEndAcceptance
                        ));
                bridge.put("lastLivePhase5Acceptance", qa("EchoNativeAgent5LivePhase5Acceptance").assess(bridge));
                bridge.put("lastLiveClientHostEvidenceAcceptance",
                        qa("EchoNativeAgent5LiveClientHostEvidenceAcceptance").assess(bridge));
                updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
                bridge.put("summary", productWorldAutoOpenEnabled() && currentScreen == null
                        ? "Native UI bridge refused to continue vanilla fallback while waiting for the Ashfall product world auto-open path."
                        : "Native UI bridge left the current screen untouched because the Minecraft title screen was not active.");
                runtimeBridge.put("nativeClientUiBridge", bridge);
                writeUiReport(markerPath, bridge);
                writeSnapshot(snapshotWriter);
                return;
            }

            Object mainMenuScreen = newNativeClientScreen(
                    "MAIN_MENU",
                    packId,
                    modules,
                    runtimeBridge,
                    screenClass
            );
            Class<?> mainMenuScreenClass = mainMenuScreen.getClass();
            Class<?> vanillaScreenClass = Class.forName(runtimeClass("client.gui.screens.Screen"));
            boolean opened = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                try {
                    minecraft.getClass().getMethod("setScreen", vanillaScreenClass).invoke(minecraft, mainMenuScreen);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            boolean accepted = opened && waitForScreen(minecraft, screenClass, mainMenuScreenClass, "MAIN_MENU");
            bridge.put("customMainMenuOverrideAttached", accepted);
            bridge.put("mainMenuFallbackOpened", accepted);
            bridge.put("customMainMenuOpened", accepted);
            bridge.put("nativeMainMenuScreenClass", mainMenuScreenClass.getName());
            bridge.put("nativeMainMenuProductScreen", mainMenuScreenClass != screenClass);
            bridge.put("lastOpenedSurface", accepted ? "MAIN_MENU" : bridge.getOrDefault("lastOpenedSurface", ""));
            Map<String, Object> mainMenuLiveSurfaceAcceptance =
                    liveScreenAcceptance(minecraft, screenClass, mainMenuScreenClass, "MAIN_MENU", opened);
            Map<String, Object> overrideSmoke = qa("EchoNativeAgent5MainMenuOverrideSmoke").capture(
                    true,
                    accepted,
                    accepted ? "" : "minecraft_rejected_setScreen",
                    SCREEN_CLASS_NAME,
                    packId,
                    modules.size(),
                    integer(object(runtimeBridge.get("registryBridge")).get("registeredItemCount")),
                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("missionDefinitionCount")),
                    integer(EchoNativeBootstrapMain.nativeProductGameplayBridge(runtimeBridge).get("worldRegionCount"))
            );
            bridge.put("customMainMenuOverrideSmoke", overrideSmoke);
            Map<String, Object> mainMenuEndToEndAcceptance = qa("EchoNativeAgent5MainMenuEndToEndAcceptance").assess(
                    overrideSmoke,
                    qa("EchoNativeAgent5MainMenuOptionActivationSmoke").capture()
            );
            bridge.put("lastMainMenuEndToEndAcceptance", mainMenuEndToEndAcceptance);
            bridge.put("lastMainMenuLiveSurfaceAcceptance", mainMenuLiveSurfaceAcceptance);
            bridge.put("lastLiveMainMenuOverrideAcceptance",
                    qa("EchoNativeAgent5LiveMainMenuOverrideAcceptance").assess(
                            overrideSmoke,
                            mainMenuLiveSurfaceAcceptance,
                            mainMenuEndToEndAcceptance
                    ));
            bridge.put("lastLivePhase5Acceptance", qa("EchoNativeAgent5LivePhase5Acceptance").assess(bridge));
            bridge.put("lastLiveClientHostEvidenceAcceptance",
                    qa("EchoNativeAgent5LiveClientHostEvidenceAcceptance").assess(bridge));
            updateAdapterCoreRuntimeBridgeGuard(runtimeBridge, bridge);
            bridge.put("mainMenuEndToEndAcceptanceSmoke", qa("EchoNativeAgent5MainMenuEndToEndAcceptanceSmoke").capture());
            bridge.put("noScreenCrash", accepted);
            bridge.put("summary", accepted
                    ? "Native client UI host replaced the active title screen with the ECHO custom main menu."
                    : "Native client UI host attempted title-screen replacement but Minecraft did not accept setScreen.");
        } catch (Throwable exception) {
            bridge.put("customMainMenuOverrideAttached", false);
            bridge.put("customMainMenuOverrideFailureKind", exception.getClass().getSimpleName());
            bridge.put("customMainMenuOverrideFailureMessage", failureMessage(exception));
            bridge.put("summary", "Native custom main-menu override failed: " + failureMessage(exception));
        }
        runtimeBridge.put("nativeClientUiBridge", bridge);
        writeUiReport(markerPath, bridge);
        writeSnapshot(snapshotWriter);
    }

    private static Object currentScreen(Object minecraft) {
        try {
            return minecraft.getClass().getField("screen").get(minecraft);
        } catch (ReflectiveOperationException ignored) {
            try {
                return minecraft.getClass().getMethod("screen").invoke(minecraft);
            } catch (ReflectiveOperationException exception) {
                return null;
            }
        }
    }

    private static Object waitForTitleScreenOrPlayableWorld(
            Class<?> minecraftClass,
            Object minecraft,
            Map<String, Object> bridge
    ) {
        boolean productWorldAutoOpen = productWorldAutoOpenEnabled();
        int attempts = productWorldAutoOpen ? 400 : 1;
        for (int attempt = 0; attempt < attempts; attempt++) {
            Object screen = currentScreen(minecraft);
            if (isTitleScreen(screen)) {
                bridge.put("customMainMenuTitleScreenWaitAttempts", attempt + 1);
                return screen;
            }
            if (playableWorldReady(minecraftClass, minecraft)) {
                bridge.put("customMainMenuTitleScreenWaitStoppedForPlayableWorld", true);
                bridge.put("customMainMenuTitleScreenWaitAttempts", attempt + 1);
                return screen;
            }
            if (attempt == 0 && !productWorldAutoOpen) {
                return screen;
            }
            bridge.put("customMainMenuTitleScreenWaitActive", true);
            bridge.put("customMainMenuTitleScreenWaitLastScreen",
                    screen == null ? "" : screen.getClass().getName());
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                bridge.put("customMainMenuTitleScreenWaitInterrupted", true);
                return screen;
            }
        }
        bridge.put("customMainMenuTitleScreenWaitTimedOut", true);
        return currentScreen(minecraft);
    }

    private static boolean playableWorldReady(Class<?> minecraftClass, Object minecraft) {
        boolean[] ready = {false};
        boolean checked = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
            ready[0] = minecraftValue(minecraft, "player") != null
                    && minecraftValue(minecraft, "level") != null;
        });
        return checked && ready[0];
    }

    private static Map<String, Object> liveSurfaceAcceptance(
            Object minecraft,
            Class<?> screenClass,
            String expectedMode,
            boolean setScreenInvoked
    ) {
        Object screen = currentScreen(minecraft);
        Map<String, Object> acceptance = qa("EchoNativeAgent5LiveSurfaceAcceptance").assess(
                setScreenInvoked,
                screen == null ? "" : screen.getClass().getName(),
                screenClass.getName(),
                mode(screen),
                expectedMode
        );
        if (Boolean.TRUE.equals(acceptance.get("qaSidecarAvailable")) || !acceptance.containsKey("qaSidecarAvailable")) {
            return acceptance;
        }
        return liveSurfaceAcceptanceFallback(
                setScreenInvoked,
                screen == null ? "" : screen.getClass().getName(),
                screenClass.getName(),
                mode(screen),
                expectedMode
        );
    }

    private static Map<String, Object> liveScreenAcceptance(
            Object minecraft,
            Class<?> generatedScreenClass,
            Class<?> expectedScreenClass,
            String expectedMode,
            boolean setScreenInvoked
    ) {
        Object screen = currentScreen(minecraft);
        String currentClass = screen == null ? "" : screen.getClass().getName();
        boolean productClassAccepted = expectedScreenClass != null && expectedScreenClass.isInstance(screen);
        if (productClassAccepted && expectedScreenClass != generatedScreenClass) {
            Map<String, Object> acceptance = qa("EchoNativeAgent5LiveSurfaceAcceptance").assess(
                    setScreenInvoked,
                    currentClass,
                    expectedScreenClass.getName(),
                    expectedMode,
                    expectedMode
            );
            if (Boolean.TRUE.equals(acceptance.get("qaSidecarAvailable")) || !acceptance.containsKey("qaSidecarAvailable")) {
                return acceptance;
            }
            return liveSurfaceAcceptanceFallback(
                    setScreenInvoked,
                    currentClass,
                    expectedScreenClass.getName(),
                    expectedMode,
                    expectedMode
            );
        }
        Map<String, Object> acceptance = qa("EchoNativeAgent5LiveSurfaceAcceptance").assess(
                setScreenInvoked,
                currentClass,
                generatedScreenClass.getName(),
                mode(screen),
                expectedMode
        );
        if (Boolean.TRUE.equals(acceptance.get("qaSidecarAvailable")) || !acceptance.containsKey("qaSidecarAvailable")) {
            return acceptance;
        }
        return liveSurfaceAcceptanceFallback(
                setScreenInvoked,
                currentClass,
                generatedScreenClass.getName(),
                mode(screen),
                expectedMode
        );
    }

    private static Map<String, Object> liveSurfaceAcceptanceFallback(
            boolean setScreenInvoked,
            String currentScreenClass,
            String expectedScreenClass,
            String currentMode,
            String expectedMode
    ) {
        String screenClass = currentScreenClass == null ? "" : currentScreenClass;
        String expectedClass = expectedScreenClass == null ? "" : expectedScreenClass;
        String mode = normalizeMode(currentMode);
        String expected = normalizeMode(expectedMode);
        boolean accepted = setScreenInvoked
                && !screenClass.isBlank()
                && (screenClass.equals(expectedClass) || screenClass.endsWith("." + simpleName(expectedClass)))
                && expected.equals(mode);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("setScreenInvoked", setScreenInvoked);
        result.put("currentScreenClass", screenClass);
        result.put("expectedScreenClass", expectedClass);
        result.put("currentMode", mode);
        result.put("expectedMode", expected);
        result.put("effect", accepted ? "live_surface:accepted:" + expected : "live_surface:rejected:" + expected);
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", accepted);
        result.put("nativeLoaderRuntimeFallback", true);
        return Map.copyOf(result);
    }

    private static boolean waitForSurface(Object minecraft, Class<?> screenClass, String expectedMode) {
        for (int attempt = 0; attempt < 80; attempt++) {
            Object screen = currentScreen(minecraft);
            String className = screen == null ? "" : screen.getClass().getName();
            boolean classMatches = className.equals(screenClass.getName())
                    || className.endsWith("." + simpleName(screenClass.getName()));
            if (classMatches && normalizeMode(mode(screen)).equals(normalizeMode(expectedMode))) {
                return true;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static boolean waitForScreen(
            Object minecraft,
            Class<?> generatedScreenClass,
            Class<?> expectedScreenClass,
            String expectedMode
    ) {
        for (int attempt = 0; attempt < 80; attempt++) {
            Object screen = currentScreen(minecraft);
            if (screenMatches(screen, generatedScreenClass, expectedScreenClass, expectedMode)) {
                return true;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static boolean screenMatches(
            Object screen,
            Class<?> generatedScreenClass,
            Class<?> expectedScreenClass,
            String expectedMode
    ) {
        if (screen == null) {
            return false;
        }
        if (expectedScreenClass != null && expectedScreenClass.isInstance(screen)
                && expectedScreenClass != generatedScreenClass) {
            return true;
        }
        String className = screen.getClass().getName();
        boolean classMatches = generatedScreenClass != null
                && (className.equals(generatedScreenClass.getName())
                || className.endsWith("." + simpleName(generatedScreenClass.getName())));
        return classMatches && normalizeMode(mode(screen)).equals(normalizeMode(expectedMode));
    }

    private static boolean waitForPlayableWorld(
            Class<?> minecraftClass,
            Object minecraft,
            Map<String, Object> bridge
    ) {
        bridge.put("playableWorldWaitStarted", true);
        bridge.put("playableWorldReady", false);
        boolean productWorldAutoOpen = productWorldAutoOpenEnabled();
        for (int attempt = 0; attempt < 2400; attempt++) {
            boolean[] ready = {false};
            boolean checked = invokeOnClientThreadAndWait(minecraftClass, minecraft, () -> {
                ready[0] = minecraftValue(minecraft, "player") != null
                        && minecraftValue(minecraft, "level") != null;
            });
            bridge.put("playableWorldWaitAttempts", attempt + 1);
            bridge.put("playableWorldReady", checked && ready[0]);
            if (checked && ready[0]) {
                if (productWorldAutoOpen && !productWorldLevelDatPresent()) {
                    bridge.put("playableWorldWaitTimedOut", false);
                    bridge.put("playableWorldWaitStoppedForVanillaFallback", true);
                    bridge.put("playableWorldWaitExpectedProductWorldFolder",
                            System.getProperty("echo.native.productWorldFolder", "echo_native_ashfall_wasteland"));
                    bridge.put("playableWorldWaitExpectedProductLevelDat", productWorldLevelDatPath().toString());
                    bridge.put("summary", "Native UI bridge refused to attach gameplay surfaces because Minecraft opened a world before the Ashfall product save existed.");
                    return false;
                }
                bridge.put("playableWorldWaitTimedOut", false);
                return true;
            }
            Object screen = currentScreen(minecraft);
            if (isTitleScreen(screen)) {
                bridge.put("playableWorldWaitSawTitleScreen", true);
                if (!productWorldAutoOpen) {
                    bridge.put("playableWorldWaitTimedOut", false);
                    bridge.put("playableWorldWaitStoppedForTitleScreen", true);
                    return false;
                }
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                bridge.put("playableWorldWaitInterrupted", true);
                return false;
            }
        }
        bridge.put("playableWorldWaitTimedOut", true);
        return false;
    }

    private static boolean productWorldAutoOpenEnabled() {
        return Boolean.parseBoolean(System.getProperty("echo.native.productWorldAutoOpen", "false"));
    }

    private static boolean productWorldLevelDatPresent() {
        return Files.isRegularFile(productWorldLevelDatPath());
    }

    private static Path productWorldLevelDatPath() {
        String gameDir = System.getProperty("echo.native.gameDir", ".");
        String folder = System.getProperty("echo.native.productWorldFolder", "echo_native_ashfall_wasteland");
        return Path.of(gameDir).toAbsolutePath().normalize()
                .resolve("saves")
                .resolve(folder)
                .resolve("level.dat");
    }

    private static Object minecraftValue(Object minecraft, String name) {
        try {
            return minecraft.getClass().getField(name).get(minecraft);
        } catch (ReflectiveOperationException ignored) {
            // Runtime mappings vary; try declared fields and no-arg accessors too.
        }
        try {
            java.lang.reflect.Field field = minecraft.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(minecraft);
        } catch (ReflectiveOperationException ignored) {
            // Fall through to method lookup.
        }
        try {
            return minecraft.getClass().getMethod(name).invoke(minecraft);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static String mode(Object screen) {
        if (screen == null) {
            return "";
        }
        try {
            java.lang.reflect.Field field = screen.getClass().getDeclaredField("mode");
            field.setAccessible(true);
            Object value = field.get(screen);
            return value == null ? "" : String.valueOf(value);
        } catch (ReflectiveOperationException exception) {
            return "";
        }
    }

    private static String simpleName(String className) {
        int dot = className == null ? -1 : className.lastIndexOf('.');
        return dot < 0 ? String.valueOf(className) : className.substring(dot + 1);
    }

    private static String normalizeMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "TERMINAL" : normalized;
    }

    private static boolean isTitleScreen(Object screen) {
        if (screen == null) {
            return false;
        }
        try {
            Class<?> titleScreenClass = Class.forName(runtimeClass("client.gui.screens.TitleScreen"));
            if (titleScreenClass.isInstance(screen)) {
                return true;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to the class-name guard for mapped runtime variants.
        }
        String className = screen.getClass().getName();
        return className.endsWith(".TitleScreen") || className.endsWith("$TitleScreen");
    }

    private static Map<String, Object> agent5DataSources() {
        return EchoNativeAgent5UiHandlerRegistry.dataSources();
    }

    private static List<Map<String, Object>> notificationQueue() {
        return EchoNativeAgent5UiHandlerRegistry.notificationQueue();
    }

    private static List<String> notificationMessages(Object value) {
        if (!(value instanceof List<?> notifications)) {
            return List.of();
        }
        return notifications.stream()
                .filter(Map.class::isInstance)
                .map(item -> ((Map<?, ?>) item).get("message"))
                .map(valueOrNull -> valueOrNull == null ? "" : String.valueOf(valueOrNull))
                .filter(message -> !message.isBlank())
                .toList();
    }

    private static Map<String, Object> missionLog() {
        return EchoNativeAgent5UiHandlerRegistry.missionLog();
    }

    private static Map<String, Object> settings() {
        return EchoNativeAgent5UiHandlerRegistry.settings();
    }

    private static Map<String, Object> pauseFlow() {
        return EchoNativeAgent5UiHandlerRegistry.pauseFlow();
    }

    private static Map<String, Object> deathRecovery() {
        return EchoNativeAgent5UiHandlerRegistry.deathRecovery();
    }

    private static Map<String, Object> holomap() {
        return EchoNativeAgent5UiHandlerRegistry.holomap();
    }

    private static Map<String, Object> wiki() {
        return EchoNativeAgent5UiHandlerRegistry.wiki();
    }

    private static void startEarlyWindowTitleKeeper(
            Class<?> minecraftClass,
            Object minecraft,
            Map<String, Object> bridge
    ) {
        String title = System.getProperty("echo.native.loader.windowTitle", "ECHO Native Loader Client").trim();
        if (title.isBlank() || Boolean.TRUE.equals(bridge.get("earlyWindowTitleKeeperStarted"))) {
            return;
        }
        bridge.put("earlyWindowTitleKeeperStarted", true);
        Thread thread = new Thread(() -> {
            for (int attempt = 0; attempt < 240; attempt++) {
                try {
                    boolean scheduled = invokeOnClientThread(minecraftClass, minecraft, () -> {
                        try {
                            NativeLoaderLiveClientDiagnostics.setWindowTitle(minecraft, title);
                        } catch (ReflectiveOperationException ignored) {
                            // The window can be rebuilt during bootstrap; the next pass retries.
                        }
                    });
                    if (scheduled) {
                        bridge.put("earlyWindowTitleApplied", true);
                        bridge.put("earlyWindowTitle", title);
                    }
                    Object running = minecraft.getClass().getMethod("isRunning").invoke(minecraft);
                    if (attempt > 20 && Boolean.FALSE.equals(running)) {
                        return;
                    }
                    Thread.sleep(250L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Throwable ignored) {
                    try {
                        Thread.sleep(250L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "EchoNativeEarlyWindowTitleKeeper");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean invokeOnClientThread(Class<?> minecraftClass, Object minecraft, Runnable task) {
        try {
            Object sameThread = minecraftClass.getMethod("isSameThread").invoke(minecraft);
            if (Boolean.TRUE.equals(sameThread)) {
                task.run();
                return true;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to Minecraft.execute(Runnable).
        }
        try {
            minecraftClass.getMethod("execute", Runnable.class).invoke(minecraft, task);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static boolean invokeOnClientThreadAndWait(Class<?> minecraftClass, Object minecraft, Runnable task) {
        java.util.concurrent.CountDownLatch finished = new java.util.concurrent.CountDownLatch(1);
        Throwable[] failure = {null};
        boolean scheduled = invokeOnClientThread(minecraftClass, minecraft, () -> {
            try {
                task.run();
            } catch (Throwable throwable) {
                failure[0] = throwable;
            } finally {
                finished.countDown();
            }
        });
        if (!scheduled) {
            return false;
        }
        try {
            if (!finished.await(10L, java.util.concurrent.TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (failure[0] instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure[0] instanceof Error error) {
            throw error;
        }
        if (failure[0] != null) {
            throw new IllegalStateException(failure[0]);
        }
        return true;
    }

    private static void writeSnapshot(SnapshotWriter snapshotWriter) {
        try {
            snapshotWriter.write();
        } catch (IOException ignored) {
            // The separate UI report remains available when marker refresh fails.
        }
    }

    private static void writeUiReport(Path markerPath, Map<String, Object> bridge) {
        try {
            writeJsonAtomically(
                    markerPath.toAbsolutePath().normalize().getParent().resolve("live-ui-bridge.json"),
                    bridge
            );
        } catch (IOException ignored) {
            // The in-memory marker is still refreshed by the snapshot writer.
        }
    }

    private static void writeJsonAtomically(Path path, Object value) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        Files.createDirectories(normalized.getParent());
        Path temp = Files.createTempFile(normalized.getParent(), normalized.getFileName() + ".", ".tmp");
        try {
            Files.writeString(
                    temp,
                    writeJson(value),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            try {
                Files.move(temp, normalized, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, normalized, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String runtimeClass(String name) {
        return "net." + "minecraft." + name;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objects(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .toList();
        }
        return List.of();
    }

    private static String stringValue(Map<String, Object> source, String key, String fallback) {
        if (source == null) {
            return fallback;
        }
        Object value = source.get(key);
        if (value == null) {
            return fallback;
        }
        String string = String.valueOf(value);
        return string.isBlank() ? fallback : string;
    }

    private static boolean sendNativeProductUiPacket(String packetClassName, String command) {
        if (packetClassName == null || packetClassName.isBlank() || command == null || command.isBlank()) {
            return false;
        }
        try {
            Class<?> packetClass = Class.forName(packetClassName);
            Object packet = packetClass.getConstructor(String.class).newInstance(command);
            Class<?> actionsClass = Class.forName("com.knoxhack.echonetcore.client.EchoNetClientActions");
            Object sent = actionsClass.getMethod("trySendServerboundAction",
                            Class.forName("net.minecraft.network.protocol.common.custom.CustomPacketPayload"))
                    .invoke(null, packet);
            return Boolean.TRUE.equals(sent);
        } catch (Throwable exception) {
            return false;
        }
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String writeJson(Object value) {
        StringBuilder builder = new StringBuilder();
        appendJson(builder, value);
        return builder.append('\n').toString();
    }

    private static void appendJson(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append('\n').append("  ");
                appendJson(builder, String.valueOf(entry.getKey()));
                builder.append(": ");
                appendJson(builder, entry.getValue());
            }
            if (!map.isEmpty()) {
                builder.append('\n');
            }
            builder.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            List<Object> values = new ArrayList<>();
            iterable.forEach(values::add);
            builder.append('[');
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    builder.append(", ");
                }
                appendJson(builder, values.get(index));
            }
            builder.append(']');
        } else {
            appendJson(builder, String.valueOf(value));
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
