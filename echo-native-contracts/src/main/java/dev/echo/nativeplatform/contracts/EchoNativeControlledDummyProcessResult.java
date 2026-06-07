package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeControlledDummyProcessResult(
        String resultId,
        boolean executed,
        boolean dummyProcessOnly,
        boolean dummyProcessLaunched,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean minecraftLaunched,
        boolean classloaderCreated,
        boolean resolvesRuntimeClasses,
        boolean commandExecuted,
        boolean filesystemMutated,
        boolean timedOut,
        int exitCode,
        String outcome
) {
}
