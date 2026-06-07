package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5HudOverlayEndToEndAcceptanceSmoke {
    private EchoNativeAgent5HudOverlayEndToEndAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> hudSignal = hudDataSignal();
        Map<String, Object> overlay = EchoNativeAgent5HudOverlaySmoke.capture(
                true,
                true,
                "echoashfallprotocol:hud_mode:field",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> update = EchoNativeAgent5HudUpdateSmoke.capture();
        Map<String, Object> camera = EchoNativeAgent5CameraCinematicSmoke.capture();
        Map<String, Object> accepted = EchoNativeAgent5HudOverlayEndToEndAcceptance.assess(
                hudSignal,
                overlay,
                update,
                camera
        );
        Map<String, Object> rejectedNoOverlay = EchoNativeAgent5HudOverlayEndToEndAcceptance.assess(
                hudSignal,
                EchoNativeAgent5HudOverlaySmoke.capture(
                        true,
                        false,
                        "echoashfallprotocol:hud_mode:field",
                        "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                        "echoashfallprotocol",
                        92,
                        20,
                        1,
                        1
                ),
                update,
                camera
        );
        Map<String, Object> rejectedNoHudUpdate = EchoNativeAgent5HudOverlayEndToEndAcceptance.assess(
                hudSignal,
                overlay,
                Map.of("passed", false, "hudHealth", 92, "effect", ""),
                camera
        );
        Map<String, Object> rejectedNoCamera = EchoNativeAgent5HudOverlayEndToEndAcceptance.assess(
                hudSignal,
                overlay,
                update,
                Map.of("passed", false, "cameraMode", "", "effect", "")
        );
        Map<String, Object> rejectedNoMutation = EchoNativeAgent5HudOverlayEndToEndAcceptance.assess(
                hudSignal,
                overlay,
                withoutRuntimeMutation(update),
                camera
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "hud_overlay_end_to_end:data_backed:85".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoOverlay.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHudUpdate.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoCamera.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMutation.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("hudOverlayEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5HudOverlayEndToEndAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoOverlay", rejectedNoOverlay);
        smoke.put("rejectedNoHudUpdate", rejectedNoHudUpdate);
        smoke.put("rejectedNoCamera", rejectedNoCamera);
        smoke.put("rejectedNoMutation", rejectedNoMutation);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
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

    private static Map<String, Object> withoutRuntimeMutation(Map<String, Object> update) {
        Map<String, Object> copy = new LinkedHashMap<>(update);
        copy.put("runtimeMutationAccepted", false);
        copy.put("runtimeActionId", "native.ui.hud_refresh");
        copy.put("eventName", "client_tick");
        return Map.copyOf(copy);
    }
}
