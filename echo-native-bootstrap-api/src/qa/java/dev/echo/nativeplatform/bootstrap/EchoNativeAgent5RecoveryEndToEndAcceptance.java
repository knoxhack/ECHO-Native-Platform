package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5RecoveryEndToEndAcceptance {
    private static final String RECOVERY_ITEM_ID = "echoashfallprotocol:portable_signal_scanner";
    private static final String RECOVERY_RUNTIME_ACTION_ID = "player.inventory.grant";

    private EchoNativeAgent5RecoveryEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> physicalInputAcceptance,
            Map<String, Object> liveSurfaceRenderAcceptance,
            Map<String, Object> interactionSmoke
    ) {
        Map<String, Object> routeAction = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> input = physicalInputAcceptance == null ? Map.of() : physicalInputAcceptance;
        Map<String, Object> render = liveSurfaceRenderAcceptance == null ? Map.of() : liveSurfaceRenderAcceptance;
        Map<String, Object> interaction = interactionSmoke == null ? Map.of() : interactionSmoke;
        Map<String, Object> recoveryMutation = object(interaction.get("recoveryMutation"));
        String key = text(routeAction.getOrDefault("key", "RECOVERY_ACTION"));
        String surface = normalize(routeAction.get("surface"));
        Map<String, Object> step = step(interaction, "recovery_action");
        Map<String, Object> snapshot = object(step.get("snapshot"));
        List<String> lines = strings(snapshot, "surfaceLines");
        String recoveryItemId = firstNonBlank(recoveryMutation.get("itemId"), recoveryMutation.get("requestedItemId"));
        boolean runtimeMutationAccepted = Boolean.TRUE.equals(recoveryMutation.get("mutated"))
                && Boolean.TRUE.equals(recoveryMutation.get("saveTouched"))
                && Boolean.TRUE.equals(recoveryMutation.get("missionUpdated"))
                && Boolean.TRUE.equals(recoveryMutation.get("feedbackEmitted"))
                && RECOVERY_RUNTIME_ACTION_ID.equals(recoveryMutation.get("runtimeActionId"))
                && RECOVERY_ITEM_ID.equals(recoveryItemId);
        boolean interactionAccepted = Boolean.TRUE.equals(interaction.get("passed"))
                && Boolean.TRUE.equals(step.get("passed"))
                && "RECOVERY".equals(step.get("surface"))
                && "recovery:recover".equals(step.get("focusPath"))
                && text(step.get("moduleRendererClass")).contains("Recovery")
                && lines.stream().anyMatch(line -> line.contains("echorecovery:ashfall_field_recovery_cache"))
                && lines.stream().anyMatch(line -> line.contains("Status: RECOVERED"));
        boolean recoveryRendered = "RECOVERY".equals(normalize(render.get("surface")))
                && text(render.get("moduleRendererClass")).contains("Recovery");
        boolean accepted = "RECOVERY".equals(surface)
                && Boolean.TRUE.equals(input.get("accepted"))
                && Boolean.TRUE.equals(render.get("accepted"))
                && recoveryRendered
                && interactionAccepted
                && runtimeMutationAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("recoveryFocusPath", step.getOrDefault("focusPath", ""));
        result.put("physicalInputAccepted", Boolean.TRUE.equals(input.get("accepted")));
        result.put("renderAccepted", Boolean.TRUE.equals(render.get("accepted")));
        result.put("interactionAccepted", interactionAccepted);
        result.put("recoveryRendered", recoveryRendered);
        result.put("runtimeMutationAccepted", runtimeMutationAccepted);
        result.put("runtimeActionId", text(recoveryMutation.get("runtimeActionId")));
        result.put("itemId", recoveryItemId);
        result.put("requestedItemId", text(recoveryMutation.get("requestedItemId")));
        result.put("effect", accepted
                ? "recovery_end_to_end:" + key + "->RECOVERY:RECOVERED"
                : "recovery_end_to_end:rejected:" + (surface.isBlank() ? "none" : surface));
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

    private static String normalize(Object value) {
        return text(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String firstNonBlank(Object first, Object second) {
        String firstText = text(first);
        return firstText.isBlank() ? text(second) : firstText;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
