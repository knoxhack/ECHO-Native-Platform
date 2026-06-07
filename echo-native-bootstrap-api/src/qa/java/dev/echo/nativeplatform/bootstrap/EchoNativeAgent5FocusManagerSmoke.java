package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5FocusManagerSmoke {
    private EchoNativeAgent5FocusManagerSmoke() {
    }

    public static Map<String, Object> capture() {
        ArrayList<String> focusOrder = new ArrayList<>();
        ArrayList<String> typedEffects = new ArrayList<>();
        ArrayList<String> ignoredReasons = new ArrayList<>();
        ArrayList<String> activationKeys = new ArrayList<>();
        ArrayList<String> renderedFocusLines = new ArrayList<>();

        String terminalFocus = EchoNativeAgent5UiActionRouter.focusPath("TERMINAL", "WIKI");
        String indexFocus = EchoNativeAgent5UiActionRouter.focusPath("INDEX", "WIKI");
        String lensFocus = EchoNativeAgent5UiActionRouter.focusPath("LENS", "WIKI");
        String recoveryFocus = EchoNativeAgent5UiActionRouter.focusPath("RECOVERY", "WIKI");
        focusOrder.addAll(List.of(terminalFocus, indexFocus, lensFocus, recoveryFocus));

        Map<String, Object> wrongTerminalFocus = EchoNativeAgent5UiActionRouter.routeCharacter(
                "TERMINAL",
                indexFocus,
                "",
                "",
                's'
        );
        ignoredReasons.add(String.valueOf(wrongTerminalFocus.get("reason")));
        Map<String, Object> controlCharacter = EchoNativeAgent5UiActionRouter.routeCharacter(
                "INDEX",
                indexFocus,
                "",
                "",
                '\n'
        );
        ignoredReasons.add(String.valueOf(controlCharacter.get("reason")));

        String terminalBuffer = "";
        for (char character : "status".toCharArray()) {
            Map<String, Object> typed = EchoNativeAgent5UiActionRouter.routeCharacter(
                    "TERMINAL",
                    terminalFocus,
                    terminalBuffer,
                    "",
                    character
            );
            terminalBuffer = String.valueOf(typed.get("value"));
            typedEffects.add(String.valueOf(typed.get("effect")));
        }
        Map<String, Object> terminalAction = EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                "focusedControl", terminalFocus,
                "terminalBuffer", terminalBuffer
        ));
        activationKeys.add(String.valueOf(terminalAction.get("executedKey")));
        renderedFocusLines.addAll(lines(EchoNativeAgent5UiHandlerRegistry.renderSurface("TERMINAL", Map.of(
                "focusedControl", terminalFocus,
                "mouseRouted", true,
                "terminalBuffer", terminalBuffer,
                "terminalOutput", terminalAction.get("output"),
                "terminalCommandExecuted", true
        ))));

        String indexBuffer = "";
        for (char character : "ashfall".toCharArray()) {
            Map<String, Object> typed = EchoNativeAgent5UiActionRouter.routeCharacter(
                    "INDEX",
                    indexFocus,
                    "",
                    indexBuffer,
                    character
            );
            indexBuffer = String.valueOf(typed.get("value"));
            typedEffects.add(String.valueOf(typed.get("effect")));
        }
        Map<String, Object> indexAction = EchoNativeAgent5UiActionRouter.activate("INDEX", Map.of(
                "focusedControl", indexFocus,
                "indexBuffer", indexBuffer
        ));
        activationKeys.add(String.valueOf(indexAction.get("executedKey")));
        renderedFocusLines.addAll(lines(EchoNativeAgent5UiHandlerRegistry.renderSurface("INDEX", Map.of(
                "focusedControl", indexFocus,
                "mouseRouted", true,
                "indexBuffer", indexBuffer,
                "indexOutput", indexAction.get("output"),
                "indexSearchExecuted", true
        ))));

        Map<String, Object> lensAction = EchoNativeAgent5UiActionRouter.activate("LENS", Map.of(
                "focusedControl", lensFocus
        ));
        activationKeys.add(String.valueOf(lensAction.get("executedKey")));
        renderedFocusLines.addAll(lines(EchoNativeAgent5UiHandlerRegistry.renderSurface("LENS", Map.of(
                "focusedControl", lensFocus,
                "mouseRouted", true,
                "lensOutput", lensAction.get("output"),
                "lensScanExecuted", true
        ))));

        Map<String, Object> recoveryAction = EchoNativeAgent5UiActionRouter.activate("RECOVERY", Map.of(
                "focusedControl", recoveryFocus
        ));
        activationKeys.add(String.valueOf(recoveryAction.get("executedKey")));
        renderedFocusLines.addAll(lines(EchoNativeAgent5UiHandlerRegistry.renderSurface("RECOVERY", Map.of(
                "focusedControl", recoveryFocus,
                "mouseRouted", true,
                "recoveryOutput", recoveryAction.get("output"),
                "recoveryActionExecuted", true
        ))));

        boolean passed = focusOrder.equals(List.of("terminal:input", "index:search", "lens:scan", "recovery:recover"))
                && ignoredReasons.containsAll(List.of("character:unfocused", "character:control"))
                && "status".equals(terminalBuffer)
                && "ashfall".equals(indexBuffer)
                && typedEffects.stream().filter("terminal-character"::equals).count() == 6
                && typedEffects.stream().filter("index-character"::equals).count() == 7
                && activationKeys.containsAll(List.of(
                        "terminalCommandExecuted",
                        "indexSearchExecuted",
                        "lensScanExecuted",
                        "recoveryActionExecuted"
                ))
                && renderedFocusLines.stream().anyMatch(line -> line.contains("terminal:input ready"))
                && renderedFocusLines.stream().anyMatch(line -> line.contains("index:search ready"))
                && renderedFocusLines.stream().anyMatch(line -> line.contains("lens:scan ready"))
                && renderedFocusLines.stream().anyMatch(line -> line.contains("recovery:recover ready"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("focusManagerSmokeClass", EchoNativeAgent5FocusManagerSmoke.class.getSimpleName());
        smoke.put("focusOrder", List.copyOf(focusOrder));
        smoke.put("typedEffects", List.copyOf(typedEffects));
        smoke.put("ignoredReasons", List.copyOf(ignoredReasons));
        smoke.put("activationKeys", List.copyOf(activationKeys));
        smoke.put("renderedFocusLines", List.copyOf(renderedFocusLines));
        smoke.put("terminalBuffer", terminalBuffer);
        smoke.put("indexBuffer", indexBuffer);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
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
