package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5MissionLogEndToEndAcceptance {
    private EchoNativeAgent5MissionLogEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> physicalInputAcceptance,
            Map<String, Object> liveSurfaceRenderAcceptance,
            Map<String, Object> interactionSmoke,
            Map<String, Object> updateSmoke
    ) {
        Map<String, Object> routeAction = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> input = physicalInputAcceptance == null ? Map.of() : physicalInputAcceptance;
        Map<String, Object> render = liveSurfaceRenderAcceptance == null ? Map.of() : liveSurfaceRenderAcceptance;
        Map<String, Object> interaction = interactionSmoke == null ? Map.of() : interactionSmoke;
        Map<String, Object> update = updateSmoke == null ? Map.of() : updateSmoke;
        String key = text(routeAction.getOrDefault("key", "MISSION_ACTION"));
        String surface = normalize(routeAction.get("surface"));
        Map<String, Object> step = step(interaction, "mission_log_open");
        Map<String, Object> snapshot = object(step.get("snapshot"));
        List<String> lines = strings(snapshot, "surfaceLines");
        boolean runtimeMutationAccepted = Boolean.TRUE.equals(update.get("runtimeMutationAccepted"))
                && "native.ui.mission_log_update".equals(update.get("runtimeActionId"))
                && "mission.objective_completed".equals(update.get("eventName"));
        boolean interactionAccepted = Boolean.TRUE.equals(interaction.get("passed"))
                && Boolean.TRUE.equals(step.get("passed"))
                && "MISSION_LOG".equals(step.get("surface"))
                && "mission_log:surface".equals(step.get("focusPath"))
                && text(step.get("moduleRendererClass")).contains("MissionLog")
                && lines.stream().anyMatch(line -> line.contains("Anchor Pod Outpost"))
                && lines.stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.missionObjective()));
        boolean updateAccepted = Boolean.TRUE.equals(update.get("passed"))
                && "echoashfallprotocol:secure_crash_outpost".equals(update.get("missionId"))
                && "UPDATED".equals(update.get("missionStatus"))
                && Double.valueOf(0.5D).equals(update.get("missionProgress"))
                && "mission:update:echoashfallprotocol:secure_crash_outpost".equals(update.get("effect"))
                && runtimeMutationAccepted
                && strings(update, "renderedLines").stream()
                .anyMatch(line -> line.contains("Update: Drop pod signal confirmed"));
        boolean missionLogRendered = "MISSION_LOG".equals(normalize(render.get("surface")))
                && text(render.get("moduleRendererClass")).contains("MissionLog");
        boolean accepted = "MISSION_LOG".equals(surface)
                && Boolean.TRUE.equals(input.get("accepted"))
                && Boolean.TRUE.equals(render.get("accepted"))
                && missionLogRendered
                && interactionAccepted
                && updateAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("missionId", "echoashfallprotocol:secure_crash_outpost");
        result.put("missionStatus", update.getOrDefault("missionStatus", ""));
        result.put("missionProgress", update.getOrDefault("missionProgress", 0.0D));
        result.put("physicalInputAccepted", Boolean.TRUE.equals(input.get("accepted")));
        result.put("renderAccepted", Boolean.TRUE.equals(render.get("accepted")));
        result.put("interactionAccepted", interactionAccepted);
        result.put("updateAccepted", updateAccepted);
        result.put("missionLogRendered", missionLogRendered);
        result.put("runtimeMutationAccepted", runtimeMutationAccepted);
        result.put("runtimeActionId", text(update.get("runtimeActionId")));
        result.put("eventName", text(update.get("eventName")));
        result.put("effect", accepted
                ? "mission_log_end_to_end:" + key + "->MISSION_LOG:secure_crash_outpost:UPDATED"
                : "mission_log_end_to_end:rejected:" + (surface.isBlank() ? "none" : surface));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    private static Map<String, Object> step(Map<String, Object> interaction, String id) {
        return maps(interaction.get("steps")).stream()
                .filter(entry -> id.equals(entry.get("id")))
                .findFirst()
                .orElse(Map.of());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .toList();
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

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object source, String key) {
        if (source instanceof Map<?, ?> map) {
            return strings((Map<String, Object>) map, key);
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
