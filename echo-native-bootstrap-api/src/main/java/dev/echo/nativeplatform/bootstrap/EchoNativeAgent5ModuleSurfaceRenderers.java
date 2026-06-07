package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderModuleSurfaceRenderers;

import java.util.Map;

public final class EchoNativeAgent5ModuleSurfaceRenderers {
    private EchoNativeAgent5ModuleSurfaceRenderers() {
    }

    public static Map<String, Object> renderTerminal(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderTerminal(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderIndex(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderIndex(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderLens(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderLens(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderHolomap(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderHolomap(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderWiki(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderWiki(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderSignalos(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderSignalos(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderProductActionSurface(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderProductActionSurface(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderMachine(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderMachine(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderMissionLog(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderMissionLog(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderSettings(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderSettings(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderPause(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderPause(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderRecovery(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderRecovery(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderMainMenu(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderMainMenu(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    public static Map<String, Object> renderHud(Map<String, Object> state, Map<String, Object> dataSources) {
        return NativeLoaderModuleSurfaceRenderers.renderHud(state, dataSources, EchoNativeAgent5SurfaceRenderer.context());
    }

    static final class EchoNativeTerminalSurfaceRenderer {
        private EchoNativeTerminalSurfaceRenderer() {
        }
    }

    static final class EchoNativeIndexSurfaceRenderer {
        private EchoNativeIndexSurfaceRenderer() {
        }
    }

    static final class EchoNativeLensSurfaceRenderer {
        private EchoNativeLensSurfaceRenderer() {
        }
    }

    static final class EchoNativeHolomapSurfaceRenderer {
        private EchoNativeHolomapSurfaceRenderer() {
        }
    }

    static final class EchoNativeWikiSurfaceRenderer {
        private EchoNativeWikiSurfaceRenderer() {
        }
    }

    static final class EchoNativeSignalOsSurfaceRenderer {
        private EchoNativeSignalOsSurfaceRenderer() {
        }
    }

    static final class EchoNativeProductActionSurfaceRenderer {
        private EchoNativeProductActionSurfaceRenderer() {
        }
    }

    static final class EchoNativeMachineSurfaceRenderer {
        private EchoNativeMachineSurfaceRenderer() {
        }
    }

    static final class EchoNativeMissionLogSurfaceRenderer {
        private EchoNativeMissionLogSurfaceRenderer() {
        }
    }

    static final class EchoNativeSettingsSurfaceRenderer {
        private EchoNativeSettingsSurfaceRenderer() {
        }
    }

    static final class EchoNativePauseSurfaceRenderer {
        private EchoNativePauseSurfaceRenderer() {
        }
    }

    static final class EchoNativeRecoverySurfaceRenderer {
        private EchoNativeRecoverySurfaceRenderer() {
        }
    }

    static final class EchoNativeMainMenuSurfaceRenderer {
        private EchoNativeMainMenuSurfaceRenderer() {
        }
    }

    static final class EchoNativeHudSurfaceRenderer {
        private EchoNativeHudSurfaceRenderer() {
        }
    }
}
