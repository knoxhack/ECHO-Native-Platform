package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveRouteBoundTextCommandAcceptance {
    private EchoNativeAgent5LiveRouteBoundTextCommandAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> terminal,
            Map<String, Object> index,
            Map<String, Object> routeEffectTranscript
    ) {
        boolean terminalAccepted = acceptedText(
                terminal,
                "TERMINAL",
                EchoNativeAgent5UiExpectedValues.terminalCommand(),
                EchoNativeAgent5UiExpectedValues.terminalOutput(),
                "live_ui_interaction:TERMINAL:" + EchoNativeAgent5UiExpectedValues.terminalCommand()
        );
        boolean indexAccepted = acceptedText(
                index,
                "INDEX",
                EchoNativeAgent5UiExpectedValues.indexQuery(),
                EchoNativeAgent5UiExpectedValues.indexSearchOutput(),
                "live_ui_interaction:INDEX:" + EchoNativeAgent5UiExpectedValues.indexQuery()
        );
        List<String> observedKeys = strings(routeEffectTranscript == null
                ? null
                : routeEffectTranscript.get("observedKeys"));
        boolean routeBound = routeEffectTranscript != null
                && Boolean.TRUE.equals(routeEffectTranscript.get("accepted"))
                && observedKeys.contains("M")
                && observedKeys.contains("G");
        boolean accepted = terminalAccepted && indexAccepted && routeBound;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("terminalAccepted", terminalAccepted);
        result.put("indexAccepted", indexAccepted);
        result.put("routeBound", routeBound);
        result.put("observedKeys", observedKeys);
        result.put("terminalSequence", integer(terminal == null ? null : terminal.get("sequence")));
        result.put("indexSequence", integer(index == null ? null : index.get("sequence")));
        result.put("effect", accepted
                ? "live_route_bound_text_command:accepted:terminal+index"
                : "live_route_bound_text_command:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> terminal = textSnapshot(
                "TERMINAL",
                7,
                EchoNativeAgent5UiExpectedValues.terminalCommand(),
                EchoNativeAgent5UiExpectedValues.terminalOutput(),
                "live_ui_interaction:TERMINAL:" + EchoNativeAgent5UiExpectedValues.terminalCommand()
        );
        Map<String, Object> index = textSnapshot(
                "INDEX",
                9,
                EchoNativeAgent5UiExpectedValues.indexQuery(),
                EchoNativeAgent5UiExpectedValues.indexSearchOutput(),
                "live_ui_interaction:INDEX:" + EchoNativeAgent5UiExpectedValues.indexQuery()
        );
        Map<String, Object> route = Map.of(
                "accepted", true,
                "observedKeys", List.of("M", "G", "R", "U", "B", "LEFT_ALT", "J", "K", "[", "]", "\\", "N", "ESCAPE")
        );
        Map<String, Object> accepted = assess(terminal, index, route);
        Map<String, Object> rejectedNoTerminal = assess(Map.of(), index, route);
        Map<String, Object> rejectedNoIndex = assess(terminal, Map.of(), route);
        Map<String, Object> rejectedNoRoute = assess(terminal, index, Map.of(
                "accepted", true,
                "observedKeys", List.of("LEFT_ALT", "J")
        ));
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_route_bound_text_command:accepted:terminal+index".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoTerminal.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoIndex.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRoute.get("accepted"));
        return Map.of(
                "liveRouteBoundTextCommandAcceptanceClass",
                EchoNativeAgent5LiveRouteBoundTextCommandAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoTerminal", rejectedNoTerminal,
                "rejectedNoIndex", rejectedNoIndex,
                "rejectedNoRoute", rejectedNoRoute,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static boolean acceptedText(
            Map<String, Object> value,
            String mode,
            String buffer,
            String output,
            String effect
    ) {
        return value != null
                && Boolean.TRUE.equals(value.get("accepted"))
                && mode.equals(value.get("mode"))
                && integer(value.get("characterCount")) >= buffer.length()
                && Boolean.TRUE.equals(value.get("editHandled"))
                && Boolean.TRUE.equals(value.get("submitHandled"))
                && buffer.equals(value.get("finalBuffer"))
                && output.equals(value.get("output"))
                && effect.equals(value.get("effect"));
    }

    private static Map<String, Object> textSnapshot(
            String mode,
            int sequence,
            String buffer,
            String output,
            String effect
    ) {
        return Map.of(
                "accepted", true,
                "mode", mode,
                "sequence", sequence,
                "characterCount", buffer.length() + 1,
                "editHandled", true,
                "submitHandled", true,
                "finalBuffer", buffer,
                "output", output,
                "effect", effect
        );
    }

    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(EchoNativeAgent5LiveRouteBoundTextCommandAcceptance::text).toList();
        }
        return List.of();
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
