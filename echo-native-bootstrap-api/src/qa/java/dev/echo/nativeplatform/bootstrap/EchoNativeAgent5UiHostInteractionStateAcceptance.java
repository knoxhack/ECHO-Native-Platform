package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5UiHostInteractionStateAcceptance {
    private EchoNativeAgent5UiHostInteractionStateAcceptance() {
    }

    public static Map<String, Object> assess(Map<String, Object> interactionSmoke) {
        Map<String, Object> smoke = interactionSmoke == null ? Map.of() : interactionSmoke;
        List<Map<String, Object>> steps = maps(smoke.get("steps"));
        boolean terminal = passedStep(steps, "terminal_command", "TERMINAL", "terminal:input",
                EchoNativeAgent5UiExpectedValues.terminalOutput());
        boolean index = passedStep(steps, "index_search", "INDEX", "index:search",
                "index result(s):");
        boolean lens = passedStep(steps, "lens_scan", "LENS", "lens:scan",
                String.valueOf(EchoNativeAgent5UiExpectedValues.lens().get("summary")));
        boolean mission = passedStep(steps, "mission_log_open", "MISSION_LOG", "mission_log:surface",
                EchoNativeAgent5UiExpectedValues.missionObjective());
        boolean settings = passedStep(steps, "settings_open", "SETTINGS", "settings:surface", "ashfall-accessible");
        boolean pause = steps.stream().anyMatch(step -> Boolean.TRUE.equals(step.get("passed"))
                && "pause_resume".equals(step.get("id"))
                && "PAUSE".equals(step.get("surface"))
                && "pause:resume:LENS".equals(step.get("focusPath"))
                && "LENS".equals(step.get("resumeDestinationMode")));
        boolean recovery = passedStep(steps, "recovery_action", "RECOVERY", "recovery:recover", "Status: RECOVERED");
        boolean holomap = passedStep(steps, "holomap_open", "HOLOMAP", "holomap:surface",
                String.valueOf(EchoNativeAgent5UiExpectedValues.holomap().get("layer")));
        boolean wiki = passedStep(steps, "wiki_open", "WIKI", "wiki:surface",
                String.valueOf(EchoNativeAgent5UiExpectedValues.wiki().get("page")));
        boolean mainMenu = passedStep(steps, "main_menu_open", "MAIN_MENU", "main_menu:surface",
                "Main Menu: ECHO Ashfall Terminal boot routes");
        boolean accepted = Boolean.TRUE.equals(smoke.get("serviceCodeExecuted"))
                && Boolean.TRUE.equals(smoke.get("passed"))
                && steps.size() == 10
                && terminal
                && index
                && lens
                && mission
                && settings
                && pause
                && recovery
                && holomap
                && wiki
                && mainMenu;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("stepCount", steps.size());
        result.put("terminalAccepted", terminal);
        result.put("indexAccepted", index);
        result.put("lensAccepted", lens);
        result.put("missionLogAccepted", mission);
        result.put("settingsAccepted", settings);
        result.put("pauseAccepted", pause);
        result.put("recoveryAccepted", recovery);
        result.put("holomapAccepted", holomap);
        result.put("wikiAccepted", wiki);
        result.put("mainMenuAccepted", mainMenu);
        result.put("effect", accepted
                ? "ui_host_interaction_state:accepted:10"
                : "ui_host_interaction_state:rejected:" + steps.size());
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    private static boolean passedStep(
            List<Map<String, Object>> steps,
            String id,
            String surface,
            String focusPath,
            String lineToken
    ) {
        return steps.stream().anyMatch(step -> Boolean.TRUE.equals(step.get("passed"))
                && id.equals(step.get("id"))
                && surface.equals(step.get("surface"))
                && focusPath.equals(step.get("focusPath"))
                && !String.valueOf(step.get("moduleRendererClass")).isBlank()
                && strings(object(step.get("snapshot")).get("surfaceLines")).stream()
                .anyMatch(line -> line.contains(lineToken)));
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
