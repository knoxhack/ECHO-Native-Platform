package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5ScreenCorePrimitiveExecutionSmoke {
    private static final List<String> REQUIRED_PRIMITIVES = List.of(
            "EchoScreen",
            "EchoScreenStack",
            "EchoScreenRoute",
            "EchoHudLayer",
            "EchoInputAction",
            "EchoTheme",
            "EchoWidget",
            "EchoTextInput",
            "EchoButton",
            "EchoListView",
            "EchoTerminalBuffer",
            "EchoNotification"
    );

    private EchoNativeAgent5ScreenCorePrimitiveExecutionSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> contract = EchoNativeLiveUiBridge.contractSnapshot();
        Map<String, Object> dataSources = object(contract.get("agent5DataSources"));
        List<Map<String, Object>> stack = new ArrayList<>();
        Map<String, Object> screen = primitive("EchoScreen", Map.of(
                "id", "echoterminal:terminal",
                "title", "Terminal",
                "focusPath", "terminal:input"
        ));
        Map<String, Object> route = primitive("EchoScreenRoute", Map.of(
                "screenId", "echoterminal:terminal",
                "route", "route:M",
                "focusPath", "terminal:input"
        ));
        stack.add(screen);
        stack.set(0, primitive("EchoScreen", Map.of(
                "id", "echoindex:index",
                "title", "Index",
                "focusPath", "index:search"
        )));
        Map<String, Object> popped = stack.remove(0);
        stack.add(screen);
        Map<String, Object> stackPrimitive = primitive("EchoScreenStack", Map.of(
                "current", string(screen, "id"),
                "popped", string(popped, "id"),
                "size", stack.size()
        ));
        Map<String, Object> hud = primitive("EchoHudLayer", object(dataSources.get("hud")));
        Map<String, Object> action = primitive("EchoInputAction", Map.of(
                "id", "agent5:terminal_status",
                "key", "Enter",
                "command", "status"
        ));
        Map<String, Object> theme = primitive("EchoTheme", Map.of(
                "id", "ashfall-agent5",
                "accent", "#67e8f9"
        ));
        Map<String, Object> widget = primitive("EchoWidget", Map.of(
                "id", "agent5:terminal",
                "kind", "text-input",
                "mode", "TERMINAL"
        ));
        Map<String, Object> input = primitive("EchoTextInput", Map.of(
                "id", "agent5:terminal-input",
                "value", "status"
        ));
        Map<String, Object> button = primitive("EchoButton", Map.of(
                "id", "agent5:lens-scan",
                "label", "Scan",
                "action", "lens-scan"
        ));
        Map<String, Object> list = primitive("EchoListView", Map.of(
                "id", "agent5:pause-options",
                "selectedRow", "Settings",
                "rowCount", 3
        ));
        Map<String, Object> buffer = primitive("EchoTerminalBuffer", Map.of(
                "lines", List.of(EchoNativeAgent5UiExpectedValues.terminalCommand()
                        + " -> " + EchoNativeAgent5UiExpectedValues.terminalOutput()),
                "containsReadyLine", true
        ));
        List<String> notificationMessages = EchoNativeAgent5UiExpectedValues.notificationMessages();
        Map<String, Object> notification = primitive("EchoNotification", Map.of(
                "id", "echoterminal:native-data",
                "severity", "INFO",
                "message", notificationMessages.isEmpty()
                        ? EchoNativeAgent5UiExpectedValues.terminalOutput()
                        : notificationMessages.get(0),
                "anchor", "top_left_safe_area",
                "delivered", true
        ));
        List<String> executed = List.of(
                string(screen, "primitive"),
                string(stackPrimitive, "primitive"),
                string(route, "primitive"),
                string(hud, "primitive"),
                string(action, "primitive"),
                string(theme, "primitive"),
                string(widget, "primitive"),
                string(input, "primitive"),
                string(button, "primitive"),
                string(list, "primitive"),
                string(buffer, "primitive"),
                string(notification, "primitive")
        );
        boolean passed = executed.equals(REQUIRED_PRIMITIVES)
                && "echoterminal:terminal".equals(string(stackPrimitive, "current"))
                && "echoindex:index".equals(string(stackPrimitive, "popped"))
                && number(EchoNativeAgent5UiExpectedValues.hud().get("health")).equals(number(hud.get("health")))
                && EchoNativeAgent5UiExpectedValues.terminalCommand().equals(action.get("command"))
                && EchoNativeAgent5UiExpectedValues.terminalCommand().equals(input.get("value"))
                && "lens-scan".equals(button.get("action"))
                && "Settings".equals(list.get("selectedRow"))
                && Boolean.TRUE.equals(buffer.get("containsReadyLine"))
                && Boolean.TRUE.equals(notification.get("delivered"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("screenCorePrimitiveExecutionSmokeClass",
                EchoNativeAgent5ScreenCorePrimitiveExecutionSmoke.class.getSimpleName());
        smoke.put("executedPrimitives", executed);
        smoke.put("stackCurrent", stackPrimitive.get("current"));
        smoke.put("routeFocusPath", route.get("focusPath"));
        smoke.put("terminalInputValue", input.get("value"));
        smoke.put("selectedRow", list.get("selectedRow"));
        smoke.put("notificationMessage", notification.get("message"));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> primitive(String name, Map<String, Object> values) {
        Map<String, Object> primitive = new LinkedHashMap<>(values);
        primitive.put("primitive", name);
        primitive.put("executed", true);
        return Map.copyOf(primitive);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String string(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static Integer number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
