package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class NativeLoaderLiveHudRenderBridge {
    public static final String SERVICE_ID = "echo.native.live_hud_render_bridge";
    private static final int GLFW_PRESS = 1;
    private static final Map<String, Method> RENDERERS = new ConcurrentHashMap<>();
    private static final AtomicLong FRAMES = new AtomicLong();
    private static final List<GameplayInputBinding> GAMEPLAY_INPUT_BINDINGS = List.of(
            new GameplayInputBinding(77, "key.echoterminal.open", "terminal", "terminal.open"),
            new GameplayInputBinding(71, "key.echoindex.catalog", "index", "index.catalog"),
            new GameplayInputBinding(82, "key.echoindex.recipe", "index", "index.recipe"),
            new GameplayInputBinding(85, "key.echoindex.usage", "index", "index.usage"),
            new GameplayInputBinding(66, "key.echoindex.bookmark", "index", "index.bookmark"),
            new GameplayInputBinding(342, "echolens.key.deep_scan", "lens", "lens.deep_scan"),
            new GameplayInputBinding(74, "key.echoholomap.open_map", "holomap", "holomap.open"),
            new GameplayInputBinding(75, "key.echoholomap.toggle_minimap", "holomap", "holomap.toggle_minimap"),
            new GameplayInputBinding(93, "key.echoholomap.minimap_zoom_in", "holomap", "holomap.zoom_in"),
            new GameplayInputBinding(91, "key.echoholomap.minimap_zoom_out", "holomap", "holomap.zoom_out"),
            new GameplayInputBinding(92, "key.echoholomap.minimap_cycle_corner", "holomap", "holomap.cycle_corner"),
            new GameplayInputBinding(78, "key.signalos.open", "terminal", "signalos.terminal")
    );
    private static final Map<Integer, Boolean> GAMEPLAY_KEY_DOWN = new ConcurrentHashMap<>();
    private static volatile Map<String, Object> lastFrame = Map.of();
    private static volatile Supplier<List<String>> rendererClassNames = List::of;
    private static volatile Supplier<ClassLoader> moduleClassLoader =
            NativeLoaderLiveHudRenderBridge.class::getClassLoader;

    private NativeLoaderLiveHudRenderBridge() {
    }

    public static void configure(Supplier<List<String>> hudRendererClassNames, Supplier<ClassLoader> classLoader) {
        rendererClassNames = hudRendererClassNames == null ? List::of : hudRendererClassNames;
        moduleClassLoader = classLoader == null ? NativeLoaderLiveHudRenderBridge.class::getClassLoader : classLoader;
    }

    public static void render(Object graphics, Object deltaTracker) {
        if (graphics == null) {
            return;
        }
        long frame = FRAMES.incrementAndGet();
        List<String> classNames = rendererClassNames().stream()
                .filter(NativeLoaderLiveHudRenderBridge::alwaysOnHudRendererClassName)
                .toList();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("nativeLiveHudRenderBridgeServiceId", SERVICE_ID);
        state.put("frame", frame);
        state.put("rendererCount", classNames.size());
        state.put("rendered", false);
        state.put("gameplayInputPump", pumpGameplayInput(frame));
        state.put("routeDispatch", dispatchAlwaysOnRoutes(graphics, deltaTracker, frame));
        List<Map<String, Object>> rendererStates = new ArrayList<>();
        for (String className : classNames) {
            if (className == null || className.isBlank()) {
                continue;
            }
            Map<String, Object> rendererState = invokeRenderer(className.trim(), graphics, deltaTracker);
            rendererStates.add(rendererState);
            state.put("lastRenderer", className.trim());
            state.put("lastRendererState", rendererState);
            if (Boolean.TRUE.equals(rendererState.get("rendered"))) {
                state.put("rendered", true);
            }
        }
        state.put("rendererStates", List.copyOf(rendererStates));
        lastFrame = Map.copyOf(state);
    }

    public static Map<String, Object> snapshot() {
        return lastFrame;
    }

    private static List<String> rendererClassNames() {
        ArrayList<String> classes = new ArrayList<>(safeRendererClassNames());
        classes.add("com.knoxhack.echo.hudcore.client.EchoHudCoreOverlay");
        classes.add("com.knoxhack.echolens.client.LensHudOverlay");
        classes.add("com.knoxhack.echoindex.client.IndexOverlay");
        classes.add("com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay");
        return classes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static List<String> safeRendererClassNames() {
        try {
            List<String> configured = rendererClassNames.get();
            return configured == null ? List.of() : configured;
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static Map<String, Object> invokeRenderer(String className, Object graphics, Object deltaTracker) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("renderer", className);
        try {
            Method method = RENDERERS.computeIfAbsent(className, NativeLoaderLiveHudRenderBridge::resolveRenderer);
            Object result;
            if (method.getParameterCount() == 2 && method.getParameterTypes()[1] == float.class) {
                result = method.invoke(null, graphics, partialTick(deltaTracker));
            } else {
                result = method.invoke(null, graphics, deltaTracker);
            }
            boolean rendered = false;
            if (result instanceof Map<?, ?> map) {
                state.put("result", map);
                rendered = Boolean.TRUE.equals(map.get("rendered"));
                if (Boolean.TRUE.equals(map.get("nativeHudDataPlumbing"))) {
                    state.put("nativeHudDataPlumbing", true);
                    state.put("nativeHudDataSource", string(map.get("nativeHudDataSource")));
                    state.put("nativeHudDataSnapshot", map.get("nativeHudDataSnapshot"));
                }
            } else {
                state.put("sideEffectRenderer", true);
                rendered = true;
            }
            state.put("rendered", rendered);
        } catch (Throwable exception) {
            Throwable failure = exception instanceof InvocationTargetException invocation
                    && invocation.getCause() != null
                    ? invocation.getCause()
                    : exception;
            state.put("rendered", false);
            state.put("failureKind", failure.getClass().getSimpleName());
            state.put("failureMessage", failure.getMessage() == null ? "" : failure.getMessage());
        }
        return Map.copyOf(state);
    }

    private static Map<String, Object> dispatchAlwaysOnRoutes(Object graphics, Object deltaTracker, long frame) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("source", "native_loader_live_hud_projection");
        state.put("frame", frame);
        state.put("routeTableDriven", true);
        state.put("hud", dispatch("hud", "hud.render", graphics, deltaTracker, frame, "hud_layer"));
        state.put("missionTracker", dispatch("hud_widget", "hud.mission_tracker.render", graphics, deltaTracker, frame, "hud_layer"));
        state.put("hazardReadout", dispatch("hud_widget", "hud.hazard_readout.render", graphics, deltaTracker, frame, "hud_layer"));
        state.put("compassIndicator", dispatch("hud_widget", "hud.compass_indicator.render", graphics, deltaTracker, frame, "hud_layer"));
        state.put("lens", dispatch("client_overlay", "lens.overlay.render", graphics, deltaTracker, frame, "gui_layer"));
        state.put("indexInventory", dispatch("client_overlay", "index.inventory_overlay_render", graphics, deltaTracker, frame, "gui_layer"));
        state.put("holomapMinimap", dispatch("holomap", "holomap.minimap.render", graphics, deltaTracker, frame, "gui_layer"));
        state.put("terminalMissionHud", dispatch("client_overlay", "terminal.mission_hud.render", graphics, deltaTracker, frame, "gui_layer"));
        state.put("terminalDiscoveryToast", dispatch("client_overlay", "terminal.discovery_toast.render", graphics, deltaTracker, frame, "gui_layer"));
        return Map.copyOf(state);
    }

    private static Map<String, Object> dispatch(
            String surfaceType,
            String actionId,
            Object graphics,
            Object deltaTracker,
            long frame,
            String layer
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_loader_live_hud_projection");
        metadata.put("service", SERVICE_ID);
        metadata.put("frame", frame);
        metadata.put("frameSource", layer);
        metadata.put("partialTick", partialTick(deltaTracker));
        metadata.put("graphicsClass", graphics == null ? "" : graphics.getClass().getName());
        metadata.put("deltaTrackerClass", deltaTracker == null ? "" : deltaTracker.getClass().getName());
        EchoNativeLoadStatus status = "hud_layer".equals(layer)
                ? EchoNativeClientRouteRegistries.get().renderHudLayer(surfaceType, actionId, Map.copyOf(metadata))
                : EchoNativeClientRouteRegistries.get().renderGuiLayer(surfaceType, actionId, Map.copyOf(metadata));
        return Map.of(
                "surfaceType", surfaceType,
                "actionId", actionId,
                "status", status.name(),
                "mutated", status == EchoNativeLoadStatus.MUTATED
        );
    }

    private static Map<String, Object> pumpGameplayInput(long frame) {
        Object minecraft = minecraftInstance();
        if (minecraft == null || fieldValue(minecraft, "player") == null || fieldValue(minecraft, "screen") != null) {
            GAMEPLAY_KEY_DOWN.clear();
            return Map.of(
                    "source", "native_loader_live_hud_gameplay_input",
                    "active", false,
                    "reason", "world_or_gameplay_focus_unavailable");
        }
        long window = minecraftWindowHandle(minecraft);
        if (window == 0L) {
            return Map.of(
                    "source", "native_loader_live_hud_gameplay_input",
                    "active", false,
                    "reason", "window_unavailable");
        }
        List<Map<String, Object>> dispatches = new ArrayList<>();
        for (GameplayInputBinding binding : GAMEPLAY_INPUT_BINDINGS) {
            boolean pressed = glfwGetKey(window, binding.keyCode()) == GLFW_PRESS;
            boolean wasPressed = Boolean.TRUE.equals(GAMEPLAY_KEY_DOWN.put(binding.keyCode(), pressed));
            if (!pressed || wasPressed) {
                continue;
            }
            Map<String, Object> metadata = Map.of(
                    "source", "native_loader_live_hud_gameplay_input",
                    "service", SERVICE_ID,
                    "frame", frame,
                    "eventType", "gameplay_key_press",
                    "keyMapping", binding.keyMapping(),
                    "keyCode", binding.keyCode(),
                    "surfaceType", binding.surfaceType(),
                    "actionId", binding.actionId(),
                    "neoForgeEventOwnershipRequired", false);
            EchoNativeLoadStatus status = NativeLoaderClientRouteTable.dispatchStatus(
                    binding.surfaceType(),
                    binding.actionId(),
                    metadata);
            if (status != EchoNativeLoadStatus.MUTATED) {
                status = NativeLoaderClientRouteTable.dispatchInputBindingStatus(
                        binding.keyMapping(),
                        binding.keyCode(),
                        "press",
                        metadata);
            }
            dispatches.add(Map.of(
                    "keyMapping", binding.keyMapping(),
                    "keyCode", binding.keyCode(),
                    "surfaceType", binding.surfaceType(),
                    "actionId", binding.actionId(),
                    "status", status.name(),
                    "mutated", status == EchoNativeLoadStatus.MUTATED));
        }
        return Map.of(
                "source", "native_loader_live_hud_gameplay_input",
                "active", true,
                "dispatchCount", dispatches.size(),
                "dispatches", List.copyOf(dispatches));
    }

    private static Object minecraftInstance() {
        try {
            return Class.forName("net.minecraft.client.Minecraft").getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return null;
        }
    }

    private static Object fieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            Class<?> type = target.getClass();
            while (type != null) {
                try {
                    java.lang.reflect.Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
        return null;
    }

    private static long minecraftWindowHandle(Object minecraft) {
        try {
            Object window = minecraft.getClass().getMethod("getWindow").invoke(minecraft);
            Object handle = window == null ? null : window.getClass().getMethod("getWindow").invoke(window);
            return handle instanceof Number number ? number.longValue() : 0L;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return 0L;
        }
    }

    private static int glfwGetKey(long window, int keyCode) {
        try {
            Object result = Class.forName("org.lwjgl.glfw.GLFW")
                    .getMethod("glfwGetKey", long.class, int.class)
                    .invoke(null, window, keyCode);
            return result instanceof Number number ? number.intValue() : 0;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return 0;
        }
    }

    private static boolean alwaysOnHudRendererClassName(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        String normalized = className.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.contains(".client.hud.")
                || normalized.contains(".hud.")
                || normalized.endsWith(".echohudcoreoverlay")
                || normalized.endsWith(".lenshudoverlay")
                || normalized.endsWith(".indexoverlay")
                || normalized.endsWith(".holomapminimapoverlay");
    }

    private static Method resolveRenderer(String className) {
        try {
            Class<?> type = Class.forName(className, true, moduleClassLoader.get());
            Class<?> graphics = Class.forName("net.minecraft.client.gui.GuiGraphicsExtractor", false, type.getClassLoader());
            try {
                return type.getMethod("render", graphics, Object.class);
            } catch (NoSuchMethodException ignoredObjectSignature) {
                try {
                    return type.getMethod("render", graphics,
                            Class.forName("net.minecraft.client.DeltaTracker", false, type.getClassLoader()));
                } catch (NoSuchMethodException ignoredDeltaTrackerSignature) {
                    return type.getMethod("render", graphics, float.class);
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static float partialTick(Object deltaTracker) {
        if (deltaTracker == null) {
            return 0.0F;
        }
        try {
            Object value = deltaTracker.getClass()
                    .getMethod("getGameTimeDeltaPartialTick", boolean.class)
                    .invoke(deltaTracker, false);
            return value instanceof Number number ? number.floatValue() : 0.0F;
        } catch (ReflectiveOperationException ignored) {
            try {
                Object value = deltaTracker.getClass().getMethod("getRealtimeDeltaTicks").invoke(deltaTracker);
                return value instanceof Number number ? number.floatValue() : 0.0F;
            } catch (ReflectiveOperationException ignoredAgain) {
                return 0.0F;
            }
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record GameplayInputBinding(int keyCode, String keyMapping, String surfaceType, String actionId) {
    }
}
