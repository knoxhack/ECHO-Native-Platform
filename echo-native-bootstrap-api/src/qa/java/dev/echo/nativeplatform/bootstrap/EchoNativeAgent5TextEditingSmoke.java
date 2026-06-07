package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5TextEditingSmoke {
    private EchoNativeAgent5TextEditingSmoke() {
    }

    public static Map<String, Object> capture() {
        ArrayList<String> editEffects = new ArrayList<>();
        ArrayList<String> activationKeys = new ArrayList<>();
        ArrayList<String> renderedLines = new ArrayList<>();

        String terminalFocus = EchoNativeAgent5UiActionRouter.focusPath("TERMINAL", "WIKI");
        String terminalBuffer = type("TERMINAL", terminalFocus, "statuz", "", editEffects);
        Map<String, Object> terminalBackspace = EchoNativeAgent5UiActionRouter.routeEditKey(
                "BACKSPACE",
                "TERMINAL",
                terminalFocus,
                terminalBuffer,
                ""
        );
        terminalBuffer = String.valueOf(terminalBackspace.get("value"));
        editEffects.add(String.valueOf(terminalBackspace.get("effect")));
        terminalBuffer = type("TERMINAL", terminalFocus, "s", terminalBuffer, editEffects);
        Map<String, Object> terminalAction = EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                "focusedControl", terminalFocus,
                "terminalBuffer", terminalBuffer
        ));
        activationKeys.add(String.valueOf(terminalAction.get("executedKey")));
        renderedLines.addAll(lines(EchoNativeAgent5UiHandlerRegistry.renderSurface("TERMINAL", Map.of(
                "focusedControl", terminalFocus,
                "mouseRouted", true,
                "terminalBuffer", terminalBuffer,
                "terminalOutput", terminalAction.get("output"),
                "terminalCommandExecuted", true
        ))));

        String indexFocus = EchoNativeAgent5UiActionRouter.focusPath("INDEX", "WIKI");
        String indexBuffer = type("INDEX", indexFocus, "ashx", "", editEffects);
        Map<String, Object> indexBackspace = EchoNativeAgent5UiActionRouter.routeEditKey(
                "BACKSPACE",
                "INDEX",
                indexFocus,
                "",
                indexBuffer
        );
        indexBuffer = String.valueOf(indexBackspace.get("value"));
        editEffects.add(String.valueOf(indexBackspace.get("effect")));
        indexBuffer = type("INDEX", indexFocus, "fall", indexBuffer, editEffects);
        Map<String, Object> indexAction = EchoNativeAgent5UiActionRouter.activate("INDEX", Map.of(
                "focusedControl", indexFocus,
                "indexBuffer", indexBuffer
        ));
        activationKeys.add(String.valueOf(indexAction.get("executedKey")));
        renderedLines.addAll(lines(EchoNativeAgent5UiHandlerRegistry.renderSurface("INDEX", Map.of(
                "focusedControl", indexFocus,
                "mouseRouted", true,
                "indexBuffer", indexBuffer,
                "indexOutput", indexAction.get("output"),
                "indexSearchExecuted", true
        ))));

        Map<String, Object> emptyBackspace = EchoNativeAgent5UiActionRouter.routeEditKey(
                "BACKSPACE",
                "TERMINAL",
                terminalFocus,
                "",
                ""
        );

        boolean passed = EchoNativeAgent5UiExpectedValues.terminalCommand().equals(terminalBuffer)
                && EchoNativeAgent5UiExpectedValues.indexQuery().equals(indexBuffer)
                && "".equals(emptyBackspace.get("value"))
                && editEffects.containsAll(List.of("terminal-character", "terminal-backspace", "index-character", "index-backspace"))
                && activationKeys.containsAll(List.of("terminalCommandExecuted", "indexSearchExecuted"))
                && renderedLines.stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.terminalCommand()
                        + " -> " + EchoNativeAgent5UiExpectedValues.terminalOutput()))
                && renderedLines.stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.indexQuery()
                        + " -> ") && line.contains("index result(s):"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("textEditingSmokeClass", EchoNativeAgent5TextEditingSmoke.class.getSimpleName());
        smoke.put("terminalBuffer", terminalBuffer);
        smoke.put("indexBuffer", indexBuffer);
        smoke.put("emptyBackspaceValue", emptyBackspace.get("value"));
        smoke.put("editEffects", List.copyOf(editEffects));
        smoke.put("activationKeys", List.copyOf(activationKeys));
        smoke.put("renderedLines", List.copyOf(renderedLines));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static String type(
            String mode,
            String focus,
            String value,
            String buffer,
            List<String> effects
    ) {
        String next = buffer;
        for (char character : value.toCharArray()) {
            Map<String, Object> typed = EchoNativeAgent5UiActionRouter.routeCharacter(
                    mode,
                    focus,
                    "TERMINAL".equals(mode) ? next : "",
                    "INDEX".equals(mode) ? next : "",
                    character
            );
            next = String.valueOf(typed.get("value"));
            effects.add(String.valueOf(typed.get("effect")));
        }
        return next;
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> model) {
        Object value = model.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
