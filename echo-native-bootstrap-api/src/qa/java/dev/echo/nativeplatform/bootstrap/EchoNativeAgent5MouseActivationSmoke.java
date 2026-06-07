package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5MouseActivationSmoke {
    private EchoNativeAgent5MouseActivationSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        Map<String, Object> terminal = object(dataSources.get("terminal"));
        Map<String, Object> index = object(dataSources.get("index"));

        Map<String, Object> focusOnlyClick = EchoNativeAgent5UiActionRouter.routeMouseClick(
                "TERMINAL",
                "WIKI",
                Map.of()
        );
        Map<String, Object> terminalClick = EchoNativeAgent5UiActionRouter.routeMouseClick(
                "TERMINAL",
                "WIKI",
                Map.of("terminalBuffer", terminal.get("command"))
        );
        Map<String, Object> indexClick = EchoNativeAgent5UiActionRouter.routeMouseClick(
                "INDEX",
                "WIKI",
                Map.of("indexBuffer", index.get("query"))
        );
        Map<String, Object> lensClick = EchoNativeAgent5UiActionRouter.routeMouseClick("LENS", "WIKI", Map.of());
        Map<String, Object> recoveryClick = EchoNativeAgent5UiActionRouter.routeMouseClick("RECOVERY", "WIKI", Map.of());

        List<String> focusPaths = List.of(
                String.valueOf(focusOnlyClick.get("focusedControl")),
                String.valueOf(terminalClick.get("focusedControl")),
                String.valueOf(indexClick.get("focusedControl")),
                String.valueOf(lensClick.get("focusedControl")),
                String.valueOf(recoveryClick.get("focusedControl"))
        );
        List<String> clickEffects = List.of(
                String.valueOf(focusOnlyClick.get("effect")),
                String.valueOf(terminalClick.get("effect")),
                String.valueOf(indexClick.get("effect")),
                String.valueOf(lensClick.get("effect")),
                String.valueOf(recoveryClick.get("effect"))
        );
        List<String> executedKeys = List.of(
                String.valueOf(terminalClick.get("executedKey")),
                String.valueOf(indexClick.get("executedKey")),
                String.valueOf(lensClick.get("executedKey")),
                String.valueOf(recoveryClick.get("executedKey"))
        );

        ArrayList<String> renderedLines = new ArrayList<>();
        renderedLines.addAll(lines(EchoNativeAgent5SurfaceRenderer.render(
                "TERMINAL",
                stateWithAction("terminalBuffer", terminal.get("command"), terminalClick),
                dataSources
        )));
        renderedLines.addAll(lines(EchoNativeAgent5SurfaceRenderer.render(
                "INDEX",
                stateWithAction("indexBuffer", index.get("query"), indexClick),
                dataSources
        )));
        renderedLines.addAll(lines(EchoNativeAgent5SurfaceRenderer.render("LENS", stateWithAction(null, null, lensClick), dataSources)));
        renderedLines.addAll(lines(EchoNativeAgent5SurfaceRenderer.render(
                "RECOVERY",
                stateWithAction(null, null, recoveryClick),
                dataSources
        )));

        boolean passed = Boolean.TRUE.equals(focusOnlyClick.get("handled"))
                && !focusOnlyClick.containsKey("executedKey")
                && focusPaths.containsAll(List.of(
                        "terminal:input",
                        "index:search",
                        "lens:scan",
                        "recovery:recover"
                ))
                && clickEffects.containsAll(List.of(
                        "mouse:focus:terminal",
                        "mouse:activate:terminal",
                        "mouse:activate:index",
                        "mouse:activate:lens",
                        "mouse:activate:recovery"
                ))
                && executedKeys.containsAll(List.of(
                        "terminalCommandExecuted",
                        "indexSearchExecuted",
                        "lensScanExecuted",
                        "recoveryActionExecuted"
                ))
                && renderedLines.stream().anyMatch(line -> line.contains(String.valueOf(terminal.get("readyLine"))))
                && renderedLines.stream().anyMatch(line -> line.contains(
                        EchoNativeAgent5UiExpectedValues.indexSearchOutput()))
                && renderedLines.stream().anyMatch(line -> line.contains("scan locked"))
                && renderedLines.stream().anyMatch(line -> line.contains("Status: RECOVERED"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("mouseActivationSmokeClass", EchoNativeAgent5MouseActivationSmoke.class.getSimpleName());
        smoke.put("serviceCodeExecuted", true);
        smoke.put("adapterCoreBridge", true);
        smoke.put("focusPaths", focusPaths);
        smoke.put("clickEffects", clickEffects);
        smoke.put("executedKeys", executedKeys);
        smoke.put("renderedLines", List.copyOf(renderedLines));
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> stateWithAction(String bufferKey, Object bufferValue, Map<String, Object> click) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("focusedControl", click.get("focusedControl"));
        state.put("mouseRouted", click.get("mouseRouted"));
        if (bufferKey != null) {
            state.put(bufferKey, bufferValue);
        }
        if (click.containsKey("outputKey")) {
            state.put(String.valueOf(click.get("outputKey")), click.get("output"));
        }
        if (click.containsKey("executedKey")) {
            state.put(String.valueOf(click.get("executedKey")), true);
        }
        return Map.copyOf(state);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
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
