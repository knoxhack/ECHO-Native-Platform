package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeConfigSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean localOnly,
        boolean writePlanOnly,
        boolean installedConfigMutated,
        boolean fixtureConfigMutated,
        boolean filesystemMutated,
        boolean serviceCodeExecuted,
        boolean addonCodeExecuted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean commandExecuted,
        boolean registryInjected,
        boolean registryMutated,
        List<String> completedChecks
) {
}
