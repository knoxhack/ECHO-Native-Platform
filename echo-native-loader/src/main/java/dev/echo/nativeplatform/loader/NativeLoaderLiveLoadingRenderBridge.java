package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeLoaderLiveLoadingRenderBridge {
    public static final String SERVICE_ID = "echo.native.live_loading_render_bridge";
    private static final Map<String, Method> RENDERERS = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Method>> PROGRESS_RENDERERS = new ConcurrentHashMap<>();
    private static volatile Map<String, Object> lastFrame = Map.of();
    private static volatile Supplier<List<String>> rendererClassNames = List::of;
    private static volatile Supplier<ClassLoader> moduleClassLoader =
            NativeLoaderLiveLoadingRenderBridge.class::getClassLoader;

    private NativeLoaderLiveLoadingRenderBridge() {
    }

    public static void configure(Supplier<List<String>> loadingRendererClassNames, Supplier<ClassLoader> classLoader) {
        rendererClassNames = loadingRendererClassNames == null ? List::of : loadingRendererClassNames;
        moduleClassLoader = classLoader == null ? NativeLoaderLiveLoadingRenderBridge.class::getClassLoader : classLoader;
    }

    public static void render(Object graphics, float partialTick, int ticks) {
        render(graphics, partialTick, ticks, -1.0F, "");
    }

    public static void render(Object graphics, float partialTick, int ticks, float progress, String phase) {
        if (graphics == null) {
            return;
        }
        List<String> classNames = safeRendererClassNames();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("nativeLiveLoadingRenderBridgeServiceId", SERVICE_ID);
        state.put("tick", ticks);
        state.put("progress", clampProgress(progress));
        state.put("progressAvailable", progress >= 0.0F);
        state.put("phase", phase == null ? "" : phase.trim());
        state.put("rendererCount", classNames.size());
        state.put("rendered", false);
        state.put("routeDispatch", dispatchLoadingRoutes(partialTick, ticks, progress, phase));
        for (String className : classNames) {
            if (className == null || className.isBlank()) {
                continue;
            }
            Map<String, Object> rendererState = invokeRenderer(className.trim(), graphics, partialTick, ticks, progress, phase);
            state.put("lastRenderer", className.trim());
            state.put("lastRendererState", rendererState);
            if (Boolean.TRUE.equals(rendererState.get("rendered"))) {
                state.put("rendered", true);
            }
        }
        lastFrame = Map.copyOf(state);
    }

    public static Map<String, Object> snapshot() {
        return lastFrame;
    }

    private static Map<String, Object> invokeRenderer(
            String className,
            Object graphics,
            float partialTick,
            int ticks,
            float progress,
            String phase
    ) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("renderer", className);
        state.put("progress", clampProgress(progress));
        state.put("progressAvailable", progress >= 0.0F);
        state.put("phase", phase == null ? "" : phase.trim());
        try {
            Optional<Method> progressMethod = PROGRESS_RENDERERS.computeIfAbsent(
                    className, NativeLoaderLiveLoadingRenderBridge::resolveProgressRenderer);
            Method method = progressMethod.orElseGet(() ->
                    RENDERERS.computeIfAbsent(className, NativeLoaderLiveLoadingRenderBridge::resolveRenderer));
            Object result = progressMethod.isPresent()
                    ? method.invoke(null, graphics, partialTick, ticks, progress, phase)
                    : method.invoke(null, graphics, partialTick, ticks);
            boolean rendered = true;
            if (result instanceof Map<?, ?> map) {
                state.put("result", map);
                rendered = Boolean.TRUE.equals(map.get("rendered"));
            }
            state.put("rendered", rendered);
        } catch (Throwable exception) {
            state.put("rendered", false);
            state.put("failureKind", exception.getClass().getSimpleName());
            state.put("failureMessage", exception.getMessage() == null ? "" : exception.getMessage());
        }
        return Map.copyOf(state);
    }

    private static Map<String, Object> dispatchLoadingRoutes(float partialTick, int ticks, float progress, String phase) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("source", "native_loader_live_loading_render_bridge");
        state.put("service", SERVICE_ID);
        state.put("tick", ticks);
        state.put("progress", clampProgress(progress));
        state.put("progressAvailable", progress >= 0.0F);
        state.put("phase", phase == null ? "" : phase.trim());
        state.put("loadingRender", dispatchLoadingRoute("loading.render", partialTick, ticks, progress, phase));
        if (progress >= 0.0F) {
            state.put("loadingProgress", dispatchLoadingRoute("loading.progress", partialTick, ticks, progress, phase));
        }
        if ("complete".equalsIgnoreCase(phase == null ? "" : phase.trim())) {
            state.put("loadingComplete", EchoNativeClientRouteRegistries.get().dispatchStatus(
                    "loading_screen",
                    "loading.complete",
                    loadingMetadata(partialTick, ticks, progress, phase)));
        }
        return Map.copyOf(state);
    }

    private static Map<String, Object> dispatchLoadingRoute(
            String actionId,
            float partialTick,
            int ticks,
            float progress,
            String phase
    ) {
        EchoNativeLoadStatus status = EchoNativeClientRouteRegistries.get().renderGuiLayer(
                "loading_screen",
                actionId,
                loadingMetadata(partialTick, ticks, progress, phase));
        return Map.of(
                "surfaceType", "loading_screen",
                "actionId", actionId,
                "status", status.name(),
                "mutated", status == EchoNativeLoadStatus.MUTATED
        );
    }

    private static Map<String, Object> loadingMetadata(float partialTick, int ticks, float progress, String phase) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_loader_live_loading_render_bridge");
        metadata.put("service", SERVICE_ID);
        metadata.put("eventType", "loading_screen_render");
        metadata.put("partialTick", partialTick);
        metadata.put("tick", ticks);
        metadata.put("progress", clampProgress(progress));
        metadata.put("progressAvailable", progress >= 0.0F);
        metadata.put("phase", phase == null ? "" : phase.trim());
        return Map.copyOf(metadata);
    }

    private static Optional<Method> resolveProgressRenderer(String className) {
        try {
            Class<?> type = Class.forName(className, true, moduleClassLoader.get());
            for (Method method : type.getMethods()) {
                if (method.getName().equals("render")
                        && method.getParameterCount() == 5
                        && method.getParameterTypes()[1] == float.class
                        && method.getParameterTypes()[2] == int.class
                        && method.getParameterTypes()[3] == float.class
                        && method.getParameterTypes()[4] == String.class) {
                    return Optional.of(method);
                }
            }
            return Optional.empty();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Method resolveRenderer(String className) {
        try {
            Class<?> type = Class.forName(className, true, moduleClassLoader.get());
            for (Method method : type.getMethods()) {
                if (method.getName().equals("render")
                        && method.getParameterCount() == 3
                        && method.getParameterTypes()[1] == float.class
                        && method.getParameterTypes()[2] == int.class) {
                    return method;
                }
            }
            throw new NoSuchMethodException(className + ".render(graphics, partialTick, ticks)");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static float clampProgress(float progress) {
        if (progress < 0.0F) {
            return -1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    private static List<String> safeRendererClassNames() {
        try {
            List<String> configured = rendererClassNames.get();
            return configured == null ? List.of() : configured;
        } catch (Throwable ignored) {
            return List.of();
        }
    }
}
