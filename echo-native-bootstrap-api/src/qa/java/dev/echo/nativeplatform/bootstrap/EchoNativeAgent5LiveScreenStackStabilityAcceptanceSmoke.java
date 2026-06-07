package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveScreenStackStabilityAcceptanceSmoke {
    private static final String SCREEN_CLASS = "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";

    private EchoNativeAgent5LiveScreenStackStabilityAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> stack = EchoNativeAgent5ScreenStackSmoke.capture(
                SCREEN_CLASS,
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> lifecycle = EchoNativeAgent5ScreenLifecycleSmoke.capture(
                SCREEN_CLASS,
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> interaction = EchoNativeAgent5UiHostInteractionSmoke.run(
                SCREEN_CLASS,
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> accepted = EchoNativeAgent5LiveScreenStackStabilityAcceptance.assess(
                stack,
                lifecycle,
                interaction
        );
        Map<String, Object> rejectedNoStack = EchoNativeAgent5LiveScreenStackStabilityAcceptance.assess(
                Map.of("passed", false),
                lifecycle,
                interaction
        );
        Map<String, Object> rejectedNoLifecycle = EchoNativeAgent5LiveScreenStackStabilityAcceptance.assess(
                stack,
                Map.of("passed", false),
                interaction
        );
        Map<String, Object> rejectedNoInteraction = EchoNativeAgent5LiveScreenStackStabilityAcceptance.assess(
                stack,
                lifecycle,
                Map.of("passed", false, "steps", java.util.List.of())
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_screen_stack_stability:accepted:10-surfaces:no-crash".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoStack.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoLifecycle.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoInteraction.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveScreenStackStabilityAcceptanceSmokeClass",
                EchoNativeAgent5LiveScreenStackStabilityAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoStack", rejectedNoStack);
        smoke.put("rejectedNoLifecycle", rejectedNoLifecycle);
        smoke.put("rejectedNoInteraction", rejectedNoInteraction);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }
}
