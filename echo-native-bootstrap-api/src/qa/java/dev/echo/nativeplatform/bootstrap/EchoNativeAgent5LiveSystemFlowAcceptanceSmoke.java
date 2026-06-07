package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveSystemFlowAcceptanceSmoke {
    private EchoNativeAgent5LiveSystemFlowAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> settings = object(EchoNativeAgent5SettingsEndToEndAcceptanceSmoke.capture()
                .get("accepted"));
        Map<String, Object> pause = object(EchoNativeAgent5PauseEndToEndAcceptanceSmoke.capture()
                .get("accepted"));
        Map<String, Object> recovery = object(EchoNativeAgent5RecoveryEndToEndAcceptanceSmoke.capture()
                .get("accepted"));
        Map<String, Object> accepted = EchoNativeAgent5LiveSystemFlowAcceptance.assess(settings, pause, recovery);
        Map<String, Object> rejectedNoSettings = EchoNativeAgent5LiveSystemFlowAcceptance.assess(
                Map.of("accepted", false),
                pause,
                recovery
        );
        Map<String, Object> rejectedNoPause = EchoNativeAgent5LiveSystemFlowAcceptance.assess(
                settings,
                Map.of("accepted", false),
                recovery
        );
        Map<String, Object> rejectedNoRecovery = EchoNativeAgent5LiveSystemFlowAcceptance.assess(
                settings,
                pause,
                Map.of("accepted", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_system_flow:accepted:SETTINGS_ACTION/screen_escape/RECOVERY_ACTION".equals(
                        accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoSettings.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoPause.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRecovery.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveSystemFlowAcceptanceSmokeClass",
                EchoNativeAgent5LiveSystemFlowAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoSettings", rejectedNoSettings);
        smoke.put("rejectedNoPause", rejectedNoPause);
        smoke.put("rejectedNoRecovery", rejectedNoRecovery);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
