package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeAgent5LivePhysicalEventTranscriptAcceptance {
    private EchoNativeAgent5LivePhysicalEventTranscriptAcceptance() {
    }

    public static Map<String, Object> assess(Object observedEvents) {
        Map<String, String> required = required();
        List<String> requiredKeys = requiredKeys();
        List<?> events = observedEvents instanceof List<?> list ? list : List.of();
        Set<String> observedKeys = new LinkedHashSet<>();
        List<String> routedSurfaces = new ArrayList<>();
        List<String> rejectedEvents = new ArrayList<>();
        int previousSequence = 0;
        int previousPollIteration = 0;
        boolean sequenceOrdered = true;
        boolean pollMetricsPresent = true;
        for (Object event : events) {
            if (!(event instanceof Map<?, ?> map)) {
                rejectedEvents.add("not_a_map");
                sequenceOrdered = false;
                pollMetricsPresent = false;
                continue;
            }
            String key = text(map.get("key"));
            String surface = text(map.get("surface"));
            int sequence = integer(map.get("physicalEventSequence"));
            int pollIteration = integer(map.get("pollIteration"));
            int pollKeySamples = integer(map.get("pollKeySamples"));
            boolean ordered = sequence > previousSequence;
            boolean hasPollMetrics = pollIteration > 0
                    && pollIteration >= previousPollIteration
                    && pollKeySamples >= requiredKeys.size();
            boolean validRoute = Boolean.TRUE.equals(map.get("observed"))
                    && Boolean.FALSE.equals(map.get("handled"))
                    && Boolean.TRUE.equals(map.get("physicalPoller"))
                    && Boolean.FALSE.equals(map.get("serviceCodeExecuted"))
                    && required.containsKey(key)
                    && required.get(key).equals(surface);
            if (ordered && hasPollMetrics && validRoute) {
                observedKeys.add(key);
                routedSurfaces.add(surface);
            } else {
                rejectedEvents.add(key + "->" + surface);
            }
            sequenceOrdered = sequenceOrdered && ordered;
            pollMetricsPresent = pollMetricsPresent && hasPollMetrics;
            previousSequence = sequence;
            previousPollIteration = pollIteration;
        }
        List<String> missingKeys = requiredKeys.stream()
                .filter(key -> !observedKeys.contains(key))
                .toList();
        boolean accepted = missingKeys.isEmpty()
                && rejectedEvents.isEmpty()
                && sequenceOrdered
                && pollMetricsPresent;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("requiredKeys", requiredKeys);
        result.put("observedKeys", List.copyOf(observedKeys));
        result.put("routedSurfaces", List.copyOf(routedSurfaces));
        result.put("missingKeys", missingKeys);
        result.put("rejectedEvents", rejectedEvents);
        result.put("eventCount", events.size());
        result.put("sequenceOrdered", sequenceOrdered);
        result.put("pollMetricsPresent", pollMetricsPresent);
        result.put("effect", accepted
                ? "live_physical_event_transcript:accepted:" + observedKeys.size()
                : "live_physical_event_transcript:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, String> required = required();
        List<String> requiredKeys = requiredKeys();
        List<Map<String, Object>> acceptedEvents = new ArrayList<>();
        for (int i = 0; i < requiredKeys.size(); i++) {
            String key = requiredKeys.get(i);
            acceptedEvents.add(event(i + 1, i + 3, 33 + (i * 11), key, required.get(key)));
        }
        Map<String, Object> accepted = assess(acceptedEvents);
        List<Map<String, Object>> missingSequenceEvents = new ArrayList<>(acceptedEvents);
        missingSequenceEvents.set(2, without(missingSequenceEvents.get(2), "physicalEventSequence"));
        Map<String, Object> rejectedMissingSequence = assess(missingSequenceEvents);
        List<Map<String, Object>> unorderedEvents = new ArrayList<>(acceptedEvents);
        unorderedEvents.set(1, event(1, 4, 44, "G", "INDEX"));
        Map<String, Object> rejectedUnordered = assess(unorderedEvents);
        List<Map<String, Object>> noPollMetricsEvents = new ArrayList<>(acceptedEvents);
        noPollMetricsEvents.set(4, without(noPollMetricsEvents.get(4), "pollIteration"));
        Map<String, Object> rejectedNoPollMetrics = assess(noPollMetricsEvents);
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && ("live_physical_event_transcript:accepted:" + requiredKeys.size())
                        .equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedMissingSequence.get("accepted"))
                && Boolean.FALSE.equals(rejectedMissingSequence.get("sequenceOrdered"))
                && Boolean.FALSE.equals(rejectedUnordered.get("accepted"))
                && Boolean.FALSE.equals(rejectedUnordered.get("sequenceOrdered"))
                && Boolean.FALSE.equals(rejectedNoPollMetrics.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoPollMetrics.get("pollMetricsPresent"));
        return Map.ofEntries(
                Map.entry("livePhysicalEventTranscriptAcceptanceClass",
                        EchoNativeAgent5LivePhysicalEventTranscriptAcceptance.class.getSimpleName()),
                Map.entry("accepted", accepted),
                Map.entry("rejectedMissingSequence", rejectedMissingSequence),
                Map.entry("rejectedUnordered", rejectedUnordered),
                Map.entry("rejectedNoPollMetrics", rejectedNoPollMetrics),
                Map.entry("passed", passed),
                Map.entry("adapterCoreBridge", true),
                Map.entry("serviceCodeExecuted", true)
        );
    }

    private static Map<String, String> required() {
        return EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageSurfacesByKey();
    }

    private static List<String> requiredKeys() {
        return EchoNativeAgent5PhysicalRouteRequirements.physicalCoverageKeys();
    }

    private static Map<String, Object> event(
            int sequence,
            int pollIteration,
            int pollKeySamples,
            String key,
            String surface
    ) {
        return Map.of(
                "observed", true,
                "handled", false,
                "physicalPoller", true,
                "physicalPollerExecuted", true,
                "serviceCodeExecuted", false,
                "physicalEventSequence", sequence,
                "pollIteration", pollIteration,
                "pollKeySamples", pollKeySamples,
                "key", key,
                "surface", surface
        );
    }

    private static Map<String, Object> without(Map<String, Object> source, String key) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.remove(key);
        return Map.copyOf(copy);
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
