package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderModuleSurfaceRenderers;
import dev.echo.nativeplatform.loader.NativeLoaderSurfaceRenderer;

import java.util.Map;

public final class EchoNativeAgent5SurfaceRenderer {
    private EchoNativeAgent5SurfaceRenderer() {
    }

    public static Map<String, Object> render(
            String mode,
            Map<String, Object> state,
            Map<String, Object> dataSources
    ) {
        return NativeLoaderSurfaceRenderer.render(mode, state, dataSources, context());
    }

    static NativeLoaderModuleSurfaceRenderers.Context context() {
        return new NativeLoaderModuleSurfaceRenderers.Context(
                EchoNativeAgent5UiHandlerRegistry::openHolomap,
                EchoNativeAgent5UiHandlerRegistry::openWiki,
                EchoNativeBootstrapMain::nativeProductNamespace
        );
    }
}
