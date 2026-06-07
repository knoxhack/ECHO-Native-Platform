package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveInputFocusRoutingAcceptance {
    private EchoNativeAgent5LiveInputFocusRoutingAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> focusManagerSmoke,
            Map<String, Object> textEditingSmoke,
            Map<String, Object> mouseActivationSmoke,
            Map<String, Object> listNavigationSmoke
    ) {
        Map<String, Object> focus = focusManagerSmoke == null ? Map.of() : focusManagerSmoke;
        Map<String, Object> editing = textEditingSmoke == null ? Map.of() : textEditingSmoke;
        Map<String, Object> mouse = mouseActivationSmoke == null ? Map.of() : mouseActivationSmoke;
        Map<String, Object> list = listNavigationSmoke == null ? Map.of() : listNavigationSmoke;
        boolean focusAccepted = Boolean.TRUE.equals(focus.get("passed"))
                && "EchoNativeAgent5FocusManagerSmoke".equals(focus.get("focusManagerSmokeClass"))
                && strings(focus, "focusOrder").equals(List.of(
                "terminal:input",
                "index:search",
                "lens:scan",
                "recovery:recover"
        ))
                && strings(focus, "ignoredReasons").containsAll(List.of("character:unfocused", "character:control"))
                && "status".equals(focus.get("terminalBuffer"))
                && "ashfall".equals(focus.get("indexBuffer"))
                && strings(focus, "activationKeys").containsAll(List.of(
                "terminalCommandExecuted",
                "indexSearchExecuted",
                "lensScanExecuted",
                "recoveryActionExecuted"
        ));
        boolean editingAccepted = Boolean.TRUE.equals(editing.get("passed"))
                && "EchoNativeAgent5TextEditingSmoke".equals(editing.get("textEditingSmokeClass"))
                && "status".equals(editing.get("terminalBuffer"))
                && "ashfall".equals(editing.get("indexBuffer"))
                && "".equals(editing.get("emptyBackspaceValue"))
                && strings(editing, "editEffects").containsAll(List.of(
                "terminal-character",
                "terminal-backspace",
                "index-character",
                "index-backspace"
        ))
                && strings(editing, "activationKeys").containsAll(List.of(
                "terminalCommandExecuted",
                "indexSearchExecuted"
        ));
        boolean mouseAccepted = Boolean.TRUE.equals(mouse.get("passed"))
                && "EchoNativeAgent5MouseActivationSmoke".equals(mouse.get("mouseActivationSmokeClass"))
                && strings(mouse, "focusPaths").containsAll(List.of(
                "terminal:input",
                "index:search",
                "lens:scan",
                "recovery:recover"
        ))
                && strings(mouse, "clickEffects").containsAll(List.of(
                "mouse:focus:terminal",
                "mouse:activate:terminal",
                "mouse:activate:index",
                "mouse:activate:lens",
                "mouse:activate:recovery"
        ))
                && strings(mouse, "executedKeys").containsAll(List.of(
                "terminalCommandExecuted",
                "indexSearchExecuted",
                "lensScanExecuted",
                "recoveryActionExecuted"
        ));
        boolean listAccepted = Boolean.TRUE.equals(list.get("passed"))
                && "EchoNativeAgent5ListNavigationSmoke".equals(list.get("listNavigationSmokeClass"))
                && strings(list, "selectedOptions").equals(List.of(
                "New Ashfall Run",
                "Settings",
                "Theme",
                "Input Mode",
                "Quit to Main Menu"
        ))
                && strings(list, "effects").containsAll(List.of(
                "list:main_menu:down",
                "list:settings:down",
                "list:pause:up"
        ));
        boolean accepted = focusAccepted && editingAccepted && mouseAccepted && listAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("focusAccepted", focusAccepted);
        result.put("editingAccepted", editingAccepted);
        result.put("mouseAccepted", mouseAccepted);
        result.put("listAccepted", listAccepted);
        result.put("terminalBuffer", String.valueOf(focus.getOrDefault("terminalBuffer", "")));
        result.put("indexBuffer", String.valueOf(focus.getOrDefault("indexBuffer", "")));
        result.put("selectedOptions", strings(list, "selectedOptions"));
        result.put("effect", accepted
                ? "live_input_focus_routing:accepted:focus/text/mouse/list"
                : "live_input_focus_routing:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
