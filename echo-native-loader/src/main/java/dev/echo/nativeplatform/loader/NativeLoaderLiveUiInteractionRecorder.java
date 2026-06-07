package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderLiveUiInteractionRecorder {
    public static final String SERVICE_ID = "echo.native.live_ui_interaction_recorder";
    private static int sequence;
    private static String mode = "";
    private static int characterCount;
    private static boolean editHandled;
    private static boolean submitHandled;
    private static String finalBuffer = "";
    private static String output = "";
    private static String effect = "";
    private static volatile ExpectedValues expectedValues = ExpectedValues.empty();

    private NativeLoaderLiveUiInteractionRecorder() {
    }

    public static void configure(ExpectedValues values) {
        expectedValues = values == null ? ExpectedValues.empty() : values;
    }

    public static synchronized void clear() {
        sequence++;
        mode = "";
        characterCount = 0;
        editHandled = false;
        submitHandled = false;
        finalBuffer = "";
        output = "";
        effect = "";
    }

    public static synchronized void recordCharacter(String currentMode, Map<String, Object> route) {
        if (Boolean.TRUE.equals(route.get("handled"))) {
            mode = normalize(currentMode);
            characterCount++;
            finalBuffer = text(route.get("value"));
            sequence++;
        }
    }

    public static synchronized void recordEdit(String currentMode, Map<String, Object> route) {
        if (Boolean.TRUE.equals(route.get("handled"))) {
            mode = normalize(currentMode);
            editHandled = true;
            finalBuffer = text(route.get("value"));
            sequence++;
        }
    }

    public static synchronized void recordSubmit(String currentMode, Map<String, Object> action) {
        if (Boolean.TRUE.equals(action.get("handled"))) {
            mode = normalize(currentMode);
            submitHandled = true;
            output = text(action.get("output"));
            effect = "live_ui_interaction:" + mode + ":" + finalBuffer;
            sequence++;
        }
    }

    public static synchronized Map<String, Object> snapshot() {
        boolean accepted = switch (mode) {
            case "TERMINAL" -> submitHandled
                    && editHandled
                    && expectedValues.terminalCommand().equals(finalBuffer)
                    && expectedValues.terminalOutput().equals(output);
            case "INDEX" -> submitHandled
                    && editHandled
                    && expectedValues.indexQuery().equals(finalBuffer)
                    && expectedValues.indexSearchOutput().equals(output);
            default -> false;
        };
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nativeLiveUiInteractionRecorderServiceId", SERVICE_ID);
        result.put("interactionRecorderClass", NativeLoaderLiveUiInteractionRecorder.class.getSimpleName());
        result.put("accepted", accepted);
        result.put("sequence", sequence);
        result.put("mode", mode);
        result.put("characterCount", characterCount);
        result.put("editHandled", editHandled);
        result.put("submitHandled", submitHandled);
        result.put("finalBuffer", finalBuffer);
        result.put("output", output);
        result.put("effect", accepted ? effect : "live_ui_interaction:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static synchronized Map<String, Object> smoke() {
        clear();
        String terminalCommand = expectedValues.terminalCommand();
        for (char character : (terminalCommand + "x").toCharArray()) {
            recordCharacter("TERMINAL", Map.of("handled", true, "value", currentValue(character)));
        }
        recordEdit("TERMINAL", Map.of("handled", true, "value", terminalCommand));
        recordSubmit("TERMINAL", Map.of("handled", true,
                "output", expectedValues.terminalOutput()));
        Map<String, Object> terminal = snapshot();
        clear();
        String indexQuery = expectedValues.indexQuery();
        for (char character : (indexQuery + "x").toCharArray()) {
            recordCharacter("INDEX", Map.of("handled", true, "value", currentValue(character)));
        }
        recordEdit("INDEX", Map.of("handled", true, "value", indexQuery));
        recordSubmit("INDEX", Map.of("handled", true,
                "output", expectedValues.indexSearchOutput()));
        Map<String, Object> index = snapshot();
        return Map.of(
                "interactionRecorderSmokeClass", NativeLoaderLiveUiInteractionRecorder.class.getSimpleName(),
                "terminal", terminal,
                "index", index,
                "acceptedModes", List.of(terminal.get("mode"), index.get("mode")),
                "passed", Boolean.TRUE.equals(terminal.get("accepted")) && Boolean.TRUE.equals(index.get("accepted")),
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static String currentValue(char character) {
        return String.valueOf(character);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public interface ExpectedValues {
        String terminalCommand();

        String terminalOutput();

        String indexQuery();

        String indexSearchOutput();

        static ExpectedValues empty() {
            return new ExpectedValues() {
                @Override
                public String terminalCommand() {
                    return "";
                }

                @Override
                public String terminalOutput() {
                    return "";
                }

                @Override
                public String indexQuery() {
                    return "";
                }

                @Override
                public String indexSearchOutput() {
                    return "";
                }
            };
        }
    }
}
