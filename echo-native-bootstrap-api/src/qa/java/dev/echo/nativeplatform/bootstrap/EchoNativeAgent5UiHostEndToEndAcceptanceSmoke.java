package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5UiHostEndToEndAcceptanceSmoke {
    private EchoNativeAgent5UiHostEndToEndAcceptanceSmoke() {
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
        Map<String, Object> interaction = EchoNativeAgent5UiHostInteractionStateAcceptance.assess(
                EchoNativeAgent5UiHostInteractionSmoke.run(
                        "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                        "echoashfallprotocol",
                        92,
                        20,
                        1,
                        1
                )
        );
        Map<String, Object> accepted = EchoNativeAgent5UiHostEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                liveSurface,
                render,
                interaction
        );
        Map<String, Object> rejectedNoInput = EchoNativeAgent5UiHostEndToEndAcceptance.assess(
                hotkey,
                Map.of("accepted", false, "surface", "TERMINAL"),
                liveSurface,
                render,
                interaction
        );
        Map<String, Object> rejectedRender = EchoNativeAgent5UiHostEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                liveSurface,
                Map.of("accepted", false, "surface", "TERMINAL"),
                interaction
        );
        Map<String, Object> rejectedInteraction = EchoNativeAgent5UiHostEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                liveSurface,
                render,
                Map.of("accepted", false, "stepCount", 9)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.TRUE.equals(accepted.get("serviceCodeExecuted"))
                && "ui_host_end_to_end:M->TERMINAL:10".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoInput.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoInput.get("serviceCodeExecuted"))
                && Boolean.FALSE.equals(rejectedRender.get("accepted"))
                && Boolean.FALSE.equals(rejectedRender.get("serviceCodeExecuted"))
                && Boolean.FALSE.equals(rejectedInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedInteraction.get("serviceCodeExecuted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("uiHostEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5UiHostEndToEndAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoInput", rejectedNoInput);
        smoke.put("rejectedRender", rejectedRender);
        smoke.put("rejectedInteraction", rejectedInteraction);
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
}
