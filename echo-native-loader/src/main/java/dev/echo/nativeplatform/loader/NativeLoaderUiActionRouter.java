package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NativeLoaderUiActionRouter {
    public static final String SERVICE_ID = "echo.native.ui_action_router";
    private static volatile Context context = Context.empty();

    private NativeLoaderUiActionRouter() {
    }

    public static void configure(Context context) {
        NativeLoaderUiActionRouter.context = context == null ? Context.empty() : context;
    }

    public static String focusPath(String mode, String previousMode) {
        String normalizedMode = normalizeMode(mode);
        return switch (normalizedMode) {
            case "TERMINAL" -> "terminal:input";
            case "INDEX" -> "index:search";
            case "LENS" -> "lens:scan";
            case "RECOVERY" -> "recovery:recover";
            case "PAUSE" -> "pause:resume:" + fallbackPreviousMode(previousMode);
            default -> normalizedMode.toLowerCase(java.util.Locale.ROOT) + ":surface";
        };
    }

    public static Map<String, Object> routeInitialFocus(String mode, String previousMode) {
        String normalizedMode = normalizeMode(mode);
        String focusedControl = focusPath(normalizedMode, previousMode);
        return handled(Map.of(
                "focusedControl", focusedControl,
                "initialFocusRouted", true,
                "effect", "focus:initial:" + normalizedMode.toLowerCase(java.util.Locale.ROOT)
        ));
    }

    public static Map<String, Object> routeCharacter(
            String mode,
            String focusedControl,
            String terminalBuffer,
            String indexBuffer,
            char codePoint
    ) {
        if (codePoint < 32 || codePoint == 127) {
            return ignored("character:control");
        }
        String normalizedMode = normalizeMode(mode);
        if ("TERMINAL".equals(normalizedMode) && "terminal:input".equals(focusedControl)) {
            return handled(Map.of(
                    "targetBuffer", "terminalBuffer",
                    "value", (normalize(terminalBuffer) + codePoint).strip(),
                    "effect", "terminal-character"
            ));
        }
        if ("INDEX".equals(normalizedMode) && "index:search".equals(focusedControl)) {
            return handled(Map.of(
                    "targetBuffer", "indexBuffer",
                    "value", (normalize(indexBuffer) + codePoint).strip(),
                    "effect", "index-character"
            ));
        }
        return ignored("character:unfocused");
    }

    public static Map<String, Object> routeKey(String keyName, String mode, String previousMode) {
        String normalizedKey = normalize(keyName).toUpperCase(java.util.Locale.ROOT);
        String normalizedMode = normalizeMode(mode);
        Map<String, Object> productActionRoute = productActionRoute(normalizedKey, normalizedMode, previousMode);
        if (productActionRoute != null) {
            return productActionRoute;
        }
        return switch (normalizedKey) {
            case "ESCAPE" -> handled(Map.of(
                    "destinationMode", "PAUSE".equals(normalizedMode) ? fallbackPreviousMode(previousMode) : "PAUSE",
                    "destinationPreviousMode", "PAUSE".equals(normalizedMode) ? "WIKI" : normalizedMode,
                    "effect", "route:escape"
            ));
            case "M" -> route("TERMINAL");
            case "G", "R", "U" -> route("INDEX");
            case "B" -> route("INDEX");
            case "LEFT_ALT" -> route("LENS");
            case "J", "K", "RIGHT_BRACKET", "LEFT_BRACKET", "BACKSLASH" -> route("HOLOMAP");
            case "N" -> handled(Map.of(
                    "destinationMode", "SIGNALOS",
                    "destinationPreviousMode", normalizedMode,
                    "signalOsTerminalActive", true,
                    "effect", "route:signalos"
            ));
            default -> ignored("route:unmapped:" + normalizedKey);
        };
    }

    public static Map<String, Object> routeEditKey(
            String keyName,
            String mode,
            String focusedControl,
            String terminalBuffer,
            String indexBuffer
    ) {
        String normalizedKey = normalize(keyName).toUpperCase(java.util.Locale.ROOT);
        String normalizedMode = normalizeMode(mode);
        if (!"BACKSPACE".equals(normalizedKey)) {
            return ignored("edit:unmapped:" + normalizedKey);
        }
        if ("TERMINAL".equals(normalizedMode) && "terminal:input".equals(focusedControl)) {
            return handled(Map.of(
                    "targetBuffer", "terminalBuffer",
                    "value", removeLast(terminalBuffer),
                    "effect", "terminal-backspace"
            ));
        }
        if ("INDEX".equals(normalizedMode) && "index:search".equals(focusedControl)) {
            return handled(Map.of(
                    "targetBuffer", "indexBuffer",
                    "value", removeLast(indexBuffer),
                    "effect", "index-backspace"
            ));
        }
        return ignored("edit:unfocused");
    }

    public static Map<String, Object> routeMouseClick(String mode, String previousMode, Map<String, Object> state) {
        String normalizedMode = normalizeMode(mode);
        String focusedControl = focusPath(normalizedMode, previousMode);
        Map<String, Object> clickState = new LinkedHashMap<>();
        if (state != null) {
            clickState.putAll(state);
        }
        clickState.put("focusedControl", focusedControl);
        clickState.put("mouseRouted", true);

        Map<String, Object> action = activate(normalizedMode, clickState);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("focusedControl", focusedControl);
        result.put("mouseRouted", true);
        result.put("effect", "mouse:focus:" + normalizedMode.toLowerCase(java.util.Locale.ROOT));
        if (Boolean.TRUE.equals(action.get("handled"))) {
            result.put("outputKey", action.get("outputKey"));
            result.put("output", action.get("output"));
            result.put("executedKey", action.get("executedKey"));
            result.put("effects", action.get("effects"));
            result.put("effect", "mouse:activate:" + normalizedMode.toLowerCase(java.util.Locale.ROOT));
        }
        return handled(result);
    }

    public static Map<String, Object> routeListNavigation(String keyName, String mode, int selectedIndex) {
        String normalizedKey = normalize(keyName).toUpperCase(java.util.Locale.ROOT);
        String normalizedMode = normalizeMode(mode);
        List<String> options = listOptions(normalizedMode);
        if (options.isEmpty()) {
            return ignored("list:unsupported:" + normalizedMode);
        }
        int current = clampIndex(selectedIndex, options.size());
        int next = switch (normalizedKey) {
            case "UP" -> current == 0 ? options.size() - 1 : current - 1;
            case "DOWN" -> current == options.size() - 1 ? 0 : current + 1;
            default -> -1;
        };
        if (next < 0) {
            return ignored("list:unmapped:" + normalizedKey);
        }
        return handled(Map.of(
                "selectedIndex", next,
                "selectedOption", options.get(next),
                "optionCount", options.size(),
                "effect", "list:" + normalizedMode.toLowerCase(java.util.Locale.ROOT) + ":" + normalizedKey.toLowerCase(java.util.Locale.ROOT),
                "focusPath", focusPath(normalizedMode, "WIKI")
        ));
    }

    public static Map<String, Object> routeNotificationDismiss(Object notifications) {
        List<Map<String, Object>> source = notificationList(notifications);
        if (source.isEmpty()) {
            return ignored("notification:empty");
        }
        Map<String, Object> dismissed = source.get(0);
        List<Map<String, Object>> remaining = source.stream().skip(1).map(Map::copyOf).toList();
        return handled(Map.of(
                "dismissedId", String.valueOf(dismissed.get("id")),
                "dismissedMessage", String.valueOf(dismissed.get("message")),
                "remainingNotifications", remaining,
                "remainingMessages", remaining.stream()
                        .map(notification -> String.valueOf(notification.get("message")))
                        .toList(),
                "effect", "notification:dismiss-oldest"
        ));
    }

    public static Map<String, Object> routeSettingsAdjustment(String selectedOption, double hudScale, boolean subtitles) {
        String option = normalize(selectedOption).toUpperCase(java.util.Locale.ROOT);
        if ("HUD SCALE".equals(option)) {
            double nextScale = hudScale >= 1.25D ? 1.0D : 1.25D;
            return handled(Map.of(
                    "settingsHudScale", nextScale,
                    "settingsSubtitles", subtitles,
                    "settingsAppliedOption", "HUD Scale",
                    "settingsOutput", "HUD scale " + nextScale,
                    "effect", "settings:hud_scale"
            ));
        }
        if ("SUBTITLES".equals(option)) {
            boolean nextSubtitles = !subtitles;
            return handled(Map.of(
                    "settingsHudScale", hudScale,
                    "settingsSubtitles", nextSubtitles,
                    "settingsAppliedOption", "Subtitles",
                    "settingsOutput", "Subtitles " + (nextSubtitles ? "enabled" : "disabled"),
                    "effect", "settings:subtitles"
            ));
        }
        return ignored("settings:unsupported:" + option);
    }

    public static Map<String, Object> routePauseOption(String selectedOption, String previousMode) {
        String option = normalize(selectedOption);
        if (option.isBlank()) {
            option = "Resume";
        }
        String normalizedOption = option.toUpperCase(java.util.Locale.ROOT);
        String resumeTarget = fallbackPreviousMode(previousMode);
        return switch (normalizedOption) {
            case "RESUME" -> handled(Map.of(
                    "destinationMode", resumeTarget,
                    "destinationPreviousMode", "WIKI",
                    "selectedOption", "Resume",
                    "effect", "pause:resume"
            ));
            case "SETTINGS" -> handled(Map.of(
                    "destinationMode", "SETTINGS",
                    "destinationPreviousMode", "PAUSE",
                    "selectedOption", "Settings",
                    "effect", "pause:settings"
            ));
            case "QUIT TO MAIN MENU" -> handled(Map.of(
                    "destinationMode", "MAIN_MENU",
                    "destinationPreviousMode", "PAUSE",
                    "selectedOption", "Quit to Main Menu",
                    "effect", "pause:main_menu"
            ));
            default -> ignored("pause:unsupported:" + normalizedOption);
        };
    }

    public static Map<String, Object> routeMainMenuOption(String selectedOption) {
        NativeLoaderClientUiHost.seedBuiltInProductRoutes();
        String option = normalize(selectedOption);
        if (option.isBlank()) {
            option = "Continue";
        }
        String normalizedOption = option.toUpperCase(java.util.Locale.ROOT);
        if (newRunOption(normalizedOption)) {
            return routeNativeMainMenuOption(option, "menu.new_run", Map.of(
                    "destinationMode", "WORLD_SETUP",
                    "destinationPreviousMode", "MAIN_MENU",
                    "selectedOption", option,
                    "mainMenuOutput", option + " selected: opening Native World Setup",
                    "quitRequested", false,
                    "effect", "main_menu:new_run_world_setup"
            ));
        }
        return switch (normalizedOption) {
            case "CONTINUE" -> routeNativeMainMenuOption("Continue", "menu.continue", Map.of(
                    "destinationMode", "WIKI",
                    "destinationPreviousMode", "MAIN_MENU",
                    "selectedOption", "Continue",
                    "mainMenuOutput", "Continue selected: opening Wiki",
                    "quitRequested", false,
                    "effect", "main_menu:continue"
            ));
            case "SETTINGS" -> routeNativeMainMenuOption("Settings", "menu.settings", Map.of(
                    "destinationMode", "SETTINGS",
                    "destinationPreviousMode", "MAIN_MENU",
                    "selectedOption", "Settings",
                    "mainMenuOutput", "Settings selected: opening Settings",
                    "quitRequested", false,
                    "effect", "main_menu:settings"
            ));
            case "QUIT" -> routeNativeMainMenuOption("Quit", "menu.quit", Map.of(
                    "destinationMode", "MAIN_MENU",
                    "destinationPreviousMode", "MAIN_MENU",
                    "selectedOption", "Quit",
                    "mainMenuOutput", "Quit selected: native quit requested",
                    "quitRequested", true,
                    "effect", "main_menu:quit_requested"
            ));
            default -> ignored("main_menu:unsupported:" + normalizedOption);
        };
    }

    private static Map<String, Object> routeNativeMainMenuOption(
            String selectedOption,
            String actionId,
            Map<String, Object> routed
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_loader_generated_main_menu");
        metadata.put("eventType", "generated_main_menu_selection");
        metadata.put("selectedOption", selectedOption);
        metadata.put("nativeRouteOwner", "EchoNativeClientRouteRegistries");
        metadata.put("neoForgeEventOwnershipRequired", false);
        EchoNativeLoadStatus status = EchoNativeClientRouteRegistries.get()
                .dispatchStatus("main_menu", actionId, Map.copyOf(metadata));
        if (status != EchoNativeLoadStatus.MUTATED) {
            return ignored("main_menu:native-route-unavailable:" + actionId);
        }
        Map<String, Object> result = new LinkedHashMap<>(routed);
        result.put("nativeRouteActionId", actionId);
        result.put("nativeRouteStatus", status.name());
        result.put("nativeRouteOwner", "EchoNativeClientRouteRegistries");
        result.put("neoForgeEventOwnershipRequired", false);
        return handled(Map.copyOf(result));
    }

    public static Map<String, Object> routeWorldSetupCreate() {
        NativeLoaderClientUiHost.seedBuiltInProductRoutes();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_loader_generated_world_setup");
        metadata.put("eventType", "generated_world_setup_create");
        metadata.put("nativeRouteOwner", "EchoNativeClientRouteRegistries");
        metadata.put("neoForgeEventOwnershipRequired", false);
        EchoNativeLoadStatus status = EchoNativeClientRouteRegistries.get()
                .dispatchStatus("world_setup", "world_setup.create", Map.copyOf(metadata));
        if (status != EchoNativeLoadStatus.MUTATED) {
            return ignored("world_setup:native-route-unavailable:world_setup.create");
        }
        return handled(Map.of(
                "destinationMode", "MISSION_LOG",
                "destinationPreviousMode", "WORLD_SETUP",
                "worldSetupOutput", "Native Loader owned world setup accepted",
                "nativeRouteActionId", "world_setup.create",
                "nativeRouteStatus", status.name(),
                "nativeRouteOwner", "EchoNativeClientRouteRegistries",
                "neoForgeEventOwnershipRequired", false,
                "effect", "world_setup:create"
        ));
    }

    public static Map<String, Object> routeHudUpdate(Map<String, Object> state) {
        if (!hostSupportsAny(state, "native.ui.hud_refresh")) {
            return ignored("activate:unsupported-host-action:native.ui.hud_refresh");
        }
        Map<String, Object> hud = object(context.dataSources().get().get("hud"));
        int currentHealth = integer(state == null ? null : state.get("hudHealth"), integer(hud.get("health"), 92));
        int nextHealth = Math.max(0, currentHealth - 7);
        String hazard = String.valueOf(hud.getOrDefault("hazard", ""));
        String mission = String.valueOf(hud.getOrDefault("mission", ""));
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("hudHealth", nextHealth);
        update.put("hudHazard", hazard);
        update.put("hudMission", mission);
        update.put("hudUpdateOutput", "HUD refreshed: health " + nextHealth + " / " + hazard);
        update.put("runtimeActionId", "native.ui.hud_refresh");
        update.put("runtimeEventName", "client_tick");
        update.put("effect", "hud:update:health_hazard_mission");
        return handled(update);
    }

    public static Map<String, Object> routeCameraCinematicFrame(Map<String, Object> state) {
        Map<String, Object> sources = context.dataSources().get();
        Map<String, Object> camera = object(sources.get("camera"));
        Map<String, Object> cinematic = object(sources.get("cinematic"));
        int currentFrame = integer(state == null ? null : state.get("cinematicFrame"), 0);
        int nextFrame = currentFrame + 1;
        String cameraMode = String.valueOf(camera.get("mode"));
        String cue = String.valueOf(cinematic.get("cue"));
        return handled(Map.of(
                "cameraMode", cameraMode,
                "cameraFov", integer(camera.get("fov"), 72),
                "cameraTarget", String.valueOf(camera.get("target")),
                "cinematicCue", cue,
                "cinematicFrame", nextFrame,
                "cinematicLetterbox", Boolean.TRUE.equals(cinematic.get("letterbox")),
                "cinematicSubtitle", String.valueOf(cinematic.get("subtitle")),
                "cinematicOutput", "Camera " + cameraMode + " frame " + nextFrame + " cue " + cue,
                "effect", "camera_cinematic:frame:" + cue
        ));
    }

    public static Map<String, Object> routeMissionLogUpdate(Map<String, Object> state) {
        if (!hostSupportsAny(state, "native.ui.mission_log_update")) {
            return ignored("activate:unsupported-host-action:native.ui.mission_log_update");
        }
        Map<String, Object> mission = object(context.dataSources().get().get("missionLog"));
        double currentProgress = doubleValue(state == null ? null : state.get("missionProgress"), 0.25D);
        double nextProgress = currentProgress >= 0.5D ? currentProgress : 0.5D;
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("missionId", String.valueOf(mission.get("missionId")));
        update.put("missionTitle", String.valueOf(mission.get("title")));
        update.put("missionObjective", String.valueOf(mission.get("objective")));
        update.put("missionProgress", nextProgress);
        update.put("missionStatus", "UPDATED");
        update.put("missionUpdateLine", "Drop pod signal confirmed");
        update.put("runtimeActionId", "native.ui.mission_log_update");
        update.put("runtimeEventName", "mission.objective_completed");
        update.put("effect", "mission:update:" + mission.get("missionId"));
        return handled(update);
    }

    public static Map<String, Object> activate(String mode, Map<String, Object> state) {
        String normalizedMode = normalizeMode(mode);
        String focusedControl = string(state, "focusedControl", "");
        return switch (normalizedMode) {
            case "TERMINAL" -> activateTerminal(focusedControl, state);
            case "INDEX" -> activateIndex(focusedControl, state);
            case "LENS" -> activateLens(focusedControl, state);
            case "RECOVERY" -> activateRecovery(focusedControl, state);
            default -> ignored("activate:unsupported:" + normalizedMode);
        };
    }

    private static Map<String, Object> activateTerminal(String focusedControl, Map<String, Object> state) {
        if (!"terminal:input".equals(focusedControl)) {
            return ignored("terminal:not-focused");
        }
        if (!hostSupportsAny(state, "native.ui.terminal_command")) {
            return ignored("activate:unsupported-host-action:native.ui.terminal_command");
        }
        String terminalBuffer = string(state, "terminalBuffer", "");
        Map<String, Object> result = context.terminalExecutor().apply(terminalBuffer);
        if (!Boolean.TRUE.equals(result.get("handled"))) {
            return ignored("terminal:unhandled");
        }
        Map<String, Object> action = new LinkedHashMap<>(action("terminalOutput", "terminalCommandExecuted", result));
        action.put("runtimeActionId", "native.ui.terminal_command");
        action.put("runtimeEventName", "command_execution");
        action.put("terminalCommand", terminalBuffer);
        return Map.copyOf(action);
    }

    private static Map<String, Object> activateIndex(String focusedControl, Map<String, Object> state) {
        if (!"index:search".equals(focusedControl)) {
            return ignored("index:not-focused");
        }
        if (!hostSupportsAny(state, "native.ui.index_search")) {
            return ignored("activate:unsupported-host-action:native.ui.index_search");
        }
        String indexBuffer = string(state, "indexBuffer", "");
        Map<String, Object> result = context.indexSearcher().apply(indexBuffer);
        if (!Boolean.TRUE.equals(result.get("handled"))) {
            return ignored("index:unhandled");
        }
        Map<String, Object> action = new LinkedHashMap<>(action("indexOutput", "indexSearchExecuted", result));
        action.put("runtimeActionId", "native.ui.index_search");
        action.put("runtimeEventName", "player.terminal_opened");
        action.put("indexQuery", indexBuffer);
        return Map.copyOf(action);
    }

    private static Map<String, Object> activateLens(String focusedControl, Map<String, Object> state) {
        if (!"lens:scan".equals(focusedControl)) {
            return ignored("lens:not-focused");
        }
        if (!hostSupportsAny(state, "player.scanner_used", "native.ui.use_scanner")) {
            return ignored("activate:unsupported-host-action:player.scanner_used");
        }
        Map<String, Object> lens = object(context.dataSources().get().get("lens"));
        Map<String, Object> result = context.lensScanner().apply(String.valueOf(lens.get("target")));
        if (!Boolean.TRUE.equals(result.get("handled"))) {
            return ignored("lens:unhandled");
        }
        return action("lensOutput", "lensScanExecuted", result);
    }

    private static Map<String, Object> activateRecovery(String focusedControl, Map<String, Object> state) {
        if (!"recovery:recover".equals(focusedControl)) {
            return ignored("recovery:not-focused");
        }
        if (!hostSupportsAny(state, "player.inventory.grant")) {
            return ignored("activate:unsupported-host-action:player.inventory.grant");
        }
        Map<String, Object> result = context.recoveryExecutor().get();
        if (!Boolean.TRUE.equals(result.get("handled"))) {
            return ignored("recovery:unhandled");
        }
        Map<String, Object> action = new LinkedHashMap<>(action("recoveryOutput", "recoveryActionExecuted", result));
        action.put("runtimeActionId", "player.inventory.grant");
        String recoveryItemId = context.recoveryItemId().get();
        if (!recoveryItemId.isBlank()) {
            action.put("grantItemId", recoveryItemId);
            action.put("grantItemCount", 1);
        }
        return Map.copyOf(action);
    }

    private static Map<String, Object> productActionRoute(String normalizedKey, String normalizedMode, String previousMode) {
        NativeLoaderPhysicalRouteRequirements.RouteSpec primaryRoute =
                NativeLoaderPhysicalRouteRequirements.primaryRouteForKey(normalizedKey);
        NativeLoaderPhysicalRouteRequirements.RouteSpec route = primaryRoute;
        NativeLoaderPhysicalRouteRequirements.RouteSpec contextualRoute =
                NativeLoaderPhysicalRouteRequirements.contextualRouteForKey(normalizedKey);
        if (contextualRoute != null && (primaryRoute == null || !primaryRoute.surface().equals(normalizedMode))) {
            route = contextualRoute;
        }
        if (route == null || !"action".equals(route.routeType())) {
            return null;
        }
        return handled(Map.of(
                "destinationMode", route.surface(),
                "destinationPreviousMode", fallbackPreviousMode(previousMode),
                "productActionKey", normalizedKey,
                "productAction", route.action(),
                "productActionContextual", route.contextual(),
                "effect", "route:product_action:" + normalizedKey
        ));
    }

    private static Map<String, Object> route(String mode) {
        return handled(Map.of(
                "destinationMode", mode,
                "destinationPreviousMode", "WIKI",
                "effect", "route:" + mode.toLowerCase(java.util.Locale.ROOT)
        ));
    }

    private static Map<String, Object> action(String outputKey, String executedKey, Map<String, Object> result) {
        return handled(Map.of(
                "outputKey", outputKey,
                "output", String.valueOf(result.get("output")),
                "executedKey", executedKey,
                "effects", result.get("effects")
        ));
    }

    private static List<String> listOptions(String mode) {
        return switch (mode) {
            case "MAIN_MENU" -> strings(object(context.dataSources().get().get("mainMenu")).get("options"));
            case "WORLD_SETUP" -> List.of("Create Native World", "Back");
            case "PAUSE" -> strings(object(context.dataSources().get().get("pauseFlow")).get("options"));
            case "SETTINGS" -> List.of("Profile", "Theme", "Input Mode", "HUD Scale", "Subtitles");
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> notificationList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .map(Map::copyOf)
                    .toList();
        }
        return context.notificationQueue().get();
    }

    private static int clampIndex(int selectedIndex, int size) {
        if (size <= 0) {
            return 0;
        }
        if (selectedIndex < 0) {
            return 0;
        }
        if (selectedIndex >= size) {
            return size - 1;
        }
        return selectedIndex;
    }

    private static int integer(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static Map<String, Object> handled(Map<String, Object> values) {
        Map<String, Object> result = new LinkedHashMap<>(values);
        result.put("handled", true);
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        result.put("nativeUiActionRouterServiceId", SERVICE_ID);
        result.put("routerClass", NativeLoaderUiActionRouter.class.getSimpleName());
        return Map.copyOf(result);
    }

    private static Map<String, Object> ignored(String reason) {
        return Map.of(
                "handled", false,
                "reason", reason,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true,
                "nativeUiActionRouterServiceId", SERVICE_ID,
                "routerClass", NativeLoaderUiActionRouter.class.getSimpleName()
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static boolean newRunOption(String normalizedOption) {
        return normalizedOption.contains("NEW") && normalizedOption.contains("RUN");
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        if (values == null) {
            return fallback;
        }
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean hostSupportsAny(Map<String, Object> state, String... actionIds) {
        if (!Boolean.TRUE.equals(state == null ? null : state.get("runtimeHostActionGateActive"))) {
            return true;
        }
        if (state == null || actionIds == null || actionIds.length == 0) {
            return false;
        }
        Object supported = state.get("runtimeSupportedActions");
        if (supported instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                String action = normalize(String.valueOf(entry));
                for (String actionId : actionIds) {
                    if (action.equals(actionId)) {
                        return true;
                    }
                }
            }
            return false;
        }
        String supportedText = normalize(String.valueOf(supported));
        if (supportedText.isBlank()) {
            return false;
        }
        for (String actionId : actionIds) {
            if (supportedActionTextContains(supportedText, actionId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean supportedActionTextContains(String supportedText, String actionId) {
        if (supportedText == null || actionId == null || actionId.isBlank()) {
            return false;
        }
        for (String token : supportedText.split("[,;\\s\\[\\]\"']+")) {
            if (token.trim().equals(actionId)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String removeLast(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.substring(0, normalized.length() - 1);
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

    private static String normalizeMode(String mode) {
        String normalized = normalize(mode).toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "TERMINAL" : normalized;
    }

    private static String fallbackPreviousMode(String previousMode) {
        return previousMode == null || previousMode.isBlank() ? "WIKI" : previousMode;
    }

    public record Context(
            Supplier<Map<String, Object>> dataSources,
            Function<String, Map<String, Object>> terminalExecutor,
            Function<String, Map<String, Object>> indexSearcher,
            Function<String, Map<String, Object>> lensScanner,
            Supplier<Map<String, Object>> recoveryExecutor,
            Supplier<List<Map<String, Object>>> notificationQueue,
            Supplier<String> recoveryItemId
    ) {
        public static Context empty() {
            return new Context(
                    Map::of,
                    ignoredAction("terminal"),
                    ignoredAction("index"),
                    ignoredAction("lens"),
                    () -> Map.of("handled", false, "reason", "recovery:unconfigured"),
                    List::of,
                    () -> ""
            );
        }

        private static Function<String, Map<String, Object>> ignoredAction(String action) {
            return value -> Map.of("handled", false, "reason", action + ":unconfigured");
        }
    }
}
