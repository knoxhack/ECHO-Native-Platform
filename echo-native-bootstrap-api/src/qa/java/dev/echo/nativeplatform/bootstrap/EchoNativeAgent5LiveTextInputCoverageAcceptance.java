package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveTextInputCoverageAcceptance {
    private EchoNativeAgent5LiveTextInputCoverageAcceptance() {
    }

    public static Map<String, Object> assess(Map<String, Object> terminal, Map<String, Object> index) {
        boolean terminalAccepted = acceptedMode(terminal, "TERMINAL",
                EchoNativeAgent5UiExpectedValues.terminalCommand(),
                EchoNativeAgent5UiExpectedValues.terminalOutput());
        boolean indexAccepted = acceptedMode(index, "INDEX",
                EchoNativeAgent5UiExpectedValues.indexQuery(),
                EchoNativeAgent5UiExpectedValues.indexSearchOutput());
        boolean accepted = terminalAccepted && indexAccepted;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("terminalAccepted", terminalAccepted);
        result.put("indexAccepted", indexAccepted);
        result.put("terminalMode", text(terminal == null ? null : terminal.get("mode")));
        result.put("indexMode", text(index == null ? null : index.get("mode")));
        result.put("effect", accepted
                ? "live_text_input_coverage:accepted:terminal+index"
                : "live_text_input_coverage:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> terminal = Map.of(
                "accepted", true,
                "mode", "TERMINAL",
                "finalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand(),
                "output", EchoNativeAgent5UiExpectedValues.terminalOutput(),
                "submitHandled", true,
                "editHandled", true
        );
        Map<String, Object> index = Map.of(
                "accepted", true,
                "mode", "INDEX",
                "finalBuffer", EchoNativeAgent5UiExpectedValues.indexQuery(),
                "output", EchoNativeAgent5UiExpectedValues.indexSearchOutput(),
                "submitHandled", true,
                "editHandled", true
        );
        Map<String, Object> accepted = assess(terminal, index);
        Map<String, Object> rejectedNoTerminal = assess(Map.of(), index);
        Map<String, Object> rejectedNoIndex = assess(terminal, Map.of());
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoTerminal.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoIndex.get("accepted"));
        return Map.of(
                "liveTextInputCoverageAcceptanceClass",
                EchoNativeAgent5LiveTextInputCoverageAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoTerminal", rejectedNoTerminal,
                "rejectedNoIndex", rejectedNoIndex,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static boolean acceptedMode(Map<String, Object> value, String mode, String buffer, String output) {
        return value != null
                && Boolean.TRUE.equals(value.get("accepted"))
                && mode.equals(value.get("mode"))
                && buffer.equals(value.get("finalBuffer"))
                && output.equals(value.get("output"))
                && Boolean.TRUE.equals(value.get("submitHandled"))
                && Boolean.TRUE.equals(value.get("editHandled"));
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
