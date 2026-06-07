package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5UiHostInteractionStateAcceptanceSmoke {
    private EchoNativeAgent5UiHostInteractionStateAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> interaction = EchoNativeAgent5UiHostInteractionSmoke.run(
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> accepted = EchoNativeAgent5UiHostInteractionStateAcceptance.assess(interaction);
        Map<String, Object> rejectedMissingStep = EchoNativeAgent5UiHostInteractionStateAcceptance.assess(withoutStep(
                interaction,
                "wiki_open"
        ));
        Map<String, Object> rejectedFailedStep = EchoNativeAgent5UiHostInteractionStateAcceptance.assess(failedStep(
                interaction,
                "terminal_command"
        ));
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "ui_host_interaction_state:accepted:10".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedMissingStep.get("accepted"))
                && Boolean.FALSE.equals(rejectedFailedStep.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("uiHostInteractionStateAcceptanceSmokeClass",
                EchoNativeAgent5UiHostInteractionStateAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedMissingStep", rejectedMissingStep);
        smoke.put("rejectedFailedStep", rejectedFailedStep);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> withoutStep(Map<String, Object> smoke, String id) {
        List<Map<String, Object>> filtered = maps(smoke.get("steps")).stream()
                .filter(step -> !id.equals(step.get("id")))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>(smoke);
        result.put("steps", filtered);
        result.put("passed", false);
        return Map.copyOf(result);
    }

    private static Map<String, Object> failedStep(Map<String, Object> smoke, String id) {
        ArrayList<Map<String, Object>> changed = new ArrayList<>();
        for (Map<String, Object> step : maps(smoke.get("steps"))) {
            if (id.equals(step.get("id"))) {
                Map<String, Object> failed = new LinkedHashMap<>(step);
                failed.put("passed", false);
                changed.add(Map.copyOf(failed));
            } else {
                changed.add(step);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>(smoke);
        result.put("steps", List.copyOf(changed));
        result.put("passed", false);
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .map(Map::copyOf)
                    .toList();
        }
        return List.of();
    }
}
