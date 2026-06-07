package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveScreenStackStabilityAcceptance {
    private EchoNativeAgent5LiveScreenStackStabilityAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> screenStackSmoke,
            Map<String, Object> screenLifecycleSmoke,
            Map<String, Object> uiHostInteractionSmoke
    ) {
        Map<String, Object> stack = screenStackSmoke == null ? Map.of() : screenStackSmoke;
        Map<String, Object> lifecycle = screenLifecycleSmoke == null ? Map.of() : screenLifecycleSmoke;
        Map<String, Object> interaction = uiHostInteractionSmoke == null ? Map.of() : uiHostInteractionSmoke;
        List<Map<String, Object>> steps = maps(interaction.get("steps"));
        boolean stackAccepted = Boolean.TRUE.equals(stack.get("passed"))
                && "EchoNativeAgent5ScreenStackSmoke".equals(stack.get("screenStackSmokeClass"))
                && strings(stack, "events").containsAll(List.of(
                "push:MAIN_MENU",
                "push:TERMINAL",
                "push:INDEX",
                "push:LENS",
                "push:PAUSE",
                "pop:PAUSE",
                "replace:SETTINGS",
                "replace:LENS",
                "push:RECOVERY",
                "pop:RECOVERY",
                "empty-pop"
        ))
                && Boolean.TRUE.equals(stack.get("emptyPopSafe"))
                && "LENS".equals(stack.get("resumeMode"))
                && "MAIN_MENU".equals(stack.get("finalCurrentMode"))
                && Integer.valueOf(1).equals(stack.get("finalStackSize"));
        boolean lifecycleAccepted = Boolean.TRUE.equals(lifecycle.get("passed"))
                && "EchoNativeAgent5ScreenLifecycleSmoke".equals(lifecycle.get("screenLifecycleSmokeClass"))
                && strings(lifecycle, "visitedModes").containsAll(List.of(
                "MAIN_MENU",
                "TERMINAL",
                "INDEX",
                "LENS",
                "PAUSE",
                "RECOVERY"
        ))
                && strings(lifecycle, "routeEffects").containsAll(List.of(
                "route:terminal",
                "route:index",
                "route:lens",
                "route:escape",
                "system:recovery",
                "system:main_menu"
        ))
                && "LENS".equals(lifecycle.get("pausePreviousMode"))
                && "LENS".equals(lifecycle.get("resumeMode"))
                && strings(lifecycle, "actionExecutedKeys").containsAll(List.of(
                "terminalCommandExecuted",
                "indexSearchExecuted",
                "lensScanExecuted",
                "recoveryActionExecuted"
        ));
        boolean interactionAccepted = Boolean.TRUE.equals(interaction.get("passed"))
                && "EchoNativeAgent5UiHostInteractionSmoke".equals(interaction.get("interactionSmokeClass"))
                && steps.size() == 10
                && hasStep(steps, "terminal_command")
                && hasStep(steps, "index_search")
                && hasStep(steps, "lens_scan")
                && hasStep(steps, "mission_log_open")
                && hasStep(steps, "settings_open")
                && hasStep(steps, "pause_resume")
                && hasStep(steps, "recovery_action")
                && hasStep(steps, "holomap_open")
                && hasStep(steps, "wiki_open")
                && hasStep(steps, "main_menu_open");
        boolean accepted = stackAccepted && lifecycleAccepted && interactionAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("stackAccepted", stackAccepted);
        result.put("lifecycleAccepted", lifecycleAccepted);
        result.put("interactionAccepted", interactionAccepted);
        result.put("finalCurrentMode", String.valueOf(stack.getOrDefault("finalCurrentMode", "")));
        result.put("finalStackSize", stack.getOrDefault("finalStackSize", 0));
        result.put("resumeMode", String.valueOf(lifecycle.getOrDefault("resumeMode", "")));
        result.put("interactionStepCount", steps.size());
        result.put("effect", accepted
                ? "live_screen_stack_stability:accepted:10-surfaces:no-crash"
                : "live_screen_stack_stability:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    private static boolean hasStep(List<Map<String, Object>> steps, String id) {
        return steps.stream()
                .anyMatch(step -> id.equals(step.get("id")) && Boolean.TRUE.equals(step.get("passed")));
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
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
}
