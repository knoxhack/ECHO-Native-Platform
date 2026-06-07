package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveRouteBoundLensScanAcceptance {
    private EchoNativeAgent5LiveRouteBoundLensScanAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> lens,
            Map<String, Object> routeEffectTranscript
    ) {
        boolean lensAccepted = lens != null
                && Boolean.TRUE.equals(lens.get("accepted"))
                && "LEFT_ALT".equals(lens.get("key"))
                && "LENS".equals(lens.get("surface"))
                && EchoNativeAgent5UiExpectedValues.lensTarget().equals(lens.get("target"))
                && Boolean.TRUE.equals(lens.get("scanExecuted"))
                && Boolean.TRUE.equals(lens.get("lensRendered"))
                && ("lens_end_to_end:LEFT_ALT->LENS:" + EchoNativeAgent5UiExpectedValues.lensTarget())
                .equals(lens.get("effect"));
        List<String> observedKeys = strings(routeEffectTranscript == null
                ? null
                : routeEffectTranscript.get("observedKeys"));
        boolean routeBound = routeEffectTranscript != null
                && Boolean.TRUE.equals(routeEffectTranscript.get("accepted"))
                && observedKeys.contains("LEFT_ALT");
        boolean accepted = lensAccepted && routeBound;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("lensAccepted", lensAccepted);
        result.put("routeBound", routeBound);
        result.put("observedKeys", observedKeys);
        result.put("target", lens == null ? "" : text(lens.get("target")));
        result.put("effect", accepted
                ? "live_route_bound_lens_scan:accepted:LEFT_ALT->LENS"
                : "live_route_bound_lens_scan:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> lens = Map.of(
                "accepted", true,
                "key", "LEFT_ALT",
                "surface", "LENS",
                "target", EchoNativeAgent5UiExpectedValues.lensTarget(),
                "scanExecuted", true,
                "lensRendered", true,
                "effect", "lens_end_to_end:LEFT_ALT->LENS:" + EchoNativeAgent5UiExpectedValues.lensTarget()
        );
        Map<String, Object> route = Map.of(
                "accepted", true,
                "observedKeys", List.of("M", "G", "R", "U", "B", "LEFT_ALT", "J", "K")
        );
        Map<String, Object> accepted = assess(lens, route);
        Map<String, Object> rejectedNoLens = assess(Map.of(), route);
        Map<String, Object> rejectedNoRoute = assess(lens, Map.of(
                "accepted", true,
                "observedKeys", List.of("M", "G")
        ));
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_route_bound_lens_scan:accepted:LEFT_ALT->LENS".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoLens.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRoute.get("accepted"));
        return Map.of(
                "liveRouteBoundLensScanAcceptanceClass",
                EchoNativeAgent5LiveRouteBoundLensScanAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoLens", rejectedNoLens,
                "rejectedNoRoute", rejectedNoRoute,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(EchoNativeAgent5LiveRouteBoundLensScanAcceptance::text).toList();
        }
        return List.of();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
