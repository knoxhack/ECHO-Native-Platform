package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Headless UI host for the Native Loader that records UI surface registrations
 * from modules during {@code CLIENT_SETUP}. In a live client environment this
 * would be backed by a real window/render pipeline; in the loader it serves as
 * the contract surface for modules to declare UI intent.
 */
public final class NativeLoaderClientUiHost {
    public static final String SERVICE_ID = "echo.native.client_ui_host";
    private static final Map<String, Map<String, Object>> BUILT_IN_PRODUCT_SURFACE_STATE = new LinkedHashMap<>();

    private final Map<String, Map<String, Object>> surfaces = new LinkedHashMap<>();
    private final Map<String, Object> clientAssessment = new LinkedHashMap<>();
    private final List<Map<String, Object>> hostServiceEvents = new ArrayList<>();
    private final Map<String, Map<String, Object>> clientRuntimeServices = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> clientRuntimeScreens = new LinkedHashMap<>();
    private Map<String, Object> lastHostServiceEvent = Map.of();
    private Map<String, Object> lastInputState = Map.of();
    private Map<String, Object> lastMouseState = Map.of();
    private Map<String, Object> lastOverlayFocusState = Map.of();
    private Map<String, Object> lastGuiLayerState = Map.of();
    private Map<String, Object> lastHudLayerState = Map.of();
    private Map<String, Object> lastTickState = Map.of();
    private NativeLoaderLiveClientBridge liveClientBridge = NativeLoaderLiveClientBridge.UNATTACHED;
    private boolean attached = false;

    public NativeLoaderClientUiHost() {
        seedBuiltInProductRoutes();
    }

    public static synchronized void seedBuiltInProductRoutes() {
        registerBuiltInRoute(
                "echo-native-loader:main_menu",
                "main_menu",
                Map.of(
                        "menu.open", Map.of("kind", "native_loader_menu", "command", "open"),
                        "menu.new_run", Map.of("kind", "native_loader_menu", "command", "new_run"),
                        "menu.continue", Map.of("kind", "native_loader_menu", "command", "continue"),
                        "menu.settings", Map.of("kind", "native_loader_menu", "command", "settings"),
                        "menu.quit", Map.of("kind", "native_loader_menu", "command", "quit")
                ),
                Map.of(
                        "nativeSurfaceImplementationClass", NativeLoaderClientUiHost.class.getName(),
                        "nativeScreenBridgeClass", NativeLoaderLiveClientBridge.class.getName(),
                        "productSurface", "native_loader_main_menu",
                        "source", "native_loader_builtin_product_route",
                        "visibleRenderRequiresWindowedClient", true
                )
        );
        NativeLoaderClientRouteTable.registerInputBinding("main_menu", "menu.open", Map.of(
                "keyMapping", "key.echo.native.menu",
                "keyCode", 256,
                "inputType", "press",
                "action", "menu.open",
                "source", "native_loader_builtin_product_route"
        ));
        NativeLoaderClientRouteTable.registerInputBinding("main_menu", "menu.new_run", Map.of(
                "keyMapping", "key.echo.native.menu.new_run",
                "keyCode", 257,
                "inputType", "press",
                "action", "menu.new_run",
                "source", "native_loader_builtin_product_route"
        ));
        NativeLoaderClientRouteTable.registerInputBinding("main_menu", "menu.quit", Map.of(
                "keyMapping", "key.echo.native.menu.quit",
                "keyCode", 256,
                "inputType", "press",
                "action", "menu.quit",
                "source", "native_loader_builtin_product_route"
        ));
        registerBuiltInRoute(
                "echo-native-loader:world_setup",
                "world_setup",
                Map.of(
                        "world_setup.open", Map.of("kind", "native_loader_world_setup", "command", "open"),
                        "world_setup.create", Map.of("kind", "native_loader_world_setup", "command", "create"),
                        "world_setup.back", Map.of("kind", "native_loader_world_setup", "command", "back")
                ),
                Map.of(
                        "nativeSurfaceImplementationClass", NativeLoaderClientUiHost.class.getName(),
                        "nativeScreenBridgeClass", NativeLoaderLiveClientBridge.class.getName(),
                        "productSurface", "world_setup",
                        "source", "native_loader_builtin_product_route",
                        "visibleRenderRequiresWindowedClient", true
                )
        );
        registerBuiltInRoute(
                "echo-native-loader:loading",
                "loading_screen",
                Map.of(
                        "loading.open", Map.of("kind", "native_loader_loading", "command", "open"),
                        "loading.render", Map.of("kind", "native_loader_loading", "command", "render"),
                        "loading.progress", Map.of("kind", "native_loader_loading", "command", "progress"),
                        "loading.complete", Map.of("kind", "native_loader_loading", "command", "complete")
                ),
                Map.of(
                        "nativeSurfaceImplementationClass", NativeLoaderClientUiHost.class.getName(),
                        "nativeScreenBridgeClass", NativeLoaderLiveClientBridge.class.getName(),
                        "productSurface", "native_loader_loading",
                        "source", "native_loader_builtin_product_route",
                        "visibleRenderRequiresWindowedClient", true
                )
        );
        registerBuiltInRoute(
                "echo-native-loader:generated_dashboard",
                "native_dashboard",
                Map.of(
                        "dashboard.render", Map.of("kind", "native_loader_dashboard", "command", "render"),
                        "dashboard.mouse", Map.of("kind", "native_loader_dashboard", "command", "mouse"),
                        "dashboard.character", Map.of("kind", "native_loader_dashboard", "command", "character"),
                        "dashboard.edit", Map.of("kind", "native_loader_dashboard", "command", "edit"),
                        "dashboard.list_navigation", Map.of("kind", "native_loader_dashboard", "command", "list_navigation"),
                        "dashboard.settings", Map.of("kind", "native_loader_dashboard", "command", "settings"),
                        "dashboard.submit", Map.of("kind", "native_loader_dashboard", "command", "submit"),
                        "dashboard.navigate", Map.of("kind", "native_loader_dashboard", "command", "navigate"),
                        "dashboard.close", Map.of("kind", "native_loader_dashboard", "command", "close")
                ),
                Map.of(
                        "nativeSurfaceImplementationClass", NativeLoaderClientUiHost.class.getName(),
                        "nativeScreenBridgeClass", NativeLoaderLiveClientBridge.class.getName(),
                        "productSurface", "generated_dashboard",
                        "source", "native_loader_generated_dashboard_route",
                        "visibleRenderRequiresWindowedClient", true
                )
        );
    }

    public static Map<String, Object> markerFields(Map<String, Object> nativeClientUiBridge) {
        Map<String, Object> bridge = nativeClientUiBridge == null ? Map.of() : nativeClientUiBridge;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeClientUiMarkerServiceId", SERVICE_ID);
        fields.put("nativeClientUiHostAttached", Boolean.TRUE.equals(bridge.get("clientUiHostAttached")));
        fields.put("nativeUiFallbackHostAttached", Boolean.TRUE.equals(bridge.get("fallbackHostAttached")));
        fields.put("nativeHeadlessUiHostAttached", Boolean.TRUE.equals(bridge.get("headlessUiHostAttached")));
        fields.put("nativeTerminalFallbackReady", Boolean.TRUE.equals(bridge.get("terminalFallbackReady")));
        fields.put("nativeIndexFallbackReady", Boolean.TRUE.equals(bridge.get("indexFallbackReady")));
        fields.put("nativeLensFallbackReady", Boolean.TRUE.equals(bridge.get("lensFallbackReady")));
        fields.put("nativeHudFallbackReady", Boolean.TRUE.equals(bridge.get("hudFallbackReady")));
        fields.put("nativeCustomMainMenuReady", Boolean.TRUE.equals(bridge.get("customMainMenuReady")));
        fields.put("nativeModuleDeclaredClientSurfaceCount", intValue(bridge.get("moduleDeclaredClientSurfaceCount")));
        fields.put("nativeModuleDeclaredClientSurfaceIds",
                bridge.getOrDefault("moduleDeclaredClientSurfaceIds", List.of()));
        fields.put("nativeModuleDeclaredClientSurfaceTypes",
                bridge.getOrDefault("moduleDeclaredClientSurfaceTypes", List.of()));
        fields.put("nativeProfileExpectedClientSurfaceCount",
                intValue(bridge.get("profileExpectedClientSurfaceCount")));
        fields.put("nativeProfileExpectedClientSurfaceTypes",
                bridge.getOrDefault("profileExpectedClientSurfaceTypes", List.of()));
        fields.put("nativeProfileDeclaredClientSurfaceTypes",
                bridge.getOrDefault("profileDeclaredClientSurfaceTypes", List.of()));
        fields.put("nativeProfileMissingClientSurfaceTypes",
                bridge.getOrDefault("profileMissingClientSurfaceTypes", List.of()));
        fields.put("nativeProfileClientSurfaceContractSatisfied",
                Boolean.TRUE.equals(bridge.get("profileClientSurfaceContractSatisfied")));
        fields.put("nativeModuleDeclaredClientSurfacesPromoted",
                Boolean.TRUE.equals(bridge.get("moduleDeclaredClientSurfacesPromoted")));
        fields.put("nativeLiveClientBridgeRequiredByModuleDeclarations",
                Boolean.TRUE.equals(bridge.get("liveClientBridgeRequiredByModuleDeclarations")));
        return Map.copyOf(fields);
    }

    public synchronized void attach(Map<String, Object> assessment) {
        this.clientAssessment.clear();
        this.clientAssessment.putAll(assessment == null ? Map.of() : assessment);
        this.attached = true;
    }

    public synchronized void attachLiveBridge(NativeLoaderLiveClientBridge liveClientBridge) {
        this.liveClientBridge = liveClientBridge == null
                ? NativeLoaderLiveClientBridge.UNATTACHED
                : liveClientBridge;
    }

    public synchronized void registerSurface(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Object> config
    ) {
        registerSurfaceStatus(moduleId, surfaceId, surfaceType, config);
    }

    public synchronized EchoNativeLoadStatus registerSurfaceStatus(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Object> config
    ) {
        if (blank(moduleId) || blank(surfaceId) || blank(surfaceType)) {
            recordSurface(
                    moduleId,
                    surfaceId,
                    surfaceType,
                    config == null ? Map.of() : Map.copyOf(config),
                    false,
                    false,
                    false,
                    EchoNativeLoadStatus.FAILED,
                    Map.of("failure", "moduleId, surfaceId, and surfaceType are required")
            );
            return EchoNativeLoadStatus.FAILED;
        }
        Map<String, Object> safeConfig = config == null ? Map.of() : Map.copyOf(config);
        if (liveClientBridge.attached()) {
            EchoNativeLoadStatus status = liveClientBridge.registerSurface(moduleId, surfaceId, surfaceType, safeConfig);
            if (status != null && status != EchoNativeLoadStatus.UNSUPPORTED) {
                boolean nativeClientRouteTableMutated = nativeClientRouteTableMutationAccepted(status);
                EchoNativeLoadStatus effectiveStatus = nativeClientRouteTableMutated
                        ? EchoNativeLoadStatus.MUTATED
                        : status;
                Map<String, Object> bridgeEvidence = liveClientBridge.surfaceRegistrationEvidence(
                        moduleId,
                        surfaceId,
                        surfaceType,
                        safeConfig
                );
                recordSurface(
                        moduleId,
                        surfaceId,
                        surfaceType,
                        safeConfig,
                        status != EchoNativeLoadStatus.FAILED,
                        status == EchoNativeLoadStatus.MUTATED,
                        nativeClientRouteTableMutated,
                        effectiveStatus,
                        bridgeEvidence
                );
                return effectiveStatus;
            }
        }
        recordSurface(
                moduleId,
                surfaceId,
                surfaceType,
                safeConfig,
                false,
                false,
                false,
                EchoNativeLoadStatus.REGISTERED,
                Map.of(
                        "nativeClientUiHostRegistered", true,
                        "liveClientBridgeRequired", true
                )
        );
        return EchoNativeLoadStatus.REGISTERED;
    }

    private void recordSurface(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Object> config,
            boolean liveClientBridgeAccepted,
            boolean liveClientBridgeMutated,
            boolean nativeClientRouteTableMutated,
            EchoNativeLoadStatus status,
            Map<String, Object> bridgeEvidence
    ) {
        NativeLoaderClientRouteTable.registerRoute(
                moduleId,
                surfaceId,
                surfaceType,
                config,
                bridgeEvidence,
                liveClientBridgeMutated || nativeClientRouteTableMutated
        );
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("moduleId", moduleId);
        entry.put("surfaceId", surfaceId);
        entry.put("surfaceType", surfaceType);
        entry.put("config", config);
        entry.put("liveClientBridgeAccepted", liveClientBridgeAccepted);
        entry.put("liveClientBridgeMutated", liveClientBridgeMutated);
        entry.put("nativeClientRouteTableMutated", nativeClientRouteTableMutated);
        entry.put("status", status == null ? EchoNativeLoadStatus.UNSUPPORTED.name() : status.name());
        entry.put("liveClientBridgeEvidence", bridgeEvidence == null ? Map.of() : Map.copyOf(bridgeEvidence));
        entry.put("nativeClientSurfaceLifecycle",
                NativeLoaderClientRouteTable.lifecycle(surfaceType).toEvidence());
        if (liveClientBridgeAccepted) {
            entry.put("liveClientBridgeId", liveClientBridge.bridgeId());
        } else {
            entry.put("liveClientBridgeId", liveClientBridge.bridgeId());
            entry.put("liveClientBridgeRequired", true);
        }
        surfaces.put(moduleId + ":" + surfaceId, entry);
    }

    public synchronized Map<String, Map<String, Object>> surfaces() {
        return Map.copyOf(surfaces);
    }

    public synchronized Map<String, Object> clientAssessment() {
        return Map.copyOf(clientAssessment);
    }

    public boolean dispatchRoute(String surfaceType, String actionId) {
        return dispatchRoute(surfaceType, actionId, Map.of("source", "native_loader_client_ui_host"));
    }

    public boolean dispatchRoute(String surfaceType, String actionId, Map<String, Object> metadata) {
        EchoNativeLoadStatus status = dispatchRouteStatus(surfaceType, actionId, metadata);
        return status == EchoNativeLoadStatus.MUTATED;
    }

    public EchoNativeLoadStatus dispatchRouteStatus(String surfaceType, String actionId, Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.dispatchRoute(
                surfaceType,
                actionId,
                safeMetadata);
        EchoNativeLoadStatus status;
        if (bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            status = bridgeStatus;
        } else {
            status = NativeLoaderClientRouteTable.dispatchStatus(surfaceType, actionId, safeMetadata);
        }
        recordHostService("route_dispatch", surfaceType, actionId, status,
                hostServiceMetadata(safeMetadata, "route_dispatch"));
        return status;
    }

    public boolean dispatchInputBinding(String keyMapping, int keyCode, String inputType) {
        EchoNativeLoadStatus status = dispatchInputBindingStatus(keyMapping, keyCode, inputType);
        return status == EchoNativeLoadStatus.MUTATED;
    }

    public boolean dispatchInputBinding(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        EchoNativeLoadStatus status = dispatchInputBindingStatus(keyMapping, keyCode, inputType, metadata);
        return status == EchoNativeLoadStatus.MUTATED;
    }

    public EchoNativeLoadStatus dispatchInputBindingStatus(String keyMapping, int keyCode, String inputType) {
        return dispatchInputBindingStatus(keyMapping, keyCode, inputType, Map.of());
    }

    public EchoNativeLoadStatus dispatchInputBindingStatus(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        Map<String, Object> safeMetadata = inputServiceMetadata(
                metadata,
                "input_binding",
                keyMapping,
                keyCode,
                inputType);
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.dispatchInputBinding(
                keyMapping,
                keyCode,
                inputType,
                safeMetadata);
        EchoNativeLoadStatus status;
        if (bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            status = bridgeStatus;
        } else {
            status = NativeLoaderClientRouteTable.dispatchInputBindingStatus(keyMapping, keyCode, inputType, safeMetadata);
        }
        recordHostService("input_binding", "", keyMapping, status, safeMetadata, Map.of(
                "inputDispatch", NativeLoaderClientRouteTable.latestInputDispatchEvent()
        ));
        return status;
    }

    public EchoNativeLoadStatus tick(String phase) {
        return tick(phase, Map.of());
    }

    public EchoNativeLoadStatus tick(String phase, Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = new LinkedHashMap<>(hostServiceMetadata(metadata, "tick"));
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.tick(phase, safeMetadata);
        EchoNativeLoadStatus status;
        if (bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            status = bridgeStatus;
        } else {
            status = publishForMountedSurfaces("tick", phase, safeMetadata);
        }
        recordHostService("tick", "", phase, status, safeMetadata);
        return status;
    }

    public EchoNativeLoadStatus tickRoute(
            String surfaceType,
            String actionId,
            String phase,
            Map<String, Object> metadata
    ) {
        Map<String, Object> safeMetadata = new LinkedHashMap<>(hostServiceMetadata(metadata, "tick"));
        safeMetadata.put("tickRouteDispatch", true);
        safeMetadata.put("tickRouteActionId", actionId == null ? "" : actionId);
        safeMetadata.putIfAbsent("eventType", "client_tick_post");
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.tick(phase, safeMetadata);
        EchoNativeLoadStatus status;
        if (bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            status = bridgeStatus;
        } else {
            status = NativeLoaderClientRouteTable.dispatchStatus(surfaceType, actionId, safeMetadata);
        }
        recordHostService("tick", surfaceType, actionId, status, safeMetadata);
        return status;
    }

    public EchoNativeLoadStatus mountSurface(String surfaceType, String actionId) {
        return mountSurface(surfaceType, actionId, Map.of());
    }

    public EchoNativeLoadStatus mountSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        String safeActionId = blank(actionId)
                ? builtInProductActionForHostPhase(surfaceType, "mount")
                : actionId;
        if (blank(safeActionId)) {
            safeActionId = "native_loader.mount_surface";
        }
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, "screen_lifecycle");
        EchoNativeLoadStatus status = screenLifecycle(
                surfaceType,
                "mount",
                safeActionId,
                safeMetadata
        );
        recordHostService("screen_mount", surfaceType, safeActionId, status, safeMetadata);
        return status;
    }

    public EchoNativeLoadStatus openSurface(String surfaceType, String actionId) {
        return openSurface(surfaceType, actionId, Map.of());
    }

    public EchoNativeLoadStatus openSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        String safeActionId = blank(actionId)
                ? builtInProductActionForHostPhase(surfaceType, "open")
                : actionId;
        if (blank(safeActionId)) {
            safeActionId = "native_loader.open_surface";
        }
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, "screen_lifecycle");
        EchoNativeLoadStatus finalStatus = screenLifecycle(
                surfaceType,
                "open",
                safeActionId,
                safeMetadata
        );
        recordHostService("screen_open", surfaceType, safeActionId, finalStatus, safeMetadata);
        return finalStatus;
    }

    public EchoNativeLoadStatus closeSurface(String surfaceType, String actionId) {
        return closeSurface(surfaceType, actionId, Map.of());
    }

    public EchoNativeLoadStatus closeSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        String safeActionId = blank(actionId)
                ? builtInProductActionForHostPhase(surfaceType, "close")
                : actionId;
        if (blank(safeActionId)) {
            safeActionId = "native_loader.close_surface";
        }
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, "screen_lifecycle");
        EchoNativeLoadStatus status = screenLifecycle(
                surfaceType,
                "close",
                safeActionId,
                safeMetadata
        );
        recordHostService("screen_close", surfaceType, safeActionId, status, safeMetadata);
        return status;
    }

    public EchoNativeLoadStatus unmountSurface(String surfaceType, String actionId) {
        return unmountSurface(surfaceType, actionId, Map.of());
    }

    public EchoNativeLoadStatus unmountSurface(String surfaceType, String actionId, Map<String, Object> metadata) {
        String safeActionId = blank(actionId)
                ? builtInProductActionForHostPhase(surfaceType, "unmount")
                : actionId;
        if (blank(safeActionId)) {
            safeActionId = "native_loader.unmount_surface";
        }
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, "screen_lifecycle");
        EchoNativeLoadStatus status = screenLifecycle(
                surfaceType,
                "unmount",
                safeActionId,
                safeMetadata
        );
        recordHostService("screen_unmount", surfaceType, safeActionId, status, safeMetadata);
        return status;
    }

    private EchoNativeLoadStatus screenLifecycle(String surfaceType, String phase, String actionId) {
        return screenLifecycle(surfaceType, phase, actionId, Map.of());
    }

    public EchoNativeLoadStatus screenLifecycleEvent(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        String safeActionId = blank(actionId)
                ? builtInProductActionForHostPhase(surfaceType, phase)
                : actionId;
        if (blank(safeActionId)) {
            safeActionId = "native_loader." + phase;
        }
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, "screen_lifecycle");
        EchoNativeLoadStatus status = screenLifecycle(surfaceType, phase, safeActionId, safeMetadata);
        recordHostService("screen_lifecycle", surfaceType, safeActionId, status, safeMetadata);
        return status;
    }

    private EchoNativeLoadStatus screenLifecycle(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        String safeActionId = blank(actionId)
                ? builtInProductActionForHostPhase(surfaceType, phase)
                : actionId;
        if (blank(safeActionId)) {
            safeActionId = "native_loader." + phase;
        }
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, "screen_lifecycle");
        boolean lifecycleHandoff = Boolean.TRUE.equals(safeMetadata.get("nativeLoaderScreenLifecycleHandoff"));
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.screenLifecycle(
                surfaceType,
                phase,
                safeActionId,
                safeMetadata
        );
        EchoNativeLoadStatus lifecycleStatus = lifecycleHandoff
                ? NativeLoaderClientRouteTable.publishLifecycleEvent(surfaceType, phase, safeActionId, safeMetadata)
                : NativeLoaderClientRouteTable.screenLifecycle(
                        surfaceType,
                        phase,
                        safeActionId,
                        safeMetadata
                );
        EchoNativeLoadStatus status;
        if (bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            status = merge(lifecycleStatus, bridgeStatus);
        } else {
            status = safeActionId.startsWith("native_loader.") || lifecycleHandoff
                    ? lifecycleStatus
                    : merge(lifecycleStatus, NativeLoaderClientRouteTable.dispatchStatus(
                            surfaceType,
                            safeActionId,
                            safeMetadata));
        }
        return status;
    }

    public EchoNativeLoadStatus focusOverlay(String surfaceType, boolean focused) {
        return focusOverlay(surfaceType, focused, Map.of());
    }

    public EchoNativeLoadStatus focusOverlay(
            String surfaceType,
            boolean focused,
            Map<String, Object> metadata
    ) {
        Map<String, Object> safeMetadata = focusServiceMetadata(metadata, focused);
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.overlayFocus(
                surfaceType,
                focused,
                safeMetadata
        );
        EchoNativeLoadStatus status;
        if (bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            status = bridgeStatus;
        } else {
            EchoNativeLoadStatus lifecycleStatus = NativeLoaderClientRouteTable.publishLifecycleEvent(
                    surfaceType,
                    focused ? "focus" : "blur",
                    "native_loader.overlay_focus",
                    safeMetadata
            );
            status = merge(lifecycleStatus, NativeLoaderClientRouteTable.dispatchStatus(
                    surfaceType,
                    "native_loader.overlay_focus",
                    safeMetadata));
        }
        recordHostService("overlay_focus", surfaceType, "native_loader.overlay_focus", status, safeMetadata);
        return status;
    }

    public EchoNativeLoadStatus keyInput(String keyMapping, int keyCode, String inputType) {
        return keyInput(keyMapping, keyCode, inputType, Map.of());
    }

    public EchoNativeLoadStatus keyInput(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        EchoNativeLoadStatus status = dispatchInputBindingStatus(keyMapping, keyCode, inputType, metadata);
        recordHostService("key_input", "", keyMapping, status, inputServiceMetadata(
                metadata,
                "key_input",
                keyMapping,
                keyCode,
                inputType
        ), Map.of(
                "inputDispatch", NativeLoaderClientRouteTable.latestInputDispatchEvent()
        ));
        return status;
    }

    public EchoNativeLoadStatus mouseInput(String surfaceType, String actionId, Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = mouseServiceMetadata(metadata);
        String safeActionId = blank(actionId) ? "native_loader.mouse" : actionId;
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.mouseInput(
                surfaceType,
                safeActionId,
                safeMetadata
        );
        EchoNativeLoadStatus finalStatus;
        if (bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            finalStatus = bridgeStatus;
        } else {
            EchoNativeLoadStatus eventStatus = NativeLoaderClientRouteTable.publishLifecycleEvent(
                    surfaceType,
                    "mouse",
                    safeActionId,
                    safeMetadata
            );
            finalStatus = blank(actionId)
                    ? eventStatus
                    : merge(eventStatus, NativeLoaderClientRouteTable.dispatchStatus(surfaceType, actionId, safeMetadata));
        }
        recordHostService("mouse", surfaceType, safeActionId, finalStatus, safeMetadata);
        return finalStatus;
    }

    public EchoNativeLoadStatus overlayInput(String surfaceType, String actionId, Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, "overlay_input");
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.overlayInput(surfaceType, actionId, safeMetadata);
        EchoNativeLoadStatus status = bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED
                ? bridgeStatus
                : NativeLoaderClientRouteTable.dispatchStatus(surfaceType, actionId, safeMetadata);
        recordHostService("overlay_input", surfaceType, actionId, status, safeMetadata);
        return status;
    }

    public EchoNativeLoadStatus renderGuiLayer(String surfaceType, String actionId) {
        return renderGuiLayer(surfaceType, actionId, Map.of());
    }

    public EchoNativeLoadStatus renderGuiLayer(String surfaceType, String actionId, Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, "gui_layer");
        String safeActionId = resolvedHostActionId(surfaceType, "render", actionId);
        if (blank(safeActionId)) {
            safeActionId = "native_loader.render";
        }
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.renderGuiLayer(
                surfaceType,
                safeActionId,
                safeMetadata
        );
        EchoNativeLoadStatus status;
        if (bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            status = bridgeStatus;
        } else {
            status = renderLayer(surfaceType, safeActionId, "gui_layer", safeMetadata);
        }
        recordHostService("gui_layer", surfaceType, safeActionId, status, safeMetadata);
        return status;
    }

    public EchoNativeLoadStatus renderHudLayer(String surfaceType, String actionId) {
        return renderHudLayer(surfaceType, actionId, Map.of());
    }

    public EchoNativeLoadStatus renderHudLayer(String surfaceType, String actionId, Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, "hud_layer");
        String safeActionId = resolvedHostActionId(surfaceType, "render", actionId);
        if (blank(safeActionId)) {
            safeActionId = "native_loader.render";
        }
        EchoNativeLoadStatus bridgeStatus = liveClientBridge.renderHudLayer(
                surfaceType,
                safeActionId,
                safeMetadata
        );
        EchoNativeLoadStatus status;
        if (bridgeStatus != null && bridgeStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            status = bridgeStatus;
        } else {
            status = renderLayer(surfaceType, safeActionId, "hud_layer", safeMetadata);
        }
        recordHostService("hud_layer", surfaceType, safeActionId, status, safeMetadata);
        return status;
    }

    public synchronized Map<String, Object> routeHostEvidence() {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("serviceId", SERVICE_ID);
        evidence.put("attached", attached);
        evidence.put("surfaceCount", surfaces.size());
        evidence.put("liveClientBridgeId", liveClientBridge.bridgeId());
        evidence.put("firstClassNativeClientRouteTable", firstClassNativeClientRouteTable());
        evidence.put("nativeClientRouteProcess", nativeClientRouteProcess());
        evidence.put("releaseClientRouteTrusted", releaseClientRouteTrusted());
        evidence.put("clientRouteMutationSupported", clientRouteMutationSupported());
        evidence.put("releaseGateEvidence", releaseGateEvidence());
        evidence.put("routes", NativeLoaderClientRouteTable.routes());
        evidence.put("routesBySurfaceType", NativeLoaderClientRouteTable.routesBySurfaceType());
        evidence.put("actions", NativeLoaderClientRouteTable.actions());
        evidence.put("declaredActionEvidence", declaredActionEvidence());
        evidence.put("actionRoutes", NativeLoaderClientRouteTable.actionRouteEvidence());
        evidence.put("actionDispatchEvidence", NativeLoaderClientRouteTable.actionDispatchEvidence());
        evidence.put("actionHandlers", NativeLoaderClientRouteTable.actionHandlerEvidence());
        evidence.put("inputBindings", NativeLoaderClientRouteTable.inputBindings());
        evidence.put("inputDispatchEvidence", NativeLoaderClientRouteTable.inputDispatchEvidence());
        evidence.put("hostServiceEvents", hostServiceEvents());
        evidence.put("hostServiceEvidence", hostServiceEvidence());
        evidence.put("clientRuntimeState", clientRuntimeState());
        evidence.put("lifecycles", lifecycleEvidence());
        evidence.put("lifecycleEvents", NativeLoaderClientRouteTable.lifecycleEvents());
        evidence.put("lifecycleEventEvidence", NativeLoaderClientRouteTable.lifecycleEventEvidence());
        evidence.put("mountedSurfaceRoutes", NativeLoaderClientRouteTable.mountedSurfaceRoutes());
        evidence.put("visibleSurfaceRoutes", NativeLoaderClientRouteTable.visibleSurfaceRoutes());
        evidence.put("builtInProductSurfaceState", builtInProductSurfaceState());
        evidence.put("liveClientBridgeHostServices", liveClientBridge.clientHostServiceEvidence());
        evidence.put("neoForgeEventOwnershipRequired", false);
        return Map.copyOf(evidence);
    }

    public synchronized boolean attached() {
        return attached;
    }

    public synchronized boolean liveClientBridgeAttached() {
        return liveClientBridge.attached();
    }

    public synchronized String liveClientBridgeId() {
        return liveClientBridge.bridgeId();
    }

    public synchronized boolean nativeLoaderOwnsClientHostServices() {
        return liveClientBridge.nativeLoaderOwnsClientHostServices();
    }

    public synchronized boolean neoForgeClientEventsCompatibilityAdaptersOnly() {
        return liveClientBridge.neoForgeClientEventsCompatibilityAdaptersOnly();
    }

    public synchronized boolean firstClassNativeClientRouteTable() {
        return liveClientBridge.firstClassNativeClientRouteTable();
    }

    public synchronized boolean nativeClientRouteProcess() {
        return liveClientBridge.nativeClientRouteProcess();
    }

    public synchronized boolean releaseClientRouteTrusted() {
        return liveClientBridge.releaseClientRouteTrusted();
    }

    public synchronized boolean clientRouteMutationSupported() {
        return liveClientBridge.clientRouteMutationSupported();
    }

    public synchronized boolean firstClassNativeClientRenderPipeline() {
        return liveClientBridge.firstClassNativeClientRenderPipeline();
    }

    public synchronized boolean nativeClientRenderProcess() {
        return liveClientBridge.nativeClientRenderProcess();
    }

    public synchronized boolean releaseClientRenderTrusted() {
        return liveClientBridge.releaseClientRenderTrusted();
    }

    public synchronized boolean clientRenderMutationSupported() {
        return liveClientBridge.clientRenderMutationSupported();
    }

    public synchronized int surfaceCount() {
        return surfaces.size();
    }

    public synchronized int liveClientBridgeAcceptedSurfaceCount() {
        int count = 0;
        for (Map<String, Object> surface : surfaces.values()) {
            if (Boolean.TRUE.equals(surface.get("liveClientBridgeAccepted"))) {
                count++;
            }
        }
        return count;
    }

    public synchronized int liveClientBridgeMutatedSurfaceCount() {
        int count = 0;
        for (Map<String, Object> surface : surfaces.values()) {
            if (Boolean.TRUE.equals(surface.get("liveClientBridgeMutated"))) {
                count++;
            }
        }
        return count;
    }

    public synchronized int nativeClientRouteTableMutatedSurfaceCount() {
        int count = 0;
        for (Map<String, Object> surface : surfaces.values()) {
            if (Boolean.TRUE.equals(surface.get("nativeClientRouteTableMutated"))) {
                count++;
            }
        }
        return count;
    }

    public synchronized int trustedClientRouteMutatedSurfaceCount() {
        int count = 0;
        for (Map<String, Object> surface : surfaces.values()) {
            if (Boolean.TRUE.equals(surface.get("liveClientBridgeMutated"))
                    || Boolean.TRUE.equals(surface.get("nativeClientRouteTableMutated"))) {
                count++;
            }
        }
        return count;
    }

    public static synchronized Map<String, Map<String, Object>> builtInProductSurfaceState() {
        Map<String, Map<String, Object>> snapshot = new LinkedHashMap<>();
        BUILT_IN_PRODUCT_SURFACE_STATE.forEach((surfaceType, state) ->
                snapshot.put(surfaceType, Map.copyOf(state)));
        return Map.copyOf(snapshot);
    }

    public synchronized List<Map<String, Object>> hostServiceEvents() {
        return List.copyOf(hostServiceEvents);
    }

    public synchronized Map<String, Object> hostServiceEvidence() {
        return Map.of(
                "eventCount", hostServiceEvents.size(),
                "events", hostServiceEvents(),
                "summary", hostServiceSummary()
        );
    }

    public synchronized Map<String, Object> clientRuntimeState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("source", "native_loader_client_ui_host");
        state.put("hostServiceEventCount", hostServiceEvents.size());
        state.put("lastHostServiceEvent", lastHostServiceEvent);
        state.put("services", Map.copyOf(clientRuntimeServices));
        state.put("screens", Map.copyOf(clientRuntimeScreens));
        state.put("input", lastInputState);
        state.put("mouse", lastMouseState);
        state.put("overlayFocus", lastOverlayFocusState);
        state.put("guiLayer", lastGuiLayerState);
        state.put("hudLayer", lastHudLayerState);
        state.put("tick", lastTickState);
        state.put("mountedSurfaceRoutes", NativeLoaderClientRouteTable.mountedSurfaceRoutes());
        state.put("visibleSurfaceRoutes", NativeLoaderClientRouteTable.visibleSurfaceRoutes());
        state.put("activeUiSurfaces", activeUiSurfaces());
        return Map.copyOf(state);
    }

    private Map<String, Map<String, Object>> activeUiSurfaces() {
        Map<String, Map<String, Object>> active = new LinkedHashMap<>();
        addRouteSurfaceStates(active, NativeLoaderClientRouteTable.mountedSurfaceRoutes(), "mounted");
        addRouteSurfaceStates(active, NativeLoaderClientRouteTable.visibleSurfaceRoutes(), "visible");
        addSurfaceActivity(active, clientRuntimeScreens, "screen");
        addSurfaceActivity(active, Map.of("input", lastInputState), "input");
        addSurfaceActivity(active, Map.of("mouse", lastMouseState), "mouse");
        addSurfaceActivity(active, Map.of("overlayFocus", lastOverlayFocusState), "overlayFocus");
        addSurfaceActivity(active, Map.of("guiLayer", lastGuiLayerState), "guiLayer");
        addSurfaceActivity(active, Map.of("hudLayer", lastHudLayerState), "hudLayer");
        Map<String, Map<String, Object>> snapshot = new LinkedHashMap<>();
        active.forEach((surfaceType, state) -> snapshot.put(surfaceType, Map.copyOf(state)));
        return Map.copyOf(snapshot);
    }

    private Map<String, Object> releaseGateEvidence() {
        List<String> requiredSurfaces = List.of(
                "terminal",
                "index",
                "lens",
                "holomap",
                "hud",
                "hud_widget",
                "hud_layout",
                "client_overlay",
                "main_menu",
                "world_setup",
                "loading_screen"
        );
        Map<String, Map<String, Object>> trustedSurfaceRoutes = new LinkedHashMap<>();
        Map<String, Boolean> requiredSurfaceTrusted = new LinkedHashMap<>();
        for (String surfaceType : requiredSurfaces) {
            Map<String, Object> route = NativeLoaderClientRouteTable.routeForSurface(surfaceType);
            boolean trusted = routeTrusted(route);
            requiredSurfaceTrusted.put(surfaceType, trusted);
            if (!route.isEmpty()) {
                trustedSurfaceRoutes.put(surfaceType, route);
            }
        }
        Map<String, String> requiredActions = Map.ofEntries(
                Map.entry("terminal:terminal.open", "terminal"),
                Map.entry("index:index.catalog", "index"),
                Map.entry("lens:lens.deep_scan", "lens"),
                Map.entry("holomap:holomap.open", "holomap"),
                Map.entry("holomap:holomap.minimap.render", "holomap"),
                Map.entry("hud:hud.render", "hud"),
                Map.entry("hud:hud.update_snapshot", "hud"),
                Map.entry("hud:native_loader.overlay_focus", "hud"),
                Map.entry("hud_widget:hud.mission_tracker.render", "hud_widget"),
                Map.entry("hud_widget:hud.hazard_readout.render", "hud_widget"),
                Map.entry("hud_widget:hud.compass_indicator.render", "hud_widget"),
                Map.entry("hud_layout:hud.screen_safe_area.resolve", "hud_layout"),
                Map.entry("client_overlay:terminal.mission_hud.render", "client_overlay"),
                Map.entry("client_overlay:index.inventory_overlay_render", "client_overlay"),
                Map.entry("client_overlay:lens.overlay.render", "client_overlay"),
                Map.entry("main_menu:menu.open", "main_menu"),
                Map.entry("main_menu:menu.new_run", "main_menu"),
                Map.entry("main_menu:menu.quit", "main_menu"),
                Map.entry("world_setup:world_setup.open", "world_setup"),
                Map.entry("world_setup:world_setup.create", "world_setup"),
                Map.entry("world_setup:world_setup.back", "world_setup"),
                Map.entry("loading_screen:loading.open", "loading_screen"),
                Map.entry("loading_screen:loading.render", "loading_screen"),
                Map.entry("loading_screen:loading.progress", "loading_screen"),
                Map.entry("loading_screen:loading.complete", "loading_screen")
        );
        Map<String, Boolean> requiredActionTrusted = new LinkedHashMap<>();
        Map<String, Map<String, Object>> trustedActionRoutes = new LinkedHashMap<>();
        for (Map.Entry<String, String> requiredAction : requiredActions.entrySet()) {
            String[] actionKey = requiredAction.getKey().split(":", 2);
            if (actionKey.length != 2) {
                requiredActionTrusted.put(requiredAction.getKey(), false);
                continue;
            }
            Map<String, Object> route = NativeLoaderClientRouteTable.routeForAction(actionKey[0], actionKey[1]);
            boolean trusted = routeTrusted(route);
            requiredActionTrusted.put(requiredAction.getKey(), trusted);
            if (!route.isEmpty()) {
                trustedActionRoutes.put(requiredAction.getKey(), route);
            }
        }
        Map<String, String> requiredInputBindings = Map.ofEntries(
                Map.entry("terminal:terminal.open", "key.echoterminal.open"),
                Map.entry("index:index.catalog", "key.echoindex.catalog"),
                Map.entry("index:index.recipe", "key.echoindex.recipe"),
                Map.entry("index:index.usage", "key.echoindex.usage"),
                Map.entry("index:index.bookmark", "key.echoindex.bookmark"),
                Map.entry("lens:lens.deep_scan", "echolens.key.deep_scan"),
                Map.entry("lens:lens.index_recipe", "key.echolens.index_recipe"),
                Map.entry("lens:lens.index_usage", "key.echolens.index_usage"),
                Map.entry("lens:lens.track_in_index", "key.echolens.track_in_index"),
                Map.entry("holomap:holomap.open", "key.echoholomap.open_map"),
                Map.entry("holomap:holomap.toggle_minimap", "key.echoholomap.toggle_minimap"),
                Map.entry("holomap:holomap.zoom_in", "key.echoholomap.minimap_zoom_in"),
                Map.entry("holomap:holomap.zoom_out", "key.echoholomap.minimap_zoom_out"),
                Map.entry("holomap:holomap.cycle_corner", "key.echoholomap.minimap_cycle_corner"),
                Map.entry("main_menu:menu.open", "key.echo.native.menu"),
                Map.entry("main_menu:menu.new_run", "key.echo.native.menu.new_run"),
                Map.entry("main_menu:menu.quit", "key.echo.native.menu.quit")
        );
        Map<String, Boolean> requiredInputBindingTrusted = new LinkedHashMap<>();
        for (Map.Entry<String, String> requiredBinding : requiredInputBindings.entrySet()) {
            String[] bindingKey = requiredBinding.getKey().split(":", 2);
            requiredInputBindingTrusted.put(requiredBinding.getKey(),
                    bindingKey.length == 2
                            && inputBindingPresent(bindingKey[0], bindingKey[1], requiredBinding.getValue()));
        }
        Map<String, Boolean> requiredHostInputMutationPresent =
                hostInputMutationPresence(requiredInputBindings);
        Map<String, Map<String, String>> requiredRuntimeMutations = Map.ofEntries(
                Map.entry("terminal:terminal.open",
                        runtimeMutationRequirement("terminal", "screen", "screen_open", "terminal.open")),
                Map.entry("index:index.catalog_screen.mouse",
                        runtimeMutationRequirement("index", "mouse", "mouse", "index.catalog_screen.mouse")),
                Map.entry("lens:lens.overlay.render",
                        runtimeMutationRequirement("client_overlay", "guiLayer", "gui_layer", "lens.overlay.render")),
                Map.entry("holomap:holomap.open",
                        runtimeMutationRequirement("holomap", "screen", "screen_open", "holomap.open")),
                Map.entry("holomap:holomap.cycle_corner",
                        runtimeMutationRequirement("holomap", "input", "key_input", "key.echoholomap.minimap_cycle_corner")),
                Map.entry("hud:hud.render",
                        runtimeMutationRequirement("hud", "hudLayer", "hud_layer", "hud.render")),
                Map.entry("hud_widget:hud.compass_indicator.render",
                        runtimeMutationRequirement("hud_widget", "hudLayer", "hud_layer",
                                "hud.compass_indicator.render")),
                Map.entry("main_menu:menu.quit",
                        runtimeMutationRequirement("main_menu", "screen", "screen_close", "menu.quit")),
                Map.entry("loading_screen:loading.complete",
                        runtimeMutationRequirement("loading_screen", "screen", "screen_unmount", "loading.complete"))
        );
        Map<String, Boolean> requiredRuntimeMutationPresent = runtimeMutationPresence(requiredRuntimeMutations);
        Map<String, Map<String, String>> requiredHostServiceMutations = Map.ofEntries(
                Map.entry("terminal:terminal.open",
                        hostServiceMutationRequirement("terminal", "screen_open", "terminal.open")),
                Map.entry("terminal:terminal.screen.mouse_scroll",
                        hostServiceMutationRequirement("terminal", "mouse", "terminal.screen.mouse_scroll")),
                Map.entry("terminal:terminal.mission_hud.render",
                        hostServiceMutationRequirement("client_overlay", "gui_layer", "terminal.mission_hud.render")),
                Map.entry("terminal:terminal.screencore.action",
                        hostServiceMutationRequirement("terminal", "route_dispatch", "terminal.screencore.action")),
                Map.entry("index:index.catalog",
                        hostServiceMutationRequirement("index", "route_dispatch", "index.catalog")),
                Map.entry("index:index.catalog_screen.mouse",
                        hostServiceMutationRequirement("index", "mouse", "index.catalog_screen.mouse")),
                Map.entry("index:index.recipe_screen.key",
                        hostServiceMutationRequirement("index", "route_dispatch", "index.recipe_screen.key")),
                Map.entry("client_overlay:index.inventory_overlay_input",
                        hostServiceMutationRequirement("client_overlay", "route_dispatch", "index.inventory_overlay_input")),
                Map.entry("index:index.inventory_overlay_render",
                        hostServiceMutationRequirement("client_overlay", "gui_layer", "index.inventory_overlay_render")),
                Map.entry("lens:lens.deep_scan",
                        hostServiceMutationRequirement("lens", "route_dispatch", "lens.deep_scan")),
                Map.entry("lens:lens.overlay.render",
                        hostServiceMutationRequirement("client_overlay", "gui_layer", "lens.overlay.render")),
                Map.entry("holomap:holomap.open",
                        hostServiceMutationRequirement("holomap", "screen_open", "holomap.open")),
                Map.entry("holomap:holomap.minimap.render",
                        hostServiceMutationRequirement("holomap", "gui_layer", "holomap.minimap.render")),
                Map.entry("holomap:holomap.fullscreen.key",
                        hostServiceMutationRequirement("holomap", "route_dispatch", "holomap.fullscreen.key")),
                Map.entry("holomap:holomap.fullscreen.mouse",
                        hostServiceMutationRequirement("holomap", "route_dispatch", "holomap.fullscreen.mouse")),
                Map.entry("holomap:holomap.fullscreen.scroll",
                        hostServiceMutationRequirement("holomap", "route_dispatch", "holomap.fullscreen.scroll")),
                Map.entry("holomap:holomap.close",
                        hostServiceMutationRequirement("holomap", "route_dispatch", "holomap.close")),
                Map.entry("hud:hud.render",
                        hostServiceMutationRequirement("hud", "hud_layer", "hud.render")),
                Map.entry("hud:hud.update_snapshot",
                        hostServiceMutationRequirement("hud", "route_dispatch", "hud.update_snapshot")),
                Map.entry("hud:native_loader.overlay_focus",
                        hostServiceMutationRequirement("hud", "overlay_focus", "native_loader.overlay_focus")),
                Map.entry("hud_widget:hud.compass_indicator.render",
                        hostServiceMutationRequirement("hud_widget", "hud_layer", "hud.compass_indicator.render")),
                Map.entry("hud_layout:hud.screen_safe_area.resolve",
                        hostServiceMutationRequirement("hud_layout", "route_dispatch", "hud.screen_safe_area.resolve")),
                Map.entry("main_menu:menu.open",
                        hostServiceMutationRequirement("main_menu", "screen_open", "menu.open")),
                Map.entry("main_menu:menu.new_run",
                        hostServiceMutationRequirement("main_menu", "route_dispatch", "menu.new_run")),
                Map.entry("loading_screen:loading.render",
                        hostServiceMutationRequirement("loading_screen", "gui_layer", "loading.render")),
                Map.entry("loading_screen:loading.progress",
                        hostServiceMutationRequirement("loading_screen", "route_dispatch", "loading.progress")),
                Map.entry("loading_screen:loading.complete",
                        hostServiceMutationRequirement("loading_screen", "screen_unmount", "loading.complete"))
        );
        Map<String, Boolean> requiredHostServiceMutationPresent =
                hostServiceMutationPresence(requiredHostServiceMutations);
        Map<String, Map<String, String>> requiredHostTickMutations = Map.ofEntries(
                Map.entry("client_overlay:terminal.mission_hud.tick",
                        routeMutationRequirement("client_overlay", "terminal.mission_hud.tick")),
                Map.entry("client_overlay:terminal.discovery_toast.tick",
                        routeMutationRequirement("client_overlay", "terminal.discovery_toast.tick"))
        );
        Map<String, Boolean> requiredHostTickMutationPresent =
                hostTickMutationPresence(requiredHostTickMutations);
        Map<String, Object> summaryPresence = new LinkedHashMap<>();
        Map<String, Object> hostEvidence = hostServiceEvidence();
        Map<String, Object> actionEvidence = NativeLoaderClientRouteTable.actionDispatchEvidence();
        Map<String, Object> inputEvidence = NativeLoaderClientRouteTable.inputDispatchEvidence();
        Map<String, Object> lifecycleEvidence = NativeLoaderClientRouteTable.lifecycleEventEvidence();
        Map<String, Object> bridgeEvidence = liveClientBridge.clientHostServiceEvidence();
        summaryPresence.put("hostServiceEvidence", hasSummary(hostEvidence));
        summaryPresence.put("actionDispatchEvidence", hasSummary(actionEvidence));
        summaryPresence.put("declaredActionEvidence", hasSummary(declaredActionEvidence()));
        summaryPresence.put("inputDispatchEvidence", hasSummary(inputEvidence));
        summaryPresence.put("lifecycleEventEvidence", hasSummary(lifecycleEvidence));
        summaryPresence.put("liveClientBridgeServiceSummary", bridgeEvidence.get("serviceSummary") instanceof Map<?, ?>);
        summaryPresence.put("liveClientBridgeActiveRoutes", bridgeEvidence.get("activeClientRoutes") instanceof Map<?, ?>);
        Map<String, Object> sourceSummaryPresence = new LinkedHashMap<>();
        sourceSummaryPresence.put("hostServiceSourceCounts", summaryContains(hostEvidence, "sourceCounts"));
        sourceSummaryPresence.put("actionDispatchMetadataSourceCounts", summaryContains(actionEvidence, "metadataSourceCounts"));
        sourceSummaryPresence.put("inputDispatchMetadataSourceCounts", summaryContains(inputEvidence, "metadataSourceCounts"));
        sourceSummaryPresence.put("lifecycleMetadataSourceCounts", summaryContains(lifecycleEvidence, "metadataSourceCounts"));
        sourceSummaryPresence.put("liveClientBridgeSourceCounts", bridgeSummaryContains(bridgeEvidence, "sourceCounts"));
        Map<String, Object> requiredSourceEvidence = new LinkedHashMap<>();
        requiredSourceEvidence.put("nativeLoaderUiHost",
                summarySourcePresent(hostEvidence, "sourceCounts", "native_loader_client_ui_host"));
        requiredSourceEvidence.put("directPublicSdkRouteDispatch",
                summarySourcePresent(actionEvidence, "metadataSourceCounts", "agent2_direct_public_sdk_probe"));
        requiredSourceEvidence.put("directPublicSdkInputDispatch",
                summarySourcePresent(inputEvidence, "metadataSourceCounts", "agent2_direct_public_sdk_input_probe"));
        requiredSourceEvidence.put("directPublicSdkLifecycle",
                summarySourcePresent(lifecycleEvidence, "metadataSourceCounts", "agent2_direct_public_sdk_lifecycle_probe"));
        requiredSourceEvidence.put("defaultProductClientBridge",
                bridgeSummarySourcePresent(bridgeEvidence, "sourceCounts", "native_loader_default_product_client_bridge"));
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", "native_loader_client_ui_host");
        evidence.put("nativeClientRouteProcess", nativeClientRouteProcess());
        evidence.put("neoForgeEventOwnershipRequired", false);
        evidence.put("requiredSurfaces", requiredSurfaces);
        evidence.put("requiredSurfaceTrusted", Map.copyOf(requiredSurfaceTrusted));
        evidence.put("allRequiredSurfacesTrusted", requiredSurfaceTrusted.values().stream().allMatch(Boolean.TRUE::equals));
        evidence.put("trustedSurfaceRoutes", copyMapValues(trustedSurfaceRoutes));
        evidence.put("requiredActions", Map.copyOf(requiredActions));
        evidence.put("requiredActionTrusted", Map.copyOf(requiredActionTrusted));
        evidence.put("allRequiredActionsTrusted", requiredActionTrusted.values().stream().allMatch(Boolean.TRUE::equals));
        evidence.put("trustedActionRoutes", copyMapValues(trustedActionRoutes));
        evidence.put("requiredInputBindings", Map.copyOf(requiredInputBindings));
        evidence.put("requiredInputBindingTrusted", Map.copyOf(requiredInputBindingTrusted));
        evidence.put("allRequiredInputBindingsTrusted",
                requiredInputBindingTrusted.values().stream().allMatch(Boolean.TRUE::equals));
        evidence.put("requiredHostInputMutationPresent", Map.copyOf(requiredHostInputMutationPresent));
        evidence.put("allRequiredHostInputMutationsPresent",
                requiredHostInputMutationPresent.values().stream().allMatch(Boolean.TRUE::equals));
        evidence.put("requiredRuntimeMutations", copyNestedStringMap(requiredRuntimeMutations));
        evidence.put("requiredRuntimeMutationPresent", Map.copyOf(requiredRuntimeMutationPresent));
        evidence.put("allRequiredRuntimeMutationsPresent",
                requiredRuntimeMutationPresent.values().stream().allMatch(Boolean.TRUE::equals));
        evidence.put("requiredHostServiceMutations", copyNestedStringMap(requiredHostServiceMutations));
        evidence.put("requiredHostServiceMutationPresent", Map.copyOf(requiredHostServiceMutationPresent));
        evidence.put("allRequiredHostServiceMutationsPresent",
                requiredHostServiceMutationPresent.values().stream().allMatch(Boolean.TRUE::equals));
        evidence.put("requiredHostTickMutations", copyNestedStringMap(requiredHostTickMutations));
        evidence.put("requiredHostTickMutationPresent", Map.copyOf(requiredHostTickMutationPresent));
        evidence.put("allRequiredHostTickMutationsPresent",
                requiredHostTickMutationPresent.values().stream().allMatch(Boolean.TRUE::equals));
        evidence.put("summaryPresence", Map.copyOf(summaryPresence));
        evidence.put("allRequiredSummariesPresent", summaryPresence.values().stream().allMatch(Boolean.TRUE::equals));
        evidence.put("sourceSummaryPresence", Map.copyOf(sourceSummaryPresence));
        evidence.put("allRequiredSourceSummariesPresent",
                sourceSummaryPresence.values().stream().allMatch(Boolean.TRUE::equals));
        evidence.put("requiredSourceEvidence", Map.copyOf(requiredSourceEvidence));
        evidence.put("allRequiredSourceEvidencePresent",
                requiredSourceEvidence.values().stream().allMatch(Boolean.TRUE::equals));
        return Map.copyOf(evidence);
    }

    private static Map<String, String> runtimeMutationRequirement(
            String surfaceType,
            String activityKey,
            String service,
            String actionId
    ) {
        return Map.of(
                "surfaceType", surfaceType,
                "activityKey", activityKey,
                "service", service,
                "actionId", actionId
        );
    }

    private static Map<String, String> hostServiceMutationRequirement(
            String surfaceType,
            String service,
            String actionId
    ) {
        return Map.of(
                "surfaceType", surfaceType,
                "service", service,
                "actionId", actionId
        );
    }

    private static Map<String, String> routeMutationRequirement(String surfaceType, String actionId) {
        return Map.of(
                "surfaceType", surfaceType,
                "actionId", actionId
        );
    }

    private Map<String, Boolean> runtimeMutationPresence(
            Map<String, Map<String, String>> requiredRuntimeMutations
    ) {
        Map<String, Map<String, Object>> active = activeUiSurfaces();
        Map<String, Boolean> presence = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> requirement : requiredRuntimeMutations.entrySet()) {
            Map<String, String> expected = requirement.getValue();
            Map<String, Object> surface = active.getOrDefault(expected.get("surfaceType"), Map.of());
            Object activityObject = surface.get(expected.get("activityKey"));
            boolean currentRuntimeMutation = activityObject instanceof Map<?, ?> activity
                    && expected.get("service").equals(valueOrDefault(activity, "service",
                            valueOrDefault(activity, "lastService", "")))
                    && expected.get("actionId").equals(valueOrDefault(activity, "actionId",
                            valueOrDefault(activity, "lastActionId", "")))
                    && EchoNativeLoadStatus.MUTATED.name().equals(valueOrDefault(activity, "status",
                            valueOrDefault(activity, "lastStatus", "")))
                    && Boolean.TRUE.equals(surface.get("activeRouteTrustedMutation"))
                    && EchoNativeLoadStatus.MUTATED.name().equals(surface.get("activeRouteStatus"))
                    && Boolean.TRUE.equals(surface.get("activeNativeClientRouteProcess"))
                    && Boolean.FALSE.equals(surface.get("activeNeoForgeEventOwnershipRequired"))
                    && surface.get("activeRoute") instanceof Map<?, ?>;
            presence.put(requirement.getKey(),
                    currentRuntimeMutation || historicalRuntimeMutationPresent(expected));
        }
        return Map.copyOf(presence);
    }

    private boolean historicalRuntimeMutationPresent(Map<String, String> expected) {
        for (Map<String, Object> event : hostServiceEvents) {
            if (runtimeMutationEventMatches(hostServiceEventSnapshot(event), expected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean runtimeMutationEventMatches(
            Map<String, Object> snapshot,
            Map<String, String> expected
    ) {
        return expected.get("surfaceType").equals(String.valueOf(snapshot.getOrDefault("surfaceType", "")))
                && expected.get("service").equals(String.valueOf(snapshot.getOrDefault("service", "")))
                && expected.get("actionId").equals(String.valueOf(snapshot.getOrDefault("actionId", "")))
                && EchoNativeLoadStatus.MUTATED.name().equals(snapshot.get("status"))
                && Boolean.TRUE.equals(snapshot.get("activeRouteTrustedMutation"))
                && EchoNativeLoadStatus.MUTATED.name().equals(snapshot.get("activeRouteStatus"))
                && Boolean.TRUE.equals(snapshot.get("activeNativeClientRouteProcess"))
                && Boolean.FALSE.equals(snapshot.get("activeNeoForgeEventOwnershipRequired"))
                && snapshot.get("activeRoute") instanceof Map<?, ?>;
    }

    private Map<String, Boolean> hostTickMutationPresence(
            Map<String, Map<String, String>> requiredHostTickMutations
    ) {
        Map<String, Boolean> presence = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> requirement : requiredHostTickMutations.entrySet()) {
            presence.put(requirement.getKey(), false);
        }
        Object eventsObject = NativeLoaderClientRouteTable.actionDispatchEvidence().get("events");
        if (!(eventsObject instanceof List<?> events)) {
            return Map.copyOf(presence);
        }
        for (Object eventObject : events) {
            if (!(eventObject instanceof Map<?, ?> event)) {
                continue;
            }
            for (Map.Entry<String, Map<String, String>> requirement : requiredHostTickMutations.entrySet()) {
                if (!Boolean.TRUE.equals(presence.get(requirement.getKey()))
                        && hostTickMutationMatches(event, requirement.getValue())) {
                    presence.put(requirement.getKey(), true);
                }
            }
        }
        return Map.copyOf(presence);
    }

    private static boolean hostTickMutationMatches(Map<?, ?> event, Map<String, String> expected) {
        Object metadataObject = event.get("metadata");
        return expected.get("surfaceType").equals(String.valueOf(valueOrDefault(event, "surfaceType", "")))
                && expected.get("actionId").equals(String.valueOf(valueOrDefault(event, "actionId", "")))
                && EchoNativeLoadStatus.MUTATED.name().equals(event.get("status"))
                && Boolean.TRUE.equals(event.get("handled"))
                && event.get("route") instanceof Map<?, ?> route
                && Boolean.TRUE.equals(route.get("trustedMutation"))
                && EchoNativeLoadStatus.MUTATED.name().equals(route.get("status"))
                && route.get("evidence") instanceof Map<?, ?> evidence
                && Boolean.TRUE.equals(evidence.get("nativeClientRouteProcess"))
                && Boolean.FALSE.equals(evidence.get("neoForgeEventOwnershipRequired"))
                && Boolean.TRUE.equals(evidence.get("clientRouteMutationSupported"))
                && metadataObject instanceof Map<?, ?> metadata
                && Boolean.TRUE.equals(metadata.get("tickRouteDispatch"))
                && "native_loader_client_ui_host".equals(metadata.get("source"));
    }

    private Map<String, Boolean> hostInputMutationPresence(Map<String, String> requiredInputBindings) {
        Map<String, Boolean> presence = new LinkedHashMap<>();
        for (Map.Entry<String, String> requiredBinding : requiredInputBindings.entrySet()) {
            presence.put(requiredBinding.getKey(), false);
        }
        for (Map<String, Object> event : hostServiceEvents) {
            Map<String, Object> snapshot = hostServiceEventSnapshot(event);
            for (Map.Entry<String, String> requiredBinding : requiredInputBindings.entrySet()) {
                if (!Boolean.TRUE.equals(presence.get(requiredBinding.getKey()))
                        && hostInputMutationMatches(snapshot, requiredBinding.getKey(), requiredBinding.getValue())) {
                    presence.put(requiredBinding.getKey(), true);
                }
            }
        }
        return Map.copyOf(presence);
    }

    private static boolean hostInputMutationMatches(
            Map<String, Object> snapshot,
            String requiredBinding,
            String keyMapping
    ) {
        String[] bindingKey = requiredBinding.split(":", 2);
        if (bindingKey.length != 2) {
            return false;
        }
        return "key_input".equals(String.valueOf(snapshot.getOrDefault("service", "")))
                && keyMapping.equals(String.valueOf(snapshot.getOrDefault("actionId", "")))
                && bindingKey[0].equals(String.valueOf(snapshot.getOrDefault("activeSurfaceType", "")))
                && bindingKey[1].equals(String.valueOf(snapshot.getOrDefault("activeActionId", "")))
                && EchoNativeLoadStatus.MUTATED.name().equals(snapshot.get("status"))
                && Boolean.TRUE.equals(snapshot.get("activeRouteTrustedMutation"))
                && EchoNativeLoadStatus.MUTATED.name().equals(snapshot.get("activeRouteStatus"))
                && Boolean.TRUE.equals(snapshot.get("activeNativeClientRouteProcess"))
                && Boolean.FALSE.equals(snapshot.get("activeNeoForgeEventOwnershipRequired"))
                && snapshot.get("activeRoute") instanceof Map<?, ?>
                && snapshot.get("inputDispatch") instanceof Map<?, ?>;
    }

    private Map<String, Boolean> hostServiceMutationPresence(
            Map<String, Map<String, String>> requiredHostServiceMutations
    ) {
        Map<String, Boolean> presence = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> requirement : requiredHostServiceMutations.entrySet()) {
            presence.put(requirement.getKey(), false);
        }
        for (Map<String, Object> event : hostServiceEvents) {
            Map<String, Object> snapshot = hostServiceEventSnapshot(event);
            for (Map.Entry<String, Map<String, String>> requirement : requiredHostServiceMutations.entrySet()) {
                if (!Boolean.TRUE.equals(presence.get(requirement.getKey()))
                        && hostServiceMutationMatches(snapshot, requirement.getValue())) {
                    presence.put(requirement.getKey(), true);
                }
            }
        }
        return Map.copyOf(presence);
    }

    private static boolean hostServiceMutationMatches(
            Map<String, Object> snapshot,
            Map<String, String> expected
    ) {
        String surfaceType = expected.get("surfaceType");
        String actionId = expected.get("actionId");
        return expected.get("service").equals(String.valueOf(snapshot.getOrDefault("service", "")))
                && (surfaceType.equals(String.valueOf(snapshot.getOrDefault("activeSurfaceType", "")))
                || surfaceType.equals(String.valueOf(snapshot.getOrDefault("surfaceType", ""))))
                && (actionId.equals(String.valueOf(snapshot.getOrDefault("activeActionId", "")))
                || actionId.equals(String.valueOf(snapshot.getOrDefault("actionId", ""))))
                && EchoNativeLoadStatus.MUTATED.name().equals(snapshot.get("status"))
                && Boolean.TRUE.equals(snapshot.get("activeRouteTrustedMutation"))
                && EchoNativeLoadStatus.MUTATED.name().equals(snapshot.get("activeRouteStatus"))
                && Boolean.TRUE.equals(snapshot.get("activeNativeClientRouteProcess"))
                && Boolean.FALSE.equals(snapshot.get("activeNeoForgeEventOwnershipRequired"))
                && snapshot.get("activeRoute") instanceof Map<?, ?>;
    }

    private static Map<String, Map<String, String>> copyNestedStringMap(
            Map<String, Map<String, String>> values
    ) {
        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(key, Map.copyOf(value)));
        return Map.copyOf(copy);
    }

    private static boolean hasSummary(Map<String, Object> evidence) {
        return evidence != null && evidence.get("summary") instanceof Map<?, ?>;
    }

    private static boolean summaryContains(Map<String, Object> evidence, String key) {
        return evidence != null
                && evidence.get("summary") instanceof Map<?, ?> summary
                && summary.get(key) instanceof Map<?, ?>;
    }

    private static boolean bridgeSummaryContains(Map<String, Object> evidence, String key) {
        return evidence != null
                && evidence.get("serviceSummary") instanceof Map<?, ?> summary
                && summary.get(key) instanceof Map<?, ?>;
    }

    private static boolean summarySourcePresent(Map<String, Object> evidence, String summaryKey, String source) {
        return evidence != null
                && evidence.get("summary") instanceof Map<?, ?> summary
                && summary.get(summaryKey) instanceof Map<?, ?> sourceCounts
                && sourceCounts.get(source) instanceof Number count
                && count.intValue() > 0;
    }

    private static boolean bridgeSummarySourcePresent(Map<String, Object> evidence, String summaryKey, String source) {
        return evidence != null
                && evidence.get("serviceSummary") instanceof Map<?, ?> summary
                && summary.get(summaryKey) instanceof Map<?, ?> sourceCounts
                && sourceCounts.get(source) instanceof Number count
                && count.intValue() > 0;
    }

    private static boolean inputBindingPresent(String surfaceType, String actionId, String keyMapping) {
        Map<String, Map<String, List<Map<String, Object>>>> bindings = NativeLoaderClientRouteTable.inputBindings();
        Map<String, List<Map<String, Object>>> actions = bindings.getOrDefault(surfaceType, Map.of());
        List<Map<String, Object>> actionBindings = actions.getOrDefault(actionId, List.of());
        for (Map<String, Object> binding : actionBindings) {
            if (keyMapping.equals(String.valueOf(binding.getOrDefault("keyMapping", "")))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> declaredActionEvidence() {
        Map<String, Map<String, Map<String, Object>>> actions = NativeLoaderClientRouteTable.declaredActions();
        Map<String, Object> tickDrivenBySurface = new LinkedHashMap<>();
        int actionCount = 0;
        int tickDrivenActionCount = 0;
        for (Map.Entry<String, Map<String, Map<String, Object>>> surfaceEntry : actions.entrySet()) {
            Map<String, Object> tickDrivenActions = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> actionEntry : surfaceEntry.getValue().entrySet()) {
                actionCount++;
                if (tickDrivenAction(actionEntry.getKey(), actionEntry.getValue())) {
                    tickDrivenActionCount++;
                    Map<String, Object> action = new LinkedHashMap<>(actionEntry.getValue());
                    action.put("actionId", actionEntry.getKey());
                    tickDrivenActions.put(actionEntry.getKey(), Map.copyOf(action));
                }
            }
            if (!tickDrivenActions.isEmpty()) {
                tickDrivenBySurface.put(surfaceEntry.getKey(), Map.copyOf(tickDrivenActions));
            }
        }
        return Map.of(
                "actionCount", actionCount,
                "actionsBySurface", actions,
                "summary", Map.of(
                        "tickDrivenActionCount", tickDrivenActionCount,
                        "tickDrivenBySurface", Map.copyOf(tickDrivenBySurface)
                )
        );
    }

    private static boolean routeTrusted(Map<String, Object> route) {
        return route != null
                && !route.isEmpty()
                && Boolean.TRUE.equals(route.get("trustedMutation"))
                && EchoNativeLoadStatus.MUTATED.name().equals(route.get("status"))
                && route.get("evidence") instanceof Map<?, ?> evidence
                && Boolean.TRUE.equals(evidence.get("nativeClientRouteProcess"))
                && Boolean.FALSE.equals(evidence.get("neoForgeEventOwnershipRequired"))
                && Boolean.TRUE.equals(evidence.get("clientRouteMutationSupported"));
    }

    private static void addRouteSurfaceStates(
            Map<String, Map<String, Object>> active,
            Map<String, Map<String, Object>> routes,
            String lifecycleState
    ) {
        routes.forEach((surfaceType, route) -> {
            Map<String, Object> state = activeSurfaceState(active, surfaceType);
            state.put("source", "native_loader_client_ui_host");
            state.put("surfaceType", surfaceType);
            state.put(lifecycleState, true);
            state.put("route", route);
            state.put("routeModuleId", route.getOrDefault("moduleId", ""));
            state.put("routeSurfaceId", route.getOrDefault("surfaceId", ""));
            state.put("routeTrustedMutation", route.getOrDefault("trustedMutation", false));
            state.put("routeStatus", route.getOrDefault("status", ""));
            Object evidence = route.get("evidence");
            if (evidence instanceof Map<?, ?> evidenceMap) {
                state.put("nativeClientRouteProcess",
                        Boolean.TRUE.equals(evidenceMap.get("nativeClientRouteProcess")));
                state.put("neoForgeEventOwnershipRequired",
                        Boolean.TRUE.equals(evidenceMap.get("neoForgeEventOwnershipRequired")));
            }
        });
    }

    private static void addSurfaceActivity(
            Map<String, Map<String, Object>> active,
            Map<String, ? extends Map<String, Object>> states,
            String activityKey
    ) {
        states.forEach((ignored, activity) -> {
            if (activity == null || activity.isEmpty()) {
                return;
            }
            String surfaceType = textActivitySurfaceType(activity);
            if (surfaceType.isBlank()) {
                return;
            }
            Map<String, Object> state = activeSurfaceState(active, surfaceType);
            state.put(activityKey, Map.copyOf(activity));
            state.put("lastActivity", activityKey);
            state.put("lastService", activity.getOrDefault("service", activity.getOrDefault("lastService", "")));
            state.put("lastActionId", activity.getOrDefault("actionId", activity.getOrDefault("lastActionId", "")));
            state.put("lastStatus", activity.getOrDefault("status", activity.getOrDefault("lastStatus", "")));
            putCurrentLifecycleState(state, activityKey, activity);
            putActiveRouteEvidence(state, activity);
        });
    }

    private static Map<String, Object> activeSurfaceState(
            Map<String, Map<String, Object>> active,
            String surfaceType
    ) {
        return active.computeIfAbsent(surfaceType, key -> {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("source", "native_loader_client_ui_host");
            state.put("surfaceType", key);
            state.put("mounted", false);
            state.put("visible", false);
            state.put("currentMounted", false);
            state.put("currentOpen", false);
            state.put("currentVisible", false);
            return state;
        });
    }

    private static String textActivitySurfaceType(Map<String, Object> activity) {
        Object targetSurfaceType = activity.get("inputTargetSurfaceType");
        if (targetSurfaceType instanceof String inputTargetSurface && !inputTargetSurface.isBlank()) {
            return inputTargetSurface;
        }
        Object surfaceType = activity.get("surfaceType");
        return surfaceType instanceof String value ? value : "";
    }

    private static void putCurrentLifecycleState(
            Map<String, Object> state,
            String activityKey,
            Map<String, Object> activity
    ) {
        if ("screen".equals(activityKey)) {
            boolean open = booleanValue(activity.get("open"));
            boolean mounted = booleanValue(activity.get("mounted")) || open;
            state.put("currentMounted", mounted);
            state.put("currentOpen", open);
            state.put("currentVisible", open);
        } else if ("guiLayer".equals(activityKey) || "hudLayer".equals(activityKey)) {
            state.put("currentMounted", true);
            state.put("currentVisible", true);
        } else if ("overlayFocus".equals(activityKey)) {
            boolean focused = false;
            Object metadata = activity.get("metadata");
            if (metadata instanceof Map<?, ?> metadataMap) {
                focused = booleanValue(metadataMap.get("focused"));
            }
            state.put("currentFocused", focused);
            state.put("currentMounted", true);
            state.put("currentVisible", focused);
        } else if ("input".equals(activityKey)) {
            state.put("currentInputActive", true);
        } else if ("mouse".equals(activityKey)) {
            state.put("currentMouseActive", true);
        }
    }

    private static boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private static void putActiveRouteEvidence(Map<String, Object> state, Map<String, Object> activity) {
        Object routeObject = activity.get("route");
        if (!(routeObject instanceof Map<?, ?>) && activity.get("inputRoute") instanceof Map<?, ?>) {
            routeObject = activity.get("inputRoute");
        }
        if (routeObject instanceof Map<?, ?> route) {
            state.put("activeRoute", Map.copyOf(route));
            putIfPresent(state, "activeRouteModuleId", route.get("moduleId"));
            putIfPresent(state, "activeRouteSurfaceId", route.get("surfaceId"));
            putIfPresent(state, "activeRouteStatus", route.get("status"));
            putIfPresent(state, "activeRouteTrustedMutation", route.get("trustedMutation"));
            Object evidence = route.get("evidence");
            if (evidence instanceof Map<?, ?> evidenceMap) {
                state.put("activeNativeClientRouteProcess",
                        Boolean.TRUE.equals(evidenceMap.get("nativeClientRouteProcess")));
                state.put("activeNeoForgeEventOwnershipRequired",
                        Boolean.TRUE.equals(evidenceMap.get("neoForgeEventOwnershipRequired")));
            }
        }
        putFirstPresent(state, "activeRouteModuleId", activity, "inputRouteModuleId", "routeModuleId");
        putFirstPresent(state, "activeRouteSurfaceId", activity, "inputRouteSurfaceId", "routeSurfaceId");
        putFirstPresent(state, "activeRouteStatus", activity, "inputRouteStatus", "routeStatus");
        putFirstPresent(state, "activeRouteTrustedMutation", activity,
                "inputRouteTrustedMutation", "routeTrustedMutation");
        if (activity.containsKey("nativeClientRouteProcess")) {
            state.put("activeNativeClientRouteProcess",
                    Boolean.TRUE.equals(activity.get("nativeClientRouteProcess")));
        }
        if (activity.containsKey("neoForgeEventOwnershipRequired")) {
            state.put("activeNeoForgeEventOwnershipRequired",
                    Boolean.TRUE.equals(activity.get("neoForgeEventOwnershipRequired")));
        }
    }

    private static void putFirstPresent(
            Map<String, Object> state,
            String outputKey,
            Map<String, Object> activity,
            String firstKey,
            String secondKey
    ) {
        Object first = activity.get(firstKey);
        if (hasValue(first)) {
            state.put(outputKey, first);
            return;
        }
        Object second = activity.get(secondKey);
        if (hasValue(second)) {
            state.put(outputKey, second);
        }
    }

    private static void putIfPresent(Map<String, Object> state, String key, Object value) {
        if (hasValue(value)) {
            state.put(key, value);
        }
    }

    private static boolean hasValue(Object value) {
        return !(value == null || (value instanceof String text && text.isBlank()));
    }

    private EchoNativeLoadStatus renderLayer(
            String surfaceType,
            String actionId,
            String service,
            Map<String, Object> metadata
    ) {
        Map<String, Object> safeMetadata = hostServiceMetadata(metadata, service);
        String safeActionId = blank(actionId)
                ? builtInProductActionForHostPhase(surfaceType, "render")
                : actionId;
        if (blank(safeActionId)) {
            safeActionId = "native_loader.render";
        }
        EchoNativeLoadStatus eventStatus = NativeLoaderClientRouteTable.publishLifecycleEvent(
                surfaceType,
                "render",
                safeActionId,
                safeMetadata
        );
        if (safeActionId.startsWith("native_loader.")) {
            return eventStatus;
        }
        return merge(eventStatus, NativeLoaderClientRouteTable.dispatchStatus(surfaceType, safeActionId, safeMetadata));
    }

    private EchoNativeLoadStatus publishForMountedSurfaces(
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        EchoNativeLoadStatus aggregate = EchoNativeLoadStatus.UNSUPPORTED;
        Map<String, Map<String, Map<String, Object>>> declaredActions =
                NativeLoaderClientRouteTable.declaredActions();
        for (String surfaceType : NativeLoaderClientRouteTable.mountedSurfaceRoutes().keySet()) {
            aggregate = merge(aggregate, NativeLoaderClientRouteTable.publishLifecycleEvent(
                    surfaceType,
                    phase,
                    blank(actionId) ? "native_loader." + phase : actionId,
                    metadata
            ));
            if ("tick".equals(phase)) {
                Map<String, Map<String, Object>> actions = declaredActions.getOrDefault(surfaceType, Map.of());
                for (Map.Entry<String, Map<String, Object>> action : actions.entrySet()) {
                    if (tickDrivenAction(action.getKey(), action.getValue())) {
                        aggregate = merge(aggregate, NativeLoaderClientRouteTable.dispatchStatus(
                                surfaceType,
                                action.getKey(),
                                tickDispatchMetadata(metadata, action.getKey(), action.getValue())));
                    }
                }
            }
        }
        return aggregate;
    }

    private static boolean tickDrivenAction(String actionId, Map<String, Object> actionMetadata) {
        String safeActionId = actionId == null ? "" : actionId.trim().toLowerCase(Locale.ROOT);
        String kind = String.valueOf(actionMetadata == null ? "" : actionMetadata.getOrDefault("kind", ""))
                .trim()
                .toLowerCase(Locale.ROOT);
        return safeActionId.endsWith(".tick") || kind.contains("tick");
    }

    private static Map<String, Object> tickDispatchMetadata(
            Map<String, Object> metadata,
            String actionId,
            Map<String, Object> actionMetadata
    ) {
        Map<String, Object> safeMetadata = new LinkedHashMap<>();
        if (metadata != null) {
            safeMetadata.putAll(metadata);
        }
        safeMetadata.put("tickRouteDispatch", true);
        safeMetadata.put("tickRouteActionId", actionId == null ? "" : actionId);
        safeMetadata.put("tickRouteAction", actionMetadata == null ? Map.of() : Map.copyOf(actionMetadata));
        safeMetadata.putIfAbsent("eventType", "client_tick_post");
        return Map.copyOf(safeMetadata);
    }

    private Map<String, Object> lifecycleEvidence() {
        Map<String, Object> evidence = new LinkedHashMap<>();
        NativeLoaderClientRouteTable.lifecycles().forEach((surfaceType, lifecycle) ->
                evidence.put(surfaceType, lifecycle.toEvidence()));
        return Map.copyOf(evidence);
    }

    private synchronized void recordHostService(
            String service,
            String surfaceType,
            String actionId,
            EchoNativeLoadStatus status,
            Map<String, Object> metadata
    ) {
        recordHostService(service, surfaceType, actionId, status, metadata, Map.of());
    }

    private synchronized void recordHostService(
            String service,
            String surfaceType,
            String actionId,
            EchoNativeLoadStatus status,
            Map<String, Object> metadata,
            Map<String, Object> extraEvidence
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("source", "native_loader_client_ui_host");
        event.put("service", service == null ? "" : service);
        event.put("surfaceType", surfaceType == null ? "" : surfaceType);
        event.put("actionId", actionId == null ? "" : actionId);
        event.put("status", status == null ? EchoNativeLoadStatus.UNSUPPORTED.name() : status.name());
        event.put("metadata", metadata == null ? Map.of() : Map.copyOf(metadata));
        if (extraEvidence != null) {
            event.putAll(extraEvidence);
        }
        Map<String, Object> safeEvent = Map.copyOf(event);
        hostServiceEvents.add(safeEvent);
        if (hostServiceEvents.size() > 256) {
            hostServiceEvents.remove(0);
        }
        recordClientRuntimeState(safeEvent);
    }

    private Map<String, Object> hostServiceSummary() {
        Map<String, Integer> serviceCounts = new LinkedHashMap<>();
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<String, Integer> sourceCounts = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestByService = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestBySource = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestBySurfaceService = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestByActiveSurfaceService = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestRouteOwnedBySurface = new LinkedHashMap<>();
        for (Map<String, Object> event : hostServiceEvents) {
            Map<String, Object> snapshot = hostServiceEventSnapshot(event);
            String source = String.valueOf(snapshot.getOrDefault("source", ""));
            String service = String.valueOf(snapshot.getOrDefault("service", ""));
            String status = String.valueOf(snapshot.getOrDefault("status", ""));
            String surfaceType = String.valueOf(snapshot.getOrDefault("surfaceType", ""));
            String activeSurfaceType = String.valueOf(snapshot.getOrDefault("activeSurfaceType", ""));
            if (!source.isBlank()) {
                sourceCounts.merge(source, 1, Integer::sum);
                latestBySource.put(source, snapshot);
            }
            if (!service.isBlank()) {
                serviceCounts.merge(service, 1, Integer::sum);
                latestByService.put(service, snapshot);
            }
            if (!status.isBlank()) {
                statusCounts.merge(status, 1, Integer::sum);
            }
            if (!surfaceType.isBlank() && !service.isBlank()) {
                latestBySurfaceService.put(surfaceType + ":" + service, snapshot);
            }
            if (!activeSurfaceType.isBlank() && !service.isBlank()) {
                latestByActiveSurfaceService.put(activeSurfaceType + ":" + service, snapshot);
            }
            if (!activeSurfaceType.isBlank()
                    && Boolean.TRUE.equals(snapshot.get("activeRouteTrustedMutation"))
                    && EchoNativeLoadStatus.MUTATED.name().equals(snapshot.get("activeRouteStatus"))) {
                latestRouteOwnedBySurface.put(activeSurfaceType, snapshot);
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("serviceCounts", Map.copyOf(serviceCounts));
        summary.put("statusCounts", Map.copyOf(statusCounts));
        summary.put("sourceCounts", Map.copyOf(sourceCounts));
        summary.put("latestByService", copyMapValues(latestByService));
        summary.put("latestBySource", copyMapValues(latestBySource));
        summary.put("latestBySurfaceService", copyMapValues(latestBySurfaceService));
        summary.put("latestByActiveSurfaceService", copyMapValues(latestByActiveSurfaceService));
        summary.put("latestRouteOwnedBySurface", copyMapValues(latestRouteOwnedBySurface));
        return Map.copyOf(summary);
    }

    private static Map<String, Object> hostServiceEventSnapshot(Map<String, Object> event) {
        Map<String, Object> snapshot = new LinkedHashMap<>(event == null ? Map.of() : event);
        String surfaceType = String.valueOf(snapshot.getOrDefault("surfaceType", ""));
        String actionId = String.valueOf(snapshot.getOrDefault("actionId", ""));
        putRouteOwnerEvidence(snapshot, surfaceType, actionId);
        putInputTargetEvidence(snapshot, snapshot);
        if (snapshot.containsKey("inputTargetSurfaceType")) {
            snapshot.put("activeSurfaceType", snapshot.getOrDefault("inputTargetSurfaceType", ""));
            snapshot.put("activeActionId", snapshot.getOrDefault("inputTargetActionId", ""));
            snapshot.put("activeRouteModuleId", snapshot.getOrDefault("inputRouteModuleId", ""));
            snapshot.put("activeRouteSurfaceId", snapshot.getOrDefault("inputRouteSurfaceId", ""));
            snapshot.put("activeRouteTrustedMutation", snapshot.getOrDefault("inputRouteTrustedMutation", false));
            snapshot.put("activeRouteStatus", snapshot.getOrDefault("inputRouteStatus", ""));
            Object inputRoute = snapshot.get("inputRoute");
            if (inputRoute instanceof Map<?, ?> routeMap) {
                snapshot.put("activeRoute", Map.copyOf(routeMap));
                putActiveRouteProcessEvidence(snapshot, routeMap);
            }
        } else if (snapshot.containsKey("routeModuleId")) {
            snapshot.put("activeSurfaceType", surfaceType);
            snapshot.put("activeActionId", actionId);
            snapshot.put("activeRouteModuleId", snapshot.getOrDefault("routeModuleId", ""));
            snapshot.put("activeRouteSurfaceId", snapshot.getOrDefault("routeSurfaceId", ""));
            snapshot.put("activeRouteTrustedMutation", snapshot.getOrDefault("routeTrustedMutation", false));
            snapshot.put("activeRouteStatus", snapshot.getOrDefault("routeStatus", ""));
            Object route = snapshot.get("route");
            if (route instanceof Map<?, ?> routeMap) {
                snapshot.put("activeRoute", Map.copyOf(routeMap));
                putActiveRouteProcessEvidence(snapshot, routeMap);
            }
        }
        return Map.copyOf(snapshot);
    }

    private static void putActiveRouteProcessEvidence(Map<String, Object> state, Map<?, ?> route) {
        Object evidence = route.get("evidence");
        if (evidence instanceof Map<?, ?> evidenceMap) {
            state.put("activeNativeClientRouteProcess",
                    Boolean.TRUE.equals(evidenceMap.get("nativeClientRouteProcess")));
            state.put("activeNeoForgeEventOwnershipRequired",
                    Boolean.TRUE.equals(evidenceMap.get("neoForgeEventOwnershipRequired")));
            state.put("activeClientRouteMutationSupported",
                    Boolean.TRUE.equals(evidenceMap.get("clientRouteMutationSupported")));
        }
    }

    private void recordClientRuntimeState(Map<String, Object> event) {
        lastHostServiceEvent = event;
        String service = (String) event.getOrDefault("service", "");
        String surfaceType = (String) event.getOrDefault("surfaceType", "");
        String actionId = (String) event.getOrDefault("actionId", "");
        String status = (String) event.getOrDefault("status", EchoNativeLoadStatus.UNSUPPORTED.name());
        Map<String, Object> serviceState = new LinkedHashMap<>();
        serviceState.put("source", "native_loader_client_ui_host");
        serviceState.put("service", service);
        serviceState.put("surfaceType", surfaceType);
        serviceState.put("actionId", actionId);
        serviceState.put("status", status);
        serviceState.put("metadata", event.getOrDefault("metadata", Map.of()));
        putRouteOwnerEvidence(serviceState, surfaceType, actionId);
        putInputTargetEvidence(serviceState, event);
        clientRuntimeServices.put(service, Map.copyOf(serviceState));
        if (service.startsWith("screen_")) {
            Map<String, Object> screenState = new LinkedHashMap<>(clientRuntimeScreens.getOrDefault(surfaceType, Map.of()));
            screenState.put("source", "native_loader_client_ui_host");
            screenState.put("surfaceType", surfaceType);
            screenState.put("lastService", service);
            screenState.put("lastActionId", actionId);
            screenState.put("lastStatus", status);
            screenState.put("metadata", event.getOrDefault("metadata", Map.of()));
            Map<String, Object> route = NativeLoaderClientRouteTable.routeForAction(surfaceType, actionId);
            if (!route.isEmpty()) {
                screenState.put("route", route);
                screenState.put("routeModuleId", route.getOrDefault("moduleId", ""));
                screenState.put("routeSurfaceId", route.getOrDefault("surfaceId", ""));
                screenState.put("routeTrustedMutation", route.getOrDefault("trustedMutation", false));
                screenState.put("routeStatus", route.getOrDefault("status", ""));
                Object evidence = route.get("evidence");
                if (evidence instanceof Map<?, ?> evidenceMap) {
                    screenState.put("nativeClientRouteProcess",
                            Boolean.TRUE.equals(evidenceMap.get("nativeClientRouteProcess")));
                    screenState.put("neoForgeEventOwnershipRequired",
                            Boolean.TRUE.equals(evidenceMap.get("neoForgeEventOwnershipRequired")));
                }
            }
            screenState.put("mounted", "screen_mount".equals(service)
                    || (!"screen_unmount".equals(service) && Boolean.TRUE.equals(screenState.get("mounted"))));
            screenState.put("open", "screen_open".equals(service)
                    || (!"screen_close".equals(service)
                    && !"screen_unmount".equals(service)
                    && Boolean.TRUE.equals(screenState.get("open"))));
            clientRuntimeScreens.put(surfaceType, Map.copyOf(screenState));
        }
        if ("input_binding".equals(service) || "key_input".equals(service)) {
            lastInputState = Map.copyOf(serviceState);
        } else if ("mouse".equals(service)) {
            lastMouseState = Map.copyOf(serviceState);
        } else if ("overlay_focus".equals(service)) {
            lastOverlayFocusState = Map.copyOf(serviceState);
        } else if ("gui_layer".equals(service)) {
            lastGuiLayerState = Map.copyOf(serviceState);
        } else if ("hud_layer".equals(service)) {
            lastHudLayerState = Map.copyOf(serviceState);
        } else if ("tick".equals(service)) {
            lastTickState = Map.copyOf(serviceState);
        }
    }

    private static void putInputTargetEvidence(Map<String, Object> state, Map<String, Object> event) {
        Object inputDispatchObject = event.get("inputDispatch");
        if (!(inputDispatchObject instanceof Map<?, ?> inputDispatch) || inputDispatch.isEmpty()) {
            return;
        }
        state.put("inputDispatch", Map.copyOf(inputDispatch));
        Object targetsObject = inputDispatch.get("targets");
        if (!(targetsObject instanceof List<?> targets)) {
            return;
        }
        state.put("inputTargets", List.copyOf(targets));
        for (Object targetObject : targets) {
            if (!(targetObject instanceof Map<?, ?> target)
                    || !Boolean.TRUE.equals(target.get("handled"))) {
                continue;
            }
            state.put("inputTargetSurfaceType", valueOrDefault(target, "surfaceType", ""));
            state.put("inputTargetActionId", valueOrDefault(target, "actionId", ""));
            state.put("inputTargetStatus", valueOrDefault(target, "status", ""));
            state.put("inputRouteModuleId", valueOrDefault(target, "routeModuleId", ""));
            state.put("inputRouteSurfaceId", valueOrDefault(target, "routeSurfaceId", ""));
            state.put("inputRouteTrustedMutation", valueOrDefault(target, "routeTrustedMutation", false));
            state.put("inputRouteStatus", valueOrDefault(target, "routeStatus", ""));
            Object route = target.get("route");
            if (route instanceof Map<?, ?> routeMap) {
                state.put("inputRoute", Map.copyOf(routeMap));
            }
            break;
        }
    }

    private static Object valueOrDefault(Map<?, ?> map, String key, Object fallback) {
        return map.containsKey(key) ? map.get(key) : fallback;
    }

    private static Map<String, Map<String, Object>> copyMapValues(Map<String, Map<String, Object>> values) {
        Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(key, Map.copyOf(value)));
        return Map.copyOf(copy);
    }

    private static void putRouteOwnerEvidence(
            Map<String, Object> state,
            String surfaceType,
            String actionId
    ) {
        Map<String, Object> route = NativeLoaderClientRouteTable.routeForAction(surfaceType, actionId);
        if (route.isEmpty()) {
            return;
        }
        state.put("route", route);
        state.put("routeModuleId", route.getOrDefault("moduleId", ""));
        state.put("routeSurfaceId", route.getOrDefault("surfaceId", ""));
        state.put("routeTrustedMutation", route.getOrDefault("trustedMutation", false));
        state.put("routeStatus", route.getOrDefault("status", ""));
        Object evidence = route.get("evidence");
        if (evidence instanceof Map<?, ?> evidenceMap) {
            state.put("nativeClientRouteProcess",
                    Boolean.TRUE.equals(evidenceMap.get("nativeClientRouteProcess")));
            state.put("neoForgeEventOwnershipRequired",
                    Boolean.TRUE.equals(evidenceMap.get("neoForgeEventOwnershipRequired")));
        }
    }

    private static Map<String, Object> hostServiceMetadata(Map<String, Object> metadata, String service) {
        Map<String, Object> next = new LinkedHashMap<>();
        if (metadata != null) {
            next.putAll(metadata);
        }
        next.putIfAbsent("source", "native_loader_client_ui_host");
        next.putIfAbsent("service", service == null ? "" : service);
        return Map.copyOf(next);
    }

    private static Map<String, Object> inputServiceMetadata(
            Map<String, Object> metadata,
            String service,
            String keyMapping,
            int keyCode,
            String inputType
    ) {
        Map<String, Object> next = new LinkedHashMap<>(hostServiceMetadata(metadata, service));
        next.put("keyMapping", keyMapping == null ? "" : keyMapping);
        next.put("keyCode", keyCode);
        next.put("inputType", inputType == null ? "" : inputType);
        return Map.copyOf(next);
    }

    private static Map<String, Object> focusServiceMetadata(Map<String, Object> metadata, boolean focused) {
        Map<String, Object> next = new LinkedHashMap<>(hostServiceMetadata(metadata, "overlay_focus"));
        next.put("focused", focused);
        return Map.copyOf(next);
    }

    private static Map<String, Object> mouseServiceMetadata(Map<String, Object> metadata) {
        Map<String, Object> next = new LinkedHashMap<>(hostServiceMetadata(metadata, "mouse"));
        if (metadata != null) {
            next.putIfAbsent("metadata", Map.copyOf(metadata));
        }
        return Map.copyOf(next);
    }

    private static EchoNativeLoadStatus merge(EchoNativeLoadStatus left, EchoNativeLoadStatus right) {
        List<EchoNativeLoadStatus> statuses = List.of(
                left == null ? EchoNativeLoadStatus.UNSUPPORTED : left,
                right == null ? EchoNativeLoadStatus.UNSUPPORTED : right
        );
        if (statuses.contains(EchoNativeLoadStatus.MUTATED)) {
            return EchoNativeLoadStatus.MUTATED;
        }
        if (statuses.contains(EchoNativeLoadStatus.REGISTERED)) {
            return EchoNativeLoadStatus.REGISTERED;
        }
        if (statuses.contains(EchoNativeLoadStatus.RESOLVED)) {
            return EchoNativeLoadStatus.RESOLVED;
        }
        if (statuses.contains(EchoNativeLoadStatus.FAILED)) {
            return EchoNativeLoadStatus.FAILED;
        }
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    private boolean nativeClientRouteTableMutationAccepted(EchoNativeLoadStatus status) {
        return liveClientBridge.firstClassNativeClientRouteTable()
                && liveClientBridge.nativeClientRouteProcess()
                && liveClientBridge.releaseClientRouteTrusted()
                && liveClientBridge.clientRouteMutationSupported()
                && status == EchoNativeLoadStatus.MUTATED;
    }

    private static String builtInProductActionForHostPhase(String surfaceType, String phase) {
        String safeSurfaceType = surfaceType == null ? "" : surfaceType.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');
        String safePhase = phase == null ? "" : phase.trim().toLowerCase(Locale.ROOT);
        return switch (safeSurfaceType + ":" + safePhase) {
            case "main_menu:mount", "main_menu:open" -> "menu.open";
            case "main_menu:close", "main_menu:unmount" -> "menu.quit";
            case "world_setup:mount", "world_setup:open" -> "world_setup.open";
            case "world_setup:create", "world_setup:submit" -> "world_setup.create";
            case "world_setup:close", "world_setup:unmount", "world_setup:back" -> "world_setup.back";
            case "loading_screen:mount", "loading_screen:open" -> "loading.open";
            case "loading_screen:render" -> "loading.render";
            case "loading_screen:progress" -> "loading.progress";
            case "loading_screen:close", "loading_screen:unmount", "loading_screen:complete" -> "loading.complete";
            default -> "";
        };
    }

    private static String resolvedHostActionId(String surfaceType, String phase, String actionId) {
        if (!blank(actionId)) {
            return actionId;
        }
        String productAction = builtInProductActionForHostPhase(surfaceType, phase);
        return blank(productAction) ? "native_loader." + phase : productAction;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void registerBuiltInRoute(
            String surfaceId,
            String surfaceType,
            Map<String, Map<String, Object>> actions,
            Map<String, Object> config
    ) {
        Map<String, Object> evidence = Map.of(
                "nativeClientRouteProcess", true,
                "clientRouteMutationSupported", true,
                "nativeClientRouteSdk", "echo-native-client-route-registry",
                "nativeLoaderBuiltInProductRoute", true,
                "neoForgeEventOwnershipRequired", false
        );
        NativeLoaderClientRouteTable.registerRoute(
                "echo-native-loader",
                surfaceId,
                surfaceType,
                config,
                evidence,
                true
        );
        NativeLoaderClientRouteTable.registerActions("echo-native-loader", surfaceId, surfaceType, actions);
        NativeLoaderClientRouteTable.registerActionHandler(
                surfaceType,
                "echo-native-loader:" + surfaceType + ":builtin_product_route",
                context -> {
            if (!actions.containsKey(context.actionId())) {
                return false;
            }
            Map<String, Object> state = mutateBuiltInProductSurface(context);
            NativeLoaderClientRouteTable.publishLifecycleEvent(
                    context.surfaceType(),
                    "native_loader_builtin_action",
                    context.actionId(),
                    Map.of(
                            "source", "native_loader_builtin_product_route",
                            "action", context.action(),
                            "state", state
                    )
            );
            return true;
        });
    }

    private static synchronized Map<String, Object> mutateBuiltInProductSurface(
            NativeLoaderClientRouteTable.NativeClientRouteActionContext context
    ) {
        String surfaceType = context.surfaceType();
        Map<String, Object> previous = BUILT_IN_PRODUCT_SURFACE_STATE.getOrDefault(surfaceType, Map.of());
        Map<String, Object> next = new LinkedHashMap<>(previous);
        String command = text(context.action().get("command"));
        Map<String, Object> safeMetadata = context.metadata() == null ? Map.of() : context.metadata();
        next.put("surfaceType", surfaceType);
        next.put("surfaceId", text(context.route().get("surfaceId")));
        next.put("moduleId", "echo-native-loader");
        next.put("lastActionId", context.actionId());
        next.put("lastCommand", command);
        next.put("lastMetadata", Map.copyOf(safeMetadata));
        next.put("lastSource", text(safeMetadata.getOrDefault("source", "native_loader_builtin_product_route")));
        putIfPresent(next, "lastService", safeMetadata.get("service"));
        putIfPresent(next, "lastEventType", safeMetadata.get("eventType"));
        putIfPresent(next, "lastFrameSource", safeMetadata.get("frameSource"));
        putIfPresent(next, "lastScreenSource", safeMetadata.get("screenSource"));
        putIfPresent(next, "lastScreenClass", safeMetadata.get("screenClass"));
        putIfPresent(next, "lastScreenWidth", safeMetadata.get("screenWidth"));
        putIfPresent(next, "lastScreenHeight", safeMetadata.get("screenHeight"));
        putIfPresent(next, "lastPartialTick", safeMetadata.get("partialTick"));
        next.put("mutationCount", intValue(previous.get("mutationCount")) + 1);
        switch (surfaceType) {
            case "main_menu" -> mutateBuiltInMenu(next, previous, command);
            case "world_setup" -> mutateBuiltInWorldSetup(next, previous, command, safeMetadata);
            case "loading_screen" -> mutateBuiltInLoading(next, previous, command, safeMetadata);
            default -> next.put("phase", command.isBlank() ? "action" : command);
        }
        next.put("routeDrivenRendererState", true);
        next.put("nativeProductUiReady", true);
        BUILT_IN_PRODUCT_SURFACE_STATE.put(surfaceType, Map.copyOf(next));
        return Map.copyOf(next);
    }

    private static void mutateBuiltInMenu(
            Map<String, Object> next,
            Map<String, Object> previous,
            String command
    ) {
        next.put("visible", !"quit".equals(command));
        next.put("phase", command.isBlank() ? "open" : command);
        if (!command.isBlank()) {
            next.put("selectedCommand", command);
        }
        if ("open".equals(command)) {
            next.put("openCount", intValue(previous.get("openCount")) + 1);
        }
        if ("new_run".equals(command) || "continue".equals(command)) {
            next.put("pendingAshfallWorldStartup", true);
        }
        if ("settings".equals(command)) {
            next.put("settingsRequested", true);
        }
        if ("quit".equals(command)) {
            next.put("quitRequested", true);
        }
        next.put("renderModel", menuRenderModel(next));
    }

    private static void mutateBuiltInLoading(
            Map<String, Object> next,
            Map<String, Object> previous,
            String command,
            Map<String, Object> metadata
    ) {
        next.put("visible", !"complete".equals(command));
        next.put("phase", command.isBlank() ? "render" : command);
        if ("open".equals(command)) {
            next.put("openCount", intValue(previous.get("openCount")) + 1);
            next.put("progress", 0.0D);
        }
        if ("render".equals(command)) {
            next.put("renderCount", intValue(previous.get("renderCount")) + 1);
        }
        if ("progress".equals(command)) {
            next.put("progress", doubleValue(metadata.get("progress"), doubleValue(previous.get("progress"), 0.0D)));
            next.put("progressLabel", text(metadata.get("label")));
        }
        if ("complete".equals(command)) {
            next.put("progress", 1.0D);
            next.put("completed", true);
            next.put("completeCount", intValue(previous.get("completeCount")) + 1);
        }
        next.put("renderModel", loadingRenderModel(next));
    }

    private static void mutateBuiltInWorldSetup(
            Map<String, Object> next,
            Map<String, Object> previous,
            String command,
            Map<String, Object> metadata
    ) {
        next.put("visible", !"back".equals(command));
        next.put("phase", command.isBlank() ? "open" : command);
        next.put("nativeLoaderOwnedWorldPolicy", true);
        next.put("forcedWorldPreset", NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID);
        next.put("vanillaWorldCreationFallbackAllowed", false);
        next.put("worldName", NativeLoaderAshfallWorldStartupService.configuredProductWorldName());
        next.put("worldFolder", NativeLoaderAshfallWorldStartupService.configuredProductWorldFolder());
        if ("open".equals(command)) {
            next.put("openCount", intValue(previous.get("openCount")) + 1);
        }
        if ("create".equals(command)) {
            next.put("createRequested", true);
            next.put("createCount", intValue(previous.get("createCount")) + 1);
            putIfPresent(next, "createSource", metadata.get("source"));
            copyWorldSetupEvidence(next, metadata);
        }
        if ("back".equals(command)) {
            next.put("backRequested", true);
        }
        next.put("renderModel", worldSetupRenderModel(next));
    }

    private static void copyWorldSetupEvidence(Map<String, Object> next, Map<String, Object> metadata) {
        for (String key : List.of(
                "worldSetupPrepared",
                "worldSetupBlocked",
                "worldSetupStartupAction",
                "worldSetupFailureKind",
                "worldSetupFailureMessage",
                "worldSetupSummary",
                "worldSetupGameDir",
                "worldSetupSaveDir",
                "worldSetupStagedDatapack",
                "worldSetupProductWorldMarker",
                "nativeProductWorldOpenDispatchRecorded",
                "nativeProductWorldOpenDispatchMarker",
                "worldSetupPlan",
                "worldSetupLiveProductWorldEvidence"
        )) {
            putIfPresent(next, key, metadata.get(key));
        }
    }

    private static Map<String, Object> menuRenderModel(Map<String, Object> state) {
        Map<String, Object> model = new LinkedHashMap<>();
        String selected = text(state.get("selectedCommand"));
        NativeLoaderTheme theme = NativeLoaderThemeResolver.activeTheme();
        model.put("surface", "main_menu");
        model.put("product", "ECHO Native Loader");
        model.put("displayProduct", theme.token("identityLabel"));
        model.put("routeDriven", true);
        model.put("visible", Boolean.TRUE.equals(state.get("visible")));
        model.put("phase", text(state.get("phase")));
        model.put("selectedCommand", selected);
        model.put("pendingAshfallWorldStartup", Boolean.TRUE.equals(state.get("pendingAshfallWorldStartup")));
        model.putAll(theme.evidence());
        model.put("commands", List.of(
                menuCommand("new_run", "New Run", selected),
                menuCommand("continue", "Continue", selected),
                menuCommand("settings", "Settings", selected),
                menuCommand("quit", "Quit", selected)
        ));
        return Map.copyOf(model);
    }

    private static Map<String, Object> menuCommand(String id, String label, String selected) {
        return Map.of(
                "id", id,
                "label", label,
                "selected", id.equals(selected)
        );
    }

    private static Map<String, Object> loadingRenderModel(Map<String, Object> state) {
        Map<String, Object> model = new LinkedHashMap<>();
        NativeLoaderTheme theme = NativeLoaderThemeResolver.activeTheme();
        model.put("surface", "loading_screen");
        model.put("product", "ECHO Native Loader");
        model.put("displayProduct", theme.token("identityLabel"));
        model.put("routeDriven", true);
        model.put("visible", Boolean.TRUE.equals(state.get("visible")));
        model.put("phase", text(state.get("phase")));
        model.put("progress", doubleValue(state.get("progress"), 0.0D));
        model.put("label", text(state.get("progressLabel")));
        model.put("completed", Boolean.TRUE.equals(state.get("completed")));
        model.put("renderCount", intValue(state.get("renderCount")));
        model.putAll(theme.evidence());
        return Map.copyOf(model);
    }

    private static Map<String, Object> worldSetupRenderModel(Map<String, Object> state) {
        Map<String, Object> model = new LinkedHashMap<>();
        NativeLoaderTheme theme = NativeLoaderThemeResolver.activeTheme();
        model.put("surface", "world_setup");
        model.put("product", "ECHO Native Loader");
        model.put("displayProduct", theme.token("identityLabel"));
        model.put("routeDriven", true);
        model.put("visible", Boolean.TRUE.equals(state.get("visible")));
        model.put("phase", text(state.get("phase")));
        model.put("worldName", text(state.get("worldName")));
        model.put("worldFolder", text(state.get("worldFolder")));
        model.put("forcedWorldPreset", text(state.get("forcedWorldPreset")));
        model.put("nativeLoaderOwnedWorldPolicy", Boolean.TRUE.equals(state.get("nativeLoaderOwnedWorldPolicy")));
        model.put("vanillaWorldCreationFallbackAllowed",
                Boolean.TRUE.equals(state.get("vanillaWorldCreationFallbackAllowed")));
        model.put("worldSetupPrepared", Boolean.TRUE.equals(state.get("worldSetupPrepared")));
        model.put("worldSetupBlocked", Boolean.TRUE.equals(state.get("worldSetupBlocked")));
        model.put("worldSetupStartupAction", text(state.get("worldSetupStartupAction")));
        model.put("worldSetupFailureKind", text(state.get("worldSetupFailureKind")));
        model.put("worldSetupSummary", text(state.get("worldSetupSummary")));
        model.put("worldSetupGameDir", text(state.get("worldSetupGameDir")));
        model.put("worldSetupSaveDir", text(state.get("worldSetupSaveDir")));
        model.put("worldSetupStagedDatapack", text(state.get("worldSetupStagedDatapack")));
        model.put("worldSetupProductWorldMarker", text(state.get("worldSetupProductWorldMarker")));
        model.put("nativeProductWorldOpenDispatchRecorded",
                Boolean.TRUE.equals(state.get("nativeProductWorldOpenDispatchRecorded")));
        model.put("nativeProductWorldOpenDispatchMarker", text(state.get("nativeProductWorldOpenDispatchMarker")));
        putIfPresent(model, "worldSetupPlan", state.get("worldSetupPlan"));
        putIfPresent(model, "worldSetupLiveProductWorldEvidence", state.get("worldSetupLiveProductWorldEvidence"));
        model.putAll(theme.evidence());
        return Map.copyOf(model);
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
