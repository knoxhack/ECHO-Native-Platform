package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5SettingsAdjustmentSmoke {
    private EchoNativeAgent5SettingsAdjustmentSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        Map<String, Object> settings = object(dataSources.get("settings"));
        Map<String, Object> hudScaleNavigation = EchoNativeAgent5UiActionRouter.routeListNavigation("DOWN", "SETTINGS", 2);
        Map<String, Object> hudScaleAdjustment = EchoNativeAgent5UiActionRouter.routeSettingsAdjustment(
                String.valueOf(hudScaleNavigation.get("selectedOption")),
                doubleValue(settings.get("hudScale")),
                Boolean.TRUE.equals(settings.get("subtitles"))
        );
        Map<String, Object> subtitlesNavigation = EchoNativeAgent5UiActionRouter.routeListNavigation(
                "DOWN",
                "SETTINGS",
                integer(hudScaleNavigation.get("selectedIndex"))
        );
        Map<String, Object> subtitlesAdjustment = EchoNativeAgent5UiActionRouter.routeSettingsAdjustment(
                String.valueOf(subtitlesNavigation.get("selectedOption")),
                doubleValue(hudScaleAdjustment.get("settingsHudScale")),
                Boolean.TRUE.equals(hudScaleAdjustment.get("settingsSubtitles"))
        );
        Map<String, Object> rendered = EchoNativeAgent5SurfaceRenderer.render("SETTINGS", Map.of(
                "selectedOption", subtitlesNavigation.get("selectedOption"),
                "settingsHudScale", subtitlesAdjustment.get("settingsHudScale"),
                "settingsSubtitles", subtitlesAdjustment.get("settingsSubtitles")
        ), dataSources);

        boolean passed = Boolean.TRUE.equals(hudScaleAdjustment.get("handled"))
                && Boolean.TRUE.equals(subtitlesAdjustment.get("handled"))
                && "HUD Scale".equals(hudScaleNavigation.get("selectedOption"))
                && "Subtitles".equals(subtitlesNavigation.get("selectedOption"))
                && Double.valueOf(1.25D).equals(hudScaleAdjustment.get("settingsHudScale"))
                && Boolean.FALSE.equals(subtitlesAdjustment.get("settingsSubtitles"))
                && "settings:hud_scale".equals(hudScaleAdjustment.get("effect"))
                && "settings:subtitles".equals(subtitlesAdjustment.get("effect"))
                && lines(rendered).stream().anyMatch(line -> line.contains("Selected: Subtitles"))
                && lines(rendered).stream().anyMatch(line -> line.contains("HUD scale: 1.25    Subtitles: disabled"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("settingsAdjustmentSmokeClass", EchoNativeAgent5SettingsAdjustmentSmoke.class.getSimpleName());
        smoke.put("serviceCodeExecuted", true);
        smoke.put("adapterCoreBridge", true);
        smoke.put("selectedOptions", List.of(hudScaleNavigation.get("selectedOption"), subtitlesNavigation.get("selectedOption")));
        smoke.put("effects", List.of(hudScaleAdjustment.get("effect"), subtitlesAdjustment.get("effect")));
        smoke.put("settingsHudScale", subtitlesAdjustment.get("settingsHudScale"));
        smoke.put("settingsSubtitles", subtitlesAdjustment.get("settingsSubtitles"));
        smoke.put("renderedLines", lines(rendered));
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> rendered) {
        Object value = rendered.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 1.0D;
    }
}
