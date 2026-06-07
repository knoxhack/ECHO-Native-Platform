package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5InitialFocusSmoke {
    private EchoNativeAgent5InitialFocusSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> terminalFocus = EchoNativeAgent5UiActionRouter.routeInitialFocus("TERMINAL", "WIKI");
        Map<String, Object> indexFocus = EchoNativeAgent5UiActionRouter.routeInitialFocus("INDEX", "WIKI");
        Map<String, Object> lensFocus = EchoNativeAgent5UiActionRouter.routeInitialFocus("LENS", "WIKI");
        Map<String, Object> recoveryFocus = EchoNativeAgent5UiActionRouter.routeInitialFocus("RECOVERY", "WIKI");

        Map<String, Object> terminalTyped = EchoNativeAgent5UiActionRouter.routeCharacter(
                "TERMINAL",
                String.valueOf(terminalFocus.get("focusedControl")),
                "statu",
                "",
                's'
        );
        Map<String, Object> indexTyped = EchoNativeAgent5UiActionRouter.routeCharacter(
                "INDEX",
                String.valueOf(indexFocus.get("focusedControl")),
                "",
                "ashfal",
                'l'
        );
        Map<String, Object> lensAction = EchoNativeAgent5UiActionRouter.activate("LENS", Map.of(
                "focusedControl", lensFocus.get("focusedControl")
        ));
        Map<String, Object> recoveryAction = EchoNativeAgent5UiActionRouter.activate("RECOVERY", Map.of(
                "focusedControl", recoveryFocus.get("focusedControl")
        ));
        Map<String, Object> renderedTerminal = EchoNativeAgent5SurfaceRenderer.render("TERMINAL", Map.of(
                "focusedControl", terminalFocus.get("focusedControl"),
                "initialFocusRouted", true
        ), EchoNativeAgent5UiHandlerRegistry.dataSources());
        Map<String, Object> renderedLens = EchoNativeAgent5SurfaceRenderer.render("LENS", Map.of(
                "focusedControl", lensFocus.get("focusedControl"),
                "initialFocusRouted", true,
                "lensOutput", lensAction.get("output"),
                "lensScanExecuted", true
        ), EchoNativeAgent5UiHandlerRegistry.dataSources());

        List<String> focusPaths = List.of(
                String.valueOf(terminalFocus.get("focusedControl")),
                String.valueOf(indexFocus.get("focusedControl")),
                String.valueOf(lensFocus.get("focusedControl")),
                String.valueOf(recoveryFocus.get("focusedControl"))
        );
        List<String> effects = List.of(
                String.valueOf(terminalFocus.get("effect")),
                String.valueOf(indexFocus.get("effect")),
                String.valueOf(lensFocus.get("effect")),
                String.valueOf(recoveryFocus.get("effect"))
        );
        boolean passed = focusPaths.equals(List.of("terminal:input", "index:search", "lens:scan", "recovery:recover"))
                && effects.equals(List.of("focus:initial:terminal", "focus:initial:index", "focus:initial:lens", "focus:initial:recovery"))
                && "status".equals(terminalTyped.get("value"))
                && "ashfall".equals(indexTyped.get("value"))
                && "lensScanExecuted".equals(lensAction.get("executedKey"))
                && "recoveryActionExecuted".equals(recoveryAction.get("executedKey"))
                && lines(renderedTerminal).stream().anyMatch(line -> line.contains("terminal:input ready"))
                && lines(renderedLens).stream().anyMatch(line -> line.contains("lens:scan ready"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("initialFocusSmokeClass", EchoNativeAgent5InitialFocusSmoke.class.getSimpleName());
        smoke.put("serviceCodeExecuted", true);
        smoke.put("adapterCoreBridge", true);
        smoke.put("focusPaths", focusPaths);
        smoke.put("effects", effects);
        smoke.put("terminalBuffer", terminalTyped.get("value"));
        smoke.put("indexBuffer", indexTyped.get("value"));
        smoke.put("executedKeys", List.of(lensAction.get("executedKey"), recoveryAction.get("executedKey")));
        smoke.put("renderedLines", List.of(lines(renderedTerminal), lines(renderedLens)).stream().flatMap(List::stream).toList());
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> rendered) {
        Object value = rendered.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
