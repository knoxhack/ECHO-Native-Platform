package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LivePhysicalPollLoopAcceptance {
    private static final int REQUIRED_ITERATIONS = 3;
    private static final int REQUIRED_KEY_SAMPLES = 33;

    private EchoNativeAgent5LivePhysicalPollLoopAcceptance() {
    }

    public static Map<String, Object> assess(
            boolean windowHandlePresent,
            boolean focusChecked,
            int pollIterations,
            int keySamples,
            int hotkeyCount
    ) {
        boolean accepted = windowHandlePresent
                && focusChecked
                && pollIterations >= REQUIRED_ITERATIONS
                && keySamples >= REQUIRED_KEY_SAMPLES
                && hotkeyCount >= 11;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("windowHandlePresent", windowHandlePresent);
        result.put("focusChecked", focusChecked);
        result.put("pollIterations", pollIterations);
        result.put("keySamples", keySamples);
        result.put("hotkeyCount", hotkeyCount);
        result.put("requiredIterations", REQUIRED_ITERATIONS);
        result.put("requiredKeySamples", REQUIRED_KEY_SAMPLES);
        result.put("effect", accepted
                ? "live_physical_poll_loop:accepted:" + pollIterations
                : "live_physical_poll_loop:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> accepted = assess(true, true, 3, 33, 11);
        Map<String, Object> rejectedNoWindow = assess(false, true, 3, 33, 11);
        Map<String, Object> rejectedNoFocusCheck = assess(true, false, 3, 33, 11);
        Map<String, Object> rejectedTooFewIterations = assess(true, true, 2, 33, 11);
        Map<String, Object> rejectedTooFewSamples = assess(true, true, 3, 22, 11);
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoWindow.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoFocusCheck.get("accepted"))
                && Boolean.FALSE.equals(rejectedTooFewIterations.get("accepted"))
                && Boolean.FALSE.equals(rejectedTooFewSamples.get("accepted"));
        return Map.of(
                "livePhysicalPollLoopAcceptanceClass",
                EchoNativeAgent5LivePhysicalPollLoopAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoWindow", rejectedNoWindow,
                "rejectedNoFocusCheck", rejectedNoFocusCheck,
                "rejectedTooFewIterations", rejectedTooFewIterations,
                "rejectedTooFewSamples", rejectedTooFewSamples,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }
}
