package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance {
    private EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance() {
    }

    public static Map<String, Object> assess(Object observedEvents) {
        Map<String, String> required = required();
        List<String> requiredKeys = requiredKeys();
        List<?> events = observedEvents instanceof List<?> list ? list : List.of();
        Set<String> observedKeys = new LinkedHashSet<>();
        List<String> routedSurfaces = new ArrayList<>();
        List<String> rejectedEvents = new ArrayList<>();
        for (Object event : events) {
            if (!(event instanceof Map<?, ?> map)) {
                rejectedEvents.add("not_a_map");
                continue;
            }
            String key = text(map.get("key"));
            String surface = text(map.get("surface"));
            boolean sampled = integer(map.get("physicalEventSequence")) > 0
                    && integer(map.get("pollIteration")) > 0
                    && integer(map.get("pollKeySamples")) >= requiredKeys.size();
            boolean routeMatches = Boolean.TRUE.equals(map.get("observed"))
                    && Boolean.FALSE.equals(map.get("handled"))
                    && Boolean.TRUE.equals(map.get("physicalPoller"))
                    && Boolean.FALSE.equals(map.get("serviceCodeExecuted"))
                    && required.containsKey(key)
                    && required.get(key).equals(surface);
            boolean routeEvidence = Boolean.TRUE.equals(map.get("screenOpened"))
                    || Boolean.TRUE.equals(map.get("overlayRendered"))
                    || Boolean.TRUE.equals(map.get("clientOverlayStateChanged"))
                    || Boolean.TRUE.equals(map.get("liveSurfaceAccepted"))
                    || Boolean.TRUE.equals(map.get("dataBackedAction"))
                    || Boolean.TRUE.equals(map.get("stateChanged"))
                    || Boolean.TRUE.equals(map.get("mapStateChanged"))
                    || Boolean.TRUE.equals(map.get("hudStateChanged"))
                    || Boolean.TRUE.equals(map.get("serverboundPacketSent"))
                    || Boolean.TRUE.equals(map.get("entityCommandExecuted"));
            boolean runtimeMutation = Boolean.TRUE.equals(map.get("runtimeHostMutated"));
            boolean durableFeedback = Boolean.TRUE.equals(map.get("saveTouched"))
                    && Boolean.TRUE.equals(map.get("feedbackEmitted"));
            boolean missionUpdated = Boolean.TRUE.equals(map.get("missionUpdated"));
            boolean effectAccepted = Boolean.TRUE.equals(map.get("routeEffectAccepted"))
                    && routeEvidence
                    && runtimeMutation
                    && durableFeedback
                    && missionUpdated;
            if (sampled && routeMatches && effectAccepted) {
                observedKeys.add(key);
                routedSurfaces.add(surface);
            } else {
                rejectedEvents.add(key + "->" + surface);
            }
        }
        List<String> missingKeys = requiredKeys.stream()
                .filter(key -> !observedKeys.contains(key))
                .toList();
        boolean accepted = missingKeys.isEmpty() && rejectedEvents.isEmpty();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("requiredKeys", requiredKeys);
        result.put("observedKeys", List.copyOf(observedKeys));
        result.put("routedSurfaces", List.copyOf(routedSurfaces));
        result.put("missingKeys", missingKeys);
        result.put("rejectedEvents", rejectedEvents);
        result.put("eventCount", events.size());
        result.put("effect", accepted
                ? "live_physical_route_effect_transcript:accepted:" + observedKeys.size()
                : "live_physical_route_effect_transcript:rejected");
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
            acceptedEvents.add(event(i + 1, i + 3, 33 + (i * 11), key, required.get(key), true));
        }
        Map<String, Object> accepted = assess(acceptedEvents);
        List<Map<String, Object>> noSurfaceEffectEvents = new ArrayList<>(acceptedEvents);
        noSurfaceEffectEvents.set(0, event(1, 3, 33, "M", "TERMINAL", false));
        Map<String, Object> rejectedNoSurfaceEffect = assess(noSurfaceEffectEvents);
        List<Map<String, Object>> noLastRouteEffectEvents = new ArrayList<>(acceptedEvents);
        int lastRouteIndex = requiredKeys.size() - 1;
        String lastRouteKey = requiredKeys.get(lastRouteIndex);
        noLastRouteEffectEvents.set(lastRouteIndex,
                event(lastRouteIndex + 1, lastRouteIndex + 3, 33 + (lastRouteIndex * 11),
                        lastRouteKey, required.get(lastRouteKey), false));
        Map<String, Object> rejectedNoHudEffect = assess(noLastRouteEffectEvents);
        List<Map<String, Object>> noSampleMetricsEvents = new ArrayList<>(acceptedEvents);
        Map<String, Object> missingSample = new LinkedHashMap<>(noSampleMetricsEvents.get(1));
        missingSample.remove("pollIteration");
        noSampleMetricsEvents.set(1, Map.copyOf(missingSample));
        Map<String, Object> rejectedNoSampleMetrics = assess(noSampleMetricsEvents);
        List<Map<String, Object>> adapterCoreOnlyEvents = new ArrayList<>(acceptedEvents);
        Map<String, Object> adapterCoreOnly = new LinkedHashMap<>(adapterCoreOnlyEvents.get(2));
        adapterCoreOnly.put("runtimeHostMutated", false);
        adapterCoreOnly.put("adapterCoreMutation", true);
        adapterCoreOnly.put("saveTouched", true);
        adapterCoreOnlyEvents.set(2, Map.copyOf(adapterCoreOnly));
        Map<String, Object> rejectedAdapterCoreOnly = assess(adapterCoreOnlyEvents);
        List<Map<String, Object>> noSaveEvents = new ArrayList<>(acceptedEvents);
        Map<String, Object> missingSave = new LinkedHashMap<>(noSaveEvents.get(3));
        missingSave.put("saveTouched", false);
        noSaveEvents.set(3, Map.copyOf(missingSave));
        Map<String, Object> rejectedNoSave = assess(noSaveEvents);
        List<Map<String, Object>> noFeedbackEvents = new ArrayList<>(acceptedEvents);
        Map<String, Object> missingFeedback = new LinkedHashMap<>(noFeedbackEvents.get(4));
        missingFeedback.put("feedbackEmitted", false);
        noFeedbackEvents.set(4, Map.copyOf(missingFeedback));
        Map<String, Object> rejectedNoFeedback = assess(noFeedbackEvents);
        List<Map<String, Object>> noMissionEvents = new ArrayList<>(acceptedEvents);
        Map<String, Object> missingMission = new LinkedHashMap<>(noMissionEvents.get(5));
        missingMission.put("missionUpdated", false);
        noMissionEvents.set(5, Map.copyOf(missingMission));
        Map<String, Object> rejectedNoMission = assess(noMissionEvents);
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && ("live_physical_route_effect_transcript:accepted:" + requiredKeys.size())
                        .equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoSurfaceEffect.get("accepted"))
                && strings(rejectedNoSurfaceEffect.get("missingKeys")).contains("M")
                && Boolean.FALSE.equals(rejectedNoHudEffect.get("accepted"))
                && strings(rejectedNoHudEffect.get("missingKeys")).contains(lastRouteKey)
                && Boolean.FALSE.equals(rejectedNoSampleMetrics.get("accepted"))
                && strings(rejectedNoSampleMetrics.get("missingKeys")).contains("G")
                && Boolean.FALSE.equals(rejectedAdapterCoreOnly.get("accepted"))
                && strings(rejectedAdapterCoreOnly.get("missingKeys")).contains("R")
                && Boolean.FALSE.equals(rejectedNoSave.get("accepted"))
                && strings(rejectedNoSave.get("missingKeys")).contains("U")
                && Boolean.FALSE.equals(rejectedNoFeedback.get("accepted"))
                && strings(rejectedNoFeedback.get("missingKeys")).contains("B")
                && Boolean.FALSE.equals(rejectedNoMission.get("accepted"))
                && strings(rejectedNoMission.get("missingKeys")).contains("LEFT_ALT");
        return Map.ofEntries(
                Map.entry("livePhysicalRouteEffectTranscriptAcceptanceClass",
                        EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance.class.getSimpleName()),
                Map.entry("accepted", accepted),
                Map.entry("rejectedNoSurfaceEffect", rejectedNoSurfaceEffect),
                Map.entry("rejectedNoHudEffect", rejectedNoHudEffect),
                Map.entry("rejectedNoSampleMetrics", rejectedNoSampleMetrics),
                Map.entry("rejectedAdapterCoreOnly", rejectedAdapterCoreOnly),
                Map.entry("rejectedNoSave", rejectedNoSave),
                Map.entry("rejectedNoFeedback", rejectedNoFeedback),
                Map.entry("rejectedNoMission", rejectedNoMission),
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
            String surface,
            boolean effectAccepted
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("observed", true);
        event.put("handled", false);
        event.put("physicalPoller", true);
        event.put("physicalPollerExecuted", true);
        event.put("serviceCodeExecuted", false);
        event.put("physicalEventSequence", sequence);
        event.put("pollIteration", pollIteration);
        event.put("pollKeySamples", pollKeySamples);
        event.put("key", key);
        event.put("surface", surface);
        event.put("routeEffectAccepted", effectAccepted);
        event.put("hudOverlay", false);
        event.put("liveSurfaceAccepted", effectAccepted);
        event.put("liveSurfaceRendered", effectAccepted);
        event.put("physicalInputAccepted", effectAccepted);
        event.put("screenOwnershipAccepted", effectAccepted);
        event.put("renderCallbackAccepted", effectAccepted);
        event.put("screenOpened", effectAccepted && !"LENS".equals(surface));
        event.put("overlayRendered", effectAccepted && "LENS".equals(surface));
        event.put("dataBackedAction", effectAccepted);
        event.put("stateChanged", effectAccepted);
        event.put("runtimeHostMutated", effectAccepted);
        event.put("adapterCoreMutation", effectAccepted);
        event.put("saveTouched", effectAccepted);
        event.put("feedbackEmitted", effectAccepted);
        event.put("missionUpdated", effectAccepted);
        return Map.copyOf(event);
    }

    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(EchoNativeAgent5LivePhysicalRouteEffectTranscriptAcceptance::text).toList();
        }
        return List.of();
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
