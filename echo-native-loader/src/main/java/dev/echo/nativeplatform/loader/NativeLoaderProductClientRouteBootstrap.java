package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeLoaderProductClientRouteBootstrap {
    public static final List<ClientRouteBootstrapEntrypoint> FIRST_PARTY_CLIENT_ROUTE_ENTRYPOINTS = List.of(
            new ClientRouteBootstrapEntrypoint(
                    "echoashfallprotocol",
                    "com.knoxhack.echoashfallprotocol.EchoAshfallProtocolClient",
                    "ensureNativeClientRoutesRegisteredForNativeLoader"),
            new ClientRouteBootstrapEntrypoint(
                    "echoterminal",
                    "com.knoxhack.echoterminal.EchoTerminalClient",
                    "ensureNativeClientRoutesRegisteredForNativeLoader"),
            new ClientRouteBootstrapEntrypoint(
                    "echoterminal",
                    "com.knoxhack.echoterminal.integration.TerminalRenderCoreClientIntegration",
                    "ensureNativeScreenFrameRouteRegisteredForNativeLoader"),
            new ClientRouteBootstrapEntrypoint(
                    "echoindex",
                    "com.knoxhack.echoindex.EchoIndexClient",
                    "ensureNativeClientRoutesRegisteredForNativeLoader"),
            new ClientRouteBootstrapEntrypoint(
                    "echolens",
                    "com.knoxhack.echolens.EchoLensClient",
                    "ensureNativeClientRoutesRegisteredForNativeLoader"),
            new ClientRouteBootstrapEntrypoint(
                    "echoholomap",
                    "com.knoxhack.echoholomap.EchoHoloMapClient",
                    "ensureNativeClientRoutesRegisteredForNativeLoader"),
            new ClientRouteBootstrapEntrypoint(
                    "echohudcore",
                    "com.knoxhack.echo.hudcore.EchoHudCoreClient",
                    "ensureNativeClientRoutesRegisteredForNativeLoader")
    );

    private NativeLoaderProductClientRouteBootstrap() {
    }

    public static ClientRouteBootstrapReport bootstrapFirstPartyClientRoutes(
            List<EchoNativeAddonDescriptor> descriptors
    ) {
        return bootstrapClientRoutes(descriptors, FIRST_PARTY_CLIENT_ROUTE_ENTRYPOINTS);
    }

    public static ClientRouteBootstrapReport bootstrapFirstPartyClientRoutes(ClassLoader classLoader) {
        return bootstrapClientRoutes(classLoader, FIRST_PARTY_CLIENT_ROUTE_ENTRYPOINTS);
    }

    public static ClientRouteBootstrapReport bootstrapFirstPartyClientRoutes(
            ClassLoader classLoader,
            List<String> activeModules
    ) {
        return bootstrapClientRoutes(
                classLoader,
                entrypointsForActiveModules(FIRST_PARTY_CLIENT_ROUTE_ENTRYPOINTS, activeModules));
    }

    public static ClientRouteBootstrapReport bootstrapClientRoutes(
            ClassLoader classLoader,
            List<ClientRouteBootstrapEntrypoint> entrypoints
    ) {
        List<ClientRouteBootstrapEntrypoint> safeEntrypoints =
                entrypoints == null ? List.of() : List.copyOf(entrypoints);
        String previousNativeLoaderProperty =
                System.getProperty(EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY);
        System.setProperty(EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY, "true");
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            ClassLoader effectiveClassLoader = classLoader == null
                    ? NativeLoaderProductClientRouteBootstrap.class.getClassLoader()
                    : classLoader;
            for (ClientRouteBootstrapEntrypoint entrypoint : safeEntrypoints) {
                results.add(invokeEntrypoint(effectiveClassLoader, entrypoint));
            }
        } finally {
            if (previousNativeLoaderProperty == null) {
                System.clearProperty(EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY);
            } else {
                System.setProperty(
                        EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY,
                        previousNativeLoaderProperty);
            }
        }
        return ClientRouteBootstrapReport.from(results);
    }

    public static ClientRouteBootstrapReport bootstrapClientRoutes(
            List<EchoNativeAddonDescriptor> descriptors,
            List<ClientRouteBootstrapEntrypoint> entrypoints
    ) {
        List<EchoNativeAddonDescriptor> safeDescriptors = descriptors == null ? List.of() : List.copyOf(descriptors);
        List<ClientRouteBootstrapEntrypoint> safeEntrypoints =
                entrypoints == null ? List.of() : List.copyOf(entrypoints);
        Map<String, EchoNativeAddonDescriptor> descriptorsByModule = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor descriptor : safeDescriptors) {
            if (descriptor != null && descriptor.id() != null && !descriptor.id().isBlank()) {
                descriptorsByModule.putIfAbsent(descriptor.id().trim(), descriptor);
            }
        }

        String previousNativeLoaderProperty =
                System.getProperty(EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY);
        System.setProperty(EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY, "true");
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            for (ClientRouteBootstrapEntrypoint entrypoint : safeEntrypoints) {
                results.add(invokeEntrypoint(descriptorsByModule, entrypoint));
            }
        } finally {
            if (previousNativeLoaderProperty == null) {
                System.clearProperty(EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY);
            } else {
                System.setProperty(
                        EchoNativeClientRuntimeEnvironment.NATIVE_LOADER_PROPERTY,
                        previousNativeLoaderProperty);
            }
        }
        return ClientRouteBootstrapReport.from(results);
    }

    private static List<ClientRouteBootstrapEntrypoint> entrypointsForActiveModules(
            List<ClientRouteBootstrapEntrypoint> entrypoints,
            List<String> activeModules
    ) {
        if (activeModules == null || activeModules.isEmpty()) {
            return entrypoints == null ? List.of() : List.copyOf(entrypoints);
        }
        Set<String> active = new LinkedHashSet<>();
        for (String module : activeModules) {
            if (module != null && !module.isBlank()) {
                active.add(module.trim());
            }
        }
        if (active.isEmpty()) {
            return entrypoints == null ? List.of() : List.copyOf(entrypoints);
        }
        List<ClientRouteBootstrapEntrypoint> filtered = new ArrayList<>();
        for (ClientRouteBootstrapEntrypoint entrypoint : entrypoints == null ? List.<ClientRouteBootstrapEntrypoint>of() : entrypoints) {
            if (active.contains(entrypoint.moduleId())) {
                filtered.add(entrypoint);
            }
        }
        return List.copyOf(filtered);
    }

    private static Map<String, Object> invokeEntrypoint(
            Map<String, EchoNativeAddonDescriptor> descriptorsByModule,
            ClientRouteBootstrapEntrypoint entrypoint
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleId", entrypoint.moduleId());
        result.put("className", entrypoint.className());
        result.put("methodName", entrypoint.methodName());
        result.put("source", "native_loader_product_client_route_bootstrap");
        EchoNativeAddonDescriptor descriptor = descriptorsByModule.get(entrypoint.moduleId());
        if (descriptor == null) {
            result.put("status", EchoNativeLoadStatus.UNSUPPORTED.name());
            result.put("descriptorPresent", false);
            result.put("failure", "module descriptor not present");
            return Map.copyOf(result);
        }
        result.put("descriptorPresent", true);
        result.put("descriptorPath", descriptor.descriptorPath() == null ? "" : descriptor.descriptorPath().toString());
        EchoNativeModuleDescriptor moduleDescriptor = EchoNativeModuleDescriptor.fromAddon(descriptor);
        List<Path> classpath = moduleDescriptor.classpath();
        result.put("classpathEntryCount", classpath.size());
        result.put("classpathReady", !classpath.isEmpty());
        result.put("nativeClasspathDeclared", moduleDescriptor.nativeClasspathDeclared());
        result.put("inferredClasspathRequested", moduleDescriptor.inferredClasspathRequested());
        result.put("compatibilityClasspathFallback", moduleDescriptor.compatibilityClasspathFallback());
        if (classpath.isEmpty()) {
            result.put("status", EchoNativeLoadStatus.FAILED.name());
            result.put("failure", "module native classpath is empty");
            return Map.copyOf(result);
        }
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = NativeLoaderProductClientRouteBootstrap.class.getClassLoader();
        }
        try (EchoNativeModuleClassLoader classLoader = new EchoNativeModuleClassLoader(classpath, parent)) {
            Class<?> type = classLoader.loadClass(entrypoint.className());
            Method method = type.getDeclaredMethod(entrypoint.methodName());
            boolean publicStaticBoolean = Modifier.isPublic(method.getModifiers())
                    && Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 0
                    && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class);
            result.put("classLoaded", true);
            result.put("loadedClassName", type.getName());
            result.put("loadedByClassLoader", type.getClassLoader() == null
                    ? "bootstrap"
                    : type.getClassLoader().getClass().getName());
            result.put("publicStaticBooleanEntrypoint", publicStaticBoolean);
            if (!publicStaticBoolean) {
                result.put("status", EchoNativeLoadStatus.FAILED.name());
                result.put("failure", "bootstrap method must be public static boolean with no arguments");
                return Map.copyOf(result);
            }
            Object rawResult = method.invoke(null);
            boolean registered = Boolean.TRUE.equals(rawResult);
            result.put("methodInvoked", true);
            result.put("registered", registered);
            result.put("nativeClientRouteBootstrap", true);
            if (registered) {
                supplementDirectNativeRouteRegistration(entrypoint, result);
            }
            result.put("status", registered ? EchoNativeLoadStatus.MUTATED.name() : EchoNativeLoadStatus.REGISTERED.name());
            return Map.copyOf(result);
        } catch (IOException | ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return installNativeLoaderOwnedRouteFallback(result, entrypoint, exception);
        }
    }

    private static Map<String, Object> invokeEntrypoint(
            ClassLoader classLoader,
            ClientRouteBootstrapEntrypoint entrypoint
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleId", entrypoint.moduleId());
        result.put("className", entrypoint.className());
        result.put("methodName", entrypoint.methodName());
        result.put("source", "native_loader_windowed_client_route_bootstrap");
        result.put("classLoaderProvided", classLoader != null);
        try {
            Class<?> type = Class.forName(entrypoint.className(), true, classLoader);
            Method method = type.getDeclaredMethod(entrypoint.methodName());
            boolean publicStaticBoolean = Modifier.isPublic(method.getModifiers())
                    && Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 0
                    && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class);
            result.put("classLoaded", true);
            result.put("loadedClassName", type.getName());
            result.put("loadedByClassLoader", type.getClassLoader() == null
                    ? "bootstrap"
                    : type.getClassLoader().getClass().getName());
            result.put("publicStaticBooleanEntrypoint", publicStaticBoolean);
            if (!publicStaticBoolean) {
                result.put("status", EchoNativeLoadStatus.FAILED.name());
                result.put("failure", "bootstrap method must be public static boolean with no arguments");
                return Map.copyOf(result);
            }
            Object rawResult = method.invoke(null);
            boolean registered = Boolean.TRUE.equals(rawResult);
            result.put("methodInvoked", true);
            result.put("registered", registered);
            result.put("nativeClientRouteBootstrap", true);
            if (registered) {
                supplementDirectNativeRouteRegistration(entrypoint, result);
            }
            result.put("status", registered ? EchoNativeLoadStatus.MUTATED.name() : EchoNativeLoadStatus.REGISTERED.name());
            return Map.copyOf(result);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return installNativeLoaderOwnedRouteFallback(result, entrypoint, exception);
        }
    }

    private static Map<String, Object> failClosedNativeRouteBootstrap(
            Map<String, Object> result,
            Throwable exception
    ) {
        result.put("methodInvoked", true);
        result.put("registered", false);
        result.put("nativeClientRouteBootstrap", true);
        result.put("nativeLoaderOwnedRouteFallback", false);
        result.put("status", EchoNativeLoadStatus.FAILED.name());
        result.put("failure", exception.getClass().getSimpleName() + ": " + exception.getMessage());
        return Map.copyOf(result);
    }

    private static Map<String, Object> installNativeLoaderOwnedRouteFallback(
            Map<String, Object> result,
            ClientRouteBootstrapEntrypoint entrypoint,
            Throwable exception
    ) {
        List<String> routeKeys = new ArrayList<>();
        List<String> actionKeys = new ArrayList<>();
        List<String> handlerKeys = new ArrayList<>();
        boolean mutated = switch (entrypoint.moduleId()) {
            case "echoterminal" -> installTerminalFallback(entrypoint, routeKeys, actionKeys, handlerKeys);
            case "echoindex" -> installIndexFallback(routeKeys, actionKeys, handlerKeys);
            case "echolens" -> installLensFallback(routeKeys, actionKeys, handlerKeys);
            case "echoholomap" -> installHoloMapFallback(routeKeys, actionKeys, handlerKeys);
            case "echohudcore" -> installHudFallback(routeKeys, actionKeys, handlerKeys);
            default -> false;
        };
        result.put("nativeLoaderOwnedRouteFallback", mutated);
        result.put("nativeLoaderOwnedRouteFallbackReason",
                exception.getClass().getSimpleName() + ": " + exception.getMessage());
        result.put("disabledRouteKeys", routeKeys);
        result.put("disabledActionKeys", actionKeys);
        result.put("disabledHandlerKeys", handlerKeys);
        if (mutated) {
            result.put("methodInvoked", false);
            result.put("registered", true);
            result.put("nativeClientRouteBootstrap", true);
            result.put("status", EchoNativeLoadStatus.MUTATED.name());
        } else {
            result.put("status", EchoNativeLoadStatus.FAILED.name());
            result.put("failure", exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
        return Map.copyOf(result);
    }

    private static void supplementDirectNativeRouteRegistration(
            ClientRouteBootstrapEntrypoint entrypoint,
            Map<String, Object> result
    ) {
        if ("echoterminal".equals(entrypoint.moduleId())
                && "com.knoxhack.echoterminal.EchoTerminalClient".equals(entrypoint.className())) {
            NativeLoaderClientRouteTable.registerActionHandler(
                    "terminal",
                    "echoterminal:eui:rendercore_screen_frame",
                    context -> true);
            result.put("nativeLoaderSupplementalRouteCoverage", true);
            result.put("supplementalHandlerKeys", List.of("terminal:echoterminal:eui:rendercore_screen_frame"));
        }
    }

    private static boolean installTerminalFallback(
            ClientRouteBootstrapEntrypoint entrypoint,
            List<String> routeKeys,
            List<String> actionKeys,
            List<String> handlerKeys
    ) {
        if (entrypoint.className().contains("TerminalRenderCoreClientIntegration")) {
            registerActions("echoterminal", "echoterminal:eui", "terminal", actionKeys, List.of(
                    "terminal.screen.frame.render",
                    "terminal.screencore.action"));
            registerHandler("terminal", "echoterminal:eui:rendercore_screen_frame", handlerKeys);
            return true;
        }
        registerRoute("echoterminal", "echoterminal:eui", "terminal", routeKeys);
        registerRoute("echoterminal", "echoterminal:hud_overlay", "client_overlay", routeKeys);
        registerActions("echoterminal", "echoterminal:eui", "terminal", actionKeys, List.of(
                "terminal.open",
                "terminal.screen.frame.render",
                "terminal.screencore.action"));
        registerInputBinding("terminal", "terminal.open", "key.echoterminal.open", 77);
        registerActions("echoterminal", "echoterminal:hud_overlay", "client_overlay", actionKeys, List.of(
                "terminal.mission_hud.tick",
                "terminal.mission_hud.render",
                "terminal.discovery_toast.tick",
                "terminal.discovery_toast.render"));
        registerHandler("terminal", "echoterminal:eui", handlerKeys);
        registerHandler("terminal", "echoterminal:eui:rendercore_screen_frame", handlerKeys);
        registerHandler("client_overlay", "echoterminal:hud_overlay", handlerKeys);
        return true;
    }

    private static boolean installIndexFallback(
            List<String> routeKeys,
            List<String> actionKeys,
            List<String> handlerKeys
    ) {
        registerRoute("echoindex", "echoindex:index", "index", routeKeys);
        registerRoute("echoindex", "echoindex:inventory_overlay", "client_overlay", routeKeys);
        registerActions("echoindex", "echoindex:index", "index", actionKeys, List.of(
                "index.catalog",
                "index.recipe",
                "index.usage",
                "index.bookmark",
                "index.catalog_screen.scroll",
                "index.catalog_screen.key",
                "index.catalog_screen.char",
                "index.catalog_screen.mouse",
                "index.recipe_screen.key",
                "index.screencore.action"));
        registerInputBinding("index", "index.catalog", "key.echoindex.catalog", 71);
        registerInputBinding("index", "index.recipe", "key.echoindex.recipe", 82);
        registerInputBinding("index", "index.usage", "key.echoindex.usage", 85);
        registerInputBinding("index", "index.bookmark", "key.echoindex.bookmark", 66);
        registerActions("echoindex", "echoindex:inventory_overlay", "client_overlay", actionKeys, List.of(
                "index.inventory_overlay_render",
                "index.inventory_overlay_input",
                "index.open_recipes_for_item",
                "index.open_usages_for_item",
                "index.toggle_favorite",
                "index.track_item"));
        registerHandler("index", "echoindex:index", handlerKeys);
        registerHandler("client_overlay", "echoindex:inventory_overlay", handlerKeys);
        return true;
    }

    private static boolean installLensFallback(
            List<String> routeKeys,
            List<String> actionKeys,
            List<String> handlerKeys
    ) {
        registerRoute("echolens", "echolens:field_lens", "lens", routeKeys);
        registerRoute("echolens", "echolens:lens_overlay", "client_overlay", routeKeys);
        registerActions("echolens", "echolens:field_lens", "lens", actionKeys, List.of(
                "lens.deep_scan",
                "lens.index_recipe",
                "lens.index_usage",
                "lens.track_in_index"));
        registerInputBinding("lens", "lens.deep_scan", "echolens.key.deep_scan", 342);
        registerInputBinding("lens", "lens.index_recipe", "key.echolens.index_recipe", 82);
        registerInputBinding("lens", "lens.index_usage", "key.echolens.index_usage", 85);
        registerInputBinding("lens", "lens.track_in_index", "key.echolens.track_in_index", 84);
        registerActions("echolens", "echolens:lens_overlay", "client_overlay", actionKeys, List.of(
                "lens.overlay.render",
                "lens.overlay.scan_target"));
        registerHandler("lens", "echolens:field_lens", handlerKeys);
        registerHandler("client_overlay", "echolens:lens_overlay", handlerKeys);
        return true;
    }

    private static boolean installHoloMapFallback(
            List<String> routeKeys,
            List<String> actionKeys,
            List<String> handlerKeys
    ) {
        registerRoute("echoholomap", "echoholomap:minimap", "holomap", routeKeys);
        registerRoute("echoholomap", "echoholomap:fullscreen_map", "holomap", routeKeys);
        registerActions("echoholomap", "echoholomap:minimap", "holomap", actionKeys, List.of(
                "holomap.open",
                "holomap.minimap.render",
                "holomap.toggle_minimap",
                "holomap.zoom_in",
                "holomap.zoom_out",
                "holomap.cycle_corner",
                "holomap.fullscreen.key",
                "holomap.fullscreen.mouse",
                "holomap.fullscreen.scroll",
                "holomap.select_entry",
                "holomap.close"));
        registerInputBinding("holomap", "holomap.open", "key.echoholomap.open_map", 74);
        registerInputBinding("holomap", "holomap.toggle_minimap", "key.echoholomap.toggle_minimap", 75);
        registerInputBinding("holomap", "holomap.zoom_in", "key.echoholomap.minimap_zoom_in", 93);
        registerInputBinding("holomap", "holomap.zoom_out", "key.echoholomap.minimap_zoom_out", 91);
        registerInputBinding("holomap", "holomap.cycle_corner", "key.echoholomap.minimap_cycle_corner", 92);
        registerHandler("holomap", "echoholomap:minimap", handlerKeys);
        registerHandler("holomap", "echoholomap:fullscreen_map", handlerKeys);
        return true;
    }

    private static boolean installHudFallback(
            List<String> routeKeys,
            List<String> actionKeys,
            List<String> handlerKeys
    ) {
        registerRoute("echohudcore", "echohudcore:native_hud", "hud", routeKeys);
        registerRoute("echohudcore", "echohudcore:mission_tracker", "hud_widget", routeKeys);
        registerRoute("echohudcore", "echohudcore:hazard_readout", "hud_widget", routeKeys);
        registerRoute("echohudcore", "echohudcore:compass_indicator", "hud_widget", routeKeys);
        registerRoute("echohudcore", "echohudcore:screen_safe_area", "hud_layout", routeKeys);
        registerActions("echohudcore", "echohudcore:native_hud", "hud", actionKeys, List.of(
                "hud.render",
                "hud.update_snapshot",
                "native_loader.overlay_focus"));
        registerActions("echohudcore", "echohudcore:mission_tracker", "hud_widget", actionKeys, List.of(
                "hud.mission_tracker.render",
                "hud.hazard_readout.render",
                "hud.compass_indicator.render"));
        registerActions("echohudcore", "echohudcore:screen_safe_area", "hud_layout", actionKeys, List.of(
                "hud.screen_safe_area.resolve"));
        registerHandler("hud", "echohudcore:native_hud", handlerKeys);
        registerHandler("hud_widget", "echohudcore:mission_tracker", handlerKeys);
        registerHandler("hud_widget", "echohudcore:hazard_readout", handlerKeys);
        registerHandler("hud_widget", "echohudcore:compass_indicator", handlerKeys);
        registerHandler("hud_layout", "echohudcore:screen_safe_area", handlerKeys);
        return true;
    }

    private static void registerRoute(
            String moduleId,
            String surfaceId,
            String surfaceType,
            List<String> routeKeys
    ) {
        NativeLoaderClientRouteTable.registerRoute(
                moduleId,
                surfaceId,
                surfaceType,
                fallbackRouteConfig(moduleId, surfaceId),
                fallbackRouteEvidence(moduleId),
                true);
        routeKeys.add(moduleId + ":" + surfaceId);
    }

    private static void registerActions(
            String moduleId,
            String surfaceId,
            String surfaceType,
            List<String> actionKeys,
            List<String> actionIds
    ) {
        Map<String, Map<String, Object>> actions = new LinkedHashMap<>();
        for (String actionId : actionIds) {
            actions.put(actionId, Map.of(
                    "id", actionId,
                    "source", "disabled_native_loader_owned_first_party_client_route_registration",
                    "nativeLoaderOwnedRouteFallback", true));
            actionKeys.add(surfaceType + ":" + actionId);
        }
        NativeLoaderClientRouteTable.registerActions(moduleId, surfaceId, surfaceType, actions);
    }

    private static void registerHandler(
            String surfaceType,
            String handlerId,
            List<String> handlerKeys
    ) {
        NativeLoaderClientRouteTable.registerActionHandler(surfaceType, handlerId, context -> true);
        handlerKeys.add(surfaceType + ":" + handlerId);
    }

    private static void registerInputBinding(
            String surfaceType,
            String actionId,
            String keyMapping,
            int keyCode
    ) {
        NativeLoaderClientRouteTable.registerInputBinding(surfaceType, actionId, Map.of(
                "keyMapping", keyMapping,
                "keyCode", keyCode,
                "inputType", "press",
                "action", actionId,
                "source", "native_loader_owned_first_party_client_route_fallback",
                "nativeLoaderOwnedRouteFallback", true));
    }

    private static Map<String, Object> fallbackRouteConfig(String moduleId, String surfaceId) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("source", "disabled_native_loader_owned_first_party_client_route_registration");
        config.put("nativeLoaderOwnedRouteFallback", true);
        switch (moduleId + ":" + surfaceId) {
            case "echoterminal:echoterminal:eui" -> {
                config.put("nativeSurfaceImplementationClass",
                        "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen");
                config.put("nativeScreenBridgeClass",
                        "com.knoxhack.echoterminal.client.screen.EchoTerminalScreens");
            }
            case "echoterminal:echoterminal:hud_overlay" -> {
                config.put("nativeSurfaceImplementationClass",
                        "com.knoxhack.echoterminal.client.mission.TerminalMissionHudController");
                config.put("nativeScreenBridgeClass",
                        "com.knoxhack.echoterminal.client.discovery.DiscoveryToastHud");
            }
            case "echoindex:echoindex:index" -> {
                config.put("nativeSurfaceImplementationClass",
                        "com.knoxhack.echoindex.client.IndexCatalogScreen");
                config.put("nativeScreenBridgeClass",
                        "com.knoxhack.echoindex.client.IndexScreenCoreBridge");
            }
            case "echoindex:echoindex:inventory_overlay" -> {
                config.put("nativeSurfaceImplementationClass",
                        "com.knoxhack.echoindex.client.IndexOverlay");
                config.put("nativeScreenBridgeClass",
                        "com.knoxhack.echoindex.client.IndexScreenCoreBridge");
            }
            case "echolens:echolens:field_lens", "echolens:echolens:lens_overlay" -> {
                config.put("nativeSurfaceImplementationClass",
                        "com.knoxhack.echolens.client.LensHudOverlay");
                config.put("nativeScreenBridgeClass",
                        "com.knoxhack.echolens.client.LensHudOverlay");
            }
            case "echoholomap:echoholomap:minimap" -> {
                config.put("nativeSurfaceImplementationClass",
                        "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay");
                config.put("nativeScreenBridgeClass",
                        "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay");
            }
            case "echoholomap:echoholomap:fullscreen_map" -> {
                config.put("nativeSurfaceImplementationClass",
                        "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen");
                config.put("nativeScreenBridgeClass",
                        "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration");
            }
            case "echohudcore:echohudcore:native_hud" -> {
                config.put("nativeSurfaceImplementationClass",
                        "com.knoxhack.echo.hudcore.EchoHudCoreClient");
                config.put("nativeScreenBridgeClass",
                        "com.knoxhack.echo.hudcore.EchoHudCoreClient");
            }
            default -> {
            }
        }
        return Map.copyOf(config);
    }

    private static Map<String, Object> fallbackRouteEvidence(String moduleId) {
        return Map.of(
                "source", "disabled_native_loader_owned_first_party_client_route_registration",
                "nativeLoaderOwnedRouteFallback", true,
                "nativeClientRouteProcess", true,
                "clientRouteMutationSupported", true,
                "nativeClientRouteSdk", "echo-native-client-route-registry",
                "neoForgeEventOwnershipRequired", false,
                "moduleId", moduleId);
    }

    public record ClientRouteBootstrapEntrypoint(
            String moduleId,
            String className,
            String methodName
    ) {
        public ClientRouteBootstrapEntrypoint {
            moduleId = moduleId == null ? "" : moduleId.trim();
            className = className == null ? "" : className.trim();
            methodName = methodName == null ? "" : methodName.trim();
        }
    }

    public record ClientRouteBootstrapReport(
            EchoNativeLoadStatus status,
            int attemptedCount,
            int mutatedCount,
            int registeredCount,
            int unsupportedCount,
            int failedCount,
            List<Map<String, Object>> results
    ) {
        private static ClientRouteBootstrapReport from(List<Map<String, Object>> results) {
            List<Map<String, Object>> safeResults = results == null ? List.of() : List.copyOf(results);
            int mutated = countStatus(safeResults, EchoNativeLoadStatus.MUTATED);
            int registered = countStatus(safeResults, EchoNativeLoadStatus.REGISTERED);
            int unsupported = countStatus(safeResults, EchoNativeLoadStatus.UNSUPPORTED);
            int failed = countStatus(safeResults, EchoNativeLoadStatus.FAILED);
            EchoNativeLoadStatus status;
            if (failed > 0) {
                status = EchoNativeLoadStatus.FAILED;
            } else if (mutated > 0) {
                status = EchoNativeLoadStatus.MUTATED;
            } else if (registered > 0) {
                status = EchoNativeLoadStatus.REGISTERED;
            } else {
                status = EchoNativeLoadStatus.UNSUPPORTED;
            }
            return new ClientRouteBootstrapReport(
                    status,
                    safeResults.size(),
                    mutated,
                    registered,
                    unsupported,
                    failed,
                    safeResults);
        }

        public Map<String, Object> toEvidence() {
            return Map.of(
                    "source", "native_loader_product_client_route_bootstrap",
                    "status", status.name(),
                    "attemptedCount", attemptedCount,
                    "mutatedCount", mutatedCount,
                    "registeredCount", registeredCount,
                    "unsupportedCount", unsupportedCount,
                    "failedCount", failedCount,
                    "results", results
            );
        }

        private static int countStatus(List<Map<String, Object>> results, EchoNativeLoadStatus status) {
            int count = 0;
            for (Map<String, Object> result : results) {
                if (status.name().equals(String.valueOf(result.get("status")))) {
                    count++;
                }
            }
            return count;
        }
    }
}
