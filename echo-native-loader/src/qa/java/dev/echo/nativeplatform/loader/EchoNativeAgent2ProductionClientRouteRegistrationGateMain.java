package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment;
import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent2ProductionClientRouteRegistrationGateMain {
    private static final List<String> REQUIRED_ROUTE_KEYS = List.of(
            "echoterminal:echoterminal:eui",
            "echoterminal:echoterminal:hud_overlay",
            "echoindex:echoindex:index",
            "echoindex:echoindex:inventory_overlay",
            "echolens:echolens:field_lens",
            "echolens:echolens:lens_overlay",
            "echoholomap:echoholomap:minimap",
            "echoholomap:echoholomap:fullscreen_map",
            "echohudcore:echohudcore:native_hud",
            "echohudcore:echohudcore:mission_tracker",
            "echohudcore:echohudcore:hazard_readout",
            "echohudcore:echohudcore:compass_indicator",
            "echohudcore:echohudcore:screen_safe_area"
    );

    private static final Map<String, List<String>> REQUIRED_ACTIONS_BY_SURFACE = Map.of(
            "terminal", List.of(
                    "terminal.open",
                    "terminal.screen.frame.render",
                    "terminal.screencore.action"),
            "index", List.of(
                    "index.catalog",
                    "index.recipe",
                    "index.catalog_screen.mouse",
                    "index.recipe_screen.key",
                    "index.screencore.action"),
            "lens", List.of(
                    "lens.deep_scan",
                    "lens.index_recipe",
                    "lens.track_in_index"),
            "client_overlay", List.of(
                    "terminal.mission_hud.tick",
                    "terminal.mission_hud.render",
                    "terminal.discovery_toast.tick",
                    "terminal.discovery_toast.render",
                    "index.inventory_overlay_render",
                    "index.inventory_overlay_input",
                    "index.track_item",
                    "lens.overlay.render",
                    "lens.overlay.scan_target"),
            "holomap", List.of(
                    "holomap.open",
                    "holomap.minimap.render",
                    "holomap.fullscreen.key",
                    "holomap.fullscreen.mouse",
                    "holomap.select_entry",
                    "holomap.close"),
            "hud", List.of(
                    "hud.render",
                    "hud.update_snapshot",
                    "native_loader.overlay_focus"),
            "hud_widget", List.of(
                    "hud.mission_tracker.render",
                    "hud.hazard_readout.render",
                    "hud.compass_indicator.render"),
            "hud_layout", List.of("hud.screen_safe_area.resolve")
    );

    private static final Map<String, List<String>> REQUIRED_HANDLER_IDS_BY_SURFACE = Map.of(
            "terminal", List.of("echoterminal:eui", "echoterminal:eui:rendercore_screen_frame"),
            "index", List.of("echoindex:index"),
            "lens", List.of("echolens:field_lens"),
            "client_overlay", List.of(
                    "echoterminal:hud_overlay",
                    "echoindex:inventory_overlay",
                    "echolens:lens_overlay"),
            "holomap", List.of("echoholomap:minimap", "echoholomap:fullscreen_map"),
            "hud", List.of("echohudcore:native_hud"),
            "hud_widget", List.of(
                    "echohudcore:mission_tracker",
                    "echohudcore:hazard_readout",
                    "echohudcore:compass_indicator"),
            "hud_layout", List.of("echohudcore:screen_safe_area")
    );

    private static final Map<String, List<String>> REQUIRED_ROUTE_CLASSES = Map.ofEntries(
            Map.entry("echoterminal:echoterminal:eui", List.of(
                    "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreScreen",
                    "com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreBridge")),
            Map.entry("echoterminal:echoterminal:hud_overlay", List.of(
                    "com.knoxhack.echoterminal.client.mission.TerminalMissionHudController",
                    "com.knoxhack.echoterminal.client.discovery.DiscoveryToastHud")),
            Map.entry("echoindex:echoindex:index", List.of(
                    "com.knoxhack.echoindex.client.IndexCatalogScreen",
                    "com.knoxhack.echoindex.client.IndexScreenCoreBridge")),
            Map.entry("echoindex:echoindex:inventory_overlay", List.of(
                    "com.knoxhack.echoindex.client.IndexOverlay",
                    "com.knoxhack.echoindex.client.IndexScreenCoreBridge")),
            Map.entry("echolens:echolens:field_lens", List.of(
                    "com.knoxhack.echolens.client.LensHudOverlay",
                    "com.knoxhack.echolens.client.LensHudOverlay")),
            Map.entry("echolens:echolens:lens_overlay", List.of(
                    "com.knoxhack.echolens.client.LensHudOverlay",
                    "com.knoxhack.echolens.client.LensHudOverlay")),
            Map.entry("echoholomap:echoholomap:minimap", List.of(
                    "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay",
                    "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay")),
            Map.entry("echoholomap:echoholomap:fullscreen_map", List.of(
                    "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                    "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration")),
            Map.entry("echohudcore:echohudcore:native_hud", List.of(
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay")),
            Map.entry("echohudcore:echohudcore:mission_tracker", List.of(
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay")),
            Map.entry("echohudcore:echohudcore:hazard_readout", List.of(
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay")),
            Map.entry("echohudcore:echohudcore:compass_indicator", List.of(
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay")),
            Map.entry("echohudcore:echohudcore:screen_safe_area", List.of(
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay",
                    "com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay"))
    );

    private static final Map<String, Map<String, String>> REQUIRED_INPUT_BINDINGS = Map.of(
            "terminal", Map.of("terminal.open", "key.echoterminal.open"),
            "index", Map.of(
                    "index.catalog", "key.echoindex.catalog",
                    "index.recipe", "key.echoindex.recipe",
                    "index.usage", "key.echoindex.usage",
                    "index.bookmark", "key.echoindex.bookmark"),
            "lens", Map.of(
                    "lens.deep_scan", "echolens.key.deep_scan",
                    "lens.index_recipe", "key.echolens.index_recipe",
                    "lens.index_usage", "key.echolens.index_usage",
                    "lens.track_in_index", "key.echolens.track_in_index"),
            "holomap", Map.of(
                    "holomap.open", "key.echoholomap.open_map",
                    "holomap.toggle_minimap", "key.echoholomap.toggle_minimap",
                    "holomap.zoom_in", "key.echoholomap.minimap_zoom_in",
                    "holomap.zoom_out", "key.echoholomap.minimap_zoom_out",
                    "holomap.cycle_corner", "key.echoholomap.minimap_cycle_corner")
    );

    private EchoNativeAgent2ProductionClientRouteRegistrationGateMain() {
    }

    public static void main(String[] args) throws Exception {
        Path productRoot = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("fixtures/ashfall").toAbsolutePath().normalize();
        Path output = args.length > 1
                ? Path.of(args[1]).toAbsolutePath().normalize()
                : Path.of("build/native-agent2-client-routes/production-client-route-registration.json")
                .toAbsolutePath()
                .normalize();

        NativeLoaderClientRouteTable.clear();
        System.setProperty(EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY, "true");
        EchoNativeClientRouteRegistries.resetDiscoveryForRuntimeReload();
        EchoNativeClientRouteRegistry registry = EchoNativeClientRouteRegistries.get();
        require(registry != EchoNativeClientRouteRegistry.NOOP,
                "Native Loader client route registry must be available before production bootstrap.");

        EchoNativeScanResult scan = new EchoNativeDescriptorScanner().scanProduct(productRoot);
        require(scan.diagnostics().stream().noneMatch(EchoNativeAgent2ProductionClientRouteRegistrationGateMain::blocking),
                "Product descriptor scan must not contain blocking diagnostics: " + scan.diagnostics());

        NativeLoaderProductClientRouteBootstrap.ClientRouteBootstrapReport bootstrapReport =
                NativeLoaderProductClientRouteBootstrap.bootstrapFirstPartyClientRoutes(scan.descriptors());
        require(bootstrapReport.status() == EchoNativeLoadStatus.MUTATED,
                "Production first-party client route bootstrap must mutate Native Loader route table: "
                        + bootstrapReport.toEvidence());
        require(bootstrapReport.attemptedCount() == 7,
                "Production first-party client route bootstrap must attempt all seven callable seams: "
                        + bootstrapReport.attemptedCount());
        require(bootstrapReport.mutatedCount() == bootstrapReport.attemptedCount(),
                "Every production first-party client route bootstrap method must mutate the route table: "
                        + bootstrapReport.toEvidence());
        require(bootstrapReport.failedCount() == 0,
                "Production first-party client route bootstrap must not fail: " + bootstrapReport.toEvidence());
        Map<String, Boolean> bootstrapMethodResults = requireBootstrapMethodsInvoked(bootstrapReport);

        Map<String, Boolean> routeResults = requireRoutes(NativeLoaderClientRouteTable.routes());
        Map<String, Boolean> routeClassResults = requireRouteClasses(NativeLoaderClientRouteTable.routes());
        Map<String, Boolean> actionResults = requireActions(NativeLoaderClientRouteTable.declaredActions());
        Map<String, Boolean> inputBindingResults = requireInputBindings(NativeLoaderClientRouteTable.inputBindings());
        Map<String, Boolean> handlerResults = requireHandlers(NativeLoaderClientRouteTable.actionHandlerEvidence());
        Map<String, Boolean> descriptorGateResults = requireDescriptorPermissionAccessGates(scan.descriptors());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.native.agent2.production_client_route_registration.v1");
        report.put("productRoot", productRoot.toString());
        report.put("bootstrapReport", bootstrapReport.toEvidence());
        report.put("bootstrapMethodResults", bootstrapMethodResults);
        report.put("routeResults", routeResults);
        report.put("routeClassResults", routeClassResults);
        report.put("actionResults", actionResults);
        report.put("inputBindingResults", inputBindingResults);
        report.put("handlerResults", handlerResults);
        report.put("descriptorGateResults", descriptorGateResults);
        report.put("routes", NativeLoaderClientRouteTable.routes());
        report.put("declaredActions", NativeLoaderClientRouteTable.declaredActions());
        report.put("inputBindings", NativeLoaderClientRouteTable.inputBindings());
        report.put("actionHandlerEvidence", NativeLoaderClientRouteTable.actionHandlerEvidence());
        Files.createDirectories(output.getParent());
        Files.writeString(output, EchoNativeJson.write(report), StandardCharsets.UTF_8);
        System.out.println("agent2 production client route registration gate PASS " + output);
    }

    private static Map<String, Boolean> requireBootstrapMethodsInvoked(
            NativeLoaderProductClientRouteBootstrap.ClientRouteBootstrapReport bootstrapReport
    ) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        int index = 0;
        for (Map<String, Object> result : bootstrapReport.results()) {
            String key = String.valueOf(result.getOrDefault("moduleId", "entry")) + ":" + index++;
            boolean methodInvoked = Boolean.TRUE.equals(result.get("methodInvoked"));
            boolean fallbackMutation = Boolean.TRUE.equals(result.get("nativeLoaderOwnedRouteFallback"));
            boolean routeTableMutated = methodInvoked || fallbackMutation;
            results.put(key + ":methodInvokedOrNativeLoaderFallback", routeTableMutated);
            results.put(key + ":nativeLoaderOwnedRouteFallback", fallbackMutation);
            require(routeTableMutated,
                    "Production first-party client route bootstrap must invoke the addon bootstrap method or install Native Loader owned route-table mutation: " + result);
            if (fallbackMutation) {
                Object fallbackRouteKeys = result.get("fallbackRouteKeys");
                Object fallbackActionKeys = result.get("fallbackActionKeys");
                Object fallbackHandlerKeys = result.get("fallbackHandlerKeys");
                require(fallbackRouteKeys instanceof List<?> routes && !routes.isEmpty(),
                        "Native Loader route fallback must report concrete fallback route keys: " + result);
                require(fallbackActionKeys instanceof List<?> actions && !actions.isEmpty(),
                        "Native Loader route fallback must report concrete fallback action keys: " + result);
                require(fallbackHandlerKeys instanceof List<?> handlers && !handlers.isEmpty(),
                        "Native Loader route fallback must report concrete fallback handler keys: " + result);
            }
        }
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireRoutes(Map<String, Map<String, Object>> routes) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (String key : REQUIRED_ROUTE_KEYS) {
            boolean present = routes.containsKey(key);
            results.put(key, present);
            require(present, "Production route table is missing route " + key + ": " + routes.keySet());
        }
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireActions(
            Map<String, Map<String, Map<String, Object>>> actions
    ) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : REQUIRED_ACTIONS_BY_SURFACE.entrySet()) {
            Map<String, Map<String, Object>> surfaceActions = actions.getOrDefault(entry.getKey(), Map.of());
            for (String actionId : entry.getValue()) {
                String key = entry.getKey() + ":" + actionId;
                boolean present = surfaceActions.containsKey(actionId);
                results.put(key, present);
                require(present, "Production route table is missing action " + key + ": " + surfaceActions.keySet());
            }
        }
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireRouteClasses(Map<String, Map<String, Object>> routes) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : REQUIRED_ROUTE_CLASSES.entrySet()) {
            Map<String, Object> route = routes.getOrDefault(entry.getKey(), Map.of());
            Object configObject = route.get("config");
            require(configObject instanceof Map<?, ?>,
                    "Production route table is missing config for route " + entry.getKey() + ": " + route);
            Map<?, ?> config = (Map<?, ?>) configObject;
            String implementationClass = String.valueOf(
                    config.containsKey("nativeSurfaceImplementationClass")
                            ? config.get("nativeSurfaceImplementationClass")
                            : "");
            String bridgeClass = String.valueOf(
                    config.containsKey("nativeScreenBridgeClass")
                            ? config.get("nativeScreenBridgeClass")
                            : "");
            boolean implementationMatches = entry.getValue().get(0).equals(implementationClass);
            boolean bridgeMatches = entry.getValue().get(1).equals(bridgeClass);
            results.put(entry.getKey() + ":nativeSurfaceImplementationClass", implementationMatches);
            results.put(entry.getKey() + ":nativeScreenBridgeClass", bridgeMatches);
            require(implementationMatches && bridgeMatches,
                    "Production route " + entry.getKey()
                            + " must carry real native implementation/bridge classes, not placeholder route metadata: "
                            + config);
        }
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireInputBindings(
            Map<String, Map<String, List<Map<String, Object>>>> inputBindings
    ) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> surfaceEntry : REQUIRED_INPUT_BINDINGS.entrySet()) {
            Map<String, List<Map<String, Object>>> surfaceBindings =
                    inputBindings.getOrDefault(surfaceEntry.getKey(), Map.of());
            for (Map.Entry<String, String> bindingEntry : surfaceEntry.getValue().entrySet()) {
                List<Map<String, Object>> bindings = surfaceBindings.getOrDefault(bindingEntry.getKey(), List.of());
                boolean present = bindings.stream()
                        .anyMatch(binding -> bindingEntry.getValue().equals(String.valueOf(binding.get("keyMapping"))));
                String key = surfaceEntry.getKey() + ":" + bindingEntry.getKey();
                results.put(key, present);
                require(present,
                        "Production route table is missing input binding " + key + " -> "
                                + bindingEntry.getValue() + ": " + bindings);
            }
        }
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireHandlers(Map<String, Object> handlerEvidence) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : REQUIRED_HANDLER_IDS_BY_SURFACE.entrySet()) {
            Object rawSurface = handlerEvidence.get(entry.getKey());
            require(rawSurface instanceof Map<?, ?>,
                    "Production route handler evidence is missing surface " + entry.getKey());
            Object rawIds = ((Map<?, ?>) rawSurface).get("handlerIds");
            require(rawIds instanceof List<?>,
                    "Production route handler evidence is missing handlerIds for " + entry.getKey());
            List<?> handlerIds = (List<?>) rawIds;
            for (String handlerId : entry.getValue()) {
                String key = entry.getKey() + ":" + handlerId;
                boolean present = handlerIds.contains(handlerId);
                results.put(key, present);
                require(present, "Production route handler evidence is missing handler " + key + ": " + handlerIds);
            }
        }
        return Map.copyOf(results);
    }

    private static Map<String, Boolean> requireDescriptorPermissionAccessGates(
            List<EchoNativeAddonDescriptor> descriptors
    ) throws Exception {
        Map<String, Boolean> results = new LinkedHashMap<>();
        Map<String, EchoNativeAddonDescriptor> descriptorsById = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor descriptor : descriptors) {
            descriptorsById.put(descriptor.id(), descriptor);
        }

        Map<String, Object> lensJson = descriptorJson(descriptorsById, "echolens");
        List<String> lensPermissions = EchoNativeJson.stringList(lensJson.get("permissions"));
        Map<String, Object> lensAccess = EchoNativeJson.asObject(lensJson.get("access"));
        Map<String, Object> lensAdapterCore = EchoNativeJson.asObject(lensAccess.get("adapterCore"));
        List<String> lensDomains = EchoNativeJson.stringList(lensAdapterCore.get("domains"));
        boolean lensUiPermission = lensPermissions.contains("ui.screens");
        boolean lensUiDomain = lensDomains.contains("ui_screens");
        results.put("echolens_permissions_ui_screens", lensUiPermission);
        results.put("echolens_domains_ui_screens", lensUiDomain);
        require(lensUiPermission,
                "echolens descriptor permissions must include ui.screens for lens overlay routes.");
        require(lensUiDomain,
                "echolens descriptor adapterCore domains must include ui_screens for lens overlay routes.");

        Map<String, Object> hudJson = descriptorJson(descriptorsById, "echohudcore");
        List<String> hudPermissions = EchoNativeJson.stringList(hudJson.get("permissions"));
        Map<String, Object> hudAccess = EchoNativeJson.asObject(hudJson.get("access"));
        Map<String, Object> hudAdapterCore = EchoNativeJson.asObject(hudAccess.get("adapterCore"));
        List<String> hudDomains = EchoNativeJson.stringList(hudAdapterCore.get("domains"));
        boolean hudUiPermission = hudPermissions.contains("ui.screens");
        boolean hudWidgetPermission = hudPermissions.contains("hud.widgets");
        boolean hudUiDomain = hudDomains.contains("ui_screens");
        boolean hudRequiresMinecraft = Boolean.TRUE.equals(hudAccess.get("requiresMinecraft"));
        boolean hudNotContractsOnly = Boolean.FALSE.equals(hudAccess.get("contractsOnly"));
        results.put("echohudcore_permissions_ui_screens", hudUiPermission);
        results.put("echohudcore_permissions_hud_widgets", hudWidgetPermission);
        results.put("echohudcore_domains_ui_screens", hudUiDomain);
        results.put("echohudcore_requires_minecraft", hudRequiresMinecraft);
        results.put("echohudcore_not_contracts_only", hudNotContractsOnly);
        require(hudUiPermission && hudWidgetPermission,
                "echohudcore descriptor permissions must include ui.screens and hud.widgets for live HUD routes.");
        require(hudUiDomain,
                "echohudcore descriptor adapterCore domains must include ui_screens for live HUD routes.");
        require(hudRequiresMinecraft && hudNotContractsOnly,
                "echohudcore live HUD access must require Minecraft and must not be contracts-only.");

        return Map.copyOf(results);
    }

    private static Map<String, Object> descriptorJson(
            Map<String, EchoNativeAddonDescriptor> descriptorsById,
            String moduleId
    ) throws Exception {
        EchoNativeAddonDescriptor descriptor = descriptorsById.get(moduleId);
        require(descriptor != null, "Descriptor scan must include " + moduleId + ".");
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(
                descriptor.descriptorPath(),
                StandardCharsets.UTF_8
        )));
    }

    private static boolean blocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
