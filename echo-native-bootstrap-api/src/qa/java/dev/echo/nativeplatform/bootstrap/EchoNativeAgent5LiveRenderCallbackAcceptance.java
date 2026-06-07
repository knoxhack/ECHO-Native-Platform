package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5LiveRenderCallbackAcceptance {
    private EchoNativeAgent5LiveRenderCallbackAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> liveSurfaceAcceptance,
            Map<String, Object> renderState
    ) {
        Map<String, Object> surface = liveSurfaceAcceptance == null ? Map.of() : liveSurfaceAcceptance;
        Map<String, Object> state = renderState == null ? Map.of() : renderState;
        String expectedMode = normalize(surface.get("expectedMode"));
        String currentMode = normalize(surface.get("currentMode"));
        String callbackMode = normalize(state.get("renderCallbackMode"));
        boolean callbackExecuted = Boolean.TRUE.equals(state.get("renderCallbackExecuted"));
        int callbackCount = integer(state.get("renderCallbackCount"));
        int lineCount = integer(state.get("renderCallbackLineCount"));
        int width = integer(state.get("renderCallbackWidth"));
        int height = integer(state.get("renderCallbackHeight"));
        boolean accepted = Boolean.TRUE.equals(surface.get("accepted"))
                && callbackExecuted
                && callbackCount > 0
                && lineCount > 0
                && width > 0
                && height > 0
                && callbackMode.equals(expectedMode)
                && callbackMode.equals(currentMode);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("expectedMode", expectedMode);
        result.put("currentMode", currentMode);
        result.put("callbackMode", callbackMode);
        result.put("callbackExecuted", callbackExecuted);
        result.put("callbackCount", callbackCount);
        result.put("lineCount", lineCount);
        result.put("width", width);
        result.put("height", height);
        result.put("effect", accepted
                ? "live_render_callback:accepted:" + callbackMode
                : "live_render_callback:rejected:" + (callbackMode.isBlank() ? "none" : callbackMode));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> surface = Map.of(
                "accepted", true,
                "expectedMode", "TERMINAL",
                "currentMode", "TERMINAL"
        );
        Map<String, Object> state = Map.of(
                "renderCallbackExecuted", true,
                "renderCallbackCount", 1,
                "renderCallbackMode", "TERMINAL",
                "renderCallbackLineCount", 5,
                "renderCallbackWidth", 1280,
                "renderCallbackHeight", 720
        );
        Map<String, Object> accepted = assess(surface, state);
        Map<String, Object> rejectedNoSurface = assess(Map.of("accepted", false), state);
        Map<String, Object> rejectedNoCallback = assess(surface, Map.of(
                "renderCallbackExecuted", false,
                "renderCallbackCount", 0,
                "renderCallbackMode", "TERMINAL",
                "renderCallbackLineCount", 5,
                "renderCallbackWidth", 1280,
                "renderCallbackHeight", 720
        ));
        Map<String, Object> rejectedWrongMode = assess(surface, Map.of(
                "renderCallbackExecuted", true,
                "renderCallbackCount", 1,
                "renderCallbackMode", "INDEX",
                "renderCallbackLineCount", 5,
                "renderCallbackWidth", 1280,
                "renderCallbackHeight", 720
        ));
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoSurface.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoCallback.get("accepted"))
                && Boolean.FALSE.equals(rejectedWrongMode.get("accepted"));
        return Map.of(
                "liveRenderCallbackAcceptanceClass",
                EchoNativeAgent5LiveRenderCallbackAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoSurface", rejectedNoSurface,
                "rejectedNoCallback", rejectedNoCallback,
                "rejectedWrongMode", rejectedWrongMode,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static String normalize(Object value) {
        return (value == null ? "" : String.valueOf(value)).trim().toUpperCase(Locale.ROOT);
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
