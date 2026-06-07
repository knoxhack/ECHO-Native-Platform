package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveMainMenuOverrideAcceptanceSmoke {
    private static final String SCREEN_CLASS = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";

    private EchoNativeAgent5LiveMainMenuOverrideAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> override = override(true, true, "");
        Map<String, Object> surface = liveSurface(true, "MAIN_MENU");
        Map<String, Object> options = EchoNativeAgent5MainMenuOptionActivationSmoke.capture();
        Map<String, Object> endToEnd = EchoNativeAgent5MainMenuEndToEndAcceptance.assess(override, options);
        Map<String, Object> accepted = EchoNativeAgent5LiveMainMenuOverrideAcceptance.assess(
                override,
                surface,
                endToEnd
        );
        Map<String, Object> rejectedNoTitle = EchoNativeAgent5LiveMainMenuOverrideAcceptance.assess(
                override(false, false, "current_screen_not_title:PauseScreen"),
                liveSurface(false, "MAIN_MENU"),
                endToEnd
        );
        Map<String, Object> rejectedNoSurface = EchoNativeAgent5LiveMainMenuOverrideAcceptance.assess(
                override,
                liveSurface(false, "MAIN_MENU"),
                endToEnd
        );
        Map<String, Object> rejectedNoOptions = EchoNativeAgent5LiveMainMenuOverrideAcceptance.assess(
                override,
                surface,
                Map.of("accepted", false, "selectedOptions", java.util.List.of())
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_main_menu_override:accepted:MAIN_MENU:4".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoTitle.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoSurface.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoOptions.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveMainMenuOverrideAcceptanceSmokeClass",
                EchoNativeAgent5LiveMainMenuOverrideAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoTitle", rejectedNoTitle);
        smoke.put("rejectedNoSurface", rejectedNoSurface);
        smoke.put("rejectedNoOptions", rejectedNoOptions);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> override(boolean titleDetected, boolean attached, String skipReason) {
        return EchoNativeAgent5MainMenuOverrideSmoke.capture(
                titleDetected,
                attached,
                skipReason,
                SCREEN_CLASS,
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
    }

    private static Map<String, Object> liveSurface(boolean accepted, String surface) {
        return EchoNativeAgent5LiveSurfaceAcceptance.assess(
                accepted,
                SCREEN_CLASS,
                SCREEN_CLASS,
                surface,
                surface
        );
    }
}
