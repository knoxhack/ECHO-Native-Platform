package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveHudOverlayRouteAcceptanceSmoke {
    private static final String SCREEN_CLASS = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";

    private EchoNativeAgent5LiveHudOverlayRouteAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> hotkey = hudDataSignal();
        Map<String, Object> route = Map.of(
                "handled", true,
                "dataBackedAction", true,
                "effect", "echohudcore:data_backed_overlay"
        );
        Map<String, Object> overlay = overlay(true);
        Map<String, Object> update = EchoNativeAgent5HudUpdateSmoke.capture();
        Map<String, Object> camera = EchoNativeAgent5CameraCinematicSmoke.capture();
        Map<String, Object> endToEnd = EchoNativeAgent5HudOverlayEndToEndAcceptance.assess(
                hotkey,
                overlay,
                update,
                camera
        );
        Map<String, Object> accepted = EchoNativeAgent5LiveHudOverlayRouteAcceptance.assess(
                hotkey,
                route,
                overlay,
                endToEnd
        );
        Map<String, Object> rejectedNoRoute = EchoNativeAgent5LiveHudOverlayRouteAcceptance.assess(
                hotkey,
                Map.of("handled", false, "destinationMode", "", "effect", ""),
                overlay,
                endToEnd
        );
        Map<String, Object> rejectedNoOverlay = EchoNativeAgent5LiveHudOverlayRouteAcceptance.assess(
                hotkey,
                route,
                overlay(false),
                endToEnd
        );
        Map<String, Object> rejectedNoEndToEnd = EchoNativeAgent5LiveHudOverlayRouteAcceptance.assess(
                hotkey,
                route,
                overlay,
                Map.of("accepted", false, "effect", "hud_overlay_end_to_end:rejected")
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_hud_overlay_route:accepted:data_backed_hud:85".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoRoute.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoOverlay.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoEndToEnd.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveHudOverlayRouteAcceptanceSmokeClass",
                EchoNativeAgent5LiveHudOverlayRouteAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoRoute", rejectedNoRoute);
        smoke.put("rejectedNoOverlay", rejectedNoOverlay);
        smoke.put("rejectedNoEndToEnd", rejectedNoEndToEnd);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> overlay(boolean rendered) {
        return EchoNativeAgent5HudOverlaySmoke.capture(
                true,
                rendered,
                "echoashfallprotocol:hud_mode:field",
                SCREEN_CLASS,
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
    }

    private static Map<String, Object> hudDataSignal() {
        Map<String, Object> signal = new LinkedHashMap<>();
        signal.put("observed", true);
        signal.put("handled", true);
        signal.put("physicalPoller", false);
        signal.put("physicalPollerExecuted", false);
        signal.put("key", "");
        signal.put("surface", "HUD");
        signal.put("action", "hud.data_update");
        signal.put("effect", "hud:data_backed_overlay");
        signal.put("adapterCoreBridge", true);
        signal.put("serviceCodeExecuted", true);
        return Map.copyOf(signal);
    }
}
