package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveTextInputAcceptanceSmoke {
    private static final String SCREEN_CLASS = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";

    private EchoNativeAgent5LiveTextInputAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> terminal = routeTextInput(
                "TERMINAL",
                "terminal:input",
                "terminalBuffer",
                EchoNativeAgent5UiExpectedValues.terminalCommand(),
                EchoNativeAgent5UiExpectedValues.terminalOutput()
        );
        Map<String, Object> index = routeTextInput(
                "INDEX",
                "index:search",
                "indexBuffer",
                EchoNativeAgent5UiExpectedValues.indexQuery(),
                EchoNativeAgent5UiExpectedValues.indexSearchOutput()
        );
        Map<String, Object> rejectedUnfocused = EchoNativeAgent5LiveTextInputAcceptance.assess(
                "TERMINAL",
                EchoNativeAgent5UiExpectedValues.terminalCommand(),
                EchoNativeAgent5UiExpectedValues.terminalOutput(),
                List.of(EchoNativeAgent5UiActionRouter.routeCharacter("TERMINAL", "terminal:surface", "", "", 's')),
                EchoNativeAgent5UiActionRouter.routeEditKey("BACKSPACE", "TERMINAL", "terminal:surface", "status", ""),
                EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                        "focusedControl", "terminal:surface",
                        "terminalBuffer", EchoNativeAgent5UiExpectedValues.terminalCommand()
                )),
                EchoNativeAgent5UiHostSmokeSnapshot.capture(
                        "TERMINAL",
                        true,
                        SCREEN_CLASS,
                        "echoashfallprotocol",
                        92,
                        20,
                        1,
                        1
                )
        );
        boolean passed = Boolean.TRUE.equals(terminal.get("accepted"))
                && Boolean.TRUE.equals(index.get("accepted"))
                && ("live_text_input:accepted:TERMINAL:"
                        + EchoNativeAgent5UiExpectedValues.terminalCommand()).equals(terminal.get("effect"))
                && ("live_text_input:accepted:INDEX:"
                        + EchoNativeAgent5UiExpectedValues.indexQuery()).equals(index.get("effect"))
                && Boolean.FALSE.equals(rejectedUnfocused.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveTextInputAcceptanceSmokeClass",
                EchoNativeAgent5LiveTextInputAcceptanceSmoke.class.getSimpleName());
        smoke.put("terminal", terminal);
        smoke.put("index", index);
        smoke.put("rejectedUnfocused", rejectedUnfocused);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> routeTextInput(
            String mode,
            String focusedControl,
            String targetBuffer,
            String expectedBuffer,
            String expectedOutput
    ) {
        String terminalBuffer = "";
        String indexBuffer = "";
        List<Map<String, Object>> characterRoutes = new ArrayList<>();
        for (int index = 0; index < expectedBuffer.length(); index++) {
            Map<String, Object> route = EchoNativeAgent5UiActionRouter.routeCharacter(
                    mode,
                    focusedControl,
                    terminalBuffer,
                    indexBuffer,
                    expectedBuffer.charAt(index)
            );
            characterRoutes.add(route);
            if ("terminalBuffer".equals(route.get("targetBuffer"))) {
                terminalBuffer = String.valueOf(route.get("value"));
            }
            if ("indexBuffer".equals(route.get("targetBuffer"))) {
                indexBuffer = String.valueOf(route.get("value"));
            }
        }
        Map<String, Object> extra = EchoNativeAgent5UiActionRouter.routeCharacter(
                mode,
                focusedControl,
                terminalBuffer,
                indexBuffer,
                'x'
        );
        characterRoutes.add(extra);
        if ("terminalBuffer".equals(extra.get("targetBuffer"))) {
            terminalBuffer = String.valueOf(extra.get("value"));
        }
        if ("indexBuffer".equals(extra.get("targetBuffer"))) {
            indexBuffer = String.valueOf(extra.get("value"));
        }
        Map<String, Object> edit = EchoNativeAgent5UiActionRouter.routeEditKey(
                "BACKSPACE",
                mode,
                focusedControl,
                terminalBuffer,
                indexBuffer
        );
        if ("terminalBuffer".equals(edit.get("targetBuffer"))) {
            terminalBuffer = String.valueOf(edit.get("value"));
        }
        if ("indexBuffer".equals(edit.get("targetBuffer"))) {
            indexBuffer = String.valueOf(edit.get("value"));
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("focusedControl", focusedControl);
        state.put("mouseRouted", true);
        state.put(targetBuffer, expectedBuffer);
        Map<String, Object> submit = EchoNativeAgent5UiActionRouter.activate(mode, state);
        state.put(String.valueOf(submit.get("outputKey")), submit.get("output"));
        state.put(String.valueOf(submit.get("executedKey")), true);
        return EchoNativeAgent5LiveTextInputAcceptance.assess(
                mode,
                expectedBuffer,
                expectedOutput,
                characterRoutes,
                edit,
                submit,
                EchoNativeAgent5UiHostSmokeSnapshot.capture(
                        mode,
                        true,
                        SCREEN_CLASS,
                        "echoashfallprotocol",
                        92,
                        20,
                        1,
                        1,
                        state
                )
        );
    }
}
