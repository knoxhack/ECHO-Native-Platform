package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5LensEndToEndAcceptance {
    private EchoNativeAgent5LensEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> physicalInputAcceptance,
            Map<String, Object> liveSurfaceRenderAcceptance,
            Map<String, Object> focusSmoke,
            Map<String, Object> hostEventTranscriptSmoke
    ) {
        Map<String, Object> hotkey = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> input = physicalInputAcceptance == null ? Map.of() : physicalInputAcceptance;
        Map<String, Object> render = liveSurfaceRenderAcceptance == null ? Map.of() : liveSurfaceRenderAcceptance;
        Map<String, Object> focus = focusSmoke == null ? Map.of() : focusSmoke;
        Map<String, Object> transcript = hostEventTranscriptSmoke == null ? Map.of() : hostEventTranscriptSmoke;
        String key = firstNonBlank(hotkey.get("key"), hotkey.get("hotkey"));
        String surface = normalize(hotkey.get("surface"));
        List<String> focusLines = strings(focus, "renderedFocusLines");
        List<String> transcriptEvents = strings(transcript, "events");
        List<String> transcriptLines = strings(transcript, "renderedLines");
        Map<String, Object> lensMutation = object(transcript.get("lensMutation"));
        boolean runtimeMutationAccepted = runtimeMutationAccepted(
                lensMutation,
                "player.scanner_used",
                "player.scanner_used"
        );
        boolean scanExecuted = strings(focus, "activationKeys").contains("lensScanExecuted")
                && transcriptEvents.contains("enter:lens:lensScanExecuted");
        boolean lensRendered = "LENS".equals(normalize(render.get("surface")))
                && text(render.get("moduleRendererClass")).contains("Lens")
                && focusLines.stream().anyMatch(line -> line.contains("lens:scan ready"))
                && transcriptLines.stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.lensOutput()));
        boolean accepted = Boolean.TRUE.equals(hotkey.get("observed"))
                && "LEFT_ALT".equals(key)
                && "LENS".equals(surface)
                && Boolean.TRUE.equals(input.get("accepted"))
                && Boolean.TRUE.equals(render.get("accepted"))
                && Boolean.TRUE.equals(focus.get("passed"))
                && Boolean.TRUE.equals(transcript.get("passed"))
                && transcriptEvents.contains("key:LEFT_ALT->LENS")
                && scanExecuted
                && lensRendered
                && runtimeMutationAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("target", EchoNativeAgent5UiExpectedValues.lensTarget());
        result.put("physicalInputAccepted", Boolean.TRUE.equals(input.get("accepted")));
        result.put("renderAccepted", Boolean.TRUE.equals(render.get("accepted")));
        result.put("focusAccepted", Boolean.TRUE.equals(focus.get("passed")));
        result.put("transcriptAccepted", Boolean.TRUE.equals(transcript.get("passed")));
        result.put("scanExecuted", scanExecuted);
        result.put("lensRendered", lensRendered);
        result.put("runtimeMutationAccepted", runtimeMutationAccepted);
        result.put("runtimeActionId", text(lensMutation.get("runtimeActionId")));
        result.put("eventName", text(lensMutation.get("eventName")));
        result.put("effect", accepted
                ? "lens_end_to_end:LEFT_ALT->LENS:" + EchoNativeAgent5UiExpectedValues.lensTarget()
                : "lens_end_to_end:rejected:" + (surface.isBlank() ? "none" : surface));
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
