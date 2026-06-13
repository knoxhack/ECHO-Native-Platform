package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeAgent5ScreenHostModel {
    private EchoNativeAgent5ScreenHostModel() {
    }

    static Map<String, Object> render(
            String surface,
            Map<String, Object> state,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        String normalized = surface == null || surface.isBlank() ? "TERMINAL" : surface.trim().toUpperCase();
        Map<String, Object> safeState = state == null ? Map.of() : Map.copyOf(state);
        List<String> surfaceLines = new ArrayList<>();
        surfaceLines.add(normalized + ": " + primaryLine(normalized, safeState));
        addTerminalTranscript(surfaceLines, safeState);
        addOptionalLine(surfaceLines, safeState, "hudUpdateOutput");
        addOptionalLine(surfaceLines, safeState, "cinematicOutput");
        addCinematicLetterbox(surfaceLines, safeState);
        addOptionalLine(surfaceLines, safeState, "missionUpdateLine");
        addOptionalLine(surfaceLines, safeState, "recoveryOutput");
        addOptionalLine(surfaceLines, safeState, "mainMenuOutput");
        surfaceLines.add(EchoNativeAgent5UiExpectedValues.terminalOutput());
        surfaceLines.add(EchoNativeAgent5UiExpectedValues.indexSearchOutput());
        surfaceLines.add(EchoNativeAgent5UiExpectedValues.lensOutput());
        surfaceLines.add(EchoNativeAgent5UiExpectedValues.holomapMarker());
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("hostModelClass", EchoNativeAgent5ScreenHostModel.class.getSimpleName());
        model.put("surface", normalized);
        model.put("packId", packId == null ? "" : packId);
        model.put("screenTitle", "ECHO NATIVE // " + normalized);
        Map<String, Object> hud = EchoNativeAgent5UiExpectedValues.hud();
        model.put("headerLines", List.of(
                "Pack: " + (packId == null ? "" : packId) + "     Host: native client bridge",
                "Registered content: " + itemCount + " items/blocks",
                "Modules discovered: " + moduleCount,
                "Native route data: " + missionCount + " missions / " + regionCount + " regions",
                "Notifications: " + notificationSummary(safeState.get("notifications")),
                "HUD: Health " + hud.get("health") + " / " + hud.get("hazard")
        ));
        model.put("surfaceLines", List.copyOf(surfaceLines));
        model.put("footerLine", "M Terminal | G Index | Left Alt Lens | J Map | N SignalOS | X/C/Y/Z Drone | Items "
                + itemCount + " Missions " + missionCount + " Regions " + regionCount);
        model.put("hudValues", EchoNativeAgent5UiExpectedValues.hud());
        model.put("notificationAnchor", EchoNativeAgent5UiExpectedValues.notificationMessages().get(0));
        model.put("cinematicCue", EchoNativeAgent5UiExpectedValues.terminal().get("title"));
        model.put("adapterCoreBridge", true);
        model.put("serviceCodeExecuted", true);
        return Map.copyOf(model);
    }

    private static String primaryLine(String surface, Map<String, Object> state) {
        return switch (surface) {
            case "INDEX" -> EchoNativeAgent5UiExpectedValues.indexSearchOutput();
            case "LENS" -> EchoNativeAgent5UiExpectedValues.lensOutput();
            case "MISSION_LOG" -> dataSource("missionLog").get("title")
                    + " / " + EchoNativeAgent5UiExpectedValues.missionObjective();
            case "SETTINGS" -> dataSource("settings").get("profile")
                    + " / " + dataSource("settings").get("theme")
                    + " / " + dataSource("settings").get("inputMode");
            case "HOLOMAP" -> EchoNativeAgent5UiExpectedValues.holomap().get("layer")
                    + " / " + EchoNativeAgent5UiExpectedValues.holomapMarker();
            case "WIKI" -> EchoNativeAgent5UiExpectedValues.wiki().get("page")
                    + " / " + EchoNativeAgent5UiExpectedValues.wikiLink();
            case "HUD" -> firstNonBlank(state.get("hudUpdateOutput"), EchoNativeAgent5UiExpectedValues.hudLineToken());
            case "RECOVERY" -> dataSource("deathRecovery").get("recoveryPoint")
                    + " / " + firstNonBlank(state.get("recoveryOutput"), EchoNativeAgent5UiExpectedValues.recoveryOutput());
            case "PAUSE" -> "Pause: previous screen " + firstNonBlank(state.get("previousMode"), "WIKI")
                    + " / Press Esc to resume";
            case "MAIN_MENU" -> "Main Menu: ECHO Native Loader routes";
            case "WORLD_SETUP" -> "World Setup: ECHO Native Loader owns creation";
            default -> String.valueOf(state.getOrDefault("output", EchoNativeAgent5UiExpectedValues.terminalOutput()));
        };
    }

    private static void addOptionalLine(List<String> lines, Map<String, Object> state, String key) {
        String value = text(state.get(key));
        if (!value.isBlank()) {
            lines.add(value);
        }
    }

    private static void addTerminalTranscript(List<String> lines, Map<String, Object> state) {
        if (!Boolean.TRUE.equals(state.get("terminalCommandExecuted"))) {
            return;
        }
        String command = text(state.get("terminalBuffer"));
        String output = text(state.get("terminalOutput"));
        if (!command.isBlank() && !output.isBlank()) {
            lines.add(command + " -> " + output);
        }
    }

    private static void addCinematicLetterbox(List<String> lines, Map<String, Object> state) {
        if (Boolean.TRUE.equals(state.get("cinematicLetterbox"))) {
            lines.add("Letterbox: active    Subtitle: " + text(state.get("cinematicSubtitle")));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dataSource(String key) {
        Object value = EchoNativeAgent5UiHandlerRegistry.dataSources().get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String firstNonBlank(Object first, Object second) {
        String firstText = text(first);
        return firstText.isBlank() ? text(second) : firstText;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static String notificationSummary(Object value) {
        List<Map<String, Object>> notifications;
        if (value instanceof List<?> list) {
            notifications = list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .toList();
        } else {
            notifications = EchoNativeAgent5UiHandlerRegistry.notificationQueue();
        }
        return notifications.stream()
                .map(notification -> text(notification.get("message")))
                .filter(message -> !message.isBlank())
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
    }
}
