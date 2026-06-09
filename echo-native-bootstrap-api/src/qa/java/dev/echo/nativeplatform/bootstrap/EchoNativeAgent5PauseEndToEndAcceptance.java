package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5PauseEndToEndAcceptance {
    private EchoNativeAgent5PauseEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> physicalInputAcceptance,
            Map<String, Object> liveSurfaceRenderAcceptance,
            Map<String, Object> interactionSmoke,
            Map<String, Object> optionSmoke
    ) {
        Map<String, Object> hotkey = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> input = physicalInputAcceptance == null ? Map.of() : physicalInputAcceptance;
        Map<String, Object> render = liveSurfaceRenderAcceptance == null ? Map.of() : liveSurfaceRenderAcceptance;
        Map<String, Object> interaction = interactionSmoke == null ? Map.of() : interactionSmoke;
        Map<String, Object> option = optionSmoke == null ? Map.of() : optionSmoke;
        String key = firstNonBlank(hotkey.get("key"), hotkey.get("hotkey"));
        String surface = normalize(hotkey.get("surface"));
        String routeType = normalize(hotkey.get("routeType"));
        Map<String, Object> step = step(interaction, "pause_resume");
        Map<String, Object> snapshot = object(step.get("snapshot"));
        List<String> lines = strings(snapshot, "surfaceLines");
        boolean interactionAccepted = Boolean.TRUE.equals(interaction.get("passed"))
                && Boolean.TRUE.equals(step.get("passed"))
                && "PAUSE".equals(step.get("surface"))
                && "pause:resume:LENS".equals(step.get("focusPath"))
                && "LENS".equals(step.get("resumeDestinationMode"))
                && text(step.get("moduleRendererClass")).contains("Pause")
                && lines.stream().anyMatch(line -> line.contains("Pause: previous screen LENS"))
                && lines.stream().anyMatch(line -> line.contains("Press Esc to resume"));
        boolean optionAccepted = Boolean.TRUE.equals(option.get("passed"))
                && strings(option, "selectedOptions").equals(List.of("Resume", "Settings", "Quit to Main Menu"))
                && strings(option, "destinations").equals(List.of("LENS", "SETTINGS", "MAIN_MENU"))
                && strings(option, "effects").equals(List.of("pause:resume", "pause:settings", "pause:main_menu"))
                && strings(option, "renderedLines").stream().anyMatch(line -> line.contains("Selected: Settings"));
        boolean pauseRendered = "PAUSE".equals(normalize(render.get("surface")))
                && text(render.get("moduleRendererClass")).contains("Pause");
        boolean screenRouteHandled = Boolean.TRUE.equals(hotkey.get("handled"))
                && "ESCAPE".equals(key)
                && "PAUSE".equals(surface)
                && "SCREEN".equals(routeType)
                && Boolean.TRUE.equals(hotkey.get("screenRouteHandled"))
                && "pause:screen_escape".equals(hotkey.get("effect"));
        boolean screenInputAccepted = Boolean.TRUE.equals(input.get("accepted"))
                && "ESCAPE".equals(input.get("key"))
                && "PAUSE".equals(normalize(input.get("surface")))
                && "SCREEN".equals(normalize(input.get("routeType")))
                && Boolean.TRUE.equals(input.get("screenRouteHandled"));
        boolean accepted = screenRouteHandled
                && screenInputAccepted
                && Boolean.TRUE.equals(render.get("accepted"))
                && pauseRendered
                && interactionAccepted
                && optionAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("routeType", routeType.toLowerCase(Locale.ROOT));
        result.put("resumeDestinationMode", step.getOrDefault("resumeDestinationMode", ""));
        result.put("screenRouteHandled", screenRouteHandled);
        result.put("inputAccepted", screenInputAccepted);
        result.put("screenInputAccepted", screenInputAccepted);
        result.put("physicalInputAccepted", false);
        result.put("renderAccepted", Boolean.TRUE.equals(render.get("accepted")));
        result.put("interactionAccepted", interactionAccepted);
        result.put("optionAccepted", optionAccepted);
        result.put("pauseRendered", pauseRendered);
        result.put("effect", accepted
                ? "pause_end_to_end:screen_escape->PAUSE:LENS"
                : "pause_end_to_end:rejected:" + (surface.isBlank() ? "none" : surface));
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

    private static String firstNonBlank(Object first, Object second) {
        String firstText = text(first);
        return firstText.isBlank() ? text(second) : firstText;
    }
}
