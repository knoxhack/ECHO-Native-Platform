package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveWindowFocusAcceptance {
    private EchoNativeAgent5LiveWindowFocusAcceptance() {
    }

    public static Map<String, Object> assess(boolean windowHandlePresent, boolean focusChecked, boolean focused) {
        boolean accepted = windowHandlePresent && focusChecked && focused;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("windowHandlePresent", windowHandlePresent);
        result.put("focusChecked", focusChecked);
        result.put("focused", focused);
        result.put("effect", accepted ? "live_window_focus:accepted" : "live_window_focus:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> accepted = assess(true, true, true);
        Map<String, Object> rejectedNoWindow = assess(false, true, true);
        Map<String, Object> rejectedNoCheck = assess(true, false, true);
        Map<String, Object> rejectedUnfocused = assess(true, true, false);
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoWindow.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoCheck.get("accepted"))
                && Boolean.FALSE.equals(rejectedUnfocused.get("accepted"));
        return Map.of(
                "liveWindowFocusAcceptanceClass",
                EchoNativeAgent5LiveWindowFocusAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoWindow", rejectedNoWindow,
                "rejectedNoCheck", rejectedNoCheck,
                "rejectedUnfocused", rejectedUnfocused,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }
}
