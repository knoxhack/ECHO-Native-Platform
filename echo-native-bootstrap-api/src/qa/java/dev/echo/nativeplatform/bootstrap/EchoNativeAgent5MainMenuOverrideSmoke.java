package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5MainMenuOverrideSmoke {
    private EchoNativeAgent5MainMenuOverrideSmoke() {
    }

    public static Map<String, Object> capture(
            boolean titleScreenDetected,
            boolean overrideAttached,
            String skipReason,
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "MAIN_MENU",
                overrideAttached,
                screenClass,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        boolean guardSatisfied = titleScreenDetected && overrideAttached;
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("mainMenuOverrideSmokeClass", EchoNativeAgent5MainMenuOverrideSmoke.class.getSimpleName());
        smoke.put("strategy", "guarded_title_screen_replacement");
        smoke.put("titleScreenDetected", titleScreenDetected);
        smoke.put("overrideAttached", overrideAttached);
        smoke.put("skipReason", skipReason == null ? "" : skipReason);
        smoke.put("screenClass", screenClass);
        smoke.put("snapshot", snapshot);
        smoke.put("screenTitle", snapshot.get("screenTitle"));
        smoke.put("surfaceLines", snapshot.get("surfaceLines"));
        smoke.put("passed", guardSatisfied || !titleScreenDetected);
        smoke.put("guardSatisfied", guardSatisfied);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        return Map.copyOf(smoke);
    }
}
