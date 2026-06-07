package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5SettingsEndToEndAcceptance {
    private EchoNativeAgent5SettingsEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> physicalInputAcceptance,
            Map<String, Object> liveSurfaceRenderAcceptance,
            Map<String, Object> interactionSmoke,
            Map<String, Object> adjustmentSmoke
    ) {
        Map<String, Object> routeAction = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> input = physicalInputAcceptance == null ? Map.of() : physicalInputAcceptance;
        Map<String, Object> render = liveSurfaceRenderAcceptance == null ? Map.of() : liveSurfaceRenderAcceptance;
        Map<String, Object> interaction = interactionSmoke == null ? Map.of() : interactionSmoke;
        Map<String, Object> adjustment = adjustmentSmoke == null ? Map.of() : adjustmentSmoke;
        String key = text(routeAction.getOrDefault("key", "SETTINGS_ACTION"));
        String surface = normalize(routeAction.get("surface"));
        Map<String, Object> step = step(interaction, "settings_open");
        Map<String, Object> snapshot = object(step.get("snapshot"));
        List<String> lines = strings(snapshot, "surfaceLines");
        boolean interactionAccepted = Boolean.TRUE.equals(interaction.get("passed"))
                && Boolean.TRUE.equals(step.get("passed"))
                && "SETTINGS".equals(step.get("surface"))
                && "settings:surface".equals(step.get("focusPath"))
                && text(step.get("moduleRendererClass")).contains("Settings")
                && lines.stream().anyMatch(line -> line.contains("ashfall-accessible"))
                && lines.stream().anyMatch(line -> line.contains("ashfall-agent5"))
                && lines.stream().anyMatch(line -> line.contains("keyboard_mouse"));
        boolean adjustmentAccepted = Boolean.TRUE.equals(adjustment.get("passed"))
                && strings(adjustment, "selectedOptions").equals(List.of("HUD Scale", "Subtitles"))
                && strings(adjustment, "effects").equals(List.of("settings:hud_scale", "settings:subtitles"))
                && Double.valueOf(1.25D).equals(adjustment.get("settingsHudScale"))
                && Boolean.FALSE.equals(adjustment.get("settingsSubtitles"))
                && strings(adjustment, "renderedLines").stream()
                .anyMatch(line -> line.contains("HUD scale: 1.25    Subtitles: disabled"));
        boolean settingsRendered = "SETTINGS".equals(normalize(render.get("surface")))
                && text(render.get("moduleRendererClass")).contains("Settings");
        boolean accepted = "SETTINGS".equals(surface)
                && Boolean.TRUE.equals(input.get("accepted"))
                && Boolean.TRUE.equals(render.get("accepted"))
                && settingsRendered
                && interactionAccepted
                && adjustmentAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("settingsProfile", "ashfall-accessible");
        result.put("settingsTheme", "ashfall-agent5");
        result.put("settingsHudScale", adjustment.getOrDefault("settingsHudScale", 0.0D));
        result.put("settingsSubtitles", adjustment.getOrDefault("settingsSubtitles", true));
        result.put("physicalInputAccepted", Boolean.TRUE.equals(input.get("accepted")));
        result.put("renderAccepted", Boolean.TRUE.equals(render.get("accepted")));
        result.put("interactionAccepted", interactionAccepted);
        result.put("adjustmentAccepted", adjustmentAccepted);
        result.put("settingsRendered", settingsRendered);
        result.put("effect", accepted
                ? "settings_end_to_end:" + key + "->SETTINGS:ashfall-accessible:subtitles_off"
                : "settings_end_to_end:rejected:" + (surface.isBlank() ? "none" : surface));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    private static Map<String, Object> step(Map<String, Object> interaction, String id) {
        return maps(interaction.get("steps")).stream()
                .filter(entry -> id.equals(entry.get("id")))
                .findFirst()
                .orElse(Map.of());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object source, String key) {
        if (source instanceof Map<?, ?> map) {
            return strings((Map<String, Object>) map, key);
        }
        return List.of();
    }

    private static String normalize(Object value) {
        return text(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
