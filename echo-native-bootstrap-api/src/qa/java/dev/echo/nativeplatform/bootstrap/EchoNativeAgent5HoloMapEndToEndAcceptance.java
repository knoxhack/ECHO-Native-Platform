package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5HoloMapEndToEndAcceptance {
    private static final String SURFACE_OPEN_ACTION_ID = "native.ui.surface_open";
    private static final String HOLOMAP_CANONICAL_ID = "echoholomap:ashfall_map";

    private EchoNativeAgent5HoloMapEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> physicalInputAcceptance,
            Map<String, Object> liveSurfaceRenderAcceptance,
            Map<String, Object> interactionSmoke
    ) {
        Map<String, Object> hotkey = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> input = physicalInputAcceptance == null ? Map.of() : physicalInputAcceptance;
        Map<String, Object> render = liveSurfaceRenderAcceptance == null ? Map.of() : liveSurfaceRenderAcceptance;
        Map<String, Object> interaction = interactionSmoke == null ? Map.of() : interactionSmoke;
        Map<String, Object> surfaceOpenMutation = object(interaction.get("surfaceOpenMutation"));
        String key = text(hotkey.get("key"));
        String surface = normalize(hotkey.get("surface"));
        Map<String, Object> step = step(interaction, "holomap_open");
        Map<String, Object> snapshot = object(step.get("snapshot"));
        List<String> lines = strings(snapshot, "surfaceLines");
        boolean runtimeMutationAccepted = runtimeSurfaceOpenAccepted(surfaceOpenMutation, HOLOMAP_CANONICAL_ID);
        boolean interactionAccepted = Boolean.TRUE.equals(interaction.get("passed"))
                && Boolean.TRUE.equals(step.get("passed"))
                && "HOLOMAP".equals(step.get("surface"))
                && "holomap:surface".equals(step.get("focusPath"))
                && text(step.get("moduleRendererClass")).contains("Holomap")
                && lines.stream().anyMatch(line -> line.contains(
                EchoNativeAgent5UiExpectedValues.text(EchoNativeAgent5UiExpectedValues.holomap().get("layer"))))
                && lines.stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.holomapMarker()));
        boolean holomapRendered = "HOLOMAP".equals(normalize(render.get("surface")))
                && text(render.get("moduleRendererClass")).contains("Holomap");
        boolean accepted = Boolean.TRUE.equals(hotkey.get("observed"))
                && "J".equals(key)
                && "HOLOMAP".equals(surface)
                && Boolean.TRUE.equals(input.get("accepted"))
                && Boolean.TRUE.equals(render.get("accepted"))
                && holomapRendered
                && interactionAccepted
                && runtimeMutationAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("layer", EchoNativeAgent5UiExpectedValues.holomap().get("layer"));
        result.put("marker", EchoNativeAgent5UiExpectedValues.holomapMarker());
        result.put("physicalInputAccepted", Boolean.TRUE.equals(input.get("accepted")));
        result.put("renderAccepted", Boolean.TRUE.equals(render.get("accepted")));
        result.put("interactionAccepted", interactionAccepted);
        result.put("holomapRendered", holomapRendered);
        result.put("runtimeMutationAccepted", runtimeMutationAccepted);
        result.put("runtimeActionId", text(surfaceOpenMutation.get("runtimeActionId")));
        result.put("eventName", text(surfaceOpenMutation.get("eventName")));
        result.put("surfaceCanonicalId", HOLOMAP_CANONICAL_ID);
        result.put("effect", accepted
                ? "holomap_end_to_end:J->HOLOMAP:" + EchoNativeAgent5UiExpectedValues.holomapMarker()
                : "holomap_end_to_end:rejected:" + (surface.isBlank() ? "none" : surface));
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

    private static String normalize(Object value) {
        return text(value).trim().toUpperCase(Locale.ROOT);
    }

    private static boolean runtimeSurfaceOpenAccepted(Map<String, Object> evidence, String canonicalId) {
        Map<String, Object> snapshot = object(evidence.get("resultSnapshot"));
        Object target = snapshot.get("target");
        if (target == null) {
            target = snapshot.get("canonicalId");
        }
        if (target == null) {
            target = object(snapshot.get("details")).get("canonicalId");
        }
        return Boolean.TRUE.equals(evidence.get("mutated"))
                && Boolean.TRUE.equals(evidence.get("saveTouched"))
                && Boolean.TRUE.equals(evidence.get("missionUpdated"))
                && Boolean.TRUE.equals(evidence.get("feedbackEmitted"))
                && SURFACE_OPEN_ACTION_ID.equals(evidence.get("runtimeActionId"))
                && SURFACE_OPEN_ACTION_ID.equals(evidence.get("eventName"))
                && canonicalId.equals(String.valueOf(target));
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
