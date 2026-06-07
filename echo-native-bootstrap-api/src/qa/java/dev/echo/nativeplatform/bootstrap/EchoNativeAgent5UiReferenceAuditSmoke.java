package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5UiReferenceAuditSmoke {
    private static final List<String> REQUIRED_BEHAVIORS = List.of(
            "custom_main_menu",
            "terminal",
            "index",
            "lens_scanner",
            "hud",
            "mission_log",
            "notifications",
            "holomap",
            "wiki",
            "settings",
            "pause_flow",
            "death_recovery_screen",
            "signalos_terminal",
            "ashfall_drone"
    );

    private EchoNativeAgent5UiReferenceAuditSmoke() {
    }

    public static Map<String, Object> capture() {
        return capture(EchoNativeLiveUiBridge.contractSnapshot());
    }

    public static Map<String, Object> capture(Map<String, Object> contract) {
        Map<String, Object> safeContract = contract == null ? Map.of() : contract;
        List<Map<String, Object>> records = EchoNativeAgent5UiReferenceAudit.records();
        List<String> screenIds = strings(safeContract.get("screenIds"));
        List<String> features = strings(safeContract.get("features"));
        Map<String, Object> dataSources = object(safeContract.get("agent5DataSources"));
        List<String> missingBehaviors = new ArrayList<>();
        List<String> missingScreens = new ArrayList<>();
        List<String> missingDataSources = new ArrayList<>();
        List<String> missingAcceptanceFeatures = new ArrayList<>();
        for (Map<String, Object> record : records) {
            String behavior = String.valueOf(record.get("behavior"));
            if (!REQUIRED_BEHAVIORS.contains(behavior)) {
                missingBehaviors.add(behavior);
            }
            if (!screenIds.contains(String.valueOf(record.get("screenId")))) {
                missingScreens.add(behavior);
            }
            if (!dataSources.containsKey(String.valueOf(record.get("dataSource")))) {
                missingDataSources.add(behavior);
            }
            if (!features.contains(String.valueOf(record.get("acceptanceFeature")))) {
                missingAcceptanceFeatures.add(behavior);
            }
        }
        boolean behaviorCoverage = records.stream()
                .map(record -> String.valueOf(record.get("behavior")))
                .toList()
                .equals(REQUIRED_BEHAVIORS);
        boolean passed = behaviorCoverage
                && missingBehaviors.isEmpty()
                && missingScreens.isEmpty()
                && missingDataSources.isEmpty()
                && missingAcceptanceFeatures.isEmpty();

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("uiReferenceAuditSmokeClass", EchoNativeAgent5UiReferenceAuditSmoke.class.getSimpleName());
        smoke.put("behaviors", REQUIRED_BEHAVIORS);
        smoke.put("records", records);
        smoke.put("behaviorCount", records.size());
        smoke.put("missingBehaviors", missingBehaviors);
        smoke.put("missingScreens", missingScreens);
        smoke.put("missingDataSources", missingDataSources);
        smoke.put("missingAcceptanceFeatures", missingAcceptanceFeatures);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
