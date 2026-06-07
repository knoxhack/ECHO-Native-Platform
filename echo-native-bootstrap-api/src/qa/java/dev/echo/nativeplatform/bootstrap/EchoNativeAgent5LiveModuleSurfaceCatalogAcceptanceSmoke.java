package dev.echo.nativeplatform.bootstrap;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveModuleSurfaceCatalogAcceptanceSmoke {
    private EchoNativeAgent5LiveModuleSurfaceCatalogAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        List<Map<String, Object>> surfaces = List.of(
                EchoNativeAgent5ModuleSurfaceRenderers.renderTerminal(
                        EchoNativeAgent5UiExpectedValues.terminalState(), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderIndex(
                        EchoNativeAgent5UiExpectedValues.indexState(), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderLens(
                        EchoNativeAgent5UiExpectedValues.lensState(), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderHolomap(Map.of(), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderWiki(Map.of(), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderMissionLog(Map.of(), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderSettings(Map.of(), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderPause(Map.of("previousMode", "WIKI"), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderRecovery(
                        EchoNativeAgent5UiExpectedValues.recoveryState(), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderMainMenu(Map.of(), dataSources),
                EchoNativeAgent5ModuleSurfaceRenderers.renderHud(Map.of(), dataSources)
        );
        Map<String, Object> accepted = EchoNativeAgent5LiveModuleSurfaceCatalogAcceptance.assess(surfaces);
        Map<String, Object> rejectedMissingHud = EchoNativeAgent5LiveModuleSurfaceCatalogAcceptance.assess(
                surfaces.subList(0, 10)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_module_surface_catalog:accepted:11-surfaces".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedMissingHud.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveModuleSurfaceCatalogAcceptanceSmokeClass",
                EchoNativeAgent5LiveModuleSurfaceCatalogAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedMissingHud", rejectedMissingHud);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }
}
