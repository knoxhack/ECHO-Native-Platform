package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLifecycleStubExecutionPlan(
        String planId,
        boolean ready,
        boolean stubOnly,
        boolean inertHandlersOnly,
        boolean realAddonCodeExecuted,
        boolean minecraftClassesResolved,
        boolean classloaderCreated,
        boolean registryBridgeTouched,
        boolean transformsRequested,
        List<String> lifecyclePhases,
        List<String> moduleOrder
) {
}
