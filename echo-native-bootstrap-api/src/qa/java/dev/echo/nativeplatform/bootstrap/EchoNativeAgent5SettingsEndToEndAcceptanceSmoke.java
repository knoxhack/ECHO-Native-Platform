package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5SettingsEndToEndAcceptanceSmoke {
    private EchoNativeAgent5SettingsEndToEndAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> settingsAction = routeAction("SETTINGS_ACTION", "SETTINGS", "settings.open");
        Map<String, Object> liveSurface = EchoNativeAgent5LiveSurfaceAcceptance.assess(
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "SETTINGS",
                "SETTINGS"
        );
        Map<String, Object> menuInput = menuInput("SETTINGS");
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "SETTINGS",
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
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
        Map<String, Object> adjustment = EchoNativeAgent5SettingsAdjustmentSmoke.capture();
        Map<String, Object> accepted = EchoNativeAgent5SettingsEndToEndAcceptance.assess(
                settingsAction,
                menuInput,
                render,
                interaction,
                adjustment
        );
        Map<String, Object> rejectedNoInput = EchoNativeAgent5SettingsEndToEndAcceptance.assess(
                settingsAction,
                Map.of("accepted", false, "surface", "SETTINGS"),
                render,
                interaction,
                adjustment
        );
        Map<String, Object> rejectedNoRender = EchoNativeAgent5SettingsEndToEndAcceptance.assess(
                settingsAction,
                menuInput,
                Map.of("accepted", false, "surface", "SETTINGS"),
                interaction,
                adjustment
        );
        Map<String, Object> rejectedNoInteraction = EchoNativeAgent5SettingsEndToEndAcceptance.assess(
                settingsAction,
                menuInput,
                render,
                Map.of("passed", false, "steps", java.util.List.of()),
                adjustment
        );
        Map<String, Object> rejectedNoAdjustment = EchoNativeAgent5SettingsEndToEndAcceptance.assess(
                settingsAction,
                menuInput,
                render,
                interaction,
                Map.of("passed", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "settings_end_to_end:SETTINGS_ACTION->SETTINGS:ashfall-accessible:subtitles_off".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoInput.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRender.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoAdjustment.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("settingsEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5SettingsEndToEndAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoInput", rejectedNoInput);
        smoke.put("rejectedNoRender", rejectedNoRender);
        smoke.put("rejectedNoInteraction", rejectedNoInteraction);
        smoke.put("rejectedNoAdjustment", rejectedNoAdjustment);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> routeAction(String key, String surface, String action) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("handled", true);
        route.put("key", key);
        route.put("surface", surface);
        route.put("action", action);
        route.put("source", "menu_action");
        return Map.copyOf(route);
    }

    private static Map<String, Object> menuInput(String surface) {
        return Map.of(
                "accepted", true,
                "surface", surface,
                "source", "menu_action",
                "serviceCodeExecuted", true
        );
    }
}
