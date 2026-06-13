package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeActivationSurfaceRegistrar;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeAgent2ClientRouteOwnershipSmokeMain {
    private static final List<String> REQUIRED_SURFACES = List.of(
            "terminal",
            "index",
            "lens",
            "holomap",
            "hud",
            "main_menu",
            "world_setup",
            "loading_screen"
    );

    private EchoNativeAgent2ClientRouteOwnershipSmokeMain() {
    }

    private static Path addonSourcePath(String moduleId, String... parts) {
        Path path = echoModulesRoot().resolve(moduleId);
        for (String part : parts) {
            path = path.resolve(part);
        }
        return path.normalize();
    }

    private static Path echoModulesRoot() {
        String configured = System.getProperty("echo.modules.root");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("ECHO_MODULES_ROOT");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        Path workspaceRoot = Path.of("").toAbsolutePath().normalize().getParent();
        Path workspaceModules = workspaceRoot == null
                ? Path.of("..", "ECHO-Modules", "addons")
                : workspaceRoot.resolve("ECHO-Modules").resolve("addons");
        if (Files.isDirectory(workspaceModules)) {
            return workspaceModules;
        }
        Path legacyAddons = workspaceRoot == null
                ? Path.of("..", "addons")
                : workspaceRoot.resolve("addons");
        return legacyAddons.toAbsolutePath().normalize();
    }

    public static void main(String[] args) throws Exception {
        Path output = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("build/native-agent2-client-routes/native-client-route-ownership.json")
                .toAbsolutePath()
                .normalize();

        NativeLoaderClientRouteTable.clear();
        System.setProperty(EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY, "true");
        EchoNativeClientRouteRegistries.resetDiscoveryForRuntimeReload();

        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        require(registry != EchoNativeClientRouteRegistry.NOOP,
                "Native Loader must provide EchoNativeClientRouteRegistries while native client mode is active.");

        NativeLoaderClientUiHost host = new NativeLoaderClientUiHost();
        new NativeLoaderClientUiHost();
        Map<String, Integer> dispatchCounts = new LinkedHashMap<>();
        Map<String, Map<String, Object>> dispatchMetadata = new LinkedHashMap<>();
        registerSurface(registry, dispatchCounts, "echoterminal", "echoterminal:eui", "terminal",
                "terminal.open", "key.echoterminal.open", 77);
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:eui", "terminal",
                "terminal.screen.char_typed", "terminal_screen_input", "echoterminal");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:eui", "terminal",
                "terminal.screen.mouse_scroll", "terminal_screen_input", "echoterminal");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:eui", "terminal",
                "terminal.screen.frame.render", "terminal_screen_frame_render", "echoterminal");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:eui", "terminal",
                "terminal.screencore.mouse", "terminal_screencore_mouse_input", "echoterminal");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:eui", "terminal",
                "terminal.screencore.scroll", "terminal_screencore_scroll_input", "echoterminal");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:eui", "terminal",
                "terminal.screencore.key", "terminal_screencore_key_input", "echoterminal");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:eui", "terminal",
                "terminal.screencore.char", "terminal_screencore_char_input", "echoterminal");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:eui", "terminal",
                "terminal.screencore.action", "terminal_screencore_action", "echoterminal");
        registerSurface(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.catalog", "key.echoindex.catalog", 71);
        registerAdditionalInputAction(registry, dispatchCounts, "echoindex", "echoindex:index", "index",
                "index.recipe", "item_recipe", "key.echoindex.recipe", 82);
        registerAdditionalInputAction(registry, dispatchCounts, "echoindex", "echoindex:index", "index",
                "index.usage", "item_recipe", "key.echoindex.usage", 85);
        registerAdditionalInputAction(registry, dispatchCounts, "echoindex", "echoindex:index", "index",
                "index.bookmark", "screen_core_mode", "key.echoindex.bookmark", 66);
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.hotkey_screen_render", "hotkey_screen_render", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.hotkey_key_pressed", "hotkey_key_pressed", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.client.login", "client_lifecycle", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.client.logout", "client_lifecycle", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.client.resources_reloaded", "client_lifecycle", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.catalog_screen.mouse", "catalog_screen_mouse_input", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.catalog_screen.scroll", "catalog_screen_scroll_input", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.catalog_screen.key", "catalog_screen_key_input", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.catalog_screen.char", "catalog_screen_char_input", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.recipe_screen.mouse", "recipe_screen_mouse_input", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.recipe_screen.scroll", "recipe_screen_scroll_input", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.recipe_screen.key", "recipe_screen_key_input", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.recipe_screen.char", "recipe_screen_char_input", "echoindex");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:index", "index",
                "index.screencore.action", "index_screencore_action", "echoindex");
        registerSurface(registry, dispatchCounts, "echolens", "echolens:field_lens", "lens",
                "lens.deep_scan", "echolens.key.deep_scan", 342);
        registerAdditionalInputAction(registry, dispatchCounts, "echolens", "echolens:field_lens", "lens",
                "lens.index_recipe", "target_index", "key.echolens.index_recipe", 82);
        registerAdditionalInputAction(registry, dispatchCounts, "echolens", "echolens:field_lens", "lens",
                "lens.index_usage", "target_index", "key.echolens.index_usage", 85);
        registerAdditionalInputAction(registry, dispatchCounts, "echolens", "echolens:field_lens", "lens",
                "lens.track_in_index", "target_index", "key.echolens.track_in_index", 84);
        registerSurface(registry, dispatchCounts, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.open", "key.echoholomap.open_map", 74);
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.fullscreen.key", "fullscreen_key_input", "echoholomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.fullscreen.mouse", "fullscreen_mouse_input", "echoholomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.fullscreen.scroll", "fullscreen_scroll_input", "echoholomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.sync", "fullscreen_command", "echoholomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.center", "fullscreen_command", "echoholomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.toggle_markers", "fullscreen_command", "echoholomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.cycle_fields", "fullscreen_command", "echoholomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.toggle_waypoints", "fullscreen_command", "echoholomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.select_entry", "fullscreen_command", "echoholomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:fullscreen_map", "holomap",
                "holomap.close", "fullscreen_command", "echoholomap");
        registerAdditionalRoute(registry, "echoholomap", "echoholomap:minimap", "holomap");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoholomap", "echoholomap:minimap", "holomap",
                "holomap.minimap.render", "client_overlay_render", "echoholomap");
        registerAdditionalInputAction(registry, dispatchCounts, "echoholomap", "echoholomap:minimap", "holomap",
                "holomap.toggle_minimap", "overlay_command", "key.echoholomap.toggle_minimap", 75);
        registerAdditionalInputAction(registry, dispatchCounts, "echoholomap", "echoholomap:minimap", "holomap",
                "holomap.zoom_in", "overlay_command", "key.echoholomap.minimap_zoom_in", 93);
        registerAdditionalInputAction(registry, dispatchCounts, "echoholomap", "echoholomap:minimap", "holomap",
                "holomap.zoom_out", "overlay_command", "key.echoholomap.minimap_zoom_out", 91);
        registerAdditionalInputAction(registry, dispatchCounts, "echoholomap", "echoholomap:minimap", "holomap",
                "holomap.cycle_corner", "overlay_command", "key.echoholomap.minimap_cycle_corner", 92);
        registerSurface(registry, dispatchCounts, dispatchMetadata, "echohudcore", "echohudcore:native_hud", "hud",
                "hud.render", "key.echohudcore.render_probe", -1);
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echohudcore", "echohudcore:native_hud", "hud",
                "hud.update_snapshot", "hud_state_update", "echohudcore");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echohudcore", "echohudcore:native_hud", "hud",
                "native_loader.overlay_focus", "hud_overlay_focus", "echohudcore");
        registerAdditionalRoute(registry, "echohudcore", "echohudcore:mission_tracker", "hud_widget");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echohudcore", "echohudcore:mission_tracker", "hud_widget",
                "hud.mission_tracker.render", "hud_widget_render", "echohudcore");
        registerAdditionalRoute(registry, "echohudcore", "echohudcore:hazard_readout", "hud_widget");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echohudcore", "echohudcore:hazard_readout", "hud_widget",
                "hud.hazard_readout.render", "hud_widget_render", "echohudcore");
        registerAdditionalRoute(registry, "echohudcore", "echohudcore:compass_indicator", "hud_widget");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echohudcore", "echohudcore:compass_indicator", "hud_widget",
                "hud.compass_indicator.render", "hud_widget_render", "echohudcore");
        registerAdditionalRoute(registry, "echohudcore", "echohudcore:screen_safe_area", "hud_layout");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echohudcore", "echohudcore:screen_safe_area", "hud_layout",
                "hud.screen_safe_area.resolve", "hud_layout_resolve", "echohudcore");
        registerOverlaySurface(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:hud_overlay",
                "terminal.mission_hud.tick", "terminal.mission_hud.render");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:hud_overlay", "client_overlay",
                "terminal.discovery_toast.tick", "client_overlay_tick", "echoterminal");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoterminal", "echoterminal:hud_overlay", "client_overlay",
                "terminal.discovery_toast.render", "client_overlay_render", "echoterminal");
        registerOverlaySurface(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:inventory_overlay",
                "index.inventory_overlay_input", "index.inventory_overlay_render");
        registerAdditionalAction(registry, dispatchCounts, dispatchMetadata, "echoindex", "echoindex:inventory_overlay",
                "client_overlay", "index.track_item", "item_recipe", "echoindex");
        registerOverlaySurface(registry, dispatchCounts, dispatchMetadata, "echolens", "echolens:lens_overlay",
                "lens.overlay.scan_target", "lens.overlay.render");

        host.attach(Map.of(
                "firstClassNativeClientRouteTable", true,
                "nativeClientRouteProcess", true,
                "releaseClientRouteTrusted", true,
                "clientRouteMutationSupported", true,
                "neoForgeEventOwnershipRequired", false
        ));
        NativeLoaderLiveClientBridge clientBridge = new NativeLoaderDefaultProductBridgeProvider()
                .liveClientBridge(new NativeLoaderProductBridgeContext(
                        "ashfall",
                        "agent2_client_route_ownership",
                        output.getParent(),
                        output.getParent().resolve("agent2"),
                        Map.of("source", "agent2_client_route_ownership_smoke")));
        host.attachLiveBridge(clientBridge);
        NativeLoaderClientWindowPump windowPump = new NativeLoaderClientWindowPump(host);

        Map<String, Boolean> directPublicSdkDispatchResults = new LinkedHashMap<>();
        directPublicSdkDispatchResults.put("terminal.open", registry.dispatchStatus(
                "terminal",
                "terminal.open",
                directPublicSdkMetadata("terminal")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("index.catalog", registry.dispatchStatus(
                "index",
                "index.catalog",
                directPublicSdkMetadata("index")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("lens.deep_scan", registry.dispatchStatus(
                "lens",
                "lens.deep_scan",
                directPublicSdkMetadata("lens")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("holomap.open", registry.dispatchStatus(
                "holomap",
                "holomap.open",
                directPublicSdkMetadata("holomap")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("hud.render", registry.dispatchStatus(
                "hud",
                "hud.render",
                directPublicSdkMetadata("hud")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("hud.update_snapshot", registry.dispatchStatus(
                "hud",
                "hud.update_snapshot",
                directPublicSdkMetadata("hud", Map.of("hudSnapshotSource", "direct_public_sdk"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("hud.overlay_focus", registry.dispatchStatus(
                "hud",
                "native_loader.overlay_focus",
                directPublicSdkMetadata("hud", Map.of("focused", true))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("hud.mission_tracker.render", registry.dispatchStatus(
                "hud_widget",
                "hud.mission_tracker.render",
                directPublicSdkMetadata("hud_widget", Map.of("widget", "mission_tracker"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("hud.hazard_readout.render", registry.dispatchStatus(
                "hud_widget",
                "hud.hazard_readout.render",
                directPublicSdkMetadata("hud_widget", Map.of("widget", "hazard_readout"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("hud.compass_indicator.render", registry.dispatchStatus(
                "hud_widget",
                "hud.compass_indicator.render",
                directPublicSdkMetadata("hud_widget", Map.of("widget", "compass_indicator"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("hud.screen_safe_area.resolve", registry.dispatchStatus(
                "hud_layout",
                "hud.screen_safe_area.resolve",
                directPublicSdkMetadata("hud_layout", Map.of("layout", "screen_safe_area"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("menu.open", registry.dispatchStatus(
                "main_menu",
                "menu.open",
                directPublicSdkMetadata("main_menu")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("menu.new_run", registry.dispatchStatus(
                "main_menu",
                "menu.new_run",
                directPublicSdkMetadata("main_menu", Map.of("selection", "new_run"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("world_setup.open", registry.dispatchStatus(
                "world_setup",
                "world_setup.open",
                directPublicSdkMetadata("world_setup", Map.of("phase", "open"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("world_setup.create", registry.dispatchStatus(
                "world_setup",
                "world_setup.create",
                directPublicSdkMetadata("world_setup", Map.of("phase", "create"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("world_setup.back", registry.dispatchStatus(
                "world_setup",
                "world_setup.back",
                directPublicSdkMetadata("world_setup", Map.of("phase", "back"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("loading.open", registry.dispatchStatus(
                "loading_screen",
                "loading.open",
                directPublicSdkMetadata("loading_screen", Map.of("phase", "open"))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("loading.render", registry.dispatchStatus(
                "loading_screen",
                "loading.render",
                directPublicSdkMetadata("loading_screen")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("loading.progress", registry.dispatchStatus(
                "loading_screen",
                "loading.progress",
                directPublicSdkMetadata("loading_screen", Map.of(
                        "progress", 0.42D,
                        "label", "Direct SDK loading progress"
                ))) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkDispatchResults.put("loading.complete", registry.dispatchStatus(
                "loading_screen",
                "loading.complete",
                directPublicSdkMetadata("loading_screen", Map.of("phase", "complete"))) == EchoNativeLoadStatus.MUTATED);

        Map<String, Boolean> directPublicSdkInputDispatchResults = new LinkedHashMap<>();
        directPublicSdkInputDispatchResults.put("terminal.open", registry.dispatchInputBindingStatus(
                "key.echoterminal.open",
                77,
                "press",
                directPublicSdkInputMetadata("terminal", "terminal.open")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("index.catalog", registry.dispatchInputBindingStatus(
                "key.echoindex.catalog",
                71,
                "press",
                directPublicSdkInputMetadata("index", "index.catalog")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("index.recipe", registry.dispatchInputBindingStatus(
                "key.echoindex.recipe",
                82,
                "press",
                directPublicSdkInputMetadata("index", "index.recipe")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("index.usage", registry.dispatchInputBindingStatus(
                "key.echoindex.usage",
                85,
                "press",
                directPublicSdkInputMetadata("index", "index.usage")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("index.bookmark", registry.dispatchInputBindingStatus(
                "key.echoindex.bookmark",
                66,
                "press",
                directPublicSdkInputMetadata("index", "index.bookmark")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("lens.deep_scan", registry.dispatchInputBindingStatus(
                "echolens.key.deep_scan",
                342,
                "press",
                directPublicSdkInputMetadata("lens", "lens.deep_scan")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("lens.index_recipe", registry.dispatchInputBindingStatus(
                "key.echolens.index_recipe",
                82,
                "press",
                directPublicSdkInputMetadata("lens", "lens.index_recipe")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("lens.index_usage", registry.dispatchInputBindingStatus(
                "key.echolens.index_usage",
                85,
                "press",
                directPublicSdkInputMetadata("lens", "lens.index_usage")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("lens.track_in_index", registry.dispatchInputBindingStatus(
                "key.echolens.track_in_index",
                84,
                "press",
                directPublicSdkInputMetadata("lens", "lens.track_in_index")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("holomap.open", registry.dispatchInputBindingStatus(
                "key.echoholomap.open_map",
                74,
                "press",
                directPublicSdkInputMetadata("holomap", "holomap.open")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("holomap.toggle_minimap", registry.dispatchInputBindingStatus(
                "key.echoholomap.toggle_minimap",
                75,
                "press",
                directPublicSdkInputMetadata("holomap", "holomap.toggle_minimap")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("holomap.zoom_in", registry.dispatchInputBindingStatus(
                "key.echoholomap.minimap_zoom_in",
                93,
                "press",
                directPublicSdkInputMetadata("holomap", "holomap.zoom_in")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("holomap.zoom_out", registry.dispatchInputBindingStatus(
                "key.echoholomap.minimap_zoom_out",
                91,
                "press",
                directPublicSdkInputMetadata("holomap", "holomap.zoom_out")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("holomap.cycle_corner", registry.dispatchInputBindingStatus(
                "key.echoholomap.minimap_cycle_corner",
                92,
                "press",
                directPublicSdkInputMetadata("holomap", "holomap.cycle_corner")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("menu.open", registry.dispatchInputBindingStatus(
                "key.echo.native.menu",
                256,
                "press",
                directPublicSdkInputMetadata("main_menu", "menu.open")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("menu.new_run", registry.dispatchInputBindingStatus(
                "key.echo.native.menu.new_run",
                257,
                "press",
                directPublicSdkInputMetadata("main_menu", "menu.new_run")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkInputDispatchResults.put("menu.quit", registry.dispatchInputBindingStatus(
                "key.echo.native.menu.quit",
                256,
                "press",
                directPublicSdkInputMetadata("main_menu", "menu.quit")) == EchoNativeLoadStatus.MUTATED);
        EchoNativeLoadStatus hostMenuInputStatus = windowPump.keyInput(
                "key.echo.native.menu",
                256,
                "press",
                Map.of(
                        "screenClass", "dev.echo.nativeplatform.loader.AshfallMainMenuScreen",
                        "focusedSurface", "main_menu"
                ));
        EchoNativeLoadStatus hostMenuNewRunInputStatus = windowPump.keyInput(
                "key.echo.native.menu.new_run",
                257,
                "press",
                Map.of(
                        "screenClass", "dev.echo.nativeplatform.loader.AshfallMainMenuScreen",
                        "focusedSurface", "main_menu",
                        "command", "new_run"
                ));
        EchoNativeLoadStatus hostMenuQuitInputStatus = windowPump.keyInput(
                "key.echo.native.menu.quit",
                256,
                "press",
                Map.of(
                        "screenClass", "dev.echo.nativeplatform.loader.AshfallMainMenuScreen",
                        "focusedSurface", "main_menu",
                        "command", "quit"
                ));

        Map<String, Boolean> dispatchResults = new LinkedHashMap<>();
        dispatchResults.put("terminal.open", windowPump.openScreen(
                "terminal",
                "terminal.open",
                "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen",
                320,
                180,
                "terminal",
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("terminal.screen_lifecycle", host.screenLifecycleEvent(
                "terminal",
                "open",
                "terminal.open",
                Map.of(
                        "eventType", "host_screen_lifecycle_probe"
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("terminal.screen.char_typed", host.dispatchRoute(
                "terminal",
                "terminal.screen.char_typed",
                Map.of(
                        "source", "agent2_smoke",
                        "eventType", "character_typed",
                        "characterEvent", "CharacterEvent[codePoint=65]"
                )));
        dispatchResults.put("terminal.screen.mouse_scroll", windowPump.mouseInput(
                "terminal",
                "terminal.screen.mouse_scroll",
                140,
                92,
                -1,
                "scroll",
                Map.of(
                        "scrollY", -1.0D
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("terminal.screen.frame.render", host.dispatchRoute(
                "terminal",
                "terminal.screen.frame.render",
                Map.of(
                        "source", "native_loader_gui_layer",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "screen_render_post",
                        "screenClass", "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen",
                        "screenWidth", 320,
                        "screenHeight", 180,
                        "partialTick", 0.5F
                )));
        dispatchResults.put("terminal.screencore.mouse", host.dispatchRoute(
                "terminal",
                "terminal.screencore.mouse",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "terminal_screencore_mouse_input",
                        "screenClass", "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen",
                        "phase", "drag",
                        "mouseX", 92.0D,
                        "mouseY", 64.0D,
                        "button", 0,
                        "dragX", 4.0D,
                        "dragY", -2.0D
                )));
        dispatchResults.put("terminal.screencore.scroll", host.dispatchRoute(
                "terminal",
                "terminal.screencore.scroll",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "terminal_screencore_scroll_input",
                        "screenClass", "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen",
                        "mouseX", 92.0D,
                        "mouseY", 64.0D,
                        "scrollX", 0.0D,
                        "scrollY", -1.0D
                )));
        dispatchResults.put("terminal.screencore.key", host.dispatchRoute(
                "terminal",
                "terminal.screencore.key",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "terminal_screencore_key_input",
                        "screenClass", "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen",
                        "key", 77,
                        "openTerminalKey", true
                )));
        dispatchResults.put("terminal.screencore.char", host.dispatchRoute(
                "terminal",
                "terminal.screencore.char",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "terminal_screencore_character_typed",
                        "screenClass", "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen",
                        "character", "m",
                        "allowedChatCharacter", true
                )));
        dispatchResults.put("terminal.screencore.action", host.dispatchRoute(
                "terminal",
                "terminal.screencore.action",
                Map.ofEntries(
                        Map.entry("source", "native_screencore_action"),
                        Map.entry("eventType", "terminal_screencore_action"),
                        Map.entry("screenClass", "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreActions"),
                        Map.entry("actionCatalog", "TerminalScreenCoreActionIds"),
                        Map.entry("screenCoreActionId", "terminal.open_mission"),
                        Map.entry("pageId", "echoterminal:mission_graph"),
                        Map.entry("componentId", "mission_detail"),
                        Map.entry("action", "open_mission"),
                        Map.entry("argument", "echoterminal:survive_first_night"),
                        Map.entry("actionValue", "echoterminal:survive_first_night"),
                        Map.entry("inputEvent", "button_pressed")
                )));
        dispatchResults.put("index.catalog", windowPump.dispatchRoute(
                "index",
                "index.catalog",
                Map.of("eventType", "window_pump_route_dispatch")) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("index.hotkey_screen_render", host.dispatchRoute(
                "index",
                "index.hotkey_screen_render",
                Map.of(
                        "source", "native_loader_gui_layer",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "screen_render_post",
                        "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                        "mouseX", 112,
                        "mouseY", 48,
                        "partialTick", 0.5F
                )));
        dispatchResults.put("index.hotkey_key_pressed", host.dispatchRoute(
                "index",
                "index.hotkey_key_pressed",
                Map.of(
                        "source", "native_loader_input_binding",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "hotkey_key_pressed",
                        "key", 82,
                        "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen"
                )));
        dispatchResults.put("index.client.login", host.dispatchRoute(
                "index",
                "index.client.login",
                Map.of(
                        "source", "native_loader_client_lifecycle",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "client_login",
                        "reason", "client login"
                )));
        dispatchResults.put("index.client.logout", host.dispatchRoute(
                "index",
                "index.client.logout",
                Map.of(
                        "source", "native_loader_client_lifecycle",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "client_logout",
                        "reason", "client logout"
                )));
        dispatchResults.put("index.client.resources_reloaded", host.dispatchRoute(
                "index",
                "index.client.resources_reloaded",
                Map.of(
                        "source", "native_loader_client_lifecycle",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "client_resources_reloaded",
                        "reason", "client resources reloaded",
                        "invalidateScreenCoreIndex", true
                )));
        dispatchResults.put("index.catalog_screen.mouse", host.dispatchRoute(
                "index",
                "index.catalog_screen.mouse",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "catalog_screen_mouse_input",
                        "screenClass", "com.knoxhack.echoindex.client.IndexCatalogScreen",
                        "phase", "click",
                        "mouseX", 124.0D,
                        "mouseY", 68.0D,
                        "button", 0,
                        "modifiers", 0
                )));
        dispatchResults.put("index.catalog_screen.scroll", host.dispatchRoute(
                "index",
                "index.catalog_screen.scroll",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "catalog_screen_scroll_input",
                        "screenClass", "com.knoxhack.echoindex.client.IndexCatalogScreen",
                        "mouseX", 124.0D,
                        "mouseY", 112.0D,
                        "scrollX", 0.0D,
                        "scrollY", -1.0D
                )));
        dispatchResults.put("index.catalog_screen.key", host.dispatchRoute(
                "index",
                "index.catalog_screen.key",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "catalog_screen_key_input",
                        "screenClass", "com.knoxhack.echoindex.client.IndexCatalogScreen",
                        "key", 85,
                        "recipeKey", false,
                        "usageKey", true
                )));
        dispatchResults.put("index.catalog_screen.char", host.dispatchRoute(
                "index",
                "index.catalog_screen.char",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "catalog_screen_character_typed",
                        "screenClass", "com.knoxhack.echoindex.client.IndexCatalogScreen",
                        "character", "n",
                        "allowedChatCharacter", true
                )));
        dispatchResults.put("index.screencore.action", host.dispatchRoute(
                "index",
                "index.screencore.action",
                Map.ofEntries(
                        Map.entry("source", "native_screencore_action"),
                        Map.entry("eventType", "index_screencore_action"),
                        Map.entry("screenClass", "com.knoxhack.echoindex.client.IndexActions"),
                        Map.entry("actionCatalog", "IndexActions"),
                        Map.entry("screenCoreActionId", "index.toggle_favorite"),
                        Map.entry("pageId", "echoindex:item_detail"),
                        Map.entry("componentId", "favorite_toggle"),
                        Map.entry("action", "toggle_favorite"),
                        Map.entry("argument", "minecraft:iron_ingot"),
                        Map.entry("actionValue", "minecraft:iron_ingot"),
                        Map.entry("inputEvent", "button_pressed")
                )));
        dispatchResults.put("index.recipe_screen.mouse", host.dispatchRoute(
                "index",
                "index.recipe_screen.mouse",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "recipe_screen_mouse_input",
                        "screenClass", "com.knoxhack.echoindex.client.IndexRecipeScreen",
                        "phase", "click",
                        "mouseX", 156.0D,
                        "mouseY", 74.0D,
                        "button", 0,
                        "modifiers", 0
                )));
        dispatchResults.put("index.recipe_screen.scroll", host.dispatchRoute(
                "index",
                "index.recipe_screen.scroll",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "recipe_screen_scroll_input",
                        "screenClass", "com.knoxhack.echoindex.client.IndexRecipeScreen",
                        "mouseX", 156.0D,
                        "mouseY", 104.0D,
                        "scrollX", 0.0D,
                        "scrollY", -1.0D
                )));
        dispatchResults.put("index.recipe_screen.key", host.dispatchRoute(
                "index",
                "index.recipe_screen.key",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "recipe_screen_key_input",
                        "screenClass", "com.knoxhack.echoindex.client.IndexRecipeScreen",
                        "key", 82,
                        "recipeKey", true,
                        "usageKey", false
                )));
        dispatchResults.put("index.recipe_screen.char", host.dispatchRoute(
                "index",
                "index.recipe_screen.char",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "recipe_screen_character_typed",
                        "screenClass", "com.knoxhack.echoindex.client.IndexRecipeScreen",
                        "character", "a",
                        "allowedChatCharacter", true
                )));
        dispatchResults.put("lens.deep_scan", host.dispatchRoute("lens", "lens.deep_scan"));
        dispatchResults.put("holomap.open", windowPump.openScreen(
                "holomap",
                "holomap.open",
                "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                320,
                180,
                "holomap",
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("holomap.fullscreen.key", host.dispatchRoute(
                "holomap",
                "holomap.fullscreen.key",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "fullscreen_key_pressed",
                        "screenClass", "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                        "key", 67
                )));
        dispatchResults.put("holomap.fullscreen.mouse", host.dispatchRoute(
                "holomap",
                "holomap.fullscreen.mouse",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "fullscreen_mouse_input",
                        "screenClass", "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                        "phase", "drag",
                        "mouseX", 148.0D,
                        "mouseY", 92.0D,
                        "button", 0,
                        "modifiers", 0,
                        "doubleClick", false
                )));
        dispatchResults.put("holomap.fullscreen.scroll", host.dispatchRoute(
                "holomap",
                "holomap.fullscreen.scroll",
                Map.of(
                        "source", "native_screen_lifecycle",
                        "eventType", "fullscreen_scroll_input",
                        "screenClass", "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                        "mouseX", 148.0D,
                        "mouseY", 92.0D,
                        "scrollX", 0.0D,
                        "scrollY", 1.0D
                )));
        dispatchResults.put("holomap.screencore.key", host.dispatchRoute(
                "holomap",
                "holomap.fullscreen.key",
                Map.of(
                        "source", "native_screencore_lifecycle",
                        "eventType", "fullscreen_screencore_key_pressed",
                        "screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        "key", 73,
                        "canvasWidth", 320,
                        "canvasHeight", 180
                )));
        dispatchResults.put("holomap.screencore.mouse", host.dispatchRoute(
                "holomap",
                "holomap.fullscreen.mouse",
                Map.ofEntries(
                        Map.entry("source", "native_screencore_lifecycle"),
                        Map.entry("eventType", "fullscreen_screencore_mouse_input"),
                        Map.entry("screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration"),
                        Map.entry("phase", "drag"),
                        Map.entry("mouseX", 148.0D),
                        Map.entry("mouseY", 92.0D),
                        Map.entry("button", 0),
                        Map.entry("modifiers", 0),
                        Map.entry("doubleClick", false),
                        Map.entry("dragX", 3.0D),
                        Map.entry("dragY", -2.0D),
                        Map.entry("canvasX", 8),
                        Map.entry("canvasY", 24),
                        Map.entry("canvasWidth", 320),
                        Map.entry("canvasHeight", 180)
                )));
        dispatchResults.put("holomap.screencore.scroll", host.dispatchRoute(
                "holomap",
                "holomap.fullscreen.scroll",
                Map.ofEntries(
                        Map.entry("source", "native_screencore_lifecycle"),
                        Map.entry("eventType", "fullscreen_screencore_scroll_input"),
                        Map.entry("screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration"),
                        Map.entry("mouseX", 148.0D),
                        Map.entry("mouseY", 92.0D),
                        Map.entry("scrollX", 0.0D),
                        Map.entry("scrollY", -1.0D),
                        Map.entry("canvasX", 8),
                        Map.entry("canvasY", 24),
                        Map.entry("canvasWidth", 320),
                        Map.entry("canvasHeight", 180)
                )));
        dispatchResults.put("holomap.screencore.sync", host.dispatchRoute(
                "holomap",
                "holomap.sync",
                Map.of(
                        "source", "native_screencore_lifecycle",
                        "eventType", "fullscreen_screencore_command",
                        "screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        "command", "sync"
                )));
        dispatchResults.put("holomap.screencore.center", host.dispatchRoute(
                "holomap",
                "holomap.center",
                Map.of(
                        "source", "native_screencore_lifecycle",
                        "eventType", "fullscreen_screencore_command",
                        "screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        "command", "center"
                )));
        dispatchResults.put("holomap.screencore.toggle_markers", host.dispatchRoute(
                "holomap",
                "holomap.toggle_markers",
                Map.of(
                        "source", "native_screencore_lifecycle",
                        "eventType", "fullscreen_screencore_command",
                        "screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        "command", "toggleMarkers"
                )));
        dispatchResults.put("holomap.screencore.cycle_fields", host.dispatchRoute(
                "holomap",
                "holomap.cycle_fields",
                Map.of(
                        "source", "native_screencore_lifecycle",
                        "eventType", "fullscreen_screencore_command",
                        "screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        "command", "cycleFields"
                )));
        dispatchResults.put("holomap.screencore.toggle_waypoints", host.dispatchRoute(
                "holomap",
                "holomap.toggle_waypoints",
                Map.of(
                        "source", "native_screencore_lifecycle",
                        "eventType", "fullscreen_screencore_command",
                        "screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        "command", "toggleWaypoints"
                )));
        dispatchResults.put("holomap.screencore.close", host.dispatchRoute(
                "holomap",
                "holomap.close",
                Map.of(
                        "source", "native_screencore_lifecycle",
                        "eventType", "fullscreen_screencore_command",
                        "screenClass", "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration",
                        "command", "close"
                )));
        dispatchResults.put("holomap.minimap.render", windowPump.renderGuiLayer(
                "holomap",
                "holomap.minimap.render",
                320,
                180,
                96,
                44,
                0.5F,
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("hud.render", windowPump.renderHudLayer(
                "hud",
                "hud.render",
                320,
                180,
                0.5F,
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("hud.update_snapshot", host.dispatchRoute("hud", "hud.update_snapshot"));
        dispatchResults.put("hud.mission_tracker.render",
                windowPump.renderHudLayer("hud_widget", "hud.mission_tracker.render", 320, 180, 0.5F, Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("hud.hazard_readout.render",
                windowPump.renderHudLayer("hud_widget", "hud.hazard_readout.render", 320, 180, 0.5F, Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("hud.compass_indicator.render",
                windowPump.renderHudLayer("hud_widget", "hud.compass_indicator.render", 320, 180, 0.5F, Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("hud.screen_safe_area.resolve",
                host.dispatchRoute("hud_layout", "hud.screen_safe_area.resolve"));
        dispatchResults.put("menu.open", windowPump.openScreen(
                "main_menu",
                "menu.open",
                "dev.echo.nativeplatform.loader.AshfallMainMenuScreen",
                320,
                180,
                "main_menu",
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("menu.close.quit", windowPump.closeScreen(
                "main_menu",
                "menu.quit",
                "dev.echo.nativeplatform.loader.AshfallMainMenuScreen",
                "quit",
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("menu.new_run", host.dispatchRoute(
                "main_menu",
                "menu.new_run",
                Map.of(
                        "source", "native_loader_client_ui_host",
                        "service", "screen_lifecycle",
                        "selection", "new_run"
                )));
        Map<String, Object> mainMenuRendererFrame = windowPump.builtInProductRendererFrame(
                "main_menu",
                "menu.new_run",
                320,
                180,
                152,
                96,
                0.5F,
                Map.of(
                        "selection", "new_run"
                ));
        dispatchResults.put("loading.mount.open", windowPump.mountScreen(
                "loading_screen",
                "loading.open",
                "dev.echo.nativeplatform.loader.AshfallLoadingScreen",
                320,
                180,
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("loading.render", windowPump.renderGuiLayer(
                "loading_screen",
                "loading.render",
                320,
                180,
                -1,
                -1,
                0.5F,
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("loading.progress", host.dispatchRoute(
                "loading_screen",
                "loading.progress",
                Map.of(
                        "source", "native_loader_client_ui_host",
                        "service", "gui_layer",
                        "progress", 0.65D,
                        "label", "Ashfall terrain bootstrap"
                )));
        Map<String, Object> loadingRendererFrame = windowPump.builtInProductRendererFrame(
                "loading_screen",
                "loading.progress",
                320,
                180,
                -1,
                -1,
                0.5F,
                Map.of(
                        "progress", 0.65D,
                        "label", "Ashfall terrain bootstrap"
                ));
        dispatchResults.put("loading.complete", host.dispatchRoute(
                "loading_screen",
                "loading.complete",
                Map.of(
                        "source", "native_loader_client_ui_host",
                        "service", "gui_layer"
                )));
        dispatchResults.put("loading.unmount.complete", windowPump.unmountScreen(
                "loading_screen",
                "loading.complete",
                "dev.echo.nativeplatform.loader.AshfallLoadingScreen",
                "complete",
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("terminal.overlay.tick", host.dispatchRoute(
                "client_overlay",
                "terminal.mission_hud.tick",
                Map.of(
                        "source", "native_loader_tick_service",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "client_tick_post",
                        "overlay", "mission_hud"
                )));
        dispatchResults.put("terminal.overlay.render", windowPump.renderGuiLayer(
                "client_overlay",
                "terminal.mission_hud.render",
                320,
                180,
                -1,
                -1,
                0.5F,
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("terminal.discovery_toast.tick", registry.tickRoute(
                "client_overlay",
                "terminal.discovery_toast.tick",
                "post",
                Map.of(
                        "source", "native_loader_tick_service",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "client_tick_post",
                        "overlay", "discovery_toast"
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("terminal.discovery_toast.render", host.dispatchRoute(
                "client_overlay",
                "terminal.discovery_toast.render",
                Map.of(
                        "source", "native_loader_gui_layer",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "render_gui_post",
                        "overlay", "discovery_toast",
                        "partialTick", 0.5F
                )));
        dispatchResults.put("index.overlay.input", host.dispatchRoute(
                "client_overlay",
                "index.inventory_overlay_input",
                Map.of(
                        "source", "native_loader_overlay_input",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "mouse_scrolled",
                        "eventMetadata", Map.of("scrollDeltaY", -1.0D)
                )));
        dispatchResults.put("index.overlay.mouse_clicked", host.dispatchRoute(
                "client_overlay",
                "index.inventory_overlay_input",
                Map.of(
                        "source", "native_loader_overlay_input",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "mouse_clicked",
                        "eventMetadata", Map.of(
                                "button", 0,
                                "modifiers", 0,
                                "mouseX", 144.0D,
                                "mouseY", 88.0D
                        )
                )));
        dispatchResults.put("index.overlay.key_pressed", host.dispatchRoute(
                "client_overlay",
                "index.inventory_overlay_input",
                Map.of(
                        "source", "native_loader_overlay_input",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "key_pressed",
                        "eventMetadata", Map.of(
                                "key", 82,
                                "recipeKey", true,
                                "usageKey", false,
                                "bookmarkKey", false
                        )
                )));
        dispatchResults.put("index.overlay.character_typed", host.dispatchRoute(
                "client_overlay",
                "index.inventory_overlay_input",
                Map.of(
                        "source", "native_loader_overlay_input",
                        "forwardedFrom", "neoforge_compatibility_adapter",
                        "eventType", "character_typed",
                        "eventMetadata", Map.of(
                                "character", "f",
                                "allowedChatCharacter", true
                        )
                )));
        dispatchResults.put("index.overlay.render", windowPump.renderGuiLayer(
                "client_overlay",
                "index.inventory_overlay_render",
                320,
                180,
                144,
                88,
                0.5F,
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("lens.overlay.render", windowPump.renderGuiLayer(
                "client_overlay",
                "lens.overlay.render",
                320,
                180,
                112,
                56,
                0.5F,
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("terminal.input",
                windowPump.keyInput("key.echoterminal.open", 77, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("overlay.focus", windowPump.focusOverlay(
                "hud",
                true,
                "hud",
                "client_overlay",
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("mouse.lifecycle", windowPump.mouseInput(
                "index",
                "index.catalog_screen.mouse",
                96,
                48,
                0,
                "press",
                Map.of(
                )) == EchoNativeLoadStatus.MUTATED);
        dispatchResults.put("tick.lifecycle", windowPump.tick(
                "client_tick",
                42,
                0.5F,
                Map.of(
                        "eventType", "client_tick_post"
                )) == EchoNativeLoadStatus.MUTATED);
        EchoNativeLoadStatus unownedRouteStatus = registry.dispatchStatus(
                "index",
                "index.unowned_side_event_fallback",
                Map.of("source", "agent2_negative_route_probe"));
        dispatchResults.put("unowned.route.rejected", unownedRouteStatus == EchoNativeLoadStatus.UNSUPPORTED);
        EchoNativeLoadStatus unknownInputBindingStatus = registry.dispatchInputBindingStatus(
                "key.echo.native.unowned_side_event_fallback",
                999,
                "press");
        dispatchResults.put("unknown.input.rejected", unknownInputBindingStatus == EchoNativeLoadStatus.UNSUPPORTED);

        Map<String, Boolean> directPublicSdkLifecycleResults = new LinkedHashMap<>();
        directPublicSdkLifecycleResults.put("terminal.open", registry.openSurface(
                "terminal",
                "terminal.open",
                directPublicSdkLifecycleMetadata("terminal", "screen_lifecycle")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkLifecycleResults.put("terminal.screen_lifecycle", registry.screenLifecycle(
                "terminal",
                "open",
                "terminal.open",
                directPublicSdkLifecycleMetadata("terminal", "screen_lifecycle")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkLifecycleResults.put("index.open", registry.openSurface(
                "index",
                "index.catalog",
                directPublicSdkLifecycleMetadata("index", "screen_lifecycle")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkLifecycleResults.put("lens.overlay.render", registry.publishLifecycleEvent(
                "client_overlay",
                "render",
                "lens.overlay.render",
                directPublicSdkLifecycleMetadata("client_overlay", "gui_layer")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkLifecycleResults.put("holomap.minimap.render", registry.publishLifecycleEvent(
                "holomap",
                "render",
                "holomap.minimap.render",
                directPublicSdkLifecycleMetadata("holomap", "gui_layer")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkLifecycleResults.put("hud.render", registry.publishLifecycleEvent(
                "hud",
                "render",
                "hud.render",
                directPublicSdkLifecycleMetadata("hud", "hud_layer")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkLifecycleResults.put("menu.open", registry.openSurface(
                "main_menu",
                "menu.open",
                directPublicSdkLifecycleMetadata("main_menu", "screen_lifecycle")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkLifecycleResults.put("loading.render", registry.publishLifecycleEvent(
                "loading_screen",
                "render",
                "loading.render",
                directPublicSdkLifecycleMetadata("loading_screen", "gui_layer")) == EchoNativeLoadStatus.MUTATED);

        Map<String, Boolean> directPublicSdkHostServiceResults = new LinkedHashMap<>();
        directPublicSdkHostServiceResults.put("terminal.key_input", registry.keyInput(
                "key.echoterminal.open",
                77,
                "press",
                directPublicSdkInputMetadata("terminal", "terminal.open")) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkHostServiceResults.put("index.overlay_input", registry.overlayInput(
                "index",
                "index.catalog_screen.key",
                directPublicSdkMetadata("index", Map.of("eventType", "direct_public_sdk_overlay_input"))
        ) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkHostServiceResults.put("index.mouse_input", registry.mouseInput(
                "index",
                "index.catalog_screen.mouse",
                directPublicSdkMetadata("index", Map.of(
                        "eventType", "direct_public_sdk_mouse_input",
                        "phase", "press",
                        "mouseX", 12.0D,
                        "mouseY", 24.0D))
        ) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkHostServiceResults.put("hud.focus_overlay", registry.focusOverlay(
                "hud",
                true,
                directPublicSdkMetadata("hud", Map.of("eventType", "direct_public_sdk_overlay_focus"))
        ) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkHostServiceResults.put("client.tick", registry.tick(
                "direct_public_sdk_tick",
                directPublicSdkMetadata("", Map.of("eventType", "direct_public_sdk_tick"))
        ) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkHostServiceResults.put("terminal.tick_route", registry.tickRoute(
                "terminal",
                "terminal.open",
                "direct_public_sdk_tick_route",
                directPublicSdkMetadata("terminal", Map.of("eventType", "direct_public_sdk_tick_route"))
        ) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkHostServiceResults.put("loading.gui_layer", registry.renderGuiLayer(
                "loading_screen",
                "loading.render",
                directPublicSdkMetadata("loading_screen", Map.of("eventType", "direct_public_sdk_gui_layer"))
        ) == EchoNativeLoadStatus.MUTATED);
        directPublicSdkHostServiceResults.put("hud.hud_layer", registry.renderHudLayer(
                "hud",
                "hud.render",
                directPublicSdkMetadata("hud", Map.of("eventType", "direct_public_sdk_hud_layer"))
        ) == EchoNativeLoadStatus.MUTATED);

        Map<String, Boolean> inputDispatchResults = new LinkedHashMap<>();
        inputDispatchResults.put("menu.open", hostMenuInputStatus == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("menu.new_run", hostMenuNewRunInputStatus == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("menu.quit", hostMenuQuitInputStatus == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("terminal.open",
                windowPump.keyInput("key.echoterminal.open", 77, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("index.catalog",
                windowPump.keyInput("key.echoindex.catalog", 71, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("index.recipe",
                windowPump.keyInput("key.echoindex.recipe", 82, "press", Map.of(
                        "screenClass", "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                        "scanCode", 19,
                        "modifiers", 2,
                        "focusedSurface", "index"
                )) == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("index.usage",
                windowPump.keyInput("key.echoindex.usage", 85, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("index.bookmark",
                windowPump.keyInput("key.echoindex.bookmark", 66, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("lens.deep_scan",
                windowPump.keyInput("echolens.key.deep_scan", 342, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("lens.index_recipe",
                windowPump.keyInput("key.echolens.index_recipe", 82, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("lens.index_usage",
                windowPump.keyInput("key.echolens.index_usage", 85, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("lens.track_in_index",
                windowPump.keyInput("key.echolens.track_in_index", 84, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("holomap.open",
                windowPump.keyInput("key.echoholomap.open_map", 74, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("holomap.toggle_minimap",
                windowPump.keyInput("key.echoholomap.toggle_minimap", 75, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("holomap.zoom_in",
                windowPump.keyInput("key.echoholomap.minimap_zoom_in", 93, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("holomap.zoom_out",
                windowPump.keyInput("key.echoholomap.minimap_zoom_out", 91, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        inputDispatchResults.put("holomap.cycle_corner",
                windowPump.keyInput("key.echoholomap.minimap_cycle_corner", 92, "press", Map.of())
                        == EchoNativeLoadStatus.MUTATED);
        Map<String, Boolean> mutationOnlyBooleanDispatchResults = requireMutationOnlyBooleanDispatch();

        require(dispatchResults.values().stream().allMatch(Boolean.TRUE::equals),
                "Every required native client route dispatch must succeed.");
        require(directPublicSdkDispatchResults.values().stream().allMatch(Boolean.TRUE::equals),
                "Every primary product surface must mutate through direct public SDK route dispatch.");
        require(directPublicSdkInputDispatchResults.values().stream().allMatch(Boolean.TRUE::equals),
                "Every primary product key binding must mutate through direct public SDK input dispatch.");
        require(directPublicSdkLifecycleResults.values().stream().allMatch(Boolean.TRUE::equals),
                "Every primary product lifecycle surface must mutate through direct public SDK lifecycle publication.");
        require(directPublicSdkHostServiceResults.values().stream().allMatch(Boolean.TRUE::equals),
                "Every typed public SDK host-service method must mutate through Native Loader UI host ownership.");
        require(inputDispatchResults.values().stream().allMatch(Boolean.TRUE::equals),
                "Every product key input must dispatch through Native Loader route-owned input bindings.");
        for (String surface : REQUIRED_SURFACES) {
            require(registry.hasTrustedRoute(surface), "Missing trusted native route for " + surface + ".");
        }
        require(registry.hasTrustedRoute("hud_widget"),
                "Missing trusted native route for HUD widget surfaces.");
        require(registry.hasTrustedRoute("hud_layout"),
                "Missing trusted native route for HUD layout surfaces.");
        requireInputBindings(registry.inputBindings(), Map.of(
                "terminal", List.of("terminal.open"),
                "index", List.of("index.catalog", "index.recipe", "index.usage", "index.bookmark"),
                "lens", List.of("lens.deep_scan", "lens.index_recipe", "lens.index_usage", "lens.track_in_index"),
                "holomap", List.of("holomap.open", "holomap.toggle_minimap", "holomap.zoom_in",
                        "holomap.zoom_out", "holomap.cycle_corner"),
                "main_menu", List.of("menu.open", "menu.new_run", "menu.quit")
        ));
        requireInputDispatchEvidence(NativeLoaderClientRouteTable.inputDispatchEvidence(), Map.ofEntries(
                Map.entry("key.echo.native.menu", "main_menu/menu.open"),
                Map.entry("key.echo.native.menu.new_run", "main_menu/menu.new_run"),
                Map.entry("key.echo.native.menu.quit", "main_menu/menu.quit"),
                Map.entry("key.echoterminal.open", "terminal/terminal.open"),
                Map.entry("key.echoindex.catalog", "index/index.catalog"),
                Map.entry("key.echoindex.recipe", "index/index.recipe"),
                Map.entry("key.echoindex.usage", "index/index.usage"),
                Map.entry("key.echoindex.bookmark", "index/index.bookmark"),
                Map.entry("echolens.key.deep_scan", "lens/lens.deep_scan"),
                Map.entry("key.echolens.index_recipe", "lens/lens.index_recipe"),
                Map.entry("key.echolens.index_usage", "lens/lens.index_usage"),
                Map.entry("key.echolens.track_in_index", "lens/lens.track_in_index"),
                Map.entry("key.echoholomap.open_map", "holomap/holomap.open"),
                Map.entry("key.echoholomap.toggle_minimap", "holomap/holomap.toggle_minimap"),
                Map.entry("key.echoholomap.minimap_zoom_in", "holomap/holomap.zoom_in"),
                Map.entry("key.echoholomap.minimap_zoom_out", "holomap/holomap.zoom_out"),
                Map.entry("key.echoholomap.minimap_cycle_corner", "holomap/holomap.cycle_corner")
        ));
        Map<String, Boolean> sourceGateResults = requireStatusAwareInputAdapters();
        Map<String, Boolean> routeSourceGateResults = requireStatusAwareRouteAdapters();
        Map<String, Boolean> nativeEventAdapterSourceGateResults = requireNativeEventAdaptersRouteOwned();
        Map<String, Boolean> nativeHostStatusFallbackGateResults = requireStatusAwareNativeHostFallbacks();
        Map<String, Boolean> inputStatusPreservationGateResults = requireStatusAwareInputDispatchTargets();
        Map<String, Boolean> metadataAwareInputAdapterGateResults = requireMetadataAwareInputAdapters();
        Map<String, Boolean> terminalNativeRouteStateGateResults = requireTerminalNativeRouteStateSource();
        Map<String, Boolean> holoMapNativeRouteStateGateResults = requireHoloMapNativeRouteStateSource();
        Map<String, Boolean> productRouteStateGateResults = requireProductRouteStateSources();
        Map<String, Boolean> productionClientRouteRegistrationGateResults =
                requireProductionClientRouteRegistrationSources();
        Map<String, Boolean> sharedRouteHandlerSourceGateResults = requireSharedRouteHandlerSurfaceIds();
        Map<String, Boolean> routeTableOwnerHandlerGateResults = requireRouteTableOwnerPreferredHandlers();
        Map<String, Boolean> nativeCrossSurfaceRouteGateResults = requireNativeCrossSurfaceRouteHandoffs();
        Map<String, Boolean> nativeScreenLifecycleTransitionGateResults = requireNativeScreenLifecycleTransitions();
        Map<String, Boolean> nativeWindowPumpGateResults = requireNativeWindowPumpService();
        Map<String, Object> clientWindowPumpServiceGateResults = requireClientWindowPumpServiceRegistration();
        Map<String, Object> activationClientWindowPumpGateResults = requireActivationClientWindowPumpRegistration();
        require(Boolean.TRUE.equals(dispatchResults.get("terminal.input")),
                "Native input binding dispatch must open Terminal without NeoForge event ownership.");
        requireTerminalScreenInputMetadata(dispatchMetadata);
        requireTerminalScreenCoreMetadata(dispatchMetadata);
        requireOverlayAdapterMetadata(dispatchMetadata);
        requireIndexCatalogScreenMetadata(dispatchMetadata);
        requireIndexScreenCoreActionMetadata(dispatchMetadata);
        requireIndexRecipeScreenMetadata(dispatchMetadata);
        requireHoloMapScreenMetadata(dispatchMetadata);
        requireHoloMapMinimapMetadata(dispatchMetadata);
        requireHudRenderMetadata(dispatchMetadata);
        requireHudRouteActionMetadata(dispatchMetadata);
        requireLensOverlayMetadata(dispatchMetadata);
        require(dispatchCounts.keySet().containsAll(List.of(
                        "terminal.open",
                        "terminal.screen.char_typed",
                        "terminal.screen.mouse_scroll",
                        "terminal.screen.frame.render",
                        "terminal.screencore.mouse",
                        "terminal.screencore.scroll",
                        "terminal.screencore.key",
                        "terminal.screencore.char",
                        "terminal.screencore.action",
                        "index.catalog",
                        "index.recipe",
                        "index.usage",
                        "index.bookmark",
                        "index.hotkey_screen_render",
                        "index.hotkey_key_pressed",
                        "index.catalog_screen.mouse",
                        "index.catalog_screen.scroll",
                        "index.catalog_screen.key",
                        "index.catalog_screen.char",
                        "index.screencore.action",
                        "index.recipe_screen.mouse",
                        "index.recipe_screen.scroll",
                        "index.recipe_screen.key",
                        "index.recipe_screen.char",
                        "lens.deep_scan",
                        "lens.index_recipe",
                        "lens.index_usage",
                        "lens.track_in_index",
                        "holomap.open",
                        "holomap.fullscreen.key",
                        "holomap.fullscreen.mouse",
                        "holomap.fullscreen.scroll",
                        "holomap.sync",
                        "holomap.center",
                        "holomap.toggle_markers",
                        "holomap.cycle_fields",
                        "holomap.toggle_waypoints",
                        "holomap.close",
                        "holomap.minimap.render",
                        "holomap.toggle_minimap",
                        "holomap.zoom_in",
                        "holomap.zoom_out",
                        "holomap.cycle_corner",
                        "hud.render",
                        "hud.update_snapshot",
                        "native_loader.overlay_focus",
                        "hud.mission_tracker.render",
                        "hud.hazard_readout.render",
                        "hud.compass_indicator.render",
                        "hud.screen_safe_area.resolve",
                        "terminal.mission_hud.tick",
                        "terminal.mission_hud.render",
                        "terminal.discovery_toast.tick",
                        "terminal.discovery_toast.render",
                        "index.inventory_overlay_input",
                        "index.inventory_overlay_render",
                        "lens.overlay.render"
                )),
                "Dispatch counts must include every required product surface action.");
        requireDispatchCounts(dispatchCounts, Map.ofEntries(
                Map.entry("terminal.open", 14),
                Map.entry("terminal.screen.char_typed", 1),
                Map.entry("terminal.screen.mouse_scroll", 1),
                Map.entry("terminal.screen.frame.render", 1),
                Map.entry("terminal.screencore.mouse", 1),
                Map.entry("terminal.screencore.scroll", 1),
                Map.entry("terminal.screencore.key", 1),
                Map.entry("terminal.screencore.char", 1),
                Map.entry("terminal.screencore.action", 1),
                Map.entry("index.catalog", 6),
                Map.entry("index.recipe", 2),
                Map.entry("index.usage", 2),
                Map.entry("index.bookmark", 2),
                Map.entry("index.hotkey_screen_render", 1),
                Map.entry("index.hotkey_key_pressed", 1),
                Map.entry("index.client.login", 1),
                Map.entry("index.client.logout", 1),
                Map.entry("index.client.resources_reloaded", 1),
                Map.entry("index.catalog_screen.mouse", 3),
                Map.entry("index.catalog_screen.scroll", 1),
                Map.entry("index.catalog_screen.key", 2),
                Map.entry("index.catalog_screen.char", 1),
                Map.entry("index.screencore.action", 1),
                Map.entry("index.recipe_screen.mouse", 1),
                Map.entry("index.recipe_screen.scroll", 1),
                Map.entry("index.recipe_screen.key", 1),
                Map.entry("index.recipe_screen.char", 1),
                Map.entry("lens.deep_scan", 4),
                Map.entry("lens.index_recipe", 2),
                Map.entry("lens.index_usage", 2),
                Map.entry("lens.track_in_index", 2),
                Map.entry("holomap.open", 5),
                Map.entry("holomap.fullscreen.key", 2),
                Map.entry("holomap.fullscreen.mouse", 2),
                Map.entry("holomap.fullscreen.scroll", 2),
                Map.entry("holomap.sync", 1),
                Map.entry("holomap.center", 1),
                Map.entry("holomap.toggle_markers", 1),
                Map.entry("holomap.cycle_fields", 1),
                Map.entry("holomap.toggle_waypoints", 1),
                Map.entry("holomap.close", 1),
                Map.entry("holomap.minimap.render", 3),
                Map.entry("holomap.toggle_minimap", 2),
                Map.entry("holomap.zoom_in", 2),
                Map.entry("holomap.zoom_out", 2),
                Map.entry("holomap.cycle_corner", 2),
                Map.entry("hud.render", 5),
                Map.entry("hud.update_snapshot", 2),
                Map.entry("native_loader.overlay_focus", 2),
                Map.entry("hud.mission_tracker.render", 2),
                Map.entry("hud.hazard_readout.render", 2),
                Map.entry("hud.compass_indicator.render", 2),
                Map.entry("hud.screen_safe_area.resolve", 2),
                Map.entry("terminal.mission_hud.tick", 3),
                Map.entry("terminal.mission_hud.render", 1),
                Map.entry("terminal.discovery_toast.tick", 3),
                Map.entry("terminal.discovery_toast.render", 1),
                Map.entry("index.inventory_overlay_input", 4),
                Map.entry("index.inventory_overlay_render", 1),
                Map.entry("lens.overlay.render", 3)
        ));
        require("echo-native-loader".equals(registry.mountedSurfaceRoutes()
                        .getOrDefault("main_menu", Map.of())
                        .get("moduleId")),
                "Main menu route must be seeded by Native Loader, not the test stub.");
        require("echo-native-loader".equals(registry.mountedSurfaceRoutes()
                        .getOrDefault("loading_screen", Map.of())
                        .get("moduleId")),
                "Loading screen route must be seeded by Native Loader, not the test stub.");
        Map<String, Object> actionHandlerEvidence = NativeLoaderClientRouteTable.actionHandlerEvidence();
        require(handlerCount(actionHandlerEvidence, "main_menu") == 1,
                "Native Loader built-in main menu action handler must be idempotent across host construction.");
        require(handlerCount(actionHandlerEvidence, "loading_screen") == 1,
                "Native Loader built-in loading action handler must be idempotent across host construction.");
        requireBuiltInProductSurfaceState(NativeLoaderClientUiHost.builtInProductSurfaceState());
        requireBuiltInProductRendererFrame(mainMenuRendererFrame, "main_menu", "menu.new_run");
        requireBuiltInProductRendererFrame(loadingRendererFrame, "loading_screen", "loading.progress");
        require(host.overlayInput(
                        "client_overlay",
                        "index.inventory_overlay_input",
                        Map.of(
                                "source", "native_loader_overlay_input",
                                "eventType", "agent2_bridge_overlay_input_probe",
                                "key", 82,
                                "usageKey", true,
                                "screenClass", "IndexCatalogScreen"
                        )
                ) == EchoNativeLoadStatus.MUTATED,
                "Native Loader UI host overlay input must dispatch through the live client bridge.");
        Map<String, Integer> productBridgeServiceCounts = serviceCounts(clientBridge.clientHostServiceEvidence());
        require(productBridgeServiceCounts.keySet().containsAll(List.of(
                        "screenLifecycle",
                        "dispatchRoute",
                        "renderHudLayer",
                        "renderGuiLayer",
                        "dispatchInputBinding",
                        "overlayInput",
                        "overlayFocus",
                        "mouseInput",
                        "tick"
                )),
                "Default product NativeLoaderLiveClientBridge must receive every Native Loader client host service callback.");
        requireLiveClientBridgeServiceEvents(clientBridge.clientHostServiceEvidence());
        refreshDirectPublicSdkHostServiceEvidence(registry);
        requirePublicSdkHostServiceEvents(NativeLoaderClientRouteRegistryProvider.sdkRouteHostEvidence());
        refreshScenarioHostServiceEvidence(host);
        requireHostServiceEvents(host.routeHostEvidence());
        requireRouteHostReleaseGateEvidence(host.routeHostEvidence());
        requireClientRuntimeState(host.routeHostEvidence());
        Map<String, Boolean> runtimeRouteSnapshotGateResults =
                requireRuntimeRouteSnapshots(host.routeHostEvidence());
        refreshDirectPublicSdkActionDispatchEvidence(registry);
        requireActionDispatchEvidence(host.routeHostEvidence());
        refreshDirectPublicSdkLifecycleEvidence(registry);
        requireLifecycleEventEvidence(host.routeHostEvidence());
        Map<String, Boolean> directPublicSdkDispatchGateResults =
                requireDirectPublicSdkDispatchEvidence(host.routeHostEvidence());
        Map<String, Boolean> directPublicSdkInputDispatchGateResults =
                requireDirectPublicSdkInputDispatchEvidence(NativeLoaderClientRouteTable.inputDispatchEvidence());
        Map<String, Boolean> directPublicSdkLifecycleGateResults =
                requireDirectPublicSdkLifecycleEvidence(registry.lifecycleEvents());
        requireRouteOwner("client_overlay", "terminal.mission_hud.render", "echoterminal", "echoterminal:hud_overlay");
        requireRouteOwner("client_overlay", "terminal.discovery_toast.tick", "echoterminal", "echoterminal:hud_overlay");
        requireRouteOwner("client_overlay", "terminal.discovery_toast.render", "echoterminal", "echoterminal:hud_overlay");
        requireRouteOwner("client_overlay", "index.inventory_overlay_render", "echoindex", "echoindex:inventory_overlay");
        requireRouteOwner("client_overlay", "index.track_item", "echoindex", "echoindex:inventory_overlay");
        requireRouteOwner("client_overlay", "lens.overlay.render", "echolens", "echolens:lens_overlay");
        requireRouteOwner("index", "index.hotkey_screen_render", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.hotkey_key_pressed", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.client.login", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.client.logout", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.client.resources_reloaded", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.catalog_screen.mouse", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.catalog_screen.scroll", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.catalog_screen.key", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.catalog_screen.char", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.screencore.action", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.recipe_screen.mouse", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.recipe_screen.scroll", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.recipe_screen.key", "echoindex", "echoindex:index");
        requireRouteOwner("index", "index.recipe_screen.char", "echoindex", "echoindex:index");
        requireRouteOwner("terminal", "terminal.screen.char_typed", "echoterminal", "echoterminal:eui");
        requireRouteOwner("terminal", "terminal.screen.mouse_scroll", "echoterminal", "echoterminal:eui");
        requireRouteOwner("terminal", "terminal.screen.frame.render", "echoterminal", "echoterminal:eui");
        requireRouteOwner("terminal", "terminal.screencore.mouse", "echoterminal", "echoterminal:eui");
        requireRouteOwner("terminal", "terminal.screencore.scroll", "echoterminal", "echoterminal:eui");
        requireRouteOwner("terminal", "terminal.screencore.key", "echoterminal", "echoterminal:eui");
        requireRouteOwner("terminal", "terminal.screencore.char", "echoterminal", "echoterminal:eui");
        requireRouteOwner("terminal", "terminal.screencore.action", "echoterminal", "echoterminal:eui");
        requireRouteOwner("holomap", "holomap.open", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.fullscreen.key", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.fullscreen.mouse", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.fullscreen.scroll", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.sync", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.center", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.toggle_markers", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.cycle_fields", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.toggle_waypoints", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.select_entry", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.close", "echoholomap", "echoholomap:fullscreen_map");
        requireRouteOwner("holomap", "holomap.minimap.render", "echoholomap", "echoholomap:minimap");
        requireRouteOwner("hud", "hud.render", "echohudcore", "echohudcore:native_hud");
        requireRouteOwner("hud", "hud.update_snapshot", "echohudcore", "echohudcore:native_hud");
        requireRouteOwner("hud", "native_loader.overlay_focus", "echohudcore", "echohudcore:native_hud");
        requireRouteOwner("hud_widget", "hud.mission_tracker.render", "echohudcore", "echohudcore:mission_tracker");
        requireRouteOwner("hud_widget", "hud.hazard_readout.render", "echohudcore", "echohudcore:hazard_readout");
        requireRouteOwner("hud_widget", "hud.compass_indicator.render", "echohudcore", "echohudcore:compass_indicator");
        requireRouteOwner("hud_layout", "hud.screen_safe_area.resolve", "echohudcore", "echohudcore:screen_safe_area");

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.native.agent2.client_route_ownership.v1");
        report.put("runtimeLane", "Native Loader");
        report.put("scope", "Parallel Agent 2 native client route/UI ownership");
        report.put("requiredSurfaces", REQUIRED_SURFACES);
        report.put("dispatchResults", dispatchResults);
        report.put("directPublicSdkDispatchResults", directPublicSdkDispatchResults);
        report.put("unownedRouteStatus", unownedRouteStatus.name());
        report.put("unknownInputBindingStatus", unknownInputBindingStatus.name());
        report.put("inputDispatchResults", inputDispatchResults);
        report.put("directPublicSdkInputDispatchResults", directPublicSdkInputDispatchResults);
        report.put("directPublicSdkLifecycleResults", directPublicSdkLifecycleResults);
        report.put("directPublicSdkHostServiceResults", directPublicSdkHostServiceResults);
        report.put("mutationOnlyBooleanDispatchResults", mutationOnlyBooleanDispatchResults);
        report.put("sourceGateResults", sourceGateResults);
        report.put("routeSourceGateResults", routeSourceGateResults);
        report.put("nativeEventAdapterSourceGateResults", nativeEventAdapterSourceGateResults);
        report.put("nativeHostStatusFallbackGateResults", nativeHostStatusFallbackGateResults);
        report.put("inputStatusPreservationGateResults", inputStatusPreservationGateResults);
        report.put("metadataAwareInputAdapterGateResults", metadataAwareInputAdapterGateResults);
        report.put("terminalNativeRouteStateGateResults", terminalNativeRouteStateGateResults);
        report.put("holoMapNativeRouteStateGateResults", holoMapNativeRouteStateGateResults);
        report.put("productRouteStateGateResults", productRouteStateGateResults);
        report.put("productionClientRouteRegistrationGateResults", productionClientRouteRegistrationGateResults);
        report.put("runtimeRouteSnapshotGateResults", runtimeRouteSnapshotGateResults);
        report.put("sharedRouteHandlerSourceGateResults", sharedRouteHandlerSourceGateResults);
        report.put("routeTableOwnerHandlerGateResults", routeTableOwnerHandlerGateResults);
        report.put("nativeCrossSurfaceRouteGateResults", nativeCrossSurfaceRouteGateResults);
        report.put("nativeScreenLifecycleTransitionGateResults", nativeScreenLifecycleTransitionGateResults);
        report.put("nativeWindowPumpGateResults", nativeWindowPumpGateResults);
        report.put("clientWindowPumpServiceGateResults", clientWindowPumpServiceGateResults);
        report.put("activationClientWindowPumpGateResults", activationClientWindowPumpGateResults);
        report.put("directPublicSdkDispatchGateResults", directPublicSdkDispatchGateResults);
        report.put("directPublicSdkInputDispatchGateResults", directPublicSdkInputDispatchGateResults);
        report.put("directPublicSdkLifecycleGateResults", directPublicSdkLifecycleGateResults);
        report.put("dispatchCounts", dispatchCounts);
        report.put("dispatchMetadata", dispatchMetadata);
        report.put("registryProvider", registry.getClass().getName());
        report.put("routeHostEvidence", host.routeHostEvidence());
        report.put("actionRouteEvidence", NativeLoaderClientRouteTable.actionRouteEvidence());
        report.put("actionDispatchEvidence", NativeLoaderClientRouteTable.actionDispatchEvidence());
        report.put("actionHandlerEvidence", actionHandlerEvidence);
        report.put("builtInProductSurfaceState", NativeLoaderClientUiHost.builtInProductSurfaceState());
        report.put("builtInProductRendererFrames", Map.of(
                "main_menu", mainMenuRendererFrame,
                "loading_screen", loadingRendererFrame
        ));
        report.put("liveClientBridgeServiceCounts", productBridgeServiceCounts);
        report.put("liveClientBridgeHostServiceEvidence", clientBridge.clientHostServiceEvidence());
        report.put("builtInProductRoutes", Map.of(
                "main_menu", NativeLoaderClientRouteTable.routeForSurface("main_menu"),
                "loading_screen", NativeLoaderClientRouteTable.routeForSurface("loading_screen")
        ));
        report.put("inputBindingsFromRouteRegistry", true);
        report.put("hudOverlayLifecycleNativeOwned", true);
        report.put("sharedClientOverlayRouteOwned", true);
        report.put("neoForgeEventOwnershipRequired", false);
        report.put("exitGate",
                "Native route dispatch opened Terminal, Index, Lens, HoloMap, HUD, menu, and loading actions without NeoForge event ownership.");

        Files.createDirectories(output.getParent());
        Files.writeString(output, EchoNativeJson.write(report), StandardCharsets.UTF_8);
        System.out.println("agent2 native client route ownership smoke PASS " + output);
    }

    private static Map<String, Object> directPublicSdkMetadata(String surfaceType) {
        return directPublicSdkMetadata(surfaceType, Map.of());
    }

    private static Map<String, Object> directPublicSdkMetadata(String surfaceType, Map<String, Object> extraMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "agent2_direct_public_sdk_probe");
        metadata.put("eventType", "direct_route_dispatch");
        metadata.put("surfaceType", surfaceType);
        metadata.put("neoForgeEventOwnershipRequired", false);
        if (extraMetadata != null) {
            metadata.putAll(extraMetadata);
        }
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> directPublicSdkInputMetadata(String surfaceType, String actionId) {
        return Map.of(
                "source", "agent2_direct_public_sdk_input_probe",
                "eventType", "direct_input_binding_dispatch",
                "surfaceType", surfaceType,
                "actionId", actionId,
                "neoForgeEventOwnershipRequired", false
        );
    }

    private static Map<String, Object> directPublicSdkLifecycleMetadata(String surfaceType, String service) {
        return Map.of(
                "source", "agent2_direct_public_sdk_lifecycle_probe",
                "eventType", "direct_lifecycle_publication",
                "surfaceType", surfaceType,
                "service", service,
                "neoForgeEventOwnershipRequired", false
        );
    }

    private static void registerSurface(
            EchoNativeClientRouteRegistry registry,
            Map<String, Integer> dispatchCounts,
            String moduleId,
            String surfaceId,
            String surfaceType,
            String actionId,
            String keyMapping,
            int keyCode
    ) {
        registerSurface(registry, dispatchCounts, null, moduleId, surfaceId, surfaceType, actionId, keyMapping, keyCode);
    }

    private static void registerSurface(
            EchoNativeClientRouteRegistry registry,
            Map<String, Integer> dispatchCounts,
            Map<String, Map<String, Object>> dispatchMetadata,
            String moduleId,
            String surfaceId,
            String surfaceType,
            String actionId,
            String keyMapping,
            int keyCode
    ) {
        EchoNativeLoadStatus status = registry.registerRoute(
                moduleId,
                surfaceId,
                surfaceType,
                Map.of(
                        "nativeSurfaceImplementationClass", moduleId + ".NativeSurface",
                        "nativeScreenBridgeClass", moduleId + ".NativeRouteBridge",
                        "source", "agent2_client_route_ownership_smoke"
                ),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry",
                        "neoForgeEventOwnershipRequired", false
                ),
                true
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "Route registration must mutate native route table for " + surfaceType + ".");
        registry.registerActions(moduleId, surfaceId, surfaceType, Map.of(actionId, Map.of(
                "kind", surfaceType + "_action",
                "opensLiveSurface", true,
                "rendersLiveSurface", surfaceType.equals("hud") || surfaceType.equals("loading_screen"),
                "updatesLiveSurface", true
        )));
        if (keyCode >= 0) {
            registry.registerInputBinding(surfaceType, actionId, Map.of(
                    "keyMapping", keyMapping,
                    "keyCode", keyCode,
                    "inputType", "press",
                    "action", actionId,
                    "source", "agent2_client_route_ownership_smoke"
            ));
        }
        registry.registerActionHandler(surfaceType, surfaceId + ":" + actionId, context -> {
            if (!actionId.equals(context.actionId())) {
                return false;
            }
            dispatchCounts.merge(actionId, 1, Integer::sum);
            if (dispatchMetadata != null) {
                dispatchMetadata.put(context.actionId(), context.metadata());
                Object eventType = context.metadata().get("eventType");
                if (eventType != null) {
                    dispatchMetadata.put(context.actionId() + ":" + eventType, context.metadata());
                }
            }
            return true;
        });
    }

    private static void registerAdditionalAction(
            EchoNativeClientRouteRegistry registry,
            Map<String, Integer> dispatchCounts,
            String moduleId,
            String surfaceId,
            String surfaceType,
            String actionId,
            String kind,
            String actionModuleId
    ) {
        registry.registerActions(moduleId, surfaceId, surfaceType, Map.of(
                actionId, Map.of("kind", kind, "moduleId", actionModuleId)
        ));
        registry.registerActionHandler(surfaceType, surfaceId + ":" + actionId, context -> {
            if (!actionId.equals(context.actionId())) {
                return false;
            }
            dispatchCounts.merge(context.actionId(), 1, Integer::sum);
            return true;
        });
    }

    private static void registerAdditionalAction(
            EchoNativeClientRouteRegistry registry,
            Map<String, Integer> dispatchCounts,
            Map<String, Map<String, Object>> dispatchMetadata,
            String moduleId,
            String surfaceId,
            String surfaceType,
            String actionId,
            String kind,
            String actionModuleId
    ) {
        registry.registerActions(moduleId, surfaceId, surfaceType, Map.of(
                actionId, Map.of("kind", kind, "moduleId", actionModuleId)
        ));
        registry.registerActionHandler(surfaceType, surfaceId + ":" + actionId, context -> {
            if (!actionId.equals(context.actionId())) {
                return false;
            }
            dispatchCounts.merge(context.actionId(), 1, Integer::sum);
            dispatchMetadata.put(context.actionId(), context.metadata());
            Object eventType = context.metadata().get("eventType");
            if (eventType != null) {
                dispatchMetadata.put(context.actionId() + ":" + eventType, context.metadata());
            }
            return true;
        });
    }

    private static void registerAdditionalInputAction(
            EchoNativeClientRouteRegistry registry,
            Map<String, Integer> dispatchCounts,
            String moduleId,
            String surfaceId,
            String surfaceType,
            String actionId,
            String kind,
            String keyMapping,
            int keyCode
    ) {
        registry.registerActions(moduleId, surfaceId, surfaceType, Map.of(
                actionId, Map.of("kind", kind, "moduleId", moduleId)
        ));
        registry.registerInputBinding(surfaceType, actionId, Map.of(
                "keyMapping", keyMapping,
                "keyCode", keyCode,
                "inputType", "press",
                "action", actionId,
                "source", "agent2_client_route_ownership_smoke"
        ));
        registry.registerActionHandler(surfaceType, surfaceId + ":" + actionId, context -> {
            if (!actionId.equals(context.actionId())) {
                return false;
            }
            dispatchCounts.merge(context.actionId(), 1, Integer::sum);
            return true;
        });
    }

    private static void registerAdditionalRoute(
            EchoNativeClientRouteRegistry registry,
            String moduleId,
            String surfaceId,
            String surfaceType
    ) {
        EchoNativeLoadStatus status = registry.registerRoute(
                moduleId,
                surfaceId,
                surfaceType,
                Map.of(
                        "nativeSurfaceImplementationClass", moduleId + ".NativeOverlay",
                        "nativeScreenBridgeClass", moduleId + ".NativeOverlayBridge",
                        "source", "agent2_client_route_ownership_smoke"
                ),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry",
                        "neoForgeEventOwnershipRequired", false
                ),
                true
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "Additional route registration must mutate native route table for " + surfaceId + ".");
    }

    private static void registerOverlaySurface(
            EchoNativeClientRouteRegistry registry,
            Map<String, Integer> dispatchCounts,
            Map<String, Map<String, Object>> dispatchMetadata,
            String moduleId,
            String surfaceId,
            String updateActionId,
            String renderActionId
    ) {
        EchoNativeLoadStatus status = registry.registerRoute(
                moduleId,
                surfaceId,
                "client_overlay",
                Map.of(
                        "nativeSurfaceImplementationClass", moduleId + ".NativeOverlay",
                        "nativeScreenBridgeClass", moduleId + ".NativeOverlayBridge",
                        "source", "agent2_client_route_ownership_smoke"
                ),
                Map.of(
                        "nativeClientRouteProcess", true,
                        "clientRouteMutationSupported", true,
                        "nativeClientRouteSdk", "echo-native-client-route-registry",
                        "neoForgeEventOwnershipRequired", false
                ),
                true
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "Shared client overlay route registration must mutate native route table for " + moduleId + ".");
        registry.registerActions(moduleId, surfaceId, "client_overlay", Map.of(
                updateActionId, Map.of("kind", "client_overlay_update", "moduleId", moduleId),
                renderActionId, Map.of("kind", "client_overlay_render", "moduleId", moduleId)
        ));
        registry.registerActionHandler("client_overlay", surfaceId, context -> {
            if (!updateActionId.equals(context.actionId()) && !renderActionId.equals(context.actionId())) {
                return false;
            }
            dispatchCounts.merge(context.actionId(), 1, Integer::sum);
            dispatchMetadata.put(context.actionId(), context.metadata());
            Object eventType = context.metadata().get("eventType");
            if (eventType != null) {
                dispatchMetadata.put(context.actionId() + ":" + eventType, context.metadata());
            }
            return true;
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireRouteOwner(
            String surfaceType,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        Map<String, Object> route = NativeLoaderClientRouteTable.routeForAction(surfaceType, actionId);
        require(moduleId.equals(route.get("moduleId")) && surfaceId.equals(route.get("surfaceId")),
                "Action " + actionId + " must be owned by " + moduleId + "/" + surfaceId
                        + " but was " + route + ".");
    }

    private static void requireInputBindings(
            Map<String, Map<String, List<Map<String, Object>>>> inputBindings,
            Map<String, List<String>> requiredBindings
    ) {
        for (Map.Entry<String, List<String>> surfaceEntry : requiredBindings.entrySet()) {
            Map<String, List<Map<String, Object>>> surfaceBindings =
                    inputBindings.getOrDefault(surfaceEntry.getKey(), Map.of());
            for (String actionId : surfaceEntry.getValue()) {
                require(surfaceBindings.containsKey(actionId) && !surfaceBindings.get(actionId).isEmpty(),
                        "Native input binding must be declared for " + surfaceEntry.getKey() + "/" + actionId + ".");
            }
        }
    }

    private static void requireInputDispatchEvidence(
            Map<String, Object> inputDispatchEvidence,
            Map<String, String> requiredTargets
    ) {
        Object eventsObject = inputDispatchEvidence.get("events");
        require(eventsObject instanceof List<?>,
                "Native input dispatch evidence must include every product key dispatch.");
        List<?> events = (List<?>) eventsObject;
        require(events.size() >= requiredTargets.size(),
                "Native input dispatch evidence must include every product key dispatch.");
        for (Map.Entry<String, String> required : requiredTargets.entrySet()) {
            String keyMapping = required.getKey();
            String[] target = required.getValue().split("/", 2);
            require(target.length == 2, "Invalid required input dispatch target " + required.getValue() + ".");
            boolean found = false;
            boolean metadataFound = false;
            for (Object eventObject : events) {
                if (!(eventObject instanceof Map<?, ?> event)
                        || !keyMapping.equals(event.get("keyMapping"))
                        || !EchoNativeLoadStatus.MUTATED.name().equals(event.get("status"))
                        || !Boolean.TRUE.equals(event.get("handled"))
                        || !(event.get("targets") instanceof List<?> targets)) {
                    continue;
                }
                for (Object targetObject : targets) {
                    if (targetObject instanceof Map<?, ?> targetEvent
                            && target[0].equals(targetEvent.get("surfaceType"))
                            && target[1].equals(targetEvent.get("actionId"))
                            && Boolean.TRUE.equals(targetEvent.get("handled"))
                            && EchoNativeLoadStatus.MUTATED.name().equals(targetEvent.get("status"))) {
                        if ("key.echoindex.recipe".equals(keyMapping)) {
                            Object eventMetadata = event.get("metadata");
                            Object targetMetadata = targetEvent.get("metadata");
                            if (inputMetadataPresent(eventMetadata) && inputMetadataPresent(targetMetadata)) {
                                requireInputMetadata(eventMetadata, "event");
                                requireInputMetadata(targetMetadata, "target");
                                metadataFound = true;
                            }
                        }
                        found = true;
                    }
                    require(!(targetObject instanceof Map<?, ?> targetEvent)
                                    || !Boolean.TRUE.equals(targetEvent.get("handled"))
                                    || EchoNativeLoadStatus.MUTATED.name().equals(targetEvent.get("status")),
                            "Handled Native Loader input target must preserve MUTATED route dispatch status.");
                    require(!(targetObject instanceof Map<?, ?> targetEvent)
                                    || !keyMapping.equals("key.echoindex.recipe")
                                    || !"lens".equals(targetEvent.get("surfaceType")),
                            "Index recipe key mapping must not cross-dispatch to Lens by raw key code.");
                    require(!(targetObject instanceof Map<?, ?> targetEvent)
                                    || !keyMapping.equals("key.echolens.index_recipe")
                                    || !"index".equals(targetEvent.get("surfaceType")),
                            "Lens recipe key mapping must not cross-dispatch to Index by raw key code.");
                }
            }
            require(found, "Native input dispatch evidence must route " + keyMapping + " to " + required.getValue() + ".");
            require(!"key.echoindex.recipe".equals(keyMapping) || metadataFound,
                    "Native input dispatch evidence must include window key-event metadata for Index recipe input.");
        }
        boolean unknownRejected = false;
        for (Object eventObject : events) {
            if (eventObject instanceof Map<?, ?> event
                    && "key.echo.native.unowned_side_event_fallback".equals(event.get("keyMapping"))
                    && EchoNativeLoadStatus.UNSUPPORTED.name().equals(event.get("status"))
                    && Boolean.FALSE.equals(event.get("handled"))) {
                unknownRejected = true;
                break;
            }
        }
        require(unknownRejected,
                "Native input dispatch evidence must preserve UNSUPPORTED status for unknown input bindings.");
        requireInputDispatchSummary(inputDispatchEvidence);
    }

    private static void requireInputDispatchSummary(Map<String, Object> inputDispatchEvidence) {
        Object summaryObject = inputDispatchEvidence.get("summary");
        require(summaryObject instanceof Map<?, ?>,
                "Native input dispatch evidence must expose a compact summary.");
        Map<?, ?> summary = (Map<?, ?>) summaryObject;
        require(summary.get("statusCounts") instanceof Map<?, ?> statusCounts
                        && statusCounts.containsKey(EchoNativeLoadStatus.MUTATED.name())
                        && statusCounts.containsKey(EchoNativeLoadStatus.UNSUPPORTED.name()),
                "Native input dispatch summary must include mutated and unsupported status counts.");
        require(summary.get("metadataSourceCounts") instanceof Map<?, ?> metadataSourceCounts
                        && metadataSourceCounts.get("agent2_direct_public_sdk_input_probe") instanceof Number directInputCount
                        && directInputCount.intValue() >= 15,
                "Native input dispatch summary must include source counts for direct SDK input dispatch.");
        Object latestByKeyObject = summary.get("latestByKeyMapping");
        require(latestByKeyObject instanceof Map<?, ?>,
                "Native input dispatch summary must include latest events by key mapping.");
        Map<?, ?> latestByKey = (Map<?, ?>) latestByKeyObject;
        require(latestByKey.get("key.echoholomap.minimap_cycle_corner") instanceof Map<?, ?> holomapKey
                        && EchoNativeLoadStatus.MUTATED.name().equals(holomapKey.get("status")),
                "Native input dispatch summary must expose latest HoloMap minimap key dispatch.");
        require(latestByKey.get("key.echo.native.menu.new_run") instanceof Map<?, ?> menuNewRunKey
                        && EchoNativeLoadStatus.MUTATED.name().equals(menuNewRunKey.get("status")),
                "Native input dispatch summary must expose Native Loader menu new-run key dispatch.");
        require(latestByKey.get("key.echo.native.menu.quit") instanceof Map<?, ?> menuQuitKey
                        && EchoNativeLoadStatus.MUTATED.name().equals(menuQuitKey.get("status")),
                "Native input dispatch summary must expose Native Loader menu quit key dispatch.");
        require(latestByKey.get("key.echo.native.unowned_side_event_fallback") instanceof Map<?, ?> unknownKey
                        && EchoNativeLoadStatus.UNSUPPORTED.name().equals(unknownKey.get("status")),
                "Native input dispatch summary must expose unsupported unknown key dispatch.");
        Object latestBySourceObject = summary.get("latestByMetadataSource");
        require(latestBySourceObject instanceof Map<?, ?> latestBySource
                        && latestBySource.get("agent2_direct_public_sdk_input_probe") instanceof Map<?, ?> directInput
                        && EchoNativeLoadStatus.MUTATED.name().equals(directInput.get("status")),
                "Native input dispatch summary must expose latest direct public SDK input dispatch by source.");
        Object latestTargetsObject = summary.get("latestHandledTargetBySurface");
        require(latestTargetsObject instanceof Map<?, ?>,
                "Native input dispatch summary must include latest handled targets by surface.");
        Map<?, ?> latestTargets = (Map<?, ?>) latestTargetsObject;
        requireInputSummaryTarget(latestTargets, "terminal", "terminal.open", "echoterminal", "echoterminal:eui");
        requireInputSummaryTarget(latestTargets, "index", "index.bookmark", "echoindex", "echoindex:index");
        requireInputSummaryTarget(latestTargets, "lens", "lens.track_in_index", "echolens", "echolens:field_lens");
        requireInputSummaryTarget(latestTargets, "holomap", "holomap.cycle_corner",
                "echoholomap", "echoholomap:minimap");
        requireInputSummaryTarget(latestTargets, "main_menu", "menu.quit",
                "echo-native-loader", "echo-native-loader:main_menu");
        Object latestTargetsBySourceObject = summary.get("latestHandledTargetByMetadataSource");
        require(latestTargetsBySourceObject instanceof Map<?, ?>,
                "Native input dispatch summary must include latest handled targets by metadata source.");
        Map<?, ?> latestTargetsBySource = (Map<?, ?>) latestTargetsBySourceObject;
        requireInputSummaryTarget(latestTargetsBySource,
                "agent2_direct_public_sdk_input_probe:main_menu",
                "main_menu",
                "menu.quit",
                "echo-native-loader",
                "echo-native-loader:main_menu");
    }

    private static void requireInputSummaryTarget(
            Map<?, ?> latestTargets,
            String surfaceType,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        Object targetObject = latestTargets.get(surfaceType);
        require(targetObject instanceof Map<?, ?> target
                        && surfaceType.equals(target.get("surfaceType"))
                        && actionId.equals(target.get("actionId"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(target.get("targetStatus"))
                        && moduleId.equals(target.get("routeModuleId"))
                        && surfaceId.equals(target.get("routeSurfaceId"))
                        && Boolean.TRUE.equals(target.get("routeTrustedMutation"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(target.get("routeStatus")),
                "Native input dispatch summary must expose latest route-owned target for "
                        + surfaceType + "/" + actionId + ".");
    }

    private static void requireInputSummaryTarget(
            Map<?, ?> latestTargets,
            String key,
            String surfaceType,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        Object targetObject = latestTargets.get(key);
        require(targetObject instanceof Map<?, ?> target
                        && surfaceType.equals(target.get("surfaceType"))
                        && actionId.equals(target.get("actionId"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(target.get("targetStatus"))
                        && moduleId.equals(target.get("routeModuleId"))
                        && surfaceId.equals(target.get("routeSurfaceId"))
                        && Boolean.TRUE.equals(target.get("routeTrustedMutation"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(target.get("routeStatus")),
                "Native input dispatch summary must expose latest route-owned target for " + key + ".");
    }

    private static void requireInputMetadata(Object metadataObject, String scope) {
        if (!(metadataObject instanceof Map<?, ?>)) {
            throw new IllegalStateException("Native input dispatch " + scope + " must preserve input metadata.");
        }
        Map<?, ?> metadata = (Map<?, ?>) metadataObject;
        require("native_loader_window_pump".equals(metadata.get("inputSource"))
                        && "input_binding".equals(metadata.get("service"))
                        && "index".equals(metadata.get("focusedSurface"))
                        && Integer.valueOf(19).equals(metadata.get("scanCode"))
                        && Integer.valueOf(2).equals(metadata.get("modifiers"))
                        && metadata.containsKey("screenClass"),
                "Native input dispatch " + scope + " must carry window key-event metadata.");
    }

    private static boolean inputMetadataPresent(Object metadataObject) {
        return metadataObject instanceof Map<?, ?> metadata
                && "native_loader_window_pump".equals(metadata.get("inputSource"));
    }

    private static Map<String, Boolean> requireDirectPublicSdkInputDispatchEvidence(
            Map<String, Object> inputDispatchEvidence
    ) {
        Object eventsObject = inputDispatchEvidence.get("events");
        require(eventsObject instanceof List<?>,
                "Native input dispatch evidence must include direct public SDK input events.");
        List<?> events = (List<?>) eventsObject;
        Map<String, Boolean> results = new LinkedHashMap<>();
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoterminal.open", "terminal", "terminal.open");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoindex.catalog", "index", "index.catalog");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoindex.recipe", "index", "index.recipe");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoindex.usage", "index", "index.usage");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoindex.bookmark", "index", "index.bookmark");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "echolens.key.deep_scan", "lens", "lens.deep_scan");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echolens.index_recipe", "lens", "lens.index_recipe");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echolens.index_usage", "lens", "lens.index_usage");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echolens.track_in_index", "lens", "lens.track_in_index");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoholomap.open_map", "holomap", "holomap.open");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoholomap.toggle_minimap", "holomap", "holomap.toggle_minimap");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoholomap.minimap_zoom_in", "holomap", "holomap.zoom_in");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoholomap.minimap_zoom_out", "holomap", "holomap.zoom_out");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echoholomap.minimap_cycle_corner", "holomap", "holomap.cycle_corner");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echo.native.menu", "main_menu", "menu.open");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echo.native.menu.new_run", "main_menu", "menu.new_run");
        requireDirectPublicSdkInputDispatchEvent(
                results, events, "key.echo.native.menu.quit", "main_menu", "menu.quit");
        return Map.copyOf(results);
    }

    private static void requireDirectPublicSdkInputDispatchEvent(
            Map<String, Boolean> results,
            List<?> events,
            String keyMapping,
            String surfaceType,
            String actionId
    ) {
        boolean found = false;
        for (Object eventObject : events) {
            if (!(eventObject instanceof Map<?, ?> event)
                    || !keyMapping.equals(event.get("keyMapping"))
                    || !EchoNativeLoadStatus.MUTATED.name().equals(event.get("status"))
                    || !Boolean.TRUE.equals(event.get("handled"))
                    || !(event.get("metadata") instanceof Map<?, ?> metadata)
                    || !"agent2_direct_public_sdk_input_probe".equals(metadata.get("source"))
                    || !"direct_input_binding_dispatch".equals(metadata.get("eventType"))
                    || !Boolean.FALSE.equals(metadata.get("neoForgeEventOwnershipRequired"))
                    || !(event.get("targets") instanceof List<?> targets)) {
                continue;
            }
            for (Object targetObject : targets) {
                if (targetObject instanceof Map<?, ?> targetEvent
                        && surfaceType.equals(targetEvent.get("surfaceType"))
                        && actionId.equals(targetEvent.get("actionId"))
                        && Boolean.TRUE.equals(targetEvent.get("handled"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(targetEvent.get("status"))
                        && targetEvent.get("metadata") instanceof Map<?, ?> targetMetadata
                        && "agent2_direct_public_sdk_input_probe".equals(targetMetadata.get("source"))
                        && "direct_input_binding_dispatch".equals(targetMetadata.get("eventType"))) {
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        require(found, "Direct public SDK input dispatch must route " + keyMapping
                + " to " + surfaceType + "/" + actionId + " without NeoForge event ownership.");
        results.put(surfaceType + ":" + actionId, true);
    }

    private static Map<String, Boolean> requireDirectPublicSdkLifecycleEvidence(
            Map<String, List<EchoNativeClientRouteRegistry.NativeClientSurfaceLifecycleEvent>> lifecycleEvents
    ) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        requireDirectPublicSdkLifecycleEvent(results, lifecycleEvents, "terminal", "open", "terminal.open");
        requireDirectPublicSdkLifecycleEvent(results, lifecycleEvents, "index", "open", "index.catalog");
        requireDirectPublicSdkLifecycleEvent(results, lifecycleEvents, "client_overlay", "render", "lens.overlay.render");
        requireDirectPublicSdkLifecycleEvent(results, lifecycleEvents, "holomap", "render", "holomap.minimap.render");
        requireDirectPublicSdkLifecycleEvent(results, lifecycleEvents, "hud", "render", "hud.render");
        requireDirectPublicSdkLifecycleEvent(results, lifecycleEvents, "main_menu", "open", "menu.open");
        requireDirectPublicSdkLifecycleEvent(results, lifecycleEvents, "loading_screen", "render", "loading.render");
        return Map.copyOf(results);
    }

    private static void requireDirectPublicSdkLifecycleEvent(
            Map<String, Boolean> results,
            Map<String, List<EchoNativeClientRouteRegistry.NativeClientSurfaceLifecycleEvent>> lifecycleEvents,
            String surfaceType,
            String phase,
            String actionId
    ) {
        boolean found = false;
        for (EchoNativeClientRouteRegistry.NativeClientSurfaceLifecycleEvent event
                : lifecycleEvents.getOrDefault(surfaceType, List.of())) {
            if (!phase.equals(event.phase()) || !actionId.equals(event.actionId())) {
                continue;
            }
            Map<String, Object> metadata = event.metadata();
            if ("agent2_direct_public_sdk_lifecycle_probe".equals(metadata.get("source"))
                    && "direct_lifecycle_publication".equals(metadata.get("eventType"))
                    && Boolean.FALSE.equals(metadata.get("neoForgeEventOwnershipRequired"))) {
                found = true;
                break;
            }
        }
        require(found, "Direct public SDK lifecycle publication must record "
                + surfaceType + "/" + phase + "/" + actionId + " without NeoForge event ownership.");
        results.put(surfaceType + ":" + actionId, true);
    }

    private static Map<String, Boolean> requireMutationOnlyBooleanDispatch() {
        NativeLoaderClientUiHost registeredOnlyHost = new NativeLoaderClientUiHost();
        registeredOnlyHost.attachLiveBridge(new NativeLoaderLiveClientBridge() {
            @Override
            public String bridgeId() {
                return "agent2:registered_only_bridge";
            }

            @Override
            public EchoNativeLoadStatus dispatchRoute(String surfaceType, String actionId, Map<String, Object> metadata) {
                return EchoNativeLoadStatus.REGISTERED;
            }

            @Override
            public EchoNativeLoadStatus dispatchInputBinding(
                    String keyMapping,
                    int keyCode,
                    String inputType,
                    Map<String, Object> metadata
            ) {
                return EchoNativeLoadStatus.REGISTERED;
            }
        });
        EchoNativeLoadStatus routeStatus = registeredOnlyHost.dispatchRouteStatus(
                "terminal",
                "terminal.open",
                Map.of("source", "agent2_registered_only_status_probe"));
        boolean routeBoolean = registeredOnlyHost.dispatchRoute(
                "terminal",
                "terminal.open",
                Map.of("source", "agent2_registered_only_boolean_probe"));
        EchoNativeLoadStatus inputStatus = registeredOnlyHost.dispatchInputBindingStatus(
                "key.echoterminal.open",
                77,
                "press");
        boolean inputBoolean = registeredOnlyHost.dispatchInputBinding(
                "key.echoterminal.open",
                77,
                "press");
        require(routeStatus == EchoNativeLoadStatus.REGISTERED,
                "Registered-only route status probe must preserve REGISTERED status.");
        require(!routeBoolean,
                "Boolean route dispatch must not treat REGISTERED as live Native Loader mutation.");
        require(inputStatus == EchoNativeLoadStatus.REGISTERED,
                "Registered-only input status probe must preserve REGISTERED status.");
        require(!inputBoolean,
                "Boolean input dispatch must not treat REGISTERED as live Native Loader mutation.");

        NativeLoaderClientUiHost registeredOnlySurfaceHost = new NativeLoaderClientUiHost();
        registeredOnlySurfaceHost.attachLiveBridge(new NativeLoaderLiveClientBridge() {
            @Override
            public boolean attached() {
                return true;
            }

            @Override
            public String bridgeId() {
                return "agent2:registered_only_surface_bridge";
            }

            @Override
            public boolean firstClassNativeClientRouteTable() {
                return true;
            }

            @Override
            public boolean nativeClientRouteProcess() {
                return true;
            }

            @Override
            public boolean releaseClientRouteTrusted() {
                return true;
            }

            @Override
            public boolean clientRouteMutationSupported() {
                return true;
            }

            @Override
            public EchoNativeLoadStatus registerSurface(
                    String moduleId,
                    String surfaceId,
                    String surfaceType,
                    Map<String, Object> config
            ) {
                return EchoNativeLoadStatus.REGISTERED;
            }
        });
        EchoNativeLoadStatus registeredSurfaceStatus = registeredOnlySurfaceHost.registerSurfaceStatus(
                "agent2",
                "registered_only_surface",
                "terminal",
                Map.of("source", "agent2_registered_only_surface_probe"));
        Map<String, Map<String, Object>> registeredSurfaces = registeredOnlySurfaceHost.surfaces();
        Map<String, Object> registeredSurface = registeredSurfaces.getOrDefault(
                "agent2:registered_only_surface",
                Map.of());
        require(registeredSurfaceStatus == EchoNativeLoadStatus.REGISTERED,
                "Registered-only surface bridge must preserve REGISTERED status.");
        require(!Boolean.TRUE.equals(registeredSurface.get("nativeClientRouteTableMutated"))
                        && !Boolean.TRUE.equals(registeredSurface.get("liveClientBridgeMutated")),
                "Registered-only surface bridge must not be promoted to live Native Loader mutation.");
        require(EchoNativeLoadStatus.REGISTERED.name().equals(registeredSurface.get("status")),
                "Registered-only surface bridge must remain REGISTERED in host surface evidence.");
        Map<String, Boolean> results = new LinkedHashMap<>();
        results.put("registeredRouteStatusPreserved", true);
        results.put("registeredRouteBooleanRejected", true);
        results.put("registeredInputStatusPreserved", true);
        results.put("registeredInputBooleanRejected", true);
        results.put("registeredSurfaceStatusPreserved", true);
        results.put("registeredSurfaceMutationRejected", true);
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireStatusAwareInputAdapters() throws Exception {
        Map<String, Path> sources = Map.of(
                "echoterminal",
                addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                        "echoterminal", "EchoTerminalClient.java"),
                "echoindex",
                addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                        "echoindex", "EchoIndexClient.java"),
                "echolens",
                addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                        "echolens", "EchoLensClient.java"),
                "echoholomap",
                addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                        "echoholomap", "EchoHoloMapClient.java")
        );
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (Map.Entry<String, Path> sourceEntry : sources.entrySet()) {
            String source = Files.readString(sourceEntry.getValue(), StandardCharsets.UTF_8);
            boolean statusAware = source.contains(".keyInput(")
                    && !source.contains(".dispatchInputBinding(");
            require(statusAware,
                    sourceEntry.getKey() + " Native Loader key adapter must dispatch through the Native Loader key input host service.");
            results.put(sourceEntry.getKey(), true);
        }
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireMetadataAwareInputAdapters() throws Exception {
        Map<String, Path> sources = Map.of(
                "echoterminal",
                addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                        "echoterminal", "EchoTerminalClient.java"),
                "echoindex",
                addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                        "echoindex", "EchoIndexClient.java"),
                "echolens",
                addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                        "echolens", "EchoLensClient.java"),
                "echoholomap",
                addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                        "echoholomap", "EchoHoloMapClient.java")
        );
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (Map.Entry<String, Path> sourceEntry : sources.entrySet()) {
            String source = Files.readString(sourceEntry.getValue(), StandardCharsets.UTF_8);
            boolean metadataAware = source.contains("\"source\", \"native_loader_input_binding\"")
                    && source.contains("\"forwardedFrom\"")
                    && source.contains("\"eventType\", \"key_input\"")
                    && source.contains("\"nativeInputOwner\", \"EchoNativeClientRouteRegistries\"")
                    && source.contains("\"nativeLoaderHostService\", \"key_input\"")
                    && source.contains("\"inputType\", \"press\"")
                    && source.contains("\"keyEvent\", String.valueOf(")
                    && source.contains(".keyInput(")
                    && source.contains("nativeKeyMetadata(event,")
                    && !source.contains("\"compatibilityAdapter\", \"NeoForge InputEvent.Key\"");
            require(metadataAware,
                    sourceEntry.getKey()
                            + " Native Loader key adapter must use route-owned input binding metadata and record a bridge origin.");
            results.put(sourceEntry.getKey(), true);
        }
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireStatusAwareInputDispatchTargets() throws Exception {
        Path sourcePath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderClientRouteTable.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        Path contractPath = Path.of("echo-native-contracts", "src", "main", "java", "dev", "echo",
                "nativeplatform", "contracts", "EchoNativeClientRouteRegistry.java");
        String contractSource = Files.readString(contractPath, StandardCharsets.UTF_8);
        Path providerPath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo",
                "nativeplatform", "loader", "NativeLoaderClientRouteRegistryProvider.java");
        String providerSource = Files.readString(providerPath, StandardCharsets.UTF_8);
        Path uiHostPath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo",
                "nativeplatform", "loader", "NativeLoaderClientUiHost.java");
        String uiHostSource = Files.readString(uiHostPath, StandardCharsets.UTF_8);
        boolean statusAware = source.contains("EchoNativeLoadStatus targetStatus = dispatchStatus(")
                && source.contains("\"status\", targetStatus.name()")
                && source.contains("event.put(\"status\", safeStatus.name())")
                && source.contains("event.put(\"metadata\", metadata == null ? Map.of() : Map.copyOf(metadata))")
                && source.contains("inputDispatchMetadata(")
                && contractSource.contains("dispatchInputBindingStatus(")
                && contractSource.contains("keyInput(")
                && contractSource.contains("renderGuiLayer(")
                && contractSource.contains("renderHudLayer(")
                && contractSource.contains("tickRoute(")
                && contractSource.contains("overlayInput(")
                && contractSource.contains("mouseInput(")
                && contractSource.contains("focusOverlay(")
                && contractSource.contains("Map<String, Object> metadata")
                && contractSource.contains("openSurface(String surfaceType, String actionId, Map<String, Object> metadata)")
                && contractSource.contains("screenLifecycle(")
                && providerSource.contains("SDK_UI_HOST.dispatchInputBindingStatus(keyMapping, keyCode, inputType, metadata)")
                && providerSource.contains("SDK_UI_HOST.dispatchRouteStatus(surfaceType, actionId, metadata)")
                && providerSource.contains("SDK_UI_HOST.keyInput(keyMapping, keyCode, inputType, metadata)")
                && providerSource.contains("SDK_UI_HOST.renderGuiLayer(surfaceType, actionId, metadata)")
                && providerSource.contains("SDK_UI_HOST.renderHudLayer(surfaceType, actionId, metadata)")
                && providerSource.contains("SDK_UI_HOST.tickRoute(surfaceType, actionId, phase, metadata)")
                && providerSource.contains("SDK_UI_HOST.overlayInput(surfaceType, actionId, metadata)")
                && providerSource.contains("SDK_UI_HOST.mouseInput(surfaceType, actionId, metadata)")
                && providerSource.contains("SDK_UI_HOST.focusOverlay(surfaceType, focused, metadata)")
                && providerSource.contains("SDK_UI_HOST.openSurface(surfaceType, actionId, metadata)")
                && providerSource.contains("SDK_UI_HOST.screenLifecycleEvent(surfaceType, phase, actionId, metadata)")
                && uiHostSource.contains("public EchoNativeLoadStatus screenLifecycleEvent(")
                && uiHostSource.contains("recordHostService(\"screen_lifecycle\"")
                && source.contains("public static EchoNativeLoadStatus screenLifecycle(")
                && !source.contains("boolean handled = dispatch(target.surfaceType(), target.actionId(), metadata)");
        require(statusAware,
                "Native Loader input binding and public screen lifecycle targets must preserve route dispatch status instead of using boolean dispatch.");
        return Map.of("native_loader_route_table_input_targets", true);
    }

    private static Map<String, Boolean> requireStatusAwareRouteAdapters() throws Exception {
        Map<String, Path> sources = Map.of(
                "echoterminal",
                addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                        "echoterminal", "EchoTerminalClient.java"),
                "echoterminal_screen_events",
                addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                        "echoterminal", "client", "TerminalEventHandler.java"),
                "echoterminal_rendercore",
                addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                        "echoterminal", "integration", "TerminalRenderCoreClientIntegration.java"),
                "echoindex",
                addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                        "echoindex", "EchoIndexClient.java"),
                "echolens",
                addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                        "echolens", "EchoLensClient.java"),
                "echoholomap",
                addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                        "echoholomap", "EchoHoloMapClient.java"),
                "echohudcore",
                addonSourcePath("echohudcore", "src", "main", "java", "com", "knoxhack",
                        "echo", "hudcore", "EchoHudCoreClient.java")
        );
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (Map.Entry<String, Path> sourceEntry : sources.entrySet()) {
            String source = Files.readString(sourceEntry.getValue(), StandardCharsets.UTF_8);
            boolean statusAware = (source.contains(".dispatchStatus(")
                    || source.contains(".keyInput(")
                    || source.contains(".overlayInput(")
                    || source.contains(".mouseInput(")
                    || source.contains(".tickRoute(")
                    || source.contains(".renderGuiLayer(")
                    || source.contains(".renderHudLayer("))
                    && !source.contains(".dispatch(");
            require(statusAware,
                    sourceEntry.getKey() + " Native Loader route adapter must use status-returning route or host-service dispatch.");
            results.put(sourceEntry.getKey(), true);
        }
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireSharedRouteHandlerSurfaceIds() throws Exception {
        Path holoMapPath = addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                "echoholomap", "EchoHoloMapClient.java");
        String holoMapSource = Files.readString(holoMapPath, StandardCharsets.UTF_8);
        require(holoMapSource.contains("registry.registerActionHandler(\"holomap\", \"echoholomap:minimap\"")
                        && holoMapSource.contains("registry.registerActionHandler(\"holomap\", \"echoholomap:fullscreen_map\"")
                        && holoMapSource.contains("dispatchNativeRouteSurface(\"echoholomap:minimap\", context)")
                        && holoMapSource.contains("dispatchNativeRouteSurface(\"echoholomap:fullscreen_map\", context)")
                        && !holoMapSource.contains("registry.registerActionHandler(\"holomap\", \"echoholomap:client\""),
                "HoloMap shared holomap route handlers must be registered with declared minimap/fullscreen surface IDs.");

        Path hudCorePath = addonSourcePath("echohudcore", "src", "main", "java", "com", "knoxhack",
                "echo", "hudcore", "EchoHudCoreClient.java");
        String hudCoreSource = Files.readString(hudCorePath, StandardCharsets.UTF_8);
        require(hudCoreSource.contains("registry.registerActionHandler(\"hud_widget\", \"echohudcore:mission_tracker\"")
                        && hudCoreSource.contains("registry.registerActionHandler(\"hud_widget\", \"echohudcore:hazard_readout\"")
                        && hudCoreSource.contains("registry.registerActionHandler(\"hud_widget\", \"echohudcore:compass_indicator\"")
                        && hudCoreSource.contains("registry.registerActionHandler(\"hud_layout\", \"echohudcore:screen_safe_area\"")
                        && hudCoreSource.contains("dispatchNativeRouteSurface(\"echohudcore:mission_tracker\", context)")
                        && hudCoreSource.contains("dispatchNativeRouteSurface(\"echohudcore:screen_safe_area\", context)")
                        && !hudCoreSource.contains("registry.registerActionHandler(\"hud_widget\", \"echohudcore:hud_widgets\"")
                        && !hudCoreSource.contains("registry.registerActionHandler(\"hud_layout\", \"echohudcore:hud_layout\""),
                "HUDCore shared widget/layout handlers must be registered with declared HUD surface IDs.");

        return Map.of(
                "echoholomap_shared_route_handlers", true,
                "echohudcore_shared_route_handlers", true
        );
    }

    private static Map<String, Boolean> requireRouteTableOwnerPreferredHandlers() throws Exception {
        Path routeTablePath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderClientRouteTable.java");
        String source = Files.readString(routeTablePath, StandardCharsets.UTF_8);
        require(source.contains("NativeClientRouteHandlerSelection handlerSelection = selectHandlersForRoute(handlers, route)")
                        && source.contains("handlerIdMatchesRoute(handler.handlerId(), routeSurfaceId)")
                        && source.contains("event.put(\"ownerPreferredHandlers\", safeSelection.ownerPreferred())")
                        && source.contains("event.put(\"ownerHandlerIds\", safeSelection.ownerHandlerIds())")
                        && source.contains("event.put(\"handlerDispatchOrder\", safeSelection.orderedHandlerIds())"),
                "Native route table must prefer handlers registered to the action owner's route surface.");
        return Map.of("native_route_table_owner_preferred_handlers", true);
    }

    private static Map<String, Boolean> requireNativeCrossSurfaceRouteHandoffs() throws Exception {
        Path lensPath = addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                "echolens", "EchoLensClient.java");
        String lensSource = Files.readString(lensPath, StandardCharsets.UTF_8);
        String lensTargetDispatch = methodBody(lensSource,
                "private static boolean dispatchTargetIndexAction(String mode)");
        require(lensTargetDispatch.contains("dispatchNativeIndexTargetRoute(\"index.open_recipes_for_item\", \"recipes\")")
                        && lensTargetDispatch.contains("dispatchNativeIndexTargetRoute(\"index.open_usages_for_item\", \"usages\")")
                        && lensTargetDispatch.contains("dispatchNativeIndexTargetRoute(\"index.track_item\", \"track\")")
                        && !lensTargetDispatch.contains("LensClientActions.openIndexRecipes(")
                        && !lensTargetDispatch.contains("LensClientActions.openIndexUses(")
                        && !lensTargetDispatch.contains("LensClientActions.trackInIndex("),
                "Lens native target Index actions must hand off through Index native routes, not direct screen helpers.");
        String lensRouteHandoff = methodBody(lensSource,
                "private static boolean dispatchNativeIndexTargetRoute(String actionId, String mode)");
        require(lensRouteHandoff.contains("metadata.put(\"source\", \"echolens_native_route\")")
                        && lensRouteHandoff.contains("metadata.put(\"itemId\", itemId.toString())")
                        && lensRouteHandoff.contains("metadata.put(\"upstreamSurfaceType\", \"lens\")")
                        && lensRouteHandoff.contains(".dispatchStatus(")
                        && lensRouteHandoff.contains("\"client_overlay\""),
                "Lens native Index handoff must dispatch client_overlay route metadata with target item evidence.");
        Path lensActionsPath = addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                "echolens", "client", "LensClientActions.java");
        String lensActionsSource = Files.readString(lensActionsPath, StandardCharsets.UTF_8);
        String legacyHandoff = methodBody(lensActionsSource,
                "private static boolean dispatchNativeIndexRoute(ItemStack stack, String actionId, String mode)");
        require(legacyHandoff.contains("EchoNativeClientRuntimeEnvironment.isNativeLoaderActive()")
                        && legacyHandoff.contains("metadata.put(\"source\", \"echolens_legacy_client_action_adapter\")")
                        && legacyHandoff.contains("metadata.put(\"itemId\", itemId.toString())")
                        && legacyHandoff.contains("EchoNativeClientRouteRegistries.get().dispatchStatus(")
                        && legacyHandoff.contains("\"client_overlay\"")
                        && methodBody(lensActionsSource, "public static void openIndexRecipes(ItemStack stack)")
                                .contains("dispatchNativeIndexRoute(stack, \"index.open_recipes_for_item\", \"recipes\")")
                        && methodBody(lensActionsSource, "public static void openIndexUses(ItemStack stack)")
                                .contains("dispatchNativeIndexRoute(stack, \"index.open_usages_for_item\", \"usages\")")
                        && methodBody(lensActionsSource, "public static void trackInIndex(ItemStack stack)")
                                .contains("dispatchNativeIndexRoute(stack, \"index.track_item\", \"track\")"),
                "Lens legacy Index helpers must hand off to Index-owned native routes before reflective screen/network fallback while Native Loader is active.");

        Path indexPath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "EchoIndexClient.java");
        String indexSource = Files.readString(indexPath, StandardCharsets.UTF_8);
        String routeItemOpen = methodBody(indexSource,
                "private static boolean openRouteItemIndexRecipe(Map<String, Object> metadata, boolean recipes)");
        String routeItemTrack = methodBody(indexSource,
                "private static boolean trackRouteItemInIndex(Map<String, Object> metadata)");
        String routeItemId = methodBody(indexSource,
                "private static Identifier routeItemId(Map<String, Object> metadata)");
        require(routeItemOpen.contains("ItemStack stack = routeItemStack(metadata)")
                        && routeItemOpen.contains("return openItemIndexRecipe(stack, recipes)")
                        && routeItemTrack.contains("Identifier itemId = routeItemId(metadata)")
                        && routeItemTrack.contains("new IndexActionPacket(IndexActionPacket.Action.BOOKMARK, itemId)")
                        && routeItemId.contains("metadata == null ? \"\" : metadata.get(\"itemId\")")
                        && routeItemId.contains("BuiltInRegistries.ITEM.containsKey(itemId)"),
                "Index native route recipe opens must resolve route metadata itemId into a live recipe screen stack.");
        String indexRouteDispatch = methodBody(indexSource,
                "private static boolean dispatchNativeClientRoute(NativeClientRouteActionContext context)");
        int metadataOpen = indexRouteDispatch.indexOf("openRouteItemIndexRecipe(context.metadata(), recipes)");
        int heldOpen = indexRouteDispatch.indexOf("openHeldItemIndexRecipe(recipes)");
        require(metadataOpen >= 0 && heldOpen > metadataOpen,
                "Index item_recipe route must prefer route metadata before held-item fallback.");
        require(indexRouteDispatch.contains("openRouteItemIndexRecipe(context.metadata(), true)")
                        && indexRouteDispatch.contains("openRouteItemIndexRecipe(context.metadata(), false)")
                        && indexRouteDispatch.contains("\"track\".equals(recipeMode)")
                        && indexRouteDispatch.contains("trackRouteItemInIndex(context.metadata())"),
                "Index native recipe/usage route actions must support route metadata item targets.");

        return Map.of(
                "echolens_index_target_route_handoff", true,
                "echolens_legacy_index_action_native_handoff", true,
                "echoindex_route_item_recipe_metadata", true
        );
    }

    private static Map<String, Boolean> requireNativeScreenLifecycleTransitions() throws Exception {
        Path indexClientPath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "EchoIndexClient.java");
        String indexClient = Files.readString(indexClientPath, StandardCharsets.UTF_8);
        String lifecyclePublisher = methodBody(indexClient,
                "public static EchoNativeLoadStatus publishNativeScreenLifecycle(");
        require(lifecyclePublisher.contains("native_index_screen_transition")
                        && lifecyclePublisher.contains("EchoNativeClientRouteRegistries.get().openSurface(")
                        && lifecyclePublisher.contains("EchoNativeClientRouteRegistries.get().closeSurface(")
                        && lifecyclePublisher.contains("EchoNativeClientRouteRegistries.get().screenLifecycle(")
                        && lifecyclePublisher.contains("\"index\"")
                        && lifecyclePublisher.contains("screenTransitionPhase")
                        && lifecyclePublisher.contains("nativeLoaderUiHostService")
                        && lifecyclePublisher.contains("screen_lifecycle")
                        && lifecyclePublisher.contains("nativeLoaderScreenLifecycleHandoff"),
                "Index native screen transitions must publish route lifecycle evidence and Native Loader UI-host handoff metadata through the public registry.");
        String indexCatalogOpen = methodBody(indexClient, "private static boolean openIndexCatalog()");
        String indexRecipeOpen = methodBody(indexClient, "private static boolean openItemIndexRecipe(ItemStack stack, boolean recipes)");
        require(indexCatalogOpen.contains("EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(")
                        && indexCatalogOpen.contains("\"index.catalog\"")
                        && indexCatalogOpen.contains("index_route_catalog_fallback")
                        && indexCatalogOpen.contains("if (nativeLoaderActive() && lifecycleStatus != EchoNativeLoadStatus.MUTATED)")
                        && indexRecipeOpen.contains("EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(")
                        && indexRecipeOpen.contains("index_route_item_recipe")
                        && indexRecipeOpen.contains("IndexRecipeScreen.class.getName()")
                        && indexRecipeOpen.contains("if (nativeLoaderActive() && lifecycleStatus != EchoNativeLoadStatus.MUTATED)"),
                "Index route-owned catalog and recipe fallbacks must require mutating lifecycle evidence before setScreen.");

        Path catalogPath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "client", "IndexCatalogScreen.java");
        String catalogSource = Files.readString(catalogPath, StandardCharsets.UTF_8);
        boolean catalogInputNoSuperFallback = nativeLoaderBranchHasNoSuperFallback(catalogSource,
                List.of(
                        "public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)",
                        "public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)",
                        "public boolean keyPressed(KeyEvent event)",
                        "public boolean charTyped(CharacterEvent event)"));
        require(catalogSource.contains("index.catalog_screen.open_diagnostics")
                        && catalogSource.contains("index.catalog_screen.close")
                        && catalogSource.contains("index.catalog_screen.open_recipe")
                        && catalogSource.contains("closeNativeIndexScreen(")
                        && catalogSource.contains("EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(")
                        && catalogSource.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && catalogSource.contains("targetScreenClass\", IndexDiagnosticsScreen.class.getName()")
                        && catalogSource.contains("targetScreenClass\", IndexRecipeScreen.class.getName()"),
                "Index catalog native screen transitions must require mutating lifecycle evidence before setScreen.");

        Path indexTerminalIntegrationPath = addonSourcePath("echoindex", "src", "main", "java", "com",
                "knoxhack", "echoindex", "integration", "IndexTerminalClientIntegration.java");
        String indexTerminalIntegration = Files.readString(indexTerminalIntegrationPath, StandardCharsets.UTF_8);
        require(indexTerminalIntegration.contains("index.terminal_archive.open_diagnostics")
                        && indexTerminalIntegration.contains("EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(")
                        && indexTerminalIntegration.contains("targetScreenClass\", IndexDiagnosticsScreen.class.getName()")
                        && indexTerminalIntegration.contains("transitionSource\", \"index_terminal_archive\"")
                        && indexTerminalIntegration.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED"),
                "Index Terminal archive diagnostics open must require mutating lifecycle evidence before setScreen.");

        Path recipePath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "client", "IndexRecipeScreen.java");
        String recipeSource = Files.readString(recipePath, StandardCharsets.UTF_8);
        boolean recipeInputNoSuperFallback = nativeLoaderBranchHasNoSuperFallback(recipeSource,
                List.of(
                        "public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)",
                        "public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)",
                        "public boolean keyPressed(KeyEvent event)",
                        "public boolean charTyped(CharacterEvent event)"));
        require(recipeSource.contains("publishNativeRecipeScreenOpen(")
                        && recipeSource.contains("index.recipe_screen.open_recipe")
                        && recipeSource.contains("recipe_trace_root_button")
                        && recipeSource.contains("recipe_slot_navigation")
                        && recipeSource.contains("private EchoNativeLoadStatus publishNativeRecipeScreenOpen(")
                        && recipeSource.contains("return EchoIndexClient.publishNativeScreenLifecycle(")
                        && recipeSource.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && recipeSource.contains("targetScreenClass\", IndexRecipeScreen.class.getName()"),
                "Index recipe native screen transitions must require mutating lifecycle evidence before setScreen.");

        Path indexHotkeysPath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "client", "IndexHotkeys.java");
        String indexHotkeys = Files.readString(indexHotkeysPath, StandardCharsets.UTF_8);
        String hoveredStackOpen = methodBody(indexHotkeys,
                "private static boolean openNativeHoveredStack(Screen screen, IndexRecipeScreen.Mode mode)");
        require(hoveredStackOpen.contains("EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(")
                        && hoveredStackOpen.contains("index.hotkey_key_pressed")
                        && hoveredStackOpen.contains("index_hotkey_hovered_stack")
                        && hoveredStackOpen.contains("itemId")
                        && hoveredStackOpen.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED"),
                "Index hovered-stack hotkey route must require mutating lifecycle evidence before opening the recipe screen.");

        Path indexOverlayPath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "client", "IndexOverlay.java");
        String indexOverlay = Files.readString(indexOverlayPath, StandardCharsets.UTF_8);
        String overlayRecipeOpen = methodBody(indexOverlay,
                "private static void openRecipeScreen(ItemStack stack, IndexRecipeScreen.Mode mode, String transitionSource)");
        String overlayDiagnosticsOpen = methodBody(indexOverlay,
                "private static void openDiagnosticsScreen(String transitionSource)");
        require(overlayRecipeOpen.contains("EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(")
                        && overlayRecipeOpen.contains("index.inventory_overlay.open_recipe")
                        && overlayRecipeOpen.contains("IndexRecipeScreen.class.getName()")
                        && overlayRecipeOpen.contains("itemId")
                        && overlayRecipeOpen.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && indexOverlay.contains("index_overlay_recipe_key")
                        && indexOverlay.contains("index_overlay_usage_key")
                        && indexOverlay.contains("index_overlay_detail_open")
                        && overlayDiagnosticsOpen.contains("EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(")
                        && overlayDiagnosticsOpen.contains("index.inventory_overlay.open_diagnostics")
                        && overlayDiagnosticsOpen.contains("IndexDiagnosticsScreen.class.getName()")
                        && overlayDiagnosticsOpen.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && indexOverlay.contains("index_overlay_diagnostics_button"),
                "Index inventory overlay recipe and diagnostics opens must require mutating lifecycle evidence before setScreen.");

        Path indexFallbackPath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "client", "IndexFallbackScreen.java");
        String indexFallback = Files.readString(indexFallbackPath, StandardCharsets.UTF_8);
        String fallbackOpen = methodBody(indexFallback, "public static void open()");
        require(fallbackOpen.contains("EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(")
                        && fallbackOpen.contains("index.fallback_screen.open_catalog")
                        && fallbackOpen.contains("index_fallback_screen")
                        && fallbackOpen.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED"),
                "Index fallback catalog helper must require mutating lifecycle evidence before setScreen.");

        Path indexScreenCoreBridgePath = addonSourcePath("echoindex", "src", "main", "java", "com",
                "knoxhack", "echoindex", "client", "IndexScreenCoreBridge.java");
        String indexScreenCoreBridge = Files.readString(indexScreenCoreBridgePath, StandardCharsets.UTF_8);
        String indexScreenCorePublisher = methodBody(indexScreenCoreBridge,
                "private static EchoNativeLoadStatus publishOpenLifecycle(");
        require(indexScreenCorePublisher.contains("return EchoIndexClient.publishNativeScreenLifecycle(")
                        && indexScreenCorePublisher.contains("\"echoscreencore\"")
                        && indexScreenCorePublisher.contains("pageId")
                        && indexScreenCoreBridge.contains("EchoNativeLoadStatus lifecycleStatus = publishOpenLifecycle(")
                        && indexScreenCoreBridge.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && indexScreenCoreBridge.contains("index.screencore.open")
                        && indexScreenCoreBridge.contains("index.screencore.open_mode")
                        && indexScreenCoreBridge.contains("index.screencore.open_item")
                        && indexScreenCoreBridge.contains("index.screencore.open_recipe"),
                "Index ScreenCore route opens must require mutating lifecycle evidence before EchoScreens.open.");

        Path holoMapClientPath = addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                "echoholomap", "EchoHoloMapClient.java");
        String holoMapClient = Files.readString(holoMapClientPath, StandardCharsets.UTF_8);
        String holoMapLifecyclePublisher = methodBody(holoMapClient,
                "public static EchoNativeLoadStatus publishNativeScreenLifecycle(");
        require(holoMapLifecyclePublisher.contains("native_holomap_screen_transition")
                        && holoMapLifecyclePublisher.contains("EchoNativeClientRouteRegistries.get().openSurface(")
                        && holoMapLifecyclePublisher.contains("EchoNativeClientRouteRegistries.get().closeSurface(")
                        && holoMapLifecyclePublisher.contains("EchoNativeClientRouteRegistries.get().screenLifecycle(")
                        && holoMapLifecyclePublisher.contains("\"holomap\"")
                        && holoMapLifecyclePublisher.contains("screenTransitionPhase")
                        && holoMapLifecyclePublisher.contains("nativeLoaderUiHostService")
                        && holoMapLifecyclePublisher.contains("screen_lifecycle")
                        && holoMapLifecyclePublisher.contains("nativeLoaderScreenLifecycleHandoff"),
                "HoloMap native screen transitions must publish route lifecycle evidence and Native Loader UI-host handoff metadata through the public registry.");
        String holoMapOpen = methodBody(holoMapClient, "private static boolean openHoloMapScreen()");
        require(holoMapOpen.contains("EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(")
                        && holoMapOpen.contains("\"holomap.open\"")
                        && holoMapOpen.contains("HoloMapFullScreenMapScreen.class.getName()")
                        && holoMapOpen.contains("\"classic_fullscreen\"")
                        && holoMapOpen.contains("\"echoscreencore\"")
                        && holoMapOpen.contains("if (nativeLoaderActive() && lifecycleStatus != EchoNativeLoadStatus.MUTATED)"),
                "HoloMap route-owned fullscreen open must require mutating lifecycle evidence before classic setScreen.");
        String holoMapCommand = methodBody(holoMapClient,
                "private static boolean dispatchNativeFullscreenCommand(String command, Map<String, Object> metadata)");
        require(holoMapCommand.contains("holomap.fullscreen.close")
                        && holoMapCommand.contains("fullscreen_route_command")
                        && holoMapCommand.contains("EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(")
                        && holoMapCommand.contains("if (nativeLoaderActive() && lifecycleStatus != EchoNativeLoadStatus.MUTATED)"),
                "HoloMap route-owned fullscreen close command must require mutating lifecycle evidence before setScreen.");
        require(holoMapCommand.contains("case \"selectEntry\"")
                        && holoMapCommand.contains("if (id == null || !controller.selectEntry(id))")
                        && holoMapCommand.contains(".getMethod(\"invalidateData\")")
                        && !holoMapCommand.contains("return id != null && controller.selectEntry(id)"),
                "HoloMap select-entry route handler must own selection mutation and ScreenCore invalidation.");

        Path holoMapScreenPath = addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                "echoholomap", "client", "HoloMapFullScreenMapScreen.java");
        String holoMapScreen = Files.readString(holoMapScreenPath, StandardCharsets.UTF_8);
        boolean holoMapInputNoSuperFallback = nativeLoaderBranchHasNoSuperFallback(holoMapScreen,
                List.of(
                        "public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)",
                        "public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY)",
                        "public boolean mouseReleased(MouseButtonEvent event)",
                        "public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)",
                        "public boolean keyPressed(KeyEvent event)"));
        String holoMapKey = methodBody(holoMapScreen, "public boolean handleNativeRouteKey(int key)");
        require(holoMapKey.contains("EchoNativeLoadStatus lifecycleStatus = EchoHoloMapClient.publishNativeScreenLifecycle(")
                        && holoMapKey.contains("holomap.fullscreen.close")
                        && holoMapKey.contains("fullscreen_key")
                        && holoMapKey.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED"),
                "HoloMap fullscreen native key close must require mutating lifecycle evidence before setScreen.");
        String holoMapHitbox = methodBody(holoMapScreen, "private void handleHitbox(Hitbox hitbox)");
        require(holoMapHitbox.contains("EchoNativeLoadStatus lifecycleStatus = EchoHoloMapClient.publishNativeScreenLifecycle(")
                        && holoMapHitbox.contains("holomap.fullscreen.close")
                        && holoMapHitbox.contains("fullscreen_header_button")
                        && holoMapHitbox.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED"),
                "HoloMap fullscreen native header close must require mutating lifecycle evidence before setScreen.");
        Path holoMapScreenCorePath = addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                "echoholomap", "integration", "HoloMapScreenCoreIntegration.java");
        String holoMapScreenCore = Files.readString(holoMapScreenCorePath, StandardCharsets.UTF_8);
        require(holoMapScreenCore.contains("return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenCommand(")
                        && holoMapScreenCore.contains("\"holomap.select_entry\"")
                        && !holoMapScreenCore.contains("boolean selected = EchoHoloMapClient.dispatchNativeScreenCoreFullscreenCommand("),
                "HoloMap ScreenCore list selection must not locally invalidate after native route dispatch.");
        String holoMapScreenCoreOpen = methodBody(holoMapScreenCore, "public static boolean openFullscreen()");
        require(holoMapScreenCoreOpen.contains("EchoNativeLoadStatus lifecycleStatus = EchoHoloMapClient.publishNativeScreenLifecycle(")
                        && holoMapScreenCoreOpen.contains("holomap.screencore.open_fullscreen")
                        && holoMapScreenCoreOpen.contains("HoloMapScreenCoreIntegration.class.getName()")
                        && holoMapScreenCoreOpen.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && holoMapScreenCoreOpen.contains("EchoScreens.open("),
                "HoloMap ScreenCore fullscreen open must require mutating lifecycle evidence before EchoScreens.open.");

        Path terminalClientPath = addonSourcePath("echoterminal", "src", "main", "java", "com",
                "knoxhack", "echoterminal", "EchoTerminalClient.java");
        String terminalClient = Files.readString(terminalClientPath, StandardCharsets.UTF_8);
        String terminalLifecyclePublisher = methodBody(terminalClient,
                "public static EchoNativeLoadStatus publishNativeScreenLifecycle(");
        require(terminalLifecyclePublisher.contains("native_terminal_screen_transition")
                        && terminalLifecyclePublisher.contains("EchoNativeClientRouteRegistries.get().openSurface(")
                        && terminalLifecyclePublisher.contains("EchoNativeClientRouteRegistries.get().closeSurface(")
                        && terminalLifecyclePublisher.contains("EchoNativeClientRouteRegistries.get().screenLifecycle(")
                        && terminalLifecyclePublisher.contains("\"terminal\"")
                        && terminalLifecyclePublisher.contains("screenTransitionPhase")
                        && terminalLifecyclePublisher.contains("nativeLoaderUiHostService")
                        && terminalLifecyclePublisher.contains("screen_lifecycle")
                        && terminalLifecyclePublisher.contains("nativeLoaderScreenLifecycleHandoff"),
                "Terminal native screen transitions must publish route lifecycle evidence and Native Loader UI-host handoff metadata through the public registry.");
        String terminalOpen = methodBody(terminalClient, "private static boolean openTerminalScreen()");
        require(terminalOpen.contains("EchoNativeLoadStatus lifecycleStatus = publishNativeScreenLifecycle(")
                        && terminalOpen.contains("\"terminal.open\"")
                        && terminalOpen.contains("EchoTerminalScreen")
                        && terminalOpen.contains("terminal_route_open")
                        && terminalOpen.contains("if (nativeLoaderActive() && lifecycleStatus != EchoNativeLoadStatus.MUTATED)"),
                "Terminal route-owned classic screen open must require mutating lifecycle evidence before setScreen.");

        Path terminalClassicPath = addonSourcePath("echoterminal", "src", "main", "java", "com",
                "knoxhack", "echoterminal", "client", "screen", "EchoTerminalScreen.java");
        String terminalClassic = Files.readString(terminalClassicPath, StandardCharsets.UTF_8);
        String terminalClassicKey = methodBody(terminalClassic, "public boolean keyPressed(KeyEvent event)");
        require(terminalClassicKey.contains("EchoNativeLoadStatus lifecycleStatus = EchoTerminalClient.publishNativeScreenLifecycle(")
                        && terminalClassicKey.contains("terminal.screen.close")
                        && terminalClassicKey.contains("terminal_key")
                        && terminalClassicKey.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED"),
                "Terminal classic screen key close must require mutating lifecycle evidence before setScreen.");

        Path terminalScreenCorePath = addonSourcePath("echoterminal", "src", "main", "java", "com",
                "knoxhack", "echoterminal", "client", "screencore", "TerminalScreenCoreScreen.java");
        String terminalScreenCore = Files.readString(terminalScreenCorePath, StandardCharsets.UTF_8);
        boolean terminalScreenCoreInputNoSuperFallback = nativeLoaderBranchHasNoSuperFallback(terminalScreenCore,
                List.of(
                        "public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)",
                        "public boolean mouseReleased(MouseButtonEvent event)",
                        "public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY)",
                        "public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)",
                        "public boolean keyPressed(KeyEvent event)",
                        "public boolean charTyped(CharacterEvent event)"));
        String terminalLegacyOpen = methodBody(terminalScreenCore, "boolean openLegacyRenderer()");
        String terminalScreenCoreKey = methodBody(terminalScreenCore,
                "public boolean handleNativeRouteKey(int key, boolean openTerminalKey)");
        String terminalControlsClose = methodBody(terminalScreenCore, "public boolean close()");
        String terminalControlsBack = methodBody(terminalScreenCore, "public boolean back()");
        String terminalControlsOpen = methodBody(terminalScreenCore,
                "public boolean open(Identifier nextPage, EchoDataContext context)");
        Path terminalScreenCoreBridgePath = addonSourcePath("echoterminal", "src", "main", "java", "com",
                "knoxhack", "echoterminal", "client", "screencore", "TerminalScreenCoreBridge.java");
        String terminalScreenCoreBridge = Files.readString(terminalScreenCoreBridgePath, StandardCharsets.UTF_8);
        String terminalBridgeOpenTab = methodBody(terminalScreenCoreBridge,
                "public static boolean openTab(Identifier tabId)");
        require(terminalLegacyOpen.contains("EchoTerminalClient.publishNativeScreenLifecycle(")
                        && terminalLegacyOpen.contains("terminal.screencore.open_legacy_renderer")
                        && terminalLegacyOpen.contains("terminal_screencore_legacy_renderer")
                        && terminalLegacyOpen.contains("EchoNativeLoadStatus lifecycleStatus = EchoTerminalClient.publishNativeScreenLifecycle(")
                        && terminalLegacyOpen.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && terminalScreenCoreKey.contains("EchoNativeLoadStatus lifecycleStatus = EchoTerminalClient.publishNativeScreenLifecycle(")
                        && terminalScreenCoreKey.contains("terminal.screencore.close")
                        && terminalScreenCoreKey.contains("terminal_screencore_key")
                        && terminalControlsClose.contains("terminal_screencore_controls_close")
                        && terminalControlsClose.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && terminalControlsBack.contains("terminal_screencore_controls_back")
                        && terminalControlsBack.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && terminalControlsOpen.contains("terminal.screencore.open_page")
                        && terminalControlsOpen.contains("terminal_screencore_controls_open")
                        && terminalControlsOpen.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && terminalControlsOpen.contains("targetScreenClass\", TerminalScreenCoreScreen.class.getName()")
                        && terminalBridgeOpenTab.contains("EchoNativeLoadStatus lifecycleStatus = EchoTerminalClient.publishNativeScreenLifecycle(")
                        && terminalBridgeOpenTab.contains("terminal.screencore.open_tab")
                        && terminalBridgeOpenTab.contains("terminal_screencore_bridge_open_tab")
                        && terminalBridgeOpenTab.contains("lifecycleStatus != EchoNativeLoadStatus.MUTATED")
                        && terminalBridgeOpenTab.contains("replacingActiveScreen"),
                "Terminal ScreenCore open/close/back transitions must require mutating lifecycle evidence before setScreen.");
        require(catalogInputNoSuperFallback
                        && recipeInputNoSuperFallback
                        && holoMapInputNoSuperFallback
                        && terminalScreenCoreInputNoSuperFallback,
                "Native Loader-owned screen input branches must not fall back to super/local screen input when route dispatch does not mutate.");

        return Map.ofEntries(
                Map.entry("echoindex_screen_lifecycle_publisher", true),
                Map.entry("echoindex_screen_lifecycle_ui_host_handoff", true),
                Map.entry("echoindex_route_fallback_screen_lifecycle", true),
                Map.entry("echoindex_catalog_screen_transition_lifecycle", true),
                Map.entry("echoindex_catalog_screen_input_no_super_fallback", true),
                Map.entry("echoindex_recipe_screen_transition_lifecycle", true),
                Map.entry("echoindex_recipe_screen_input_no_super_fallback", true),
                Map.entry("echoindex_hotkey_screen_transition_lifecycle", true),
                Map.entry("echoindex_overlay_screen_transition_lifecycle", true),
                Map.entry("echoindex_screencore_screen_transition_lifecycle", true),
                Map.entry("echoholomap_screen_lifecycle_publisher", true),
                Map.entry("echoholomap_screen_lifecycle_ui_host_handoff", true),
                Map.entry("echoholomap_fullscreen_open_lifecycle", true),
                Map.entry("echoholomap_fullscreen_close_lifecycle", true),
                Map.entry("echoholomap_fullscreen_input_no_super_fallback", true),
                Map.entry("echoholomap_screencore_select_entry_route_owned", true),
                Map.entry("echoterminal_screen_lifecycle_publisher", true),
                Map.entry("echoterminal_screen_lifecycle_ui_host_handoff", true),
                Map.entry("echoterminal_classic_screen_transition_lifecycle", true),
                Map.entry("echoterminal_screencore_screen_transition_lifecycle", true),
                Map.entry("echoterminal_screencore_input_no_super_fallback", true)
        );
    }

    private static Map<String, Boolean> requireNativeWindowPumpService() throws Exception {
        Path pumpPath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderClientWindowPump.java");
        Path registrarPath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderCoreServiceRegistrar.java");
        Path smokePath = Path.of("echo-native-loader", "src", "qa", "java", "dev", "echo", "nativeplatform",
                "loader", "EchoNativeAgent2ClientRouteOwnershipSmokeMain.java");
        Path uiHostPath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderClientUiHost.java");
        Path routeTablePath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderClientRouteTable.java");
        Path defaultBridgePath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderDefaultProductBridgeProvider.java");
        Path liveHudBridgePath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderLiveHudRenderBridge.java");
        Path liveLoadingBridgePath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderLiveLoadingRenderBridge.java");
        Path uiActionRouterPath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderUiActionRouter.java");
        Path generatedUiSourcesPath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                "loader", "NativeLoaderGeneratedUiSources.java");

        String pump = Files.readString(pumpPath, StandardCharsets.UTF_8);
        String registrar = Files.readString(registrarPath, StandardCharsets.UTF_8);
        String smoke = Files.readString(smokePath, StandardCharsets.UTF_8);
        String uiHost = Files.readString(uiHostPath, StandardCharsets.UTF_8);
        String routeTable = Files.readString(routeTablePath, StandardCharsets.UTF_8);
        String defaultBridge = Files.readString(defaultBridgePath, StandardCharsets.UTF_8);
        String liveHudBridge = Files.readString(liveHudBridgePath, StandardCharsets.UTF_8);
        String liveLoadingBridge = Files.readString(liveLoadingBridgePath, StandardCharsets.UTF_8);
        String uiActionRouter = Files.readString(uiActionRouterPath, StandardCharsets.UTF_8);
        String generatedUiSources = Files.readString(generatedUiSourcesPath, StandardCharsets.UTF_8);

        require(pump.contains("public static final String SERVICE_ID = \"echo.native.client_window_pump\"")
                        && pump.contains("public static final String SOURCE = \"native_loader_window_pump\""),
                "Native Loader window pump must expose a stable service id and source stamp.");
        require(pump.contains("uiHost.mountSurface(")
                        && pump.contains("uiHost.openSurface(")
                        && pump.contains("uiHost.closeSurface(")
                        && pump.contains("uiHost.unmountSurface(")
                        && pump.contains("uiHost.keyInput(")
                        && pump.contains("uiHost.mouseInput(")
                        && pump.contains("uiHost.focusOverlay(")
                        && pump.contains("uiHost.renderGuiLayer(")
                        && pump.contains("uiHost.renderHudLayer(")
                        && pump.contains("uiHost.tick(")
                        && pump.contains("uiHost.dispatchRouteStatus(")
                        && pump.contains("builtInProductRendererFrame(")
                        && pump.contains("NativeLoaderClientUiHost.builtInProductSurfaceState()")
                        && pump.contains("frame.put(\"renderModel\", surfaceState.getOrDefault(\"renderModel\", Map.of()))"),
                "Native Loader window pump must delegate every window/input/render service through NativeLoaderClientUiHost.");
        require(pump.contains("enriched.put(\"screenSource\", SOURCE)")
                        && pump.contains("enriched.put(\"inputSource\", SOURCE)")
                        && pump.contains("enriched.put(\"mouseSource\", SOURCE)")
                        && pump.contains("enriched.put(\"focusSource\", SOURCE)")
                        && pump.contains("enriched.put(\"frameSource\", SOURCE)")
                        && pump.contains("frameMetadata.put(\"builtinProductRendererSource\", SOURCE)")
                        && pump.contains("enriched.put(\"tickSource\", SOURCE)")
                        && pump.contains("enriched.put(\"routeDispatchSource\", SOURCE)")
                        && pump.contains("enriched.putIfAbsent(\"source\", \"native_loader_client_ui_host\")")
                        && pump.contains("enriched.put(\"windowPumpSource\", SOURCE)")
                        && pump.contains("enriched.put(\"neoForgeEventOwnershipRequired\", false)"),
                "Native Loader window pump must stamp native pump metadata and reject NeoForge ownership requirements.");
        require(registrar.contains("registerClientWindowPump(serviceRegistry, new NativeLoaderClientWindowPump(clientUiHost))")
                        && registrar.contains("NativeLoaderClientWindowPump.SERVICE_ID")
                        && registrar.contains("\"client_window_pump\"")
                        && registrar.contains("\"client.gui_layer\"")
                        && registrar.contains("\"client.hud_layer\""),
                "Native Loader core services must register the client window pump beside the UI host.");
        require(uiHost.contains("case \"main_menu:mount\", \"main_menu:open\" -> \"menu.open\"")
                        && uiHost.contains("case \"main_menu:close\", \"main_menu:unmount\" -> \"menu.quit\"")
                        && uiHost.contains("case \"loading_screen:mount\", \"loading_screen:open\" -> \"loading.open\"")
                        && uiHost.contains("case \"loading_screen:render\" -> \"loading.render\"")
                        && uiHost.contains("case \"loading_screen:progress\" -> \"loading.progress\"")
                        && uiHost.contains("case \"loading_screen:close\", \"loading_screen:unmount\", \"loading_screen:complete\" -> \"loading.complete\"")
                        && uiHost.contains("String safeActionId = blank(actionId)")
                        && routeTable.contains("case \"main_menu:mount\", \"main_menu:open\" -> \"menu.open\"")
                        && routeTable.contains("case \"main_menu:close\", \"main_menu:unmount\" -> \"menu.quit\"")
                        && routeTable.contains("case \"loading_screen:mount\", \"loading_screen:open\" -> \"loading.open\"")
                        && routeTable.contains("case \"loading_screen:render\" -> \"loading.render\"")
                        && routeTable.contains("case \"loading_screen:progress\" -> \"loading.progress\"")
                        && routeTable.contains("case \"loading_screen:close\", \"loading_screen:unmount\", \"loading_screen:complete\" -> \"loading.complete\"")
                        && defaultBridge.contains("case \"main_menu:mount\", \"main_menu:open\" -> \"menu.open\"")
                        && defaultBridge.contains("case \"main_menu:close\", \"main_menu:unmount\" -> \"menu.quit\"")
                        && defaultBridge.contains("case \"loading_screen:mount\", \"loading_screen:open\" -> \"loading.open\"")
                        && defaultBridge.contains("case \"loading_screen:render\" -> \"loading.render\"")
                        && defaultBridge.contains("case \"loading_screen:progress\" -> \"loading.progress\"")
                        && defaultBridge.contains("case \"loading_screen:close\", \"loading_screen:unmount\", \"loading_screen:complete\" -> \"loading.complete\""),
                "Native Loader built-in menu/loading fallback must resolve omitted host actions to declared route actions before lifecycle-only evidence can satisfy host ownership.");
        require(uiHost.contains("\"echo-native-loader:generated_dashboard\"")
                        && uiHost.contains("\"native_dashboard\"")
                        && uiHost.contains("\"dashboard.render\"")
                        && uiHost.contains("\"dashboard.mouse\"")
                        && uiHost.contains("\"dashboard.character\"")
                        && uiHost.contains("\"dashboard.edit\"")
                        && uiHost.contains("\"dashboard.list_navigation\"")
                        && uiHost.contains("\"dashboard.settings\"")
                        && uiHost.contains("\"dashboard.submit\"")
                        && uiHost.contains("\"dashboard.close\""),
                "Native Loader must seed a route-owned generated dashboard shell for generated UI render/input/close handoffs.");
        require(defaultBridge.contains("recordService(\"screenLifecycle\", surfaceType, resolvedHostActionId(surfaceType, phase, actionId), status, metadata)")
                        && defaultBridge.contains("recordService(\"renderGuiLayer\", surfaceType, resolvedHostActionId(surfaceType, \"render\", actionId), status, metadata)")
                        && defaultBridge.contains("recordService(\"renderHudLayer\", surfaceType, resolvedHostActionId(surfaceType, \"render\", actionId), status, metadata)")
                        && defaultBridge.contains("private static String resolvedHostActionId(String surfaceType, String phase, String actionId)")
                        && defaultBridge.contains("String safeActionId = resolvedHostActionId(surfaceType, phase, actionId);"),
                "Default product live-client bridge service evidence must record the resolved built-in route action id that it dispatches.");
        require(uiHost.contains("String safeActionId = resolvedHostActionId(surfaceType, \"render\", actionId);")
                        && uiHost.contains("safeActionId = \"native_loader.render\";")
                        && uiHost.contains("recordHostService(\"gui_layer\", surfaceType, safeActionId, status, safeMetadata);")
                        && uiHost.contains("recordHostService(\"hud_layer\", surfaceType, safeActionId, status, safeMetadata);")
                        && uiHost.contains("private static String resolvedHostActionId(String surfaceType, String phase, String actionId)"),
                "Native Loader UI host render service evidence must record the resolved route action id that it dispatches.");
        require(smoke.contains("NativeLoaderClientWindowPump windowPump = new NativeLoaderClientWindowPump(host);")
                        && smoke.contains("windowPump.openScreen(")
                        && smoke.contains("windowPump.keyInput(")
                        && smoke.contains("windowPump.mouseInput(")
                        && smoke.contains("windowPump.renderGuiLayer(")
                        && smoke.contains("windowPump.renderHudLayer(")
                        && smoke.contains("windowPump.dispatchRoute(")
                        && smoke.contains("windowPump.builtInProductRendererFrame(")
                        && smoke.contains("requireBuiltInProductRendererFrame(")
                        && smoke.contains("windowPump.focusOverlay(")
                        && smoke.contains("windowPump.tick("),
                "Agent 2 smoke must exercise native window-pump APIs for host-service route ownership evidence.");
        String liveHudDispatch = methodBody(liveHudBridge,
                "private static Map<String, Object> dispatch(");
        require(liveHudDispatch.contains("EchoNativeClientRouteRegistries.get().renderHudLayer(")
                        && liveHudDispatch.contains("EchoNativeClientRouteRegistries.get().renderGuiLayer(")
                        && !liveHudDispatch.contains("NativeLoaderClientRouteTable.dispatchStatus("),
                "Native Loader live HUD projection must route always-on HUD/overlay rendering through public GUI/HUD host services instead of direct route-table dispatch.");
        require(liveLoadingBridge.contains("state.put(\"routeDispatch\", dispatchLoadingRoutes(")
                        && liveLoadingBridge.contains("EchoNativeClientRouteRegistries.get().renderGuiLayer(")
                        && liveLoadingBridge.contains("\"loading_screen\"")
                        && liveLoadingBridge.contains("\"loading.render\"")
                        && liveLoadingBridge.contains("\"loading.progress\"")
                        && liveLoadingBridge.contains("\"loading.complete\"")
                        && liveLoadingBridge.contains("native_loader_live_loading_render_bridge"),
                "Native Loader live loading renderer must mutate loading_screen routes through public GUI-layer/route host services.");
        require(generatedUiSources.contains("EchoNativeAgent5UiActionRouter.routeMainMenuOption(this.selectedOption)")
                        && generatedUiSources.contains("EchoNativeClientRouteRegistries.get().renderGuiLayer(")
                        && generatedUiSources.contains("\"native_loader_generated_main_menu_render\"")
                        && generatedUiSources.contains("menuRenderStatus == dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED")
                        && generatedUiSources.contains("private boolean openNativeDashboardScreen(")
                        && generatedUiSources.contains("private boolean dispatchNativeOpenRoute(")
                        && generatedUiSources.contains(".openSurface(\"terminal\", \"terminal.open\"")
                        && generatedUiSources.contains(".openSurface(\"index\", \"index.catalog\"")
                        && generatedUiSources.contains(".dispatchStatus(\"lens\", \"lens.deep_scan\"")
                        && generatedUiSources.contains(".openSurface(\"holomap\", \"holomap.open\"")
                        && generatedUiSources.contains(".renderHudLayer(\"hud\", \"hud.render\"")
                        && generatedUiSources.contains("status == dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED")
                        && generatedUiSources.contains("private boolean dispatchGeneratedDashboardRoute(")
                        && generatedUiSources.contains("private boolean dispatchGeneratedDashboardClose(")
                        && generatedUiSources.contains(".overlayInput(")
                        && generatedUiSources.contains("\"native_dashboard\"")
                        && generatedUiSources.contains("\"dashboard.render\"")
                        && generatedUiSources.contains("\"dashboard.mouse\"")
                        && generatedUiSources.contains("\"dashboard.character\"")
                        && generatedUiSources.contains("\"dashboard.edit\"")
                        && generatedUiSources.contains("\"dashboard.list_navigation\"")
                        && generatedUiSources.contains("\"dashboard.settings\"")
                        && generatedUiSources.contains("\"dashboard.submit\"")
                        && generatedUiSources.contains(".closeSurface(")
                        && generatedUiSources.contains("\"dashboard.close\"")
                        && generatedUiSources.contains("\"native_loader_generated_loading_overlay\"")
                        && generatedUiSources.contains("\"generated_loading_overlay_complete\"")
                        && generatedUiSources.contains(".renderGuiLayer(")
                        && generatedUiSources.contains("\"loading_screen\"")
                        && generatedUiSources.contains("\"loading.complete\"")
                        && generatedUiSources.contains("status != dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED")
                        && generatedUiSources.contains("this.minecraft.setOverlay(null)")
                        && uiActionRouter.contains("private static Map<String, Object> routeNativeMainMenuOption(")
                        && uiActionRouter.contains("EchoNativeClientRouteRegistries.get()")
                        && uiActionRouter.contains(".dispatchStatus(\"main_menu\", actionId")
                        && uiActionRouter.contains("\"native_loader_generated_main_menu\"")
                        && uiActionRouter.contains("\"menu.new_run\"")
                        && uiActionRouter.contains("\"menu.continue\"")
                        && uiActionRouter.contains("\"menu.settings\"")
                        && uiActionRouter.contains("\"menu.quit\"")
                        && uiActionRouter.contains("status != EchoNativeLoadStatus.MUTATED")
                        && uiActionRouter.contains("\"neoForgeEventOwnershipRequired\", false"),
                "Generated Ashfall dashboard menu/render/navigation paths must hand off to EchoNativeClientRouteRegistries before changing screen state.");

        return Map.ofEntries(
                Map.entry("native_window_pump_service_id", true),
                Map.entry("native_window_pump_host_delegates", true),
                Map.entry("native_window_pump_builtin_product_renderer_frame", true),
                Map.entry("native_window_pump_metadata_stamps", true),
                Map.entry("native_window_pump_core_service_registration", true),
                Map.entry("native_window_pump_builtin_route_action_resolution", true),
                Map.entry("native_dashboard_builtin_route_seeded", true),
                Map.entry("default_product_bridge_resolved_action_evidence", true),
                Map.entry("native_ui_host_render_resolved_action_evidence", true),
                Map.entry("native_live_hud_projection_uses_host_render_services", true),
                Map.entry("native_live_loading_render_uses_host_services", true),
                Map.entry("generated_main_menu_selection_uses_native_routes", true),
                Map.entry("generated_dashboard_navigation_uses_native_routes", true),
                Map.entry("generated_dashboard_input_uses_native_routes", true),
                Map.entry("generated_loading_overlay_completion_uses_native_routes", true),
                Map.entry("native_window_pump_smoke_exercised", true)
        );
    }

    private static Map<String, Object> requireClientWindowPumpServiceRegistration() {
        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        NativeLoaderCoreServiceRegistrar.registerCoreServices(
                serviceRegistry,
                "adaptercore.native_loader.backend"
        );

        Object uiHost = serviceRegistry.service(NativeLoaderCoreServiceRegistrar.CORE_MODULE_ID,
                        NativeLoaderClientUiHost.SERVICE_ID)
                .orElseThrow(() -> new IllegalStateException("Native Loader core services must register client UI host."));
        Object windowPump = serviceRegistry.service(NativeLoaderCoreServiceRegistrar.CORE_MODULE_ID,
                        NativeLoaderClientWindowPump.SERVICE_ID)
                .orElseThrow(() -> new IllegalStateException("Native Loader core services must register client window pump."));

        require(uiHost instanceof NativeLoaderClientUiHost,
                "Registered client UI host service must expose NativeLoaderClientUiHost.");
        require(windowPump instanceof NativeLoaderClientWindowPump,
                "Registered client window pump service must expose NativeLoaderClientWindowPump.");
        NativeLoaderClientWindowPump pump = (NativeLoaderClientWindowPump) windowPump;
        require(pump.uiHost() == uiHost,
                "Registered client window pump must share the same NativeLoaderClientUiHost service instance.");

        Map<String, Object> uiHostService = registeredService(serviceRegistry, NativeLoaderClientUiHost.SERVICE_ID);
        Map<String, Object> pumpService = registeredService(serviceRegistry, NativeLoaderClientWindowPump.SERVICE_ID);
        require("dev.echo.nativeplatform.loader.NativeLoaderClientUiHost".equals(uiHostService.get("implementationClass")),
                "Registered client UI host descriptor must name the production implementation class.");
        require("dev.echo.nativeplatform.loader.NativeLoaderClientWindowPump".equals(pumpService.get("implementationClass")),
                "Registered client window pump descriptor must name the production implementation class.");
        require(list(pumpService.get("surfaces")).containsAll(List.of(
                        "client_window_pump",
                        "client.tick",
                        "client.input",
                        "client.mouse",
                        "client.screen.lifecycle",
                        "client.overlay.focus",
                        "client.gui_layer",
                        "client.hud_layer"
                )),
                "Registered client window pump descriptor must expose every Native Loader client pump surface.");

        return Map.of(
                "uiHostServiceId", NativeLoaderClientUiHost.SERVICE_ID,
                "windowPumpServiceId", NativeLoaderClientWindowPump.SERVICE_ID,
                "uiHostRegistered", true,
                "windowPumpRegistered", true,
                "sharedUiHostInstance", true,
                "uiHostService", uiHostService,
                "windowPumpService", pumpService
        );
    }

    private static Map<String, Object> requireActivationClientWindowPumpRegistration() {
        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        NativeLoaderCoreServiceRegistrar.registerCoreServices(
                serviceRegistry,
                "adaptercore.native_loader.backend"
        );
        EchoNativeModuleLoadContext context = new EchoNativeModuleLoadContext(
                new EchoNativeModuleDescriptor(
                        "agent2activation",
                        "agent2activation",
                        "1.0.0",
                        "native_module",
                        "client",
                        "dev.echo.nativeplatform.loader.Agent2ActivationProbe",
                        EchoNativeRuntimeSide.CLIENT,
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        List.of()
                ),
                serviceRegistry,
                new LinkedHashMap<>(Map.of(
                        "packId", "ashfall",
                        "runtime", "echo_native",
                        "bootstrap", "agent2_activation_client_window_pump_probe"
                ))
        );
        Map<String, Object> activation = Map.of(
                "registryBridge", Map.of(
                        "registrations", List.of(Map.of(
                                "registry", "screen",
                                "id", "agent2:activation_surface",
                                "actions", Map.of(
                                        "agent2.activation_probe.open", Map.of("kind", "activation_probe_open")
                                ),
                                "source", "agent2_activation_client_window_pump_probe"
                        ))
                )
        );

        EchoNativeActivationSurfaceRegistrar.registerContent(context, activation);
        Map<String, Object> attributes = context.attributes();
        require(Boolean.TRUE.equals(attributes.get("nativeClientWindowPumpAvailable")),
                "Activation registrar must observe the Native Loader client window pump service.");
        require(Integer.valueOf(1).equals(number(attributes.get("nativeClientWindowPumpAvailableCount"))),
                "Activation registrar must count pump-available client UI registrations.");
        require(Integer.valueOf(1).equals(number(attributes.get("nativeClientRouteSdkRegistrationCount"))),
                "Activation registrar must register descriptor client UI declarations through the route SDK.");

        NativeLoaderClientUiHost uiHost = (NativeLoaderClientUiHost) serviceRegistry
                .service(NativeLoaderCoreServiceRegistrar.CORE_MODULE_ID, NativeLoaderClientUiHost.SERVICE_ID)
                .orElseThrow(() -> new IllegalStateException("Client UI host missing after activation probe."));
        Map<String, Object> route = NativeLoaderClientRouteTable.routeForSurface("screen");
        require("agent2activation".equals(route.get("moduleId"))
                        && "agent2:activation_surface".equals(route.get("surfaceId")),
                "Activation registrar must mount descriptor client UI registration into the Native Loader route table.");
        Map<String, Object> hostRoute = uiHost.routeHostEvidence();
        require(hostRoute.get("routesBySurfaceType") instanceof Map<?, ?> routesBySurface
                        && routesBySurface.get("screen") instanceof List<?> screenRoutes
                        && screenRoutes.stream().anyMatch(entry -> entry instanceof Map<?, ?> screenRoute
                                && "agent2activation".equals(screenRoute.get("moduleId"))
                                && "agent2:activation_surface".equals(screenRoute.get("surfaceId"))),
                "Activation-mounted client UI route must be visible through routeHostEvidence.");
        require(route.get("config") instanceof Map<?, ?> config
                        && Boolean.TRUE.equals(config.get("nativeClientWindowPumpServiceAvailable"))
                        && "echo.native.client_window_pump".equals(config.get("nativeClientWindowPumpServiceId"))
                        && "dev.echo.nativeplatform.loader.NativeLoaderClientWindowPump"
                        .equals(config.get("nativeClientWindowPumpServiceClass")),
                "Activation-mounted route config must preserve window-pump availability evidence.");
        require(NativeLoaderClientRouteTable.actions().getOrDefault("screen", Map.of())
                        .containsKey("agent2.activation_probe.open"),
                "Activation registrar must register descriptor-declared client UI route actions.");
        require(NativeLoaderClientRouteTable.actionRouteEvidence().getOrDefault("screen", Map.of())
                        .get("agent2.activation_probe.open") instanceof Map<?, ?> ownerRoute
                        && "agent2activation".equals(ownerRoute.get("moduleId"))
                        && "agent2:activation_surface".equals(ownerRoute.get("surfaceId")),
                "Descriptor-declared client UI action must be owner-mapped to the activation-mounted route.");
        NativeLoaderClientWindowPump pump = (NativeLoaderClientWindowPump) serviceRegistry
                .service(NativeLoaderCoreServiceRegistrar.CORE_MODULE_ID, NativeLoaderClientWindowPump.SERVICE_ID)
                .orElseThrow(() -> new IllegalStateException("Client window pump missing after activation probe."));
        EchoNativeLoadStatus dispatchStatus = pump.dispatchRoute(
                "screen",
                "agent2.activation_probe.open",
                Map.of("probe", "activation_descriptor_declared_action")
        );
        Map<String, Object> latestDispatch = latestActionDispatchEvent();
        require(dispatchStatus == EchoNativeLoadStatus.UNSUPPORTED
                        && "no_handlers".equals(latestDispatch.get("outcome"))
                        && "screen".equals(latestDispatch.get("surfaceType"))
                        && "agent2.activation_probe.open".equals(latestDispatch.get("actionId"))
                        && latestDispatch.get("route") instanceof Map<?, ?> dispatchRoute
                        && "agent2activation".equals(dispatchRoute.get("moduleId"))
                        && "agent2:activation_surface".equals(dispatchRoute.get("surfaceId")),
                "Descriptor-declared activation action must dispatch through the window pump to the owner route and fail honestly without a native handler.");

        return Map.of(
                "activationClientWindowPumpAvailable", true,
                "nativeClientWindowPumpAvailableCount", number(attributes.get("nativeClientWindowPumpAvailableCount")),
                "nativeClientRouteSdkRegistrationCount", number(attributes.get("nativeClientRouteSdkRegistrationCount")),
                "mountedRoute", route,
                "declaredActionOwnerMapped", true,
                "windowPumpDispatchStatusWithoutHandler", dispatchStatus.name(),
                "windowPumpDispatchOutcomeWithoutHandler", latestDispatch.get("outcome"),
                "mutationCount", context.mutations().size()
        );
    }

    private static Integer number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static Map<String, Object> latestActionDispatchEvent() {
        Object eventsObject = NativeLoaderClientRouteTable.actionDispatchEvidence().get("events");
        if (eventsObject instanceof List<?> events && !events.isEmpty()) {
            Object latest = events.get(events.size() - 1);
            if (latest instanceof Map<?, ?> event) {
                Map<String, Object> typed = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : event.entrySet()) {
                    if (entry.getKey() != null) {
                        typed.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                return Map.copyOf(typed);
            }
        }
        return Map.of();
    }

    private static Map<String, Object> registeredService(EchoNativeServiceRegistry serviceRegistry, String serviceId) {
        for (EchoNativeRegisteredService service : serviceRegistry.registeredServices()) {
            if (service.serviceId().equals(serviceId)) {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("moduleId", service.moduleId());
                summary.put("serviceId", service.serviceId());
                summary.put("implementationClass", service.implementationClass());
                summary.put("surfaces", service.surfaces());
                return Map.copyOf(summary);
            }
        }
        throw new IllegalStateException("Registered service descriptor missing for " + serviceId + ".");
    }

    private static List<String> list(Object value) {
        if (value instanceof List<?> values) {
            return values.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    private static Map<String, Boolean> requireStatusAwareNativeHostFallbacks() throws Exception {
        Map<String, Path> sources = Map.of(
                "native_loader_client_ui_host",
                Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                        "loader", "NativeLoaderClientUiHost.java"),
                "native_loader_default_product_bridge",
                Path.of("echo-native-loader", "src", "main", "java", "dev", "echo", "nativeplatform",
                        "loader", "NativeLoaderDefaultProductBridgeProvider.java")
        );
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (Map.Entry<String, Path> sourceEntry : sources.entrySet()) {
            String source = Files.readString(sourceEntry.getValue(), StandardCharsets.UTF_8);
            boolean statusAware = source.contains("NativeLoaderClientRouteTable.dispatchStatus(")
                    && !source.contains("NativeLoaderClientRouteTable.dispatch(");
            require(statusAware,
                    sourceEntry.getKey() + " Native Loader host fallback must preserve route dispatch status.");
            results.put(sourceEntry.getKey(), true);
        }
        String uiHostSource = Files.readString(sources.get("native_loader_client_ui_host"), StandardCharsets.UTF_8);
        String bridgeSource = Files.readString(sources.get("native_loader_default_product_bridge"), StandardCharsets.UTF_8);
        require(uiHostSource.contains("&& status == EchoNativeLoadStatus.MUTATED")
                        && !uiHostSource.contains("status == EchoNativeLoadStatus.MUTATED || status == EchoNativeLoadStatus.REGISTERED"),
                "Native Loader UI host must not promote REGISTERED bridge surface registration to trusted mutation.");
        require(bridgeSource.contains("return EchoNativeLoadStatus.MUTATED;")
                        && bridgeSource.contains("productClientRouteTableMutated\", true"),
                "Default product bridge must report MUTATED when it accepts a supported production client surface.");
        results.put("registered_surface_not_trusted_as_mutation", true);
        results.put("default_product_bridge_surface_registration_mutated", true);
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireTerminalNativeRouteStateSource() throws Exception {
        Path clientPath = addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                "echoterminal", "EchoTerminalClient.java");
        String clientSource = Files.readString(clientPath, StandardCharsets.UTF_8);
        require(clientSource.contains("private static Map<String, Object> nativeRouteState"),
                "Terminal native client must keep route-owned state evidence.");
        require(clientSource.contains("public static synchronized Map<String, Object> nativeRouteState()"),
                "Terminal native route state must be inspectable by release gates.");
        require(clientSource.contains("recordNativeRoute(context.actionId(), context.action(), handled, context.metadata());"),
                "Terminal native route dispatch must record the handled action result with route metadata.");
        require(clientSource.contains("Map<String, Object> nativeSession = EchoTerminalNativeSessionBridge.snapshot()")
                        && clientSource.contains("next.put(\"nativeSession\", nativeSession)"),
                "Terminal native route state must include native session bridge state.");
        require(clientSource.contains("next.put(\"screenCoreScreenAttached\", NATIVE_SCREEN_CORE_SCREEN.get() != null)")
                        && clientSource.contains("next.put(\"screenCoreActionAttached\", NATIVE_SCREEN_CORE_ACTION.get() != null")
                        && clientSource.contains("next.put(\"overlayRenderContextAttached\", NATIVE_TERMINAL_OVERLAY_RENDER.get() != null)"),
                "Terminal native route state must expose screen, action, and overlay render context attachment.");
        require(clientSource.contains("next.put(\"lastMetadata\", Map.copyOf(safeMetadata))")
                        && clientSource.contains("putIfPresent(next, \"lastSource\", safeMetadata.get(\"source\"))")
                        && clientSource.contains("putIfPresent(next, \"lastEventType\", safeMetadata.get(\"eventType\"))")
                        && clientSource.contains("putIfPresent(next, \"lastService\", safeMetadata.get(\"service\"))")
                        && clientSource.contains("putIfPresent(next, \"lastFrameSource\", safeMetadata.get(\"frameSource\"))")
                        && clientSource.contains("putIfPresent(next, \"lastPartialTick\", safeMetadata.get(\"partialTick\"))"),
                "Terminal native route state must preserve route metadata.");
        require(clientSource.contains("next.put(\"routeDrivenTerminalModel\", routeDrivenTerminalModel(")
                        && clientSource.contains("private static Map<String, Object> routeDrivenTerminalModel(")
                        && clientSource.contains("model.put(\"modelType\", \"terminal_route\")")
                        && clientSource.contains("model.put(\"routeDrivenTerminalState\", true)")
                        && clientSource.contains("model.put(\"terminalOpenRoute\",")
                        && clientSource.contains("model.put(\"screenInputRoute\",")
                        && clientSource.contains("model.put(\"screenCoreRoute\",")
                        && clientSource.contains("model.put(\"overlayRoute\",")
                        && clientSource.contains("model.put(\"nativeSession\", Map.copyOf(nativeSession))")
                        && clientSource.contains("model.put(\"routeMetadata\", Map.copyOf(safeMetadata))")
                        && clientSource.contains("putIfPresent(model, \"routeSource\", safeMetadata.get(\"source\"))"),
                "Terminal native route state must expose a concrete route-driven terminal model with session, route flags, attachments, and route metadata.");
        require(clientSource.contains("handled = dispatchNativeTerminalScreenInput(context);")
                        && clientSource.contains("private static boolean dispatchNativeTerminalScreenInput(")
                        && clientSource.contains("screen.handleCharTyped(new CharacterEvent(codePoint))")
                        && clientSource.contains("screen.handleMouseScroll(")
                        && !clientSource.contains("handled = EchoTerminalScreens.isManagedTerminalScreen(Minecraft.getInstance().screen);"),
                "Terminal native screen char/scroll routes must call real screen input handlers, not only acknowledge a managed screen.");
        Path modulePath = addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                "echoterminal", "EchoTerminalNativeModule.java");
        String moduleSource = Files.readString(modulePath, StandardCharsets.UTF_8);
        require(moduleSource.contains("\"terminal.screen.char_typed\", Map.of(")
                        && moduleSource.contains("\"terminal.screen.mouse_scroll\", Map.of(")
                        && moduleSource.contains("\"terminal.screen.frame.render\", Map.of(")
                        && moduleSource.contains("\"terminal.screencore.mouse\", Map.of(")
                        && moduleSource.contains("\"terminal.screencore.action\", Map.of(")
                        && moduleSource.contains("\"signalos.terminal\", Map.of(")
                        && moduleSource.contains(".register(\"client_overlay\", \"echoterminal:hud_overlay\"")
                        && moduleSource.contains("\"terminal.mission_hud.tick\", Map.of(\"kind\", \"terminal_overlay_tick\"")
                        && moduleSource.contains("\"terminal.discovery_toast.render\", Map.of(\"kind\", \"terminal_overlay_render\"")
                        && moduleSource.contains("registerNativeClientRoutesFromModule(context);")
                        && moduleSource.contains("context.recordMutation(\"client_routes\", \"register\", \"echoterminal:native_client_routes\"")
                        && moduleSource.contains("result.put(\"registryMutated\", true)")
                        && moduleSource.contains("nativeClientRouteRegistrarClass"),
                "Terminal native module descriptor must declare screen input, ScreenCore, SignalOS alias, and overlay route actions.");

        Map<String, Boolean> results = new LinkedHashMap<>();
        results.put("echoterminal_client_route_state", true);
        results.put("echoterminal_session_route_state", true);
        results.put("echoterminal_native_route_metadata_state", true);
        results.put("echoterminal_native_route_model_state", true);
        results.put("echoterminal_screen_input_real_mutation", true);
        results.put("echoterminal_descriptor_route_actions", true);
        Path renderCorePath = addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                "echoterminal", "integration", "TerminalRenderCoreClientIntegration.java");
        String renderCoreSource = Files.readString(renderCorePath, StandardCharsets.UTF_8);
        require(renderCoreSource.contains("registerActionHandler(\"terminal\", \"echoterminal:eui:rendercore_screen_frame\""),
                "Terminal RenderCore frame route handler must be owner-prefixed for echoterminal:eui.");
        results.put("echoterminal_rendercore_owner_handler", true);
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireHoloMapNativeRouteStateSource() throws Exception {
        Path clientPath = addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                "echoholomap", "EchoHoloMapClient.java");
        String clientSource = Files.readString(clientPath, StandardCharsets.UTF_8);
        require(clientSource.contains("private static Map<String, Object> nativeRouteState"),
                "HoloMap native client must keep route-owned state evidence.");
        require(clientSource.contains("public static synchronized Map<String, Object> nativeRouteState()"),
                "HoloMap native route state must be inspectable by release gates.");
        require(clientSource.contains("recordNativeRoute(context.actionId(), context.action(), handled, context.metadata());"),
                "HoloMap native route dispatch must record the handled action result with route metadata.");
        require(clientSource.contains("next.put(\"lastMetadata\", Map.copyOf(safeMetadata))")
                        && clientSource.contains("putIfPresent(next, \"lastSource\", safeMetadata.get(\"source\"))")
                        && clientSource.contains("putIfPresent(next, \"lastEventType\", safeMetadata.get(\"eventType\"))")
                        && clientSource.contains("putIfPresent(next, \"lastService\", safeMetadata.get(\"service\"))")
                        && clientSource.contains("putIfPresent(next, \"lastFrameSource\", safeMetadata.get(\"frameSource\"))")
                        && clientSource.contains("putIfPresent(next, \"lastScreenClass\", safeMetadata.get(\"screenClass\"))")
                        && clientSource.contains("putIfPresent(next, \"lastPartialTick\", safeMetadata.get(\"partialTick\"))"),
                "HoloMap native route state must preserve route metadata for minimap and fullscreen actions.");
        require(clientSource.contains("HoloMapMiniMapOverlay.nativeOverlayState()"),
                "HoloMap native route state must include minimap overlay mutation state.");
        require(clientSource.contains("HoloMapUiController.fullscreen().nativeRouteState()"),
                "HoloMap native route state must include fullscreen controller mutation state.");
        require(clientSource.contains("next.put(\"routeDrivenMapModel\", routeDrivenMapModel(actionId, action, handled, safeMetadata, minimap, fullscreen))")
                        && clientSource.contains("private static Map<String, Object> routeDrivenMapModel(")
                        && clientSource.contains("model.put(\"modelType\", \"holomap_route\")")
                        && clientSource.contains("model.put(\"routeDrivenMapState\", true)")
                        && clientSource.contains("model.put(\"minimapRoute\",")
                        && clientSource.contains("model.put(\"fullscreenRoute\",")
                        && clientSource.contains("model.put(\"screenBridgeRoute\",")
                        && clientSource.contains("model.put(\"minimap\", Map.copyOf(minimap))")
                        && clientSource.contains("model.put(\"fullscreen\", Map.copyOf(fullscreen))")
                        && clientSource.contains("model.put(\"routeMetadata\", Map.copyOf(safeMetadata))")
                        && clientSource.contains("putIfPresent(model, \"routeSource\", safeMetadata.get(\"source\"))")
                        && clientSource.contains("putIfPresent(model, \"routePartialTick\", safeMetadata.get(\"partialTick\"))"),
                "HoloMap native route state must expose a concrete route-driven map model with minimap/fullscreen state and route metadata.");
        Path modulePath = addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                "echoholomap", "EchoHoloMapNativeModule.java");
        String moduleSource = Files.readString(modulePath, StandardCharsets.UTF_8);
        require(moduleSource.contains("\"holomap.minimap.render\", Map.of(\"kind\", \"overlay_render\")")
                        && moduleSource.contains("\"holomap.toggle_minimap\", Map.of(")
                        && moduleSource.contains("\"bridgeMethod\", \"toggle\"")
                        && moduleSource.contains("\"holomap.zoom_in\", Map.of(")
                        && moduleSource.contains("\"bridgeMethod\", \"zoomIn\"")
                        && moduleSource.contains("\"holomap.zoom_out\", Map.of(")
                        && moduleSource.contains("\"bridgeMethod\", \"zoomOut\"")
                        && moduleSource.contains("\"holomap.cycle_corner\", Map.of(")
                        && moduleSource.contains("\"bridgeMethod\", \"cycleCorner\"")
                        && moduleSource.contains("\"holomap.fullscreen.key\", Map.of(\"kind\", \"fullscreen_key_input\")")
                        && moduleSource.contains("\"holomap.fullscreen.mouse\", Map.of(\"kind\", \"fullscreen_mouse_input\")")
                        && moduleSource.contains("\"holomap.fullscreen.scroll\", Map.of(\"kind\", \"fullscreen_scroll_input\")")
                        && moduleSource.contains("\"holomap.sync\", Map.of(\"kind\", \"fullscreen_command\", \"bridgeMethod\", \"sync\")")
                        && moduleSource.contains("\"holomap.center\", Map.of(\"kind\", \"fullscreen_command\", \"bridgeMethod\", \"center\")")
                        && moduleSource.contains("\"bridgeMethod\", \"toggleMarkers\"")
                        && moduleSource.contains("\"bridgeMethod\", \"cycleFields\"")
                        && moduleSource.contains("\"bridgeMethod\", \"toggleWaypoints\"")
                        && moduleSource.contains("\"bridgeMethod\", \"selectEntry\"")
                        && moduleSource.contains("\"holomap.close\", Map.of(\"kind\", \"fullscreen_command\", \"bridgeMethod\", \"close\")")
                        && moduleSource.contains("registerNativeClientRoutesFromModule(context);")
                        && moduleSource.contains("context.recordMutation(\"client_routes\", \"register\", \"echoholomap:native_client_routes\"")
                        && moduleSource.contains("result.put(\"registryMutated\", true)")
                        && moduleSource.contains("nativeClientRouteRegistrarClass"),
                "HoloMap native module descriptor must declare the same minimap command and fullscreen route actions as the client route table.");

        Path controllerPath = addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                "echoholomap", "client", "HoloMapUiController.java");
        String controllerSource = Files.readString(controllerPath, StandardCharsets.UTF_8);
        require(controllerSource.contains("public synchronized Map<String, Object> nativeRouteState()"),
                "HoloMap fullscreen controller must expose native route state.");
        require(controllerSource.contains("state.put(\"showMarkers\", showMarkers)")
                        && controllerSource.contains("state.put(\"fieldMode\", fieldMode.name())")
                        && controllerSource.contains("state.put(\"showWaypoints\", showWaypoints)")
                        && controllerSource.contains("state.put(\"zoom\", zoom)"),
                "HoloMap fullscreen native route state must expose live controller mutation fields.");

        Map<String, Boolean> results = new LinkedHashMap<>();
        results.put("echoholomap_client_route_state", true);
        results.put("echoholomap_client_route_metadata_state", true);
        results.put("echoholomap_native_route_model_state", true);
        results.put("echoholomap_descriptor_route_actions", true);
        Path routeBootstrapPath = Path.of("echo-native-loader", "src", "main", "java", "dev", "echo",
                "nativeplatform", "loader", "NativeLoaderProductClientRouteBootstrap.java");
        String routeBootstrap = Files.readString(routeBootstrapPath, StandardCharsets.UTF_8);
        require(!routeBootstrap.contains("nativeLoaderOwnedRouteFallback(entrypoint, exception, result)")
                        && !routeBootstrap.contains("result.put(\"nativeLoaderOwnedRouteFallback\", true)")
                        && !routeBootstrap.contains("fallbackRouteKeys")
                        && !routeBootstrap.contains("native_loader_owned_route_fallback")
                        && routeBootstrap.contains("result.put(\"methodInvoked\", true)")
                        && routeBootstrap.contains("result.put(\"nativeLoaderOwnedRouteFallback\", false)")
                        && routeBootstrap.contains("result.put(\"status\", EchoNativeLoadStatus.FAILED.name())"),
                "Native Loader product route bootstrap must fail closed when native client entrypoints cannot load; it must not install fallback route-table mutation.");
        results.put("native_loader_product_route_bootstrap_fail_closed", true);
        results.put("echoholomap_fullscreen_controller_state", true);
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireProductRouteStateSources() throws Exception {
        Map<String, Boolean> results = new LinkedHashMap<>();

        Path indexClientPath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "EchoIndexClient.java");
        String indexClient = Files.readString(indexClientPath, StandardCharsets.UTF_8);
        require(indexClient.contains("IndexNativeSessionBridge.recordNativeRoute(")
                        && indexClient.contains("context.metadata()"),
                "Index native route handlers must record product-side session state with route metadata.");
        Path indexBridgePath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "client", "IndexNativeSessionBridge.java");
        String indexBridge = Files.readString(indexBridgePath, StandardCharsets.UTF_8);
        require(indexBridge.contains("nativeIndexSessionReady")
                        && indexBridge.contains("nativeIndexOverlayUxComplete")
                        && indexBridge.contains("session.put(\"overlay\", IndexOverlay.snapshot())")
                        && indexBridge.contains("session.put(\"inventoryFacts\", inventoryFacts(cache, state))")
                        && indexBridge.contains("session.put(\"recipeFacts\", recipeFacts(cache, state))"),
                "Index native session bridge must expose live overlay, inventory, and recipe state.");
        results.put("echoindex_native_session_state", true);
        require(indexBridge.contains("Map<String, Object> routeMetadata")
                        && indexBridge.contains("Map<String, Object> safeRouteMetadata = routeMetadata == null ? Map.of() : routeMetadata")
                        && indexBridge.contains("actionEntry.put(\"routeMetadata\", Map.copyOf(safeRouteMetadata))")
                        && indexBridge.contains("putIfPresent(actionEntry, \"routeSource\", safeRouteMetadata.get(\"source\"))")
                        && indexBridge.contains("session.put(\"routeMetadata\", actionEntry.get(\"routeMetadata\"))")
                        && indexBridge.contains("session.put(\"routeSource\", actionEntry.getOrDefault(\"routeSource\", \"\"))"),
                "Index native session bridge must preserve Native Loader route metadata.");
        results.put("echoindex_native_route_metadata_state", true);
        require(indexBridge.contains("session.put(\"routeDrivenIndexModel\", routeDrivenIndexModel(actionEntry, session, safeRouteMetadata))")
                        && indexBridge.contains("private static Map<String, Object> routeDrivenIndexModel(")
                        && indexBridge.contains("model.put(\"modelType\", \"index_route\")")
                        && indexBridge.contains("model.put(\"routeDrivenIndexState\", true)")
                        && indexBridge.contains("model.put(\"catalogRoute\",")
                        && indexBridge.contains("model.put(\"recipeRoute\",")
                        && indexBridge.contains("model.put(\"usageRoute\",")
                        && indexBridge.contains("model.put(\"screenInputRoute\",")
                        && indexBridge.contains("model.put(\"overlayRoute\",")
                        && indexBridge.contains("model.put(\"inventoryFacts\", session.get(\"inventoryFacts\"))")
                        && indexBridge.contains("model.put(\"recipeFacts\", session.get(\"recipeFacts\"))")
                        && indexBridge.contains("model.put(\"overlay\", session.get(\"overlay\"))")
                        && indexBridge.contains("model.put(\"routeMetadata\", Map.copyOf(safeRouteMetadata))")
                        && indexBridge.contains("putIfPresent(model, \"routeSource\", safeRouteMetadata.get(\"source\"))"),
                "Index native session bridge must expose a concrete route-driven Index model with route flags, live data, and route metadata.");
        results.put("echoindex_native_route_model_state", true);
        Path indexModulePath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "EchoIndexNativeModule.java");
        String indexModule = Files.readString(indexModulePath, StandardCharsets.UTF_8);
        require(indexModule.contains("\"index.catalog\", Map.of(")
                        && indexModule.contains("\"index.recipe\", Map.of(")
                        && indexModule.contains("\"index.usage\", Map.of(")
                        && indexModule.contains("\"index.bookmark\", Map.of(")
                        && indexModule.contains("\"index.hotkey_screen_render\", Map.of(\"kind\", \"hotkey_screen_render\")")
                        && indexModule.contains("\"index.hotkey_key_pressed\", Map.of(\"kind\", \"hotkey_key_pressed\")")
                        && indexModule.contains("\"index.client.login\", Map.of(")
                        && indexModule.contains("\"index.client.logout\", Map.of(")
                        && indexModule.contains("\"index.client.resources_reloaded\", Map.of(")
                        && indexModule.contains("\"index.track_item\", Map.of(\"kind\", \"item_recipe\", \"recipeMode\", \"track\")")
                        && indexModule.contains("\"kind\", \"client_lifecycle\"")
                        && indexModule.contains("\"reason\", \"client resources reloaded\"")
                        && indexModule.contains("\"index.recipe_screen.mouse\", Map.of(\"kind\", \"recipe_screen_mouse_input\")")
                        && indexModule.contains("\"index.recipe_screen.scroll\", Map.of(\"kind\", \"recipe_screen_scroll_input\")")
                        && indexModule.contains("\"index.recipe_screen.key\", Map.of(\"kind\", \"recipe_screen_key_input\")")
                        && indexModule.contains("\"index.recipe_screen.char\", Map.of(\"kind\", \"recipe_screen_char_input\")")
                        && indexModule.contains("\"index.catalog_screen.mouse\", Map.of(\"kind\", \"catalog_screen_mouse_input\")")
                        && indexModule.contains("\"index.catalog_screen.scroll\", Map.of(\"kind\", \"catalog_screen_scroll_input\")")
                        && indexModule.contains("\"index.catalog_screen.key\", Map.of(\"kind\", \"catalog_screen_key_input\")")
                        && indexModule.contains("\"index.catalog_screen.char\", Map.of(\"kind\", \"catalog_screen_char_input\")")
                        && indexModule.contains("\"index.screencore.action\", Map.of(")
                        && indexModule.contains("\"screenBridge\", \"echoscreencore\"")
                        && indexModule.contains("\"actionCatalog\", \"IndexActions\"")
                        && indexModule.contains("registerNativeClientRoutesFromModule(context);")
                        && indexModule.contains("context.recordMutation(\"client_routes\", \"register\", \"echoindex:native_client_routes\"")
                        && indexModule.contains("result.put(\"registryMutated\", true)")
                        && indexModule.contains("nativeClientRouteRegistrarClass"),
                "Index native module descriptor must declare every live Index route action.");
        results.put("echoindex_descriptor_route_actions", true);

        Path lensClientPath = addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                "echolens", "EchoLensClient.java");
        String lensClient = Files.readString(lensClientPath, StandardCharsets.UTF_8);
        require(lensClient.contains("LensHudOverlay.recordNativeRoute(")
                        && lensClient.contains("context.metadata()"),
                "Lens native route handlers must record product-side route state with route metadata.");
        Path lensOverlayPath = addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                "echolens", "client", "LensHudOverlay.java");
        String lensOverlay = Files.readString(lensOverlayPath, StandardCharsets.UTF_8);
        require(lensOverlay.contains("private static volatile Map<String, Object> lastNativeRoute")
                        && lensOverlay.contains("nativeLensSessionReady")
                        && lensOverlay.contains("nativeLensUxComplete")
                        && lensOverlay.contains("session.put(\"targetDetection\", snapshot.get(\"targetDetection\"))")
                        && lensOverlay.contains("session.put(\"serverScan\", actionEntry.get(\"serverScan\"))")
                        && lensOverlay.contains("state.put(\"lastNativeRoute\", lastNativeRoute)"),
                "Lens native route state must expose target detection, server scan, and last route snapshot.");
        results.put("echolens_native_route_state", true);
        require(lensOverlay.contains("Map<String, Object> routeMetadata")
                        && lensOverlay.contains("Map<String, Object> safeRouteMetadata = routeMetadata == null ? Map.of() : routeMetadata")
                        && lensOverlay.contains("actionEntry.put(\"routeMetadata\", Map.copyOf(safeRouteMetadata))")
                        && lensOverlay.contains("putIfPresent(actionEntry, \"routeSource\", safeRouteMetadata.get(\"source\"))")
                        && lensOverlay.contains("putIfPresent(actionEntry, \"routeEventType\", safeRouteMetadata.get(\"eventType\"))")
                        && lensOverlay.contains("putIfPresent(actionEntry, \"routeService\", safeRouteMetadata.get(\"service\"))")
                        && lensOverlay.contains("session.put(\"routeMetadata\", actionEntry.get(\"routeMetadata\"))")
                        && lensOverlay.contains("session.put(\"routeSource\", actionEntry.getOrDefault(\"routeSource\", \"\"))")
                        && lensOverlay.contains("routeMetadataKeys")
                        && lensOverlay.contains("dispatch.put(\"routeSource\", clean(value(safeRouteMetadata, \"source\"), \"\"))")
                        && lensOverlay.contains("dispatch.put(\"routeService\", clean(value(safeRouteMetadata, \"service\"), \"\"))"),
                "Lens native route state must expose Native Loader route metadata in the last route snapshot.");
        results.put("echolens_native_route_metadata_state", true);
        require(lensOverlay.contains("session.put(\"routeDrivenOverlayModel\", routeDrivenOverlayModel(actionEntry, snapshot, safeRouteMetadata))")
                        && lensOverlay.contains("private static Map<String, Object> routeDrivenOverlayModel(")
                        && lensOverlay.contains("model.put(\"modelType\", \"lens_overlay_route\")")
                        && lensOverlay.contains("model.put(\"routeDrivenOverlayState\", true)")
                        && lensOverlay.contains("model.put(\"targetDetection\", snapshot.get(\"targetDetection\"))")
                        && lensOverlay.contains("model.put(\"scannerRoute\", snapshot.get(\"nativeLensScannerRoute\"))")
                        && lensOverlay.contains("model.put(\"actionFeedback\", snapshot.get(\"nativeLensLastActionFeedback\"))")
                        && lensOverlay.contains("model.put(\"serverScan\", actionEntry.get(\"serverScan\"))")
                        && lensOverlay.contains("model.put(\"routeMetadata\", Map.copyOf(safeRouteMetadata))")
                        && lensOverlay.contains("putIfPresent(model, \"routeSource\", safeRouteMetadata.get(\"source\"))"),
                "Lens native route state must expose a concrete route-driven overlay model with target, scan, feedback, and route metadata.");
        results.put("echolens_native_route_model_state", true);
        Path lensModulePath = addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                "echolens", "EchoLensNativeModule.java");
        String lensModule = Files.readString(lensModulePath, StandardCharsets.UTF_8);
        Path lensDescriptorPath = addonSourcePath("echolens", "src", "main", "resources",
                "META-INF", "echo.mod.json");
        String lensDescriptor = Files.readString(lensDescriptorPath, StandardCharsets.UTF_8);
        require(lensModule.contains("\"lens.deep_scan\", Map.of(")
                        && lensModule.contains("\"lens.index_recipe\", Map.of(")
                        && lensModule.contains("\"kind\", \"target_index\"")
                        && lensModule.contains("\"recipeMode\", \"recipes\"")
                        && lensModule.contains("\"lens.index_usage\", Map.of(")
                        && lensModule.contains("\"recipeMode\", \"usages\"")
                        && lensModule.contains("\"lens.track_in_index\", Map.of(")
                        && lensModule.contains("\"recipeMode\", \"track\"")
                        && lensModule.contains("registerNativeClientRoutesFromModule(context);")
                        && lensModule.contains("context.recordMutation(\"client_routes\", \"register\", \"echolens:native_client_routes\"")
                        && lensModule.contains("result.put(\"registryMutated\", true)")
                        && lensModule.contains("nativeClientRouteRegistrarClass")
                        && lensDescriptor.contains("\"ui.screens\"")
                        && lensDescriptor.contains("\"ui_screens\""),
                "Lens native module descriptor must declare every live Lens route action and authorize native UI overlay surfaces.");
        results.put("echolens_descriptor_route_actions", true);

        Path hudClientPath = addonSourcePath("echohudcore", "src", "main", "java", "com", "knoxhack",
                "echo", "hudcore", "EchoHudCoreClient.java");
        String hudClient = Files.readString(hudClientPath, StandardCharsets.UTF_8);
        require(hudClient.contains("EchoHudCoreOverlay.handleNativeHudAction(context.actionId(), context.action(), context.metadata())")
                        && hudClient.contains("EchoHudCoreOverlay.enableNativeRoute()")
                        && hudClient.contains("\"native_loader.overlay_focus\", Map.of(\"kind\", \"hud_overlay_focus\")"),
                "HUDCore native route handlers must enter HUD overlay route-state recording with route metadata.");
        Path hudModulePath = addonSourcePath("echohudcore", "src", "main", "java", "com", "knoxhack",
                "echo", "hudcore", "EchoHudCoreNativeModule.java");
        String hudModule = Files.readString(hudModulePath, StandardCharsets.UTF_8);
        Path hudDescriptorPath = addonSourcePath("echohudcore", "src", "main", "resources",
                "META-INF", "echo.mod.json");
        String hudDescriptor = Files.readString(hudDescriptorPath, StandardCharsets.UTF_8);
        require(hudModule.contains("\"hud.render\", Map.of(\"kind\", \"hud_render\")")
                        && hudModule.contains("\"hud.update_snapshot\", Map.of(\"kind\", \"hud_state_update\")")
                        && hudModule.contains("\"native_loader.overlay_focus\", Map.of(\"kind\", \"hud_overlay_focus\")")
                        && hudModule.contains("\"hud.mission_tracker.render\", Map.of(\"kind\", \"hud_widget_render\", \"widget\", \"mission_tracker\")")
                        && hudModule.contains("\"hud.hazard_readout.render\", Map.of(\"kind\", \"hud_widget_render\", \"widget\", \"hazard_readout\")")
                        && hudModule.contains("\"hud.compass_indicator.render\", Map.of(\"kind\", \"hud_widget_render\", \"widget\", \"compass_indicator\")")
                        && hudModule.contains("\"hud.screen_safe_area.resolve\", Map.of(\"kind\", \"hud_layout_resolve\")")
                        && hudModule.contains("registerNativeClientRoutesFromModule(context);")
                        && hudModule.contains("context.recordMutation(\"client_routes\", \"register\", \"echohudcore:native_client_routes\"")
                        && hudModule.contains("result.put(\"registryMutated\", true)")
                        && hudModule.contains("nativeClientRouteRegistrarClass")
                        && hudDescriptor.contains("\"ui.screens\"")
                        && hudDescriptor.contains("\"hud.widgets\"")
                        && hudDescriptor.contains("\"ui_screens\"")
                        && hudDescriptor.contains("\"requiresMinecraft\": true")
                        && hudDescriptor.contains("\"contractsOnly\": false"),
                "HUDCore native module descriptor must declare the same HUD overlay, widget, and layout route actions as the client route table and authorize live Native Loader HUD routes.");
        Path hudOverlayPath = addonSourcePath("echohudcore", "src", "main", "java", "com", "knoxhack",
                "echo", "hudcore", "client", "EchoHudCoreOverlay.java");
        String hudOverlay = Files.readString(hudOverlayPath, StandardCharsets.UTF_8);
        require(hudOverlay.contains("private static Map<String, Object> nativeRouteState")
                        && hudOverlay.contains("public static Map<String, Object> nativeRouteState()")
                        && hudOverlay.contains("public static boolean handleNativeHudAction(")
                        && hudOverlay.contains("Map<String, Object> metadata")
                        && hudOverlay.contains("next.put(\"lastMetadata\", Map.copyOf(safeMetadata))")
                        && hudOverlay.contains("putIfPresent(next, \"lastSource\", safeMetadata.get(\"source\"))")
                        && hudOverlay.contains("putIfPresent(next, \"lastService\", safeMetadata.get(\"service\"))")
                        && hudOverlay.contains("putIfPresent(next, \"lastFrameSource\", safeMetadata.get(\"frameSource\"))")
                        && hudOverlay.contains("putIfPresent(next, \"lastPartialTick\", safeMetadata.get(\"partialTick\"))")
                        && hudOverlay.contains("next.put(\"mutationCount\", intValue(previous.get(\"mutationCount\")) + 1)")
                        && hudOverlay.contains("next.put(\"renderCount\", intValue(previous.get(\"renderCount\")) + 1)")
                        && hudOverlay.contains("next.put(\"widgetRenderCount\", intValue(previous.get(\"widgetRenderCount\")) + 1)")
                        && hudOverlay.contains("next.put(\"layoutResolveCount\", intValue(previous.get(\"layoutResolveCount\")) + 1)")
                        && hudOverlay.contains("next.put(\"lastRenderModel\", hudRenderModel(safeMetadata))")
                        && hudOverlay.contains("next.put(\"lastSnapshotModel\", hudSnapshotModel())")
                        && hudOverlay.contains("next.put(\"lastWidgetModel\", widgetModel)")
                        && hudOverlay.contains("next.put(\"widgetModels\", updatedWidgetModels(previous.get(\"widgetModels\"), widgetModel))")
                        && hudOverlay.contains("next.put(\"lastLayoutModel\", hudLayoutModel(safeMetadata))")
                        && hudOverlay.contains("next.put(\"lastOverlayFocusModel\", overlayFocusModel(safeMetadata))")
                        && hudOverlay.contains("EchoHudSnapshotContract.executeReferenceSnapshot(")
                        && hudOverlay.contains("model.put(\"routeDrivenRendererState\", true)")
                        && hudOverlay.contains("model.put(\"routeDrivenLayoutState\", true)")
                        && hudOverlay.contains("model.put(\"routeDrivenFocusState\", true)")
                        && hudOverlay.contains("\"native_loader.overlay_focus\" -> true")
                        && hudOverlay.contains("\"hud_overlay_focus\".equals(kind)"),
                "HUDCore native route state must expose render/widget/layout/focus mutation models and route metadata.");
        results.put("echohudcore_native_route_state", true);
        results.put("echohudcore_native_route_metadata_state", true);
        results.put("echohudcore_native_route_model_state", true);
        results.put("echohudcore_overlay_focus_route_state", true);
        results.put("echohudcore_descriptor_route_actions", true);

        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireProductionClientRouteRegistrationSources() throws Exception {
        Map<String, Boolean> results = new LinkedHashMap<>();

        Path terminalPath = addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                "echoterminal", "EchoTerminalClient.java");
        String terminal = Files.readString(terminalPath, StandardCharsets.UTF_8);
        require(terminal.contains("registerNativeClientRoutes();")
                        && terminal.contains("public static boolean ensureNativeClientRoutesRegisteredForNativeLoader()")
                        && terminal.contains("return NATIVE_ROUTE_REGISTERED.get();")
                        && terminal.contains("\"echoterminal:eui\"")
                        && terminal.contains("\"echoterminal:hud_overlay\"")
                        && terminal.contains("\"terminal.screen.frame.render\", Map.of(")
                        && terminal.contains("\"terminal.screencore.action\", Map.of(")
                        && terminal.contains("\"terminal.mission_hud.render\", Map.of(\"kind\", \"terminal_overlay_render\"")
                        && terminal.contains("private static final List<NativeTerminalInputBinding> NATIVE_TERMINAL_INPUT_BINDINGS = List.of(")
                        && terminal.contains("new NativeTerminalInputBinding(\"terminal.open\", \"key.echoterminal.open\", GLFW.GLFW_KEY_M)")
                        && terminal.contains("for (NativeTerminalInputBinding binding : NATIVE_TERMINAL_INPUT_BINDINGS)")
                        && terminal.contains("registerInputBinding(registry, binding);")
                        && terminal.contains("registry.registerInputBinding(\"terminal\", binding.actionId()")
                        && terminal.contains("private static EchoNativeLoadStatus dispatchNativeInput(Object event)")
                        && terminal.contains("nativeKeyMetadata(event, binding)")
                        && !terminal.contains("nativeKeyMetadata(event, \"terminal.open\", \"key.echoterminal.open\")")
                        && terminal.contains("registry.registerActionHandler(\"terminal\", \"echoterminal:eui\"")
                        && terminal.contains("registry.registerActionHandler(\"client_overlay\", \"echoterminal:hud_overlay\"")
                        && terminal.contains("\"source\", \"echoterminal_native_module_route_registrar\""),
                "Terminal production client must register terminal and overlay native routes, actions, input, and handlers.");
        Path terminalRenderCorePath = addonSourcePath("echoterminal", "src", "main", "java", "com",
                "knoxhack", "echoterminal", "integration", "TerminalRenderCoreClientIntegration.java");
        String terminalRenderCore = Files.readString(terminalRenderCorePath, StandardCharsets.UTF_8);
        require(terminalRenderCore.contains("registry.registerActions(EchoTerminal.MODID, \"echoterminal:eui\", \"terminal\"")
                        && terminalRenderCore.contains("public static boolean ensureNativeScreenFrameRouteRegisteredForNativeLoader()")
                        && terminalRenderCore.contains("SCREEN_FRAME_ACTION, Map.of(")
                        && terminalRenderCore.contains("\"kind\", \"terminal_screen_frame_render\"")
                        && terminalRenderCore.contains("registry.registerActionHandler(\"terminal\", \"echoterminal:eui:rendercore_screen_frame\""),
                "Terminal RenderCore production integration must register the frame route under the Terminal route owner.");
        results.put("echoterminal_production_client_routes", true);
        results.put("echoterminal_rendercore_production_route", true);

        Path indexPath = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "EchoIndexClient.java");
        String index = Files.readString(indexPath, StandardCharsets.UTF_8);
        require(index.contains("registerNativeClientRoutes();")
                        && index.contains("public static boolean ensureNativeClientRoutesRegisteredForNativeLoader()")
                        && index.contains("return NATIVE_ROUTE_REGISTERED.get();")
                        && index.contains("\"echoindex:index\"")
                        && index.contains("\"echoindex:inventory_overlay\"")
                        && index.contains("\"index.catalog\", Map.of(")
                        && index.contains("\"index.client.login\", Map.of(\"kind\", \"client_lifecycle\"")
                        && index.contains("\"index.client.logout\", Map.of(\"kind\", \"client_lifecycle\"")
                        && index.contains("\"index.client.resources_reloaded\", Map.of(")
                        && index.contains("\"index.recipe_screen.key\", Map.of(\"kind\", \"recipe_screen_key_input\")")
                        && index.contains("\"index.catalog_screen.mouse\", Map.of(\"kind\", \"catalog_screen_mouse_input\")")
                        && index.contains("\"index.screencore.action\", Map.of(")
                        && index.contains("private static final List<NativeIndexInputBinding> NATIVE_INDEX_INPUT_BINDINGS = List.of(")
                        && index.contains("new NativeIndexInputBinding(\"index.catalog\", \"key.echoindex.catalog\", GLFW.GLFW_KEY_G)")
                        && index.contains("new NativeIndexInputBinding(\"index.recipe\", \"key.echoindex.recipe\", GLFW.GLFW_KEY_R)")
                        && index.contains("for (NativeIndexInputBinding binding : NATIVE_INDEX_INPUT_BINDINGS)")
                        && index.contains("registerInputBinding(registry, binding);")
                        && index.contains("registry.registerInputBinding(\"index\", binding.actionId()")
                        && index.contains("private static EchoNativeLoadStatus dispatchNativeInput(Object event)")
                        && index.contains("nativeKeyMetadata(event, binding.actionId(), binding.keyMapping())")
                        && !index.contains("case GLFW.GLFW_KEY_G -> registry.dispatchInputBindingStatus(")
                        && index.contains("registry.registerActionHandler(\"index\", \"echoindex:index\"")
                        && index.contains("registry.registerActionHandler(\"client_overlay\", \"echoindex:inventory_overlay\"")
                        && index.contains("\"source\", \"echoindex_native_module_route_registrar\""),
                "Index production client must register Index, overlay, screen, ScreenCore, input, and handler routes.");
        String indexLogin = methodBody(index, "private static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)");
        String indexLogout = methodBody(index, "private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)");
        String indexResources = methodBody(index, "private static void onClientResourceLoadFinished(ClientResourceLoadFinishedEvent event)");
        String indexLifecycleDispatch = methodBody(index,
                "private static EchoNativeLoadStatus dispatchNativeClientLifecycle(");
        require(indexLogin.contains("if (nativeLoaderActive())")
                        && indexLogin.contains("dispatchNativeClientLifecycle(\"index.client.login\"")
                        && indexLogin.contains("return;")
                        && indexLogout.contains("dispatchNativeClientLifecycle(\"index.client.logout\"")
                        && indexLogout.contains("return;")
                        && indexResources.contains("dispatchNativeClientLifecycle(\"index.client.resources_reloaded\"")
                        && indexResources.contains("\"invalidateScreenCoreIndex\", true")
                        && indexResources.contains("return;")
                        && indexLifecycleDispatch.contains(".dispatchStatus(\"index\", actionId")
                        && indexLifecycleDispatch.contains("\"source\", \"native_loader_client_lifecycle\"")
                        && indexLifecycleDispatch.contains("\"forwardedFrom\"")
                        && index.contains("if (\"client_lifecycle\".equals(kind))")
                        && index.contains("IndexService.INSTANCE.invalidateRecipes(reason)")
                        && index.contains("invalidateScreenCoreIndex();"),
                "Index client login/logout/resource reload listeners must be Native Loader lifecycle route adapters.");
        results.put("echoindex_production_client_routes", true);

        Path lensPath = addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                "echolens", "EchoLensClient.java");
        String lens = Files.readString(lensPath, StandardCharsets.UTF_8);
        require(lens.contains("registerNativeClientRoutes();")
                        && lens.contains("public static boolean ensureNativeClientRoutesRegisteredForNativeLoader()")
                        && lens.contains("return NATIVE_ROUTE_REGISTERED.get();")
                        && lens.contains("\"echolens:field_lens\"")
                        && lens.contains("\"echolens:lens_overlay\"")
                        && lens.contains("\"lens.deep_scan\", Map.of(")
                        && lens.contains("\"lens.index_recipe\", Map.of(\"kind\", \"target_index\"")
                        && lens.contains("\"lens.overlay.render\", Map.of(\"kind\", \"overlay_render\")")
                        && lens.contains("private static final Map<Integer, NativeLensInputBinding> NATIVE_INPUT_BINDINGS")
                        && lens.contains("new NativeLensInputBinding(")
                        && lens.contains("NATIVE_INPUT_BINDINGS.values().forEach(binding -> registerInputBinding(")
                        && lens.contains("NativeLensInputBinding binding = NATIVE_INPUT_BINDINGS.get(keyCode);")
                        && lens.contains("registry.keyInput(")
                        && lens.contains("registry.registerActionHandler(\"lens\", \"echolens:field_lens\"")
                        && lens.contains("registry.registerActionHandler(\"client_overlay\", \"echolens:lens_overlay\"")
                        && lens.contains("\"source\", \"echolens_native_module_route_registrar\""),
                "Lens production client must register Lens, overlay, target-index input, and handler routes from one native binding table.");
        results.put("echolens_production_client_routes", true);

        Path holoMapPath = addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                "echoholomap", "EchoHoloMapClient.java");
        String holoMap = Files.readString(holoMapPath, StandardCharsets.UTF_8);
        require(holoMap.contains("registerNativeClientRoutes();")
                        && holoMap.contains("public static boolean ensureNativeClientRoutesRegisteredForNativeLoader()")
                        && holoMap.contains("return NATIVE_ROUTE_REGISTERED.get();")
                        && holoMap.contains("\"echoholomap:minimap\"")
                        && holoMap.contains("\"echoholomap:fullscreen_map\"")
                        && holoMap.contains("\"holomap.minimap.render\", Map.of(\"kind\", \"overlay_render\")")
                        && holoMap.contains("\"holomap.fullscreen.key\", Map.of(\"kind\", \"fullscreen_key_input\")")
                        && holoMap.contains("\"holomap.close\", Map.of(\"kind\", \"fullscreen_command\", \"bridgeMethod\", \"close\")")
                        && holoMap.contains("private static final Map<Integer, NativeHoloMapInputBinding> NATIVE_INPUT_BINDINGS")
                        && holoMap.contains("new NativeHoloMapInputBinding(")
                        && holoMap.contains("NATIVE_INPUT_BINDINGS.values().forEach(binding -> registerInputBinding(")
                        && holoMap.contains("NativeHoloMapInputBinding binding = NATIVE_INPUT_BINDINGS.get(keyCode);")
                        && holoMap.contains("registry.keyInput(")
                        && holoMap.contains("registry.registerActionHandler(\"holomap\", \"echoholomap:minimap\"")
                        && holoMap.contains("registry.registerActionHandler(\"holomap\", \"echoholomap:fullscreen_map\"")
                        && holoMap.contains("\"source\", \"echoholomap_native_module_route_registrar\""),
                "HoloMap production client must register minimap, fullscreen, command, input, and handler routes from one native binding table.");
        results.put("echoholomap_production_client_routes", true);

        Path hudPath = addonSourcePath("echohudcore", "src", "main", "java", "com", "knoxhack",
                "echo", "hudcore", "EchoHudCoreClient.java");
        String hud = Files.readString(hudPath, StandardCharsets.UTF_8);
        require(hud.contains("registerNativeClientRoutes();")
                        && hud.contains("public static boolean ensureNativeClientRoutesRegisteredForNativeLoader()")
                        && hud.contains("return NATIVE_ROUTE_REGISTERED.get();")
                        && hud.contains("registerRoute(registry, \"echohudcore:native_hud\", \"hud\"")
                        && hud.contains("registerRoute(registry, \"echohudcore:mission_tracker\", \"hud_widget\"")
                        && hud.contains("registerRoute(registry, \"echohudcore:screen_safe_area\", \"hud_layout\"")
                        && hud.contains("\"hud.render\", Map.of(\"kind\", \"hud_render\")")
                        && hud.contains("\"native_loader.overlay_focus\", Map.of(\"kind\", \"hud_overlay_focus\")")
                        && hud.contains("\"hud.compass_indicator.render\"")
                        && hud.contains("registry.registerActionHandler(\"hud\", \"echohudcore:native_hud\"")
                        && hud.contains("registry.registerActionHandler(\"hud_widget\", \"echohudcore:compass_indicator\"")
                        && hud.contains("registry.registerActionHandler(\"hud_layout\", \"echohudcore:screen_safe_area\"")
                        && hud.contains("\"source\", \"echohudcore_native_module_route_registrar\""),
                "HUDCore production client must register HUD, widget, layout, focus, and handler routes.");
        results.put("echohudcore_production_client_routes", true);

        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireNativeEventAdaptersRouteOwned() throws Exception {
        Map<String, Boolean> results = new LinkedHashMap<>();
        Path terminal = addonSourcePath("echoterminal", "src", "main", "java", "com", "knoxhack",
                "echoterminal", "EchoTerminalClient.java");
        requireNativeEventAdapter(results, "echoterminal_key_input", terminal,
                "private static void onKeyInput(InputEvent.Key event)",
                List.of("dispatchNativeInput("),
                List.of("openTerminalScreen(", "minecraft.setScreen(null)"));
        requireNativeEventAdapter(results, "echoterminal_hud_tick", terminal,
                "private static void onClientTick(ClientTickEvent.Post event)",
                List.of(".tickRoute("),
                List.of("TerminalMissionHudController.tick(", "DiscoveryToastHud.tick("));
        requireNativeAdapterMetadata(results, "echoterminal_hud_tick_metadata", terminal,
                "private static void onClientTick(ClientTickEvent.Post event)",
                List.of(
                        "\"source\", \"native_loader_tick_service\"",
                        "\"forwardedFrom\"",
                        "\"eventType\", \"client_tick_post\"",
                        "\"overlay\", \"mission_hud\"",
                        "\"overlay\", \"discovery_toast\""
                ));
        requireNativeEventAdapter(results, "echoterminal_hud_render", terminal,
                "private static void onRenderGui(RenderGuiEvent.Post event)",
                List.of(".renderGuiLayer("),
                List.of("TerminalMissionHudController.render(", "DiscoveryToastHud.render("));
        requireNativeAdapterMetadata(results, "echoterminal_hud_render_metadata", terminal,
                "private static void onRenderGui(RenderGuiEvent.Post event)",
                List.of(
                        "\"source\", \"native_loader_gui_layer\"",
                        "\"forwardedFrom\"",
                        "\"eventType\", \"render_gui_post\"",
                        "\"partialTick\", partialTick"
                ));

        Path terminalScreenEvents = addonSourcePath("echoterminal", "src", "main", "java",
                "com", "knoxhack", "echoterminal", "client", "TerminalEventHandler.java");
        requireNativeEventAdapter(results, "echoterminal_screen_char", terminalScreenEvents,
                "public void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event)",
                List.of(".overlayInput("),
                List.of("screen.handleCharTyped("));
        requireNativeEventAdapter(results, "echoterminal_screen_scroll", terminalScreenEvents,
                "public void onMouseScroll(ScreenEvent.MouseScrolled.Pre event)",
                List.of(".mouseInput("),
                List.of("screen.handleMouseScroll("));

        Path terminalRenderCore = addonSourcePath("echoterminal", "src", "main", "java",
                "com", "knoxhack", "echoterminal", "integration", "TerminalRenderCoreClientIntegration.java");
        requireNativeEventAdapter(results, "echoterminal_rendercore_frame", terminalRenderCore,
                "private static void renderScreenFrame(ScreenEvent.Render.Post event)",
                List.of(".dispatchStatus("),
                List.of("drawScreenFrame("));

        Path index = addonSourcePath("echoindex", "src", "main", "java", "com", "knoxhack",
                "echoindex", "EchoIndexClient.java");
        requireNativeEventAdapter(results, "echoindex_hotkey_screen_render", index,
                "private static void onIndexHotkeyScreenRendered(ScreenEvent.Render.Post event)",
                List.of(".renderGuiLayer("),
                List.of("IndexHotkeys.onScreenRendered("));
        requireNativeAdapterMetadata(results, "echoindex_hotkey_screen_render_metadata", index,
                "private static void onIndexHotkeyScreenRendered(ScreenEvent.Render.Post event)",
                List.of(
                        "\"source\", \"native_loader_gui_layer\"",
                        "\"forwardedFrom\"",
                        "\"eventType\", \"screen_render_post\"",
                        "\"screenClass\"",
                        "\"partialTick\""
                ));
        requireNativeEventAdapter(results, "echoindex_hotkey_key", index,
                "private static void onIndexHotkeyKeyPressed(ScreenEvent.KeyPressed.Pre event)",
                List.of(".overlayInput("),
                List.of("IndexHotkeys.onKeyPressed("));
        requireNativeEventAdapter(results, "echoindex_overlay_render", index,
                "private static void onIndexOverlayRender(ContainerScreenEvent.Render.Foreground event)",
                List.of(".renderGuiLayer("),
                List.of("IndexOverlay.onRender("));
        requireNativeAdapterMetadata(results, "echoindex_overlay_render_metadata", index,
                "private static void onIndexOverlayRender(ContainerScreenEvent.Render.Foreground event)",
                List.of(
                        "\"source\", \"native_loader_gui_layer\"",
                        "\"forwardedFrom\"",
                        "\"eventType\", \"container_foreground_render\"",
                        "\"mouseX\"",
                        "\"mouseY\"",
                        "\"screenClass\""
                ));
        requireNativeEventAdapter(results, "echoindex_overlay_click", index,
                "private static void onIndexOverlayMouseClicked(ScreenEvent.MouseButtonPressed.Pre event)",
                List.of("dispatchNativeOverlayInput("),
                List.of("IndexOverlay.onMouseClicked("));
        requireNativeEventAdapter(results, "echoindex_overlay_drag", index,
                "private static void onIndexOverlayMouseDragged(ScreenEvent.MouseDragged.Pre event)",
                List.of("dispatchNativeOverlayInput("),
                List.of("IndexOverlay.onMouseDragged("));
        requireNativeEventAdapter(results, "echoindex_overlay_release", index,
                "private static void onIndexOverlayMouseReleased(ScreenEvent.MouseButtonReleased.Pre event)",
                List.of("dispatchNativeOverlayInput("),
                List.of("IndexOverlay.onMouseReleased("));
        requireNativeEventAdapter(results, "echoindex_overlay_scroll", index,
                "private static void onIndexOverlayMouseScrolled(ScreenEvent.MouseScrolled.Pre event)",
                List.of("dispatchNativeOverlayInput("),
                List.of("IndexOverlay.onMouseScrolled("));
        requireNativeEventAdapter(results, "echoindex_overlay_key", index,
                "private static void onIndexOverlayKeyPressed(ScreenEvent.KeyPressed.Pre event)",
                List.of("dispatchNativeOverlayInput("),
                List.of("IndexOverlay.onKeyPressed("));
        requireNativeMethodDispatch(results, "echoindex_catalog_screen_mouse", index,
                "public static boolean dispatchNativeCatalogScreenMouse(",
                List.of(".mouseInput("),
                List.of(".dispatchStatus(\"index\", \"index.catalog_screen.mouse\""));
        requireNativeMethodDispatch(results, "echoindex_catalog_screen_key", index,
                "public static boolean dispatchNativeCatalogScreenKey(",
                List.of(".overlayInput("),
                List.of(".dispatchStatus(\"index\", \"index.catalog_screen.key\""));
        requireNativeMethodDispatch(results, "echoindex_recipe_screen_mouse", index,
                "public static boolean dispatchNativeRecipeScreenMouse(",
                List.of(".mouseInput("),
                List.of(".dispatchStatus(\"index\", \"index.recipe_screen.mouse\""));
        requireNativeMethodDispatch(results, "echoindex_recipe_screen_key", index,
                "public static boolean dispatchNativeRecipeScreenKey(",
                List.of(".overlayInput("),
                List.of(".dispatchStatus(\"index\", \"index.recipe_screen.key\""));
        requireNativeEventAdapter(results, "echoindex_overlay_char", index,
                "private static void onIndexOverlayCharTyped(ScreenEvent.CharacterTyped.Pre event)",
                List.of("dispatchNativeOverlayInput("),
                List.of("IndexOverlay.onCharTyped("));
        requireNativeEventAdapter(results, "echoindex_key_input", index,
                "private static void onKeyInput(InputEvent.Key event)",
                List.of("dispatchNativeInput("),
                List.of("openHeldItemIndexRecipe(", "openIndexCatalog("));

        Path lens = addonSourcePath("echolens", "src", "main", "java", "com", "knoxhack",
                "echolens", "EchoLensClient.java");
        requireNativeEventAdapter(results, "echolens_key_input", lens,
                "private static void onKeyInput(InputEvent.Key event)",
                List.of("dispatchNativeInput("),
                List.of("LensHudOverlay.requestDeepScan(", "LensClientActions.openIndexRecipes(",
                        "LensClientActions.openIndexUses(", "LensClientActions.trackInIndex("));
        requireNativeEventAdapter(results, "echolens_overlay_render", lens,
                "private static void onRenderGui(RenderGuiEvent.Post event)",
                List.of(".renderGuiLayer("),
                List.of("LensHudOverlay.render("));
        requireNativeAdapterMetadata(results, "echolens_overlay_render_metadata", lens,
                "private static void onRenderGui(RenderGuiEvent.Post event)",
                List.of(
                        "\"source\", \"native_loader_gui_layer\"",
                        "\"forwardedFrom\"",
                        "\"eventType\", \"render_gui_post\"",
                        "\"partialTick\""
                ));

        Path holomap = addonSourcePath("echoholomap", "src", "main", "java", "com", "knoxhack",
                "echoholomap", "EchoHoloMapClient.java");
        requireNativeEventAdapter(results, "echoholomap_key_input", holomap,
                "private static void onKeyInput(InputEvent.Key event)",
                List.of("dispatchNativeInput("),
                List.of("toggleMiniMap(", "openHoloMapScreen(", "zoomMiniMapIn(", "zoomMiniMapOut(",
                        "cycleMiniMapCorner("));
        requireNativeEventAdapter(results, "echoholomap_minimap_render", holomap,
                "private static void renderMiniMapLayer(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker)",
                List.of(".renderGuiLayer("),
                List.of("HoloMapMiniMapOverlay.render("));
        requireNativeAdapterMetadata(results, "echoholomap_minimap_render_metadata", holomap,
                "private static void renderMiniMapLayer(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker)",
                List.of(
                        "\"source\", \"native_loader_gui_layer\"",
                        "\"forwardedFrom\"",
                        "\"eventType\", \"gui_layer_render\"",
                        "\"layerId\", MINIMAP_LAYER.toString()"
                ));
        requireNativeMethodDispatch(results, "echoholomap_fullscreen_mouse", holomap,
                "public static boolean dispatchNativeFullscreenMouse(",
                List.of(".mouseInput("),
                List.of(".dispatchStatus(\"holomap\", \"holomap.fullscreen.mouse\""));
        requireNativeMethodDispatch(results, "echoholomap_fullscreen_key", holomap,
                "public static boolean dispatchNativeFullscreenKey(",
                List.of(".overlayInput("),
                List.of(".dispatchStatus(\"holomap\", \"holomap.fullscreen.key\""));
        requireNativeMethodDispatch(results, "echoholomap_screencore_fullscreen_mouse", holomap,
                "public static boolean dispatchNativeScreenCoreFullscreenMouse(",
                List.of(".mouseInput("),
                List.of(".dispatchStatus(\"holomap\", \"holomap.fullscreen.mouse\""));
        requireNativeMethodDispatch(results, "echoholomap_screencore_fullscreen_key", holomap,
                "public static boolean dispatchNativeScreenCoreFullscreenKey(",
                List.of(".overlayInput("),
                List.of(".dispatchStatus(\"holomap\", \"holomap.fullscreen.key\""));

        Path hudCore = addonSourcePath("echohudcore", "src", "main", "java", "com", "knoxhack",
                "echo", "hudcore", "EchoHudCoreClient.java");
        requireNativeEventAdapter(results, "echohudcore_hud_render", hudCore,
                "public static void renderHud(GuiGraphicsExtractor graphics, float partialTick)",
                List.of(".renderHudLayer("),
                List.of("EchoHudCoreOverlay.render("));
        requireNativeAdapterMetadata(results, "echohudcore_hud_render_metadata", hudCore,
                "public static void renderHud(GuiGraphicsExtractor graphics, float partialTick)",
                List.of(
                        "\"source\", \"native_loader_hud_layer\"",
                        "\"forwardedFrom\"",
                        "\"eventType\", \"hud_render\"",
                        "\"partialTick\""
                ));

        return Map.copyOf(results);
    }

    private static void requireNativeEventAdapter(Map<String, Boolean> results, String key, Path sourcePath,
            String methodSignature, List<String> nativeDispatchTokens, List<String> legacyOwnerTokens) throws Exception {
        String method = methodBody(Files.readString(sourcePath, StandardCharsets.UTF_8), methodSignature);
        String nativeBranch = nativeLoaderBranchBody(method, key);
        boolean dispatchesThroughNativeRoute = false;
        for (String nativeDispatchToken : nativeDispatchTokens) {
            if (nativeBranch.contains(nativeDispatchToken)) {
                dispatchesThroughNativeRoute = true;
                break;
            }
        }
        require(dispatchesThroughNativeRoute,
                key + " Native Loader branch must dispatch through EchoNativeClientRouteRegistries.");
        require(nativeBranch.contains("return;"),
                key + " Native Loader branch must return before legacy event ownership can run.");
        for (String legacyOwnerToken : legacyOwnerTokens) {
            require(!nativeBranch.contains(legacyOwnerToken),
                    key + " Native Loader branch must not call legacy UI owner " + legacyOwnerToken + ".");
        }
        results.put(key, true);
    }

    private static void requireNativeMethodDispatch(Map<String, Boolean> results, String key, Path sourcePath,
            String methodSignature, List<String> nativeDispatchTokens, List<String> forbiddenTokens) throws Exception {
        String method = methodBody(Files.readString(sourcePath, StandardCharsets.UTF_8), methodSignature);
        boolean dispatchesThroughNativeHost = false;
        for (String nativeDispatchToken : nativeDispatchTokens) {
            if (method.contains(nativeDispatchToken)) {
                dispatchesThroughNativeHost = true;
                break;
            }
        }
        require(dispatchesThroughNativeHost,
                key + " must dispatch through a typed Native Loader host-service SDK method.");
        for (String forbiddenToken : forbiddenTokens) {
            require(!method.contains(forbiddenToken),
                    key + " must not fall back to generic route dispatch token " + forbiddenToken + ".");
        }
        results.put(key, true);
    }

    private static void requireNativeAdapterMetadata(
            Map<String, Boolean> results,
            String key,
            Path sourcePath,
            String methodSignature,
            List<String> requiredMetadataTokens
    ) throws Exception {
        String method = methodBody(Files.readString(sourcePath, StandardCharsets.UTF_8), methodSignature);
        String nativeBranch = nativeLoaderBranchBody(method, key);
        for (String token : requiredMetadataTokens) {
            require(nativeBranch.contains(token),
                    key + " Native Loader branch must preserve compatibility adapter metadata token " + token + ".");
        }
        results.put(key, true);
    }

    private static String methodBody(String source, String methodSignature) {
        int signatureIndex = source.indexOf(methodSignature);
        if (signatureIndex < 0) {
            String backendBridgeSignature = backendBridgeObjectSignature(methodSignature);
            if (!backendBridgeSignature.equals(methodSignature)) {
                signatureIndex = source.indexOf(backendBridgeSignature);
            }
        }
        if (signatureIndex < 0) {
            signatureIndex = relaxedMethodSignatureIndex(source, methodSignature);
        }
        require(signatureIndex >= 0, "Missing source method for Agent 2 gate: " + methodSignature);
        int openBrace = source.indexOf('{', signatureIndex);
        require(openBrace >= 0, "Missing method body for Agent 2 gate: " + methodSignature);
        int closeBrace = matchingBrace(source, openBrace);
        return source.substring(openBrace + 1, closeBrace);
    }

    private static String backendBridgeObjectSignature(String methodSignature) {
        int openParen = methodSignature.indexOf('(');
        int closeParen = methodSignature.lastIndexOf(')');
        if (openParen < 0 || closeParen < openParen) {
            return methodSignature;
        }
        String parameter = methodSignature.substring(openParen + 1, closeParen).trim();
        if (parameter.isBlank() || parameter.contains(",")) {
            return methodSignature;
        }
        int lastSpace = parameter.lastIndexOf(' ');
        if (lastSpace <= 0 || lastSpace == parameter.length() - 1) {
            return methodSignature;
        }
        return methodSignature.substring(0, openParen + 1)
                + "Object "
                + parameter.substring(lastSpace + 1)
                + methodSignature.substring(closeParen);
    }

    private static int relaxedMethodSignatureIndex(String source, String methodSignature) {
        String methodName = methodName(methodSignature);
        if (methodName.isBlank()) {
            return -1;
        }
        String needle = " " + methodName + "(";
        int searchFrom = 0;
        while (searchFrom < source.length()) {
            int index = source.indexOf(needle, searchFrom);
            if (index < 0) {
                return -1;
            }
            int lineStart = source.lastIndexOf('\n', index);
            String prefix = source.substring(lineStart + 1, index).trim();
            if (prefix.startsWith("private ")
                    || prefix.startsWith("public ")
                    || prefix.startsWith("protected ")
                    || prefix.startsWith("static ")) {
                return index + 1;
            }
            searchFrom = index + needle.length();
        }
        return -1;
    }

    private static String methodName(String methodSignature) {
        int openParen = methodSignature.indexOf('(');
        if (openParen < 0) {
            return "";
        }
        String beforeParameters = methodSignature.substring(0, openParen).trim();
        int lastSpace = beforeParameters.lastIndexOf(' ');
        return lastSpace < 0 ? beforeParameters : beforeParameters.substring(lastSpace + 1);
    }

    private static String nativeLoaderBranchBody(String method, String key) {
        int branchIndex = method.indexOf("if (nativeLoaderActive())");
        if (branchIndex < 0) {
            branchIndex = method.indexOf("nativeLoaderClientActiveForScreens())");
        }
        require(branchIndex >= 0, key + " must have a Native Loader compatibility branch.");
        int openBrace = method.indexOf('{', branchIndex);
        require(openBrace >= 0, key + " Native Loader branch must have a block body.");
        int closeBrace = matchingBrace(method, openBrace);
        return method.substring(openBrace + 1, closeBrace);
    }

    private static boolean nativeLoaderBranchHasNoSuperFallback(String source, List<String> methodSignatures) {
        for (String methodSignature : methodSignatures) {
            String method = methodBody(source, methodSignature);
            String nativeBranch = nativeLoaderBranchBody(method, methodSignature);
            if (!nativeBranch.contains("dispatchNative") || nativeBranch.contains("super.")) {
                return false;
            }
        }
        return true;
    }

    private static int matchingBrace(String source, int openBrace) {
        int depth = 0;
        for (int index = openBrace; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        throw new IllegalStateException("Unbalanced braces in Agent 2 source gate.");
    }

    private static void requireHostServiceEvents(Map<String, Object> routeHostEvidence) {
        Object eventsObject = routeHostEvidence.get("hostServiceEvents");
        require(eventsObject instanceof List<?>, "Native Loader UI host must expose host-service event evidence.");
        List<?> events = (List<?>) eventsObject;
        requireHostServiceEvent(events, "route_dispatch", "index", "index.catalog");
        requireHostServiceEvent(events, "input_binding", "", "key.echoterminal.open");
        requireHostServiceEvent(events, "key_input", "", "key.echo.native.menu");
        requireHostServiceEvent(events, "key_input", "", "key.echo.native.menu.new_run");
        requireHostServiceEvent(events, "key_input", "", "key.echo.native.menu.quit");
        requireHostServiceEvent(events, "key_input", "", "key.echoindex.recipe");
        requireHostServiceEvent(events, "screen_open", "terminal", "terminal.open");
        requireHostServiceEvent(events, "screen_lifecycle", "terminal", "terminal.open");
        requireHostServiceEvent(events, "screen_open", "holomap", "holomap.open");
        requireHostServiceEvent(events, "screen_open", "main_menu", "menu.open");
        requireHostServiceEvent(events, "screen_close", "main_menu", "menu.quit");
        requireHostServiceEvent(events, "screen_mount", "loading_screen", "loading.open");
        requireHostServiceEvent(events, "screen_unmount", "loading_screen", "loading.complete");
        requireHostServiceMetadata(events, "screen_open", "terminal", "terminal.open",
                "screenSource", "native_loader_window_pump");
        requireHostServiceMetadata(events, "screen_open", "holomap", "holomap.open",
                "focusedSurface", "holomap");
        requireHostServiceMetadata(events, "screen_open", "main_menu", "menu.open",
                "screenClass", "dev.echo.nativeplatform.loader.AshfallMainMenuScreen");
        requireHostServiceMetadata(events, "screen_mount", "loading_screen", "loading.open",
                "screenClass", "dev.echo.nativeplatform.loader.AshfallLoadingScreen");
        requireHostServiceMetadata(events, "screen_unmount", "loading_screen", "loading.complete",
                "closeReason", "complete");
        requireHostServiceEvent(events, "gui_layer", "loading_screen", "loading.render");
        requireHostServiceEvent(events, "gui_layer", "holomap", "holomap.minimap.render");
        requireHostServiceEvent(events, "hud_layer", "hud", "hud.render");
        requireHostServiceEvent(events, "overlay_focus", "hud", "native_loader.overlay_focus");
        requireHostServiceEvent(events, "mouse", "index", "index.catalog_screen.mouse");
        requireHostServiceEvent(events, "tick", "", "client_tick");
        requireHostServiceEvent(events, "tick", "terminal", "terminal.open");
        requireHostServiceMetadata(events, "tick", "terminal", "terminal.open",
                "tickRouteDispatch", true);
        requireHostServiceMetadata(events, "overlay_focus", "hud", "native_loader.overlay_focus",
                "focusSource", "native_loader_window_pump");
        requireHostServiceMetadata(events, "overlay_focus", "hud", "native_loader.overlay_focus",
                "focusedSurface", "hud");
        requireHostServiceMetadata(events, "mouse", "index", "index.catalog_screen.mouse",
                "mouseSource", "native_loader_window_pump");
        requireHostServiceMetadata(events, "mouse", "index", "index.catalog_screen.mouse",
                "phase", "press");
        requireHostServiceMetadata(events, "tick", "", "client_tick",
                "tickSource", "native_loader_window_pump");
        requireHostServiceMetadata(events, "tick", "", "client_tick",
                "frameIndex", 42);
        requireHostServiceSummary(routeHostEvidence);
    }

    private static void requirePublicSdkHostServiceEvents(Map<String, Object> routeHostEvidence) {
        Object eventsObject = routeHostEvidence.get("hostServiceEvents");
        require(eventsObject instanceof List<?>, "Native Loader public SDK UI host must expose host-service event evidence.");
        List<?> events = (List<?>) eventsObject;
        requireHostServiceEvent(events, "key_input", "", "key.echoterminal.open");
        requireHostServiceEvent(events, "overlay_input", "index", "index.catalog_screen.key");
        requireHostServiceEvent(events, "mouse", "index", "index.catalog_screen.mouse");
        requireHostServiceEvent(events, "overlay_focus", "hud", "native_loader.overlay_focus");
        requireHostServiceEvent(events, "tick", "", "direct_public_sdk_tick");
        requireHostServiceEvent(events, "tick", "terminal", "terminal.open");
        requireHostServiceEvent(events, "gui_layer", "loading_screen", "loading.render");
        requireHostServiceEvent(events, "hud_layer", "hud", "hud.render");
        requireHostServiceMetadata(events, "overlay_input", "index", "index.catalog_screen.key",
                "eventType", "direct_public_sdk_overlay_input");
        requireHostServiceMetadata(events, "mouse", "index", "index.catalog_screen.mouse",
                "eventType", "direct_public_sdk_mouse_input");
        requireHostServiceMetadata(events, "tick", "terminal", "terminal.open",
                "tickRouteDispatch", true);
        requireHostServiceMetadata(events, "overlay_focus", "hud", "native_loader.overlay_focus",
                "eventType", "direct_public_sdk_overlay_focus");
    }

    private static void refreshDirectPublicSdkHostServiceEvidence(EchoNativeClientRouteRegistry registry) {
        require(registry.keyInput(
                        "key.echoterminal.open",
                        77,
                        "press",
                        directPublicSdkInputMetadata("terminal", "terminal.open")
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK key input must refresh host-service evidence.");
        require(registry.overlayInput(
                        "index",
                        "index.catalog_screen.key",
                        directPublicSdkMetadata("index", Map.of("eventType", "direct_public_sdk_overlay_input"))
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK overlay input must refresh host-service evidence.");
        require(registry.mouseInput(
                        "index",
                        "index.catalog_screen.mouse",
                        directPublicSdkMetadata("index", Map.of(
                                "eventType", "direct_public_sdk_mouse_input",
                                "phase", "press",
                                "mouseSource", "native_loader_window_pump",
                                "mouseX", 12.0D,
                                "mouseY", 24.0D))
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK mouse input must refresh host-service evidence.");
        require(registry.tickRoute(
                        "terminal",
                        "terminal.open",
                        "direct_public_sdk_tick_route",
                        directPublicSdkMetadata("terminal", Map.of("eventType", "direct_public_sdk_tick_route"))
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK tick route must refresh host-service evidence.");
        require(registry.focusOverlay(
                        "hud",
                        true,
                        directPublicSdkMetadata("hud", Map.of(
                                "eventType", "direct_public_sdk_overlay_focus",
                                "focusedSurface", "hud",
                                "focusSource", "native_loader_window_pump"))
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK overlay focus must refresh host-service evidence.");
        require(registry.tick(
                        "direct_public_sdk_tick",
                        directPublicSdkMetadata("", Map.of("eventType", "direct_public_sdk_tick"))
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK tick must refresh host-service evidence.");
        require(registry.renderGuiLayer(
                        "loading_screen",
                        "loading.render",
                        directPublicSdkMetadata("loading_screen", Map.of("eventType", "direct_public_sdk_gui_layer"))
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK GUI layer must refresh host-service evidence.");
        require(registry.renderHudLayer(
                        "hud",
                        "hud.render",
                        directPublicSdkMetadata("hud", Map.of("eventType", "direct_public_sdk_hud_layer"))
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK HUD layer must refresh host-service evidence.");
    }

    private static void refreshScenarioHostServiceEvidence(NativeLoaderClientUiHost host) {
        require(host.keyInput(
                        "key.echo.native.menu",
                        256,
                        "press",
                        Map.of(
                                "screenClass", "dev.echo.nativeplatform.loader.AshfallMainMenuScreen",
                                "focusedSurface", "main_menu"
                        )
                ) == EchoNativeLoadStatus.MUTATED,
                "Scenario Native Loader UI host main-menu key input must refresh host-service evidence.");
        require(host.keyInput(
                        "key.echoholomap.minimap_cycle_corner",
                        92,
                        "press",
                        Map.of(
                                "screenClass", "com.knoxhack.echoholomap.client.HoloMapOverlay",
                                "focusedSurface", "holomap"
                        )
                ) == EchoNativeLoadStatus.MUTATED,
                "Scenario Native Loader UI host HoloMap key input must refresh host-service evidence.");
        require(host.tickRoute(
                        "terminal",
                        "terminal.open",
                        "client_tick",
                        Map.of(
                                "eventType", "client_tick_post",
                                "tickSource", "native_loader_window_pump",
                                "frameIndex", 42
                        )
                ) == EchoNativeLoadStatus.MUTATED,
                "Scenario Native Loader UI host tick route must refresh host-service evidence.");
        require(host.tick(
                        "client_tick",
                        Map.of(
                                "eventType", "client_tick_post",
                                "tickSource", "native_loader_window_pump",
                                "frameIndex", 42
                        )
                ) == EchoNativeLoadStatus.MUTATED,
                "Scenario Native Loader UI host client tick must refresh runtime state evidence.");
        require(host.openSurface(
                        "terminal",
                        "terminal.open",
                        Map.of(
                                "screenSource", "native_loader_window_pump",
                                "screenClass", "com.knoxhack.echoterminal.client.TerminalScreen"
                        )
                ) == EchoNativeLoadStatus.MUTATED,
                "Scenario Native Loader UI host terminal screen must refresh runtime state evidence.");
    }

    private static void refreshDirectPublicSdkActionDispatchEvidence(EchoNativeClientRouteRegistry registry) {
        require(registry.dispatchStatus(
                        "index",
                        "index.bookmark",
                        Map.of(
                                "source", "native_loader_client_ui_host",
                                "eventType", "action_dispatch_summary_refresh",
                                "neoForgeEventOwnershipRequired", false)
                ) == EchoNativeLoadStatus.MUTATED,
                "Index bookmark route must refresh latest handled surface evidence.");
        require(registry.dispatchStatus(
                        "hud",
                        "native_loader.overlay_focus",
                        Map.of(
                                "source", "native_loader_client_ui_host",
                                "eventType", "action_dispatch_summary_refresh",
                                "focusedSurface", "hud",
                                "neoForgeEventOwnershipRequired", false)
                ) == EchoNativeLoadStatus.MUTATED,
                "HUD overlay focus route must refresh latest handled surface evidence.");
        require(registry.dispatchStatus(
                        "main_menu",
                        "menu.new_run",
                        Map.of(
                                "source", "native_loader_client_ui_host",
                                "eventType", "action_dispatch_summary_refresh",
                                "neoForgeEventOwnershipRequired", false)
                ) == EchoNativeLoadStatus.MUTATED,
                "Main menu new-run route must refresh latest handled surface evidence.");
        require(registry.dispatchStatus(
                        "loading_screen",
                        "loading.complete",
                        directPublicSdkMetadata("loading_screen", Map.of(
                                "eventType", "direct_route_dispatch",
                                "closeReason", "complete"))
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK loading completion must refresh route dispatch evidence.");
    }

    private static void refreshDirectPublicSdkLifecycleEvidence(EchoNativeClientRouteRegistry registry) {
        require(registry.screenLifecycle(
                        "terminal",
                        "open",
                        "terminal.open",
                        directPublicSdkLifecycleMetadata("terminal", "screen_lifecycle")
                ) == EchoNativeLoadStatus.MUTATED,
                "Direct public SDK terminal screen lifecycle must refresh lifecycle evidence.");
        require(NativeLoaderClientRouteTable.screenLifecycle(
                        "terminal",
                        "open",
                        "terminal.open",
                        Map.of(
                                "source", "native_loader_client_ui_host",
                                "eventType", "screen_lifecycle_summary_refresh",
                                "screenSource", "native_loader_window_pump",
                                "neoForgeEventOwnershipRequired", false)
                ) == EchoNativeLoadStatus.MUTATED,
                "Route table terminal screen lifecycle must refresh handoff summary evidence.");
        require(NativeLoaderClientRouteTable.screenLifecycle(
                        "index",
                        "open",
                        "index.catalog",
                        Map.of(
                                "source", "native_loader_client_ui_host",
                                "eventType", "screen_lifecycle_summary_refresh",
                                "neoForgeEventOwnershipRequired", false)
                ) == EchoNativeLoadStatus.MUTATED,
                "Route table Index screen lifecycle must refresh handoff summary evidence.");
        require(NativeLoaderClientRouteTable.screenLifecycle(
                        "main_menu",
                        "open",
                        "menu.open",
                        Map.of(
                                "source", "native_loader_client_ui_host",
                                "eventType", "screen_lifecycle_summary_refresh",
                                "screenClass", "dev.echo.nativeplatform.loader.AshfallMainMenuScreen",
                                "neoForgeEventOwnershipRequired", false)
                ) == EchoNativeLoadStatus.MUTATED,
                "Route table main-menu screen lifecycle must refresh handoff summary evidence.");
    }

    private static void requireHostServiceSummary(Map<String, Object> routeHostEvidence) {
        Object evidenceObject = routeHostEvidence.get("hostServiceEvidence");
        require(evidenceObject instanceof Map<?, ?>,
                "Native Loader UI host must expose compact host-service evidence.");
        Map<?, ?> evidence = (Map<?, ?>) evidenceObject;
        require(evidence.get("eventCount") instanceof Integer eventCount && eventCount > 0,
                "Native Loader UI host compact host-service evidence must count service events.");
        require(evidence.get("events") instanceof List<?>,
                "Native Loader UI host compact host-service evidence must keep event audit trails.");
        Object summaryObject = evidence.get("summary");
        require(summaryObject instanceof Map<?, ?>,
                "Native Loader UI host compact host-service evidence must include a summary.");
        Map<?, ?> summary = (Map<?, ?>) summaryObject;
        require(summary.get("serviceCounts") instanceof Map<?, ?> serviceCounts
                        && serviceCounts.keySet().containsAll(List.of(
                                "route_dispatch",
                                "input_binding",
                                "key_input",
                                "screen_open",
                                "screen_lifecycle",
                                "screen_close",
                                "screen_mount",
                                "screen_unmount",
                                "gui_layer",
                                "hud_layer",
                                "overlay_focus",
                                "mouse",
                                "tick"
                        )),
                "Native Loader UI host service summary must count every client host service.");
        require(summary.get("statusCounts") instanceof Map<?, ?> statusCounts
                        && statusCounts.containsKey(EchoNativeLoadStatus.MUTATED.name()),
                "Native Loader UI host service summary must preserve MUTATED service status counts.");
        require(summary.get("sourceCounts") instanceof Map<?, ?> sourceCounts
                        && sourceCounts.get("native_loader_client_ui_host") instanceof Number hostSourceCount
                        && hostSourceCount.intValue() > 0,
                "Native Loader UI host service summary must count native_loader_client_ui_host source events.");
        require(summary.get("latestBySource") instanceof Map<?, ?> latestBySource
                        && latestBySource.get("native_loader_client_ui_host") instanceof Map<?, ?> hostSource
                        && "native_loader_client_ui_host".equals(hostSource.get("source"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(hostSource.get("status")),
                "Native Loader UI host service summary must expose latest native_loader_client_ui_host source event.");
        Object latestByServiceObject = summary.get("latestByService");
        require(latestByServiceObject instanceof Map<?, ?>,
                "Native Loader UI host service summary must include latest events by service.");
        Map<?, ?> latestByService = (Map<?, ?>) latestByServiceObject;
        require(latestByService.get("key_input") instanceof Map<?, ?> keyInput
                        && "key_input".equals(keyInput.get("service"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(keyInput.get("status"))
                        && "holomap".equals(keyInput.get("activeSurfaceType"))
                        && "holomap.cycle_corner".equals(keyInput.get("activeActionId"))
                        && "echoholomap".equals(keyInput.get("activeRouteModuleId"))
                        && "echoholomap:minimap".equals(keyInput.get("activeRouteSurfaceId")),
                "Native Loader UI host service summary must normalize key input to the handled route target.");
        require(latestByService.get("tick") instanceof Map<?, ?> tick
                        && "tick".equals(tick.get("service"))
                        && Set.of("client_tick", "terminal.open").contains(tick.get("actionId"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(tick.get("status")),
                "Native Loader UI host service summary must expose latest tick service state.");
        Object latestByActiveSurfaceServiceObject = summary.get("latestByActiveSurfaceService");
        require(latestByActiveSurfaceServiceObject instanceof Map<?, ?>,
                "Native Loader UI host service summary must include latest events by active surface and service.");
        Map<?, ?> latestByActiveSurfaceService = (Map<?, ?>) latestByActiveSurfaceServiceObject;
        requireHostServiceSummaryRoute(latestByActiveSurfaceService, "terminal:screen_open",
                "terminal", "screen_open", "terminal.open", "echoterminal", "echoterminal:eui");
        requireHostServiceSummaryRoute(latestByActiveSurfaceService, "index:mouse",
                "index", "mouse", "index.catalog_screen.mouse", "echoindex", "echoindex:index");
        requireHostServiceSummaryRoute(latestByActiveSurfaceService, "main_menu:key_input",
                "main_menu", "key_input", "menu.open",
                "echo-native-loader", "echo-native-loader:main_menu");
        requireHostServiceSummaryRoute(latestByActiveSurfaceService, "holomap:key_input",
                "holomap", "key_input", "holomap.cycle_corner", "echoholomap", "echoholomap:minimap");
        requireHostServiceSummaryRoute(latestByActiveSurfaceService, "client_overlay:gui_layer",
                "client_overlay", "gui_layer", "lens.overlay.render", "echolens", "echolens:lens_overlay");
        requireHostServiceSummaryRoute(latestByActiveSurfaceService, "hud_widget:hud_layer",
                "hud_widget", "hud_layer", "hud.compass_indicator.render",
                "echohudcore", "echohudcore:compass_indicator");
        requireHostServiceSummaryRoute(latestByActiveSurfaceService, "main_menu:screen_close",
                "main_menu", "screen_close", "menu.quit",
                "echo-native-loader", "echo-native-loader:main_menu");
        requireHostServiceSummaryRoute(latestByActiveSurfaceService, "loading_screen:screen_unmount",
                "loading_screen", "screen_unmount", "loading.complete",
                "echo-native-loader", "echo-native-loader:loading");
        Object latestRouteOwnedBySurfaceObject = summary.get("latestRouteOwnedBySurface");
        require(latestRouteOwnedBySurfaceObject instanceof Map<?, ?>,
                "Native Loader UI host service summary must include latest route-owned service by surface.");
        Map<?, ?> latestRouteOwnedBySurface = (Map<?, ?>) latestRouteOwnedBySurfaceObject;
        requireHostServiceSummaryRoute(latestRouteOwnedBySurface, "holomap",
                "holomap", "key_input", "holomap.cycle_corner", "echoholomap", "echoholomap:minimap");
        requireHostServiceSummaryRoute(latestRouteOwnedBySurface, "hud_widget",
                "hud_widget", "hud_layer", "hud.compass_indicator.render",
                "echohudcore", "echohudcore:compass_indicator");
    }

    private static void requireHostServiceSummaryRoute(
            Map<?, ?> summaryRoutes,
            String key,
            String surfaceType,
            String service,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        Object stateObject = summaryRoutes.get(key);
        require(stateObject instanceof Map<?, ?> state
                        && service.equals(state.get("service"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(state.get("status"))
                        && surfaceType.equals(state.get("activeSurfaceType"))
                        && actionId.equals(state.get("activeActionId"))
                        && moduleId.equals(state.get("activeRouteModuleId"))
                        && surfaceId.equals(state.get("activeRouteSurfaceId"))
                        && Boolean.TRUE.equals(state.get("activeRouteTrustedMutation"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(state.get("activeRouteStatus"))
                        && Boolean.TRUE.equals(state.get("activeNativeClientRouteProcess"))
                        && Boolean.FALSE.equals(state.get("activeNeoForgeEventOwnershipRequired"))
                        && Boolean.TRUE.equals(state.get("activeClientRouteMutationSupported"))
                        && state.get("activeRoute") instanceof Map<?, ?>,
                "Native Loader UI host service summary must expose route-owned service "
                        + key + "/" + actionId + ".");
    }

    private static void requireLiveClientBridgeServiceEvents(Map<String, Object> bridgeEvidence) {
        require(Boolean.TRUE.equals(bridgeEvidence.get("nativeLoaderOwnsClientHostServices"))
                        && Boolean.TRUE.equals(bridgeEvidence.get("neoForgeClientEventsCompatibilityAdaptersOnly")),
                "Default product NativeLoaderLiveClientBridge must declare Native Loader host-service ownership and NeoForge adapter-only events.");
        require(Boolean.TRUE.equals(bridgeEvidence.get("overlayInputSupported")),
                "Default product NativeLoaderLiveClientBridge must advertise typed overlay-input host service support.");
        Object eventsObject = bridgeEvidence.get("serviceEvents");
        require(eventsObject instanceof List<?>,
                "Default product NativeLoaderLiveClientBridge must expose service status events.");
        List<?> events = (List<?>) eventsObject;
        requireLiveClientBridgeServiceEvent(events, "screenLifecycle", "terminal", "terminal.open");
        requireLiveClientBridgeServiceEvent(events, "screenLifecycle", "main_menu", "menu.quit");
        requireLiveClientBridgeServiceEvent(events, "screenLifecycle", "loading_screen", "loading.open");
        requireLiveClientBridgeServiceEvent(events, "screenLifecycle", "loading_screen", "loading.complete");
        requireLiveClientBridgeServiceEvent(events, "dispatchRoute", "index", "index.catalog");
        requireLiveClientBridgeServiceEvent(events, "dispatchInputBinding", "", "key.echoterminal.open");
        requireLiveClientBridgeServiceEvent(events, "renderGuiLayer", "loading_screen", "loading.render");
        requireLiveClientBridgeServiceEvent(events, "renderGuiLayer", "holomap", "holomap.minimap.render");
        requireLiveClientBridgeServiceEvent(events, "renderHudLayer", "hud", "hud.render");
        requireLiveClientBridgeServiceEvent(events, "overlayFocus", "hud", "native_loader.overlay_focus");
        requireLiveClientBridgeServiceEvent(events, "overlayInput", "client_overlay", "index.inventory_overlay_input");
        requireLiveClientBridgeServiceEvent(events, "mouseInput", "index", "index.catalog_screen.mouse");
        requireLiveClientBridgeServiceEvent(events, "tick", "", "client_tick");
        requireLiveClientBridgeServiceSummary(bridgeEvidence);
        requireLiveClientBridgeActiveRoutes(bridgeEvidence);
    }

    private static void requireLiveClientBridgeServiceSummary(Map<String, Object> bridgeEvidence) {
        Object summaryObject = bridgeEvidence.get("serviceSummary");
        require(summaryObject instanceof Map<?, ?>,
                "Default product NativeLoaderLiveClientBridge must expose a compact service summary.");
        Map<?, ?> summary = (Map<?, ?>) summaryObject;
        require(summary.get("serviceCounts") instanceof Map<?, ?> serviceCounts
                        && serviceCounts.keySet().containsAll(List.of(
                                "screenLifecycle",
                                "dispatchRoute",
                                "dispatchInputBinding",
                                "renderGuiLayer",
                                "renderHudLayer",
                                "overlayInput",
                                "overlayFocus",
                                "mouseInput",
                                "tick"
                        )),
                "Default product NativeLoaderLiveClientBridge service summary must count every host callback.");
        require(summary.get("statusCounts") instanceof Map<?, ?> statusCounts
                        && statusCounts.containsKey(EchoNativeLoadStatus.MUTATED.name()),
                "Default product NativeLoaderLiveClientBridge service summary must preserve MUTATED statuses.");
        require(summary.get("sourceCounts") instanceof Map<?, ?> sourceCounts
                        && sourceCounts.get("native_loader_default_product_client_bridge") instanceof Number bridgeSourceCount
                        && bridgeSourceCount.intValue() > 0,
                "Default product NativeLoaderLiveClientBridge service summary must count bridge source events.");
        require(summary.get("latestBySource") instanceof Map<?, ?> latestBySource
                        && latestBySource.get("native_loader_default_product_client_bridge") instanceof Map<?, ?> bridgeSource
                        && "native_loader_default_product_client_bridge".equals(bridgeSource.get("source"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(bridgeSource.get("status")),
                "Default product NativeLoaderLiveClientBridge service summary must expose latest bridge source event.");
        require(summary.get("latestByService") instanceof Map<?, ?> latestByService
                        && latestByService.get("dispatchInputBinding") instanceof Map<?, ?> input
                        && "holomap".equals(input.get("activeSurfaceType"))
                        && "holomap.cycle_corner".equals(input.get("activeActionId"))
                        && "echoholomap".equals(input.get("activeRouteModuleId"))
                        && "echoholomap:minimap".equals(input.get("activeRouteSurfaceId")),
                "Default product NativeLoaderLiveClientBridge service summary must normalize input bindings to route targets.");
        Object latestByActiveSurfaceServiceObject = summary.get("latestByActiveSurfaceService");
        require(latestByActiveSurfaceServiceObject instanceof Map<?, ?>,
                "Default product NativeLoaderLiveClientBridge service summary must group by active surface and service.");
        Map<?, ?> latestByActiveSurfaceService = (Map<?, ?>) latestByActiveSurfaceServiceObject;
        requireLiveClientBridgeServiceSummaryRoute(latestByActiveSurfaceService, "terminal:screenLifecycle",
                "terminal", "screenLifecycle", "terminal.open", "echoterminal", "echoterminal:eui");
        requireLiveClientBridgeServiceSummaryRoute(latestByActiveSurfaceService, "index:mouseInput",
                "index", "mouseInput", "index.catalog_screen.mouse", "echoindex", "echoindex:index");
        requireLiveClientBridgeServiceSummaryRoute(latestByActiveSurfaceService, "holomap:dispatchInputBinding",
                "holomap", "dispatchInputBinding", "holomap.cycle_corner",
                "echoholomap", "echoholomap:minimap");
        requireLiveClientBridgeServiceSummaryRoute(latestByActiveSurfaceService, "client_overlay:renderGuiLayer",
                "client_overlay", "renderGuiLayer", "lens.overlay.render",
                "echolens", "echolens:lens_overlay");
        requireLiveClientBridgeServiceSummaryRoute(latestByActiveSurfaceService, "hud_widget:renderHudLayer",
                "hud_widget", "renderHudLayer", "hud.compass_indicator.render",
                "echohudcore", "echohudcore:compass_indicator");
        requireLiveClientBridgeServiceSummaryRoute(latestByActiveSurfaceService, "main_menu:screenLifecycle",
                "main_menu", "screenLifecycle", "menu.quit",
                "echo-native-loader", "echo-native-loader:main_menu");
        requireLiveClientBridgeServiceSummaryRoute(latestByActiveSurfaceService, "loading_screen:screenLifecycle",
                "loading_screen", "screenLifecycle", "loading.complete",
                "echo-native-loader", "echo-native-loader:loading");
        Object latestRouteOwnedBySurfaceObject = summary.get("latestRouteOwnedBySurface");
        require(latestRouteOwnedBySurfaceObject instanceof Map<?, ?>,
                "Default product NativeLoaderLiveClientBridge service summary must expose route-owned surfaces.");
        Map<?, ?> latestRouteOwnedBySurface = (Map<?, ?>) latestRouteOwnedBySurfaceObject;
        requireLiveClientBridgeServiceSummaryRoute(latestRouteOwnedBySurface, "holomap",
                "holomap", "dispatchInputBinding", "holomap.cycle_corner",
                "echoholomap", "echoholomap:minimap");
        requireLiveClientBridgeServiceSummaryRoute(latestRouteOwnedBySurface, "hud_widget",
                "hud_widget", "renderHudLayer", "hud.compass_indicator.render",
                "echohudcore", "echohudcore:compass_indicator");
    }

    private static void requireLiveClientBridgeServiceSummaryRoute(
            Map<?, ?> summaryRoutes,
            String key,
            String surfaceType,
            String service,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        Object stateObject = summaryRoutes.get(key);
        require(stateObject instanceof Map<?, ?> state
                        && "native_loader_default_product_client_bridge".equals(state.get("source"))
                        && service.equals(state.get("service"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(state.get("status"))
                        && surfaceType.equals(state.get("activeSurfaceType"))
                        && actionId.equals(state.get("activeActionId"))
                        && moduleId.equals(state.get("activeRouteModuleId"))
                        && surfaceId.equals(state.get("activeRouteSurfaceId"))
                        && Boolean.TRUE.equals(state.get("activeRouteTrustedMutation"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(state.get("activeRouteStatus"))
                        && Boolean.TRUE.equals(state.get("activeNativeClientRouteProcess"))
                        && Boolean.FALSE.equals(state.get("activeNeoForgeEventOwnershipRequired"))
                        && Boolean.TRUE.equals(state.get("activeClientRouteMutationSupported"))
                        && state.get("activeRoute") instanceof Map<?, ?>,
                "Default product NativeLoaderLiveClientBridge service summary must expose route-owned service "
                        + key + "/" + actionId + ".");
    }

    private static void requireRouteHostReleaseGateEvidence(Map<String, Object> routeHostEvidence) {
        Object gateObject = routeHostEvidence.get("releaseGateEvidence");
        require(gateObject instanceof Map<?, ?>,
                "Native Loader route-host evidence must include compact release gate evidence.");
        Map<?, ?> gate = (Map<?, ?>) gateObject;
        require("native_loader_client_ui_host".equals(gate.get("source"))
                        && Boolean.TRUE.equals(gate.get("nativeClientRouteProcess"))
                        && Boolean.FALSE.equals(gate.get("neoForgeEventOwnershipRequired")),
                "Native Loader route-host release gate evidence must be owned by the native client route process.");
        require(Boolean.TRUE.equals(gate.get("allRequiredSurfacesTrusted")),
                "Native Loader route-host release gate evidence must prove all Agent 2 surfaces are trusted.");
        require(Boolean.TRUE.equals(gate.get("allRequiredActionsTrusted")),
                "Native Loader route-host release gate evidence must prove required Agent 2 actions are trusted.");
        require(Boolean.TRUE.equals(gate.get("allRequiredInputBindingsTrusted")),
                "Native Loader route-host release gate evidence must prove required Agent 2 input bindings are trusted.");
        require(Boolean.TRUE.equals(gate.get("allRequiredHostInputMutationsPresent")),
                "Native Loader route-host release gate evidence must prove required Agent 2 host input mutations are present.");
        require(Boolean.TRUE.equals(gate.get("allRequiredRuntimeMutationsPresent")),
                "Native Loader route-host release gate evidence must prove required Agent 2 runtime mutations are present.");
        require(Boolean.TRUE.equals(gate.get("allRequiredHostServiceMutationsPresent")),
                "Native Loader route-host release gate evidence must prove required Agent 2 host-service mutations are present.");
        require(Boolean.TRUE.equals(gate.get("allRequiredHostTickMutationsPresent")),
                "Native Loader route-host release gate evidence must prove required Agent 2 host tick mutations are present.");
        require(Boolean.TRUE.equals(gate.get("allRequiredSummariesPresent")),
                "Native Loader route-host release gate evidence must prove compact summary evidence is present.");
        require(Boolean.TRUE.equals(gate.get("allRequiredSourceSummariesPresent")),
                "Native Loader route-host release gate evidence must prove source-aware compact summaries are present.");
        require(Boolean.TRUE.equals(gate.get("allRequiredSourceEvidencePresent")),
                "Native Loader route-host release gate evidence must prove required source keys are present.");
        require(gate.get("requiredSurfaceTrusted") instanceof Map<?, ?> requiredSurfaceTrusted
                        && requiredSurfaceTrusted.keySet().containsAll(List.of(
                                "terminal",
                                "index",
                                "lens",
                                "holomap",
                                "hud",
                                "hud_widget",
                                "hud_layout",
                                "client_overlay",
                                "main_menu",
                                "loading_screen"
                        ))
                        && requiredSurfaceTrusted.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required trusted Agent 2 surface.");
        require(gate.get("requiredActionTrusted") instanceof Map<?, ?> requiredActionTrusted
                        && requiredActionTrusted.keySet().containsAll(List.of(
                                "terminal:terminal.open",
                                "index:index.catalog",
                                "lens:lens.deep_scan",
                                "holomap:holomap.open",
                                "holomap:holomap.minimap.render",
                                "hud:hud.render",
                                "hud:hud.update_snapshot",
                                "hud:native_loader.overlay_focus",
                                "hud_widget:hud.mission_tracker.render",
                                "hud_widget:hud.hazard_readout.render",
                                "hud_widget:hud.compass_indicator.render",
                                "hud_layout:hud.screen_safe_area.resolve",
                                "client_overlay:terminal.mission_hud.render",
                                "client_overlay:index.inventory_overlay_render",
                                "client_overlay:lens.overlay.render",
                                "main_menu:menu.open",
                                "main_menu:menu.new_run",
                                "main_menu:menu.quit",
                                "loading_screen:loading.open",
                                "loading_screen:loading.render",
                                "loading_screen:loading.progress",
                                "loading_screen:loading.complete"
                        ))
                        && requiredActionTrusted.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required trusted Agent 2 action.");
        require(gate.get("requiredInputBindingTrusted") instanceof Map<?, ?> requiredInputBindingTrusted
                        && requiredInputBindingTrusted.keySet().containsAll(List.of(
                                "terminal:terminal.open",
                                "index:index.catalog",
                                "index:index.recipe",
                                "index:index.usage",
                                "index:index.bookmark",
                                "lens:lens.deep_scan",
                                "lens:lens.index_recipe",
                                "lens:lens.index_usage",
                                "lens:lens.track_in_index",
                                "holomap:holomap.open",
                                "holomap:holomap.toggle_minimap",
                                "holomap:holomap.zoom_in",
                                "holomap:holomap.zoom_out",
                                "holomap:holomap.cycle_corner",
                                "main_menu:menu.open",
                                "main_menu:menu.new_run",
                                "main_menu:menu.quit"
                        ))
                        && requiredInputBindingTrusted.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required trusted Agent 2 input binding.");
        require(gate.get("requiredInputBindings") instanceof Map<?, ?> requiredInputBindings
                        && "key.echoterminal.open".equals(requiredInputBindings.get("terminal:terminal.open"))
                        && "key.echoindex.recipe".equals(requiredInputBindings.get("index:index.recipe"))
                        && "echolens.key.deep_scan".equals(requiredInputBindings.get("lens:lens.deep_scan"))
                        && "key.echoholomap.minimap_cycle_corner".equals(requiredInputBindings.get("holomap:holomap.cycle_corner"))
                        && "key.echo.native.menu".equals(requiredInputBindings.get("main_menu:menu.open"))
                        && "key.echo.native.menu.new_run".equals(requiredInputBindings.get("main_menu:menu.new_run"))
                        && "key.echo.native.menu.quit".equals(requiredInputBindings.get("main_menu:menu.quit")),
                "Native Loader route-host release gate evidence must preserve expected product key mappings.");
        require(gate.get("requiredHostInputMutationPresent") instanceof Map<?, ?> requiredHostInputMutationPresent
                        && requiredHostInputMutationPresent.keySet().containsAll(List.of(
                                "terminal:terminal.open",
                                "index:index.catalog",
                                "index:index.recipe",
                                "index:index.usage",
                                "index:index.bookmark",
                                "lens:lens.deep_scan",
                                "lens:lens.index_recipe",
                                "lens:lens.index_usage",
                                "lens:lens.track_in_index",
                                "holomap:holomap.open",
                                "holomap:holomap.toggle_minimap",
                                "holomap:holomap.zoom_in",
                                "holomap:holomap.zoom_out",
                                "holomap:holomap.cycle_corner",
                                "main_menu:menu.open",
                                "main_menu:menu.new_run",
                                "main_menu:menu.quit"
                        ))
                        && requiredHostInputMutationPresent.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required host key-input mutation.");
        require(gate.get("requiredRuntimeMutationPresent") instanceof Map<?, ?> requiredRuntimeMutationPresent
                        && requiredRuntimeMutationPresent.keySet().containsAll(List.of(
                                "terminal:terminal.open",
                                "index:index.catalog_screen.mouse",
                                "lens:lens.overlay.render",
                                "holomap:holomap.open",
                                "holomap:holomap.cycle_corner",
                                "hud:hud.render",
                                "hud_widget:hud.compass_indicator.render",
                                "main_menu:menu.quit",
                                "loading_screen:loading.complete"
                        ))
                        && requiredRuntimeMutationPresent.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required live runtime mutation.");
        require(gate.get("requiredRuntimeMutations") instanceof Map<?, ?> requiredRuntimeMutations
                        && requiredRuntimeMutations.get("terminal:terminal.open") instanceof Map<?, ?> terminalMutation
                        && "screen_open".equals(terminalMutation.get("service"))
                        && "terminal.open".equals(terminalMutation.get("actionId"))
                        && requiredRuntimeMutations.get("lens:lens.overlay.render") instanceof Map<?, ?> lensMutation
                        && "client_overlay".equals(lensMutation.get("surfaceType"))
                        && "gui_layer".equals(lensMutation.get("service"))
                        && requiredRuntimeMutations.get("hud:hud.render") instanceof Map<?, ?> hudMutation
                        && "hud_layer".equals(hudMutation.get("service"))
                        && "hud.render".equals(hudMutation.get("actionId"))
                        && requiredRuntimeMutations.get("hud_widget:hud.compass_indicator.render") instanceof Map<?, ?> hudWidgetMutation
                        && "hud_layer".equals(hudWidgetMutation.get("service"))
                        && requiredRuntimeMutations.get("loading_screen:loading.complete") instanceof Map<?, ?> loadingMutation
                        && "screen_unmount".equals(loadingMutation.get("service")),
                "Native Loader route-host release gate evidence must describe required live runtime mutations.");
        require(gate.get("requiredHostServiceMutationPresent") instanceof Map<?, ?> requiredHostServiceMutationPresent
                        && requiredHostServiceMutationPresent.keySet().containsAll(List.of(
                                "terminal:terminal.open",
                                "terminal:terminal.screen.mouse_scroll",
                                "terminal:terminal.mission_hud.render",
                                "terminal:terminal.screencore.action",
                                "index:index.catalog",
                                "index:index.catalog_screen.mouse",
                                "index:index.recipe_screen.key",
                                "client_overlay:index.inventory_overlay_input",
                                "index:index.inventory_overlay_render",
                                "lens:lens.deep_scan",
                                "lens:lens.overlay.render",
                                "holomap:holomap.open",
                                "holomap:holomap.minimap.render",
                                "holomap:holomap.fullscreen.key",
                                "holomap:holomap.fullscreen.mouse",
                                "holomap:holomap.fullscreen.scroll",
                                "holomap:holomap.close",
                                "hud:hud.render",
                                "hud:hud.update_snapshot",
                                "hud:native_loader.overlay_focus",
                                "hud_widget:hud.compass_indicator.render",
                                "hud_layout:hud.screen_safe_area.resolve",
                                "main_menu:menu.open",
                                "main_menu:menu.new_run",
                                "loading_screen:loading.render",
                                "loading_screen:loading.progress",
                                "loading_screen:loading.complete"
                        ))
                        && requiredHostServiceMutationPresent.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required host-service mutation.");
        require(gate.get("requiredHostServiceMutations") instanceof Map<?, ?> requiredHostServiceMutations
                        && requiredHostServiceMutations.get("terminal:terminal.screen.mouse_scroll") instanceof Map<?, ?> terminalMouse
                        && "mouse".equals(terminalMouse.get("service"))
                        && requiredHostServiceMutations.get("terminal:terminal.mission_hud.render") instanceof Map<?, ?> terminalRender
                        && "client_overlay".equals(terminalRender.get("surfaceType"))
                        && "gui_layer".equals(terminalRender.get("service"))
                        && requiredHostServiceMutations.get("terminal:terminal.screencore.action") instanceof Map<?, ?> terminalScreenCoreAction
                        && "terminal".equals(terminalScreenCoreAction.get("surfaceType"))
                        && "route_dispatch".equals(terminalScreenCoreAction.get("service"))
                        && requiredHostServiceMutations.get("client_overlay:index.inventory_overlay_input") instanceof Map<?, ?> indexOverlayInput
                        && "route_dispatch".equals(indexOverlayInput.get("service"))
                        && requiredHostServiceMutations.get("index:index.recipe_screen.key") instanceof Map<?, ?> indexRecipeKey
                        && "index.recipe_screen.key".equals(indexRecipeKey.get("actionId"))
                        && "route_dispatch".equals(indexRecipeKey.get("service"))
                        && requiredHostServiceMutations.get("holomap:holomap.minimap.render") instanceof Map<?, ?> minimapRender
                        && "holomap".equals(minimapRender.get("surfaceType"))
                        && "gui_layer".equals(minimapRender.get("service"))
                        && requiredHostServiceMutations.get("holomap:holomap.fullscreen.key") instanceof Map<?, ?> holomapKey
                        && "route_dispatch".equals(holomapKey.get("service"))
                        && requiredHostServiceMutations.get("holomap:holomap.fullscreen.mouse") instanceof Map<?, ?> holomapMouse
                        && "route_dispatch".equals(holomapMouse.get("service"))
                        && requiredHostServiceMutations.get("holomap:holomap.fullscreen.scroll") instanceof Map<?, ?> holomapScroll
                        && "route_dispatch".equals(holomapScroll.get("service"))
                        && requiredHostServiceMutations.get("holomap:holomap.close") instanceof Map<?, ?> holomapClose
                        && "holomap.close".equals(holomapClose.get("actionId"))
                        && "route_dispatch".equals(holomapClose.get("service"))
                        && requiredHostServiceMutations.get("index:index.catalog_screen.mouse") instanceof Map<?, ?> indexMouse
                        && "mouse".equals(indexMouse.get("service"))
                        && requiredHostServiceMutations.get("hud:hud.update_snapshot") instanceof Map<?, ?> hudUpdate
                        && "route_dispatch".equals(hudUpdate.get("service"))
                        && requiredHostServiceMutations.get("hud:native_loader.overlay_focus") instanceof Map<?, ?> hudFocus
                        && "overlay_focus".equals(hudFocus.get("service"))
                        && requiredHostServiceMutations.get("hud_layout:hud.screen_safe_area.resolve") instanceof Map<?, ?> hudLayout
                        && "hud_layout".equals(hudLayout.get("surfaceType"))
                        && requiredHostServiceMutations.get("loading_screen:loading.progress") instanceof Map<?, ?> loadingProgress
                        && "route_dispatch".equals(loadingProgress.get("service")),
                "Native Loader route-host release gate evidence must describe required host-service mutations.");
        require(gate.get("requiredHostTickMutationPresent") instanceof Map<?, ?> requiredHostTickMutationPresent
                        && requiredHostTickMutationPresent.keySet().containsAll(List.of(
                                "client_overlay:terminal.mission_hud.tick",
                                "client_overlay:terminal.discovery_toast.tick"
                        ))
                        && requiredHostTickMutationPresent.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required host tick mutation.");
        require(gate.get("requiredHostTickMutations") instanceof Map<?, ?> requiredHostTickMutations
                        && requiredHostTickMutations.get("client_overlay:terminal.mission_hud.tick") instanceof Map<?, ?> missionTick
                        && "client_overlay".equals(missionTick.get("surfaceType"))
                        && "terminal.mission_hud.tick".equals(missionTick.get("actionId"))
                        && requiredHostTickMutations.get("client_overlay:terminal.discovery_toast.tick") instanceof Map<?, ?> toastTick
                        && "terminal.discovery_toast.tick".equals(toastTick.get("actionId")),
                "Native Loader route-host release gate evidence must describe required host tick mutations.");
        require(gate.get("summaryPresence") instanceof Map<?, ?> summaryPresence
                        && summaryPresence.keySet().containsAll(List.of(
                                "hostServiceEvidence",
                                "actionDispatchEvidence",
                                "declaredActionEvidence",
                                "inputDispatchEvidence",
                                "lifecycleEventEvidence",
                                "liveClientBridgeServiceSummary",
                                "liveClientBridgeActiveRoutes"
                        ))
                        && summaryPresence.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required compact summary.");
        require(gate.get("sourceSummaryPresence") instanceof Map<?, ?> sourceSummaryPresence
                        && sourceSummaryPresence.keySet().containsAll(List.of(
                                "hostServiceSourceCounts",
                                "actionDispatchMetadataSourceCounts",
                                "inputDispatchMetadataSourceCounts",
                                "lifecycleMetadataSourceCounts",
                                "liveClientBridgeSourceCounts"
                        ))
                        && sourceSummaryPresence.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required source-aware compact summary.");
        require(gate.get("requiredSourceEvidence") instanceof Map<?, ?> requiredSourceEvidence
                        && requiredSourceEvidence.keySet().containsAll(List.of(
                                "nativeLoaderUiHost",
                                "directPublicSdkRouteDispatch",
                                "directPublicSdkInputDispatch",
                                "directPublicSdkLifecycle",
                                "defaultProductClientBridge"
                        ))
                        && requiredSourceEvidence.values().stream().allMatch(Boolean.TRUE::equals),
                "Native Loader route-host release gate evidence must list every required source-key proof.");
        requireDeclaredActionEvidence(routeHostEvidence);
        require(gate.get("trustedSurfaceRoutes") instanceof Map<?, ?> trustedSurfaceRoutes
                        && trustedSurfaceRoutes.get("main_menu") instanceof Map<?, ?> mainMenu
                        && "echo-native-loader".equals(mainMenu.get("moduleId"))
                        && trustedSurfaceRoutes.get("loading_screen") instanceof Map<?, ?> loading
                        && "echo-native-loader".equals(loading.get("moduleId")),
                "Native Loader route-host release gate evidence must include built-in menu/loading trusted routes.");
        require(gate.get("trustedActionRoutes") instanceof Map<?, ?> trustedActionRoutes
                        && trustedActionRoutes.get("main_menu:menu.new_run") instanceof Map<?, ?> menuAction
                        && "echo-native-loader".equals(menuAction.get("moduleId"))
                        && trustedActionRoutes.get("loading_screen:loading.complete") instanceof Map<?, ?> loadingAction
                        && "echo-native-loader".equals(loadingAction.get("moduleId"))
                        && trustedActionRoutes.get("hud:hud.render") instanceof Map<?, ?> hudAction
                        && "echohudcore".equals(hudAction.get("moduleId")),
                "Native Loader route-host release gate evidence must include trusted built-in and HUD action routes.");
    }

    private static void requireDeclaredActionEvidence(Map<String, Object> routeHostEvidence) {
        Object evidenceObject = routeHostEvidence.get("declaredActionEvidence");
        require(evidenceObject instanceof Map<?, ?>,
                "Native Loader route-host evidence must include declared action evidence.");
        Map<?, ?> evidence = (Map<?, ?>) evidenceObject;
        require(evidence.get("actionCount") instanceof Integer actionCount && actionCount > 0,
                "Native Loader declared action evidence must count route actions.");
        Object summaryObject = evidence.get("summary");
        require(summaryObject instanceof Map<?, ?>,
                "Native Loader declared action evidence must expose a compact summary.");
        Map<?, ?> summary = (Map<?, ?>) summaryObject;
        require(summary.get("tickDrivenActionCount") instanceof Integer tickDrivenActionCount
                        && tickDrivenActionCount >= 2,
                "Native Loader declared action evidence must count tick-driven route actions.");
        require(summary.get("tickDrivenBySurface") instanceof Map<?, ?> tickDrivenBySurface
                        && tickDrivenBySurface.get("client_overlay") instanceof Map<?, ?> clientOverlay
                        && clientOverlay.get("terminal.mission_hud.tick") instanceof Map<?, ?> missionTick
                        && clientOverlay.get("terminal.discovery_toast.tick") instanceof Map<?, ?> discoveryTick
                        && "client_overlay_update".equals(missionTick.get("kind"))
                        && "client_overlay_tick".equals(discoveryTick.get("kind")),
                "Native Loader declared action evidence must expose Terminal overlay tick route declarations.");
    }

    private static void requireLiveClientBridgeActiveRoutes(Map<String, Object> bridgeEvidence) {
        Object activeObject = bridgeEvidence.get("activeClientRoutes");
        require(activeObject instanceof Map<?, ?>,
                "Default product NativeLoaderLiveClientBridge must expose active client routes.");
        Map<?, ?> active = (Map<?, ?>) activeObject;
        requireLiveClientBridgeActiveRoute(active, "terminal", "echoterminal", "echoterminal:eui",
                "terminal.open");
        requireLiveClientBridgeActiveRoute(active, "index", "echoindex", "echoindex:index",
                "index.bookmark");
        requireLiveClientBridgeActiveRoute(active, "holomap", "echoholomap", "echoholomap:minimap",
                "holomap.cycle_corner");
        requireLiveClientBridgeActiveRoute(active, "hud", "echohudcore", "echohudcore:native_hud",
                "native_loader.overlay_focus");
        requireLiveClientBridgeActiveRoute(active, "hud_widget", "echohudcore",
                "echohudcore:compass_indicator", "hud.compass_indicator.render");
        requireLiveClientBridgeActiveRoute(active, "main_menu", "echo-native-loader",
                "echo-native-loader:main_menu", "menu.new_run");
        requireLiveClientBridgeActiveRoute(active, "loading_screen", "echo-native-loader",
                "echo-native-loader:loading", "loading.complete");
    }

    private static void requireLiveClientBridgeActiveRoute(
            Map<?, ?> active,
            String surfaceType,
            String moduleId,
            String surfaceId,
            String actionId
    ) {
        Object stateObject = active.get(surfaceType);
        require(stateObject instanceof Map<?, ?>,
                "Default product NativeLoaderLiveClientBridge active routes must include " + surfaceType + ".");
        Map<?, ?> state = (Map<?, ?>) stateObject;
        require("native_loader_default_product_client_bridge".equals(state.get("source"))
                        && surfaceType.equals(state.get("surfaceType"))
                        && actionId.equals(state.get("actionId"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(state.get("status"))
                        && moduleId.equals(state.get("activeRouteModuleId"))
                        && surfaceId.equals(state.get("activeRouteSurfaceId"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(state.get("activeRouteStatus"))
                        && Boolean.TRUE.equals(state.get("activeRouteTrustedMutation"))
                        && Boolean.TRUE.equals(state.get("activeNativeClientRouteProcess"))
                        && Boolean.FALSE.equals(state.get("activeNeoForgeEventOwnershipRequired"))
                        && state.get("activeRoute") instanceof Map<?, ?>,
                "Default product NativeLoaderLiveClientBridge active route for " + surfaceType
                        + " must prove " + moduleId + "/" + surfaceId + " via " + actionId + ".");
    }

    private static void requireClientRuntimeState(Map<String, Object> routeHostEvidence) {
        Object stateObject = routeHostEvidence.get("clientRuntimeState");
        require(stateObject instanceof Map<?, ?>,
                "Native Loader UI host must expose client runtime state.");
        Map<?, ?> state = (Map<?, ?>) stateObject;
        require("native_loader_client_ui_host".equals(state.get("source")),
                "Client runtime state must be sourced from NativeLoaderClientUiHost.");
        require(state.get("lastHostServiceEvent") instanceof Map<?, ?>,
                "Client runtime state must include the last host-service event.");
        require(state.get("services") instanceof Map<?, ?>,
                "Client runtime state must include per-service snapshots.");
        Map<?, ?> services = (Map<?, ?>) state.get("services");
        for (String service : List.of(
                "route_dispatch",
                "input_binding",
                "key_input",
                "screen_open",
                "screen_close",
                "screen_mount",
                "screen_unmount",
                "gui_layer",
                "hud_layer",
                "overlay_focus",
                "mouse",
                "tick"
        )) {
            requireRuntimeService(services, service);
        }
        requireRuntimeServiceField(services, "key_input", "actionId", "key.echoholomap.minimap_cycle_corner");
        requireRuntimeInputTarget(services, "input_binding", "holomap", "holomap.cycle_corner",
                "echoholomap", "echoholomap:minimap");
        requireRuntimeInputTarget(services, "key_input", "holomap", "holomap.cycle_corner",
                "echoholomap", "echoholomap:minimap");
        requireRuntimeServiceField(services, "mouse", "surfaceType", "index");
        requireRuntimeServiceField(services, "mouse", "actionId", "index.catalog_screen.mouse");
        requireRuntimeServiceField(services, "hud_layer", "surfaceType", "hud_widget");
        requireRuntimeServiceField(services, "gui_layer", "surfaceType", "client_overlay");
        requireRuntimeServiceField(services, "tick", "actionId", "client_tick");
        requireRuntimeServiceRoute(services, "gui_layer", "client_overlay", "echolens", "echolens:lens_overlay");
        requireRuntimeServiceRoute(services, "hud_layer", "hud_widget", "echohudcore", "echohudcore:compass_indicator");
        requireRuntimeServiceRoute(services, "mouse", "index", "echoindex", "echoindex:index");
        requireRuntimeServiceMetadata(services, "overlay_focus", "focusSource", "native_loader_window_pump");
        requireRuntimeServiceMetadata(services, "mouse", "mouseSource", "native_loader_window_pump");
        requireRuntimeServiceMetadata(services, "tick", "tickSource", "native_loader_window_pump");
        require(state.get("screens") instanceof Map<?, ?>,
                "Client runtime state must include screen lifecycle snapshots.");
        Map<?, ?> screens = (Map<?, ?>) state.get("screens");
        requireRuntimeScreen(screens, "terminal", "screen_open", true, false);
        requireRuntimeScreen(screens, "holomap", "screen_open", true, false);
        requireRuntimeScreen(screens, "main_menu", "screen_close", false, false);
        requireRuntimeScreen(screens, "loading_screen", "screen_unmount", false, false);
        requireRuntimeScreenMetadata(screens, "terminal", "screenSource", "native_loader_window_pump");
        requireRuntimeScreenMetadata(screens, "holomap", "focusedSurface", "holomap");
        requireRuntimeScreenMetadata(screens, "main_menu", "closeReason", "quit");
        requireRuntimeScreenMetadata(screens, "loading_screen", "closeReason", "complete");
        requireRuntimeScreenRoute(screens, "terminal", "echoterminal", "echoterminal:eui");
        requireRuntimeScreenRoute(screens, "holomap", "echoholomap", "echoholomap:fullscreen_map");
        requireRuntimeScreenRoute(screens, "main_menu", "echo-native-loader", "echo-native-loader:main_menu");
        requireRuntimeScreenRoute(screens, "loading_screen", "echo-native-loader", "echo-native-loader:loading");
        require(state.get("input") instanceof Map<?, ?> input
                        && "key_input".equals(input.get("service"))
                        && "key.echoholomap.minimap_cycle_corner".equals(input.get("actionId"))
                        && "MUTATED".equals(input.get("status"))
                        && runtimeInputTargetMatches(input, "holomap", "holomap.cycle_corner",
                                "echoholomap", "echoholomap:minimap"),
                "Client runtime state must expose the last route-owned key input target.");
        require(state.get("mouse") instanceof Map<?, ?> mouse
                        && "index".equals(mouse.get("surfaceType"))
                        && "MUTATED".equals(mouse.get("status"))
                        && mouse.get("metadata") instanceof Map<?, ?> metadata
                        && "native_loader_window_pump".equals(metadata.get("mouseSource"))
                        && "press".equals(metadata.get("phase")),
                "Client runtime state must expose the last Native Loader mouse service.");
        require(state.get("overlayFocus") instanceof Map<?, ?> focus
                        && "hud".equals(focus.get("surfaceType"))
                        && "MUTATED".equals(focus.get("status"))
                        && focus.get("metadata") instanceof Map<?, ?> metadata
                        && "native_loader_window_pump".equals(metadata.get("focusSource"))
                        && Boolean.TRUE.equals(metadata.get("focused")),
                "Client runtime state must expose overlay focus ownership.");
        require(state.get("tick") instanceof Map<?, ?> tick
                        && "client_tick".equals(tick.get("actionId"))
                        && "MUTATED".equals(tick.get("status"))
                        && tick.get("metadata") instanceof Map<?, ?> metadata
                        && "native_loader_window_pump".equals(metadata.get("tickSource"))
                        && metadataValueEquals(42, metadata.get("frameIndex")),
                "Client runtime state must expose Native Loader tick pump metadata.");
        require(state.get("hudLayer") instanceof Map<?, ?> hud
                        && "hud_widget".equals(hud.get("surfaceType"))
                        && "MUTATED".equals(hud.get("status"))
                        && runtimeRouteOwnerMatches(hud, "hud_widget", "echohudcore", "echohudcore:compass_indicator"),
                "Client runtime state must expose HUD layer route ownership.");
        require(state.get("guiLayer") instanceof Map<?, ?> gui
                        && "client_overlay".equals(gui.get("surfaceType"))
                        && "MUTATED".equals(gui.get("status"))
                        && runtimeRouteOwnerMatches(gui, "client_overlay", "echolens", "echolens:lens_overlay"),
                "Client runtime state must expose GUI layer route ownership.");
        requireActiveUiSurfaces(state);
    }

    private static void requireActiveUiSurfaces(Map<?, ?> runtimeState) {
        Object activeObject = runtimeState.get("activeUiSurfaces");
        require(activeObject instanceof Map<?, ?>,
                "Client runtime state must expose active Native Loader UI surfaces.");
        Map<?, ?> active = (Map<?, ?>) activeObject;
        for (String surfaceType : REQUIRED_SURFACES) {
            requireActiveUiSurfaceRoute(active, surfaceType, true);
        }
        for (String surfaceType : List.of("hud", "main_menu", "loading_screen", "client_overlay", "hud_widget")) {
            requireActiveUiSurfaceVisible(active, surfaceType);
        }
        requireActiveUiSurfaceActivity(active, "terminal", "screen", "screen_open", "terminal.open");
        requireActiveUiSurfaceActivity(active, "holomap", "screen", "screen_open", "holomap.open");
        requireActiveUiSurfaceActivity(active, "holomap", "input", "key_input",
                "key.echoholomap.minimap_cycle_corner");
        requireActiveUiSurfaceActivity(active, "index", "mouse", "mouse", "index.catalog_screen.mouse");
        requireActiveUiSurfaceActivity(active, "hud", "overlayFocus", "overlay_focus",
                "native_loader.overlay_focus");
        requireActiveUiSurfaceActivity(active, "client_overlay", "guiLayer", "gui_layer", "lens.overlay.render");
        requireActiveUiSurfaceActivity(active, "hud_widget", "hudLayer", "hud_layer",
                "hud.compass_indicator.render");
        requireActiveUiSurfaceActivity(active, "main_menu", "screen", "screen_close", "menu.quit");
        requireActiveUiSurfaceActivity(active, "loading_screen", "screen", "screen_unmount",
                "loading.complete");
        requireActiveUiSurfaceCurrentLifecycle(active, "terminal", true, true, true);
        requireActiveUiSurfaceCurrentLifecycle(active, "holomap", true, true, true);
        requireActiveUiSurfaceCurrentLifecycle(active, "main_menu", false, false, false);
        requireActiveUiSurfaceCurrentLifecycle(active, "loading_screen", false, false, false);
        requireActiveUiSurfaceCurrentLifecycle(active, "hud", true, false, true);
        requireActiveUiSurfaceCurrentLifecycle(active, "client_overlay", true, false, true);
        requireActiveUiSurfaceCurrentLifecycle(active, "hud_widget", true, false, true);
        requireActiveUiSurfaceActiveRoute(active, "terminal", "echoterminal", "echoterminal:eui");
        requireActiveUiSurfaceActiveRoute(active, "holomap", "echoholomap", "echoholomap:minimap");
        requireActiveUiSurfaceActiveRoute(active, "client_overlay", "echolens", "echolens:lens_overlay");
        requireActiveUiSurfaceActiveRoute(active, "hud_widget", "echohudcore",
                "echohudcore:compass_indicator");
        requireActiveUiSurfaceActiveRoute(active, "main_menu", "echo-native-loader",
                "echo-native-loader:main_menu");
        requireActiveUiSurfaceActiveRoute(active, "loading_screen", "echo-native-loader",
                "echo-native-loader:loading");
    }

    private static void requireActiveUiSurfaceRoute(Map<?, ?> active, String surfaceType, boolean mounted) {
        Object surfaceObject = active.get(surfaceType);
        require(surfaceObject instanceof Map<?, ?>,
                "Active UI surfaces must include " + surfaceType + ".");
        Map<?, ?> surface = (Map<?, ?>) surfaceObject;
        require("native_loader_client_ui_host".equals(surface.get("source"))
                        && surfaceType.equals(surface.get("surfaceType"))
                        && (!mounted || Boolean.TRUE.equals(surface.get("mounted")))
                        && Boolean.TRUE.equals(surface.get("routeTrustedMutation"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(surface.get("routeStatus"))
                        && surface.get("routeModuleId") instanceof String moduleId
                        && !moduleId.isBlank()
                        && surface.get("routeSurfaceId") instanceof String surfaceId
                        && !surfaceId.isBlank()
                        && Boolean.TRUE.equals(surface.get("nativeClientRouteProcess"))
                        && Boolean.FALSE.equals(surface.get("neoForgeEventOwnershipRequired")),
                "Active UI surface " + surfaceType + " must be a mounted trusted Native Loader route.");
    }

    private static void requireActiveUiSurfaceVisible(Map<?, ?> active, String surfaceType) {
        requireActiveUiSurfaceRoute(active, surfaceType, true);
        Object surfaceObject = active.get(surfaceType);
        Map<?, ?> surface = (Map<?, ?>) surfaceObject;
        require(Boolean.TRUE.equals(surface.get("visible")),
                "Active UI surface " + surfaceType + " must be visible through Native Loader route state.");
    }

    private static void requireActiveUiSurfaceActivity(
            Map<?, ?> active,
            String surfaceType,
            String activityKey,
            String service,
            String actionId
    ) {
        Object surfaceObject = active.get(surfaceType);
        require(surfaceObject instanceof Map<?, ?>,
                "Active UI surfaces must include " + surfaceType + " activity.");
        Map<?, ?> surface = (Map<?, ?>) surfaceObject;
        Object activityObject = surface.get(activityKey);
        require(activityObject instanceof Map<?, ?>,
                "Active UI surface " + surfaceType + " must include " + activityKey + " activity.");
        Map<?, ?> activity = (Map<?, ?>) activityObject;
        require(service.equals(activity.get("service"))
                        || service.equals(activity.get("lastService")),
                "Active UI surface " + surfaceType + " must preserve " + service + " service activity.");
        require(actionId.equals(activity.get("actionId"))
                        || actionId.equals(activity.get("lastActionId")),
                "Active UI surface " + surfaceType + " must preserve " + actionId + " activity.");
        require(EchoNativeLoadStatus.MUTATED.name().equals(activity.get("status"))
                        || EchoNativeLoadStatus.MUTATED.name().equals(activity.get("lastStatus")),
                "Active UI surface " + surfaceType + " activity must preserve MUTATED status.");
    }

    private static void requireActiveUiSurfaceCurrentLifecycle(
            Map<?, ?> active,
            String surfaceType,
            boolean currentMounted,
            boolean currentOpen,
            boolean currentVisible
    ) {
        Object surfaceObject = active.get(surfaceType);
        require(surfaceObject instanceof Map<?, ?>,
                "Active UI surfaces must include " + surfaceType + " current lifecycle state.");
        Map<?, ?> surface = (Map<?, ?>) surfaceObject;
        require(Boolean.valueOf(currentMounted).equals(surface.get("currentMounted"))
                        && Boolean.valueOf(currentOpen).equals(surface.get("currentOpen"))
                        && Boolean.valueOf(currentVisible).equals(surface.get("currentVisible")),
                "Active UI surface " + surfaceType
                        + " must distinguish current lifecycle state from route availability.");
    }

    private static void requireActiveUiSurfaceActiveRoute(
            Map<?, ?> active,
            String surfaceType,
            String moduleId,
            String surfaceId
    ) {
        Object surfaceObject = active.get(surfaceType);
        require(surfaceObject instanceof Map<?, ?>,
                "Active UI surfaces must include " + surfaceType + " route activity.");
        Map<?, ?> surface = (Map<?, ?>) surfaceObject;
        require(moduleId.equals(surface.get("activeRouteModuleId"))
                        && surfaceId.equals(surface.get("activeRouteSurfaceId"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(surface.get("activeRouteStatus"))
                        && Boolean.TRUE.equals(surface.get("activeRouteTrustedMutation"))
                        && Boolean.TRUE.equals(surface.get("activeNativeClientRouteProcess"))
                        && Boolean.FALSE.equals(surface.get("activeNeoForgeEventOwnershipRequired"))
                        && surface.get("activeRoute") instanceof Map<?, ?>,
                "Active UI surface " + surfaceType + " must expose the current route owner "
                        + moduleId + "/" + surfaceId + ".");
    }

    private static Map<String, Boolean> requireRuntimeRouteSnapshots(Map<String, Object> routeHostEvidence) {
        Object stateObject = routeHostEvidence.get("clientRuntimeState");
        require(stateObject instanceof Map<?, ?>,
                "Native Loader UI host must expose client runtime route snapshots.");
        Map<?, ?> state = (Map<?, ?>) stateObject;
        Object mountedObject = state.get("mountedSurfaceRoutes");
        Object visibleObject = state.get("visibleSurfaceRoutes");
        Object hostMountedObject = routeHostEvidence.get("mountedSurfaceRoutes");
        Object hostVisibleObject = routeHostEvidence.get("visibleSurfaceRoutes");
        require(mountedObject instanceof Map<?, ?>
                        && visibleObject instanceof Map<?, ?>
                        && hostMountedObject instanceof Map<?, ?>
                        && hostVisibleObject instanceof Map<?, ?>,
                "Native Loader route-host evidence must include runtime mounted and visible route snapshots.");
        Map<?, ?> mounted = (Map<?, ?>) mountedObject;
        Map<?, ?> visible = (Map<?, ?>) visibleObject;
        Map<?, ?> hostMounted = (Map<?, ?>) hostMountedObject;
        Map<?, ?> hostVisible = (Map<?, ?>) hostVisibleObject;
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (String surfaceType : REQUIRED_SURFACES) {
            requireTrustedRuntimeRoute(mounted, surfaceType, "mounted");
            requireTrustedRuntimeRoute(hostMounted, surfaceType, "host mounted");
            results.put("mounted:" + surfaceType, true);
        }
        for (String surfaceType : List.of("hud", "main_menu", "loading_screen")) {
            requireTrustedRuntimeRoute(visible, surfaceType, "visible");
            requireTrustedRuntimeRoute(hostVisible, surfaceType, "host visible");
            results.put("visible:" + surfaceType, true);
        }
        requireSameRuntimeRouteOwner(mounted, hostMounted, REQUIRED_SURFACES, "mounted");
        requireSameRuntimeRouteOwner(visible, hostVisible, List.of("hud", "main_menu", "loading_screen"), "visible");
        results.put("runtime_state_host_snapshot_agreement", true);
        return Map.copyOf(results);
    }

    private static void requireTrustedRuntimeRoute(Map<?, ?> routes, String surfaceType, String snapshotName) {
        Object routeObject = routes.get(surfaceType);
        require(routeObject instanceof Map<?, ?>,
                "Client runtime " + snapshotName + " routes must include " + surfaceType + ".");
        Map<?, ?> route = (Map<?, ?>) routeObject;
        require(surfaceType.equals(route.get("surfaceType"))
                        && Boolean.TRUE.equals(route.get("trustedMutation"))
                        && "MUTATED".equals(route.get("status"))
                        && route.get("moduleId") instanceof String moduleId
                        && !moduleId.isBlank()
                        && route.get("surfaceId") instanceof String surfaceId
                        && !surfaceId.isBlank()
                        && route.get("evidence") instanceof Map<?, ?> evidence
                        && Boolean.TRUE.equals(evidence.get("nativeClientRouteProcess"))
                        && Boolean.FALSE.equals(evidence.get("neoForgeEventOwnershipRequired"))
                        && Boolean.TRUE.equals(evidence.get("clientRouteMutationSupported")),
                "Client runtime " + snapshotName + " route for " + surfaceType
                        + " must be a trusted Native Loader route, not descriptor-only evidence.");
    }

    private static void requireSameRuntimeRouteOwner(
            Map<?, ?> runtimeRoutes,
            Map<?, ?> hostRoutes,
            List<String> surfaceTypes,
            String snapshotName
    ) {
        for (String surfaceType : surfaceTypes) {
            Object runtimeObject = runtimeRoutes.get(surfaceType);
            Object hostObject = hostRoutes.get(surfaceType);
            require(runtimeObject instanceof Map<?, ?> runtimeRoute
                            && hostObject instanceof Map<?, ?> hostRoute
                            && runtimeRoute.get("moduleId").equals(hostRoute.get("moduleId"))
                            && runtimeRoute.get("surfaceId").equals(hostRoute.get("surfaceId")),
                    "Client runtime " + snapshotName + " route snapshot for " + surfaceType
                            + " must agree with route-host evidence.");
        }
    }

    private static void requireRuntimeService(Map<?, ?> services, String service) {
        require(services.get(service) instanceof Map<?, ?> serviceState
                        && "native_loader_client_ui_host".equals(serviceState.get("source"))
                        && service.equals(serviceState.get("service"))
                        && "MUTATED".equals(serviceState.get("status")),
                "Client runtime state must include MUTATED service snapshot for " + service + ".");
    }

    private static void requireRuntimeServiceField(
            Map<?, ?> services,
            String service,
            String field,
            String expected
    ) {
        Object serviceState = services.get(service);
        require(serviceState instanceof Map<?, ?> state && expected.equals(state.get(field)),
                "Client runtime state " + service + " must record " + field + "=" + expected + ".");
    }

    private static void requireRuntimeServiceMetadata(
            Map<?, ?> services,
            String service,
            String field,
            Object expected
    ) {
        Object serviceState = services.get(service);
        require(serviceState instanceof Map<?, ?> state
                        && state.get("metadata") instanceof Map<?, ?> metadata
                        && expected.equals(metadata.get(field)),
                "Client runtime state " + service + " must preserve "
                        + field + "=" + expected + " metadata.");
    }

    private static void requireRuntimeServiceRoute(
            Map<?, ?> services,
            String service,
            String surfaceType,
            String moduleId,
            String surfaceId
    ) {
        Object serviceState = services.get(service);
        require(serviceState instanceof Map<?, ?> state
                        && runtimeRouteOwnerMatches(state, surfaceType, moduleId, surfaceId),
                "Client runtime service " + service + " must preserve trusted route owner "
                        + moduleId + "/" + surfaceId + ".");
    }

    private static void requireRuntimeInputTarget(
            Map<?, ?> services,
            String service,
            String surfaceType,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        Object serviceState = services.get(service);
        require(serviceState instanceof Map<?, ?> state
                        && runtimeInputTargetMatches(state, surfaceType, actionId, moduleId, surfaceId),
                "Client runtime service " + service + " must preserve route-owned input target "
                        + surfaceType + "/" + actionId + ".");
    }

    private static boolean runtimeInputTargetMatches(
            Map<?, ?> state,
            String surfaceType,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        return surfaceType.equals(state.get("inputTargetSurfaceType"))
                && actionId.equals(state.get("inputTargetActionId"))
                && EchoNativeLoadStatus.MUTATED.name().equals(state.get("inputTargetStatus"))
                && moduleId.equals(state.get("inputRouteModuleId"))
                && surfaceId.equals(state.get("inputRouteSurfaceId"))
                && Boolean.TRUE.equals(state.get("inputRouteTrustedMutation"))
                && EchoNativeLoadStatus.MUTATED.name().equals(state.get("inputRouteStatus"))
                && state.get("inputTargets") instanceof List<?> targets
                && !targets.isEmpty()
                && state.get("inputDispatch") instanceof Map<?, ?> dispatch
                && EchoNativeLoadStatus.MUTATED.name().equals(dispatch.get("status"));
    }

    private static boolean runtimeRouteOwnerMatches(
            Map<?, ?> state,
            String surfaceType,
            String moduleId,
            String surfaceId
    ) {
        return surfaceType.equals(state.get("surfaceType"))
                && moduleId.equals(state.get("routeModuleId"))
                && surfaceId.equals(state.get("routeSurfaceId"))
                && Boolean.TRUE.equals(state.get("routeTrustedMutation"))
                && "MUTATED".equals(state.get("routeStatus"))
                && Boolean.TRUE.equals(state.get("nativeClientRouteProcess"))
                && Boolean.FALSE.equals(state.get("neoForgeEventOwnershipRequired"))
                && state.get("route") instanceof Map<?, ?> route
                && surfaceType.equals(route.get("surfaceType"));
    }

    private static void requireRuntimeScreen(
            Map<?, ?> screens,
            String surfaceType,
            String lastService,
            boolean open,
            boolean mounted
    ) {
        require(screens.get(surfaceType) instanceof Map<?, ?> screen
                        && surfaceType.equals(screen.get("surfaceType"))
                        && lastService.equals(screen.get("lastService"))
                        && Boolean.valueOf(open).equals(screen.get("open"))
                        && Boolean.valueOf(mounted).equals(screen.get("mounted")),
                "Client runtime state must track " + surfaceType + " screen lifecycle.");
    }

    private static void requireRuntimeScreenMetadata(
            Map<?, ?> screens,
            String surfaceType,
            String field,
            String expected
    ) {
        Object screenObject = screens.get(surfaceType);
        require(screenObject instanceof Map<?, ?> screen
                        && screen.get("metadata") instanceof Map<?, ?> metadata
                        && expected.equals(metadata.get(field)),
                "Client runtime state must preserve " + field + " metadata for " + surfaceType + ".");
    }

    private static void requireRuntimeScreenRoute(
            Map<?, ?> screens,
            String surfaceType,
            String moduleId,
            String surfaceId
    ) {
        Object screenObject = screens.get(surfaceType);
        require(screenObject instanceof Map<?, ?> screen
                        && moduleId.equals(screen.get("routeModuleId"))
                        && surfaceId.equals(screen.get("routeSurfaceId"))
                        && Boolean.TRUE.equals(screen.get("routeTrustedMutation"))
                        && "MUTATED".equals(screen.get("routeStatus"))
                        && Boolean.TRUE.equals(screen.get("nativeClientRouteProcess"))
                        && Boolean.FALSE.equals(screen.get("neoForgeEventOwnershipRequired"))
                        && screen.get("route") instanceof Map<?, ?> route
                        && surfaceType.equals(route.get("surfaceType")),
                "Client runtime screen state for " + surfaceType
                        + " must preserve trusted Native Loader route owner evidence.");
    }

    private static void requireActionDispatchEvidence(Map<String, Object> routeHostEvidence) {
        Object evidenceObject = routeHostEvidence.get("actionDispatchEvidence");
        require(evidenceObject instanceof Map<?, ?>,
                "Native route table must expose action dispatch evidence.");
        Map<?, ?> evidence = (Map<?, ?>) evidenceObject;
        Object eventsObject = evidence.get("events");
        require(eventsObject instanceof List<?>,
                "Native route table action dispatch evidence must include dispatch events.");
        List<?> events = (List<?>) eventsObject;
        requireActionDispatchEvent(events, "terminal", "terminal.open", "MUTATED", "handled");
        requireActionDispatchEvent(events, "index", "index.catalog", "MUTATED", "handled");
        requireActionDispatchEvent(events, "client_overlay", "lens.overlay.render", "MUTATED", "handled");
        requireActionDispatchEvent(events, "hud", "hud.render", "MUTATED", "handled");
        requireActionDispatchEvent(events, "hud", "native_loader.overlay_focus", "MUTATED", "handled");
        requireActionDispatchEvent(events, "loading_screen", "loading.open", "MUTATED", "handled");
        requireActionDispatchEvent(events, "loading_screen", "loading.render", "MUTATED", "handled");
        requireActionDispatchEvent(events, "loading_screen", "loading.complete", "MUTATED", "handled");
        requireActionDispatchEvent(events, "main_menu", "menu.quit", "MUTATED", "handled");
        requireActionDispatchEvent(events, "index", "index.unowned_side_event_fallback", "UNSUPPORTED", "unknown_action");
        requireActionDispatchOwnerEvent(events, "client_overlay", "terminal.mission_hud.render",
                "echoterminal", "echoterminal:hud_overlay", "echoterminal:hud_overlay");
        requireActionDispatchOwnerEvent(events, "client_overlay", "index.inventory_overlay_render",
                "echoindex", "echoindex:inventory_overlay", "echoindex:inventory_overlay");
        requireActionDispatchOwnerEvent(events, "client_overlay", "lens.overlay.render",
                "echolens", "echolens:lens_overlay", "echolens:lens_overlay");
        requireActionDispatchOwnerEvent(events, "holomap", "holomap.minimap.render",
                "echoholomap", "echoholomap:minimap", "echoholomap:minimap:holomap.minimap.render");
        requireActionDispatchOwnerEvent(events, "hud_widget", "hud.mission_tracker.render",
                "echohudcore", "echohudcore:mission_tracker",
                "echohudcore:mission_tracker:hud.mission_tracker.render");
        requireActionDispatchOwnerEvent(events, "hud_layout", "hud.screen_safe_area.resolve",
                "echohudcore", "echohudcore:screen_safe_area",
                "echohudcore:screen_safe_area:hud.screen_safe_area.resolve");
        requireActionDispatchSummary(evidence);
    }

    private static void requireActionDispatchSummary(Map<?, ?> evidence) {
        Object summaryObject = evidence.get("summary");
        require(summaryObject instanceof Map<?, ?>,
                "Native route table action dispatch evidence must expose a compact summary.");
        Map<?, ?> summary = (Map<?, ?>) summaryObject;
        require(summary.get("statusCounts") instanceof Map<?, ?> statusCounts
                        && statusCounts.containsKey(EchoNativeLoadStatus.MUTATED.name())
                        && statusCounts.containsKey(EchoNativeLoadStatus.UNSUPPORTED.name()),
                "Native route table action dispatch summary must include mutated and unsupported status counts.");
        require(summary.get("metadataSourceCounts") instanceof Map<?, ?> metadataSourceCounts
                        && metadataSourceCounts.get("agent2_direct_public_sdk_probe") instanceof Number directDispatchCount
                        && directDispatchCount.intValue() >= 11,
                "Native route table action dispatch summary must include direct public SDK source counts.");
        require(summary.get("latestByMetadataSource") instanceof Map<?, ?> latestBySource
                        && latestBySource.get("agent2_direct_public_sdk_probe") instanceof Map<?, ?> directDispatch
                        && "loading_screen".equals(directDispatch.get("surfaceType"))
                        && "loading.complete".equals(directDispatch.get("actionId"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(directDispatch.get("status")),
                "Native route table action dispatch summary must expose latest direct public SDK route dispatch.");
        require(summary.get("latestHandledByMetadataSource") instanceof Map<?, ?> latestHandledBySource
                        && latestHandledBySource.get("agent2_direct_public_sdk_probe:loading_screen") instanceof Map<?, ?> directTarget
                        && "loading.complete".equals(directTarget.get("actionId"))
                        && "echo-native-loader".equals(directTarget.get("routeModuleId"))
                        && "echo-native-loader:loading".equals(directTarget.get("routeSurfaceId")),
                "Native route table action dispatch summary must expose latest direct public SDK handled target.");
        Object latestHandledObject = summary.get("latestHandledBySurface");
        require(latestHandledObject instanceof Map<?, ?>,
                "Native route table action dispatch summary must include latest handled events by surface.");
        Map<?, ?> latestHandled = (Map<?, ?>) latestHandledObject;
        requireActionSummaryEvent(latestHandled, "terminal", "terminal.open", "echoterminal", "echoterminal:eui");
        requireActionSummaryEvent(latestHandled, "index", "index.bookmark", "echoindex", "echoindex:index");
        requireActionSummaryEvent(latestHandled, "holomap", "holomap.cycle_corner",
                "echoholomap", "echoholomap:minimap");
        requireActionSummaryEventOneOf(latestHandled, "client_overlay", List.of(
                        "terminal.mission_hud.tick",
                        "terminal.discovery_toast.tick"
                ),
                "echoterminal", "echoterminal:hud_overlay");
        requireActionSummaryEvent(latestHandled, "hud", "native_loader.overlay_focus",
                "echohudcore", "echohudcore:native_hud");
        requireActionSummaryEvent(latestHandled, "hud_widget", "hud.compass_indicator.render",
                "echohudcore", "echohudcore:compass_indicator");
        requireActionSummaryEvent(latestHandled, "hud_layout", "hud.screen_safe_area.resolve",
                "echohudcore", "echohudcore:screen_safe_area");
        requireActionSummaryEvent(latestHandled, "main_menu", "menu.new_run",
                "echo-native-loader", "echo-native-loader:main_menu");
        requireActionSummaryEvent(latestHandled, "loading_screen", "loading.complete",
                "echo-native-loader", "echo-native-loader:loading");
        Object latestUnsupportedBySurfaceObject = summary.get("latestUnsupportedBySurface");
        require(latestUnsupportedBySurfaceObject instanceof Map<?, ?> latestUnsupportedBySurface
                        && latestUnsupportedBySurface.get("index") instanceof Map<?, ?> index
                        && "index.unowned_side_event_fallback".equals(index.get("actionId"))
                        && EchoNativeLoadStatus.UNSUPPORTED.name().equals(index.get("status")),
                "Native route table action dispatch summary must preserve latest unsupported side-event probe.");
    }

    private static void requireActionSummaryEvent(
            Map<?, ?> latestHandled,
            String surfaceType,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        Object eventObject = latestHandled.get(surfaceType);
        require(eventObject instanceof Map<?, ?> event
                        && surfaceType.equals(event.get("surfaceType"))
                        && actionId.equals(event.get("actionId"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(event.get("status"))
                        && Boolean.TRUE.equals(event.get("handled"))
                        && moduleId.equals(event.get("routeModuleId"))
                        && surfaceId.equals(event.get("routeSurfaceId")),
                "Native route table action dispatch summary must expose latest handled owner for "
                        + surfaceType + "/" + actionId + ".");
    }

    private static void requireActionSummaryEventOneOf(
            Map<?, ?> latestHandled,
            String surfaceType,
            List<String> actionIds,
            String moduleId,
            String surfaceId
    ) {
        Object eventObject = latestHandled.get(surfaceType);
        require(eventObject instanceof Map<?, ?> event
                        && surfaceType.equals(event.get("surfaceType"))
                        && actionIds.contains(String.valueOf(event.get("actionId")))
                        && EchoNativeLoadStatus.MUTATED.name().equals(event.get("status"))
                        && Boolean.TRUE.equals(event.get("handled"))
                        && moduleId.equals(event.get("routeModuleId"))
                        && surfaceId.equals(event.get("routeSurfaceId")),
                "Native route table action dispatch summary must expose latest handled owner for "
                        + surfaceType + "/" + actionIds + ".");
    }

    private static void requireLifecycleEventEvidence(Map<String, Object> routeHostEvidence) {
        Object evidenceObject = routeHostEvidence.get("lifecycleEventEvidence");
        require(evidenceObject instanceof Map<?, ?>,
                "Native route table must expose lifecycle event evidence.");
        Map<?, ?> evidence = (Map<?, ?>) evidenceObject;
        require(evidence.get("eventCount") instanceof Integer eventCount && eventCount > 0,
                "Native route table lifecycle event evidence must include recorded events.");
        require(evidence.get("eventsBySurface") instanceof Map<?, ?>,
                "Native route table lifecycle event evidence must keep per-surface event audit trails.");
        Object summaryObject = evidence.get("summary");
        require(summaryObject instanceof Map<?, ?>,
                "Native route table lifecycle event evidence must expose a compact summary.");
        Map<?, ?> summary = (Map<?, ?>) summaryObject;
        require(summary.get("eventCountBySurface") instanceof Map<?, ?> eventCountBySurface
                        && eventCountBySurface.keySet().containsAll(List.of(
                                "terminal",
                                "index",
                                "holomap",
                                "client_overlay",
                                "hud",
                                "hud_widget",
                                "main_menu",
                                "loading_screen"
                        )),
                "Native route table lifecycle summary must count product UI surfaces.");
        require(summary.get("phaseCounts") instanceof Map<?, ?> phaseCounts
                        && phaseCounts.keySet().containsAll(List.of(
                                "action",
                                "input",
                                "open",
                                "close",
                                "render",
                                "unmount"
                        )),
                "Native route table lifecycle summary must count screen, input, render, and action phases.");
        require(summary.get("metadataSourceCounts") instanceof Map<?, ?> metadataSourceCounts
                        && metadataSourceCounts.get("agent2_direct_public_sdk_lifecycle_probe") instanceof Number directLifecycleCount
                        && directLifecycleCount.intValue() >= 7,
                "Native route table lifecycle summary must include direct public SDK source counts.");
        require(summary.get("latestByMetadataSource") instanceof Map<?, ?> latestBySource
                        && latestBySource.get("agent2_direct_public_sdk_lifecycle_probe") instanceof Map<?, ?> directLifecycle
                        && directLifecycle.get("metadata") instanceof Map<?, ?> directLifecycleMetadata
                        && "direct_lifecycle_publication".equals(directLifecycleMetadata.get("eventType")),
                "Native route table lifecycle summary must expose latest direct public SDK lifecycle publication.");
        require(summary.get("latestByMetadataSourceSurface") instanceof Map<?, ?> latestBySourceSurface
                        && latestBySourceSurface.get("agent2_direct_public_sdk_lifecycle_probe:loading_screen") instanceof Map<?, ?> directLoading
                        && "loading.render".equals(directLoading.get("actionId"))
                        && "echo-native-loader".equals(directLoading.get("routeModuleId"))
                        && "echo-native-loader:loading".equals(directLoading.get("routeSurfaceId")),
                "Native route table lifecycle summary must expose direct public SDK lifecycle target ownership.");
        Object latestBySurfaceActionObject = summary.get("latestBySurfaceAction");
        require(latestBySurfaceActionObject instanceof Map<?, ?>,
                "Native route table lifecycle summary must include latest lifecycle events by surface and action.");
        Map<?, ?> latestBySurfaceAction = (Map<?, ?>) latestBySurfaceActionObject;
        requireScreenLifecycleHostSummaryEvent(latestBySurfaceAction, "terminal:terminal.open",
                "terminal", "terminal.open", "echoterminal", "echoterminal:eui");
        requireScreenLifecycleHostSummaryEvent(latestBySurfaceAction, "index:index.catalog",
                "index", "index.catalog", "echoindex", "echoindex:index");
        requireScreenLifecycleHostSummaryEvent(latestBySurfaceAction, "main_menu:menu.open",
                "main_menu", "menu.open", "echo-native-loader", "echo-native-loader:main_menu");
        Object latestBySurfacePhaseObject = summary.get("latestBySurfacePhase");
        require(latestBySurfacePhaseObject instanceof Map<?, ?>,
                "Native route table lifecycle summary must include latest lifecycle events by surface and phase.");
        Map<?, ?> latestBySurfacePhase = (Map<?, ?>) latestBySurfacePhaseObject;
        requireLifecycleSummaryEvent(latestBySurfacePhase, "terminal:open",
                "terminal", "open", "terminal.open", "echoterminal", "echoterminal:eui");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "terminal:input",
                "terminal", "input", "terminal.open", "echoterminal", "echoterminal:eui");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "index:open",
                "index", "open", "index.catalog", "echoindex", "echoindex:index");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "holomap:render",
                "holomap", "render", "holomap.minimap.render", "echoholomap", "echoholomap:minimap");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "holomap:input",
                "holomap", "input", "holomap.cycle_corner", "echoholomap", "echoholomap:minimap");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "client_overlay:render",
                "client_overlay", "render", "lens.overlay.render", "echolens", "echolens:lens_overlay");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "hud:render",
                "hud", "render", "hud.render", "echohudcore", "echohudcore:native_hud");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "hud:focus",
                "hud", "focus", "native_loader.overlay_focus", "echohudcore", "echohudcore:native_hud");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "hud_widget:render",
                "hud_widget", "render", "hud.compass_indicator.render",
                "echohudcore", "echohudcore:compass_indicator");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "main_menu:close",
                "main_menu", "close", "menu.quit",
                "echo-native-loader", "echo-native-loader:main_menu");
        requireLifecycleSummaryEvent(latestBySurfacePhase, "loading_screen:unmount",
                "loading_screen", "unmount", "loading.complete",
                "echo-native-loader", "echo-native-loader:loading");
    }

    private static void requireLifecycleSummaryEvent(
            Map<?, ?> latestBySurfacePhase,
            String summaryKey,
            String surfaceType,
            String phase,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        Object eventObject = latestBySurfacePhase.get(summaryKey);
        require(eventObject instanceof Map<?, ?> event
                        && surfaceType.equals(event.get("surfaceType"))
                        && phase.equals(event.get("phase"))
                        && actionId.equals(event.get("actionId"))
                        && moduleId.equals(event.get("routeModuleId"))
                        && surfaceId.equals(event.get("routeSurfaceId"))
                        && Boolean.TRUE.equals(event.get("routeTrustedMutation"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(event.get("routeStatus"))
                        && Boolean.TRUE.equals(event.get("nativeClientRouteProcess"))
                        && Boolean.FALSE.equals(event.get("neoForgeEventOwnershipRequired"))
                        && Boolean.TRUE.equals(event.get("clientRouteMutationSupported"))
                        && event.get("route") instanceof Map<?, ?>,
                "Native route table lifecycle summary must expose route-owned lifecycle event "
                        + summaryKey + "/" + actionId + ".");
    }

    private static void requireScreenLifecycleHostSummaryEvent(
            Map<?, ?> latestBySurfaceAction,
            String summaryKey,
            String surfaceType,
            String actionId,
            String moduleId,
            String surfaceId
    ) {
        Object eventObject = latestBySurfaceAction.get(summaryKey);
        require(eventObject instanceof Map<?, ?> event
                        && surfaceType.equals(event.get("surfaceType"))
                        && actionId.equals(event.get("actionId"))
                        && moduleId.equals(event.get("routeModuleId"))
                        && surfaceId.equals(event.get("routeSurfaceId"))
                        && "screen_lifecycle".equals(event.get("nativeLoaderUiHostService"))
                        && surfaceType.equals(event.get("nativeLoaderUiHostSurface"))
                        && actionId.equals(event.get("nativeLoaderUiHostAction"))
                        && Boolean.TRUE.equals(event.get("nativeLoaderScreenLifecycleHandoff")),
                "Native route table lifecycle summary must expose public screen lifecycle host handoff for "
                        + summaryKey + ".");
    }

    private static Map<String, Boolean> requireDirectPublicSdkDispatchEvidence(Map<String, Object> routeHostEvidence) {
        Object evidenceObject = routeHostEvidence.get("actionDispatchEvidence");
        require(evidenceObject instanceof Map<?, ?>,
                "Native route table must expose action dispatch evidence for direct public SDK dispatch.");
        Map<?, ?> evidence = (Map<?, ?>) evidenceObject;
        Object eventsObject = evidence.get("events");
        require(eventsObject instanceof List<?>,
                "Native route table action dispatch evidence must include direct public SDK dispatch events.");
        List<?> events = (List<?>) eventsObject;
        Map<String, Boolean> results = new LinkedHashMap<>();
        requireDirectPublicSdkDispatchEvent(results, events, "terminal", "terminal.open");
        requireDirectPublicSdkDispatchEvent(results, events, "index", "index.catalog");
        requireDirectPublicSdkDispatchEvent(results, events, "lens", "lens.deep_scan");
        requireDirectPublicSdkDispatchEvent(results, events, "holomap", "holomap.open");
        requireDirectPublicSdkDispatchEvent(results, events, "hud", "hud.render");
        requireDirectPublicSdkDispatchEvent(results, events, "hud", "hud.update_snapshot");
        requireDirectPublicSdkDispatchEvent(results, events, "hud", "native_loader.overlay_focus");
        requireDirectPublicSdkDispatchEvent(results, events, "hud_widget", "hud.mission_tracker.render");
        requireDirectPublicSdkDispatchEvent(results, events, "hud_widget", "hud.hazard_readout.render");
        requireDirectPublicSdkDispatchEvent(results, events, "hud_widget", "hud.compass_indicator.render");
        requireDirectPublicSdkDispatchEvent(results, events, "hud_layout", "hud.screen_safe_area.resolve");
        requireDirectPublicSdkDispatchEvent(results, events, "main_menu", "menu.open");
        requireDirectPublicSdkDispatchEvent(results, events, "main_menu", "menu.new_run");
        requireDirectPublicSdkDispatchEvent(results, events, "loading_screen", "loading.open");
        requireDirectPublicSdkDispatchEvent(results, events, "loading_screen", "loading.render");
        requireDirectPublicSdkDispatchEvent(results, events, "loading_screen", "loading.progress");
        requireDirectPublicSdkDispatchEvent(results, events, "loading_screen", "loading.complete");
        requireDirectPublicSdkProgressDispatchEvent(results, events);
        return Map.copyOf(results);
    }

    private static void requireDirectPublicSdkDispatchEvent(
            Map<String, Boolean> results,
            List<?> events,
            String surfaceType,
            String actionId
    ) {
        boolean found = false;
        for (Object eventObject : events) {
            if (!(eventObject instanceof Map<?, ?> event)) {
                continue;
            }
            if (!surfaceType.equals(event.get("surfaceType"))
                    || !actionId.equals(event.get("actionId"))
                    || !"MUTATED".equals(event.get("status"))
                    || !"handled".equals(event.get("outcome"))) {
                continue;
            }
            if (event.get("metadata") instanceof Map<?, ?> metadata
                    && "agent2_direct_public_sdk_probe".equals(metadata.get("source"))
                    && "direct_route_dispatch".equals(metadata.get("eventType"))
                    && Boolean.FALSE.equals(metadata.get("neoForgeEventOwnershipRequired"))) {
                found = true;
                break;
            }
        }
        require(found, "Direct public SDK dispatch must mutate " + surfaceType + "/" + actionId
                + " without NeoForge event ownership.");
        results.put(surfaceType + ":" + actionId, true);
    }

    private static void requireDirectPublicSdkProgressDispatchEvent(
            Map<String, Boolean> results,
            List<?> events
    ) {
        boolean found = false;
        for (Object eventObject : events) {
            if (!(eventObject instanceof Map<?, ?> event)) {
                continue;
            }
            if (!"loading_screen".equals(event.get("surfaceType"))
                    || !"loading.progress".equals(event.get("actionId"))
                    || !"MUTATED".equals(event.get("status"))
                    || !"handled".equals(event.get("outcome"))) {
                continue;
            }
            if (event.get("metadata") instanceof Map<?, ?> metadata
                    && "agent2_direct_public_sdk_probe".equals(metadata.get("source"))
                    && "direct_route_dispatch".equals(metadata.get("eventType"))
                    && Boolean.FALSE.equals(metadata.get("neoForgeEventOwnershipRequired"))
                    && metadata.get("progress") instanceof Number progress
                    && progress.doubleValue() >= 0.42D
                    && "Direct SDK loading progress".equals(metadata.get("label"))) {
                found = true;
                break;
            }
        }
        require(found, "Direct public SDK dispatch must carry loading progress metadata into loading_screen/loading.progress.");
        results.put("loading_screen:loading.progress.metadata", true);
    }

    private static void requireHostServiceEvent(
            List<?> events,
            String service,
            String surfaceType,
            String actionId
    ) {
        boolean found = false;
        for (Object eventObject : events) {
            if (eventObject instanceof Map<?, ?> event
                    && service.equals(event.get("service"))
                    && surfaceType.equals(event.get("surfaceType"))
                    && actionId.equals(event.get("actionId"))
                    && "MUTATED".equals(event.get("status"))
                    && "native_loader_client_ui_host".equals(event.get("source"))
                    && hostServiceMetadataMatches(event.get("metadata"), service)) {
                found = true;
                break;
            }
        }
        require(found, "Native Loader UI host-service events must include "
                + service + "/" + surfaceType + "/" + actionId + ".");
    }

    private static void requireHostServiceMetadata(
            List<?> events,
            String service,
            String surfaceType,
            String actionId,
            String field,
            Object expected
    ) {
        boolean found = false;
        for (Object eventObject : events) {
            if (eventObject instanceof Map<?, ?> event
                    && service.equals(event.get("service"))
                    && surfaceType.equals(event.get("surfaceType"))
                    && actionId.equals(event.get("actionId"))
                    && event.get("metadata") instanceof Map<?, ?> metadata
                    && metadataValueEquals(expected, metadata.get(field))) {
                found = true;
                break;
            }
        }
        require(found, "Native Loader UI host-service event "
                + service + "/" + surfaceType + "/" + actionId
                + " must preserve " + field + "=" + expected + ".");
    }

    private static boolean metadataValueEquals(Object expected, Object actual) {
        if (expected instanceof Number expectedNumber && actual instanceof Number actualNumber) {
            return Double.compare(expectedNumber.doubleValue(), actualNumber.doubleValue()) == 0;
        }
        return expected == null ? actual == null : expected.equals(actual);
    }

    private static boolean hostServiceMetadataMatches(Object metadataObject, String service) {
        if (!(metadataObject instanceof Map<?, ?> metadata)) {
            return false;
        }
        String expectedService = service == null ? "" : service;
        if (expectedService.startsWith("screen_")) {
            expectedService = "screen_lifecycle";
        }
        return Set.of(
                        "native_loader_client_ui_host",
                        "agent2_direct_public_sdk_probe",
                        "agent2_direct_public_sdk_input_probe"
                ).contains(metadata.get("source"))
                && expectedService.equals(metadata.get("service"));
    }

    private static void requireActionDispatchEvent(
            List<?> events,
            String surfaceType,
            String actionId,
            String status,
            String outcome
    ) {
        boolean found = false;
        for (Object eventObject : events) {
            if (eventObject instanceof Map<?, ?> event
                    && surfaceType.equals(event.get("surfaceType"))
                    && actionId.equals(event.get("actionId"))
                    && status.equals(event.get("status"))
                    && outcome.equals(event.get("outcome"))
                    && "native_loader_route_dispatch".equals(event.get("source"))) {
                found = true;
                break;
            }
        }
        require(found, "Native route table action dispatch evidence must include "
                + surfaceType + "/" + actionId + " as " + status + "/" + outcome + ".");
    }

    private static void requireActionDispatchOwnerEvent(
            List<?> events,
            String surfaceType,
            String actionId,
            String moduleId,
            String surfaceId,
            String handlerId
    ) {
        boolean found = false;
        for (Object eventObject : events) {
            if (eventObject instanceof Map<?, ?> event
                    && surfaceType.equals(event.get("surfaceType"))
                    && actionId.equals(event.get("actionId"))
                    && "MUTATED".equals(event.get("status"))
                    && moduleId.equals(event.get("routeModuleId"))
                    && surfaceId.equals(event.get("routeSurfaceId"))
                    && handlerId.equals(event.get("handledHandlerId"))
                    && Boolean.TRUE.equals(event.get("ownerPreferredHandlers"))
                    && event.get("ownerHandlerIds") instanceof List<?> ownerHandlerIds
                    && ownerHandlerIds.contains(handlerId)
                    && event.get("handlerDispatchOrder") instanceof List<?> handlerDispatchOrder
                    && !handlerDispatchOrder.isEmpty()
                    && handlerId.equals(handlerDispatchOrder.get(0))
                    && "native_loader_route_dispatch".equals(event.get("source"))) {
                found = true;
                break;
            }
        }
        require(found, "Native route table action dispatch evidence must prove owner "
                + moduleId + "/" + surfaceId + ", owner-preferred handler order, and handler " + handlerId
                + " for " + surfaceType + "/" + actionId + ".");
    }

    private static void requireLiveClientBridgeServiceEvent(
            List<?> events,
            String service,
            String surfaceType,
            String actionId
    ) {
        boolean found = false;
        for (Object eventObject : events) {
            if (eventObject instanceof Map<?, ?> event
                    && service.equals(event.get("service"))
                    && surfaceType.equals(event.get("surfaceType"))
                    && actionId.equals(event.get("actionId"))
                    && "MUTATED".equals(event.get("status"))
                    && Boolean.TRUE.equals(event.get("nativeLoaderOwnsClientHostServices"))
                    && Boolean.TRUE.equals(event.get("neoForgeClientEventsCompatibilityAdaptersOnly"))
                    && "native_loader_default_product_client_bridge".equals(event.get("source"))) {
                found = true;
                break;
            }
        }
        require(found, "Default product NativeLoaderLiveClientBridge service events must include "
                + service + "/" + surfaceType + "/" + actionId + ".");
    }

    private static void requireTerminalScreenInputMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> charMetadata = dispatchMetadata.getOrDefault("terminal.screen.char_typed", Map.of());
        require("character_typed".equals(charMetadata.get("eventType"))
                        && String.valueOf(charMetadata.getOrDefault("characterEvent", "")).contains("codePoint=65"),
                "Terminal character input route must receive native dispatch metadata.");
        Map<String, Object> scrollMetadata = dispatchMetadata.getOrDefault("terminal.screen.mouse_scroll", Map.of());
        require("mouse".equals(scrollMetadata.get("service"))
                        && "native_loader_window_pump".equals(scrollMetadata.get("mouseSource"))
                        && "scroll".equals(scrollMetadata.get("phase"))
                        && scrollMetadata.containsKey("scrollY"),
                "Terminal mouse-scroll route must receive native mouse metadata.");
        Map<String, Object> frameMetadata = dispatchMetadata.getOrDefault("terminal.screen.frame.render", Map.of());
        require("native_loader_gui_layer".equals(frameMetadata.get("source"))
                        && "neoforge_compatibility_adapter".equals(frameMetadata.get("forwardedFrom"))
                        && "screen_render_post".equals(frameMetadata.get("eventType"))
                        && frameMetadata.containsKey("partialTick"),
                "Terminal RenderCore screen-frame route must receive Native Loader GUI-layer metadata.");
    }

    private static void requireTerminalScreenCoreMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> mouse = dispatchMetadata.getOrDefault("terminal.screencore.mouse", Map.of());
        require("native_screen_lifecycle".equals(mouse.get("source"))
                        && "terminal_screencore_mouse_input".equals(mouse.get("eventType"))
                        && "drag".equals(mouse.get("phase"))
                        && mouse.containsKey("screenClass")
                        && mouse.containsKey("mouseX")
                        && mouse.containsKey("button")
                        && mouse.containsKey("dragY"),
                "Terminal ScreenCore mouse route must receive native screen lifecycle metadata.");
        Map<String, Object> scroll = dispatchMetadata.getOrDefault("terminal.screencore.scroll", Map.of());
        require("native_screen_lifecycle".equals(scroll.get("source"))
                        && "terminal_screencore_scroll_input".equals(scroll.get("eventType"))
                        && scroll.containsKey("screenClass")
                        && scroll.containsKey("scrollY")
                        && scroll.containsKey("mouseY"),
                "Terminal ScreenCore scroll route must receive native screen lifecycle metadata.");
        Map<String, Object> key = dispatchMetadata.getOrDefault("terminal.screencore.key", Map.of());
        require("native_screen_lifecycle".equals(key.get("source"))
                        && "terminal_screencore_key_input".equals(key.get("eventType"))
                        && Boolean.TRUE.equals(key.get("openTerminalKey"))
                        && key.containsKey("key")
                        && key.containsKey("screenClass"),
                "Terminal ScreenCore key route must receive native screen lifecycle metadata.");
        Map<String, Object> character = dispatchMetadata.getOrDefault("terminal.screencore.char", Map.of());
        require("native_screen_lifecycle".equals(character.get("source"))
                        && "terminal_screencore_character_typed".equals(character.get("eventType"))
                        && Boolean.TRUE.equals(character.get("allowedChatCharacter"))
                        && character.containsKey("character")
                        && character.containsKey("screenClass"),
                "Terminal ScreenCore character route must receive native screen lifecycle metadata.");
        Map<String, Object> action = dispatchMetadata.getOrDefault("terminal.screencore.action", Map.of());
        require("native_screencore_action".equals(action.get("source"))
                        && "terminal_screencore_action".equals(action.get("eventType"))
                        && "TerminalScreenCoreActionIds".equals(action.get("actionCatalog"))
                        && "terminal.open_mission".equals(action.get("screenCoreActionId"))
                        && action.containsKey("pageId")
                        && action.containsKey("actionValue"),
                "Terminal ScreenCore catalog actions must dispatch through the native Terminal route with action metadata.");
    }

    private static void requireOverlayAdapterMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> missionRender = dispatchMetadata.getOrDefault("terminal.mission_hud.render", Map.of());
        require("native_loader_client_ui_host".equals(missionRender.get("source"))
                        && "gui_layer".equals(missionRender.get("service"))
                        && "native_loader_window_pump".equals(missionRender.get("frameSource"))
                        && missionRender.containsKey("partialTick"),
                "Terminal mission HUD render route must receive Native Loader GUI-layer host metadata.");
        Map<String, Object> discoveryTick = dispatchMetadata.getOrDefault("terminal.discovery_toast.tick", Map.of());
        require(("native_loader_client_ui_host".equals(discoveryTick.get("source"))
                        || "native_loader_tick_service".equals(discoveryTick.get("source"))
                        || "agent2_direct_public_sdk_probe".equals(discoveryTick.get("source")))
                        && "tick".equals(discoveryTick.get("service"))
                        && ("client_tick_post".equals(discoveryTick.get("eventType"))
                        || "direct_public_sdk_tick".equals(discoveryTick.get("eventType")))
                        && Boolean.TRUE.equals(discoveryTick.get("tickRouteDispatch"))
                        && "terminal.discovery_toast.tick".equals(discoveryTick.get("tickRouteActionId")),
                "Terminal discovery toast tick route must receive Native Loader tick host metadata.");
        Map<String, Object> discoveryRender = dispatchMetadata.getOrDefault("terminal.discovery_toast.render", Map.of());
        require("native_loader_gui_layer".equals(discoveryRender.get("source"))
                        && "neoforge_compatibility_adapter".equals(discoveryRender.get("forwardedFrom"))
                        && "render_gui_post".equals(discoveryRender.get("eventType"))
                        && discoveryRender.containsKey("partialTick"),
                "Terminal discovery toast render route must receive Native Loader GUI-layer render metadata.");
        Map<String, Object> indexInput = dispatchMetadata.getOrDefault(
                "index.inventory_overlay_input:mouse_scrolled",
                dispatchMetadata.getOrDefault("index.inventory_overlay_input", Map.of()));
        Object eventMetadata = indexInput.get("eventMetadata");
        require("native_loader_overlay_input".equals(indexInput.get("source"))
                        && "neoforge_compatibility_adapter".equals(indexInput.get("forwardedFrom"))
                        && "mouse_scrolled".equals(indexInput.get("eventType"))
                        && eventMetadata instanceof Map<?, ?> nested
                        && nested.containsKey("scrollDeltaY"),
                "Index overlay input route must receive Native Loader overlay input metadata.");
        Map<String, Object> indexClick = dispatchMetadata.getOrDefault("index.inventory_overlay_input:mouse_clicked", Map.of());
        Object clickMetadata = indexClick.get("eventMetadata");
        require("native_loader_overlay_input".equals(indexClick.get("source"))
                        && "neoforge_compatibility_adapter".equals(indexClick.get("forwardedFrom"))
                        && "mouse_clicked".equals(indexClick.get("eventType"))
                        && clickMetadata instanceof Map<?, ?> click
                        && click.containsKey("button")
                        && click.containsKey("mouseX"),
                "Index overlay click route must receive route-owned mouse metadata.");
        Map<String, Object> indexKey = dispatchMetadata.getOrDefault("index.inventory_overlay_input:key_pressed", Map.of());
        Object keyMetadata = indexKey.get("eventMetadata");
        require("native_loader_overlay_input".equals(indexKey.get("source"))
                        && "neoforge_compatibility_adapter".equals(indexKey.get("forwardedFrom"))
                        && "key_pressed".equals(indexKey.get("eventType"))
                        && keyMetadata instanceof Map<?, ?> key
                        && Boolean.TRUE.equals(key.get("recipeKey"))
                        && key.containsKey("key"),
                "Index overlay key route must receive route-owned key metadata.");
        Map<String, Object> indexChar = dispatchMetadata.getOrDefault("index.inventory_overlay_input:character_typed", Map.of());
        Object charMetadata = indexChar.get("eventMetadata");
        require("native_loader_overlay_input".equals(indexChar.get("source"))
                        && "neoforge_compatibility_adapter".equals(indexChar.get("forwardedFrom"))
                        && "character_typed".equals(indexChar.get("eventType"))
                        && charMetadata instanceof Map<?, ?> character
                        && Boolean.TRUE.equals(character.get("allowedChatCharacter"))
                        && character.containsKey("character"),
                "Index overlay character route must receive route-owned character metadata.");
        Map<String, Object> indexOverlayRender = dispatchMetadata.getOrDefault("index.inventory_overlay_render", Map.of());
        require("native_loader_client_ui_host".equals(indexOverlayRender.get("source"))
                        && "gui_layer".equals(indexOverlayRender.get("service"))
                        && "native_loader_window_pump".equals(indexOverlayRender.get("frameSource"))
                        && indexOverlayRender.containsKey("mouseX")
                        && indexOverlayRender.containsKey("partialTick"),
                "Index overlay render route must receive Native Loader GUI-layer frame metadata.");
        Map<String, Object> indexHotkeyRender = dispatchMetadata.getOrDefault("index.hotkey_screen_render", Map.of());
        require("native_loader_gui_layer".equals(indexHotkeyRender.get("source"))
                        && "neoforge_compatibility_adapter".equals(indexHotkeyRender.get("forwardedFrom"))
                        && "screen_render_post".equals(indexHotkeyRender.get("eventType"))
                        && indexHotkeyRender.containsKey("partialTick")
                        && indexHotkeyRender.containsKey("screenClass"),
                "Index hotkey screen render route must receive Native Loader GUI-layer metadata.");
        Map<String, Object> indexHotkeyKey = dispatchMetadata.getOrDefault("index.hotkey_key_pressed", Map.of());
        require("native_loader_input_binding".equals(indexHotkeyKey.get("source"))
                        && "neoforge_compatibility_adapter".equals(indexHotkeyKey.get("forwardedFrom"))
                        && "hotkey_key_pressed".equals(indexHotkeyKey.get("eventType"))
                        && indexHotkeyKey.containsKey("key"),
                "Index hotkey key route must receive Native Loader input metadata.");
    }

    private static void requireIndexCatalogScreenMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> catalog = dispatchMetadata.getOrDefault(
                "index.catalog:window_pump_route_dispatch",
                dispatchMetadata.getOrDefault("index.catalog", Map.of()));
        require("native_loader_client_ui_host".equals(catalog.get("source"))
                        && "native_loader_window_pump".equals(catalog.get("windowPumpSource"))
                        && "native_loader_window_pump".equals(catalog.get("routeDispatchSource"))
                        && "window_pump_route_dispatch".equals(catalog.get("eventType"))
                        && Boolean.FALSE.equals(catalog.get("neoForgeEventOwnershipRequired")),
                "Index catalog route must preserve Native Loader window-pump route dispatch metadata.");
        Map<String, Object> mouse = dispatchMetadata.getOrDefault(
                "index.catalog_screen.mouse:catalog_screen_mouse_input",
                dispatchMetadata.getOrDefault("index.catalog_screen.mouse", Map.of()));
        require("native_screen_lifecycle".equals(mouse.get("source"))
                        && "catalog_screen_mouse_input".equals(mouse.get("eventType"))
                        && "click".equals(mouse.get("phase"))
                        && mouse.containsKey("screenClass")
                        && mouse.containsKey("mouseX")
                        && mouse.containsKey("button"),
                "Index catalog screen mouse route must receive native screen lifecycle metadata.");
        Map<String, Object> scroll = dispatchMetadata.getOrDefault("index.catalog_screen.scroll", Map.of());
        require("native_screen_lifecycle".equals(scroll.get("source"))
                        && "catalog_screen_scroll_input".equals(scroll.get("eventType"))
                        && scroll.containsKey("screenClass")
                        && scroll.containsKey("scrollY")
                        && scroll.containsKey("mouseY"),
                "Index catalog screen scroll route must receive native screen lifecycle metadata.");
        Map<String, Object> key = dispatchMetadata.getOrDefault("index.catalog_screen.key", Map.of());
        require(("native_screen_lifecycle".equals(key.get("source"))
                        && "catalog_screen_key_input".equals(key.get("eventType"))
                        && Boolean.TRUE.equals(key.get("usageKey"))
                        && key.containsKey("key")
                        && key.containsKey("screenClass"))
                        || (("native_loader_client_ui_host".equals(key.get("source"))
                        || "agent2_direct_public_sdk_probe".equals(key.get("source")))
                        && "overlay_input".equals(key.get("service"))
                        && "direct_public_sdk_overlay_input".equals(key.get("eventType"))),
                "Index catalog screen key route must receive native screen lifecycle metadata.");
        Map<String, Object> character = dispatchMetadata.getOrDefault("index.catalog_screen.char", Map.of());
        require("native_screen_lifecycle".equals(character.get("source"))
                        && "catalog_screen_character_typed".equals(character.get("eventType"))
                        && Boolean.TRUE.equals(character.get("allowedChatCharacter"))
                        && character.containsKey("character")
                        && character.containsKey("screenClass"),
                "Index catalog screen character route must receive native screen lifecycle metadata.");
    }

    private static void requireIndexScreenCoreActionMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> action = dispatchMetadata.getOrDefault("index.screencore.action", Map.of());
        require("native_screencore_action".equals(action.get("source"))
                        && "index_screencore_action".equals(action.get("eventType"))
                        && "IndexActions".equals(action.get("actionCatalog"))
                        && "index.toggle_favorite".equals(action.get("screenCoreActionId"))
                        && action.containsKey("pageId")
                        && action.containsKey("actionValue"),
                "Index ScreenCore catalog actions must dispatch through the native Index route with action metadata.");
    }

    private static void requireIndexRecipeScreenMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> mouse = dispatchMetadata.getOrDefault("index.recipe_screen.mouse", Map.of());
        require("native_screen_lifecycle".equals(mouse.get("source"))
                        && "recipe_screen_mouse_input".equals(mouse.get("eventType"))
                        && "click".equals(mouse.get("phase"))
                        && mouse.containsKey("screenClass")
                        && mouse.containsKey("mouseX")
                        && mouse.containsKey("button"),
                "Index recipe screen mouse route must receive native screen lifecycle metadata.");
        Map<String, Object> scroll = dispatchMetadata.getOrDefault("index.recipe_screen.scroll", Map.of());
        require("native_screen_lifecycle".equals(scroll.get("source"))
                        && "recipe_screen_scroll_input".equals(scroll.get("eventType"))
                        && scroll.containsKey("screenClass")
                        && scroll.containsKey("scrollY")
                        && scroll.containsKey("mouseY"),
                "Index recipe screen scroll route must receive native screen lifecycle metadata.");
        Map<String, Object> key = dispatchMetadata.getOrDefault("index.recipe_screen.key", Map.of());
        require("native_screen_lifecycle".equals(key.get("source"))
                        && "recipe_screen_key_input".equals(key.get("eventType"))
                        && Boolean.TRUE.equals(key.get("recipeKey"))
                        && key.containsKey("key")
                        && key.containsKey("screenClass"),
                "Index recipe screen key route must receive native screen lifecycle metadata.");
        Map<String, Object> character = dispatchMetadata.getOrDefault("index.recipe_screen.char", Map.of());
        require("native_screen_lifecycle".equals(character.get("source"))
                        && "recipe_screen_character_typed".equals(character.get("eventType"))
                        && Boolean.TRUE.equals(character.get("allowedChatCharacter"))
                        && character.containsKey("character")
                        && character.containsKey("screenClass"),
                "Index recipe screen character route must receive native screen lifecycle metadata.");
    }

    private static void requireHudRenderMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> render = dispatchMetadata.getOrDefault("hud.render", Map.of());
        require(("native_loader_client_ui_host".equals(render.get("source"))
                        || "agent2_direct_public_sdk_probe".equals(render.get("source"))
                        || "agent2_direct_public_sdk_lifecycle_probe".equals(render.get("source")))
                        && "hud_layer".equals(render.get("service"))
                        && ("native_loader_window_pump".equals(render.get("frameSource"))
                        || "direct_public_sdk_hud_layer".equals(render.get("eventType"))
                        || "direct_lifecycle_publication".equals(render.get("eventType")))
                        && (render.containsKey("partialTick")
                        || "direct_public_sdk_hud_layer".equals(render.get("eventType"))
                        || "direct_lifecycle_publication".equals(render.get("eventType"))),
                "HUD render route must receive Native Loader HUD-layer host metadata.");
    }

    private static void requireHudRouteActionMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> update = dispatchMetadata.getOrDefault("hud.update_snapshot", Map.of());
        require("native_loader_client_ui_host".equals(update.get("source")),
                "HUD snapshot update route must receive Native Loader host metadata.");
        Map<String, Object> focus = dispatchMetadata.getOrDefault("native_loader.overlay_focus", Map.of());
        require(("native_loader_client_ui_host".equals(focus.get("source"))
                        || "agent2_direct_public_sdk_probe".equals(focus.get("source")))
                        && "overlay_focus".equals(focus.get("service"))
                        && ("native_loader_window_pump".equals(focus.get("focusSource"))
                        || "direct_public_sdk_overlay_focus".equals(focus.get("eventType")))
                        && Boolean.TRUE.equals(focus.get("focused")),
                "HUD overlay focus route must receive Native Loader focus-service metadata.");
        requireHudWidgetRenderMetadata(dispatchMetadata, "hud.mission_tracker.render");
        requireHudWidgetRenderMetadata(dispatchMetadata, "hud.hazard_readout.render");
        requireHudWidgetRenderMetadata(dispatchMetadata, "hud.compass_indicator.render");
        Map<String, Object> layout = dispatchMetadata.getOrDefault("hud.screen_safe_area.resolve", Map.of());
        require("native_loader_client_ui_host".equals(layout.get("source")),
                "HUD safe-area route must receive Native Loader host metadata.");
    }

    private static void requireHudWidgetRenderMetadata(
            Map<String, Map<String, Object>> dispatchMetadata,
            String actionId
    ) {
        Map<String, Object> metadata = dispatchMetadata.getOrDefault(actionId, Map.of());
        require("native_loader_client_ui_host".equals(metadata.get("source"))
                        && "hud_layer".equals(metadata.get("service"))
                        && "native_loader_window_pump".equals(metadata.get("frameSource"))
                        && metadata.containsKey("screenHeight")
                        && metadata.containsKey("partialTick"),
                "HUD widget render route " + actionId + " must receive Native Loader HUD-layer metadata.");
    }

    private static void requireLensOverlayMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> render = dispatchMetadata.getOrDefault("lens.overlay.render", Map.of());
        require(("native_loader_client_ui_host".equals(render.get("source"))
                        || "agent2_direct_public_sdk_lifecycle_probe".equals(render.get("source")))
                        && "gui_layer".equals(render.get("service"))
                        && ("native_loader_window_pump".equals(render.get("frameSource"))
                        || "direct_lifecycle_publication".equals(render.get("eventType")))
                        && (render.containsKey("partialTick")
                        || "direct_lifecycle_publication".equals(render.get("eventType"))),
                "Lens overlay render route must receive Native Loader GUI-layer host metadata.");
    }

    private static void requireHoloMapMinimapMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> render = dispatchMetadata.getOrDefault("holomap.minimap.render", Map.of());
        require(("native_loader_client_ui_host".equals(render.get("source"))
                        || "agent2_direct_public_sdk_probe".equals(render.get("source"))
                        || "agent2_direct_public_sdk_lifecycle_probe".equals(render.get("source")))
                        && "gui_layer".equals(render.get("service"))
                        && ("native_loader_window_pump".equals(render.get("frameSource"))
                        || "direct_public_sdk_gui_layer".equals(render.get("eventType"))
                        || "direct_lifecycle_publication".equals(render.get("eventType")))
                        && (render.containsKey("partialTick")
                        || "direct_public_sdk_gui_layer".equals(render.get("eventType"))
                        || "direct_lifecycle_publication".equals(render.get("eventType"))),
                "HoloMap minimap render route must receive Native Loader GUI-layer host metadata.");
    }

    private static void requireHoloMapScreenMetadata(Map<String, Map<String, Object>> dispatchMetadata) {
        Map<String, Object> fullscreenKey =
                dispatchMetadata.getOrDefault("holomap.fullscreen.key:fullscreen_key_pressed", Map.of());
        require("native_screen_lifecycle".equals(fullscreenKey.get("source"))
                        && "fullscreen_key_pressed".equals(fullscreenKey.get("eventType"))
                        && fullscreenKey.containsKey("key")
                        && fullscreenKey.containsKey("screenClass"),
                "HoloMap fullscreen key route must receive native screen lifecycle metadata.");
        Map<String, Object> fullscreenMouse =
                dispatchMetadata.getOrDefault("holomap.fullscreen.mouse:fullscreen_mouse_input", Map.of());
        require("native_screen_lifecycle".equals(fullscreenMouse.get("source"))
                        && "fullscreen_mouse_input".equals(fullscreenMouse.get("eventType"))
                        && "drag".equals(fullscreenMouse.get("phase"))
                        && fullscreenMouse.containsKey("mouseX")
                        && fullscreenMouse.containsKey("button")
                        && fullscreenMouse.containsKey("screenClass"),
                "HoloMap fullscreen mouse route must receive native screen lifecycle metadata.");
        Map<String, Object> fullscreenScroll =
                dispatchMetadata.getOrDefault("holomap.fullscreen.scroll:fullscreen_scroll_input", Map.of());
        require("native_screen_lifecycle".equals(fullscreenScroll.get("source"))
                        && "fullscreen_scroll_input".equals(fullscreenScroll.get("eventType"))
                        && fullscreenScroll.containsKey("scrollY")
                        && fullscreenScroll.containsKey("mouseY")
                        && fullscreenScroll.containsKey("screenClass"),
                "HoloMap fullscreen scroll route must receive native screen lifecycle metadata.");
        Map<String, Object> screenCoreKey =
                dispatchMetadata.getOrDefault("holomap.fullscreen.key:fullscreen_screencore_key_pressed", Map.of());
        require("native_screencore_lifecycle".equals(screenCoreKey.get("source"))
                        && "fullscreen_screencore_key_pressed".equals(screenCoreKey.get("eventType"))
                        && screenCoreKey.containsKey("key")
                        && screenCoreKey.containsKey("canvasWidth")
                        && screenCoreKey.containsKey("screenClass"),
                "HoloMap ScreenCore fullscreen key route must receive native ScreenCore lifecycle metadata.");
        Map<String, Object> screenCoreMouse =
                dispatchMetadata.getOrDefault("holomap.fullscreen.mouse:fullscreen_screencore_mouse_input", Map.of());
        require("native_screencore_lifecycle".equals(screenCoreMouse.get("source"))
                        && "fullscreen_screencore_mouse_input".equals(screenCoreMouse.get("eventType"))
                        && "drag".equals(screenCoreMouse.get("phase"))
                        && screenCoreMouse.containsKey("mouseX")
                        && screenCoreMouse.containsKey("dragX")
                        && screenCoreMouse.containsKey("canvasHeight")
                        && screenCoreMouse.containsKey("screenClass"),
                "HoloMap ScreenCore fullscreen mouse route must receive native ScreenCore lifecycle metadata.");
        Map<String, Object> screenCoreScroll =
                dispatchMetadata.getOrDefault("holomap.fullscreen.scroll:fullscreen_screencore_scroll_input", Map.of());
        require("native_screencore_lifecycle".equals(screenCoreScroll.get("source"))
                        && "fullscreen_screencore_scroll_input".equals(screenCoreScroll.get("eventType"))
                        && screenCoreScroll.containsKey("scrollY")
                        && screenCoreScroll.containsKey("canvasX")
                        && screenCoreScroll.containsKey("screenClass"),
                "HoloMap ScreenCore fullscreen scroll route must receive native ScreenCore lifecycle metadata.");
        requireHoloMapScreenCoreCommand(dispatchMetadata, "holomap.sync", "sync");
        requireHoloMapScreenCoreCommand(dispatchMetadata, "holomap.center", "center");
        requireHoloMapScreenCoreCommand(dispatchMetadata, "holomap.toggle_markers", "toggleMarkers");
        requireHoloMapScreenCoreCommand(dispatchMetadata, "holomap.cycle_fields", "cycleFields");
        requireHoloMapScreenCoreCommand(dispatchMetadata, "holomap.toggle_waypoints", "toggleWaypoints");
        requireHoloMapScreenCoreCommand(dispatchMetadata, "holomap.close", "close");
    }

    private static void requireHoloMapScreenCoreCommand(
            Map<String, Map<String, Object>> dispatchMetadata,
            String actionId,
            String command
    ) {
        Map<String, Object> metadata = dispatchMetadata.getOrDefault(actionId, Map.of());
        require("native_screencore_lifecycle".equals(metadata.get("source"))
                        && "fullscreen_screencore_command".equals(metadata.get("eventType"))
                        && command.equals(metadata.get("command"))
                        && metadata.containsKey("screenClass"),
                "HoloMap ScreenCore command " + actionId + " must receive native route metadata.");
    }

    private static void requireBuiltInProductSurfaceState(Map<String, Map<String, Object>> builtInState) {
        Map<String, Object> menu = builtInState.getOrDefault("main_menu", Map.of());
        Object menuMetadataObject = menu.get("lastMetadata");
        Object menuRenderModelObject = menu.get("renderModel");
        require(menuMetadataObject instanceof Map<?, ?> menuMetadata
                        && ("native_loader_client_ui_host".equals(menuMetadata.get("source"))
                        || "agent2_direct_public_sdk_lifecycle_probe".equals(menuMetadata.get("source")))
                        && "screen_lifecycle".equals(menuMetadata.get("service"))
                        && ("new_run".equals(menuMetadata.get("selection"))
                        || "direct_lifecycle_publication".equals(menuMetadata.get("eventType"))),
                "Native Loader main menu route state must preserve dispatch metadata from the UI host.");
        require(menuRenderModelObject instanceof Map<?, ?> menuRenderModel
                        && "ECHO Native Loader".equals(menuRenderModel.get("product"))
                        && Boolean.TRUE.equals(menuRenderModel.get("routeDriven"))
                        && menuRenderModel.get("commands") instanceof List<?>
                        && Boolean.TRUE.equals(menuRenderModel.get("pendingAshfallWorldStartup")),
                "Native Loader main menu route state must expose a route-driven renderer model.");
        require(List.of("menu.new_run", "menu.open").contains(menu.get("lastActionId"))
                        && List.of("new_run", "open").contains(menu.get("selectedCommand"))
                        && Boolean.TRUE.equals(menu.get("pendingAshfallWorldStartup"))
                        && Boolean.TRUE.equals(menu.get("visible"))
                        && Boolean.TRUE.equals(menu.get("routeDrivenRendererState"))
                        && Boolean.TRUE.equals(menu.get("nativeProductUiReady")),
                "Native Loader main menu route actions must mutate built-in product menu state.");
        Map<String, Object> loading = builtInState.getOrDefault("loading_screen", Map.of());
        Object loadingMetadataObject = loading.get("lastMetadata");
        Object loadingRenderModelObject = loading.get("renderModel");
        Object progress = loading.get("progress");
        require(loadingMetadataObject instanceof Map<?, ?> loadingMetadata
                        && ("native_loader_client_ui_host".equals(loadingMetadata.get("source"))
                        || "agent2_direct_public_sdk_lifecycle_probe".equals(loadingMetadata.get("source"))
                        || "agent2_direct_public_sdk_probe".equals(loadingMetadata.get("source")))
                        && ("screen_lifecycle".equals(loadingMetadata.get("service"))
                        || "gui_layer".equals(loadingMetadata.get("service")))
                        && ("complete".equals(loadingMetadata.get("closeReason"))
                        || "direct_lifecycle_publication".equals(loadingMetadata.get("eventType"))
                        || "direct_public_sdk_gui_layer".equals(loadingMetadata.get("eventType"))),
                "Native Loader loading route state must preserve dispatch metadata from the UI host.");
        require(loadingRenderModelObject instanceof Map<?, ?> loadingRenderModel
                        && "ECHO Native Loader".equals(loadingRenderModel.get("product"))
                        && Boolean.TRUE.equals(loadingRenderModel.get("routeDriven"))
                        && Boolean.TRUE.equals(loadingRenderModel.get("completed"))
                        && loadingRenderModel.get("progress") instanceof Number renderProgress
                        && renderProgress.doubleValue() >= 1.0D,
                "Native Loader loading route state must expose a route-driven renderer model.");
        require(List.of("loading.complete", "loading.render").contains(loading.get("lastActionId"))
                        && Boolean.TRUE.equals(loading.get("completed"))
                        && loading.containsKey("visible")
                        && loading.containsKey("renderCount")
                        && Boolean.TRUE.equals(loading.get("routeDrivenRendererState"))
                        && Boolean.TRUE.equals(loading.get("nativeProductUiReady"))
                        && progress instanceof Number number
                        && number.doubleValue() >= 1.0D,
                "Native Loader loading route actions must mutate loading render/progress/completion state.");
    }

    private static void requireBuiltInProductRendererFrame(
            Map<String, Object> frame,
            String surfaceType,
            String actionId
    ) {
        Object renderModelObject = frame.get("renderModel");
        Object surfaceStateObject = frame.get("surfaceState");
        Object frameMetadataObject = frame.get("frameMetadata");
        require(NativeLoaderClientWindowPump.SERVICE_ID.equals(frame.get("serviceId"))
                        && NativeLoaderClientWindowPump.SOURCE.equals(frame.get("source"))
                        && surfaceType.equals(frame.get("surfaceType"))
                        && actionId.equals(frame.get("actionId"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(frame.get("status"))
                        && Boolean.TRUE.equals(frame.get("nativeWindowPumpRendererFrame"))
                        && Boolean.TRUE.equals(frame.get("routeDrivenRendererState"))
                        && Boolean.TRUE.equals(frame.get("nativeProductUiReady")),
                "Native Loader window pump must expose a mutating built-in product renderer frame for "
                        + surfaceType + "/" + actionId + ".");
        require(renderModelObject instanceof Map<?, ?> renderModel
                        && "ECHO Native Loader".equals(renderModel.get("product"))
                        && surfaceType.equals(renderModel.get("surface"))
                        && Boolean.TRUE.equals(renderModel.get("routeDriven")),
                "Native Loader window pump renderer frame must expose the route-driven render model for "
                        + surfaceType + "/" + actionId + ".");
        require(surfaceStateObject instanceof Map<?, ?> surfaceState
                        && actionId.equals(surfaceState.get("lastActionId"))
                        && Boolean.TRUE.equals(surfaceState.get("routeDrivenRendererState"))
                        && Boolean.TRUE.equals(surfaceState.get("nativeProductUiReady")),
                "Native Loader window pump renderer frame must carry the latest route-mutated product state for "
                        + surfaceType + "/" + actionId + ".");
        require(frameMetadataObject instanceof Map<?, ?> frameMetadata
                        && NativeLoaderClientWindowPump.SOURCE.equals(frameMetadata.get("builtinProductRendererSource"))
                        && NativeLoaderClientWindowPump.SOURCE.equals(frameMetadata.get("frameSource"))
                        && "builtin_product_renderer".equals(frameMetadata.get("service"))
                        && Boolean.FALSE.equals(frameMetadata.get("neoForgeEventOwnershipRequired")),
                "Native Loader window pump renderer frame must preserve native frame metadata for "
                        + surfaceType + "/" + actionId + ".");
    }

    private static void requireDispatchCounts(
            Map<String, Integer> dispatchCounts,
            Map<String, Integer> expectedCounts
    ) {
        for (Map.Entry<String, Integer> entry : expectedCounts.entrySet()) {
            int actual = dispatchCounts.getOrDefault(entry.getKey(), 0);
            require(actual == entry.getValue(),
                    "Native route action " + entry.getKey() + " dispatched " + actual
                            + " times; expected " + entry.getValue()
                            + ". Key mapping dispatch must not cross-fire by raw key code.");
        }
    }

    private static int handlerCount(Map<String, Object> actionHandlerEvidence, String surfaceType) {
        Object surfaceEvidence = actionHandlerEvidence.get(surfaceType);
        if (!(surfaceEvidence instanceof Map<?, ?> surfaceMap)) {
            return 0;
        }
        Object count = surfaceMap.get("handlerCount");
        if (count instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static Map<String, Integer> serviceCounts(Map<String, Object> evidence) {
        Object counts = evidence.get("serviceCounts");
        if (!(counts instanceof Map<?, ?> countsMap)) {
            return Map.of();
        }
        Map<String, Integer> safeCounts = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : countsMap.entrySet()) {
            if (entry.getKey() == null || !(entry.getValue() instanceof Number number)) {
                continue;
            }
            safeCounts.put(String.valueOf(entry.getKey()), number.intValue());
        }
        return Map.copyOf(safeCounts);
    }

}
