package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeAgent5ScreenHostModel {
    private EchoNativeAgent5ScreenHostModel() {
    }

    static Map<String, Object> render(
            String surface,
            Map<String, Object> state,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        String normalized = surface == null || surface.isBlank() ? "TERMINAL" : surface.trim().toUpperCase();
        Map<String, Object> safeState = state == null ? Map.of() : Map.copyOf(state);
        List<String> surfaceLines = new ArrayList<>();
        surfaceLines.add(normalized + ": " + primaryLine(normalized, safeState));
        surfaceLines.add(EchoNativeAgent5UiExpectedValues.terminalOutput());
        surfaceLines.add(EchoNativeAgent5UiExpectedValues.indexSearchOutput());
        surfaceLines.add(EchoNativeAgent5UiExpectedValues.lensOutput());
        surfaceLines.add(EchoNativeAgent5UiExpectedValues.holomapMarker());
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("hostModelClass", EchoNativeAgent5ScreenHostModel.class.getSimpleName());
        model.put("surface", normalized);
        model.put("packId", packId == null ? "" : packId);
        model.put("screenTitle", safeState.getOrDefault("title", EchoNativeAgent5UiExpectedValues.terminal().get("title")));
        model.put("headerLines", List.of("ECHO Native", "Modules " + moduleCount));
        model.put("surfaceLines", List.copyOf(surfaceLines));
        model.put("footerLine", "Items " + itemCount + " Missions " + missionCount + " Regions " + regionCount);
        model.put("hudValues", EchoNativeAgent5UiExpectedValues.hud());
        model.put("notificationAnchor", EchoNativeAgent5UiExpectedValues.notificationMessages().get(0));
        model.put("cinematicCue", EchoNativeAgent5UiExpectedValues.terminal().get("title"));
        return Map.copyOf(model);
    }

    private static String primaryLine(String surface, Map<String, Object> state) {
        return switch (surface) {
            case "INDEX" -> EchoNativeAgent5UiExpectedValues.indexSearchOutput();
            case "LENS" -> EchoNativeAgent5UiExpectedValues.lensOutput();
            case "HOLOMAP" -> EchoNativeAgent5UiExpectedValues.holomapMarker();
            case "WIKI" -> EchoNativeAgent5UiExpectedValues.wikiLink();
            case "HUD" -> EchoNativeAgent5UiExpectedValues.hudLineToken();
            default -> String.valueOf(state.getOrDefault("output", EchoNativeAgent5UiExpectedValues.terminalOutput()));
        };
    }
}
