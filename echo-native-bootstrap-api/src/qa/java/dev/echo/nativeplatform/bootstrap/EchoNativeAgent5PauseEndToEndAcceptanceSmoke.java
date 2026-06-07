package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5PauseEndToEndAcceptanceSmoke {
    private EchoNativeAgent5PauseEndToEndAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> hotkey = screenRoute();
        Map<String, Object> liveSurface = EchoNativeAgent5LiveSurfaceAcceptance.assess(
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "PAUSE",
                "PAUSE"
        );
        Map<String, Object> screenInput = screenInput(hotkey, liveSurface);
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "PAUSE",
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1,
                Map.of("previousMode", "LENS")
        );
        Map<String, Object> render = EchoNativeAgent5LiveSurfaceRenderAcceptance.assess(liveSurface, snapshot);
        Map<String, Object> interaction = EchoNativeAgent5UiHostInteractionSmoke.run(
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> option = EchoNativeAgent5PauseOptionActivationSmoke.capture();
        Map<String, Object> accepted = EchoNativeAgent5PauseEndToEndAcceptance.assess(
                hotkey,
                screenInput,
                render,
                interaction,
                option
        );
        Map<String, Object> rejectedNoInput = EchoNativeAgent5PauseEndToEndAcceptance.assess(
                hotkey,
                Map.of("accepted", false, "surface", "PAUSE"),
                render,
                interaction,
                option
        );
        Map<String, Object> rejectedNoRender = EchoNativeAgent5PauseEndToEndAcceptance.assess(
                hotkey,
                screenInput,
                Map.of("accepted", false, "surface", "PAUSE"),
                interaction,
                option
        );
        Map<String, Object> rejectedNoInteraction = EchoNativeAgent5PauseEndToEndAcceptance.assess(
                hotkey,
                screenInput,
                render,
                Map.of("passed", false, "steps", java.util.List.of()),
                option
        );
        Map<String, Object> rejectedNoOption = EchoNativeAgent5PauseEndToEndAcceptance.assess(
                hotkey,
                screenInput,
                render,
                interaction,
                Map.of("passed", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "pause_end_to_end:screen_escape->PAUSE:LENS".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoInput.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRender.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoOption.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("pauseEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5PauseEndToEndAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoInput", rejectedNoInput);
        smoke.put("rejectedNoRender", rejectedNoRender);
        smoke.put("rejectedNoInteraction", rejectedNoInteraction);
        smoke.put("rejectedNoOption", rejectedNoOption);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> screenRoute() {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("handled", true);
        route.put("key", "ESCAPE");
        route.put("surface", "PAUSE");
        route.put("routeType", "screen");
        route.put("screenRouteHandled", true);
        route.put("effect", "pause:screen_escape");
        route.put("adapterCoreBridge", true);
        route.put("serviceCodeExecuted", true);
        return Map.copyOf(route);
    }

    private static Map<String, Object> screenInput(Map<String, Object> route, Map<String, Object> liveSurface) {
        Map<String, Object> acceptance = new LinkedHashMap<>();
        acceptance.put("accepted", Boolean.TRUE.equals(route.get("screenRouteHandled"))
                && Boolean.TRUE.equals(liveSurface.get("accepted"))
                && "PAUSE".equals(route.get("surface"))
                && "PAUSE".equals(liveSurface.get("currentMode")));
        acceptance.put("key", "ESCAPE");
        acceptance.put("surface", "PAUSE");
        acceptance.put("routeType", "screen");
        acceptance.put("screenRouteHandled", Boolean.TRUE.equals(route.get("screenRouteHandled")));
        acceptance.put("liveSurfaceAccepted", Boolean.TRUE.equals(liveSurface.get("accepted")));
        acceptance.put("effect", "screen_input_acceptance:ESCAPE->PAUSE");
        acceptance.put("adapterCoreBridge", true);
        acceptance.put("serviceCodeExecuted", true);
        return Map.copyOf(acceptance);
    }
}
