package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveModuleSurfaceCatalogAcceptance {
    private EchoNativeAgent5LiveModuleSurfaceCatalogAcceptance() {
    }

    public static Map<String, Object> assess(List<Map<String, Object>> surfaces) {
        List<Map<String, Object>> catalog = surfaces == null ? List.of() : surfaces;
        boolean terminal = accepts(catalog, "echoterminal", "EchoNativeTerminalSurfaceRenderer",
                "terminal:input", EchoNativeAgent5UiExpectedValues.terminalOutput());
        boolean index = accepts(catalog, "echoindex", "EchoNativeIndexSurfaceRenderer",
                "index:search", EchoNativeAgent5UiExpectedValues.indexSearchOutput());
        boolean lens = accepts(catalog, "echolens", "EchoNativeLensSurfaceRenderer",
                "lens:scan", EchoNativeAgent5UiExpectedValues.lensOutput());
        boolean holomap = accepts(catalog, "echoholomap", "EchoNativeHolomapSurfaceRenderer",
                "holomap:surface", EchoNativeAgent5UiExpectedValues.holomapMarker());
        boolean wiki = accepts(catalog, "echowiki", "EchoNativeWikiSurfaceRenderer",
                "wiki:surface", EchoNativeAgent5UiExpectedValues.wikiLink());
        boolean mission = accepts(catalog, "echoscreencore", "EchoNativeMissionLogSurfaceRenderer",
                "mission_log:surface", EchoNativeAgent5UiExpectedValues.missionObjective());
        boolean settings = accepts(catalog, "echothemecore", "EchoNativeSettingsSurfaceRenderer", "settings:surface", "Theme: ashfall-agent5");
        boolean pause = accepts(catalog, "echoscreencore", "EchoNativePauseSurfaceRenderer", "pause:resume:WIKI", "Pause: previous screen WIKI");
        boolean recovery = accepts(catalog, "echoscreencore", "EchoNativeRecoverySurfaceRenderer", "recovery:recover", "Status: RECOVERED");
        boolean mainMenu = accepts(catalog, "echoscreencore", "EchoNativeMainMenuSurfaceRenderer",
                "main_menu:surface", "Main Menu: ECHO Ashfall Terminal boot routes");
        boolean hud = accepts(catalog, "echohudcore", "EchoNativeHudSurfaceRenderer", "echohudcore:hud",
                "HUD: Health " + EchoNativeAgent5UiExpectedValues.hud().get("health"));
        boolean accepted = terminal && index && lens && holomap && wiki && mission && settings && pause && recovery && mainMenu && hud;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("surfaceCount", catalog.size());
        result.put("terminalAccepted", terminal);
        result.put("indexAccepted", index);
        result.put("lensAccepted", lens);
        result.put("holomapAccepted", holomap);
        result.put("wikiAccepted", wiki);
        result.put("missionAccepted", mission);
        result.put("settingsAccepted", settings);
        result.put("pauseAccepted", pause);
        result.put("recoveryAccepted", recovery);
        result.put("mainMenuAccepted", mainMenu);
        result.put("hudAccepted", hud);
        result.put("effect", accepted
                ? "live_module_surface_catalog:accepted:11-surfaces"
                : "live_module_surface_catalog:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    private static boolean accepts(
            List<Map<String, Object>> catalog,
            String moduleId,
            String rendererClass,
            String focusPath,
            String lineToken
    ) {
        return catalog.stream().anyMatch(surface -> moduleId.equals(surface.get("moduleId"))
                && rendererClass.equals(surface.get("moduleRendererClass"))
                && focusPath.equals(surface.get("focusPath"))
                && Boolean.TRUE.equals(surface.get("serviceCodeExecuted"))
                && lines(surface).stream().anyMatch(line -> line.contains(lineToken)));
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> surface) {
        Object value = surface.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
