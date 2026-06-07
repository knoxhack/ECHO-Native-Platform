package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5TerminalEndToEndAcceptanceSmoke {
    private EchoNativeAgent5TerminalEndToEndAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> hotkey = EchoNativeAgent5PhysicalHotkeyPoller.poll(
                EchoNativeAgent5PhysicalHotkeyPoller.emptyState(),
                pressed("M")
        );
        Map<String, Object> liveSurface = EchoNativeAgent5LiveSurfaceAcceptance.assess(
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "TERMINAL",
                "TERMINAL"
        );
        Map<String, Object> physicalInput = EchoNativeAgent5PhysicalInputAcceptance.assess(hotkey, liveSurface);
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "TERMINAL",
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> render = EchoNativeAgent5LiveSurfaceRenderAcceptance.assess(liveSurface, snapshot);
        Map<String, Object> focus = EchoNativeAgent5FocusManagerSmoke.capture();
        Map<String, Object> editing = EchoNativeAgent5TextEditingSmoke.capture();
        Map<String, Object> transcript = EchoNativeAgent5HostEventTranscriptSmoke.capture(
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> accepted = EchoNativeAgent5TerminalEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                focus,
                editing,
                transcript
        );
        Map<String, Object> rejectedNoInput = EchoNativeAgent5TerminalEndToEndAcceptance.assess(
                hotkey,
                Map.of("accepted", false, "surface", "TERMINAL"),
                render,
                focus,
                editing,
                transcript
        );
        Map<String, Object> rejectedNoRender = EchoNativeAgent5TerminalEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                Map.of("accepted", false, "surface", "TERMINAL"),
                focus,
                editing,
                transcript
        );
        Map<String, Object> rejectedNoCommand = EchoNativeAgent5TerminalEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                focus,
                Map.of("passed", false, "terminalBuffer", "status"),
                transcript
        );
        Map<String, Object> rejectedNoTranscript = EchoNativeAgent5TerminalEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                focus,
                editing,
                Map.of("passed", false)
        );
        Map<String, Object> rejectedNoMutation = EchoNativeAgent5TerminalEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                focus,
                editing,
                withoutRuntimeMutation(
                        transcript,
                        "terminalMutation",
                        "native.ui.terminal_command",
                        "command_execution"
                )
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "terminal_end_to_end:M->TERMINAL:status".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoInput.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRender.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoCommand.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoTranscript.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMutation.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("terminalEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5TerminalEndToEndAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoInput", rejectedNoInput);
        smoke.put("rejectedNoRender", rejectedNoRender);
        smoke.put("rejectedNoCommand", rejectedNoCommand);
        smoke.put("rejectedNoTranscript", rejectedNoTranscript);
        smoke.put("rejectedNoMutation", rejectedNoMutation);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Boolean> pressed(String key) {
        Map<String, Boolean> state = new LinkedHashMap<>(EchoNativeAgent5PhysicalHotkeyPoller.emptyState());
        state.put(key, true);
        return Map.copyOf(state);
    }

    private static Map<String, Object> withoutRuntimeMutation(
            Map<String, Object> transcript,
            String mutationKey,
            String runtimeActionId,
            String eventName
    ) {
        Map<String, Object> copy = new LinkedHashMap<>(transcript);
        Map<String, Object> mutation = new LinkedHashMap<>();
        mutation.put("mutated", false);
        mutation.put("saveTouched", true);
        mutation.put("missionUpdated", true);
        mutation.put("feedbackEmitted", true);
        mutation.put("runtimeActionId", runtimeActionId);
        mutation.put("eventName", eventName);
        copy.put(mutationKey, Map.copyOf(mutation));
        return Map.copyOf(copy);
    }
}
