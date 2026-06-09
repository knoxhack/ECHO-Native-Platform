package dev.echo.nativeplatform.bootstrap;

import java.util.List;
import java.util.Map;

final class EchoNativeAgent5UiExpectedValues {
    private EchoNativeAgent5UiExpectedValues() {
    }

    static Map<String, Object> terminal() {
        Map<String, Object> terminal = dataSource("terminal");
        return Map.of(
                "title", text(terminal.get("title")),
                "prompt", text(terminal.get("prompt"))
        );
    }

    static Map<String, Object> terminalState() {
        return Map.of(
                "title", terminal().get("title"),
                "command", terminalCommand(),
                "output", terminalOutput(),
                "focusedControl", "terminal:input",
                "mouseRouted", true,
                "terminalBuffer", terminalCommand(),
                "terminalOutput", terminalOutput(),
                "terminalCommandExecuted", true
        );
    }

    static String terminalCommand() {
        return text(dataSource("terminal").get("command"));
    }

    static String terminalOutput() {
        return text(dataSource("terminal").get("readyLine"));
    }

    static Map<String, Object> indexState() {
        return Map.of(
                "query", indexQuery(),
                "result", indexOutput(),
                "output", indexSearchOutput(),
                "focusedControl", "index:search",
                "mouseRouted", true,
                "indexBuffer", indexQuery(),
                "indexOutput", indexSearchOutput(),
                "indexSearchExecuted", true
        );
    }

    static String indexQuery() {
        return text(dataSource("index").get("query"));
    }

    static String indexOutput() {
        return text(dataSource("index").get("result"));
    }

    static String indexSearchOutput() {
        return text(EchoNativeAgent5UiHandlerRegistry.searchIndex(indexQuery()).get("output"));
    }

    static Map<String, Object> lens() {
        return Map.of("target", lensTarget(), "summary", lensOutput());
    }

    static Map<String, Object> lensState() {
        return Map.of(
                "target", lensTarget(),
                "summary", lensOutput(),
                "result", lensOutput(),
                "focusedControl", "lens:scan",
                "mouseRouted", true,
                "lensOutput", lensOutput(),
                "lensScanExecuted", true
        );
    }

    static String lensTarget() {
        return text(dataSource("lens").get("target"));
    }

    static String lensOutput() {
        return text(dataSource("lens").get("result"));
    }

    static Map<String, Object> holomap() {
        Map<String, Object> holomap = dataSource("holomap");
        return Map.of("layer", text(holomap.get("layer")), "marker", holomapMarker());
    }

    static String holomapMarker() {
        return text(dataSource("holomap").get("marker"));
    }

    static Map<String, Object> wiki() {
        Map<String, Object> wiki = dataSource("wiki");
        return Map.of("guide", text(wiki.get("guide")), "page", text(wiki.get("page")));
    }

    static String wikiLink() {
        return text(dataSource("wiki").get("link"));
    }

    static Map<String, Object> hud() {
        Map<String, Object> hud = dataSource("hud");
        return Map.of(
                "health", hud.get("health"),
                "hazard", text(hud.get("hazard")),
                "mission", missionObjective()
        );
    }

    static String hudLineToken() {
        return "HUD: Health " + hud().get("health");
    }

    static int hudUpdatedHealth() {
        return Math.max(0, number(hud().get("health")) - 7);
    }

    static String hudOverlayEffect() {
        return "hud_overlay_end_to_end:data_backed:" + hudUpdatedHealth();
    }

    static String missionObjective() {
        return text(dataSource("missionLog").get("objective"));
    }

    static Map<String, Object> recoveryState() {
        return Map.of(
                "status", recoveryOutput(),
                "focusedControl", "recovery:recover",
                "mouseRouted", true,
                "recoveryOutput", recoveryOutput()
        );
    }

    static String recoveryOutput() {
        return text(EchoNativeAgent5UiHandlerRegistry.recover().get("output"));
    }

    static List<String> notificationMessages() {
        return notifications().stream()
                .map(notification -> text(notification.get("message")))
                .toList();
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dataSource(String key) {
        Object value = EchoNativeAgent5UiHandlerRegistry.dataSources().get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> notifications() {
        Object value = EchoNativeAgent5UiHandlerRegistry.dataSources().get("notifications");
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .toList();
        }
        return List.of();
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
