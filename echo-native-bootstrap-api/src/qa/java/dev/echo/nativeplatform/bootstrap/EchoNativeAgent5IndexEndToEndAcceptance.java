package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5IndexEndToEndAcceptance {
    private EchoNativeAgent5IndexEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> physicalInputAcceptance,
            Map<String, Object> liveSurfaceRenderAcceptance,
            Map<String, Object> focusSmoke,
            Map<String, Object> textEditingSmoke,
            Map<String, Object> hostEventTranscriptSmoke
    ) {
        Map<String, Object> hotkey = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> input = physicalInputAcceptance == null ? Map.of() : physicalInputAcceptance;
        Map<String, Object> render = liveSurfaceRenderAcceptance == null ? Map.of() : liveSurfaceRenderAcceptance;
        Map<String, Object> focus = focusSmoke == null ? Map.of() : focusSmoke;
        Map<String, Object> editing = textEditingSmoke == null ? Map.of() : textEditingSmoke;
        Map<String, Object> transcript = hostEventTranscriptSmoke == null ? Map.of() : hostEventTranscriptSmoke;
        String key = firstNonBlank(hotkey.get("key"), hotkey.get("hotkey"));
        String surface = normalize(hotkey.get("surface"));
        String query = text(editing.get("indexBuffer"));
        List<String> focusLines = strings(focus, "renderedFocusLines");
        List<String> editLines = strings(editing, "renderedLines");
        List<String> transcriptEvents = strings(transcript, "events");
        List<String> transcriptLines = strings(transcript, "renderedLines");
        Map<String, Object> indexMutation = object(transcript.get("indexMutation"));
        boolean runtimeMutationAccepted = runtimeMutationAccepted(
                indexMutation,
                "native.ui.index_search",
                "player.terminal_opened"
        );
        boolean searchExecuted = strings(focus, "activationKeys").contains("indexSearchExecuted")
                && strings(editing, "activationKeys").contains("indexSearchExecuted")
                && transcriptEvents.contains("enter:index:indexSearchExecuted");
        boolean indexRendered = "INDEX".equals(normalize(render.get("surface")))
                && text(render.get("moduleRendererClass")).contains("Index")
                && focusLines.stream().anyMatch(line -> line.contains("index:search ready"))
                && editLines.stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.indexQuery()
                        + " -> ") && line.contains("index result(s):"))
                && transcriptLines.stream().anyMatch(line -> line.contains("index result(s):"));
        boolean accepted = Boolean.TRUE.equals(hotkey.get("observed"))
                && "G".equals(key)
                && "INDEX".equals(surface)
                && Boolean.TRUE.equals(input.get("accepted"))
                && Boolean.TRUE.equals(render.get("accepted"))
                && Boolean.TRUE.equals(focus.get("passed"))
                && Boolean.TRUE.equals(editing.get("passed"))
                && Boolean.TRUE.equals(transcript.get("passed"))
                && EchoNativeAgent5UiExpectedValues.indexQuery().equals(query)
                && transcriptEvents.contains("key:G->INDEX")
                && transcriptEvents.contains("text:index:" + EchoNativeAgent5UiExpectedValues.indexQuery())
                && searchExecuted
                && indexRendered
                && runtimeMutationAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("query", query);
        result.put("physicalInputAccepted", Boolean.TRUE.equals(input.get("accepted")));
        result.put("renderAccepted", Boolean.TRUE.equals(render.get("accepted")));
        result.put("focusAccepted", Boolean.TRUE.equals(focus.get("passed")));
        result.put("editingAccepted", Boolean.TRUE.equals(editing.get("passed")));
        result.put("transcriptAccepted", Boolean.TRUE.equals(transcript.get("passed")));
        result.put("searchExecuted", searchExecuted);
        result.put("indexRendered", indexRendered);
        result.put("runtimeMutationAccepted", runtimeMutationAccepted);
        result.put("runtimeActionId", text(indexMutation.get("runtimeActionId")));
        result.put("eventName", text(indexMutation.get("eventName")));
        result.put("effect", accepted
                ? "index_end_to_end:G->INDEX:" + query
                : "index_end_to_end:rejected:" + (surface.isBlank() ? "none" : surface));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
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

    private static boolean runtimeMutationAccepted(
            Map<String, Object> evidence,
            String runtimeActionId,
            String eventName
    ) {
        Map<String, Object> result = evidence == null ? Map.of() : evidence;
        return Boolean.TRUE.equals(result.get("mutated"))
                && Boolean.TRUE.equals(result.get("saveTouched"))
                && Boolean.TRUE.equals(result.get("missionUpdated"))
                && Boolean.TRUE.equals(result.get("feedbackEmitted"))
                && runtimeActionId.equals(result.get("runtimeActionId"))
                && eventName.equals(result.get("eventName"));
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstNonBlank(Object first, Object second) {
        String firstText = text(first);
        return firstText.isBlank() ? text(second) : firstText;
    }
}
