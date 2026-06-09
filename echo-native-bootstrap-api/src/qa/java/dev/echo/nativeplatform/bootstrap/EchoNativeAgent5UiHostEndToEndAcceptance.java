package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5UiHostEndToEndAcceptance {
    private EchoNativeAgent5UiHostEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> physicalInputAcceptance,
            Map<String, Object> liveSurfaceAcceptance,
            Map<String, Object> liveSurfaceRenderAcceptance,
            Map<String, Object> interactionStateAcceptance
    ) {
        Map<String, Object> hotkey = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> input = physicalInputAcceptance == null ? Map.of() : physicalInputAcceptance;
        Map<String, Object> live = liveSurfaceAcceptance == null ? Map.of() : liveSurfaceAcceptance;
        Map<String, Object> render = liveSurfaceRenderAcceptance == null ? Map.of() : liveSurfaceRenderAcceptance;
        Map<String, Object> interaction = interactionStateAcceptance == null ? Map.of() : interactionStateAcceptance;
        String key = firstNonBlank(hotkey.get("key"), hotkey.get("hotkey"));
        String surface = normalize(hotkey.get("surface"));
        String inputSurface = normalize(input.get("surface"));
        String liveSurface = normalize(live.get("currentMode"));
        String renderSurface = normalize(render.get("surface"));
        boolean accepted = Boolean.TRUE.equals(hotkey.get("observed"))
                && Boolean.TRUE.equals(input.get("accepted"))
                && Boolean.TRUE.equals(live.get("accepted"))
                && Boolean.TRUE.equals(render.get("accepted"))
                && Boolean.TRUE.equals(interaction.get("accepted"))
                && !surface.isBlank()
                && surface.equals(inputSurface)
                && surface.equals(liveSurface)
                && surface.equals(renderSurface)
                && Boolean.TRUE.equals(interaction.get("terminalAccepted"))
                && Boolean.TRUE.equals(interaction.get("indexAccepted"))
                && Boolean.TRUE.equals(interaction.get("lensAccepted"))
                && Boolean.TRUE.equals(interaction.get("holomapAccepted"))
                && Boolean.TRUE.equals(interaction.get("wikiAccepted"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("stepCount", interaction.getOrDefault("stepCount", 0));
        result.put("physicalInputAccepted", Boolean.TRUE.equals(input.get("accepted")));
        result.put("liveSurfaceAccepted", Boolean.TRUE.equals(live.get("accepted")));
        result.put("renderAccepted", Boolean.TRUE.equals(render.get("accepted")));
        result.put("interactionStateAccepted", Boolean.TRUE.equals(interaction.get("accepted")));
        result.put("effect", accepted
                ? "ui_host_end_to_end:" + key + "->" + surface + ":" + interaction.getOrDefault("stepCount", 0)
                : "ui_host_end_to_end:rejected:" + (surface.isBlank() ? "none" : surface));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", accepted);
        return Map.copyOf(result);
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
