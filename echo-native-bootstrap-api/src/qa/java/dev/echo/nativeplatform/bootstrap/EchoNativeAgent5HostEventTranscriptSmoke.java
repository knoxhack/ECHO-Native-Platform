package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5HostEventTranscriptSmoke {
    private EchoNativeAgent5HostEventTranscriptSmoke() {
    }

    public static Map<String, Object> capture(
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        ArrayList<String> events = new ArrayList<>();
        ArrayList<String> renderedLines = new ArrayList<>();

        Map<String, Object> terminalRoute = EchoNativeAgent5UiActionRouter.routeKey("M", "WIKI", "WIKI");
        events.add("key:M->" + terminalRoute.get("destinationMode"));
        Map<String, Object> terminalFocus = EchoNativeAgent5UiActionRouter.routeInitialFocus("TERMINAL", "WIKI");
        String terminalBuffer = "";
        for (char character : EchoNativeAgent5UiExpectedValues.terminalCommand().toCharArray()) {
            Map<String, Object> typed = EchoNativeAgent5UiActionRouter.routeCharacter(
                    "TERMINAL",
                    String.valueOf(terminalFocus.get("focusedControl")),
                    terminalBuffer,
                    "",
                    character
            );
            terminalBuffer = String.valueOf(typed.get("value"));
        }
        events.add("text:terminal:" + terminalBuffer);
        Map<String, Object> terminalAction = EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                "focusedControl", terminalFocus.get("focusedControl"),
                "terminalBuffer", terminalBuffer
        ));
        Map<String, Object> terminalMutation = EchoNativeBootstrapMain.executeNativeTerminalCommandFromUi(
                terminalBuffer,
                String.valueOf(terminalAction.get("output"))
        );
        boolean terminalRuntimeMutationAccepted = runtimeMutationAccepted(
                terminalMutation,
                "native.ui.terminal_command",
                "command_execution"
        );
        events.add("enter:terminal:" + terminalAction.get("executedKey"));
        renderedLines.addAll(renderSurface("TERMINAL", state(
                terminalFocus,
                "terminalBuffer", terminalBuffer,
                "terminalOutput", String.valueOf(terminalAction.get("output")),
                "terminalCommandExecuted", true
        ), packId, moduleCount, itemCount, missionCount, regionCount));

        Map<String, Object> indexRoute = EchoNativeAgent5UiActionRouter.routeKey("G", "TERMINAL", "WIKI");
        events.add("key:G->" + indexRoute.get("destinationMode"));
        Map<String, Object> indexFocus = EchoNativeAgent5UiActionRouter.routeInitialFocus("INDEX", "WIKI");
        String indexBuffer = "";
        for (char character : EchoNativeAgent5UiExpectedValues.indexQuery().toCharArray()) {
            Map<String, Object> typed = EchoNativeAgent5UiActionRouter.routeCharacter(
                    "INDEX",
                    String.valueOf(indexFocus.get("focusedControl")),
                    "",
                    indexBuffer,
                    character
            );
            indexBuffer = String.valueOf(typed.get("value"));
        }
        events.add("text:index:" + indexBuffer);
        Map<String, Object> indexAction = EchoNativeAgent5UiActionRouter.activate("INDEX", Map.of(
                "focusedControl", indexFocus.get("focusedControl"),
                "indexBuffer", indexBuffer
        ));
        Map<String, Object> indexMutation = EchoNativeBootstrapMain.executeNativeIndexSearchFromUi(
                indexBuffer,
                String.valueOf(indexAction.get("output"))
        );
        boolean indexRuntimeMutationAccepted = runtimeMutationAccepted(
                indexMutation,
                "native.ui.index_search",
                "player.terminal_opened"
        );
        events.add("enter:index:" + indexAction.get("executedKey"));
        renderedLines.addAll(renderSurface("INDEX", state(
                indexFocus,
                "indexBuffer", indexBuffer,
                "indexOutput", String.valueOf(indexAction.get("output")),
                "indexSearchExecuted", true
        ), packId, moduleCount, itemCount, missionCount, regionCount));

        Map<String, Object> lensRoute = EchoNativeAgent5UiActionRouter.routeKey("LEFT_ALT", "INDEX", "WIKI");
        events.add("key:LEFT_ALT->" + lensRoute.get("destinationMode"));
        Map<String, Object> lensFocus = EchoNativeAgent5UiActionRouter.routeInitialFocus("LENS", "WIKI");
        Map<String, Object> lensAction = EchoNativeAgent5UiActionRouter.activate("LENS", Map.of(
                "focusedControl", lensFocus.get("focusedControl")
        ));
        Map<String, Object> lensMutation = EchoNativeBootstrapMain.useNativeScannerFromUiEvidence();
        boolean lensRuntimeMutationAccepted = runtimeMutationAccepted(
                lensMutation,
                "player.scanner_used",
                "player.scanner_used"
        );
        events.add("enter:lens:" + lensAction.get("executedKey"));
        renderedLines.addAll(renderSurface("LENS", state(
                lensFocus,
                "lensOutput", String.valueOf(lensAction.get("output")),
                "lensScanExecuted", true
        ), packId, moduleCount, itemCount, missionCount, regionCount));

        events.add("hud:update->HUD");
        Map<String, Object> hudUpdate = EchoNativeAgent5UiActionRouter.routeHudUpdate(Map.of(
                "hudHealth", EchoNativeAgent5UiExpectedValues.hud().get("health")));
        Map<String, Object> cameraFrame = EchoNativeAgent5UiActionRouter.routeCameraCinematicFrame(Map.of("cinematicFrame", 0));
        events.add("enter:hud:" + hudUpdate.get("effect") + "+" + cameraFrame.get("effect"));
        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.putAll(hudUpdate);
        hudState.putAll(cameraFrame);
        renderedLines.addAll(renderSurface("HUD", hudState, packId, moduleCount, itemCount, missionCount, regionCount));

        boolean passed = Boolean.TRUE.equals(terminalAction.get("handled"))
                && Boolean.TRUE.equals(indexAction.get("handled"))
                && Boolean.TRUE.equals(lensAction.get("handled"))
                && terminalRuntimeMutationAccepted
                && indexRuntimeMutationAccepted
                && lensRuntimeMutationAccepted
                && Boolean.TRUE.equals(hudUpdate.get("handled"))
                && Boolean.TRUE.equals(cameraFrame.get("handled"))
                && renderedLines.stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.terminalOutput()))
                && renderedLines.stream().anyMatch(line -> line.contains("index result(s):"))
                && renderedLines.stream().anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.lens().get("summary").toString()))
                && renderedLines.stream().anyMatch(line -> line.contains("HUD refreshed: health " + hudUpdate.get("hudHealth")))
                && renderedLines.stream().anyMatch(line -> line.contains("Camera over_shoulder frame 1 cue "
                        + EchoNativeAgent5UiExpectedValues.terminal().get("title")));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("hostEventTranscriptSmokeClass", EchoNativeAgent5HostEventTranscriptSmoke.class.getSimpleName());
        smoke.put("screenClass", screenClass);
        smoke.put("events", List.copyOf(events));
        smoke.put("renderedLines", List.copyOf(renderedLines));
        smoke.put("terminalMutation", terminalMutation);
        smoke.put("indexMutation", indexMutation);
        smoke.put("lensMutation", lensMutation);
        smoke.put("terminalRuntimeMutationAccepted", terminalRuntimeMutationAccepted);
        smoke.put("indexRuntimeMutationAccepted", indexRuntimeMutationAccepted);
        smoke.put("lensRuntimeMutationAccepted", lensRuntimeMutationAccepted);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static List<String> renderSurface(
            String mode,
            Map<String, Object> state,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        return EchoNativeAgent5UiHostSmokeSnapshot.strings(EchoNativeAgent5ScreenHostModel.render(
                mode,
                state,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        ), "surfaceLines");
    }

    private static Map<String, Object> state(Map<String, Object> focus, Object... values) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("focusedControl", focus.get("focusedControl"));
        state.put("initialFocusRouted", true);
        for (int index = 0; index < values.length; index += 2) {
            state.put(String.valueOf(values[index]), values[index + 1]);
        }
        return Map.copyOf(state);
    }

    private static boolean runtimeMutationAccepted(
            Map<String, Object> evidence,
            String runtimeActionId,
            String eventName
    ) {
        Map<String, Object> result = evidence == null ? Map.of() : evidence;
        return Boolean.TRUE.equals(result.get("mutated"))
                && Boolean.TRUE.equals(result.get("saveTouched"))
                && Boolean.TRUE.equals(result.get("missionUpdated"))
                && Boolean.TRUE.equals(result.get("feedbackEmitted"))
                && runtimeActionId.equals(result.get("runtimeActionId"))
                && eventName.equals(result.get("eventName"));
    }
}
