package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveVisualFrameAcceptanceSmoke {
    private static final String SCREEN_CLASS = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";

    private EchoNativeAgent5LiveVisualFrameAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> theme = EchoNativeAgent5ThemeApplicationSmoke.capture(
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> layout = EchoNativeAgent5RenderCoreLayoutSmoke.capture();
        Map<String, Object> camera = EchoNativeAgent5CameraCinematicSmoke.capture();
        Map<String, Object> hud = EchoNativeAgent5HudOverlaySmoke.capture(
                true,
                true,
                "hud:passive",
                SCREEN_CLASS,
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> accepted = EchoNativeAgent5LiveVisualFrameAcceptance.assess(
                theme,
                layout,
                camera,
                hud
        );
        Map<String, Object> rejectedNoTheme = EchoNativeAgent5LiveVisualFrameAcceptance.assess(
                Map.of("passed", false),
                layout,
                camera,
                hud
        );
        Map<String, Object> rejectedNoLayout = EchoNativeAgent5LiveVisualFrameAcceptance.assess(
                theme,
                Map.of("passed", false),
                camera,
                hud
        );
        Map<String, Object> rejectedNoCamera = EchoNativeAgent5LiveVisualFrameAcceptance.assess(
                theme,
                layout,
                Map.of("passed", false),
                hud
        );
        Map<String, Object> rejectedNoHud = EchoNativeAgent5LiveVisualFrameAcceptance.assess(
                theme,
                layout,
                camera,
                Map.of("passed", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_visual_frame:accepted:theme/render/camera/hud".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoTheme.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoLayout.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoCamera.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHud.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveVisualFrameAcceptanceSmokeClass",
                EchoNativeAgent5LiveVisualFrameAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoTheme", rejectedNoTheme);
        smoke.put("rejectedNoLayout", rejectedNoLayout);
        smoke.put("rejectedNoCamera", rejectedNoCamera);
        smoke.put("rejectedNoHud", rejectedNoHud);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }
}
