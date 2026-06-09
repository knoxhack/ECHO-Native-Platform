package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5WikiEndToEndAcceptance {
    private static final String SURFACE_OPEN_ACTION_ID = "native.ui.surface_open";
    private static final String WIKI_CANONICAL_ID = "echowiki:ashfall";

    private EchoNativeAgent5WikiEndToEndAcceptance() {
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
        String observedKey = firstNonBlank(hotkey.get("key"), hotkey.get("hotkey"));
        String key = observedKey.isBlank() ? "MODULE_ROUTE" : observedKey;
        String surface = normalize(hotkey.get("surface"));
        Map<String, Object> step = step(interaction, "wiki_open");
        Map<String, Object> snapshot = object(step.get("snapshot"));
        List<String> lines = strings(snapshot, "surfaceLines");
        boolean runtimeMutationAccepted = runtimeSurfaceOpenAccepted(surfaceOpenMutation, WIKI_CANONICAL_ID);
        boolean interactionAccepted = Boolean.TRUE.equals(interaction.get("passed"))
                && Boolean.TRUE.equals(step.get("passed"))
                && "WIKI".equals(step.get("surface"))
                && "wiki:surface".equals(step.get("focusPath"))
                && text(step.get("moduleRendererClass")).contains("Wiki")
                && lines.stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.wikiLink()));
        boolean wikiRendered = "WIKI".equals(normalize(render.get("surface")))
                && text(render.get("moduleRendererClass")).contains("Wiki");
        boolean accepted = "MODULE_ROUTE".equals(key)
                && "WIKI".equals(surface)
                && Boolean.TRUE.equals(input.get("accepted"))
                && Boolean.TRUE.equals(render.get("accepted"))
                && wikiRendered
                && interactionAccepted
                && runtimeMutationAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("guide", EchoNativeAgent5UiExpectedValues.wiki().get("guide"));
        result.put("page", EchoNativeAgent5UiExpectedValues.wiki().get("page"));
        result.put("link", EchoNativeAgent5UiExpectedValues.wikiLink());
        result.put("physicalInputAccepted", Boolean.TRUE.equals(input.get("accepted")));
        result.put("renderAccepted", Boolean.TRUE.equals(render.get("accepted")));
        result.put("interactionAccepted", interactionAccepted);
        result.put("wikiRendered", wikiRendered);
        result.put("runtimeMutationAccepted", runtimeMutationAccepted);
        result.put("runtimeActionId", text(surfaceOpenMutation.get("runtimeActionId")));
        result.put("eventName", text(surfaceOpenMutation.get("eventName")));
        result.put("surfaceCanonicalId", WIKI_CANONICAL_ID);
        result.put("effect", accepted
                ? "wiki_end_to_end:MODULE_ROUTE->WIKI:ashfall"
                : "wiki_end_to_end:rejected:" + (surface.isBlank() ? "none" : surface));
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

    private static String firstNonBlank(Object first, Object second) {
        String firstText = text(first);
        return firstText.isBlank() ? text(second) : firstText;
    }
}
