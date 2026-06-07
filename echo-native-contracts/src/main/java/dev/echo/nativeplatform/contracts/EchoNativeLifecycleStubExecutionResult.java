package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLifecycleStubExecutionResult(
        String resultId,
        boolean executed,
        boolean stubOnly,
        boolean inertStubHandlersExecuted,
        boolean realAddonCodeExecuted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean registryInjected,
        boolean transformsPerformed,
        int moduleCount,
        int stubHandlerCount,
        List<Map<String, Object>> lifecycleEvents
) {
}
