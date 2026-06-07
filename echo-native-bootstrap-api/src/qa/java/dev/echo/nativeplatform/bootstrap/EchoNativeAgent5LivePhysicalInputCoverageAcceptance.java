package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeAgent5LivePhysicalInputCoverageAcceptance {
    private EchoNativeAgent5LivePhysicalInputCoverageAcceptance() {
    }

    public static Map<String, Object> assess(Object observedEvents) {
        Map<String, String> required = required();
        List<String> requiredKeys = requiredKeys();
        List<?> events = observedEvents instanceof List<?> list ? list : List.of();
        Set<String> observedKeys = new LinkedHashSet<>();
        List<String> rejectedEvents = new ArrayList<>();
        for (Object event : events) {
            if (!(event instanceof Map<?, ?> map)) {
                rejectedEvents.add("not_a_map");
                continue;
            }
            String key = text(map.get("key"));
            String surface = text(map.get("surface"));
            boolean valid = Boolean.TRUE.equals(map.get("observed"))
                    && Boolean.FALSE.equals(map.get("handled"))
                    && Boolean.TRUE.equals(map.get("physicalPoller"))
                    && Boolean.FALSE.equals(map.get("serviceCodeExecuted"))
                    && required.containsKey(key)
                    && required.get(key).equals(surface);
            if (valid) {
                observedKeys.add(key);
            } else {
                rejectedEvents.add(key + "->" + surface);
            }
        }
        List<String> missingKeys = requiredKeys.stream()
                .filter(key -> !observedKeys.contains(key))
                .toList();
        boolean accepted = missingKeys.isEmpty();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("requiredKeys", requiredKeys);
        result.put("observedKeys", List.copyOf(observedKeys));
        result.put("missingKeys", missingKeys);
        result.put("rejectedEvents", rejectedEvents);
        result.put("observedCount", observedKeys.size());
        result.put("effect", accepted
                ? "live_physical_input_coverage:accepted:" + observedKeys.size()
                : "live_physical_input_coverage:rejected:missing=" + String.join(",", missingKeys));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, String> required = required();
        List<String> requiredKeys = requiredKeys();
        List<Map<String, Object>> acceptedEvents = requiredKeys.stream()
                .map(key -> event(key, required.get(key)))
                .toList();
        Map<String, Object> accepted = assess(acceptedEvents);
        Map<String, Object> rejectedMissing = assess(acceptedEvents.stream()
                .filter(event -> !"LEFT_ALT".equals(event.get("key")))
                .toList());
        List<Map<String, Object>> wrongSurfaceEvents = new ArrayList<>(acceptedEvents);
        wrongSurfaceEvents.set(0, event("M", "INDEX"));
        Map<String, Object> rejectedWrongSurface = assess(wrongSurfaceEvents);
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && ("live_physical_input_coverage:accepted:" + requiredKeys.size()).equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedMissing.get("accepted"))
                && strings(rejectedMissing.get("missingKeys")).contains("LEFT_ALT")
                && Boolean.FALSE.equals(rejectedWrongSurface.get("accepted"))
                && strings(rejectedWrongSurface.get("missingKeys")).contains("M");
        return Map.of(
                "livePhysicalInputCoverageAcceptanceClass",
                EchoNativeAgent5LivePhysicalInputCoverageAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedMissing", rejectedMissing,
                "rejectedWrongSurface", rejectedWrongSurface,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static Map<String, String> required() {
        return EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageSurfacesByKey();
    }

    private static List<String> requiredKeys() {
        return EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageKeys();
    }

    private static Map<String, Object> event(String key, String surface) {
        return Map.of(
                "observed", true,
                "handled", false,
                "physicalPoller", true,
                "physicalPollerExecuted", true,
                "serviceCodeExecuted", false,
                "key", key,
                "surface", surface
        );
    }

    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(EchoNativeAgent5LivePhysicalInputCoverageAcceptance::text).toList();
        }
        return List.of();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
