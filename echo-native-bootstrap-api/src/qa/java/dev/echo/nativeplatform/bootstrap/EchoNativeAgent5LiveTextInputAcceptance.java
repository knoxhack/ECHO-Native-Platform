package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5LiveTextInputAcceptance {
    private EchoNativeAgent5LiveTextInputAcceptance() {
    }

    public static Map<String, Object> assess(
            String mode,
            String expectedBuffer,
            String expectedOutput,
            List<Map<String, Object>> characterRoutes,
            Map<String, Object> editRoute,
            Map<String, Object> submitAction,
            Map<String, Object> hostSnapshot
    ) {
        Map<String, Object> edit = editRoute == null ? Map.of() : editRoute;
        Map<String, Object> submit = submitAction == null ? Map.of() : submitAction;
        Map<String, Object> snapshot = hostSnapshot == null ? Map.of() : hostSnapshot;
        String normalizedMode = normalize(mode);
        String finalBuffer = text(edit.get("value"));
        List<Map<String, Object>> routes = characterRoutes == null ? List.of() : characterRoutes;
        List<String> renderedLines = strings(snapshot.get("surfaceLines"));
        boolean accepted = !routes.isEmpty()
                && routes.stream().allMatch(route -> Boolean.TRUE.equals(route.get("handled")))
                && Boolean.TRUE.equals(edit.get("handled"))
                && Boolean.TRUE.equals(submit.get("handled"))
                && expectedBuffer.equals(finalBuffer)
                && expectedOutput.equals(submit.get("output"))
                && normalizedMode.equals(normalize(snapshot.get("surface")))
                && renderedLines.stream().anyMatch(line -> line.contains(expectedOutput));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("mode", normalizedMode);
        result.put("characterCount", routes.size());
        result.put("editHandled", Boolean.TRUE.equals(edit.get("handled")));
        result.put("submitHandled", Boolean.TRUE.equals(submit.get("handled")));
        result.put("finalBuffer", finalBuffer);
        result.put("expectedBuffer", expectedBuffer);
        result.put("output", text(submit.get("output")));
        result.put("expectedOutput", expectedOutput);
        result.put("renderedLineCount", renderedLines.size());
        result.put("effect", accepted
                ? "live_text_input:accepted:" + normalizedMode + ":" + expectedBuffer
                : "live_text_input:rejected:" + normalizedMode);
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
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
